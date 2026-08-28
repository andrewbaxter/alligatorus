package compilerxyz.eval.value.short_;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.numeric.Numerics;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectDynamicBoxed;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectNumericDynamic;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorAnd;
import compilerxyz.gen.AspectOperatorBitNot;
import compilerxyz.gen.AspectOperatorDiv;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorGe;
import compilerxyz.gen.AspectOperatorGt;
import compilerxyz.gen.AspectOperatorLe;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.AspectOperatorMod;
import compilerxyz.gen.AspectOperatorMul;
import compilerxyz.gen.AspectOperatorNe;
import compilerxyz.gen.AspectOperatorNeg;
import compilerxyz.gen.AspectOperatorOr;
import compilerxyz.gen.AspectOperatorShl;
import compilerxyz.gen.AspectOperatorShr;
import compilerxyz.gen.AspectOperatorSub;
import compilerxyz.gen.AspectOperatorUshr;
import compilerxyz.gen.AspectOperatorXor;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectDynamicBoxed;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectFieldUseDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectNumericDynamic;
import compilerxyz.gen.GenAspectOperatorAdd;
import compilerxyz.gen.GenAspectOperatorAnd;
import compilerxyz.gen.GenAspectOperatorBitNot;
import compilerxyz.gen.GenAspectOperatorDiv;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorGe;
import compilerxyz.gen.GenAspectOperatorGt;
import compilerxyz.gen.GenAspectOperatorLe;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorMul;
import compilerxyz.gen.GenAspectOperatorNe;
import compilerxyz.gen.GenAspectOperatorNeg;
import compilerxyz.gen.GenAspectOperatorOr;
import compilerxyz.gen.GenAspectOperatorShl;
import compilerxyz.gen.GenAspectOperatorShr;
import compilerxyz.gen.GenAspectOperatorSub;
import compilerxyz.gen.GenAspectOperatorUshr;
import compilerxyz.gen.GenAspectOperatorXor;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.Value;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.IMap;
import compilerxyz.util.MustCast;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6167L)
@BuiltinPath("types.short")
public record TypeShortDynamic()
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

    private static final IMap<ConstKey, TypeShortToStringDynamic> METHOD_FIELDS =
            IMap.ofEntries(new Pair<>(new ConstKey.Str("to_string"), new TypeShortToStringDynamic()));

    @Override
    public String typeDesc() {
        return "dynamic short";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.DESC_SHORT;
    }

    @Override
    public Type aspectDynamicBoxedBoxedForm() {
        return new TypeShortDynamicBox();
    }

    @Override
    public NumericKind aspectNumericDynamicKind() {
        return NumericKind.SHORT;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SHORT_DYNAMIC_IDENTITY)
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
        return ExporterRoot.SHORT_DYNAMIC;
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
        var d = MustCast.mustCast(Short.class, externalHostValue.optUnwrap());
        return EResult.ok(new ValueObjectConst(new TypeShortConst(), d));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeShortDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var fieldTypeOpt = aspectFieldTypeOpt(xx, ck);
        if (fieldTypeOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on dynamic short");
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
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SHORT_DYNAMIC_IDENTITY)
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
        return Numerics.castTo(xx, loc, cv, NumericKind.SHORT, this);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, cv, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeShortDynamic::typeCastTo: destination is not numeric");
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeShortDynamic)) {
                return EResult.err(
                        loc,
                        "TypeShortDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
            }
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return switch (data) {
                case JsonValueNull n -> EResult.ok(new TypeShortDynamic());
                case JsonValueBoolean b ->
                    EResult.err(
                            loc,
                            "TypeShortDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueNumber n ->
                    EResult.err(
                            loc,
                            "TypeShortDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueString s ->
                    EResult.err(
                            loc,
                            "TypeShortDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueArray a ->
                    EResult.err(
                            loc,
                            "TypeShortDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
                case JsonValueObject o ->
                    EResult.err(
                            loc,
                            "TypeShortDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
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
