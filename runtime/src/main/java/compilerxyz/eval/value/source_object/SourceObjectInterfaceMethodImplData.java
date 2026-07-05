package compilerxyz.eval.value.source_object;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.SourceInterfaceData;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import org.jspecify.annotations.Nullable;

public final class SourceObjectInterfaceMethodImplData {
  public final int interfaceMethodIndex;
  public @Nullable Value implExpr;
  public @Nullable String compiledClassName;
  public @Nullable byte[] compiledBytecode;
  public @Nullable BindingId bindingId;
  public @Nullable BindingId innerBindingId;
  public final TList<EvalContext.ExternalEntry> capturedExternals;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public @Nullable CodeDynamic compiledBodyCode;

  public @Nullable TList<BindingId.Local> compiledArgParamIds;

  public boolean compiledIsArgMethod;

  public SourceObjectInterfaceMethodImplData(int interfaceMethodIndex) {
    this.interfaceMethodIndex = interfaceMethodIndex;
    this.capturedExternals = new TList<>();
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId rootLoc,
      SourceInterfaceData iface,
      SourceObjectData dtData,
      Type underlyingType,
      String dataField) {
    if (compiledBodyCode != null) {
      return EResult.ok(null);
    }
    var implExprValue = implExpr;
    if (implExprValue == null) {

      return EResult.ok(null);
    }
    implExpr = null;
    if (!(implExprValue instanceof Expr implExprAst)) {
      throw new AssertionError(
          "Interface method impl is not an Expr: " + implExprValue);
    }
    var ifaceMethod = iface.methods.get(interfaceMethodIndex);
    var retT = ifaceMethod.retType;
    if (retT == null) {
      throw new AssertionError("Interface method missing return type");
    }
    if (bindingId == null) {
      throw new AssertionError("Interface method impl is not registered");
    }

    var innerVx = new EvalContext(xx.vx.ix);
    var innerTarget = new TargetDynamic(innerVx.nextLocalId);
    innerTarget.registerStaticFunctionDefs(xx.vx.ix.staticFunctionDefs);
    var selfId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    innerBindingId = selfId;
    var outerId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());

    var bodyScope = new Scope(null);
    bodyScope.values.putAll(Eval.bodyBuiltinScope(xx));
    var outerType = new TypeSourceObjectDynamic(dtData);
    var outerIb = AspectInitialBind.getMaybe(xx, outerType);
    if (outerIb instanceof Opt.Some<AspectInitialBind> outerIbSome) {
      bodyScope.values.put(new ConstKey.Str("outer"), outerIbSome.value().initialBind(outerId));
    }

