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
import compilerxyz.gen.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
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
  public @Nullable BindingId innerBindingId;
  public @Nullable BindingId outerBindingId;
  public int methodImplIdx;
  public final TList<EvalContext.ExternalEntry> capturedExternals;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public @Nullable CodeDynamic compiledBodyCode;

  public @Nullable TList<BindingId.Local> compiledArgParamIds;

  public @Nullable TList<Type> compiledArgParamTypes;

  public SourceObjectInterfaceMethodImplData(int interfaceMethodIndex) {
    this.interfaceMethodIndex = interfaceMethodIndex;
    this.capturedExternals = new TList<>();
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId rootLoc,
      InterfaceData iface,
      TypeSourceObjectDynamic dtData,
      Type underlyingType) {
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
    var selfId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    innerBindingId = selfId;
    var outerId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    outerBindingId = outerId;

    var bodyScope = new Scope();
    bodyScope.pushAll(Eval.bodyBuiltinScope(xx));
    TypeSourceObjectDynamic outerType = dtData;
    var outerIb = AspectInitialBind.getMaybe(xx, outerType);
    if (outerIb instanceof Opt.Some<AspectInitialBind> outerIbSome) {
      bodyScope.pushNamed(new ConstKey.Str("outer"), outerIbSome.value().aspectInitialBindInitialBind(outerId));
    }

    var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
    if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
      bodyScope.pushNamed(
          new ConstKey.Str("inner"), selfIbSome.value().aspectInitialBindInitialBind(selfId));
    }
    var methodArgType = ifaceMethod.argType;
    if (methodArgType == null) {
      throw new AssertionError("Interface method '" + ifaceMethod.name + "' missing arg_type");
    }
    var dataFieldNames = ifaceMethod.dataFieldNames;
    var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, methodArgType);
    TList<Type> paramTypes;
    var argParamIds = new TList<BindingId.Local>();
    if (methodArgType instanceof compilerxyz.eval.value.record.Record rec) {
      if (dataFieldNames.size() != rec.fields().size()) {
        throw new AssertionError(
            "Record method '"
                + ifaceMethod.name
                + "' data_field_names ("
                + dataFieldNames.size()
                + ") does not match record field count ("
                + rec.fields().size()
                + ")");
      }
      paramTypes = new TList<>(dataFieldNames.size());
      var fields = new TOrderedMap<ConstKey, compilerxyz.eval.value.argrecord.ArgumentEntry>();
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
        var pid = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        argParamIds.add(pid);
        var ibResult = AspectInitialBind.get(xx, rootLoc, fd.type());
        if (ibResult.hasErrors()) {
          return ibResult.reerr();
        }
        var ibAspect =
            ibResult.requireValue(
                "SourceObjectInterfaceMethodImplData::compile param InitialBind");
        fields.put(
            new ConstKey.Str(fieldName),
            new compilerxyz.eval.value.argrecord.ArgumentEntry(
                ibAspect.aspectInitialBindInitialBind(pid), fd.type()));
      }
      var argRecord = new ArgumentRecord(fields);
      bodyScope.pushNamed(
          new ConstKey.Str("arg"), new ArgumentRecordBinding(argRecord));
    } else if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
      if (!dataFieldNames.isEmpty()) {
        throw new AssertionError(
            "Tuple method '"
                + ifaceMethod.name
                + "' must have empty data_field_names, got: "
                + dataFieldNames);
      }
      var elemTypes = tupleSome.value().aspectTupleElementTypes();
      paramTypes = new TList<>(elemTypes.size());
      var elemBindings = new TList<Binding>(elemTypes.size());
      for (var elemType : elemTypes) {
        paramTypes.add(elemType);
        var pid = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        argParamIds.add(pid);
        var ibResult = AspectInitialBind.get(xx, rootLoc, elemType);
        if (ibResult.hasErrors()) {
          return ibResult.reerr();
        }
        var ibAspect = ibResult.requireValue(
            "SourceObjectInterfaceMethodImplData::compile tuple elem InitialBind");
        elemBindings.add(ibAspect.aspectInitialBindInitialBind(pid));
      }
      var argTuple = new compilerxyz.eval.value.argrecord.ArgumentTuple(elemBindings, elemTypes);
      bodyScope.pushNamed(
          new ConstKey.Str("arg"),
          new compilerxyz.eval.value.argrecord.ArgumentTupleBinding(argTuple));
    } else {
      if (!dataFieldNames.isEmpty()) {
        throw new AssertionError(
            "Non-record method '"
                + ifaceMethod.name
                + "' must have empty data_field_names, got: "
                + dataFieldNames);
      }
      paramTypes = TList.of(methodArgType);
      var argId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      argParamIds.add(argId);
      var argIbMaybe = AspectInitialBind.getMaybe(xx, methodArgType);
      if (argIbMaybe instanceof Opt.Some<AspectInitialBind> argIbSome) {
        bodyScope.pushNamed(
            new ConstKey.Str("arg"), argIbSome.value().aspectInitialBindInitialBind(argId));
      }
    }
    compiledArgParamTypes = paramTypes;
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
      var valueType = someVal.value().valueType();
      var consumeResult = AspectConsumeDynamic.get(methodXx, rootLoc, valueType);
      if (consumeResult.hasErrors()) {
        return consumeResult.reerr();
      }
      var consume =
          consumeResult.requireValue(
              "SourceObjectInterfaceMethodImplData::compile consume aspect");
      var dynResult = consume.aspectConsumeDynamicToDynamic(methodXx, rootLoc, convertedCv);
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

    compiledBodyCode = bodyCode;
    compiledArgParamIds = argParamIds;
    return EResult.ok(null);
  }

}
