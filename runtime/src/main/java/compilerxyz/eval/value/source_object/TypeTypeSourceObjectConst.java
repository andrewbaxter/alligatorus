package compilerxyz.eval.value.source_object;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;

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
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.source_function.ExternalCodec;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;

@BuiltinIdentity(uniqueId = 6138L)
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    var d = type_.description;
    if (d != null) return d;
    return "object(" + (type_.name != null ? type_.name : "?") + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeTypeSourceObjectConst.class, "type_source_object_const", compilerxyz.artifact.ExporterRoot.SOURCE_OBJECT_CONST);

                public Type valueType() {
    return META;
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
    }
    if (key.equals(GenAspectType.AspectId)) {
      for (var impl : type_.interfaceImpls) {
        if (impl.interface_.builtinKind
            == BuiltinInterfaceKind.TYPE) {
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
  public compilerxyz.eval.EResult<Artifact> exportArtifact(
      compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc) {
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
                        underlyingFinal.exportArtifact(xx, loc);
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
                      var argArtRes = argT.exportArtifact(xx, loc);
                      argArtRes.drainInto(errors);
                      var argArt =
                          argArtRes.valueOrNull() != null
                              ? argArtRes.requireValue(
                                  "TypeTypeSourceObjectConst::exporterExport method arg")
                              : nullArtifact();
                      var retArtRes = retT.exportArtifact(xx, loc);
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
                        var ifaceConst = new TypeSourceInterfaceDynamic(impl.interface_);
                        var ifaceArtRes = ifaceConst.exportArtifact(xx, loc);
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
                    if (dt.compiledBinaryName == null || dt.compiledBytecode == null) {
                      errors.add(
                          new compilerxyz.eval.ExprErr(
                              loc,
                              compilerxyz.util.TList.of(
                                  new compilerxyz.eval.ExprSubErr.Opaque(
                                      "TypeTypeSourceObjectConst::exporterExport: object '"
                                          + (dt.name == null ? "(unnamed)" : dt.name)
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
                          iface = BuiltinInterfaceRegistry.resolve(impl.builtinKind());
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
                          if (!(ifaceVal instanceof TypeSourceInterfaceDynamic ic)) {
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
                          compilerxyz.eval.dynamic.SourceObjectEmitter.rebuildStaticFieldValues(dt);
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(imported, deps, fin));
              });
        }
      };

  public record ImportedSourceObjectConst(TypeSourceObjectDynamic type_) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeTypeSourceObjectConst(type_));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(new TypeTypeSourceObjectConst(type_)));
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
    if (!self.code().isNone()) {
      return dynType.aspectFieldField(xx, loc, self, key);
    }
    var selfCodeRes = aspectConsumeDynamicToDynamic(xx, loc, self);
    if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
    var selfCode = selfCodeRes.requireValue("TypeTypeSourceObjectConst::field toDynamic");
    var selfDynamic =
        new CodeValue(
            Code.of(loc, selfCode),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, dynType)));
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
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst voc
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
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
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
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeTypeSourceObjectConst.class) instanceof Opt.Some<TypeTypeSourceObjectConst> oSome
        && type_.typeIsEqual(xx, oSome.value().type_);
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