    boolean isArgMethod = iface.builtinKind == BuiltinInterfaceKind.TYPE;
    TList<BindingId.Local> argParamIds;
    if (iface.builtinKind == BuiltinInterfaceKind.TYPE) {
      var argType = ifaceMethod.argRecordType;
      if (argType == null) {
        throw new AssertionError(
            "Builtin Type interface method missing arg_record_type");
      }
      var argIbResult = AspectInitialBind.get(xx, rootLoc, argType);
      if (argIbResult.hasErrors()) {
        return argIbResult.reerr();
      }
      var argIbAspect =
          argIbResult.requireValue(
              "SourceObjectInterfaceMethodImplData::compile arg InitialBind");
      bodyScope.values.put(
          new ConstKey.Str("arg"), argIbAspect.initialBind(selfId));
      argParamIds = TList.empty();
    } else if (iface.builtinKind == BuiltinInterfaceKind.BINDING
        || iface.builtinKind == BuiltinInterfaceKind.TARGET) {
      var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
      if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
        bodyScope.values.put(
            new ConstKey.Str("inner"), selfIbSome.value().initialBind(selfId));
      }
      var methodArgType = ifaceMethod.argType;
      if (methodArgType == null) {
        throw new AssertionError(
            "Builtin Binding/Target method missing arg_type");
      }
      var dataFieldNames = ifaceMethod.dataFieldNames;
      TList<Type> paramTypes;
      if (dataFieldNames.isEmpty()) {
        paramTypes = TList.empty();
      } else if (dataFieldNames.size() == 1) {
        paramTypes = TList.of(methodArgType);
      } else {
        var tupleAspectOpt = AspectTuple.getMaybe(xx, methodArgType);
        if (!(tupleAspectOpt instanceof Opt.Some<AspectTuple> tupleSome)) {
          throw new AssertionError(
              "Multi-field builtin method arg_type must be a Tuple, got: " + methodArgType.typeDesc());
        }
        paramTypes = TList.from(tupleSome.value().elementTypes());
      }
      if (paramTypes.size() != dataFieldNames.size()) {
        throw new AssertionError(
            "data_field_names ("
                + dataFieldNames.size()
                + ") does not match arg_type element count ("
                + paramTypes.size()
                + ")");
      }
      var ids = new TList<BindingId.Local>(dataFieldNames.size());
      var fields = new TOrderedMap<ConstKey, compilerxyz.eval.value.argrecord.ArgumentEntry>();
      for (int i = 0; i < dataFieldNames.size(); i++) {
        var pid = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        ids.add(pid);
        var paramType = paramTypes.get(i);
        var ibResult = AspectInitialBind.get(xx, rootLoc, paramType);
        if (ibResult.hasErrors()) {
          return ibResult.reerr();
        }
        var ibAspect =
            ibResult.requireValue(
                "SourceObjectInterfaceMethodImplData::compile param InitialBind");
        fields.put(
            new ConstKey.Str(dataFieldNames.get(i)),
            new compilerxyz.eval.value.argrecord.ArgumentEntry(
                ibAspect.initialBind(pid), paramType));
      }
      var argRecord = new ArgumentRecord(fields);
      bodyScope.values.put(
          new ConstKey.Str("arg"), new ArgumentRecordBinding(argRecord));
      argParamIds = ids;
    } else {

      var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
      if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
        bodyScope.values.put(
            new ConstKey.Str("inner"), selfIbSome.value().initialBind(selfId));
      }
      var argId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      var argT = ifaceMethod.argRecordType != null ? ifaceMethod.argRecordType : ifaceMethod.argType;
      if (argT != null) {
        var argIbMaybe = AspectInitialBind.getMaybe(xx, argT);
        if (argIbMaybe instanceof Opt.Some<AspectInitialBind> argIbSome) {
          bodyScope.values.put(new ConstKey.Str("arg"), argIbSome.value().initialBind(argId));
        }
      }
      argParamIds = TList.of(argId);
    }
    var methodXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, null);

    var implResult = ConstEval.constEval(methodXx, implExprAst);
    if (implResult.hasErrors()) {
      return implResult.reerr();
    }
    var implCv =
        implResult.requireValue("SourceObjectInterfaceMethodImplData::compile constEval");
    var convertedResult = Eval.typecheckConvert(methodXx, rootLoc, retT, implCv);
    if (convertedResult.hasErrors()) {
      return convertedResult.reerr();
    }
    var convertedCv =
        convertedResult.requireValue(
            "SourceObjectInterfaceMethodImplData::compile typecheckConvert");
    CodeDynamic bodyCode;
    if (convertedCv.value() instanceof Opt.Some<Value> someVal) {
      var valueType = someVal.value().valueTypeOf();
      var consumeResult = AspectConsumeDynamic.get(methodXx, rootLoc, valueType);
      if (consumeResult.hasErrors()) {
        return consumeResult.reerr();
      }
      var consume =
          consumeResult.requireValue(
              "SourceObjectInterfaceMethodImplData::compile consume aspect");
      var dynResult = consume.toDynamic(methodXx, rootLoc, convertedCv);
      if (dynResult.hasErrors()) {
        return dynResult.reerr();
      }
      bodyCode =
          dynResult.requireValue(
              "SourceObjectInterfaceMethodImplData::compile consume.toDynamic");
    } else {
      bodyCode = convertedCv.code().dynamic();
      if (bodyCode == null) {
        return EResult.err(rootLoc, "Interface method impl body produced no compile-time value");
      }
    }

    for (var entry : innerVx.definitionSets.entrySet()) {
      xx.vx.definitionSets.putIfAbsent(entry.getKey(), entry.getValue());
    }

    var captured = innerVx.externals;
    capturedExternals.addAll(captured);

    TList<BindingId.Local> emitArgIds;
    if (isArgMethod) {
      emitArgIds = TList.empty();
    } else if (iface.builtinKind == BuiltinInterfaceKind.BINDING
        || iface.builtinKind == BuiltinInterfaceKind.TARGET) {
      emitArgIds = argParamIds;
    } else {
      var methodArgT = ifaceMethod.argRecordType != null ? ifaceMethod.argRecordType : ifaceMethod.argType;
      emitArgIds = methodArgT != null ? argParamIds : TList.empty();
    }

    compiledBodyCode = bodyCode;
    compiledArgParamIds = emitArgIds;
    compiledIsArgMethod = isArgMethod;
    return EResult.ok(null);
  }

}
