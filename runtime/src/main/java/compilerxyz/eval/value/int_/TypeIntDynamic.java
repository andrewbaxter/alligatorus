package compilerxyz.eval.value.int_;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectDynamicBoxed;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectInt;
import compilerxyz.gen.AspectIntDynamic;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorMod;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
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
import compilerxyz.gen.GenAspectInt;
import compilerxyz.gen.GenAspectIntDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenAspectOperatorAdd;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectStackValueDynamic;

@Builtin(uniqueId = 6023L)
@BuiltinPath("types.int")
public record TypeIntDynamic()
    implements
        Type,
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
        AspectIntDynamic,
        AspectOperatorAdd,
        AspectOperatorMod,
        AspectOperatorEq,
        AspectOperatorLt,
        AspectSet,
        AspectStackValueDynamic,
        ExporterAny {


  private static final TMap<ConstKey, Type> METHOD_FIELDS =
      TMap.ofEntries(
          new Pair<>(new ConstKey.Str("to_string"), new TypeIntToStringDynamic()));

  @Override
  public String typeDesc() {
    return "dynamic int";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_LONG;
  }

  @Override
  public Type aspectDynamicBoxedBoxedForm() {
    return new TypeIntDynamicBox();
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeIntDynamic.class, "int_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_INT_DYNAMIC_IDENTITY)
        || key.equals(GenAspectIntDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectDynamicBoxed.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(GenAspectOperatorLt.AspectId)
        || key.equals(GenAspectOperatorEq.AspectId)
        || key.equals(GenAspectOperatorAdd.AspectId)
        || key.equals(GenAspectOperatorMod.AspectId)
        || key.equals(GenAspectSet.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectArtifact.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.INT_DYNAMIC;
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, ValueDynamicSource source) {
    return EResult.ok(
        new CodeValue(
            Code.of(loc, DynUtil.renderSource(xx, loc, source)),
            EvalOutcome.ofValue(new ValueDynamic(source, this))));
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    var d = compilerxyz.util.MustCast.mustCast(Long.class, externalHostValue);
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(new TypeIntConst(), d));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 2);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeIntDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(aspectFieldTypeOpt(xx, ck) instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on dynamic int");
    }
    return EResult.ok(
        new CodeValue(
            self_.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(fieldTypeSome.value(), 2))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(METHOD_FIELDS.keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return METHOD_FIELDS.getOpt(key);
  }

  @Override
  public EResult<CodeValue> aspectOperatorAddAdd(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LADD));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 2))));
  }

  @Override
  public EResult<CodeValue> aspectOperatorModMod(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LREM));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 2))));
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LCMP));
    seq.add(compareToBoolBytecode(loc, Opcodes.IFEQ));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(new TypeBoolDynamic(), 2))));
  }

  @Override
  public EResult<CodeValue> aspectOperatorLtLt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LCMP));
    seq.add(compareToBoolBytecode(loc, Opcodes.IFLT));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(new TypeBoolDynamic(), 2))));
  }

  @Override
  public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  private EResult<JavaBytecodeSequence> binOpSeq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var baseCodeRes = aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseCodeRes.hasErrors()) return baseCodeRes.reerr();
    var refCvRes = typeCastFrom(xx, loc, reference);
    if (refCvRes.hasErrors()) return refCvRes.reerr();
    var refCodeRes = aspectConsumeDynamicToDynamic(xx, loc, refCvRes.requireValue("typeCastFrom"));
    if (refCodeRes.hasErrors()) return refCodeRes.reerr();
    var seq = new JavaBytecodeSequence();
    seq.add(baseCodeRes.requireValue("aspectConsumeDynamicToDynamic(base)"));
    seq.add(refCodeRes.requireValue("aspectConsumeDynamicToDynamic(reference)"));
    return EResult.ok(seq);
  }

  public static CodeDynamic boxLong(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(Long.class),
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                compilerxyz.eval.dynamic.JavaInternalName.fromClass(Long.class)),
            TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_LONG)));
  }

  public static CodeDynamic boxBoolean(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(Boolean.class),
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                compilerxyz.eval.dynamic.JavaInternalName.fromClass(Boolean.class)),
            TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_BOOL)));
  }

  private static CodeDynamic compareToBoolBytecode(QExprId loc, int branchTrueOpcode) {
    var seq = new JavaBytecodeSequence();
    var trueLabel = new LabelNode();
    var endLabel = new LabelNode();
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(branchTrueOpcode, trueLabel))));
    seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, endLabel))));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(trueLabel)));
    seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 1));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(endLabel)));
    return new CodeDynamic(loc, seq);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_INT_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return GenAspectInt.getMaybe(xx, sourceType).optIsSome()
        || GenAspectIntDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return GenAspectIntDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeIntDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    var numConstOpt = GenAspectInt.getMaybe(xx, sourceType);
    if (numConstOpt instanceof Opt.Some<AspectInt> numConstSome) {
      long numVal = numConstSome.value().aspectIntGet(some.value());
      var seq = new JavaBytecodeSequence();
      var existingDyn = cv.code().dynamic();
      if (existingDyn != null) {
        seq.add(existingDyn);
      }
      seq.add(JavaBytecodeUtils.literalLong(loc, numVal));
      return EResult.ok(
          new CodeValue(
              Code.of(new CodeDynamic(loc, seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(this, 2))));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 2))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 2))));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeIntDynamic)) {
            return EResult.err(
                loc, "TypeIntDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeIntDynamic());
          }
          return EResult.err(
              loc, "TypeIntDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
        }
      };

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }


  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.binding.BindingDynamicBinding(dynamicKey, this);
  }

}
