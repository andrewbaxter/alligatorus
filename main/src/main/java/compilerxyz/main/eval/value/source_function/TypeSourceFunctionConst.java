package compilerxyz.main.eval.value.source_function;

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
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.function.TypeFunctionConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataDefSetFunctionCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6022L)
public record TypeSourceFunctionConst(SourceFunctionData data)
        implements Type, AspectArtifact, AspectBind, AspectDropDynamicLight, AspectFunction, AspectType {

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public String typeDesc() {
        return data.description.optUnwrapOr("definition_set_function");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_FUNCTION_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectFunction.AspectId)
                || key.equals(GenAspectType.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_FUNCTION_CONST;
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeSourceFunctionConst fc)) {
                return EResult.err(
                        loc,
                        "TypeSourceFunctionConst::exporterRootDelegateSelfExport: expected TypeSourceFunctionConst, got "
                                + self.getClass());
            }
            var data = fc.data;
            var set = data.set;
            if (set == null) {
                return EResult.err(
                        loc,
                        "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                                + data.name.optUnwrapOr("(unnamed)")
                                + "' is not attached to a definition set");
            }
            var argTypeOpt = data.argType;
            if (argTypeOpt.optIsNone()) {
                return EResult.err(
                        loc,
                        "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                                + data.name.optUnwrapOr("(unnamed)")
                                + "' missing arg type");
            }
            var argType = argTypeOpt.optUnwrap();
            var retType = data.retType;
            if (data.compiledClassName.optIsNone()) {
                var finishRes = data.finish(xx, loc);
                if (finishRes.hasErrors()) {
                    return finishRes.reerr();
                }
            }

            if (data.compiledClassName.optIsNone() && data.functionSpec.optIsSome()) {
                set.finishFunctions();
            }
            var compiledClassName = data.compiledClassName;
            var compiledBytecode = data.compiledBytecode;
            if (compiledClassName.optIsNone() || compiledBytecode.optIsNone()) {
                return EResult.err(
                        loc,
                        "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                                + data.name.optUnwrapOr("(unnamed)")
                                + "' has not been compiled (finish() returned no source)");
            }
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, data.artifactIdCell, () -> {
                        var defSetArtRes = Artifactable.exportArtifact(xx, loc, set);
                        defSetArtRes.drainInto(errors);
                        var defSetArt = defSetArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var argArtRes = Artifactable.exportArtifact(xx, loc, argType);
                        argArtRes.drainInto(errors);
                        var argArt = argArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        Artifact retArt;
                        if (retType.optIsSome()) {
                            var retArtRes = Artifactable.exportArtifact(xx, loc, retType.optUnwrap());
                            retArtRes.drainInto(errors);
                            retArt = retArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        } else {
                            retArt = nullArtifact();
                        }
                        var externalsRes = ExternalCodec.exportExternals(xx, loc, data.capturedExternals);
                        externalsRes.drainInto(errors);
                        var externals = externalsRes.valueOpt().optUnwrapOr(TList.<ArtifactExternal>of());
                        var body = new ArtifactDataDefSetFunction(
                                defSetArt,
                                set.defSetId,
                                data.name,
                                argArt,
                                retArt,
                                data.jvmMethodName,
                                compiledClassName,
                                compiledBytecode,
                                externals);
                        return makeBody.apply(ArtifactDataDefSetFunctionCodec.toValue(body));
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
                return EResult.err(loc, "TypeSourceFunctionConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataDefSetFunction body;
                        try {
                            body = ArtifactDataDefSetFunctionCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeSourceFunctionConst data: failed to parse "
                                    + "ArtifactDataDefSetFunction: "
                                    + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var funcData = new SourceFunctionData(
                                body.name(), DynRuntime.freshUnique(xx.vx), body.jvmMethodName());
                        funcData.artifactIdCell.set(idValue);
                        funcData.compiledClassName = body.compiledClassName();
                        funcData.compiledBytecode = body.compiledBytecode();
                        var fc = new TypeSourceFunctionConst(funcData);
                        var deps = new TList<Artifact>();
                        deps.add(body.defSet());
                        deps.add(body.argType());
                        deps.add(body.retType());
                        for (var ext : body.capturedExternals()) {
                            switch (ext) {
                                case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                                case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                                case ArtifactExternal.StagedExpr ignored -> {}
                                case ArtifactExternal.UniqueRef ignored -> {}
                                case ArtifactExternal.Loc ignored -> {}
                            }
                        }
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var defSetAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.defSet());
                            var defSetVal =
                                    defSetAny.requireValue("TypeSourceFunctionConst::finalizer defSet artifact");
                            var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                            funcData.initSetReflectively(set);
                            var argRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.argType());
                            var argVal = argRes.requireValue("TypeSourceFunctionConst::finalizer arg type");
                            funcData.argType = Opt.some(argVal);
                            var retRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.retType());
                            var retVal = retRes.requireValue("TypeSourceFunctionConst::finalizer ret type");
                            funcData.retType = Opt.some(retVal);
                            var extRes = ExternalCodec.importExternals(xx2, loc2, body.capturedExternals());
                            var extVal = extRes.requireValue("TypeSourceFunctionConst::finalizer externals");
                            funcData.capturedExternals.addAll(extVal);
                            return fc;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(fc), deps, fin));
                    });
        }
    };

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceFunctionConst::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(Code.none(loc), new SourceFunctionConstBinding(v)));
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        var set = data.set;
        if (set == null) {
            return EResult.err(loc, "TypeSourceFunctionConst::call: definition set function has no set reference");
        }
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        if (data.argType.optIsNone()) {
            return EResult.err(loc, "TypeSourceFunctionConst::call: definition set function missing arg type");
        }
        var argType = data.argType.optUnwrap();
        var retType = data.retType;
        var klass = JavaInternalName.of(DefSetFunctionsEmitter.functionsInternalName(set.defSetId));
        var methodName = data.jvmMethodName;
        var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("TypeSourceFunctionConst::call converted arg");
        var argCodesRes = DynUtil.destructurePositional(xx, loc, argType, converted);
        if (argCodesRes.hasErrors()) return argCodesRes.reerr();
        var argCodes = argCodesRes.requireValue("TypeSourceFunctionConst::call arg codes");

        var descRes = TypeFunctionConst.descriptorFor(xx, loc, argType, retType, false);
        if (descRes.hasErrors()) return descRes.reerr();
        var invocationRes = TypeFunctionConst.emitStaticInvocation(
                xx,
                loc,
                klass,
                methodName,
                descRes.requireValue("TypeSourceFunctionConst::call descriptor"),
                false,
                argCodes);
        if (invocationRes.hasErrors()) return invocationRes.reerr();
        var invocation = invocationRes.requireValue("TypeSourceFunctionConst::call invocation");
        if (retType.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(invocation), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType.optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(retStackRes
                .requireValue("TypeSourceFunctionConst::call ret stack")
                .aspectStackValueDynamicToValue(xx, loc, invocation));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return data.exceptionType;
    }

    @Override
    public Type aspectTypeType(Value value) {
        return new TypeSourceFunctionConst(data);
    }

    private record SourceFunctionConstBinding(Value value) implements Binding {
        @Override
        public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
            return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
        }

        @Override
        public Binding bindingFork() {
            return this;
        }

        @Override
        public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
            for (var other : others) {
                if (!this.equals(other)) {
                    return EResult.err(
                            loc,
                            "cannot merge " + getClass().getSimpleName()
                                    + " with a differently-typed binding across branches");
                }
            }
            return EResult.ok(this);
        }

        @Override
        public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
            return Binding.bindingDrop(xx, loc, this);
        }
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_SOURCE_FUNCTION_CONST_IDENTITY,
                TypeSourceFunctionConst.class);
        return oOpt.optIsSome() && data.identity.equals(oOpt.optUnwrap().data.identity);
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
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(aspectArtifactExporter());
    }
}
