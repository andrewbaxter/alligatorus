package compilerxyz.eval.value.source_object;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import org.jspecify.annotations.Nullable;

public final class SourceDataMethodData {
  public static Type dynamicType() {
    return Type.dynamicType();
  }

  public final String name;
  public @Nullable Type argType;
  public @Nullable Type retType;
  public @Nullable Value implExpr;
  public @Nullable String compiledClassName;
  public @Nullable byte[] compiledBytecode;
  public @Nullable BindingId innerBindingId;
  public int methodIdx;
  public final TList<EvalContext.ExternalEntry> capturedExternals;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;
  public @Nullable CodeDynamic compiledBodyCode;

  public @Nullable TList<compilerxyz.eval.dynamic.DynUtil.ArgSignatureSlot> compiledArgSlots;
  public @Nullable TList<BindingId.Local> compiledArgSlotIds;

  public SourceDataMethodData(String name) {
    this.name = name;
    this.capturedExternals = new TList<>();
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId rootLoc,
      String dtName,
      TypeSourceObjectDynamic outerType,
      Type underlyingType,
      String objectInternalName) {
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
          "Data type '" + dtName + "' method '" + name + "' impl is not an Expr: " + implExprValue);
    }
    var retT = retType;
    if (retT == null) {
      return EResult.err(
          rootLoc, "Data type '" + dtName + "' method '" + name + "' is missing a return type");
    }
    var innerVx = new EvalContext(xx.vx.ix);
    innerVx.evalRootInternalName = objectInternalName;
    var innerTarget = new TargetDynamic(innerVx.nextLocalId);
    var selfId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    innerBindingId = selfId;

    var outerId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var bodyScope = new Scope();
    bodyScope.pushAll(Eval.bodyBuiltinScope(xx));
    var outerIb = AspectInitialBind.getMaybe(xx, outerType);
    if (outerIb instanceof Opt.Some<AspectInitialBind> outerIbSome) {
      bodyScope.pushNamed(new ConstKey.Str("outer"), outerIbSome.value().aspectInitialBindInitialBind(outerId));
    }
    var selfIb = AspectInitialBind.getMaybe(xx, underlyingType);
    if (selfIb instanceof Opt.Some<AspectInitialBind> selfIbSome) {
      bodyScope.pushNamed(new ConstKey.Str("inner"), selfIbSome.value().aspectInitialBindInitialBind(selfId));
    }
    TList<compilerxyz.eval.dynamic.DynUtil.ArgSignatureSlot> argSlots = null;
    TList<BindingId.Local> argSlotIds = null;
    if (argType != null) {
      var slotsRes = compilerxyz.eval.dynamic.DynUtil.argSignatureSlots(xx, rootLoc, argType);
      if (slotsRes.hasErrors()) return slotsRes.reerr();
      argSlots = slotsRes.requireValue("SourceDataMethodData::finish argSignatureSlots");
      argSlotIds = new TList<>(argSlots.size());
      var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
      var recordOpt = compilerxyz.gen.AspectRecord.getMaybe(xx, argType);
      if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
        var elemBindings = new TList<compilerxyz.gen.Binding>(argSlots.size());
        for (var slot : argSlots) {
          var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
          argSlotIds.add(slotId);
          var ibMaybe = AspectInitialBind.getMaybe(xx, slot.elementType());
          if (!(ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome)) {
            return EResult.err(rootLoc,
                "SourceDataMethodData::finish tuple element type "
                    + slot.elementType().typeDesc() + " missing AspectInitialBind");
          }
          elemBindings.add(ibSome.value().aspectInitialBindInitialBind(slotId));
        }
        var argTuple = new compilerxyz.eval.value.argrecord.ArgumentTuple(
            elemBindings, tupleSome.value().aspectTupleElementTypes());
        bodyScope.pushNamed(new ConstKey.Str("arg"),
            new compilerxyz.eval.value.argrecord.ArgumentTupleBinding(argTuple));
      } else if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
        var fields =
            new TOrderedMap<ConstKey, compilerxyz.eval.value.argrecord.ArgumentEntry>();
        int i = 0;
        for (var entry : recordSome.value().aspectRecordFieldTypes().entrySet()) {
          var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
          argSlotIds.add(slotId);
          var ibMaybe = AspectInitialBind.getMaybe(xx, entry.getValue());
          if (!(ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome)) {
            return EResult.err(rootLoc,
                "SourceDataMethodData::finish record field type "
                    + entry.getValue().typeDesc() + " missing AspectInitialBind");
          }
          fields.put(entry.getKey(),
              new compilerxyz.eval.value.argrecord.ArgumentEntry(
                  ibSome.value().aspectInitialBindInitialBind(slotId), entry.getValue()));
          i++;
        }
        var argRec = new compilerxyz.eval.value.argrecord.ArgumentRecord(fields);
        bodyScope.pushNamed(new ConstKey.Str("arg"),
            new compilerxyz.eval.value.argrecord.ArgumentRecordBinding(argRec));
      } else {
        var slotId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        argSlotIds.add(slotId);
        var argIbMaybe = AspectInitialBind.getMaybe(xx, argType);
        if (argIbMaybe instanceof Opt.Some<AspectInitialBind> argIbSome) {
          bodyScope.pushNamed(new ConstKey.Str("arg"), argIbSome.value().aspectInitialBindInitialBind(slotId));
        }
      }
    }
    var methodXx =
        new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, null);

    var implResult = ConstEval.constEval(methodXx, implExprAst);
    if (implResult.hasErrors()) {
      return implResult.reerr();
    }
    var implCv = implResult.requireValue("SourceDataMethodData::compile constEval");
    var convertedResult = Eval.typecheckConvert(methodXx, rootLoc, retT, implCv);
    if (convertedResult.hasErrors()) {
      return convertedResult.reerr();
    }
    var convertedCv =
        convertedResult.requireValue("SourceDataMethodData::compile typecheckConvert");
    CodeDynamic bodyCode;
    if (convertedCv.value() instanceof Opt.Some<Value> someVal) {
      var valueType = someVal.value().valueType();
      var consumeResult = AspectConsumeDynamic.get(methodXx, rootLoc, valueType);
      if (consumeResult.hasErrors()) {
        return consumeResult.reerr();
      }
      var consume = consumeResult.requireValue("SourceDataMethodData::compile consume aspect");
      var dynResult = consume.aspectConsumeDynamicToDynamic(methodXx, rootLoc, convertedCv);
      if (dynResult.hasErrors()) {
        return dynResult.reerr();
      }
      bodyCode = dynResult.requireValue("SourceDataMethodData::compile consume.toDynamic");
    } else {
      bodyCode = convertedCv.code().dynamic();
      if (bodyCode == null) {
        return EResult.err(
            rootLoc,
            "Data type '" + dtName + "' method '" + name + "' body produced no compile-time value");
      }
    }

    for (var entry : innerVx.definitionSets.entrySet()) {
      xx.vx.definitionSets.putIfAbsent(entry.getKey(), entry.getValue());
    }

    var captured = innerVx.externals;
    capturedExternals.addAll(captured);

    compiledBodyCode = bodyCode;
    compiledArgSlots = argSlots;
    compiledArgSlotIds = argSlotIds;
    return EResult.ok(null);
  }

}
