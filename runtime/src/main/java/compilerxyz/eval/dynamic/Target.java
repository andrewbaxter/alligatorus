package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public interface Target {

  default BindResult targetBind(QExprId loc, TargetBindSub sub) {
    throw new AssertionError(
        "user did not provide a Target::bind method on " + getClass().getName());
  }

  record BindResult(CodeDynamic code, BindingId.Local id) {}

  default EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException(
          "Target::maybeBindSub: CodeValue has no value; Rust unwraps unconditionally");
    }
    var vtype = some.value().valueTypeOf();
    var bindDynOpt = AspectBindDynamic.getMaybe(xx, vtype);
    if (!(bindDynOpt instanceof Opt.Some<AspectBindDynamic> bindDynSome)) {
      return EResult.ok(Opt.none());
    }
    return bindDynSome.value().bindDynamic(xx, loc, cv);
  }

  default EResult<TargetBindSub> targetCodeToBindSub(QExprId loc, Code code, Type type_) {
    var dyn = code.dynamic();
    if (dyn == null) {
      return EResult.err(loc, "Target::codeToBindSub: code is not dynamic");
    }
    return EResult.ok(new TargetBindSub(dyn, type_));
  }

  default TargetBindSub targetBindSubRecord(
      QExprId loc, Code carry, TList<RecordBindField> fields) {
    var sorted = new TList<RecordBindField>(fields.size());
    sorted.addAll(fields);
    sorted.sort((a, b) -> ConstKey.CANONICAL_ORDER.compare(a.key(), b.key()));
    var fieldKeys =
        new TList<RecordClassEmitter.FieldKey>(sorted.size());
    var entryCodes = new TList<CodeDynamic>(sorted.size());
    var dynamicFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : sorted) {
      var rawKeyName =
          switch (entry.key()) {
            case ConstKey.Str s -> s.value();
            case ConstKey.Uint u -> Long.toUnsignedString(u.value());
          };
      fieldKeys.add(
          new RecordClassEmitter.FieldKey(
              rawKeyName, entry.sub().type()));
      entryCodes.add(entry.sub().data());
      dynamicFields.put(entry.key(), new RecordField.Dynamic(entry.sub().type()));
    }
    Type recordType = new Record(Opt.none(), dynamicFields);
    var klass = JavaInternalName.of(RecordClassEmitter.internalNameFor(fieldKeys));
    var dynamicCode =
        DynUtil.wrapWithCarry(
            loc, carry, JavaBytecodeUtils.instantiateVarargs(loc, klass, entryCodes));
    return new TargetBindSub(dynamicCode, recordType);
  }

  record RecordBindField(ConstKey key, TargetBindSub sub) {}

  default EResult<Code> targetCastException(
      ExprContext xx, QExprId loc, Code callCode, BindingId catchBinding, CodeValue castCv) {
    throw new AssertionError(
        "user did not provide a Target::castException method on " + getClass().getName());
  }

  default EResult<Code> targetSeq(QExprId loc, TList<Code> entries) {
    throw new AssertionError(
        "user did not provide a Target::seq method on " + getClass().getName());
  }

  default EResult<Code> targetTryFinally(QExprId loc, Code body, Code finally_) {
    throw new AssertionError(
        "user did not provide a Target::tryFinally method on " + getClass().getName());
  }

  default EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new AssertionError(
        "user did not provide a Target::drop method on " + getClass().getName());
  }

  default EResult<CodeValue> targetStage(
      ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues) {
    throw new AssertionError(
        "user did not provide a Target::stage method on " + getClass().getName());
  }

  default EResult<CodeValue> targetIf_(
      ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_) {
    throw new AssertionError(
        "user did not provide a Target::if_ method on " + getClass().getName());
  }

  record IfBranch(CodeValue condition, Expr body) {}

  default EResult<CodeDynamic> targetTryCatchFinally(
      QExprId loc,
      CodeDynamic body,
      Opt<TryCatchClause> catch_,
      Opt<CodeDynamic> finally_) {
    throw new AssertionError(
        "user did not provide a Target::tryCatchFinally method on " + getClass().getName());
  }

  record TryCatchClause(BindingId.Local binding, CodeDynamic body) {}

  default EResult<CodeValue> targetMatch_(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      ConstKey elementKey,
      TList<TargetCase> cases,
      Opt<Expr> fallback) {
    throw new AssertionError(
        "user did not provide a Target::match_ method on " + getClass().getName());
  }
}
