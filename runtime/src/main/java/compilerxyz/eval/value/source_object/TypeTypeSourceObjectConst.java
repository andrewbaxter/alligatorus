package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
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
import compilerxyz.gen.Value;
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
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import compilerxyz.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.source_function.ExternalCodec;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.eval.TypeId;

@compilerxyz.annotations.BuiltinTypeId(value = 7045L, name = "type_source_object_const")
public record TypeTypeSourceObjectConst(TypeSourceObjectDynamic type_)
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
  public TypeId hasTypeIdTypeId() {
    return new TypeId.DefsetMember(type_.set.defSetId, uniqueId(type_.identity));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    var d = type_.description;
    if (d != null) return d;
    return "object(" + (type_.name != null ? type_.name : "?") + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeTypeSourceObjectConst.class, compilerxyz.gen.GenTypeIds.TYPE_SOURCE_OBJECT_CONST_TYPE_ID, "type_source_object_const", compilerxyz.artifact.ExporterRoot.SOURCE_OBJECT_CONST);

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(type_.identity)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    for (var impl : type_.interfaceImpls) {
      if (key.equals(impl.interface_.identity)) {
        return Opt.some(this);
      }
    }
    if (key.equals(AspectType.ID)) {
      for (var impl : type_.interfaceImpls) {
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
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeTypeSourceObjectConst oc)) {
            return EResult.err(
                loc,
                "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: expected TypeTypeSourceObjectConst, got "
                    + self.getClass());
          }
          var dt = oc.type_;
          var set = dt.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                    + (dt.name == null ? "(unnamed)" : dt.name)
                    + "' is not attached to a definition set");
          }
          var underlying = dt.underlyingType;
          if (underlying == null) {
            return EResult.err(
                loc,
                "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                    + (dt.name == null ? "(unnamed)" : dt.name)
                    + "' missing underlying type");
          }
          var errors = new TList<ExprErr>();
          var underlyingFinal = underlying;
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  dt.artifactIdCell,
                  () -> {
                    var defSetArtRes =
                        ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
                    defSetArtRes.drainInto(errors);
                    var defSetArt =
                        defSetArtRes.valueOrNull() != null
                            ? defSetArtRes.requireValue(
                                "TypeTypeSourceObjectConst::exporterExport defSet")
                            : nullArtifact();
                    var underlyingArtRes =
                        ArtifactSupport.exportArtifact(xx, loc, underlyingFinal);
                    underlyingArtRes.drainInto(errors);
                    var underlyingArt =
                        underlyingArtRes.valueOrNull() != null
                            ? underlyingArtRes.requireValue(
                                "TypeTypeSourceObjectConst::exporterExport underlying")
                            : nullArtifact();
                    var methods = new TList<ArtifactDataDefSetMethod>();
                    for (var m : dt.methods) {
                      var argT = m.argType;
                      var retT = m.retType;
                      if (argT == null || retT == null) {
                        errors.add(
                            new compilerxyz.eval.ExprErr(
                                loc,
                                compilerxyz.util.TList.of(
                                    new compilerxyz.eval.ExprSubErr.Opaque(
                                        "TypeTypeSourceObjectConst::exporterExport: object '"
                                            + (dt.name == null ? "(unnamed)" : dt.name)
                                            + "' method '"
                                            + m.name
                                            + "' missing arg/ret type"))));
                        continue;
                      }
                      if (m.compiledBodyCode == null && m.compiledClassName == null) {
                        var objectIndex = dt.set.objects.indexOf(dt);
                        var objInternalName =
                            compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(
                                dt.setId, objectIndex);
                        var finishRes =
                            m.finish(
                                xx,
                                loc,
                                dt.name == null ? "(unnamed)" : dt.name,
                                dt,
                                underlyingFinal,
                                objInternalName);
                        finishRes.drainInto(errors);
                      }
                      if (m.compiledBodyCode == null && m.compiledClassName == null) {
                        errors.add(
                            new compilerxyz.eval.ExprErr(
                                loc,
                                compilerxyz.util.TList.of(
                                    new compilerxyz.eval.ExprSubErr.Opaque(
                                        "TypeTypeSourceObjectConst::exporterExport: object method '"
                                            + m.name
                                            + "' has not been compiled"))));
                        continue;
                      }
                      var argArtRes = ArtifactSupport.exportArtifact(xx, loc, argT);
                      argArtRes.drainInto(errors);
                      var argArt =
                          argArtRes.valueOrNull() != null
                              ? argArtRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method arg")
                              : nullArtifact();
                      var retArtRes = ArtifactSupport.exportArtifact(xx, loc, retT);
                      retArtRes.drainInto(errors);
                      var retArt =
                          retArtRes.valueOrNull() != null
                              ? retArtRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method ret")
                              : nullArtifact();
                      var extRes = ExternalCodec.exportExternals(xx, loc, m.capturedExternals);
                      extRes.drainInto(errors);
                      var ext =
                          extRes.valueOrNull() != null
                              ? extRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method externals")
                              : compilerxyz.util.TList.<ArtifactExternal>of();
                      methods.add(
                          new ArtifactDataDefSetMethod(
                              m.name,
                              argArt,
                              retArt,
                              m.compiledClassName,
                              m.compiledBytecode,
                              ext));
                    }
                    var ifaceImpls = new TList<ArtifactDataDefSetInterfaceImpl>();
                    for (var impl : dt.interfaceImpls) {
                      BuiltinInterfaceKind kind = impl.interface_.builtinKind;
                      Artifact userIface = null;
                      if (kind == null) {
                        var ifaceConst = new TypeSourceInterfaceConst(impl.interface_);
                        var ifaceArtRes = ArtifactSupport.exportArtifact(xx, loc, ifaceConst);
                        ifaceArtRes.drainInto(errors);
                        userIface =
                            ifaceArtRes.valueOrNull() != null
                                ? ifaceArtRes.requireValue(
                                    "TypeTypeSourceObjectConst::exporterExport user iface")
                                : nullArtifact();
                      }
                      var methodImpls = new TList<ArtifactDataDefSetInterfaceMethodImpl>();
                      for (var mi : impl.methodImpls) {
                        var iface = impl.interface_;
                        if (mi.compiledBodyCode == null && mi.compiledClassName == null) {
                          var finishRes = mi.finish(xx, loc, iface, dt, underlyingFinal);
                          finishRes.drainInto(errors);
                        }
                        if (mi.compiledBodyCode == null && mi.compiledClassName == null) {
                          errors.add(
                              new compilerxyz.eval.ExprErr(
                                  loc,
                                  compilerxyz.util.TList.of(
                                      new compilerxyz.eval.ExprSubErr.Opaque(
                                          "TypeTypeSourceObjectConst::exporterExport: iface method impl (idx="
                                              + mi.interfaceMethodIndex
                                              + ") has not been compiled"))));
                          continue;
                        }
                        var extRes =
                            ExternalCodec.exportExternals(xx, loc, mi.capturedExternals);
                        extRes.drainInto(errors);
                        var ext =
                            extRes.valueOrNull() != null
                                ? extRes.requireValue(
                                    "TypeTypeSourceObjectConst::exporterExport iface method ext")
                                : compilerxyz.util.TList.<ArtifactExternal>of();
                        methodImpls.add(
                            new ArtifactDataDefSetInterfaceMethodImpl(
                                mi.interfaceMethodIndex,
                                mi.compiledClassName,
                                mi.compiledBytecode,
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
                            underlyingArt,
                            methods,
                            ifaceImpls,
                            dt.compiledBinaryName,
                            dt.compiledBytecode);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
          return new Artifact.Body(
              new compilerxyz.artifact.ArtifactBody(
                  new compilerxyz.safejson.JsonValueNull(),
                  new compilerxyz.artifact.ArtifactBodyExporter.Root()));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(loc, "TypeTypeSourceObjectConst requires an artifact id to deserialize");
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
                      "TypeTypeSourceObjectConst type_: failed to parse "
                          + "ArtifactDataDefSetObject: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var dt = new TypeSourceObjectDynamic(
                    body.defSetId(), body.name(), body.identity());
                dt.artifactIdCell.set(idValue);
                dt.compiledBinaryName = body.compiledBinaryName();
                dt.compiledBytecode = body.compiledBytecode();
                for (var m : body.methods()) {
                  var md = new SourceDataMethodData(m.name());
                  md.methodIdx = dt.methods.size();
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
                    switch (ce) {
                      case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                      case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                      case ArtifactExternal.StagedExpr ignored -> {}
                      case ArtifactExternal.Unique ignored -> {}
                    }
                  }
                }
                for (var impl : body.interfaceImpls()) {
                  if (impl.userIface() != null) {
                    deps.add(impl.userIface());
                  }
                  for (var mi : impl.methodImpls()) {
                    for (var ce : mi.capturedExternals()) {
                      switch (ce) {
                        case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                        case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                        case ArtifactExternal.StagedExpr ignored -> {}
                        case ArtifactExternal.Unique ignored -> {}
                      }
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
                              "TypeTypeSourceObjectConst::finalizer defSet artifact");
                      var set = compilerxyz.util.MustCast.mustCast(DefinitionSet.class, defSetVal);
                      dt.initSetReflectively(set);
                      dt.setId = set.defSetId;
                      var underlyingRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.underlyingType());
                      var underlyingVal =
                          underlyingRes.requireValue(
                              "TypeTypeSourceObjectConst::finalizer underlying type");
                      dt.underlyingType = underlyingVal;
                      var bodyMethodsIt = body.methods().iterator();
                      for (var md : dt.methods) {
                        var sm = bodyMethodsIt.next();
                        var argRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.argType());
                        var argVal =
                            argRes.requireValue(
                                "TypeTypeSourceObjectConst::finalizer method arg type");
                        md.argType = argVal;
                        var retRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.retType());
                        var retVal =
                            retRes.requireValue(
                                "TypeTypeSourceObjectConst::finalizer method ret type");
                        md.retType = retVal;
                        var extRes =
                            ExternalCodec.importExternals(
                                xx2, loc2, sm.capturedExternals());
                        var extVal =
                            extRes.requireValue(
                                "TypeTypeSourceObjectConst::finalizer method externals");
                        md.capturedExternals.addAll(extVal);
                      }
                      for (var impl : body.interfaceImpls()) {
                        InterfaceData iface;
                        if (impl.builtinKind() != null) {
                          iface =
                              BuiltinInterfaceRegistry
                                  .resolve(xx2.vx.ix.importState, impl.builtinKind());
                        } else {
                          var ifaceArt = impl.userIface();
                          if (ifaceArt == null) {
                            throw new IllegalStateException(
                                "TypeTypeSourceObjectConst finalizer: non-builtin iface impl missing "
                                    + "user_iface artifact");
                          }
                          var ifaceAny =
                              ArtifactSupport.independentFinalizeImportArtifact(
                                  xx2, loc2, ifaceArt);
                          var ifaceVal =
                              ifaceAny.requireValue(
                                  "TypeTypeSourceObjectConst::finalizer user iface artifact");
                          if (!(ifaceVal instanceof TypeSourceInterfaceConst ic)) {
                            throw new IllegalStateException(
                                "TypeTypeSourceObjectConst finalizer: user iface artifact resolved to "
                                    + ifaceVal.getClass());
                          }
                          iface = ic.data();
                        }
                        var implData = new SourceObjectInterfaceImplData(iface);
                        implData.implIdx = dt.interfaceImpls.size();
                        for (var mi : impl.methodImpls()) {
                          var mid = new SourceObjectInterfaceMethodImplData(
                              mi.interfaceMethodIndex());
                          mid.methodImplIdx = implData.methodImpls.size();
                          mid.compiledClassName = mi.compiledClassName();
                          mid.compiledBytecode = mi.compiledBytecode();
                          var extRes =
                              ExternalCodec.importExternals(
                                  xx2, loc2, mi.capturedExternals());
                          var extVal =
                              extRes.requireValue(
                                  "TypeTypeSourceObjectConst::finalizer iface method externals");
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

  private static InterfaceData builtinInterfaceOf(
      GenBuiltinInterfaces ifaces, BuiltinInterfaceKind kind) {
    return switch (kind) {
      case VALUE -> ifaces.value;
      case TYPE -> ifaces.type_;
      case ASPECT_FUNCTION -> ifaces.aspect_function;
      case ASPECT_FIELD -> ifaces.aspect_field;
      case ASPECT_DROP_DYNAMIC -> ifaces.aspect_drop_dynamic;
      case ASPECT_CONSUME_DYNAMIC -> ifaces.aspect_consume_dynamic;
      case ASPECT_BIND_DYNAMIC -> ifaces.aspect_bind_dynamic;
      case BINDING -> ifaces.binding;
      case TARGET -> ifaces.target;
      case CODE_DYN -> ifaces.code_dyn;
    };
  }

  public record ImportedSourceObjectConst(TypeSourceObjectDynamic type_) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeTypeSourceObjectConst(type_));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new TypeTypeSourceObjectConst(type_));
    }
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst instance)
        || !(instance.data() instanceof Value)) {
      return EResult.err(
          loc,
          "TypeTypeSourceObjectConst::toDynamic: expected source-object ValueObjectConst, got "
              + (cv.value() instanceof Opt.Some<Value> s
                  ? s.value().getClass().getName()
                  : "no value"));
    }
    Type underlyingType = type_.underlyingType;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeTypeSourceObjectConst::toDynamic: object underlying type not finalised");
    }
    var erb = new Erb(loc);
    var rawUnderlyingCv =
        new CodeValue(Code.none(loc), Opt.some((Value) instance.data()));
    var convertedOpt =
        erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, rawUnderlyingCv));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var consumeRes = compilerxyz.gen.AspectConsumeDynamic.get(xx, loc, underlyingType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var underlyingConsume =
        consumeRes.requireValue("TypeTypeSourceObjectConst::toDynamic consume");
    var underlyingJsRes = underlyingConsume.aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs = underlyingJsRes.requireValue("TypeTypeSourceObjectConst::toDynamic toDynamic");

    var set = type_.set;
    if (set == null) {
      return EResult.err(
          loc, "TypeTypeSourceObjectConst::toDynamic: object has no definition set reference");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    var objectIndex = set.objects.indexOf(type_);
    if (objectIndex < 0) {
      return EResult.err(
          loc,
          "TypeTypeSourceObjectConst::toDynamic: object '"
              + (type_.name == null ? "(unnamed)" : type_.name)
              + "' not registered in its owning def-set");
    }
    var objInternalName =
        SourceObjectEmitter.internalNameFor(
            set.defSetId, objectIndex);
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(objInternalName);
    var underlyingDesc = underlyingConsume.aspectConsumeDynamicJvmClass(xx);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            TList.of(underlyingDesc));
    var result =
        compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
    if (erb.errors().isEmpty()) {
      return EResult.ok(result);
    }
    return EResult.mixed(result, erb.errors());
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    TypeSourceObjectDynamic dynType = type_;
    if (!self.code().isNone()) {
      return dynType.aspectFieldField(xx, loc, self, key);
    }
    var selfCodeRes = aspectConsumeDynamicToDynamic(xx, loc, self);
    if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
    var selfCode = selfCodeRes.requireValue("TypeTypeSourceObjectConst::field toDynamic");
    var selfDynamic =
        new CodeValue(
            Code.of(loc, selfCode),
            Opt.some(ValueDynamic.stack(xx, dynType)));
    return dynType.aspectFieldField(xx, loc, selfDynamic, key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeTypeSourceObjectConst(type_));
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (!(value instanceof compilerxyz.eval.value.object_.ValueObjectConst inst)
        || !(inst.type() instanceof TypeTypeSourceObjectConst)) {
      throw new IllegalStateException(
          "TypeTypeSourceObjectConst::type_: expected source-object ValueObjectConst, got "
              + (value == null ? "null" : value.getClass().getName()));
    }
    var set = type_.set;
    if (set == null) {
      throw new IllegalStateException(
          "TypeTypeSourceObjectConst::type_: object '"
              + (type_.name == null ? "(unnamed)" : type_.name)
              + "' has no definition set reference");
    }
    var binaryName = type_.compiledBinaryName;
    if (binaryName == null) {
      throw new IllegalStateException(
          "TypeTypeSourceObjectConst::type_: object '"
              + (type_.name == null ? "(unnamed)" : type_.name)
              + "' has not been compiled (compiledBinaryName == null)");
    }
    var loader =
        DefinitionSetClassLoader.SHARED_CACHE.getIfPresent(set.defSetId);
    if (loader == null) {
      throw new IllegalStateException(
          "TypeTypeSourceObjectConst::type_: def-set "
              + set.defSetId
              + " has no installed class loader (call ImportInstall.installDefSetToDynamic first)");
    }
    Class<?> cls;
    try {
      cls = Class.forName(binaryName, true, loader);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "TypeTypeSourceObjectConst::type_: generated class " + binaryName + " failed to load", e);
    }
    Object instance;
    try {
      instance = cls.getDeclaredConstructor(Object.class).newInstance(inst.data());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "TypeTypeSourceObjectConst::type_: generated class "
              + binaryName
              + " failed to instantiate",
          e);
    }
    if (!(instance instanceof Type t)) {
      throw new IllegalStateException(
          "TypeTypeSourceObjectConst::type_: generated class "
              + binaryName
              + " does not implement Type — object must declare a Type builtin-interface impl");
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
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
