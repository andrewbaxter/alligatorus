package compilerxyz.eval.value.list;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.gen.TargetBindSub;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.Iterator;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectIterable;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectStringDynamic;

@BuiltinLight(uniqueId = 6117L)
public final class TypeListDynamic
    implements
        Type,
        compilerxyz.gen.AspectFieldUseDynamic,
        compilerxyz.gen.AspectDynamicConst,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectDropDynamicLight,
        AspectField,
        compilerxyz.gen.AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {

  public final Type elementType;
  public final compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder();

  public TypeListDynamic(Type elementType) {
    this.elementType = elementType;
  }

  public Type elementType() {
    return elementType;
  }



  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.value.ValueDynamicSource source) {
    return compilerxyz.eval.EResult.ok(
        new compilerxyz.eval.CodeValue(
            compilerxyz.eval.Code.of(
                loc, compilerxyz.eval.dynamic.DynUtil.renderSource(xx, loc, source)),
            compilerxyz.eval.EvalOutcome.ofValue(
                new compilerxyz.eval.value.ValueDynamic(source, this))));
  }

  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ")";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectDropDynamic.AspectId)) {
      return GenAspectDropDynamic.getMaybe(xx, elementType).optIsSome()
          ? Opt.some(this)
          : Opt.none();
    }
    if (key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.GenAspectDynamicConst.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectIterable.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }


  @Override
  public Type aspectDynamicConstConstForm() {
    return new TypeListConst(elementType);
  }

  private sealed interface FieldSpec {
    Type fieldType();

    record Factory(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }

    record Method(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }
  }

  private TMap<ConstKey, FieldSpec> fieldSpecs(ExprContext xx) {
    var m = TMap.<ConstKey, FieldSpec>of();
    m.put(new ConstKey.Str("new"), new FieldSpec.Factory(new TypeListNewDynamic(elementType)));
    m.put(new ConstKey.Str("push"), new FieldSpec.Method(new TypeListPushDynamic(elementType)));
    if (GenAspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
      m.put(
          new ConstKey.Str("join"),
          new FieldSpec.Method(new TypeListJoinDynamic(elementType)));
    }
    return m;
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeListDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var spec = fieldSpecs(xx).getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on list");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(
              new CodeValue(self.code(), EvalOutcome.ofValue(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), f.type()))));
      case FieldSpec.Method m ->
          EResult.ok(
              new CodeValue(
                  self.code(),
                  EvalOutcome.ofValue(ValueDynamic.stack(m.type(), 1))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs(xx).keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs(xx).getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldType());
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
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeListDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type aspectIterableElementType() {
    return elementType;
  }

  @Override
  public EResult<CodeValue> aspectIterableForeach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = GenAspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
    var baseDynRes =
        baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) return baseDynRes.reerr();
    var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

    var iterableName = JavaInternalName.fromClass(Iterable.class);
    var iteratorName = JavaInternalName.fromClass(Iterator.class);
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(baseDyn);
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    var localBinding = compilerxyz.util.MustCast.mustCast(BindingId.Local.class, elementScopeId);

    var elementDecode = elementDecodeCast(xx, loc);
    Code bodyCode;
    if (body.value().optIsSome()) {
      var dropRes = xx.target.targetDrop(xx, loc, body);
      if (dropRes.hasErrors()) return dropRes.reerr();
      var dropCode = dropRes.requireValue("TypeListDynamic::foreach body drop");
      var bodyDyn = body.code().dynamic();
      var dropDyn = dropCode.dynamic();
      if (bodyDyn == null && dropDyn == null) {
        bodyCode = Code.none(loc);
      } else {
        var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
        if (bodyDyn != null) seq.add(bodyDyn);
        if (dropDyn != null) seq.add(dropDyn);
        bodyCode = Code.of(loc, new CodeDynamic(loc, seq));
      }
    } else {
      bodyCode = body.code();
    }
    return EResult.ok(
        DynUtil.foreachSkeleton(xx,
            loc, iteratorSetup, elementDecode, localBinding, Opcodes.ASTORE, bodyCode));
  }

  private CodeDynamic elementDecodeCast(ExprContext xx, QExprId loc) {
    var decodeSeq = new JavaBytecodeSequence();
    var elemConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, elementType);
    if (elemConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> elemConsumeSome) {
      var elemDesc = elemConsumeSome.value().aspectConsumeDynamicJvmClass(xx);
      var d = elemDesc.value();
      if (!d.equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())) {
        char c = d.charAt(0);
        if (c == 'L' || c == '[') {
          decodeSeq.add(JavaBytecodeUtils.cast(loc, elemDesc));
        }
      }
    }
    return new CodeDynamic(loc, decodeSeq);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    var elemDropRes = GenAspectDropDynamic.get(xx, loc, elementType);
    if (elemDropRes.hasErrors()) return elemDropRes.reerr();
    var elemDrop = elemDropRes.requireValue("TypeListDynamic::drop element AspectDropDynamic");
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof compilerxyz.eval.value.ValueDynamic vd)) {
      return EResult.ok(Code.none(loc));
    }
    var elementSlot =
        new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var loadElement =
        new CodeDynamic(
            loc,
            new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(Opcodes.ALOAD, elementSlot.key()));
    var elementCv =
        new CodeValue(
            Code.of(loc, loadElement),
            EvalOutcome.ofValue(ValueDynamic.stack(elementType, 1)));
    var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
    if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
    var elemDropCode = elemDropCodeRes.requireValue("TypeListDynamic::drop element drop");

    var iterableName = JavaInternalName.fromClass(Iterable.class);
    var iteratorName = JavaInternalName.fromClass(Iterator.class);
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(vd.source().realize(xx, loc));
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    var elementDecode = elementDecodeCast(xx, loc);

    var loop =
        DynUtil.foreachSkeleton(xx,
            loc, iteratorSetup, elementDecode, elementSlot, Opcodes.ASTORE, elemDropCode);
    var loopCode = loop.code().dynamic();
    var finalSeq = new JavaBytecodeSequence();
    if (loopCode != null) finalSeq.add(loopCode);
    finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, finalSeq)));
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_LIST_DYNAMIC_IDENTITY, TypeListDynamic.class) instanceof Opt.Some<TypeListDynamic> oSome
        && elementType.typeIsEqual(xx, oSome.value().elementType);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return typeIsEqual(xx, sourceType);
  }
  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.binding.BindingDynamicBinding(dynamicKey, this);
  }
}
