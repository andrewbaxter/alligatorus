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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectNumberConst;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.bool_.ValueBoolConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;

public record TypeNumberConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectNumberConst,
        AspectOperatorAdd,
        AspectOperatorEq,
        AspectOperatorLt,
        ExporterAny {

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "number";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.NumberConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectNumberConst.ID)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
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
  public ArtifactExporter exporter() {
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
          if (self instanceof ValueNumberConst v) {
            data = new JsonValueNumber(v.value());
          } else if (self instanceof TypeNumberConst || self instanceof ValueTypeNumberConst) {
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
            case JsonValueNull ignored -> EResult.ok(new ValueTypeNumberConst());
            case JsonValueNumber n -> EResult.ok(new ValueNumberConst(n.value()));
            default -> EResult.err(
                loc, "TypeNumberConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(Code.none(loc), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeNumberConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof ValueNumberConst v) {
      return EResult.ok(JavaBytecodeUtils.literalDouble(loc, v.value()));
    }
    return EResult.err(loc, "TypeNumberConst::toDynamic: expected ValueNumberConst");
  }

  @Override
  public double get(Value value) {
    if (value instanceof ValueNumberConst v) return v.value();
    throw new IllegalArgumentException(
        "TypeNumberConst::get: expected ValueNumberConst, got " + value.getClass());
  }

  @Override
  public EResult<CodeValue> add(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(
        xx, loc, base, reference, (a, b) -> new ValueNumberConst(a + b), "add");
  }

  @Override
  public EResult<CodeValue> eq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(xx, loc, base, reference, (a, b) -> new ValueBoolConst(a == b), "eq");
  }

  @Override
  public EResult<CodeValue> lt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOpNumber(xx, loc, base, reference, (a, b) -> new ValueBoolConst(a < b), "lt");
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
    var typeAspect = AspectNumberConst.getMaybe(xx, v.valueTypeOf());
    if (!(typeAspect instanceof Opt.Some<AspectNumberConst> a)) {
      return EResult.err(loc, op + ": type " + v.valueTypeOf().typeDesc() + " is not numeric");
    }
    return EResult.ok(a.value().get(v));
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
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var dyn = new TypeNumberDynamic();
    var convertedRes = dyn.typeCastFrom(xx, loc, self_);
    if (convertedRes.hasErrors()) {
      return convertedRes.reerr();
    }
    return dyn.field(xx, loc, convertedRes.requireValue("TypeNumberDynamic::typeCastFrom"), key);
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeNumberDynamic().fieldTypeOpt(xx, key);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeNumberConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueTypeNumberConst());
  }
}
