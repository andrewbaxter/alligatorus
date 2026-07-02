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
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectNumberConst;
import compilerxyz.gen.AspectNumberDynamic;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;

public record TypeNumberDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectInitialBind,
        AspectLift,
        AspectNumberDynamic,
        AspectOperatorAdd,
        AspectOperatorEq,
        AspectOperatorLt,
        AspectSet,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "dynamic number";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.NumberDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectNumberDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectOperatorLt.ID)
        || key.equals(AspectOperatorEq.ID)
        || key.equals(AspectOperatorAdd.ID)
        || key.equals(AspectSet.ID)
        || key.equals(AspectInitialBind.ID)
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
  public EResult<CodeValue> lift(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.err(
        loc,
        "TypeNumberDynamic::lift(CodeValue): staging-side lift wrapper not yet ported");
  }

  @Override
  public EResult<Value> liftRuntime(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof Double d)) {
      return EResult.err(
          loc,
          "TypeNumberDynamic::liftRuntime: expected java.lang.Double, got "
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
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeNumberDynamic::field: key has no compile-time value");
    }
    var keyType = keySome.value().valueTypeOf();
    var stringConstRes = AspectStringConst.get(xx, loc, keyType);
    if (stringConstRes.hasErrors()) {
      return stringConstRes.reerr();
    }
    var stringConst = stringConstRes.valueOrNull();
    if (stringConst == null) {
      throw new IllegalStateException(
          "AspectStringConst.get returned neither value nor errors");
    }
    String keyName = stringConst.get(keySome.value());
    var fieldTypeOpt = fieldTypeOpt(xx, new ConstKey.Str(keyName));
    if (!(fieldTypeOpt instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field '" + keyName + "' not found on dynamic number");
    }
    var jsName = keyName.equals("to_string") ? "toString" : keyName;
    var selfDyn = self_.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeNumberDynamic::field: self has no dynamic code");
    }
    var fieldUse = new CodeDynamic(loc, DynUtil.buildFieldGetSequence(selfDyn, jsName));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, fieldUse),
            Opt.some(
                new ValueDynamic(
                    new ValueDynamic.Source.Field(selfDyn, jsName), fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) {
      return Opt.none();
    }
    if (s.value().equals("to_string")) {

      return Opt.some(new TypeFunctionDynamic(new TypeNullDynamic(), new TypeStringDynamic()));
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> add(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOp(xx, loc, base, reference, OpKind.ADD);
  }

  @Override
  public EResult<CodeValue> eq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOp(xx, loc, base, reference, OpKind.EQ);
  }

  @Override
  public EResult<CodeValue> lt(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    return binOp(xx, loc, base, reference, OpKind.LT);
  }

  @Override
  public EResult<CodeValue> set(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  private enum OpKind {
    ADD,
    EQ,
    LT
  }

  private EResult<CodeValue> binOp(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference, OpKind op) {
    var baseCodeRes = toDynamic(xx, loc, base);
    if (baseCodeRes.hasErrors()) {
      return baseCodeRes.reerr();
    }
    var baseCode = baseCodeRes.valueOrNull();
    if (baseCode == null) {
      throw new IllegalStateException(
          "TypeNumberDynamic::binOp: toDynamic returned neither value nor errors");
    }
    var refCvRes = typeCastFrom(xx, loc, reference);
    if (refCvRes.hasErrors()) {
      return refCvRes.reerr();
    }
    var refCv = refCvRes.valueOrNull();
    if (refCv == null) {
      throw new IllegalStateException(
          "TypeNumberDynamic::binOp: typeCastFrom returned neither value nor errors");
    }
    var refCodeRes = toDynamic(xx, loc, refCv);
    if (refCodeRes.hasErrors()) {
      return refCodeRes.reerr();
    }
    var refCode = refCodeRes.valueOrNull();
    if (refCode == null) {
      throw new IllegalStateException(
          "TypeNumberDynamic::binOp: toDynamic returned neither value nor errors");
    }
    var elements = new TList<JavaBytecode.LoweredSequence.Element>();
    elements.add(new JavaBytecode.LoweredSequence.Code(baseCode));
    elements.add(new JavaBytecode.LoweredSequence.Bytecode(unboxDoubleBytecode()));
    elements.add(new JavaBytecode.LoweredSequence.Code(refCode));
    elements.add(new JavaBytecode.LoweredSequence.Bytecode(unboxDoubleBytecode()));
    switch (op) {
      case ADD -> {
        elements.add(
            new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.DADD)));
        elements.add(
            new JavaBytecode.LoweredSequence.Bytecode(
                JavaBytecodeUtils.callStaticMethod(
                    -1,
                    JvmConstants.INTNAME_BOXED_DOUBLE,
                    "valueOf",
                    JavaMethodDescriptor.fromParts(
                        JvmConstants.DESC_BOXED_DOUBLE, JvmConstants.DESC_DOUBLE))));
      }
      case EQ ->
          elements.add(
              new JavaBytecode.LoweredSequence.Bytecode(
                  compareToBoolBytecode(Opcodes.DCMPL, Opcodes.IFEQ)));
      case LT ->
          elements.add(
              new JavaBytecode.LoweredSequence.Bytecode(
                  compareToBoolBytecode(Opcodes.DCMPG, Opcodes.IFLT)));
    }
    Type resultType = op == OpKind.ADD ? this : new TypeBoolDynamic();
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(loc, new JavaBytecode.LoweredSequence(elements))),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), resultType))));
  }

  private static compilerxyz.eval.dynamic.JavaBytecodeSequence unboxDoubleBytecode() {
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_BOXED_DOUBLE)));
    seq.add(
        JavaBytecodeUtils.callMethod(
            -1,
            JvmConstants.INTNAME_BOXED_DOUBLE,
            "doubleValue",
            JavaMethodDescriptor.fromParts(JvmConstants.DESC_DOUBLE)));
    return seq;
  }

  private static compilerxyz.eval.dynamic.JavaBytecodeSequence compareToBoolBytecode(
      int cmpOpcode, int branchTrueOpcode) {
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    var trueLabel = new LabelNode();
    var endLabel = new LabelNode();
    seq.add(JavaBytecodeUtils.inst(cmpOpcode));
    seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(branchTrueOpcode, trueLabel)));
    seq.add(
        JavaBytecodeUtils.accessStaticField(
            -1, JvmConstants.INTNAME_BOXED_BOOL, "FALSE", JvmConstants.DESC_BOXED_BOOL));
    seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, endLabel)));
    seq.add(new JavaBytecodeInstruction.Obj(trueLabel));
    seq.add(
        JavaBytecodeUtils.accessStaticField(
            -1, JvmConstants.INTNAME_BOXED_BOOL, "TRUE", JvmConstants.DESC_BOXED_BOOL));
    seq.add(new JavaBytecodeInstruction.Obj(endLabel));
    return seq;
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
      var literal =
          new CodeDynamic(loc, new JavaBytecode.LoweredSequence(
                  JavaBytecodeUtils.jsonLiteral(new JsonValueNumber(numVal))));
      var entries = new TList<JavaBytecode.BlockEntry>();
      var existingDyn = cv.code().dynamic();
      if (existingDyn != null) {
        entries.add(new JavaBytecode.BlockEntry.Code(existingDyn));
      }
      entries.add(new JavaBytecode.BlockEntry.Code(literal));
      var block = new CodeDynamic(loc, new JavaBytecode.Block(entries));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, block),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
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
}
