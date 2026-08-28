package compilerxyz.eval.value.source_object;




import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter.Root;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr.Opaque;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_function.ExternalCodec;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactDataDefSetObjectCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.MustCast;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6138L)
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
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return type_.description.optUnwrapOr("object(" + type_.name.optUnwrapOr("?") + ")");
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(type_.identity)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    for (var impl : type_.interfaceImpls) {
      if (key.equals(impl.interface_.identity)) {
        return Opt.some(this);
      }
      var implAspectId = impl.interface_.aspectId;
      if (impl.interface_.isAspect && implAspectId.optIsSome() && key.equals(implAspectId.optUnwrap())) {
        return Opt.some(this);
      }
    }
    if (key.equals(GenAspectType.AspectId)) {
      for (var impl : type_.interfaceImpls) {
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.TYPE) {
          return Opt.some(this);
        }
      }
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    // TTOSC is now used only as a type-descriptor (source-object VALUES wrap in TSOC).
    // Route to SOURCE_OBJECT_CONST which handles the bare-TTOSC delegate shape.
    return ExporterRoot.SOURCE_OBJECT_CONST;
  }

  /**
   * Bare-TTOSC-as-type-descriptor export: route to SOURCE_OBJECT_CONST (TTOSC's own DELEGATE),
   * distinct from {@link #aspectArtifactExporter()} which is the value-of-TTOSC path.
   */
  @Override
  public EResult<Artifact> artifactableExportArtifact(
      ExprContext xx, QExprId loc) {
    return ExporterRoot.SOURCE_OBJECT_CONST.artifactExporterExport(xx, loc, this);
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
                    + dt.name.optUnwrapOr("(unnamed)")
                    + "' is not attached to a definition set");
          }
          if (dt.underlyingType.optIsNone()) {
            return EResult.err(
                loc,
                "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                    + dt.name.optUnwrapOr("(unnamed)")
                    + "' missing underlying type");
          }
          var errors = new TList<ExprErr>();
          var underlyingFinal = dt.underlyingType.optUnwrap();
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
                        underlyingFinal.artifactableExportArtifact(xx, loc);
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
                      if (argT.optIsNone()) {
                        errors.add(
                            new ExprErr(
                                loc,
                                TList.of(
                                    new Opaque(
                                        "TypeTypeSourceObjectConst::exporterExport: object '"
                                            + dt.name.optUnwrapOr("(unnamed)")
                                            + "' method '"
                                            + m.name
                                            + "' missing arg/ret type"))));
                        continue;
                      }
                      if (m.compiledBodyCode.optIsNone() && m.compiledClassName.optIsNone()) {
                        var objInternalName =
                            SourceObjectEmitter.internalNameFor(
                                dt.setId, dt.objIdx);
                        var finishRes =
                            m.finish(
                                xx,
                                loc,
                                dt.name.optUnwrapOr("(unnamed)"),
                                dt,
                                underlyingFinal,
                                objInternalName);
                        finishRes.drainInto(errors);
                      }
                      if (m.compiledBodyCode.optIsNone() && m.compiledClassName.optIsNone()) {
                        errors.add(
                            new ExprErr(
                                loc,
                                TList.of(
                                    new Opaque(
                                        "TypeTypeSourceObjectConst::exporterExport: object method '"
                                            + m.name
                                            + "' has not been compiled"))));
                        continue;
                      }
                      var argArtRes = argT.optUnwrap().artifactableExportArtifact(xx, loc);
                      argArtRes.drainInto(errors);
                      var argArt =
                          argArtRes.valueOrNull() != null
                              ? argArtRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method arg")
                              : nullArtifact();
                      Artifact retArt;
                      if (retT.optIsSome()) {
                        var retArtRes = retT.optUnwrap().artifactableExportArtifact(xx, loc);
                        retArtRes.drainInto(errors);
                        retArt =
                            retArtRes.valueOrNull() != null
                                ? retArtRes.requireValue(
                                    "TypeTypeSourceObjectConst::exporterExport method ret")
                                : nullArtifact();
                      } else {
                        retArt = nullArtifact();
                      }
                      var extRes = ExternalCodec.exportExternals(xx, loc, m.capturedExternals);
                      extRes.drainInto(errors);
                      var ext =
                          extRes.valueOrNull() != null
                              ? extRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method externals")
                              : TList.<ArtifactExternal>of();
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
                      var kindOpt = impl.interface_.builtinKind;
                      Opt<Artifact> userIface = Opt.none();
                      if (kindOpt.optIsNone()) {
                        var ifaceConst = new TypeSourceInterfaceDynamic(impl.interface_);
                        var ifaceArtRes = ifaceConst.artifactableExportArtifact(xx, loc);
                        ifaceArtRes.drainInto(errors);
                        userIface = Opt.some(
                            ifaceArtRes.valueOrNull() != null
                                ? ifaceArtRes.requireValue(
                                    "TypeTypeSourceObjectConst::exporterExport user iface")
                                : nullArtifact());
                      }
                      var methodImpls = new TList<ArtifactDataDefSetInterfaceMethodImpl>();
                      for (var mi : impl.methodImpls) {
                        var iface = impl.interface_;
                        if (mi.compiledBodyCode.optIsNone() && mi.compiledClassName.optIsNone()) {
                          var finishRes = mi.finish(xx, loc, iface, dt, underlyingFinal);
                          finishRes.drainInto(errors);
                        }
                        if (mi.compiledBodyCode.optIsNone() && mi.compiledClassName.optIsNone()) {
                          errors.add(
                              new ExprErr(
                                  loc,
                                  TList.of(
                                      new Opaque(
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
                                : TList.<ArtifactExternal>of();
                        methodImpls.add(
                            new ArtifactDataDefSetInterfaceMethodImpl(
                                mi.interfaceMethodIndex,
                                mi.compiledClassName,
                                mi.compiledBytecode,
                                ext));
                      }
                      ifaceImpls.add(
                          new ArtifactDataDefSetInterfaceImpl(kindOpt, userIface, methodImpls));
                    }
                    if (dt.compiledBinaryName.optIsNone() || dt.compiledBytecode == null) {
                      errors.add(
                          new ExprErr(
                              loc,
                              TList.of(
                                  new Opaque(
                                      "TypeTypeSourceObjectConst::exporterExport: object '"
                                          + dt.name.optUnwrapOr("(unnamed)")
                                          + "' has not been compiled — its definition set must be"
                                          + " finished and installed before export"))));
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
                    return makeBody.apply(ArtifactDataDefSetObjectCodec.toValue(body));
                  });
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
          return new Artifact.Body(
              new ArtifactBody(
                  new JsonValueNull(),
                  new Root()));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(loc, "TypeTypeSourceObjectConst requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSetObject body;
                try {
                  body = ArtifactDataDefSetObjectCodec.fromValue(raw);
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
                      case ArtifactExternal.UniqueRef ignored -> {}
                    }
                  }
                }
                for (var impl : body.interfaceImpls()) {
                  if (impl.userIface().optIsSome()) {
                    deps.add(impl.userIface().optUnwrap());
                  }
                  for (var mi : impl.methodImpls()) {
                    for (var ce : mi.capturedExternals()) {
                      switch (ce) {
                        case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                        case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                        case ArtifactExternal.StagedExpr ignored -> {}
                        case ArtifactExternal.UniqueRef ignored -> {}
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
                      var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                      dt.initSetReflectively(set);
                      dt.setId = set.defSetId;
                      var underlyingRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.underlyingType());
                      var underlyingVal =
                          underlyingRes.requireValue(
                              "TypeTypeSourceObjectConst::finalizer underlying type");
                      dt.underlyingType = Opt.some(underlyingVal);
                      var bodyMethodsIt = body.methods().iterator();
                      for (var md : dt.methods) {
                        var sm = bodyMethodsIt.next();
                        var argRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.argType());
                        var argVal =
                            argRes.requireValue(
                                "TypeTypeSourceObjectConst::finalizer method arg type");
                        md.argType = Opt.some(argVal);
                        var retRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, sm.retType());
                        var retVal =
                            retRes.requireValue(
                                "TypeTypeSourceObjectConst::finalizer method ret type");
                        md.retType = Opt.some(retVal);
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
                        if (impl.builtinKind().optIsSome()) {
                          iface = BuiltinInterfaceRegistry.resolve(impl.builtinKind().optUnwrap());
                        } else {
                          if (impl.userIface().optIsNone()) {
                            throw new IllegalStateException(
                                "TypeTypeSourceObjectConst finalizer: non-builtin iface impl missing "
                                    + "user_iface artifact");
                          }
                          var ifaceArt = impl.userIface().optUnwrap();
                          var ifaceAny =
                              ArtifactSupport.independentFinalizeImportArtifact(
                                  xx2, loc2, ifaceArt);
                          var ifaceVal =
                              ifaceAny.requireValue(
                                  "TypeTypeSourceObjectConst::finalizer user iface artifact");
                          if (!(ifaceVal
                              instanceof TypeSourceInterfaceDynamic.ImportedInterfaceConst ic)) {
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
                      dt.compiledStaticFieldValues =
                          Opt.some(SourceObjectEmitter.rebuildStaticFieldValues(dt));
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(imported, deps, fin));
              });
        }
      };

  public record ImportedSourceObjectConst(TypeSourceObjectDynamic type_) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(type_);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new ValueObjectConst(new TypeTypeSourceObjectConst(type_), type_));
    }
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return type_.aspectConsumeDynamicToDynamic(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    TypeSourceObjectDynamic dynType = type_;
    if (!self.code().codeIsNone()) {
      return dynType.aspectFieldField(xx, loc, self, key);
    }
    var selfCodeRes = aspectConsumeDynamicToDynamic(xx, loc, self);
    if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
    var selfCode = selfCodeRes.requireValue("TypeTypeSourceObjectConst::field toDynamic");
    var selfDynamic =
        new CodeValue(
            Code.of(loc, selfCode),
            EvalOutcome.ofValue(ValueDynamic.stack(dynType, 1)));
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
    if (value instanceof ValueObjectConst voc
        && voc.data() instanceof Type t) {
      return t;
    }
    return type_;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeTypeSourceObjectConst.class);
    return oOpt.optIsSome() && type_.typeIsEqual(xx, oOpt.optUnwrap().type_);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
