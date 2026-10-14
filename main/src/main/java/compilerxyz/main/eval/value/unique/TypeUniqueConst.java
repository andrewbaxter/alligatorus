package compilerxyz.main.eval.value.unique;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindConstValue;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6148L)
public record TypeUniqueConst() implements Aspect, Type {

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var u = MustCast.mustCast(Unique.class, self);
            return EResult.ok(new Artifact.Body(makeBody.apply(TypeUniqueDynamic.encodeUnique(u))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueObject obj -> {
                    var decoded = TypeUniqueDynamic.decodeUnique(loc, obj);
                    if (decoded.hasErrors()) {
                        yield EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: malformed");
                    }
                    yield EResult.ok(
                            decoded.requireValue("TypeUniqueConst::exporterRootDelegateSelfFromArtifactData decoded"));
                }
                case JsonValueNull ignored ->
                    EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueBoolean b ->
                    EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueNumber n ->
                    EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueString s ->
                    EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
                case JsonValueArray a ->
                    EResult.err(loc, "TypeUniqueConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            };
        }
    };

    @Override
    public String typeDesc() {
        return "unique_const";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqUniqueConst.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicUniqueConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindConstValue.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactUniqueConst.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    static Opt<Unique> extractUnique(CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof Unique u) {
            return Opt.some(u);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_CONST_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException("TypeUniqueConst::typeCastTo unreachable");
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.UNIQUE_CONST_TYPE);
    }
}
