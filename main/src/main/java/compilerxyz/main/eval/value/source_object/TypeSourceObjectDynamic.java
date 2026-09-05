package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter.Root;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr.Opaque;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectDropDynamicLightDefault;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.code.TypeCodeDynamic;
import compilerxyz.main.eval.value.source_function.ExternalCodec;
import compilerxyz.main.eval.value.source_interface.TypeInterfaceMethodDynamic;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.gen.GenAspect;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.codec.ArtifactDataDefSetObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6020L)
public final class TypeSourceObjectDynamic implements Aspect, Type {

    public DefSetId setId;

    @SuppressWarnings("NullAway.Init")
    public final DefinitionSet set;

    public final Opt<String> name;
    public Opt<String> description = Opt.none();
    public Opt<Type> underlyingType = Opt.none();
    public final Unique identity;
    public final String binaryName;

    public final TList<SourceDataMethodData> methods;
    public final TList<SourceObjectInterfaceImplData> interfaceImpls;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();
    public final ArtifactSupport.ArtifactIdHolder newArtifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public Opt<byte[]> compiledBytecode = Opt.none();
    public Opt<TOrderedMap<String, Object>> compiledStaticFieldValues = Opt.none();

    public TypeSourceObjectDynamic(DefSetId setId, String name, Unique identity, DefinitionSet set, String binaryName) {
        this.setId = setId;
        this.set = set;
        this.name = Opt.some(name);
        this.identity = identity;
        this.binaryName = binaryName;
        this.methods = new TList<>();
        this.interfaceImpls = new TList<>();
    }

    @SuppressWarnings("NullAway")
    public TypeSourceObjectDynamic(DefSetId setId, Opt<String> name, Unique identity, String binaryName) {
        this.setId = setId;
        this.set = (DefinitionSet) null;
        this.name = name;
        this.identity = identity;
        this.binaryName = binaryName;
        this.methods = new TList<>();
        this.interfaceImpls = new TList<>();
    }

    public String internalName() {
        return binaryName.replace('.', '/');
    }

    public void initSetReflectively(DefinitionSet set) {
        try {
            var f = TypeSourceObjectDynamic.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(this, set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "TypeSourceObjectDynamic.initSetReflectively: failed to write final 'set' field", e);
        }
    }

