package compilerxyz.eval.value.byte_;

import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.numeric.Numerics;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectNumericConst;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectInt;
import compilerxyz.gen.GenAspectInt;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectNumericConst;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.GenAspectOperatorAdd;
import compilerxyz.gen.AspectOperatorSub;
import compilerxyz.gen.GenAspectOperatorSub;
import compilerxyz.gen.AspectOperatorMul;
import compilerxyz.gen.GenAspectOperatorMul;
import compilerxyz.gen.AspectOperatorDiv;
import compilerxyz.gen.GenAspectOperatorDiv;
import compilerxyz.gen.AspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.AspectOperatorAnd;
import compilerxyz.gen.GenAspectOperatorAnd;
import compilerxyz.gen.AspectOperatorOr;
import compilerxyz.gen.GenAspectOperatorOr;
import compilerxyz.gen.AspectOperatorXor;
import compilerxyz.gen.GenAspectOperatorXor;
import compilerxyz.gen.AspectOperatorShl;
import compilerxyz.gen.GenAspectOperatorShl;
import compilerxyz.gen.AspectOperatorShr;
import compilerxyz.gen.GenAspectOperatorShr;
import compilerxyz.gen.AspectOperatorUshr;
import compilerxyz.gen.GenAspectOperatorUshr;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.AspectOperatorNe;
import compilerxyz.gen.GenAspectOperatorNe;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.AspectOperatorLe;
import compilerxyz.gen.GenAspectOperatorLe;
import compilerxyz.gen.AspectOperatorGt;
import compilerxyz.gen.GenAspectOperatorGt;
import compilerxyz.gen.AspectOperatorGe;
import compilerxyz.gen.GenAspectOperatorGe;
import compilerxyz.gen.AspectOperatorNeg;
import compilerxyz.gen.GenAspectOperatorNeg;
import compilerxyz.gen.AspectOperatorBitNot;
import compilerxyz.gen.GenAspectOperatorBitNot;
import compilerxyz.eval.Code;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JvmConstants;

@BuiltinLight(uniqueId = 6149L)
public record TypeByteConst()
    implements
        Type,
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
        AspectOperatorBitNot,
        ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(
        JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "byte";
  }

  @Override
  public NumericKind aspectNumericConstKind() {
    return NumericKind.BYTE;
  }

  @Override
  public Number aspectNumericConstGet(Value value) {
    if (value instanceof ValueObjectConst v
        && v.data() instanceof Number dv) return dv;
    throw new IllegalArgumentException(
        "TypeByteConst::get: expected byte ValueObjectConst, got " + value.getClass());
  }

  @Override
  public long aspectIntGet(Value value) {
    if (value instanceof ValueObjectConst v
        && v.data() instanceof Number dv) return dv.longValue();
    throw new IllegalArgumentException(
        "TypeByteConst::get: expected byte ValueObjectConst, got " + value.getClass());
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_BYTE_CONST_IDENTITY)) {
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
        || key.equals(GenAspectOperatorBitNot.AspectId)
    ) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.BYTE_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          JsonValue data;
          if (self instanceof ValueObjectConst v
              && v.data() instanceof Number dv) {
            data = new JsonValueNumber(dv.doubleValue());
          } else if (self instanceof TypeByteConst) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeByteConst::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull ignored -> EResult.ok(new TypeByteConst());
            case JsonValueNumber n -> EResult.ok(
                new ValueObjectConst(new TypeByteConst(), (byte) n.value()));
            default -> EResult.err(
                loc, "TypeByteConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsSome()) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
    }
    return EResult.err(loc, "TypeByteConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return Numerics.constConsume(xx, loc, cv, NumericKind.BYTE);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var dyn = new TypeByteDynamic();
    var convertedRes = Numerics.castTo(xx, loc, self_, NumericKind.BYTE, dyn);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    return dyn.aspectFieldField(xx, loc, convertedRes.requireValue("TypeByteConst::field cast"), key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TypeByteDynamic().aspectFieldKeys(xx);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeByteDynamic().aspectFieldTypeOpt(xx, key);
  }

  @Override
  public EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.ADD);
  }
  @Override
  public EResult<CodeValue> aspectOperatorSubSub(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.SUB);
  }
  @Override
  public EResult<CodeValue> aspectOperatorMulMul(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.MUL);
  }
  @Override
  public EResult<CodeValue> aspectOperatorDivDiv(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.DIV);
  }
  @Override
  public EResult<CodeValue> aspectOperatorModMod(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.MOD);
  }
  @Override
  public EResult<CodeValue> aspectOperatorAndAnd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.AND);
  }
  @Override
  public EResult<CodeValue> aspectOperatorOrOr(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.OR);
  }
  @Override
  public EResult<CodeValue> aspectOperatorXorXor(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.XOR);
  }
  @Override
  public EResult<CodeValue> aspectOperatorShlShl(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHL);
  }
  @Override
  public EResult<CodeValue> aspectOperatorShrShr(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.SHR);
  }
  @Override
  public EResult<CodeValue> aspectOperatorUshrUshr(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.USHR);
  }
  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.EQ);
  }
  @Override
  public EResult<CodeValue> aspectOperatorNeNe(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.NE);
  }
  @Override
  public EResult<CodeValue> aspectOperatorLtLt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.LT);
  }
  @Override
  public EResult<CodeValue> aspectOperatorLeLe(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.LE);
  }
  @Override
  public EResult<CodeValue> aspectOperatorGtGt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return Numerics.binary(xx, loc, base, reference, Numerics.Op.GT);
  }
  @Override
  public EResult<CodeValue> aspectOperatorGeGe(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
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
    return EResult.ok(new TypeByteConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BYTE_CONST_IDENTITY).optIsSome();
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
    return EResult.err(loc, "TypeByteConst::typeCastTo: destination is not numeric");
  }
}
