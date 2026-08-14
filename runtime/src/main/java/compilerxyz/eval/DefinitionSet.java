package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.definition_set.ArtifactDataDefSet;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.dynamic.EmitCtx;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.function.Function;
import compilerxyz.gen.Value;
import static compilerxyz.util.MustCast.mustCast;

public final class DefinitionSet implements ExporterAny {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }

  public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();
  public final DefSetId defSetId;
  public final TOrderedMap<DefSetId, DefinitionSet> dependencies;
  public boolean finished;
  public final TList<SourceFunctionData> functions;
  public final TList<InterfaceData> interfaces;
  public final TList<TypeSourceObjectDynamic> objects;
  public final TList<TypeSourceUnionDynamic> unions;

  public DefinitionSet(DefSetId defSetId) {
    this.defSetId = defSetId;
    this.dependencies = new TOrderedMap<>();
    this.finished = false;
    this.functions = new TList<>();
    this.interfaces = new TList<>();
    this.objects = new TList<>();
    this.unions = new TList<>();
  }

  public EResult<Void> finish(ExprContext xx, QExprId loc) {
    if (finished) {
      return EResult.ok(null);
    }
    for (var func : functions) {
      var r = func.finish(xx, loc);
      if (r.hasErrors()) return r.reerr();
    }
    for (long objIdx = 0; objIdx < objects.size(); objIdx++) {
      var obj = objects.get((int) objIdx);
      if (obj.underlyingType.optIsNone()) {
        return EResult.err(
            loc, "Object '" + obj.name.optUnwrapOr("(unnamed)") + "' underlying type not finalised before finish");
      }
      var underlyingType = obj.underlyingType.optUnwrap();
      if (obj.setId.optIsNone()) {
        throw new AssertionError(
            "DefinitionSet.finish: source object '" + obj.name.optUnwrapOr("(unnamed)") + "' has null setId");
      }
      var objInternalName =
          compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(obj.setId.optUnwrap(), objIdx);
      for (var method : obj.methods) {
        var nameStr = obj.name.optUnwrapOr("(unnamed)");
        var r = method.finish(xx, loc, nameStr, obj, underlyingType, objInternalName);
        if (r.hasErrors()) return r.reerr();
      }
      for (var impl : obj.interfaceImpls) {
        for (var m : impl.methodImpls) {
          var r = m.finish(xx, loc, impl.interface_, obj, underlyingType);
          if (r.hasErrors()) return r.reerr();
        }
      }
      var r = obj.finish(xx, loc, objIdx);
      if (r.hasErrors()) return r.reerr();
    }
    for (long unionIdx = 0; unionIdx < unions.size(); unionIdx++) {
      var r = unions.get((int) unionIdx).finish(xx, loc, unionIdx);
      if (r.hasErrors()) return r.reerr();
    }
    finishFunctions();
    for (long i = 0; i < interfaces.size(); i++) {
      var iface = interfaces.get((int) i);
      var r = iface.finish(loc, i);
      if (r.hasErrors()) return r.reerr();
    }
    finished = true;
    for (var dep : dependencies.values()) {
      var r = dep.finish(xx, loc);
      if (r.hasErrors()) return r.reerr();
    }
    return EResult.ok(null);
  }

  public Opt<DefSetFunctionsEmitter.EmittedFunctions> finishFunctions() {
    var specs = new TList<DefSetFunctionsEmitter.FunctionSpec>();
    var contributors = new TList<SourceFunctionData>();
    for (var func : functions) {
      var spec = func.functionSpec;
      if (spec.optIsNone()) {
        continue;
      }
      if (func.compiledClassName.optIsSome()) {
        continue;
      }
      specs.add(spec.optUnwrap());
      contributors.add(func);
    }
    if (specs.isEmpty()) {
      return Opt.none();
    }
    var _dsfBinaryName = DefSetFunctionsEmitter.functionsBinaryName(defSetId);
    var _dsfInternalName = DefSetFunctionsEmitter.functionsInternalName(defSetId);
    var _dsfCw =
        new ClassWriter(
            ClassWriter.COMPUTE_FRAMES
                | ClassWriter.COMPUTE_MAXS) {
          @Override
          protected String getCommonSuperClass(String type1, String type2) {
            return JvmConstants.INTNAME_OBJECT.value();
          }
        };
    _dsfCw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC
            | Opcodes.ACC_FINAL
            | Opcodes.ACC_SUPER,
        _dsfInternalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);
    var _dsfFieldDescriptors =
        new TOrderedMap<String, JavaDataDescriptor>();
    var _dsfStaticFieldValues = new TOrderedMap<String, Object>();
    for (var _dsfSpec : specs) {
      var _dsfPrefix = DefSetFunctionsEmitter.functionFieldPrefix(_dsfSpec.funcIdx());
      for (int _dsfI = 0; _dsfI < _dsfSpec.externals().size(); _dsfI++) {
        var _dsfEntry = _dsfSpec.externals().get(_dsfI);
        var _dsfName =
            _dsfPrefix + Prefixes.EXTERNAL_PREFIX + _dsfI;
        var _dsfExternal = _dsfEntry.external();
        var _dsfDesc =
            JavaDataDescriptor.fromClass(
                _dsfExternal.externalHostClass());
        var _dsfPrev = _dsfFieldDescriptors.put(_dsfName, _dsfDesc);
        if (_dsfPrev != null && !_dsfPrev.value().equals(_dsfDesc.value())) {
          throw new AssertionError(
              "DefSetFunctionsEmitter::emit: descriptor conflict for " + _dsfName);
        }
        _dsfStaticFieldValues.put(_dsfName, _dsfExternal.externalHostValue());
      }
    }
    for (var _dsfFdEntry : _dsfFieldDescriptors.entrySet()) {
      _dsfCw
          .visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              _dsfFdEntry.getKey(),
              _dsfFdEntry.getValue().value(),
              null,
              null)
          .visitEnd();
    }
    _dsfCw
        .visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            EvalRootEmitter.EXPR_CONTEXT_FIELD,
            EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
            null,
            null)
        .visitEnd();
    for (var _dsfSpec : specs) {
      var _efmMethodName =
          DefSetFunctionsEmitter.functionMethodName(_dsfSpec.funcIdx());
      var _efmPrefix = DefSetFunctionsEmitter.functionFieldPrefix(_dsfSpec.funcIdx());
      var _efmArgDescs =
          new TList<JavaDataDescriptor>(_dsfSpec.argSlots().size());
      for (var _efmS : _dsfSpec.argSlots()) _efmArgDescs.add(_efmS.jvmDesc());
      var _efmMv =
          _dsfCw.visitMethod(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              _efmMethodName,
              JavaMethodDescriptor.fromParts(
                      _dsfSpec.retDesc(), _efmArgDescs)
                  .value(),
              null,
              null);
      var _efmTarget =
          new TargetDynamic(new Counter());
      int _efmReservedLocals = 0;
      for (var _efmSlot : _dsfSpec.argSlots()) {
        var _efmSlotDesc = _efmSlot.jvmDesc().value();
        _efmReservedLocals +=
            (_efmSlotDesc.equals("D") || _efmSlotDesc.equals("J")) ? 2 : 1;
      }
      var _efmCtx =
          new EmitCtx(
              _dsfInternalName, Math.max(1, _efmReservedLocals), _efmPrefix);
      var _efmLoc = CodeLoc.Infallible.INSTANCE;
      var _efmSeq = JavaBytecodeUtils.seq();
      var _efmOwnerInternal =
          JavaInternalName.of(_dsfInternalName);
      for (int _efmI = 0; _efmI < _dsfSpec.externals().size(); _efmI++) {
        var _efmEntry = _dsfSpec.externals().get(_efmI);
        var _efmFieldName =
            _efmPrefix + Prefixes.EXTERNAL_PREFIX + _efmI;
        var _efmFieldDesc =
            JavaDataDescriptor.fromClass(
                _efmEntry.external().externalHostClass());
        _efmSeq.add(
            JavaBytecodeUtils.accessStaticField(
                _efmLoc, _efmOwnerInternal, _efmFieldName, _efmFieldDesc));
        _efmSeq.add(
            new CodeDynamic(
                _efmLoc,
                new JavaBytecodeStoreLoad(
                    Opcodes.ASTORE, _efmEntry.key())));
      }
      _efmTarget.emit(_efmSeq, _efmCtx, _dsfSpec.bodyCode());
      int _efmRetOpcode =
          switch (_dsfSpec.retDesc().value()) {
            case "V" -> Opcodes.RETURN;
            case "I", "Z", "B", "S", "C" -> Opcodes.IRETURN;
            case "J" -> Opcodes.LRETURN;
            case "F" -> Opcodes.FRETURN;
            case "D" -> Opcodes.DRETURN;
            default -> Opcodes.ARETURN;
          };
      _efmSeq.add(JavaBytecodeUtils.inst(_efmLoc, _efmRetOpcode));
      var _efmInitialSlots =
          new TList<JavaBytecodeSequence.InitialSlot>();
      for (var _efmSlot : _dsfSpec.argSlots()) {
        var _efmSlotDesc = _efmSlot.jvmDesc().value();
        boolean _efmIsWide = _efmSlotDesc.equals("D") || _efmSlotDesc.equals("J");
        _efmInitialSlots.add(
            new JavaBytecodeSequence.InitialSlot(
                _efmSlot.id().key(), _efmIsWide));
      }
      JavaBytecodeUtils.renderMethodTyped(
          _efmSeq, _efmMv, _efmInitialSlots);
    }
    _dsfCw.visitEnd();
    var _dsfEmitted =
        new DefSetFunctionsEmitter.EmittedFunctions(
            _dsfBinaryName, _dsfInternalName, _dsfCw.toByteArray(), _dsfStaticFieldValues);
    for (var func : contributors) {
      func.compiledClassName = Opt.some(_dsfEmitted.binaryName());
      func.compiledBytecode = Opt.some(_dsfEmitted.bytecode());
      func.compiledStaticFieldValues = Opt.some(_dsfEmitted.staticFieldValues());
      func.functionSpec = Opt.none();
    }
    return Opt.some(_dsfEmitted);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("DefinitionSet is neither a Type nor a Value");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    throw new UnsupportedOperationException("DefinitionSet is neither a Type nor a Value");
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          var set = mustCast(DefinitionSet.class, self);
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  set.artifactIdCell,
                  () -> {
                    var objects = new TList<Artifact>(set.objects.size());
                    for (var dt : set.objects) {
                      var dtConst = new TypeTypeSourceObjectConst(dt);
                      var artRes = dtConst.artifactableExportArtifact(xx, loc);
                      artRes.drainInto(errors);
                      if (artRes.valueOrNull() != null) {
                        objects.add(
                            artRes.requireValue("DefinitionSet::exporterExport object"));
                      }
                    }
                    var functions = new TList<Artifact>(set.functions.size());
                    for (var f : set.functions) {
                      var fConst = new TypeSourceFunctionConst(f);
                      var artRes = fConst.artifactableExportArtifact(xx, loc);
                      artRes.drainInto(errors);
                      if (artRes.valueOrNull() != null) {
                        functions.add(
                            artRes.requireValue("DefinitionSet::exporterExport function"));
                      }
                    }
                    var interfaces = new TList<Artifact>(set.interfaces.size());
                    for (var i : set.interfaces) {
                      var iConst = new TypeSourceInterfaceDynamic(i);
                      var artRes = iConst.artifactableExportArtifact(xx, loc);
                      artRes.drainInto(errors);
                      if (artRes.valueOrNull() != null) {
                        interfaces.add(
                            artRes.requireValue("DefinitionSet::exporterExport interface"));
                      }
                    }
                    var unions = new TList<Artifact>(set.unions.size());
                    for (var u : set.unions) {
                      var uConst = new TypeTypeSourceUnionConst(u);
                      var artRes = uConst.artifactableExportArtifact(xx, loc);
                      artRes.drainInto(errors);
                      if (artRes.valueOrNull() != null) {
                        unions.add(artRes.requireValue("DefinitionSet::exporterExport union"));
                      }
                    }
                    var dependencies = new TList<Artifact>(set.dependencies.size());
                    for (var entry : set.dependencies.entrySet()) {
                      var depRes =
                          ExporterRoot.DEFINITION_SET.artifactExporterExport(
                              xx, loc, entry.getValue());
                      depRes.drainInto(errors);
                      if (depRes.valueOrNull() != null) {
                        dependencies.add(
                            depRes.requireValue("DefinitionSet::exporterExport dependency"));
                      }
                    }
                    var body =
                        new ArtifactDataDefSet(
                            set.defSetId,
                            TList.from(dependencies),
                            TList.from(functions),
                            TList.from(interfaces),
                            TList.from(objects),
                            TList.from(unions));
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (!errors.isEmpty()) {
            return EResult.mixed(artifact, errors);
          }
          return EResult.ok(artifact);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(loc, "DefinitionSet requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSet body;
                try {
                  body = JsonValues.fromValue(ArtifactDataDefSet.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "DefinitionSet data: failed to parse ArtifactDataDefSet: " + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var set = new DefinitionSet(body.defSetId());
                set.artifactIdCell.set(idValue);
                var deps = new TList<Artifact>();
                deps.addAll(body.objects());
                deps.addAll(body.functions());
                deps.addAll(body.interfaces());
                deps.addAll(body.unions());
                deps.addAll(body.dependencies());
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      for (var dtArt : body.objects()) {
                        var any =
                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, dtArt);
                        var v = any.requireValue("DefinitionSet::finalizer object artifact");
                        TypeSourceObjectDynamic dtData;
                        if (v instanceof TypeTypeSourceObjectConst dtConst) {
                          dtData = dtConst.type_();
                        } else if (v
                            instanceof TypeTypeSourceObjectConst.ImportedSourceObjectConst impDt) {
                          dtData = impDt.type_();
                        } else {
                          throw new IllegalStateException(
                              "def-set object artifact had wrong type: " + v.getClass());
                        }
                        dtData.objIdx = set.objects.size();
                        set.objects.add(dtData);
                      }
                      for (var fArt : body.functions()) {
                        var any =
                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, fArt);
                        var v = any.requireValue("DefinitionSet::finalizer function artifact");
                        SourceFunctionData fData;
                        if (v instanceof TypeSourceFunctionConst fConst) {
                          fData = fConst.data();
                        } else if (v
                            instanceof TypeSourceFunctionConst.ImportedSourceFunctionConst
                                impFn) {
                          fData = impFn.data();
                        } else {
                          throw new IllegalStateException(
                              "def-set function artifact had wrong type: " + v.getClass());
                        }
                        fData.funcIdx = set.functions.size();
                        set.functions.add(fData);
                      }
                      for (var iArt : body.interfaces()) {
                        var any =
                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, iArt);
                        var v = any.requireValue("DefinitionSet::finalizer interface artifact");
                        InterfaceData iData;
                        if (v instanceof TypeSourceInterfaceDynamic iConst) {
                          iData = iConst.data();
                        } else if (v
                            instanceof TypeSourceInterfaceDynamic.ImportedInterfaceConst impIc) {
                          iData = impIc.data();
                        } else {
                          throw new IllegalStateException(
                              "def-set interface artifact had wrong type: " + v.getClass());
                        }
                        set.interfaces.add(iData);
                      }
                      for (var uArt : body.unions()) {
                        var any =
                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, uArt);
                        var v = any.requireValue("DefinitionSet::finalizer union artifact");
                        TypeSourceUnionDynamic uData;
                        if (v instanceof TypeTypeSourceUnionConst uConst) {
                          uData = uConst.type_();
                        } else if (v
                            instanceof TypeTypeSourceUnionConst.ImportedSourceUnionConst impU) {
                          uData = impU.type_();
                        } else {
                          throw new IllegalStateException(
                              "def-set union artifact had wrong type: " + v.getClass());
                        }
                        set.unions.add(uData);
                      }
                      for (var dep : body.dependencies()) {
                        var any =
                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, dep);
                        var v = any.requireValue("DefinitionSet::finalizer dependency artifact");
                        var depSet = mustCast(DefinitionSet.class, v);
                        set.dependencies.putIfAbsent(depSet.defSetId, depSet);
                      }
                      set.finished = true;
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(set, deps, fin));
              });
        }
      };
}
