package compilerxyz.main.eval.value.float_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInt;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.AspectOperatorAdd;
import compilerxyz.main.gen.AspectOperatorAnd;
import compilerxyz.main.gen.AspectOperatorBitNot;
import compilerxyz.main.gen.AspectOperatorDiv;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.AspectOperatorGe;
import compilerxyz.main.gen.AspectOperatorGt;
import compilerxyz.main.gen.AspectOperatorLe;
import compilerxyz.main.gen.AspectOperatorLt;
import compilerxyz.main.gen.AspectOperatorMod;
import compilerxyz.main.gen.AspectOperatorMul;
import compilerxyz.main.gen.AspectOperatorNe;
import compilerxyz.main.gen.AspectOperatorNeg;
import compilerxyz.main.gen.AspectOperatorOr;
import compilerxyz.main.gen.AspectOperatorShl;
import compilerxyz.main.gen.AspectOperatorShr;
import compilerxyz.main.gen.AspectOperatorSub;
import compilerxyz.main.gen.AspectOperatorUshr;
import compilerxyz.main.gen.AspectOperatorXor;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInt;
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
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6172L)
public record TypeFloatConst()
        implements Type,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectNumericConst,
                AspectInt,
                AspectOperatorAdd,
                AspectOperatorSub,
                AspectOperatorMul,
                AspectOperatorDiv,
                AspectOperatorMod,
                AspectOperatorAnd,
                AspectOperatorOr,
                AspectOperatorXor,
                AspectOperatorShl,
                AspectOperatorShr,
                AspectOperatorUshr,
                AspectOperatorEq,
                AspectOperatorNe,
                AspectOperatorLt,
                AspectOperatorLe,
                AspectOperatorGt,
                AspectOperatorGe,
                AspectOperatorNeg,
                AspectOperatorBitNot {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public String typeDesc() {
        return "float";
    }

    @Override
    public NumericKind aspectNumericConstKind() {
        return NumericKind.FLOAT;
    }

    @Override
    public Number aspectNumericConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv;
        throw new IllegalArgumentException(
                "TypeFloatConst::get: expected float ValueObjectConst, got " + value.getClass());
    }

    @Override
    public long aspectIntGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof Number dv) return dv.longValue();
        throw new IllegalArgumentException(
                "TypeFloatConst::get: expected float ValueObjectConst, got " + value.getClass());
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_FLOAT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectNumericConst.AspectId)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInt.AspectId)
                || key.equals(GenAspectOperatorAdd.AspectId)
                || key.equals(GenAspectOperatorSub.AspectId)
                || key.equals(GenAspectOperatorMul.AspectId)
                || key.equals(GenAspectOperatorDiv.AspectId)
                || key.equals(GenAspectOperatorMod.AspectId)
                || key.equals(GenAspectOperatorAnd.AspectId)
                || key.equals(GenAspectOperatorOr.AspectId)
                || key.equals(GenAspectOperatorXor.AspectId)
                || key.equals(GenAspectOperatorShl.AspectId)
                || key.equals(GenAspectOperatorShr.AspectId)
                || key.equals(GenAspectOperatorUshr.AspectId)
                || key.equals(GenAspectOperatorEq.AspectId)
                || key.equals(GenAspectOperatorNe.AspectId)
                || key.equals(GenAspectOperatorLt.AspectId)
                || key.equals(GenAspectOperatorLe.AspectId)
                || key.equals(GenAspectOperatorGt.AspectId)
                || key.equals(GenAspectOperatorGe.AspectId)
                || key.equals(GenAspectOperatorNeg.AspectId)
                || key.equals(GenAspectOperatorBitNot.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.FLOAT_CONST;
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var dv = MustCast.mustCast(
                    Number.class,
                    MustCast.mustCast(ValueObjectConst.class, self).data());
            JsonValue data = new JsonValueNumber(dv.doubleValue());
            return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNumber n -> EResult.ok(new ValueObjectConst(new TypeFloatConst(), (float) n.value()));
                default ->
                    EResult.err(loc, "TypeFloatConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            };
        }
    };

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(
                    new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
        }
        return EResult.err(loc, "TypeFloatConst::bind: CodeValue has no compile-time value");
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return Numerics.constConsume(xx, loc, cv, NumericKind.FLOAT);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var dyn = new TypeFloatDynamic();
        var convertedRes = Numerics.castTo(xx, loc, self_, NumericKind.FLOAT, dyn);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        return dyn.aspectFieldField(xx, loc, convertedRes.requireValue("TypeFloatConst::field cast"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TypeFloatDynamic().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return new TypeFloatDynamic().aspectFieldTypeOpt(xx, key);
    }

    @Override
    public EResult<CodeValue> aspectOperatorAddAdd(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.ADD);
    }

    @Override
    public EResult<CodeValue> aspectOperatorSubSub(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SUB);
    }

    @Override
    public EResult<CodeValue> aspectOperatorMulMul(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.MUL);
    }

    @Override
    public EResult<CodeValue> aspectOperatorDivDiv(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.DIV);
    }

    @Override
    public EResult<CodeValue> aspectOperatorModMod(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.MOD);
    }

    @Override
    public EResult<CodeValue> aspectOperatorAndAnd(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.AND);
    }

    @Override
    public EResult<CodeValue> aspectOperatorOrOr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.OR);
    }

    @Override
    public EResult<CodeValue> aspectOperatorXorXor(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.XOR);
    }

    @Override
    public EResult<CodeValue> aspectOperatorShlShl(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHL);
    }

    @Override
    public EResult<CodeValue> aspectOperatorShrShr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHR);
    }

    @Override
    public EResult<CodeValue> aspectOperatorUshrUshr(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.USHR);
    }

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.EQ);
    }

    @Override
    public EResult<CodeValue> aspectOperatorNeNe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.NE);
    }

    @Override
    public EResult<CodeValue> aspectOperatorLtLt(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.LT);
    }

    @Override
    public EResult<CodeValue> aspectOperatorLeLe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.LE);
    }

    @Override
    public EResult<CodeValue> aspectOperatorGtGt(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.GT);
    }

    @Override
    public EResult<CodeValue> aspectOperatorGeGe(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        return Numerics.binary(xx, loc, base, reference, Numerics.Op.GE);
    }

    @Override
    public EResult<CodeValue> aspectOperatorNegNeg(ExprContext xx, QExprId loc, CodeValue base) {
        return Numerics.unary(xx, loc, base, Numerics.Unary.NEG);
    }

    @Override
    public EResult<CodeValue> aspectOperatorBitNotBitNot(ExprContext xx, QExprId loc, CodeValue base) {
        return Numerics.unary(xx, loc, base, Numerics.Unary.BITNOT);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_FLOAT_CONST_IDENTITY)
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
        return Numerics.isNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kOpt = Numerics.kindOf(xx, destType);
        if (kOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeFloatConst::typeCastTo: destination is not numeric");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.FLOAT_CONST_TYPE);
    }
}
