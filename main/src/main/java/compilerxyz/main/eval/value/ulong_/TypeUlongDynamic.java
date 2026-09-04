package compilerxyz.main.eval.value.ulong_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectNumericDynamic;
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
import compilerxyz.main.gen.AspectSet;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
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
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6192L)
@BuiltinPath("types.ulong")
public record TypeUlongDynamic()
        implements Type,
                AspectInitialBindDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectDynamicBoxed,
                AspectField,
                AspectFieldUseDynamic,
                AspectLift,
                AspectNumericDynamic,
                AspectSet,
                AspectStackValueDynamic,
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

    private static final IMap<ConstKey, TypeUlongToStringDynamic> METHOD_FIELDS =
            IMap.ofEntries(new Pair<>(new ConstKey.Str("to_string"), new TypeUlongToStringDynamic()));

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "dynamic ulong";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_LONG;
    }

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUlongDynamicBox();
    }

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.ULONG;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_ULONG_DYNAMIC_IDENTITY)
                || key.equals(GenAspectNumericDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectDynamicBoxed.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectSet.AspectId)
                || key.equals(GenAspectLift.AspectId)
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.ULONG_DYNAMIC);
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.target(DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Long.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUlongConst(), d));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeUlongDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var fieldTypeOpt = aspectFieldTypeOpt(xx, ck);
        if (fieldTypeOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on dynamic ulong");
        }
        return EResult.ok(
                new CodeValue(self_.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, fieldTypeOpt.optUnwrap()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return METHOD_FIELDS.keys();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return METHOD_FIELDS.getOpt(key).optMap(t -> t);
    }

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        return DynUtil.dynamicSet(xx, loc, base, value);
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
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_ULONG_DYNAMIC_IDENTITY)
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
        return Numerics.castTo(xx, loc, cv, NumericKind.ULONG, this);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeUlongDynamic::typeCastTo: destination is not numeric");
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }
}
