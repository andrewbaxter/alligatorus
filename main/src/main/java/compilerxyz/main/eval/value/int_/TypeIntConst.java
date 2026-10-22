package compilerxyz.main.eval.value.int_;

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
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.AspectConstDynamicOf;
import compilerxyz.main.eval.value.numeric.AspectOperatorAddNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorAndNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorBitNotNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorDivNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorEqNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorGeNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorGtNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorLeNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorLtNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorModNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorMulNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorNeNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorNegNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorOrNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorShlNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorShrNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorSubNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorUshrNumeric;
import compilerxyz.main.eval.value.numeric.AspectOperatorXorNumeric;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConstDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInt;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.GenAspectOperatorAdd;
import compilerxyz.main.gen.GenAspectOperatorAnd;
import compilerxyz.main.gen.GenAspectOperatorBitNot;
import compilerxyz.main.gen.GenAspectOperatorDiv;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectOperatorGe;
import compilerxyz.main.gen.GenAspectOperatorGt;
import compilerxyz.main.gen.GenAspectOperatorLe;
import compilerxyz.main.gen.GenAspectOperatorLt;
import compilerxyz.main.gen.GenAspectOperatorMod;
import compilerxyz.main.gen.GenAspectOperatorMul;
import compilerxyz.main.gen.GenAspectOperatorNe;
import compilerxyz.main.gen.GenAspectOperatorNeg;
import compilerxyz.main.gen.GenAspectOperatorOr;
import compilerxyz.main.gen.GenAspectOperatorShl;
import compilerxyz.main.gen.GenAspectOperatorShr;
import compilerxyz.main.gen.GenAspectOperatorSub;
import compilerxyz.main.gen.GenAspectOperatorUshr;
import compilerxyz.main.gen.GenAspectOperatorXor;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6164L)
public record TypeIntConst() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "int";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectConstDynamic.AspectId)) {
            return Opt.some(new AspectConstDynamicOf(new TypeIntDynamic()));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_INT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectOperatorBitNot.AspectId)) {
            return Opt.some(AspectOperatorBitNotNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorNeg.AspectId)) {
            return Opt.some(AspectOperatorNegNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorGe.AspectId)) {
            return Opt.some(AspectOperatorGeNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorGt.AspectId)) {
            return Opt.some(AspectOperatorGtNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorLe.AspectId)) {
            return Opt.some(AspectOperatorLeNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorLt.AspectId)) {
            return Opt.some(AspectOperatorLtNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorNe.AspectId)) {
            return Opt.some(AspectOperatorNeNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorUshr.AspectId)) {
            return Opt.some(AspectOperatorUshrNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorShr.AspectId)) {
            return Opt.some(AspectOperatorShrNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorShl.AspectId)) {
            return Opt.some(AspectOperatorShlNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorXor.AspectId)) {
            return Opt.some(AspectOperatorXorNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorOr.AspectId)) {
            return Opt.some(AspectOperatorOrNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorAnd.AspectId)) {
            return Opt.some(AspectOperatorAndNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorMod.AspectId)) {
            return Opt.some(AspectOperatorModNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorDiv.AspectId)) {
            return Opt.some(AspectOperatorDivNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorMul.AspectId)) {
            return Opt.some(AspectOperatorMulNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorSub.AspectId)) {
            return Opt.some(AspectOperatorSubNumeric.INSTANCE);
        }
        if (key.equals(GenAspectOperatorAdd.AspectId)) {
            return Opt.some(AspectOperatorAddNumeric.INSTANCE);
        }
        if (key.equals(GenAspectInt.AspectId)) {
            return Opt.some(AspectIntIntConst.INSTANCE);
        }
        if (key.equals(GenAspectNumericConst.AspectId)) {
            return Opt.some(AspectNumericConstIntConst.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldIntConst.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicIntConst.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindConstValue.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactIntConst.INSTANCE);
        }
        if (key.equals(GenAspectOperatorNeg.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftIntConst.INSTANCE);
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
            var dv = MustCast.mustCast(Number.class, self);
            JsonValue data = new JsonValueNumberInt(dv.longValue());
            return EResult.ok(new Artifact.Inline(makeBody.apply(data)));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNumberInt n -> EResult.ok((int) n.value());
                default ->
                    EResult.err(loc, "TypeIntConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            };
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_INT_CONST_IDENTITY)
                .optIsSome();
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
        return Numerics.isDynamicNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kOpt = Numerics.kindOf(xx, destType);
        if (kOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeIntConst::typeCastTo: destination is not numeric");
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.INT_CONST_TYPE);
    }
}
