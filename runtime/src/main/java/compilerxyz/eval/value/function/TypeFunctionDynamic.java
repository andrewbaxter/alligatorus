package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

@compilerxyz.annotations.BuiltinTypeId(7038L)
public record TypeFunctionDynamic(
    Type arg,
    Type ret,
    JavaInternalName klass,
    String methodName,
    boolean needsExprContext)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectFunction,
        AspectInitialBind,
        compilerxyz.gen.AspectLift,
        ExporterAny {

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    // The externalHostValue can be either an actual TypeFunctionDynamic (rare) or the JVM
    // receiver value for a method-value access. Either way, the compile-time type is the
    // TypeFunctionDynamic itself, so wrap `this` as the const value.
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType(), this));
  }

  @Override
  public TypeId hasTypeIdTypeId() { return new TypeId.Composite(GenTypeIds.TYPE_FUNCTION_DYNAMIC_TYPE_ID, TList.of(arg.hasTypeIdTypeId(), ret.hasTypeIdTypeId())); }


  public EResult<JavaMethodDescriptor> descriptor(ExprContext xx, QExprId loc) {
    var argDescs = new TList<JavaDataDescriptor>();
    if (needsExprContext) argDescs.add(JavaDataDescriptor.fromClass(ExprContext.class));
    var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, arg);
    if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
      for (var elem : tupleSome.value().aspectTupleElementTypes()) {
        var elemDescRes = jvmDescOf(xx, loc, elem);
        if (elemDescRes.hasErrors()) return elemDescRes.reerr();
        argDescs.add(elemDescRes.requireValue("TypeFunctionDynamic::descriptor tuple elem"));
      }
    } else {
      var recordOpt = compilerxyz.gen.AspectRecord.getMaybe(xx, arg);
      if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
        for (var entry : recordSome.value().aspectRecordFieldTypes().entrySet()) {
          var fieldDescRes = jvmDescOf(xx, loc, entry.getValue());
          if (fieldDescRes.hasErrors()) return fieldDescRes.reerr();
          argDescs.add(fieldDescRes.requireValue("TypeFunctionDynamic::descriptor record field"));
        }
      } else {
        var argDescRes = jvmDescOf(xx, loc, arg);
        if (argDescRes.hasErrors()) return argDescRes.reerr();
        argDescs.add(argDescRes.requireValue("TypeFunctionDynamic::descriptor single arg"));
      }
    }
    var retDescRes = jvmDescOf(xx, loc, ret);
    if (retDescRes.hasErrors()) return retDescRes.reerr();
    return EResult.ok(
        JavaMethodDescriptor.fromParts(
            retDescRes.requireValue("TypeFunctionDynamic::descriptor ret"), argDescs));
  }

  private static EResult<JavaDataDescriptor> jvmDescOf(ExprContext xx, QExprId loc, Type t) {
    var consumeRes = AspectConsumeDynamic.get(xx, loc, t);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    return EResult.ok(
        consumeRes
            .requireValue("TypeFunctionDynamic::jvmDescOf consume")
            .aspectConsumeDynamicJvmClass(xx));
  }

  public EResult<CodeDynamic> emitInvocation(
      ExprContext xx,
      QExprId loc,
      compilerxyz.util.TList<CodeDynamic> args) {
    var descRes = descriptor(xx, loc);
    if (descRes.hasErrors()) return descRes.reerr();
    var seq = new JavaBytecodeSequence();
    if (needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(xx.vx.evalRootInternalName),
              compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD,
              compilerxyz.eval.dynamic.EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    for (var argCode : args) {
      seq.add(argCode);
    }
    seq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc, klass, methodName, descRes.requireValue("TypeFunctionDynamic::emit desc")));
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic func";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeFunctionDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_FUNCTION_DYNAMIC_TYPE_ID, "function_dynamic");

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(compilerxyz.gen.AspectLift.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeFunctionDynamic::call converted arg");
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var argList = new compilerxyz.util.TList<CodeDynamic>();
    var argsDynRes =
        compilerxyz.eval.dynamic.DynUtil.destructurePositional(xx, loc, arg, converted);
    if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    for (var c : argsDynRes.requireValue("TypeFunctionDynamic::call positional args")) {
      argList.add(c);
    }
    var invocationRes = emitInvocation(xx, loc, argList);
    if (invocationRes.hasErrors()) return invocationRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeFunctionDynamic::call ret stack")
            .aspectStackValueDynamicToValue(
                xx, loc, invocationRes.requireValue("TypeFunctionDynamic::call invocation")));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeFunctionDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingFunctionDynamic(dynamicKey, this);
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
