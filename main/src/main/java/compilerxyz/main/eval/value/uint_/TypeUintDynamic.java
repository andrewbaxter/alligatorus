package compilerxyz.main.eval.value.uint_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
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
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBind;
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
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
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
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6188L)
@BuiltinPath("types.uint")
public record TypeUintDynamic()
        implements Type,
                AspectArtifact,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectDynamicBoxed,
                AspectField,
                AspectFieldUseDynamic,
                AspectInitialBind,
                AspectElementUseDynamicBind,
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
                AspectOperatorBitNot,
                ExporterAny {

    private static final IMap<ConstKey, TypeUintToStringDynamic> METHOD_FIELDS =
            IMap.ofEntries(new Pair<>(new ConstKey.Str("to_string"), new TypeUintToStringDynamic()));

    @Override
    public String typeDesc() {
        return "dynamic uint";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_INT;
    }

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeUintDynamicBox();
    }

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.UINT;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_UINT_DYNAMIC_IDENTITY)
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
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectArtifact.AspectId)
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
        return ExporterRoot.UINT_DYNAMIC;
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.of(loc, DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var d = MustCast.mustCast(Integer.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeUintConst(), d));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeUintDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var fieldTypeOpt = aspectFieldTypeOpt(xx, ck);
        if (fieldTypeOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on dynamic uint");
        }
        return EResult.ok(
                new CodeValue(self_.code(), EvalOutcome.ofValue(ValueDynamic.stack(fieldTypeOpt.optUnwrap(), 1))));
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
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UINT_DYNAMIC_IDENTITY)
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
        return Numerics.castTo(xx, loc, cv, NumericKind.UINT, this);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeUintDynamic::typeCastTo: destination is not numeric");
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeUintDynamic)) {
                return EResult.err(
                        loc,
                        "TypeUintDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
            }
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNull n -> EResult.ok(new TypeUintDynamic());
                case JsonValueBoolean b ->
                    EResult.err(
                            loc,
                            "TypeUintDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueNumber n ->
                    EResult.err(
                            loc,
                            "TypeUintDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueString s ->
                    EResult.err(
                            loc,
                            "TypeUintDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueArray a ->
                    EResult.err(
                            loc,
                            "TypeUintDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueObject o ->
                    EResult.err(
                            loc,
                            "TypeUintDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
            };
        }
    };

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(xx, loc, cv);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
