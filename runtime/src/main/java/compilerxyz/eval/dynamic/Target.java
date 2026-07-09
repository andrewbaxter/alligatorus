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
import compilerxyz.eval.Unique;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleBaseDynamic;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public interface Target {

  static Type dynamicType() {
    return TargetDynamicType.INSTANCE;
  }

  final class TargetDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private TargetDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenBuiltinInterfaces.builtinTargetIfaceIdentity());
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_ANY_TARGET_DYNAMIC_TYPE_ID,
          "target",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<compilerxyz.eval.value.object_.TypeObjectCastRule>(),
          new TList<compilerxyz.eval.value.object_.TypeObjectCastFromRule>(),
          Opt.<compilerxyz.eval.value.object_.ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }

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
      ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields) {
    var entries = new TOrderedMap<ConstKey, TargetBindSub>();
    for (var f : fields) entries.put(f.key(), f.sub());
    var sortedSubs = sortBindSubs(entries, ConstKey.CANONICAL_ORDER);
    var dynamicFields = new TOrderedMap<ConstKey, RecordField>();
    for (var f : fields) {
      dynamicFields.put(f.key(), new RecordField.Dynamic(f.sub().type()));
    }
    Type recordType = new Record(Opt.none(), dynamicFields);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, recordType);
  }

  record RecordBindField(ConstKey key, TargetBindSub sub) {}

  default TargetBindSub targetBindSubTuple(
      ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots) {
    var entries = new TOrderedMap<Integer, TargetBindSub>();
    for (int i = 0; i < slots.size(); i++) entries.put(i, slots.get(i).sub());
    var sortedSubs = sortBindSubs(entries, Integer::compare);
    var dynamicElements = new TList<TupleElement>(slots.size());
    for (var slot : slots) {
      dynamicElements.add(new TupleElement.Dynamic(slot.sub().type()));
    }
    Type tupleType = new Tuple(Opt.<TupleBaseDynamic>none(), dynamicElements);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, tupleType);
  }

  record TupleBindSlot(TargetBindSub sub) {}

  private static <K> TList<TargetBindSub> sortBindSubs(
      TOrderedMap<K, TargetBindSub> entries, java.util.Comparator<K> tiebreaker) {
    var typeEntries = new TOrderedMap<K, Type>();
    for (var e : entries.entrySet()) typeEntries.put(e.getKey(), e.getValue().type());
    var sort = CompositeSlotSort.sort(typeEntries, tiebreaker);
    var out = new TList<TargetBindSub>(entries.size());
    for (var e : entries.entrySet()) out.add(null);
    for (var e : entries.entrySet()) {
      out.set(sort.slotOfKey().get(e.getKey()), e.getValue());
    }
    return out;
  }

  private static CodeDynamic emitCompositeInstantiation(
      ExprContext xx, QExprId loc, Code carry, TList<TargetBindSub> sortedSubs) {
    var sortedTypes = new TList<Type>(sortedSubs.size());
    var argSeq = new JavaBytecodeSequence();
    for (var sub : sortedSubs) {
      sortedTypes.add(sub.type());
      argSeq.add(sub.data());
    }
    var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
    var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
    return DynUtil.wrapWithCarry(
        loc,
        carry,
        JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq)));
  }

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
