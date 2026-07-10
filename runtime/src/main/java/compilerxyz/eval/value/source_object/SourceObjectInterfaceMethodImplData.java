package compilerxyz.eval.value.source_object;

import compilerxyz.gen.Binding;
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
import compilerxyz.eval.value.source_interface.InterfaceData;
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
  public @Nullable BindingId outerBindingId;
  public final TList<EvalContext.ExternalEntry> capturedExternals;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public @Nullable CodeDynamic compiledBodyCode;

  public @Nullable TList<BindingId.Local> compiledArgParamIds;

  public @Nullable TList<Type> compiledArgParamTypes;

  public boolean compiledIsArgMethod;

  public SourceObjectInterfaceMethodImplData(int interfaceMethodIndex) {
    this.interfaceMethodIndex = interfaceMethodIndex;
    this.capturedExternals = new TList<>();
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId rootLoc,
      InterfaceData iface,
      TypeSourceObjectDynamic dtData,
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
    if (dtData.setId == null) {
      throw new AssertionError(
          "SourceObjectInterfaceMethodImplData::finish: dtData.setId is null");
    }
    var objectIndex = dtData.set.objects.indexOf(dtData);
    if (objectIndex < 0) {
      throw new AssertionError(
          "SourceObjectInterfaceMethodImplData::finish: dtData not in its owning set");
    }
    innerVx.evalRootInternalName =
        compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(dtData.setId, objectIndex);
    var innerTarget = new TargetDynamic(innerVx.nextLocalId);
    innerTarget.registerStaticFunctionDefs(xx.vx.ix.staticFunctionDefs);
    var selfId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    innerBindingId = selfId;
    var outerId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    outerBindingId = outerId;

    var bodyScope = new Scope(null);
    bodyScope.values.putAll(Eval.bodyBuiltinScope(xx));
    TypeSourceObjectDynamic outerType = dtData;
    var outerIb = AspectInitialBind.getMaybe(xx, outerType);
    if (outerIb instanceof Opt.Some<AspectInitialBind> outerIbSome) {
      bodyScope.values.put(new ConstKey.Str("outer"), outerIbSome.value().aspectInitialBindInitialBind(outerId));
    }

    boolean isArgMethod = iface.builtinKind == BuiltinInterfaceKind.TYPE;
    TList<BindingId.Local> argParamIds;
    if (iface.builtinKind == BuiltinInterfaceKind.TYPE) {
      var argType = ifaceMethod.argType;
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
          new ConstKey.Str("arg"), argIbAspect.aspectInitialBindInitialBind(selfId));
      argParamIds = TList.empty();
      compiledArgParamTypes = TList.empty();
    } else if (iface.builtinKind == BuiltinInterfaceKind.BINDING
        || iface.builtinKind == BuiltinInterfaceKind.TARGET
        || (iface.builtinKind == null && !ifaceMethod.dataFieldNames.isEmpty())) {
      var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
      if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
        bodyScope.values.put(
            new ConstKey.Str("inner"), selfIbSome.value().aspectInitialBindInitialBind(selfId));
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
      } else if (methodArgType instanceof compilerxyz.eval.value.record.Record rec) {
        paramTypes = new TList<>(dataFieldNames.size());
        for (var fieldName : dataFieldNames) {
          var f = rec.fields().getOrNull(new ConstKey.Str(fieldName));
          if (f == null) {
            throw new AssertionError(
                "Record method arg_type missing field '" + fieldName + "'");
          }
          if (!(f instanceof compilerxyz.eval.value.record.RecordField.Dynamic fd)) {
            throw new AssertionError(
                "Record method arg_type field '" + fieldName + "' is not Dynamic");
          }
          paramTypes.add(fd.type());
        }
      } else {
        var tupleAspectOpt = AspectTuple.getMaybe(xx, methodArgType);
        if (!(tupleAspectOpt instanceof Opt.Some<AspectTuple> tupleSome)) {
          throw new AssertionError(
              "Multi-field builtin method arg_type must be a Tuple or Record, got: " + methodArgType.typeDesc());
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
                ibAspect.aspectInitialBindInitialBind(pid), paramType));
      }
      var argRecord = new ArgumentRecord(fields);
      bodyScope.values.put(
          new ConstKey.Str("arg"), new ArgumentRecordBinding(argRecord));
      argParamIds = ids;
      compiledArgParamTypes = paramTypes;
    } else {

      var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
      if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
        bodyScope.values.put(
            new ConstKey.Str("inner"), selfIbSome.value().aspectInitialBindInitialBind(selfId));
      }
      var argId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      var argT = ifaceMethod.argType;
      if (argT != null) {
        var argIbMaybe = AspectInitialBind.getMaybe(xx, argT);
        if (argIbMaybe instanceof Opt.Some<AspectInitialBind> argIbSome) {
          bodyScope.values.put(new ConstKey.Str("arg"), argIbSome.value().aspectInitialBindInitialBind(argId));
        }
        compiledArgParamTypes = TList.of(argT);
      } else {
        compiledArgParamTypes = TList.empty();
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
        || iface.builtinKind == BuiltinInterfaceKind.TARGET
        || (iface.builtinKind == null && !ifaceMethod.dataFieldNames.isEmpty())) {
      emitArgIds = argParamIds;
    } else {
      var methodArgT = ifaceMethod.argType;
      emitArgIds = methodArgT != null ? argParamIds : TList.empty();
    }

    compiledBodyCode = bodyCode;
    compiledArgParamIds = emitArgIds;
    compiledIsArgMethod = isArgMethod;
    return EResult.ok(null);
  }

}
