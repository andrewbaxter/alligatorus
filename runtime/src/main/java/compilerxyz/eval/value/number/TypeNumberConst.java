package compilerxyz.eval.value.number;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectNumberConst;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeNumberConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectNumberConst,
        AspectOperatorAdd,
        AspectOperatorEq,
        AspectOperatorLt,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_NUMBER_CONST_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "number";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeNumberConst.class, compilerxyz.gen.GenTypeIds.TYPE_NUMBER_CONST_TYPE_ID, "number_const");

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectNumberConst.ID)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectOperatorAdd.ID)
        || key.equals(AspectOperatorEq.ID)
        || key.equals(AspectOperatorLt.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.NUMBER_CONST;
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
          if (self instanceof compilerxyz.eval.value.object_.ValueObjectConst v
              && v.data() instanceof Double dv) {
            data = new JsonValueNumber(dv);
          } else if (self instanceof TypeNumberConst) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeNumberConst::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull ignored -> EResult.ok(new TypeNumberConst());
            case JsonValueNumber n -> EResult.ok(
                new compilerxyz.eval.value.object_.ValueObjectConst(new TypeNumberConst(), n.value()));
            default -> EResult.err(
                loc, "TypeNumberConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(Code.none(loc), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeNumberConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Double dv) {
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(JavaBytecodeUtils.literalDouble(loc, dv));
      seq.add(TypeNumberDynamic.boxDouble(loc));
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    return EResult.err(loc, "TypeNumberConst::toDynamic: expected number ValueObjectConst");
  }

  @Override
  public double aspectNumberConstGet(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Double dv) return dv;
    throw new IllegalArgumentException(
        "TypeNumberConst::get: expected number ValueObjectConst, got " + value.getClass());
  }

  @Override
  public EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(
        xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(new TypeNumberConst(), a + b),
        "add");
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(
            new compilerxyz.eval.value.bool_.TypeBoolConst(), a == b),
        "eq");
  }

  @Override
  public EResult<CodeValue> aspectOperatorLtLt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(
            new compilerxyz.eval.value.bool_.TypeBoolConst(), a < b),
        "lt");
  }

  private EResult<CodeValue> binOpNumber(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue ref, BinNum f, String op) {
    var baseN = extractDouble(xx, loc, base, op);
    if (baseN.hasErrors()) return EResult.err(loc, opErrMsg(op, "base", baseN));
    var refN = extractDouble(xx, loc, ref, op);
    if (refN.hasErrors()) return EResult.err(loc, opErrMsg(op, "reference", refN));
    return EResult.ok(
        new CodeValue(
            base.code(),
            Opt.some(
                f.apply(
                    baseN.requireValue("extractDouble(base)"),
                    refN.requireValue("extractDouble(reference)")))));
  }

  private EResult<Double> extractDouble(ExprContext xx, QExprId loc, CodeValue cv, String op) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, op + ": CodeValue has no compile-time value");
    }
    var v = some.value();
    var typeAspect = AspectNumberConst.getMaybe(xx, v.valueType());
    if (!(typeAspect instanceof Opt.Some<AspectNumberConst> a)) {
      return EResult.err(loc, op + ": type " + v.valueType().typeDesc() + " is not numeric");
    }
    return EResult.ok(a.value().aspectNumberConstGet(v));
  }

  private static String opErrMsg(String op, String side, EResult<?> r) {
    var errs = r.errorsOrNull();
    return "TypeNumberConst::"
        + op
        + ": "
        + side
        + " — "
        + (errs == null || errs.isEmpty() ? "unknown" : errs.get(0).errors().get(0).toString());
  }

  @FunctionalInterface
  private interface BinNum {
    Value apply(double a, double b);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var dyn = new TypeNumberDynamic();
    var convertedRes = dyn.typeCastFrom(xx, loc, self_);
    if (convertedRes.hasErrors()) {
      return convertedRes.reerr();
    }
    return dyn.aspectFieldField(xx, loc, convertedRes.requireValue("TypeNumberDynamic::typeCastFrom"), key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeNumberDynamic().aspectFieldTypeOpt(xx, key);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeNumberConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
