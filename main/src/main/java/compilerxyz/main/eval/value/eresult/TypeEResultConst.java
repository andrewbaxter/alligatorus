package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConstDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactEResultTypeCodec;
import compilerxyz.main.gen.codec.ArtifactEResultValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6464L)
public final class TypeEResultConst<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeEResultConst(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public String typeDesc() {
        return "const eresult(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectConstDynamic.AspectId)) {
            return Opt.some(new AspectConstDynamicEResultConst<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(
                    new AspectUnionEResultDynamic<>(new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, elementType))));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldEResultConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicEResultConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindEResultConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(new AspectArtifactEResultConst<>(this));
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
            return EResult.err(loc, "TypeEResultConst data is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactEResultValue body;
            try {
                body = ArtifactEResultValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc, "TypeEResultConst data: failed to parse ArtifactEResultValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            EResult<Object> host;
            if (body.ok().optIsSome()) {
                var imported = erb.mustErMaybeUnreachable(
                        ArtifactSupport.importArtifact(xx, loc, body.ok().optUnwrap()));
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                host = EResult.ok(imported.optUnwrap());
            } else {
                var errs = new TList<ExprErr>(body.errs().size());
                for (var errArt : body.errs()) {
                    var imported = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, errArt));
                    if (imported.optIsNone()) continue;
                    errs.add(MustCast.mustCast(ExprErr.class, imported.optUnwrap()));
                }
                host = EResult.errs(errs);
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            return EResult.ok(host);
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var eresult = MustCast.mustCast(TypeEResultConst.class, self);
            return ArtifactSupport.implExportIndependentArtifact(xx, eresult.artifactIdCell, () -> {
                var elementArtRes = Exporters.exportType(xx, loc, eresult.elementType);
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactEResultType(elementArtRes.requireValue("TypeEResultConst::export element"));
                return EResult.ok(makeBody.apply(ArtifactEResultTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeEResultConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactEResultType body;
                        try {
                            body = ArtifactEResultTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeEResultConst data: failed to parse ArtifactEResultType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var out = new TypeEResultConst<>(
                                    elementRes.requireValue("TypeEResultConst::finalizer element type"));
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_E_RESULT_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeEResultConst<?> o
                && elementType.typeIsEqual(xx, o.elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, elementType)).typeIsEqual(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return new AspectConsumeDynamicEResultConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.ERESULT_CONST_TYPE);
    }
}
