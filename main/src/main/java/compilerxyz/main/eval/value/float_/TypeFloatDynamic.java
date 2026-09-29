package compilerxyz.main.eval.value.float_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
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
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectNumericDynamic;
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
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;

@BuiltinFull(uniqueId = 6171L)
@BuiltinPath("types.float")
public record TypeFloatDynamic() implements Aspect, Type {

    static final IMap<ConstKey, TypeFloatToStringDynamic> METHOD_FIELDS =
            IMap.ofEntries(new Pair<>(new ConstKey.Str("to_string"), new TypeFloatToStringDynamic()));

    @Override
    public String typeDesc() {
        return "dynamic float";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicFloatDynamic(this));
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
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectNumericDynamic.AspectId)) {
            return Opt.some(AspectNumericDynamicFloatDynamic.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftFloatDynamic.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicFloatDynamic(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldFloatDynamic.INSTANCE);
        }
        if (key.equals(GenAspectDynamicBoxed.AspectId)) {
            return Opt.some(AspectDynamicBoxedFloatDynamic.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicFloatDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_FLOAT_DYNAMIC_IDENTITY)
                || key.equals(GenAspectOperatorNeg.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.FLOAT_DYNAMIC);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_FLOAT_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return Numerics.isNumeric(xx, sourceType);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return Numerics.isNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return Numerics.castTo(xx, loc, cv, NumericKind.FLOAT, this);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeFloatDynamic::typeCastTo: destination is not numeric");
    }
}
