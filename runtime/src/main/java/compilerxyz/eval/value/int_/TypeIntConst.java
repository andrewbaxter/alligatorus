package compilerxyz.eval.value.int_;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;

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
import compilerxyz.gen.AspectInt;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorMod;
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
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInt;
import compilerxyz.gen.GenAspectIntDynamic;
import compilerxyz.gen.GenAspectOperatorAdd;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.GenAspectPostDeserialize;

@BuiltinIdentity(uniqueId = 6113L)
public record TypeIntConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInt,
        AspectOperatorAdd,
        AspectOperatorMod,
        AspectOperatorEq,
        AspectOperatorLt,
        ExporterAny {


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "int";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeIntConst.class, "int_const");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_INT_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectInt.AspectId)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectOperatorAdd.AspectId)
        || key.equals(GenAspectOperatorMod.AspectId)
        || key.equals(GenAspectOperatorEq.AspectId)
        || key.equals(GenAspectOperatorLt.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.INT_CONST;
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
              && v.data() instanceof Long dv) {
            data = new JsonValueNumber(dv);
          } else if (self instanceof TypeIntConst) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeIntConst::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull ignored -> EResult.ok(new TypeIntConst());
            case JsonValueNumber n -> EResult.ok(
                new compilerxyz.eval.value.object_.ValueObjectConst(new TypeIntConst(), (long) n.value()));
            default -> EResult.err(
                loc, "TypeIntConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeIntConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Long dv) {
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(JavaBytecodeUtils.literalLong(loc, dv));
      seq.add(TypeIntDynamic.boxLong(loc));
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    return EResult.err(loc, "TypeIntConst::toDynamic: expected int ValueObjectConst");
  }

  @Override
  public long aspectIntGet(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Long dv) return dv;
    throw new IllegalArgumentException(
        "TypeIntConst::get: expected int ValueObjectConst, got " + value.getClass());
  }

  /**
   * A dynamic reference operand makes the whole operation dynamic: the constant base is lifted to
   * a dynamic int (carrying any code it holds) and the dynamic int aspect takes over.
   */
  private boolean referenceIsDynamic(ExprContext xx, CodeValue reference) {
    if (!(reference.value() instanceof Opt.Some<Value> some)) return false;
    var referenceType = some.value().valueType();
    return GenAspectInt.getMaybe(xx, referenceType).optIsNone()
        && GenAspectIntDynamic.getMaybe(xx, referenceType).optIsSome();
  }

  private EResult<CodeValue> dynamicBase(ExprContext xx, QExprId loc, CodeValue base) {
    return new TypeIntDynamic().typeCastFrom(xx, loc, base);
  }

  @Override
  public EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    if (referenceIsDynamic(xx, reference)) {
      var baseRes = dynamicBase(xx, loc, base);
      if (baseRes.hasErrors()) return baseRes.reerr();
      return new TypeIntDynamic()
          .aspectOperatorAddAdd(
              xx, loc, baseRes.requireValue("TypeIntConst::add dynamic base"), reference);
    }
    return binOpInt(
        xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(new TypeIntConst(), a + b),
        "add");
  }

  @Override
  public EResult<CodeValue> aspectOperatorModMod(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    if (referenceIsDynamic(xx, reference)) {
      var baseRes = dynamicBase(xx, loc, base);
      if (baseRes.hasErrors()) return baseRes.reerr();
      return new TypeIntDynamic()
          .aspectOperatorModMod(
              xx, loc, baseRes.requireValue("TypeIntConst::mod dynamic base"), reference);
    }
    return binOpInt(
        xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(new TypeIntConst(), a % b),
        "mod");
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    if (referenceIsDynamic(xx, reference)) {
      var baseRes = dynamicBase(xx, loc, base);
      if (baseRes.hasErrors()) return baseRes.reerr();
      return new TypeIntDynamic()
          .aspectOperatorEqEq(
              xx, loc, baseRes.requireValue("TypeIntConst::eq dynamic base"), reference);
    }
    return binOpInt(xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(
            new compilerxyz.eval.value.bool_.TypeBoolConst(), a == b),
        "eq");
  }

  @Override
  public EResult<CodeValue> aspectOperatorLtLt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    if (referenceIsDynamic(xx, reference)) {
      var baseRes = dynamicBase(xx, loc, base);
      if (baseRes.hasErrors()) return baseRes.reerr();
      return new TypeIntDynamic()
          .aspectOperatorLtLt(
              xx, loc, baseRes.requireValue("TypeIntConst::lt dynamic base"), reference);
    }
    return binOpInt(xx, loc, base, reference,
        (a, b) -> new compilerxyz.eval.value.object_.ValueObjectConst(
            new compilerxyz.eval.value.bool_.TypeBoolConst(), a < b),
        "lt");
  }

  private EResult<CodeValue> binOpInt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue ref, BinInt f, String op) {
    var baseN = extractLong(xx, loc, base, op);
    if (baseN.hasErrors()) return EResult.err(loc, opErrMsg(op, "base", baseN));
    var refN = extractLong(xx, loc, ref, op);
    if (refN.hasErrors()) return EResult.err(loc, opErrMsg(op, "reference", refN));
    return EResult.ok(
        new CodeValue(
            Code.concat(loc, base.code(), ref.code()),
            EvalOutcome.ofValue(
                f.apply(
                    baseN.requireValue("extractLong(base)"),
                    refN.requireValue("extractLong(reference)")))));
  }

  private EResult<Long> extractLong(ExprContext xx, QExprId loc, CodeValue cv, String op) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, op + ": CodeValue has no compile-time value");
    }
    var v = some.value();
    var typeAspect = GenAspectInt.getMaybe(xx, v.valueType());
    if (!(typeAspect instanceof Opt.Some<AspectInt> a)) {
      return EResult.err(loc, op + ": type " + v.valueType().typeDesc() + " is not integer");
    }
    return EResult.ok(a.value().aspectIntGet(v));
  }

  private static String opErrMsg(String op, String side, EResult<?> r) {
    var errs = r.errorsOrNull();
    return "TypeIntConst::"
        + op
        + ": "
        + side
        + " — "
        + (errs == null || errs.isEmpty() ? "unknown" : errs.get(0).errors().get(0).toString());
  }

  @FunctionalInterface
  private interface BinInt {
    Value apply(long a, long b);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var dyn = new TypeIntDynamic();
    var convertedRes = dyn.typeCastFrom(xx, loc, self_);
    if (convertedRes.hasErrors()) {
      return convertedRes.reerr();
    }
    return dyn.aspectFieldField(xx, loc, convertedRes.requireValue("TypeIntDynamic::typeCastFrom"), key);
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeIntDynamic().aspectFieldTypeOpt(xx, key);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeIntConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
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
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_INT_CONST_IDENTITY).optIsSome();
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
