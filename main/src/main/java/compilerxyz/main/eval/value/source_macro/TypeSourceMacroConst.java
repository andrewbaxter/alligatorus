package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindConstValue;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataDefSetMacroCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6028L)
public record TypeSourceMacroConst(SourceMacroData data) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return data.description.optUnwrapOr("definition_set_const_function");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_MACRO_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(new AspectTypeSourceMacroConst(this));
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionSourceMacroConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindConstValue.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactSourceMacroConst.INSTANCE);
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
            if (!(self instanceof TypeSourceMacroConst mc)) {
                return EResult.err(
                        loc,
                        "TypeSourceMacroConst::exporterRootDelegateSelfExport: expected TypeSourceMacroConst, got "
                                + self.getClass());
            }
            var data = mc.data;
            return ArtifactSupport.implExportIndependentArtifact(xx, data.artifactIdCell, () -> {
                var erb = new Erb(loc);
                var functionArt = erb.mustErMaybeUnreachable(
                        Exporters.exportType(xx, loc, new TypeSourceFunctionConst(data.function)));
                Opt<Artifact> exceptionArt = Opt.none();
                if (data.exceptionType.optIsSome()) {
                    exceptionArt =
                            erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, data.exceptionType.optUnwrap()));
                }
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactDataDefSetMacro(functionArt.optUnwrap(), exceptionArt, data.description);
                return EResult.ok(makeBody.apply(ArtifactDataDefSetMacroCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeSourceMacroConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataDefSetMacro body;
                        try {
                            body = ArtifactDataDefSetMacroCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeSourceMacroConst data: failed to parse "
                                    + "ArtifactDataDefSetMacro: "
                                    + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.function());
                        if (body.exceptionType().optIsSome()) {
                            deps.add(body.exceptionType().optUnwrap());
                        }
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var functionAny =
                                    ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.function());
                            var function = MustCast.mustCast(
                                    TypeSourceFunctionConst.class,
                                    functionAny.requireValue("TypeSourceMacroConst::finalizer function artifact"));
                            var macroData = new SourceMacroData(DynRuntime.freshUnique(xx2.vx), function.data());
                            macroData.artifactIdCell.set(idValue);
                            if (body.exceptionType().optIsSome()) {
                                var exceptionRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                        xx2, loc2, body.exceptionType().optUnwrap());
                                macroData.exceptionType = Opt.some(
                                        exceptionRes.requireValue("TypeSourceMacroConst::finalizer exception type"));
                            }
                            macroData.description = body.description();
                            return new TypeSourceMacroConst(macroData);
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_MACRO_CONST_IDENTITY, TypeSourceMacroConst.class);
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
    public EResult<ArtifactExporter> typeExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(AspectArtifactSourceMacroConst.INSTANCE.aspectArtifactExporter());
    }
}
