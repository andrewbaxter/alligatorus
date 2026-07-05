package compilerxyz.eval.value.number;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectDynamicBoxed;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectNumberConst;
import compilerxyz.gen.AspectNumberDynamic;
import compilerxyz.gen.AspectOperatorAdd;
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
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
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
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeNumberDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectDynamicBoxed,
        AspectField,
        AspectInitialBind,
        AspectElementUseDynamicBind,
        AspectLift,
        AspectNumberDynamic,
        AspectOperatorAdd,
        AspectOperatorEq,
        AspectOperatorLt,
        AspectSet,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_NUMBER_DYNAMIC_TYPE_ID; }


  private static final compilerxyz.eval.dynamic.JavaInternalName DYN_RUNTIME =
      compilerxyz.eval.dynamic.JavaInternalName.fromClass(compilerxyz.eval.dynamic.DynRuntime.class);
  private static final compilerxyz.eval.dynamic.JavaMethodDescriptor DESC_DBL_TO_STR =
      compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
          compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class),
          TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_DOUBLE));

  private static final Type EMPTY_TUPLE =
      new compilerxyz.eval.value.tuple.Tuple(compilerxyz.util.Opt.none(), new TList<>());

  private static final TMap<ConstKey, Type> METHOD_FIELDS =
      TMap.ofEntries(
          new Pair<>(
              new ConstKey.Str("to_string"),
              new compilerxyz.eval.value.function.TypeFunctionDynamic(
                  EMPTY_TUPLE,
                  new TypeStringDynamic(),
                  DYN_RUNTIME,
                  "numberToString",
                  DESC_DBL_TO_STR,
                  false)));

  @Override
  public String typeDesc() {
    return "dynamic number";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_DOUBLE;
  }

  @Override
  public Type boxedForm() {
    return new TypeNumberDynamicBox();
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.NumberDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectNumberDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectDynamicBoxed.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectOperatorLt.ID)
        || key.equals(AspectOperatorEq.ID)
        || key.equals(AspectOperatorAdd.ID)
        || key.equals(AspectSet.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectElementUseDynamicBind.ID)
        || key.equals(AspectLift.ID)
        || key.equals(AspectArtifact.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.NUMBER_DYNAMIC;
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Value> lift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof Double d)) {
      return EResult.err(
          loc,
          "TypeNumberDynamic::lift: expected java.lang.Double, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    return EResult.ok(new ValueNumberConst(d));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeNumberDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(fieldTypeOpt(xx, ck) instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on dynamic number");
    }
    return EResult.ok(
        new CodeValue(
            self_.code(),
            Opt.some(ValueDynamic.stack(fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return TList.from(METHOD_FIELDS.keySet());
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return METHOD_FIELDS.getOpt(key);
  }

  @Override
  public EResult<CodeValue> add(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DADD));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(this))));
  }

  @Override
  public EResult<CodeValue> eq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DCMPL));
    seq.add(compareToBoolBytecode(loc, Opcodes.IFEQ));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(new TypeBoolDynamic()))));
  }

  @Override
  public EResult<CodeValue> lt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var seqRes = binOpSeq(xx, loc, base, reference);
    if (seqRes.hasErrors()) return seqRes.reerr();
    var seq = seqRes.requireValue("binOpSeq");
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DCMPG));
    seq.add(compareToBoolBytecode(loc, Opcodes.IFLT));
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            Opt.some(ValueDynamic.stack(new TypeBoolDynamic()))));
  }

  @Override
  public EResult<CodeValue> set(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  private EResult<JavaBytecodeSequence> binOpSeq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var baseCodeRes = toDynamic(xx, loc, base);
    if (baseCodeRes.hasErrors()) return baseCodeRes.reerr();
    var refCvRes = typeCastFrom(xx, loc, reference);
    if (refCvRes.hasErrors()) return refCvRes.reerr();
    var refCodeRes = toDynamic(xx, loc, refCvRes.requireValue("typeCastFrom"));
    if (refCodeRes.hasErrors()) return refCodeRes.reerr();
    var seq = new JavaBytecodeSequence();
    seq.add(baseCodeRes.requireValue("toDynamic(base)"));
    seq.add(refCodeRes.requireValue("toDynamic(reference)"));
    return EResult.ok(seq);
  }

  public static CodeDynamic boxDouble(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Double"),
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Double")),
            TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_DOUBLE)));
  }

  public static CodeDynamic boxBoolean(QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Boolean"),
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                compilerxyz.eval.dynamic.JavaInternalName.of("java/lang/Boolean")),
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
    return EResult.ok(this);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return AspectNumberConst.getMaybe(xx, sourceType).optIsSome()
        || AspectNumberDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return AspectNumberDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeNumberDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    var numConstOpt = AspectNumberConst.getMaybe(xx, sourceType);
    if (numConstOpt instanceof Opt.Some<AspectNumberConst> numConstSome) {
      double numVal = numConstSome.value().get(some.value());
      var seq = new JavaBytecodeSequence();
      var existingDyn = cv.code().dynamic();
      if (existingDyn != null) {
        seq.add(existingDyn);
      }
      seq.add(JavaBytecodeUtils.literalDouble(loc, numVal));
      return EResult.ok(
          new CodeValue(
              Code.of(new CodeDynamic(loc, seq)),
              Opt.some(ValueDynamic.stack(this))));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(ValueDynamic.stack(this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(ValueDynamic.stack(this))));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeNumberDynamic)) {
            return EResult.err(
                loc, "TypeNumberDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeNumberDynamic());
          }
          return EResult.err(
              loc, "TypeNumberDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
        }
      };

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }


  @Override
  public compilerxyz.eval.Binding elementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return initialBind(dynamicKey);
  }

}
