package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.Code;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.SourceInterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import compilerxyz.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.source_function.ExternalCodec;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.eval.TypeId;

public record TypeSourceObjectConst(SourceObjectData data)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectType,
        ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.DefsetMember(data.set.defSetId, uniqueId(data.identity));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "object(" + (data.name != null ? data.name : "?") + ")";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceObjectConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(data.identity)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    for (var impl : data.interfaceImpls) {
      if (key.equals(impl.interface_.identity)) {
        return Opt.some(this);
      }
    }
    if (key.equals(AspectType.ID)) {
      for (var impl : data.interfaceImpls) {
        if (impl.interface_.builtinKind
            == BuiltinInterfaceKind.TYPE) {
          return Opt.some(this);
        }
      }
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
  }

  private static final Map<SourceObjectData, ArtifactSupport.ArtifactIdHolder> INDEPENDENT_CELLS =
      java.util.Collections.synchronizedMap(new IdentityHashMap<>());

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeSourceObjectConst oc)) {
            return EResult.err(
                loc,
                "TypeSourceObjectConst::exporterRootDelegateSelfExport: expected TypeSourceObjectConst, got "
                    + self.getClass());
          }
          var dt = oc.data;
          var set = dt.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                    + (dt.name == null ? "(unnamed)" : dt.name)
                    + "' is not attached to a definition set");
          }
          var underlying = dt.underlyingType;
          if (underlying == null) {
            return EResult.err(
                loc,
                "TypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                    + (dt.name == null ? "(unnamed)" : dt.name)
                    + "' missing underlying type");
          }
          var errors = new TList<ExprErr>();
          var defSetArtRes = ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
          if (defSetArtRes.hasErrors()) {
            defSetArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var defSetArt =
              defSetArtRes.requireValue("TypeSourceObjectConst::exporterExport defSet artifact");
          var underlyingArtRes = ArtifactSupport.exportArtifact(xx, loc, underlying);
          if (underlyingArtRes.hasErrors()) {
            underlyingArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var underlyingArt =
              underlyingArtRes.requireValue(
                  "TypeSourceObjectConst::exporterExport underlying artifact");
          var methods = new TList<ArtifactDataDefSetMethod>();
          for (var m : dt.methods) {
            var argT = m.argType;
            var retT = m.retType;
            if (argT == null || retT == null) {
              return EResult.err(
                  loc,
                  "TypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                      + (dt.name == null ? "(unnamed)" : dt.name)
                      + "' method '"
                      + m.name
                      + "' missing arg/ret type");
            }
            if (m.compiledBodyCode == null && m.compiledClassName == null) {
              var finishRes = m.finish(xx, loc, dt.name == null ? "(unnamed)" : dt.name,
                  underlying, dt.dataField);
              if (finishRes.hasErrors()) {
                finishRes.drainInto(errors);
                return EResult.errs(errors);
              }
            }
            if (m.compiledBodyCode == null && m.compiledClassName == null) {
              return EResult.err(
                  loc,
                  "TypeSourceObjectConst::exporterRootDelegateSelfExport: object method '"
                      + m.name
                      + "' has not been compiled");
            }
            var mBinding = m.bindingId;
            if (!(mBinding instanceof BindingId.Def def)) {
              return EResult.err(
                  loc,
                  "TypeSourceObjectConst::exporterRootDelegateSelfExport: object method '"
                      + m.name
                      + "' has no BindingId.Def (got "
                      + mBinding
                      + ")");
            }
            var argArtRes = ArtifactSupport.exportArtifact(xx, loc, argT);
            if (argArtRes.hasErrors()) {
              argArtRes.drainInto(errors);
              return EResult.errs(errors);
            }
            var argArt =
                argArtRes.requireValue("TypeSourceObjectConst::exporterExport method arg artifact");
            var retArtRes = ArtifactSupport.exportArtifact(xx, loc, retT);
            if (retArtRes.hasErrors()) {
              retArtRes.drainInto(errors);
              return EResult.errs(errors);
            }
            var retArt =
                retArtRes.requireValue("TypeSourceObjectConst::exporterExport method ret artifact");
            var extRes =
                ExternalCodec.exportExternals(
                    xx, loc, m.capturedExternals);
            if (extRes.hasErrors()) {
              extRes.drainInto(errors);
              return EResult.errs(errors);
            }
            var ext =
                extRes.requireValue("TypeSourceObjectConst::exporterExport method externals");
            methods.add(
                new ArtifactDataDefSetMethod(
                    m.name,
                    m.fieldName,
                    argArt,
                    retArt,
                    m.compiledClassName,
                    m.compiledBytecode,
                    def.index(),
                    ext));
          }
          var ifaceImpls = new TList<ArtifactDataDefSetInterfaceImpl>();
          for (var impl : dt.interfaceImpls) {
            BuiltinInterfaceKind kind = impl.interface_.builtinKind;
            Artifact userIface = null;
            if (kind == null) {
              var ifaceConst = new TypeSourceInterfaceConst(impl.interface_);
              var ifaceArtRes = ArtifactSupport.exportArtifact(xx, loc, ifaceConst);
              if (ifaceArtRes.hasErrors()) {
                ifaceArtRes.drainInto(errors);
                return EResult.errs(errors);
              }
              userIface =
                  ifaceArtRes.requireValue(
                      "TypeSourceObjectConst::exporterExport user interface artifact");
            }
            var methodImpls = new TList<ArtifactDataDefSetInterfaceMethodImpl>();
            for (var mi : impl.methodImpls) {
              var iface = impl.interface_;
              if (mi.compiledBodyCode == null && mi.compiledClassName == null) {
                var finishRes = mi.finish(xx, loc, iface, dt, underlying, dt.dataField);
                if (finishRes.hasErrors()) {
                  finishRes.drainInto(errors);
                  return EResult.errs(errors);
                }
              }
              if (mi.compiledBodyCode == null && mi.compiledClassName == null) {
                return EResult.err(
                    loc,
                    "TypeSourceObjectConst::exporterRootDelegateSelfExport: object interface method impl (idx="
                        + mi.interfaceMethodIndex
                        + ") has not been compiled");
              }
              var miBinding = mi.bindingId;
              if (!(miBinding instanceof BindingId.Def miDef)) {
                return EResult.err(
                    loc,
                    "TypeSourceObjectConst::exporterRootDelegateSelfExport: object interface method impl has no "
                        + "BindingId.Def (got "
                        + miBinding
                        + ")");
              }
              var extRes =
                  ExternalCodec.exportExternals(
                      xx, loc, mi.capturedExternals);
              if (extRes.hasErrors()) {
                extRes.drainInto(errors);
                return EResult.errs(errors);
              }
              var ext =
                  extRes.requireValue(
                      "TypeSourceObjectConst::exporterExport interface method externals");
              methodImpls.add(
                  new ArtifactDataDefSetInterfaceMethodImpl(
                      mi.interfaceMethodIndex,
                      mi.compiledClassName,
                      mi.compiledBytecode,
                      miDef.index(),
                      ext));
            }
            ifaceImpls.add(
                new ArtifactDataDefSetInterfaceImpl(kind, userIface, methodImpls));
          }
          var body =
              new ArtifactDataDefSetObject(
                  defSetArt,
                  set.defSetId,
                  dt.name,
                  dt.identity,
                  dt.dataField,
                  underlyingArt,
                  methods,
                  ifaceImpls,
                  dt.compiledBinaryName,
                  dt.compiledBytecode);
          var cell =
              INDEPENDENT_CELLS.computeIfAbsent(dt, k -> new ArtifactSupport.ArtifactIdHolder());
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts, cell, () -> makeBody.apply(JsonValues.toValue(body)));
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(loc, "TypeSourceObjectConst requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSetObject body;
                try {
                  body = JsonValues.fromValue(ArtifactDataDefSetObject.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeSourceObjectConst data: failed to parse "
                          + "ArtifactDataDefSetObject: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var dt = new SourceObjectData(
                    body.defSetId(), body.name(), body.identity(), body.dataField());
                dt.artifactId = idValue;
                dt.compiledBinaryName = body.compiledBinaryName();
                dt.compiledBytecode = body.compiledBytecode();
                for (var m : body.methods()) {
                  var md = new SourceDataMethodData(m.name(), m.fieldName());
                  md.bindingId = new BindingId.Def(body.defSetId(), m.entryId());
                  md.compiledClassName = m.compiledClassName();
                  md.compiledBytecode = m.compiledBytecode();
                  dt.methods.add(md);
                }
                for (var impl : body.interfaceImpls()) {

                }
                var imported = new ImportedSourceObjectConst(dt);
                var deps = new TList<Artifact>();
                deps.add(body.defSet());
                deps.add(body.underlyingType());
                for (var m : body.methods()) {
                  deps.add(m.argType());
                  deps.add(m.retType());
                  for (var ce : m.capturedExternals()) {
                    deps.add(ce.typeArtifact());
                    deps.add(ce.valueArtifact());
                  }
                }
                for (var impl : body.interfaceImpls()) {
                  if (impl.userIface() != null) {
                    deps.add(impl.userIface());
                  }
                  for (var mi : impl.methodImpls()) {
                    for (var ce : mi.capturedExternals()) {
                      deps.add(ce.typeArtifact());
                      deps.add(ce.valueArtifact());
                    }
                  }
                }
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var defSetAny =
                          ArtifactSupport.independentFinalizeImportArtifact(
                              xx2, loc2, body.defSet());
                      var defSetVal =
                          defSetAny.requireValue(
                              "TypeSourceObjectConst::finalizer defSet artifact");
                      if (!(defSetVal instanceof DefinitionSet set)) {
                        throw new IllegalStateException(
                            "TypeSourceObjectConst finalizer: def-set artifact resolved to "
                                + defSetVal.getClass()
                                + " (expected DefinitionSet)");
                      }
                      dt.initSetReflectively(set);
                      dt.setId = set.defSetId;
                      var underlyingRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.underlyingType());
                      var underlyingVal =
                          underlyingRes.requireValue(
                              "TypeSourceObjectConst::finalizer underlying type");
                      dt.underlyingType = underlyingVal;
                      var bodyMethodsIt = body.methods().iterator();
                      for (var md : dt.methods) {
                        var sm = bodyMethodsIt.next();
                        var argRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.argType());
                        var argVal =
                            argRes.requireValue(
                                "TypeSourceObjectConst::finalizer method arg type");
                        md.argType = argVal;
                        var retRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.retType());
                        var retVal =
                            retRes.requireValue(
                                "TypeSourceObjectConst::finalizer method ret type");
                        md.retType = retVal;
                        var extRes =
                            ExternalCodec.importExternals(
                                xx2, loc2, sm.capturedExternals());
                        var extVal =
                            extRes.requireValue(
                                "TypeSourceObjectConst::finalizer method externals");
                        md.capturedExternals.addAll(extVal);
                      }
                      for (var impl : body.interfaceImpls()) {
                        SourceInterfaceData iface;
                        if (impl.builtinKind() != null) {
                          iface =
                              BuiltinInterfaceRegistry
                                  .resolve(xx2.vx.ix.importState, impl.builtinKind());
                        } else {
                          var ifaceArt = impl.userIface();
                          if (ifaceArt == null) {
                            throw new IllegalStateException(
                                "TypeSourceObjectConst finalizer: non-builtin iface impl missing "
                                    + "user_iface artifact");
                          }
                          var ifaceAny =
                              ArtifactSupport.independentFinalizeImportArtifact(
                                  xx2, loc2, ifaceArt);
                          var ifaceVal =
                              ifaceAny.requireValue(
                                  "TypeSourceObjectConst::finalizer user iface artifact");
                          if (!(ifaceVal instanceof TypeSourceInterfaceConst ic)) {
                            throw new IllegalStateException(
                                "TypeSourceObjectConst finalizer: user iface artifact resolved to "
                                    + ifaceVal.getClass());
                          }
                          iface = ic.data();
                        }
                        var implData = new SourceObjectInterfaceImplData(iface);
                        for (var mi : impl.methodImpls()) {
                          var mid = new SourceObjectInterfaceMethodImplData(
                              mi.interfaceMethodIndex());
                          mid.bindingId =
                              new BindingId.Def(body.defSetId(), mi.entryId());
                          mid.compiledClassName = mi.compiledClassName();
                          mid.compiledBytecode = mi.compiledBytecode();
                          var extRes =
                              ExternalCodec.importExternals(
                                  xx2, loc2, mi.capturedExternals());
                          var extVal =
                              extRes.requireValue(
                                  "TypeSourceObjectConst::finalizer iface method externals");
                          mid.capturedExternals.addAll(extVal);
                          implData.methodImpls.add(mid);
                        }
                        dt.interfaceImpls.add(implData);
                      }
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(imported, deps, fin));
              });
        }
      };

  private static SourceInterfaceData builtinInterfaceOf(
      GenBuiltinInterfaces ifaces, BuiltinInterfaceKind kind) {
    return switch (kind) {
      case VALUE -> ifaces.value;
      case TYPE -> ifaces.type_;
      case ASPECT_FUNCTION -> ifaces.aspect_function;
      case ASPECT_FIELD -> ifaces.aspect_field;
      case ASPECT_DROP_DYNAMIC -> ifaces.aspect_drop_dynamic;
      case BINDING -> ifaces.binding;
      case TARGET -> ifaces.target;
      case CODE_DYN -> ifaces.code_dyn;
    };
  }

  public record ImportedSourceObjectConst(SourceObjectData data) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeSourceObjectConst(data));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new TypeSourceObjectConst(data));
    }
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof ValueSourceObjectConst instance)) {
      return EResult.err(
          loc,
          "TypeSourceObjectConst::toDynamic: expected ValueSourceObjectConst, got "
              + (cv.value() instanceof Opt.Some<Value> s
                  ? s.value().getClass().getName()
                  : "no value"));
    }
    Type underlyingType = data.underlyingType;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceObjectConst::toDynamic: object underlying type not finalised");
    }
    var erb = new Erb(loc);
    var rawUnderlyingCv =
        new CodeValue(Code.none(loc), Opt.some(instance.underlying()));
    var convertedOpt =
        erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, rawUnderlyingCv));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var toDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, underlyingType));
    if (toDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var underlyingJsOpt = erb.mustEr(toDynamicOpt.get().toDynamic(xx, loc, converted));
    if (underlyingJsOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var underlyingJs = underlyingJsOpt.get();

    var set = data.set;
    if (set == null) {
      return EResult.err(
          loc, "TypeSourceObjectConst::toDynamic: object has no definition set reference");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    var objectIndex = set.objects.indexOf(data);
    if (objectIndex < 0) {
      return EResult.err(
          loc,
          "TypeSourceObjectConst::toDynamic: object '"
              + (data.name == null ? "(unnamed)" : data.name)
              + "' not registered in its owning def-set");
    }
    var objInternalName =
        SourceObjectEmitter.internalNameFor(
            set.defSetId, objectIndex);
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(objInternalName);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            TList.of(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT));
    var result =
        compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
    if (erb.errors().isEmpty()) {
      return EResult.ok(result);
    }
    return EResult.mixed(result, erb.errors());
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var dynType = new TypeSourceObjectDynamic(data);
    if (!self.code().isNone()) {
      return dynType.field(xx, loc, self, key);
    }
    var selfCodeRes = toDynamic(xx, loc, self);
    if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
    var selfCode = selfCodeRes.requireValue("TypeSourceObjectConst::field toDynamic");
    var selfDynamic =
        new CodeValue(
            Code.of(loc, selfCode),
            Opt.some(ValueDynamic.stack(dynType)));
    return dynType.field(xx, loc, selfDynamic, key);
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeSourceObjectConst(data));
  }

  @Override
  public Type type_(Value value) {
    if (!(value instanceof ValueSourceObjectConst inst)) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: expected ValueSourceObjectConst, got "
              + (value == null ? "null" : value.getClass().getName()));
    }
    var set = data.set;
    if (set == null) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: object '"
              + (data.name == null ? "(unnamed)" : data.name)
              + "' has no definition set reference");
    }
    var binaryName = data.compiledBinaryName;
    if (binaryName == null) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: object '"
              + (data.name == null ? "(unnamed)" : data.name)
              + "' has not been compiled (compiledBinaryName == null)");
    }
    var loader =
        DefinitionSetClassLoader.SHARED_CACHE.getIfPresent(set.defSetId);
    if (loader == null) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: def-set "
              + set.defSetId
              + " has no installed class loader (call ImportInstall.installDefSetToDynamic first)");
    }
    Class<?> cls;
    try {
      cls = Class.forName(binaryName, true, loader);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "TypeSourceObjectConst::type_: generated class " + binaryName + " failed to load", e);
    }
    Object instance;
    try {
      instance = cls.getDeclaredConstructor(Object.class).newInstance(inst.underlying());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "TypeSourceObjectConst::type_: generated class "
              + binaryName
              + " failed to instantiate",
          e);
    }
    if (!(instance instanceof Type t)) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: generated class "
              + binaryName
              + " does not implement Type — object must declare a Type builtin-interface impl"
              + " (see SourceObjectEmitter.builtinHostInterfaceInternalName)");
    }
    return t;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
