package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.ArtifactJavaClassCompiled;
import compilerxyz.main.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataDefSetFunctionCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6022L, function = true)
public record TypeSourceFunctionConst(SourceFunctionData data) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return data.description.optUnwrapOr("definition_set_function");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_FUNCTION_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(new AspectTypeSourceFunctionConst(this));
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionSourceFunctionConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindSourceFunctionConst.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactSourceFunctionConst.INSTANCE);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
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
            if (!set.finished) {
                return EResult.err(
                        loc,
                        "Function '" + data.name.optUnwrapOr("(unnamed)")
                                + "' cannot be exported: its definition set has not been finished (d.finish())");
            }
            if (data.argType.optIsNone() || data.compiledClassName.optIsNone() || data.compiledClass.optIsNone()) {
                throw new AssertionError(
                        "function '" + data.name.optUnwrapOr("(unnamed)") + "' in a finished set is not compiled");
            }
            var argType = data.argType.optUnwrap();
            var retType = data.retType;
            var compiledClassName = data.compiledClassName;
            var compiledClass = data.compiledClass;
            return ArtifactSupport.implExportIndependentArtifact(xx, data.artifactIdCell, () -> {
                var erb = new Erb(loc);
                var defSetArt = erb.mustErMaybeUnreachable(Exporters.exportObject(xx, loc, set));
                var argArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, argType));
                Opt<Artifact> retArt = Opt.none();
                if (retType.optIsSome()) {
                    retArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, retType.optUnwrap()));
                }
                var externals =
                        erb.mustErMaybeUnreachable(ExternalCodec.exportExternals(xx, loc, data.capturedExternals));
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactDataDefSetFunction(
                        defSetArt.optUnwrap(),
                        set.defSetId,
                        data.name,
                        argArt.optUnwrap(),
                        retArt,
                        data.jvmMethodName,
                        compiledClassName,
                        compiledClass.optMap(ArtifactJavaClassCompiled::fromCompiled),
                        externals.optUnwrap());
                return EResult.ok(makeBody.apply(ArtifactDataDefSetFunctionCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
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
                        funcData.compiledClass = body.compiledClass().optMap(ArtifactJavaClassCompiled::toCompiled);
                        var fc = new TypeSourceFunctionConst(funcData);
                        var deps = new TList<Artifact>();
                        deps.add(body.defSet());
                        deps.add(body.argType());
                        if (body.retType().optIsSome()) deps.add(body.retType().optUnwrap());
                        for (var ext : body.capturedExternals()) {
                            switch (ext) {
                                case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                                case ArtifactExternal.Exported ex -> deps.add(ex.artifact());
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
                            if (body.retType().optIsSome()) {
                                var retRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                        xx2, loc2, body.retType().optUnwrap());
                                var retVal = retRes.requireValue("TypeSourceFunctionConst::finalizer ret type");
                                funcData.retType = Opt.some(retVal);
                            }
                            var extRes = ExternalCodec.importExternals(xx2, loc2, body.capturedExternals());
                            var extVal = extRes.requireValue("TypeSourceFunctionConst::finalizer externals");
                            funcData.capturedExternals.addAll(extVal);
                            funcData.compiledStaticFieldValues =
                                    Opt.some(DefSetFunctionsEmitter.rebuildStaticFieldValues(funcData));
                            return fc;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(fc), deps, fin));
                    });
        }
    };

    record SourceFunctionConstBinding(Value value) implements Binding {
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
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(AspectArtifactSourceFunctionConst.INSTANCE.aspectArtifactExporter());
    }
}
