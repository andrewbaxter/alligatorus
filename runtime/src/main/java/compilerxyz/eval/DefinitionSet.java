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
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.value.source_function.SourceFunctionData;
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
    var emitted = DefSetFunctionsEmitter.emit(defSetId, specs);
    for (var func : contributors) {
      func.compiledClassName = Opt.some(emitted.binaryName());
      func.compiledBytecode = Opt.some(emitted.bytecode());
      func.compiledStaticFieldValues = Opt.some(emitted.staticFieldValues());
      func.functionSpec = Opt.none();
    }
    return Opt.some(emitted);
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
