package compilerxyz.eval.value.panic;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;

public record TypePanicDynamic()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public String typeDesc() {
    return "panic";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.PanicDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    Type stringType = new TypeStringDynamic();
    var erb = new Erb(loc);
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, stringType, argument));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var toDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, stringType));
    if (toDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCodeOpt = erb.mustEr(toDynamicOpt.get().toDynamic(xx, loc, converted));
    if (msgCodeOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCode = msgCodeOpt.get();
    var markerCode =
        new CodeDynamic(loc, new JavaBytecode.LoweredSequence(
                JavaBytecodeUtils.jsonLiteral(new JsonValueBoolean(true))));
    var fieldNames = new TList<String>();
    fieldNames.add(Eval.PANIC_MARKER_KEY);
    fieldNames.add("message");
    var fieldValues = new TList<CodeDynamic>();
    fieldValues.add(markerCode);
    fieldValues.add(msgCode);
    var panicObj =
        new CodeDynamic(loc, new JavaBytecode.LoweredSequence(
                JavaBytecodeUtils.instantiateLinkedHashMapWithFields(fieldNames, fieldValues)));
    var throwElements =
        JavaBytecodeUtils.instantiate(
            -1,
            JavaInternalName.of("compilerxyz/eval/dynamic/DynRuntime$DynException"),
            JavaMethodDescriptor.fromConstructorParts(JvmConstants.DESC_OBJECT),
            TList.of(new JavaBytecode.LoweredSequence.Code(panicObj)));
    throwElements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.ATHROW)));
    var throwCode = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(throwElements));
    return EResult.ok(new CodeValue(Code.of(loc, throwCode), Opt.none()));
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
