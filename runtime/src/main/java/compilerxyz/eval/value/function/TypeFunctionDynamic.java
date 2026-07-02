package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectNull;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.ValueDynamic;

public record TypeFunctionDynamic(Type arg, Type ret)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "dynamic func";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(self.value() instanceof Opt.Some<Value> selfSome)) {
      return EResult.err(loc, "TypeFunctionDynamic::call: function has no compile-time value");
    }
    var funcType = selfSome.value().valueTypeOf();
    var funcConsumeRes = AspectConsumeDynamic.get(xx, loc, funcType);
    if (funcConsumeRes.hasErrors()) {
      return funcConsumeRes.reerr();
    }
    var funcCodeRes = funcConsumeRes.valueOrNull().toDynamic(xx, loc, self);
    if (funcCodeRes.hasErrors()) {
      return funcCodeRes.reerr();
    }
    var funcCode = funcCodeRes.valueOrNull();
    Opt<ValueDynamic.Source.MethodField> receiver = Opt.none();
    if (!(selfSome.value() instanceof ValueDynamic vd)) {
      throw new AssertionError(
          "TypeFunctionDynamic::call: function value is not ValueDynamic: " + selfSome.value());
    }
    if (vd.source() instanceof ValueDynamic.Source.MethodField mf) {
      receiver = Opt.some(mf);
    }
    return dynamicCall(xx, loc, funcCode, arg, ret, argument, receiver);
  }

  public static EResult<CodeValue> dynamicCall(
      ExprContext xx,
      QExprId loc,
      CodeDynamic funcCode,
      Type argType,
      Type retType,
      CodeValue argument) {
    return dynamicCall(xx, loc, funcCode, argType, retType, argument, Opt.none());
  }

  public static EResult<CodeValue> dynamicCall(
      ExprContext xx,
      QExprId loc,
      CodeDynamic funcCode,
      Type argType,
      Type retType,
      CodeValue argument,
      Opt<ValueDynamic.Source.MethodField> receiver) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "dynamic_call: argument has no compile-time value");
    }
    var argValueType = argSome.value().valueTypeOf();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) {
      return retStackRes.reerr();
    }
    var retStack = retStackRes.valueOrNull();
    var argTupleOpt = AspectTuple.getMaybe(xx, argValueType);
    if (argTupleOpt instanceof Opt.Some<AspectTuple> argTupleSome) {
      var argTuple = argTupleSome.value();
      var argTypeTupleOpt = AspectTuple.getMaybe(xx, argType);
      TList<Type> paramTypes;
      if (argTypeTupleOpt instanceof Opt.Some<AspectTuple> paramTupleSome) {
        paramTypes = paramTupleSome.value().elementTypes();
      } else {
        paramTypes = TList.of(argType);
      }
      var elements = argTuple.takeElements(argSome.value());
      if (elements.isEmpty()
          && paramTypes.size() == 1
          && AspectNull.getMaybe(xx, paramTypes.get(0)).optIsSome()) {
        paramTypes = new TList<>();
      }
      if (elements.size() != paramTypes.size()) {
        if (elements.isEmpty()
            && paramTypes.size() == 1
            && !(argTypeTupleOpt instanceof Opt.Some<AspectTuple>)) {
          var unitCv =
              new CodeValue(Code.none(loc), Opt.some(new Tuple(new TList<>())));
          var argConsumeRes = AspectConsumeDynamic.get(xx, loc, argType);
          if (argConsumeRes.hasErrors()) {
            return argConsumeRes.reerr();
          }
          var convertedRes = Eval.typecheckConvert(xx, loc, argType, unitCv);
          if (convertedRes.hasErrors()) {
            return convertedRes.reerr();
          }
          var argDynRes =
              argConsumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
          if (argDynRes.hasErrors()) {
            return argDynRes.reerr();
          }
          var argsOne = new TList<CodeDynamic>();
          argsOne.add(argDynRes.valueOrNull());
          var callOne = buildCallVariant(xx, loc, funcCode, argsOne, receiver);
          return EResult.ok(retStack.toValue(loc, callOne));
        }
        return EResult.err(
            loc,
            "Function expects " + paramTypes.size() + " arguments, got " + elements.size());
      }
      var errors = new TList<ExprErr>();
      var dynamicArgs = new TList<CodeDynamic>();
      for (int i = 0; i < elements.size(); i++) {
        var elem = elements.get(i);
        var paramType = paramTypes.get(i);
        elem.drainInto(errors);
        var cv = elem.valueOrNull();
        if (cv == null) {
          errors.add(
              new ExprErr(
                  loc,
                  TList.of(
                      new ExprSubErr.Opaque(
                          "Tuple element failed to evaluate"))));
          return EResult.errs(errors);
        }
        var convertedRes = Eval.typecheckConvert(xx, loc, paramType, cv);
        if (convertedRes.hasErrors()) {
          convertedRes.drainInto(errors);
          if (convertedRes.valueOrNull() == null) {
            return EResult.errs(errors);
          }
        }
        var elemConsumeRes = AspectConsumeDynamic.get(xx, loc, paramType);
        if (elemConsumeRes.hasErrors()) {
          elemConsumeRes.drainInto(errors);
          if (elemConsumeRes.valueOrNull() == null) {
            return EResult.errs(errors);
          }
        }
        var dynRes =
            elemConsumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
        if (dynRes.hasErrors()) {
          dynRes.drainInto(errors);
          if (dynRes.valueOrNull() == null) {
            return EResult.errs(errors);
          }
        }
        dynamicArgs.add(dynRes.valueOrNull());
      }
      var call = buildCallVariant(xx, loc, funcCode, dynamicArgs, receiver);
      if (!errors.isEmpty()) {
        return EResult.mixed(retStack.toValue(loc, call), errors);
      }
      return EResult.ok(retStack.toValue(loc, call));
    }
    var argConsumeRes = AspectConsumeDynamic.get(xx, loc, argType);
    if (argConsumeRes.hasErrors()) {
      return argConsumeRes.reerr();
    }
    var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
    if (convertedRes.hasErrors()) {
      return convertedRes.reerr();
    }
    var argDynRes = argConsumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
    if (argDynRes.hasErrors()) {
      return argDynRes.reerr();
    }
    var args = new TList<CodeDynamic>();
    args.add(argDynRes.valueOrNull());
    var call = buildCallVariant(xx, loc, funcCode, args, receiver);
    return EResult.ok(retStack.toValue(loc, call));
  }

  private static CodeDynamic buildCallVariant(
      ExprContext xx,
      QExprId loc,
      CodeDynamic funcCode,
      TList<CodeDynamic> args,
      Opt<ValueDynamic.Source.MethodField> receiver) {
    if (receiver instanceof Opt.Some<ValueDynamic.Source.MethodField> some) {
      var mf = some.value();
      return new CodeDynamic(
          loc, DynUtil.buildCallContextualMethodSequence(mf.base(), mf.key(), args));
    }
    return new CodeDynamic(loc, DynUtil.buildCallSequence(xx, funcCode, args));
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeFunctionDynamic::toDynamic: CodeValue has no dynamic code");
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