    @Override
    public String typeDesc() {
        return description.optUnwrapOr("object(" + name.optUnwrapOr("?") + ")");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_OBJECT_CONST);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicSourceObjectDynamic(this));
        }
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicSourceObjectDynamic(this));
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicSourceObjectDynamic(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldSourceObjectDynamic(this));
        }
        if (key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(AspectDropDynamicLightDefault.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicSourceObjectDynamic(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }

        for (var impl : interfaceImpls) {
            var ifaceIdentity = impl.interface_.identity;
            if (ifaceIdentity.equals(GenAspectField.IdentityId) && key.equals(GenAspectField.AspectId)) {
                return Opt.some(new AspectFieldSourceObjectDynamic(this));
            }
            if (ifaceIdentity.equals(GenAspectLift.IdentityId) && key.equals(GenAspectLift.AspectId)) {
                return Opt.some(new AspectLiftSourceObjectDynamic(this));
            }
            if (ifaceIdentity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY) && key.equals(GenAspectType.AspectId)) {
                return Opt.some(new AspectTypeSourceObjectDynamic(this));
            }
        }
        return Opt.none();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (destType.typeAspect(xx, identity).optIsSome()) {
            return true;
        }
        for (var impl : interfaceImpls) {
            if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
                return true;
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
                    return true;
                }
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.CODE_DYN_IDENTITY)) {
                    return true;
                }
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
            return true;
        }
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, identity).optIsSome()) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            boolean hasTypeImpl = false;
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
                    hasTypeImpl = true;
                    break;
                }
            }
            if (hasTypeImpl) {
                var sourceCodeRes = CodeDynamic.castRequired(
                        loc, cv.code(), "cast " + typeDesc() + " -> any: source has no runtime code");
                if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
                var sourceCode = sourceCodeRes.requireValue("sourceCode");
                return EResult.ok(emitBuiltinCast(xx, sourceCode, GenType.dynamicType()));
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.CODE_DYN_IDENTITY)) {
                    var codeSourceRes = CodeDynamic.castRequired(
                            loc, cv.code(), "cast " + typeDesc() + " -> code: source has no runtime code");
                    if (codeSourceRes.hasErrors()) return codeSourceRes.reerr();
                    var codeSource = codeSourceRes.requireValue("codeSource");
                    var wrapSeq = new JavaBytecodeSequence();
                    wrapSeq.add(codeSource);
                    wrapSeq.add(JavaBytecodeUtils.callStaticMethod(
                            new CodeLoc.Some(loc), GenCodegenDescriptors.CODE_TARGET));
                    return EResult.ok(new CodeValue(
                            Code.target(new CodeDynamic(new CodeLoc.Some(loc), wrapSeq)),
                            EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeCodeDynamic()))));
                }
            }
            return EResult.err(loc, "cast " + typeDesc() + " -> code: object does not implement code_dyn");
        }
        for (var impl : interfaceImpls) {
            if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
                var frozen = impl.interface_.frozen();
                Type resultType = null;
                if (frozen.builtinType.optIsSome() && !frozen.isAspect) {
                    resultType = frozen.builtinType.optUnwrap();
                }
                if (resultType != null) {
                    var sourceCodeRes = CodeDynamic.castRequired(
                            loc, cv.code(), "cast " + typeDesc() + " -> interface: source has no runtime code");
                    if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
                    var sourceCode = sourceCodeRes.requireValue("sourceCode");
                    return EResult.ok(emitBuiltinCast(xx, sourceCode, resultType));
                }
                return EResult.ok(new CodeValue(
                        cv.code(),
                        EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeSourceInterfaceDynamic(impl.interface_)))));
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
            var sourceCodeRes = CodeDynamic.castRequired(
                    loc, cv.code(), "cast " + typeDesc() + " -> aspect: source has no runtime code");
            if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
            var sourceCode = sourceCodeRes.requireValue("sourceCode");
            return EResult.ok(emitBuiltinCast(xx, sourceCode, GenAspect.dynamicType()));
        }
        return EResult.err(loc, "Cannot convert " + typeDesc() + " to " + destType.typeDesc());
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType.typeAspect(xx, identity).optIsSome();
    }

    private CodeValue emitBuiltinCast(ExprContext xx, CodeDynamic sourceCode, Type resultType) {
        return new CodeValue(Code.target(sourceCode), EvalOutcome.ofValue(ValueDynamic.stack(xx, resultType)));
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var dt = MustCast.mustCast(TypeSourceObjectDynamic.class, self);
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
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, dt.artifactIdCell, () -> {
                        var defSetArtRes = Artifactable.exportArtifact(xx, loc, set);
                        defSetArtRes.drainInto(errors);
                        var defSetArt = defSetArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var underlyingArtRes = Artifactable.exportArtifact(xx, loc, underlyingFinal);
                        underlyingArtRes.drainInto(errors);
                        var underlyingArt = underlyingArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var methods = new TList<ArtifactDataDefSetMethod>();
                        for (var m : dt.methods) {
                            var argT = m.argType;
                            var retT = m.retType;
                            if (argT.optIsNone()) {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object '"
                                                + dt.name.optUnwrapOr("(unnamed)")
                                                + "' method '"
                                                + m.name
                                                + "' missing arg/ret type"))));
                                continue;
                            }
                            if (m.compiledBodyCode.optIsNone()) {
                                var objInternalName = dt.internalName();
                                var finishRes = m.finish(
                                        xx,
                                        loc,
                                        dt.name.optUnwrapOr("(unnamed)"),
                                        dt,
                                        underlyingFinal,
                                        objInternalName);
                                finishRes.drainInto(errors);
                            }
                            if (m.compiledBodyCode.optIsNone()) {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object method '"
                                                + m.name
                                                + "' has not been compiled"))));
                                continue;
                            }
                            var argArtRes = Artifactable.exportArtifact(xx, loc, argT.optUnwrap());
                            argArtRes.drainInto(errors);
                            var argArt = argArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            Artifact retArt;
                            if (retT.optIsSome()) {
                                var retArtRes = Artifactable.exportArtifact(xx, loc, retT.optUnwrap());
                                retArtRes.drainInto(errors);
                                retArt = retArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            } else {
                                retArt = nullArtifact();
                            }
                            var extRes = ExternalCodec.exportExternals(xx, loc, m.capturedExternals);
                            extRes.drainInto(errors);
                            var ext = extRes.valueOpt().optUnwrapOr(TList.<ArtifactExternal>of());
                            methods.add(new ArtifactDataDefSetMethod(m.name, argArt, retArt, m.jvmMethodName, ext));
                        }
                        var ifaceImpls = new TList<ArtifactDataDefSetInterfaceImpl>();
                        for (var impl : dt.interfaceImpls) {
                            var ifaceArtRes = Artifactable.exportArtifact(
                                    xx, loc, new TypeSourceInterfaceDynamic(impl.interface_));
                            ifaceArtRes.drainInto(errors);
                            var ifaceArt = ifaceArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            var methodImpls = new TList<ArtifactDataDefSetInterfaceMethodImpl>();
                            for (var mi : impl.methodImpls) {
                                if (mi.compiledBodyCode.optIsNone()) {
                                    var finishRes = mi.finish(xx, loc, dt, underlyingFinal);
                                    finishRes.drainInto(errors);
                                }
                                if (mi.compiledBodyCode.optIsNone()) {
                                    errors.add(new ExprErr(
                                            loc,
                                            TList.of(new Opaque(
                                                    "TypeTypeSourceObjectConst::exporterExport: iface method impl '"
                                                            + mi.interfaceMethod.name
                                                            + "' has not been compiled"))));
                                    continue;
                                }
                                var methodArtRes = Artifactable.exportArtifact(
                                        xx, loc, new TypeInterfaceMethodDynamic(impl.interface_, mi.interfaceMethod));
                                methodArtRes.drainInto(errors);
                                var methodArt = methodArtRes.valueOpt().optUnwrapOr(nullArtifact());
                                var extRes = ExternalCodec.exportExternals(xx, loc, mi.capturedExternals);
                                extRes.drainInto(errors);
                                var ext = extRes.valueOpt().optUnwrapOr(TList.<ArtifactExternal>of());
                                methodImpls.add(
                                        new ArtifactDataDefSetInterfaceMethodImpl(methodArt, mi.jvmMethodName, ext));
                            }
                            ifaceImpls.add(new ArtifactDataDefSetInterfaceImpl(ifaceArt, methodImpls));
                        }
                        if (dt.compiledBytecode.optIsNone()) {
                            errors.add(new ExprErr(
                                    loc,
                                    TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object '"
                                            + dt.name.optUnwrapOr("(unnamed)")
                                            + "' has not been compiled — its definition set must be"
                                            + " finished and installed before export"))));
                        }
                        var body = new ArtifactDataDefSetObject(
                                defSetArt,
                                set.defSetId,
                                dt.name,
                                dt.identity,
                                underlyingArt,
                                methods,
                                ifaceImpls,
                                dt.binaryName,
                                dt.compiledBytecode);
                        return makeBody.apply(ArtifactDataDefSetObjectCodec.toValue(body));
                    });
            if (errors.isEmpty()) {
                return EResult.ok(artifact);
            }
            return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
            return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new Root()));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
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
                            return new Result.Err<>("TypeTypeSourceObjectConst type_: failed to parse "
                                    + "ArtifactDataDefSetObject: "
                                    + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var dt = new TypeSourceObjectDynamic(
                                body.defSetId(), body.name(), body.identity(), body.binaryName());
                        dt.artifactIdCell.set(idValue);
                        dt.compiledBytecode = body.compiledBytecode();
                        for (var m : body.methods()) {
                            var md = new SourceDataMethodData(m.name(), m.jvmMethodName());
                            dt.methods.add(md);
                        }

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
                                    case ArtifactExternal.Loc ignored -> {}
                                }
                            }
                        }
                        for (var impl : body.interfaceImpls()) {
                            deps.add(impl.iface());
                            for (var mi : impl.methodImpls()) {
                                deps.add(mi.interfaceMethod());
                                for (var ce : mi.capturedExternals()) {
                                    switch (ce) {
                                        case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                                        case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                                        case ArtifactExternal.StagedExpr ignored -> {}
                                        case ArtifactExternal.UniqueRef ignored -> {}
                                        case ArtifactExternal.Loc ignored -> {}
                                    }
                                }
                            }
                        }
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var defSetAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.defSet());
                            var defSetVal =
                                    defSetAny.requireValue("TypeTypeSourceObjectConst::finalizer defSet artifact");
                            var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                            dt.initSetReflectively(set);
                            dt.setId = set.defSetId;
                            var underlyingRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.underlyingType());
                            var underlyingVal =
                                    underlyingRes.requireValue("TypeTypeSourceObjectConst::finalizer underlying type");
                            dt.underlyingType = Opt.some(underlyingVal);
                            var bodyMethodsIt = body.methods().iterator();
                            for (var md : dt.methods) {
                                var sm = bodyMethodsIt.next();
                                var argRes =
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, sm.argType());
                                var argVal =
                                        argRes.requireValue("TypeTypeSourceObjectConst::finalizer method arg type");
                                md.argType = Opt.some(argVal);
                                var retRes =
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, sm.retType());
                                var retVal =
                                        retRes.requireValue("TypeTypeSourceObjectConst::finalizer method ret type");
                                md.retType = Opt.some(retVal);
                                var extRes = ExternalCodec.importExternals(xx2, loc2, sm.capturedExternals());
                                var extVal =
                                        extRes.requireValue("TypeTypeSourceObjectConst::finalizer method externals");
                                md.capturedExternals.addAll(extVal);
                            }
                            for (var impl : body.interfaceImpls()) {
                                var ifaceAny =
                                        ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, impl.iface());
                                var ifaceVal =
                                        ifaceAny.requireValue("TypeTypeSourceObjectConst::finalizer iface artifact");
                                var iface = MustCast.mustCast(TypeSourceInterfaceDynamic.class, ifaceVal)
                                        .cell();
                                var implData = new SourceObjectInterfaceImplData(iface);
                                for (var mi : impl.methodImpls()) {
                                    var methodAny = ArtifactSupport.independentFinalizeImportArtifact(
                                            xx2, loc2, mi.interfaceMethod());
                                    var methodVal = methodAny.requireValue(
                                            "TypeTypeSourceObjectConst::finalizer iface method artifact");
                                    var mid = new SourceObjectInterfaceMethodImplData(
                                            MustCast.mustCast(TypeInterfaceMethodDynamic.class, methodVal)
                                                    .method(),
                                            mi.jvmMethodName());
                                    var extRes = ExternalCodec.importExternals(xx2, loc2, mi.capturedExternals());
                                    var extVal = extRes.requireValue(
                                            "TypeTypeSourceObjectConst::finalizer iface method externals");
                                    mid.capturedExternals.addAll(extVal);
                                    implData.methodImpls.add(mid);
                                }
                                dt.interfaceImpls.add(implData);
                            }
                            dt.compiledStaticFieldValues = Opt.some(SourceObjectEmitter.rebuildStaticFieldValues(dt));
                            return dt;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(dt), deps, fin));
                    });
        }
    };
}
