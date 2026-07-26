package compilerxyz.eval;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;

import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBoolConst;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorMod;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.AspectUnion;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.gen.Target;
import compilerxyz.eval.dynamic.target.IfBranch;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.dynamic.target.TryCatchClause;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.langhelp.Compiles;
import compilerxyz.langhelp.SourceModuleToModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.langhelp.Sources;
import compilerxyz.gen.Value;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBoolConst;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectIterable;
import compilerxyz.gen.GenAspectOperatorAdd;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenAspectUnion;

public final class ConstEval {
  private ConstEval() {}

  public static EResult<CodeValue> constEval(ExprContext xx, Expr expr) {
    var loc = expr.loc();
    return switch (expr.variant()) {
      case Expr.Literal lit ->
          EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(lit.value().consume())));
      case Expr.ScopeUse p -> {
        var erb = new Erb(loc);
        var keyR = constEval(xx, p.key());
        var keyOpt = erb.mustEr(keyR);
        if (keyOpt.isEmpty()) yield EResult.errs(erb.errors());
        var key = keyOpt.get();
        var keyVal = (Opt.Some<Value>) key.value();
        var keyConstRes = Eval.extractConstKey(xx, loc, keyVal.value());
        var keyConstOpt = erb.mustRes(keyConstRes);
        if (keyConstOpt.isEmpty()) yield EResult.errs(erb.errors());
        var ck = keyConstOpt.get();
        Binding found = xx.scope.lookup(ck);
        if (found == null) {
          erb.errPush("No bound value with id [" + ck + "]");
          yield EResult.errs(erb.errors());
        }
        yield EResult.ok(found.bindingUse(xx, loc, key.code()));
      }
      case Expr.Call p -> {
        var erb = new Erb(loc);
        var fopt = erb.mustEr(constEval(xx, p.func()));
        var aopt = erb.mustEr(constEval(xx, p.arg()));
        if (fopt.isEmpty() || aopt.isEmpty()) yield EResult.errs(erb.errors());
        var func = fopt.get();
        var arg = aopt.get();
        var fv = (Opt.Some<Value>) func.value();
        var funcType = fv.value().valueType();
        var aspectFnOpt = erb.mustEr(GenAspectFunction.get(xx, loc, funcType));
        if (aspectFnOpt.isEmpty()) yield EResult.errs(erb.errors());
        var aspectFn = aspectFnOpt.get();
        var calleeExc = aspectFn.aspectFunctionExceptionType();
        var callOpt = erb.mustErMaybeUnreachable(aspectFn.aspectFunctionCall(xx, loc, func, arg));
        if (callOpt.isEmpty()) yield EResult.errs(erb.errors());
        var wrapped = erb.mustErMaybeUnreachable(Eval.maybeWrapCallException(xx, loc, calleeExc, callOpt.get()));
        if (wrapped.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(wrapped.get());
      }
      case Expr.Set e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var vopt = erb.mustEr(constEval(xx, e.value()));
        if (bopt.isEmpty() || vopt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var value = vopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var baseType = bv.value().valueType();
        var setOpt = erb.mustEr(GenAspectSet.get(xx, loc, baseType));
        if (setOpt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(setOpt.get().aspectSetSet(xx, loc, base, value));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.Field e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var fopt = erb.mustEr(constEval(xx, e.key()));
        if (bopt.isEmpty() || fopt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var field = fopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var baseType = bv.value().valueType();
        var aspectFieldOpt = erb.mustEr(GenAspectField.get(xx, loc, baseType));
        if (aspectFieldOpt.isEmpty()) yield EResult.errs(erb.errors());
        var aspectFieldField = aspectFieldOpt.get();
        var primaryOpt = erb.mustEr(aspectFieldField.aspectFieldField(xx, loc, base, field));
        if (primaryOpt.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(primaryOpt.get());
      }
      case Expr.Bind ignored ->
          EResult.err(loc, "Bind expression must be directly inside a block");
      case Expr.Seq e -> constEvalSeq(xx, loc, e);
      case Expr.Stage e -> constEvalStage(xx, loc, e);
      case Expr.Lower lo -> constEval(xx, lo.expr());
      case Expr.Graft.Evaluated ev -> EResult.ok(ev.value());
      case Expr.Graft.Pending ignored ->
          EResult.err(loc, "Graft outside of stage processing — its inner was never evaluated");
      case Expr.Tuple t -> {
        var erb = new Erb(loc);
        var elements = new TList<TupleElement>(t.elements().size());
        for (var elExpr : t.elements()) {
          var elRes = constEval(xx, elExpr);
          if (!elRes.hasErrors() && elRes.valueOrNull() != null
              && !(elRes.valueOrNull().value() instanceof Opt.Some<Value>)) {
            erb.errors()
                .add(
                    new ExprErr(
                        elExpr.loc(),
                        TList.of(new ExprSubErr.Opaque(
                            "Tuple element does not fall through (contains an unreachable"
                                + " control jump like throw/panic); not valid in element"
                                + " position"))));
            continue;
          }
          elements.add(new TupleElement.EvalRes(elRes));
        }
        if (!erb.errors().isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(new Tuple(Opt.none(), elements))));
      }
      case Expr.Record r -> constEvalRecord(xx, loc, r);
      case Expr.OperatorLt e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var aopt = erb.mustEr(GenAspectOperatorLt.get(xx, loc, bv.value().valueType()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().aspectOperatorLtLt(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.OperatorEq e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var aopt = erb.mustEr(GenAspectOperatorEq.get(xx, loc, bv.value().valueType()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().aspectOperatorEqEq(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.OperatorAdd e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var aopt = erb.mustEr(GenAspectOperatorAdd.get(xx, loc, bv.value().valueType()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().aspectOperatorAddAdd(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.OperatorMod e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var aopt = erb.mustEr(GenAspectOperatorMod.get(xx, loc, bv.value().valueType()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().aspectOperatorModMod(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.Foreach e -> wrapControlFrame(xx, loc, Opt.none(), () -> constEvalForeach(xx, loc, e));
      case Expr.Match e -> wrapControlFrame(xx, loc, Opt.none(), () -> constEvalMatch(xx, loc, e));
      case Expr.If e -> wrapControlFrame(xx, loc, Opt.none(), () -> constEvalIf(xx, loc, e));
      case Expr.As e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var topt = erb.mustEr(constEval(xx, e.target()));
        if (bopt.isEmpty() || topt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var target = topt.get();
        var tv = (Opt.Some<Value>) target.value();
        var targetMeta = tv.value().valueType();
        var aspectTypeOpt = erb.mustEr(GenAspectType.get(xx, loc, targetMeta));
        if (aspectTypeOpt.isEmpty()) yield EResult.errs(erb.errors());
        var targetType = aspectTypeOpt.get().aspectTypeType(tv.value());
        var converted = erb.mustEr(Eval.typecheckConvert(xx, loc, targetType, base));
        if (converted.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(converted.get());
      }
      case Expr.Try e -> constEvalTry(xx, loc, e);
      case Expr.Labeled e ->
          wrapControlFrame(xx, loc, Opt.some(e.label()), () -> constEval(xx, e.inner()));
      case Expr.Return e -> constEvalReturn(xx, loc, e);
    };
  }

  private static EResult<CodeValue> wrapControlFrame(
      ExprContext xx,
      QExprId loc,
      Opt<String> label,
      java.util.function.Supplier<EResult<CodeValue>> body) {
    var endKey = new compilerxyz.eval.dynamic.JumpKey();
    var frame = new ReturnFrame(label, endKey);
    frame.scopeDepthAtPush = xx.scope.depth();
    xx.returnStack.add(frame);
    EResult<CodeValue> inner;
    try {
      inner = body.get();
    } finally {
      xx.returnStack.remove(xx.returnStack.size() - 1);
    }
    if (inner.valueOrNull() == null) return inner;
    var innerCv = inner.requireValue("wrapControlFrame body");
    Opt<Value> resultValue = innerCv.value();
    if (frame.wasTargeted
        && (frame.firstReturnValue != null || !(resultValue instanceof Opt.Some<Value>))) {
      resultValue = Opt.some(scopeResultValue(xx, frame));
    }
    var wrapped = wrapWithLand(loc, innerCv, endKey, resultValue);
    var errs = inner.errorsOrNull();
    if (errs != null && !errs.isEmpty()) return EResult.mixed(wrapped, errs);
    return EResult.ok(wrapped);
  }

  private static CodeValue wrapWithLand(
      QExprId loc, CodeValue inner, compilerxyz.eval.dynamic.JumpKey endKey, Opt<Value> resultValue) {
    var land = new compilerxyz.eval.dynamic.CodeDynamic(loc,
        new compilerxyz.eval.dynamic.JavaBytecodeLand(endKey));
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    if (inner.code() instanceof Code.Target t && t.impl() instanceof compilerxyz.eval.dynamic.CodeDynamic cd) {
      seq.add(cd);
    }
    seq.add(land);
    return new CodeValue(Code.of(new compilerxyz.eval.dynamic.CodeDynamic(loc, seq)),
        resultValue instanceof Opt.Some<Value> s ? EvalOutcome.ofValue(s.value()) : EvalOutcome.NO_VALUE);
  }

  private static EResult<CodeValue> constEvalReturn(ExprContext xx, QExprId loc, Expr.Return e) {
    var erb = new Erb(loc);
    ReturnFrame target = null;
    var stack = xx.returnStack;
    for (int i = stack.size() - 1; i >= 0; i--) {
      var f = stack.get(i);
      if (e.label() instanceof Opt.Some<String> lbl) {
        if (f.label instanceof Opt.Some<String> flbl && flbl.value().equals(lbl.value())) {
          target = f;
          break;
        }
      } else {
        target = f;
        break;
      }
    }
    if (target == null) {
      if (e.label() instanceof Opt.Some<String> lbl) {
        erb.errPush("return: no enclosing scope with label '" + lbl.value() + "'");
      } else {
        erb.errPush("return: no enclosing control scope");
      }
      return EResult.errs(erb.errors());
    }
    target.wasTargeted = true;
    CodeValue valCv;
    if (e.value() instanceof Opt.Some<Expr> someV) {
      var vOpt = erb.mustErMaybeUnreachable(constEval(xx, someV.value()));
      if (vOpt.isEmpty()) return EResult.errs(erb.errors());
      valCv = vOpt.get();
    } else {
      valCv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(emptyTupleValue()));
    }
    valCv = xx.target.targetReturn(xx, loc, valCv);
    if (!(valCv.outcome() instanceof EvalOutcome.HasValue someVal)) {
      return EResult.ok(new CodeValue(valCv.code(), EvalOutcome.CONTROL_ENDED));
    }
    if (target.firstReturnValue == null) {
      target.firstReturnValue = someVal.value();
    }
    var dropSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    boolean anyDrop = false;
    for (int i = xx.scope.depth() - 1; i >= target.scopeDepthAtPush; i--) {
      var dropRes = xx.scope.entries.get(i).bindingDrop(xx, loc);
      if (dropRes.hasErrors()) {
        dropRes.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      var d = dropRes.requireValue("constEvalReturn drop");
      if (d instanceof Code.Target dt && dt.impl() instanceof compilerxyz.eval.dynamic.CodeDynamic dcd) {
        dropSeq.add(dcd);
        anyDrop = true;
      }
    }
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    if (valCv.code() instanceof Code.Target t && t.impl() instanceof compilerxyz.eval.dynamic.CodeDynamic valCd) {
      seq.add(valCd);
      if (anyDrop) {
        // Drops run with the return value already on the stack (e.g. a
        // foreach's BindingIteratorStack POP would remove the value, not the
        // iterator). Park the value in a slot around the drops.
        var valueType =
            someVal.value() instanceof compilerxyz.eval.value.ValueDynamic vd
                ? vd.type_()
                : someVal.value().valueType();
        var tmpKey = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
        seq.add(
            new compilerxyz.eval.dynamic.CodeDynamic(
                loc,
                new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(
                    compilerxyz.eval.dynamic.JvmSort.storeOpcode(xx, valueType), tmpKey)));
        seq.add(new compilerxyz.eval.dynamic.CodeDynamic(loc, dropSeq));
        seq.add(
            new compilerxyz.eval.dynamic.CodeDynamic(
                loc,
                new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(
                    compilerxyz.eval.dynamic.JvmSort.loadOpcode(xx, valueType), tmpKey)));
      }
    } else if (anyDrop) {
      seq.add(new compilerxyz.eval.dynamic.CodeDynamic(loc, dropSeq));
    }
    seq.add(new compilerxyz.eval.dynamic.CodeDynamic(loc,
        new compilerxyz.eval.dynamic.JavaBytecodeJump(target.endKey)));
    var retCode = Code.of(new compilerxyz.eval.dynamic.CodeDynamic(loc, seq));
    return EResult.ok(new CodeValue(retCode, EvalOutcome.CONTROL_ENDED));
  }

  private static Value emptyTupleValue() {
    return new Tuple(Opt.none(), new TList<>());
  }

  private static Value scopeResultValue(ExprContext xx, ReturnFrame frame) {
    if (frame.firstReturnValue == null) {
      return emptyTupleValue();
    }
    return frame.firstReturnValue;
  }

  private static EResult<CodeValue> constEvalSeq(ExprContext xx, QExprId loc, Expr.Seq e) {
    var erb = new Erb(loc);
    var initialScopeDepth = xx.scope.depth();
    var endKey = new compilerxyz.eval.dynamic.JumpKey();
    var thisFrame = new ReturnFrame(Opt.none(), endKey);
    thisFrame.scopeDepthAtPush = xx.scope.depth();
    xx.returnStack.add(thisFrame);

    class Frame {
      final TList<Code> codes = new TList<>();
      @org.jspecify.annotations.Nullable Binding binding = null;
    }

    var frames = new TList<Frame>();
    frames.add(new Frame());
    Opt<Value> lastVal = Opt.some(emptyTupleValue());
    CodeValue pendingDiscard = null;
    boolean controlEnded = false;
    var subs = e.exprs();
    int total = subs.size();
    for (int idx = 0; idx < total; idx++) {
      var sub = subs.get(idx);
      if (controlEnded) {
        erb.errors()
            .add(
                new ExprErr(
                    sub.loc(),
                    TList.of(new ExprSubErr.Opaque(
                        "Unreachable: control flow ended at prior expression"))));
        continue;
      }
      if (pendingDiscard != null) {
        var dropOpt = erb.mustErMaybeUnreachable(xx.target.targetDrop(xx, sub.loc(), pendingDiscard));
        if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(dropOpt.get());
        pendingDiscard = null;
      }
      if (sub.variant() instanceof Expr.Bind bind) {
        var bopt = erb.mustEr(constEval(xx, bind.base()));
        var vopt = erb.mustErMaybeUnreachable(constEval(xx, bind.value()));
        if (bopt.isEmpty() || vopt.isEmpty()) return EResult.errs(erb.errors());
        var base = bopt.get();
        var value = vopt.get();
        if (!(base.value() instanceof Opt.Some<Value> baseV)) {
          erb.errPush("Bind: base has no compile-time value");
          return EResult.errs(erb.errors());
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, sub.loc(), baseV.value()));
        if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(base.code());
        if (!(value.value() instanceof Opt.Some<Value> valV)) {
          erb.errors()
              .add(
                  new ExprErr(
                      sub.loc(),
                      TList.of(new ExprSubErr.Opaque(
                          "Cannot bind result of expression that ends control flow"))));
          return EResult.errs(erb.errors());
        }
        var valType = valV.value().valueType();
        var bindAspectOpt = erb.mustEr(GenAspectBind.get(xx, sub.loc(), valType));
        if (bindAspectOpt.isEmpty()) return EResult.errs(erb.errors());
        var brOpt = erb.mustEr(bindAspectOpt.get().aspectBindBind(xx, sub.loc(), value));
        if (brOpt.isEmpty()) return EResult.errs(erb.errors());
        var br = brOpt.get();
        frames.get(frames.size() - 1).codes.add(br.code());
        var nf = new Frame();
        nf.binding = br.binding();
        frames.add(nf);
        xx.scope.pushNamed(ckOpt.get(), br.binding());
        lastVal = Opt.some(emptyTupleValue());
      } else {
        var ropt = erb.mustErMaybeUnreachable(constEval(xx, sub));
        if (ropt.isEmpty()) return EResult.errs(erb.errors());
        var r = ropt.get();
        frames.get(frames.size() - 1).codes.add(r.code());
        if (r.outcome() instanceof EvalOutcome.ControlEnded) {
          controlEnded = true;
          lastVal = Opt.none();
          continue;
        }
        pendingDiscard = r;
      }
    }
    if (pendingDiscard != null) {
      var dropOpt = erb.mustErMaybeUnreachable(xx.target.targetDrop(xx, loc, pendingDiscard));
      if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
      frames.get(frames.size() - 1).codes.add(dropOpt.get());
      pendingDiscard = null;
    }
    while (frames.size() > 1) {
      var inner = frames.remove(frames.size() - 1);
      if (inner.binding == null) {
        frames.get(frames.size() - 1).codes.addAll(inner.codes);
      } else {
        var dropRes = erb.mustErMaybeUnreachable(inner.binding.bindingDrop(xx, loc));
        if (dropRes.isEmpty()) return EResult.errs(erb.errors());
        var bodyOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, inner.codes));
        if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
        var wrappedOpt =
            erb.mustErMaybeUnreachable(xx.target.targetTryFinally(loc, bodyOpt.get(), dropRes.get()));
        if (wrappedOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(wrappedOpt.get());
      }
    }
    xx.returnStack.remove(xx.returnStack.size() - 1);
    xx.scope.truncate(initialScopeDepth);
    var outer = frames.remove(0);
    outer.codes.add(Code.of(new compilerxyz.eval.dynamic.CodeDynamic(loc,
        new compilerxyz.eval.dynamic.JavaBytecodeLand(endKey))));
    var seqOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, outer.codes));
    if (seqOpt.isEmpty()) return EResult.errs(erb.errors());
    if (thisFrame.wasTargeted
        && (thisFrame.firstReturnValue != null || !(lastVal instanceof Opt.Some<Value>))) {
      lastVal = Opt.some(scopeResultValue(xx, thisFrame));
    }
    var cv = new CodeValue(seqOpt.get(),
        lastVal instanceof Opt.Some<Value> lvs ? EvalOutcome.ofValue(lvs.value()) : (controlEnded ? EvalOutcome.CONTROL_ENDED : EvalOutcome.NO_VALUE));
    if (erb.errors().isEmpty()) {
      return EResult.ok(cv);
    }
    return EResult.mixed(cv, erb.errors());
  }

  private static EResult<CodeValue> constEvalRecord(ExprContext xx, QExprId loc, Expr.Record r) {
    var entries = new TOrderedMap<ConstKey, EResult<CodeValue>>();
    var keyCodes = new TList<CodeDynamic>();
    var errors = new TList<ExprErr>();
    for (var entry : r.entries()) {
      var kRes = constEval(xx, entry.key());
      if (kRes.hasErrors()) {
        kRes.drainInto(errors);
        continue;
      }
      var kCv = kRes.requireValue("ConstEval::record key");
      if (kCv.code().dynamic() != null) {
        keyCodes.add(kCv.code().dynamic());
      }
      if (!(kCv.value() instanceof Opt.Some<Value> kv)) {
        errors.add(
            new ExprErr(
                loc,
                TList.of(new ExprSubErr.Opaque("Record: key has no compile-time value"))));
        continue;
      }
      var keyRes = Eval.extractConstKey(xx, loc, kv.value());
      switch (keyRes) {
        case Result.Ok<ConstKey, String> ok -> {
          var valRes = constEval(xx, entry.value());
          if (!valRes.hasErrors() && valRes.valueOrNull() != null
              && !(valRes.valueOrNull().value() instanceof Opt.Some<Value>)) {
            errors.add(
                new ExprErr(
                    entry.value().loc(),
                    TList.of(new ExprSubErr.Opaque(
                        "Record field '" + ok.value().toCanonical()
                            + "' does not fall through (contains an unreachable control"
                            + " jump like throw/panic); not valid in field position"))));
          } else {
            entries.put(ok.value(), valRes);
          }
        }
        case Result.Err<ConstKey, String> err ->
            errors.add(new ExprErr(loc, TList.of(new ExprSubErr.Opaque(err.error()))));
      }
    }
    if (!errors.isEmpty()) {
      return EResult.errs(errors);
    }
    Code code;
    if (keyCodes.isEmpty()) {
      code = Code.none(loc);
    } else {
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      for (var c : keyCodes) seq.add(c);
      code = Code.of(loc, new CodeDynamic(loc, seq));
    }
    var fields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : entries.entrySet()) {
      fields.put(entry.getKey(), new RecordField.EvalRes(entry.getValue()));
    }
    return EResult.ok(new CodeValue(code, EvalOutcome.ofValue(new Record(Opt.none(), fields))));
  }

  private static EResult<CodeValue> constEvalForeach(
      ExprContext xx, QExprId loc, Expr.Foreach e) {
    var erb = new Erb(loc);
    var bopt = erb.mustEr(constEval(xx, e.base()));
    var ekopt = erb.mustEr(constEval(xx, e.elementKey()));
    if (bopt.isEmpty() || ekopt.isEmpty()) return EResult.errs(erb.errors());
    var base = bopt.get();
    var ekCv = ekopt.get();
    var ekv = (Opt.Some<Value>) ekCv.value();
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.isEmpty()) return EResult.errs(erb.errors());
    var bv = (Opt.Some<Value>) base.value();
    var baseType = bv.value().valueType();
    var iterOpt = erb.mustEr(GenAspectIterable.get(xx, loc, baseType));
    if (iterOpt.isEmpty()) return EResult.errs(erb.errors());
    var iterable = iterOpt.get();
    var elementType = iterable.aspectIterableElementType();
    var elemBindRes = GenAspectElementUseDynamicBind.get(xx, loc, elementType);
    if (elemBindRes.hasErrors()) return elemBindRes.reerr();
    var elementScopeId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var elementBinding =
        elemBindRes
            .requireValue("Foreach: AspectElementUseDynamicBind")
            .aspectElementUseDynamicBindElementUseDynamicBind(elementScopeId);
    var initialScopeDepth = xx.scope.depth();
    // Fork/merge across the loop's implicit branches: the zero-iteration case
    // (body never runs — scope unchanged) and the one-or-more case (body
    // runs, may mutate). Both must be compatible at the merge point.
    var initialScope = xx.scope.fork();
    // The loop skeleton keeps the iterator on the operand stack across body
    // iterations; a Return-Jump escaping the body would leave the iterator
    // dangling. Pushing a synthetic BindingIteratorStack (whose bindingDrop
    // emits POP) has Return's inline-drop walk restore the operand stack
    // before Jump. The element key is pushed as a named entry immediately
    // after so it shadows any outer name; the iterator sits below.
    xx.scope.push(new compilerxyz.eval.binding.BindingIteratorStack());
    xx.scope.pushNamed(ekOpt.get(), elementBinding);
    EResult<CodeValue> bodyOptResult;
    try {
      bodyOptResult = constEval(xx, e.body());
    } finally {
      xx.scope.truncate(initialScopeDepth);
    }
    var bodyOpt = erb.mustErMaybeUnreachable(bodyOptResult);
    if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
    // Merge the body-ran scope with the body-never-ran (initial) scope.
    var bodyRanScope = xx.scope.fork();
    var others = new TList<Scope>();
    others.add(initialScope);
    var mergeRes = bodyRanScope.merge(xx, loc, others);
    if (mergeRes.hasErrors()) return mergeRes.reerr();
    xx.scope.replace(bodyRanScope);
    var out = erb.mustErMaybeUnreachable(iterable.aspectIterableForeach(xx, loc, base, elementScopeId, bodyOpt.get()));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(out.get());
  }

  private static EResult<CodeValue> constEvalMatch(ExprContext xx, QExprId loc, Expr.Match e) {
    var erb = new Erb(loc);
    var bopt = erb.mustEr(constEval(xx, e.base()));
    var ekopt = erb.mustEr(constEval(xx, e.elementKey()));
    if (bopt.isEmpty() || ekopt.isEmpty()) return EResult.errs(erb.errors());
    var base = bopt.get();
    var ekCv = ekopt.get();
    var ekv = (Opt.Some<Value>) ekCv.value();
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.isEmpty()) return EResult.errs(erb.errors());
    var elementKey = ekOpt.get();
    var bv = (Opt.Some<Value>) base.value();
    var baseType = bv.value().valueType();
    var unionOpt = erb.mustEr(GenAspectUnion.get(xx, loc, baseType));
    if (unionOpt.isEmpty()) return EResult.errs(erb.errors());
    var variants = unionOpt.get().aspectUnionVariants();
    var caseTags = new TList<ConstKey>();
    for (var c : e.cases()) {
      var condOpt = erb.mustEr(constEval(xx, c.condition()));
      if (condOpt.isEmpty()) return EResult.errs(erb.errors());
      var cv = (Opt.Some<Value>) condOpt.get().value();
      var tagOpt = erb.mustRes(Eval.extractConstKey(xx, loc, cv.value()));
      if (tagOpt.isEmpty()) return EResult.errs(erb.errors());
      var tag = tagOpt.get();
      boolean found = false;
      for (var info : variants) {
        if (info.key().equals(tag)) {
          found = true;
          break;
        }
      }
      if (!found) {
        erb.errPush("Match case tag '" + tag + "' not found in union");
        return EResult.errs(erb.errors());
      }
      caseTags.add(tag);
    }
    boolean exhaustive = true;
    for (var info : variants) {
      if (!caseTags.contains(info.key())) {
        exhaustive = false;
        break;
      }
    }
    if (!exhaustive && !(e.fallback() instanceof Opt.Some<Expr>)) {
      for (var info : variants) {
        if (!caseTags.contains(info.key())) {
          erb.errPush("Non-exhaustive match: missing case for tag '" + info.key() + "'");
          return EResult.errs(erb.errors());
        }
      }
    }
    if (exhaustive && e.fallback() instanceof Opt.Some<Expr>) {
      erb.errPush("Exhaustive match must not have a fallback");
      return EResult.errs(erb.errors());
    }
    compilerxyz.eval.value.source_union.TypeSourceUnionDynamic sud;
    if (baseType instanceof compilerxyz.eval.value.source_union.TypeSourceUnionDynamic direct) {
      sud = direct;
    } else if (baseType instanceof compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst wrap) {
      sud = wrap.type_();
    } else {
      erb.errPush("Match: base type is not a source union: " + baseType.typeDesc());
      return EResult.errs(erb.errors());
    }
    var unionIndex = sud.set.unions.indexOf(sud);
    if (unionIndex < 0) {
      erb.errPush("Match: source union not in owning DefinitionSet");
      return EResult.errs(erb.errors());
    }
    xx.vx.definitionSets.putIfAbsent(sud.set.defSetId, sud.set);
    var matchCases = new TList<TargetCase>(e.cases().size());
    for (int i = 0; i < e.cases().size(); i++) {
      var tag = caseTags.get(i);
      int variantIdx = -1;
      for (int j = 0; j < sud.variants.size(); j++) {
        if (sud.variants.get(j).tag.equals(tag)) {
          variantIdx = j;
          break;
        }
      }
      if (variantIdx < 0) {
        erb.errPush("Match: tag not found in union variants: " + tag.toCanonical());
        return EResult.errs(erb.errors());
      }
      var variant = sud.variants.get(variantIdx);
      var variantBinaryName =
          compilerxyz.eval.dynamic.SourceUnionEmitter.variantBinaryNameFor(
              sud.set.defSetId, unionIndex, variantIdx);
      matchCases.add(
          new TargetCase(
              variant.jsTag,
              variant.type,
              variantBinaryName,
              e.cases().get(i).body()));
    }
    var out = erb.mustErMaybeUnreachable(xx.target.targetMatch(xx, loc, base, elementKey, matchCases, e.fallback()));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(out.get());
  }


  private static EResult<CodeValue> constEvalIf(ExprContext xx, QExprId loc, Expr.If e) {
    var erb = new Erb(loc);
    var conditionsEvaluated = new TList<IfEval>();
    boolean allConst = true;
    for (var branch : e.conditions()) {
      var condOpt = erb.mustEr(constEval(xx, branch.condition()));
      if (condOpt.isEmpty()) return EResult.errs(erb.errors());
      var cond = condOpt.get();
      var cv = (Opt.Some<Value>) cond.value();
      var condType = cv.value().valueType();
      var boolAspect = GenAspectBoolConst.getMaybe(xx, condType);
      if (boolAspect instanceof Opt.Some<AspectBoolConst> some) {
        var val = some.value().aspectBoolConstGet(cv.value());
        conditionsEvaluated.add(new IfEval(val, cond, branch.body()));
      } else {
        allConst = false;
        conditionsEvaluated.add(new IfEval(false, cond, branch.body()));
      }
    }
    if (allConst) {
      // Conditions are folded away, but the code they carry still has to run, in order, up to
      // and including the condition that selected the taken branch.
      var carried = Code.none(loc);
      for (var ie : conditionsEvaluated) {
        carried = Code.concat(loc, carried, ie.cond.code());
        if (ie.val) {
          var bodyOpt = erb.mustErMaybeUnreachable(constEval(xx, ie.body));
          if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
          var body = bodyOpt.get();
          return EResult.ok(
              new CodeValue(Code.concat(loc, carried, body.code()), body.outcome()));
        }
      }
      if (e.else_() instanceof Opt.Some<Expr> els) {
        var elseOpt = erb.mustErMaybeUnreachable(constEval(xx, els.value()));
        if (elseOpt.isEmpty()) return EResult.errs(erb.errors());
        var elseCv = elseOpt.get();
        return EResult.ok(
            new CodeValue(Code.concat(loc, carried, elseCv.code()), elseCv.outcome()));
      }
      return EResult.ok(new CodeValue(carried, EvalOutcome.ofValue(new compilerxyz.eval.value.object_.ValueObjectConst(new compilerxyz.eval.value.null_.TypeNullConst(), null))));
    } else {
      var branches = new TList<IfBranch>(conditionsEvaluated.size());
      for (var ie : conditionsEvaluated) {
        branches.add(new IfBranch(ie.cond, ie.body));
      }
      var out = erb.mustErMaybeUnreachable(xx.target.targetIf(xx, loc, branches, e.else_()));
      if (out.isEmpty()) return EResult.errs(erb.errors());
      return EResult.ok(out.get());
    }
  }

  private record IfEval(boolean val, CodeValue cond, Expr body) {}

  private static EResult<CodeValue> constEvalTry(ExprContext xx, QExprId loc, Expr.Try e) {
    if (!(e.catchClause() instanceof Opt.Some<Expr.TryCatch>)
        && !(e.finallyBlock() instanceof Opt.Some<Expr>)) {
      return EResult.err(loc, "Try requires at least one of catch/finally");
    }
    var erb = new Erb(loc);
    Type bodyExcType;
    if (e.catchClause() instanceof Opt.Some<Expr.TryCatch> catchSome) {
      var catchObj = catchSome.value();
      var etCvOpt = erb.mustEr(constEval(xx, catchObj.exceptionType()));
      if (etCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var etCv = etCvOpt.get();
      var etv = (Opt.Some<Value>) etCv.value();
      var etMeta = etv.value().valueType();
      var atOpt = erb.mustEr(GenAspectType.get(xx, loc, etMeta));
      if (atOpt.isEmpty()) return EResult.errs(erb.errors());
      bodyExcType = atOpt.get().aspectTypeType(etv.value());
    } else {
      bodyExcType = xx.currentExceptionType;
    }
    var bodyXx = xx.withExceptionType(bodyExcType);
    // Fork scope for the try body — it and the catch body run in exclusive
    // control-flow branches (either the body completes without throwing or
    // the catch fires) that must be merged into one post-try scope.
    var initialScope = xx.scope.fork();
    var bodyOpt = erb.mustErMaybeUnreachable(constEval(bodyXx, e.body()));
    if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
    var bodyRanScope = xx.scope.fork();
    var bodyCv = bodyOpt.get();
    Type bodyType;
    if (bodyCv.value() instanceof Opt.Some<Value> bv2) {
      bodyType = bv2.value().valueType();
    } else {
      bodyType = null;
    }
    if (bodyCv.code().dynamic() == null && bodyCv.value() instanceof Opt.Some<Value>) {
      erb.errPush("Try body must produce CodeDynamic (got Code.none)");
      return EResult.errs(erb.errors());
    }
    Opt<Value> resultValue = bodyCv.value();
    Opt<TryCatchClause> catch_ = Opt.none();
    if (e.catchClause() instanceof Opt.Some<Expr.TryCatch> catchSome2) {
      var catchObj = catchSome2.value();
      var bnCvOpt = erb.mustEr(constEval(xx, catchObj.bindingName()));
      if (bnCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bnCv = bnCvOpt.get();
      var bnv = (Opt.Some<Value>) bnCv.value();
      var bnCkOpt =
          erb.mustRes(Eval.extractConstKey(xx, catchObj.bindingName().loc(), bnv.value()));
      if (bnCkOpt.isEmpty()) return EResult.errs(erb.errors());
      var bindingId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      xx.scope.replace(initialScope.fork());
      var catchInitialDepth = xx.scope.depth();
      var ibMaybe = GenAspectInitialBind.getMaybe(xx, bodyExcType);
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        xx.scope.pushNamed(
            bnCkOpt.get(), ibSome.value().aspectInitialBindInitialBind(bindingId));
      }
      EResult<CodeValue> catchCvOptResult;
      try {
        catchCvOptResult = constEval(xx, catchObj.body());
      } finally {
        xx.scope.truncate(catchInitialDepth);
      }
      var catchRanScope = xx.scope.fork();
      var mergeOthers = new TList<Scope>();
      mergeOthers.add(catchRanScope);
      var mergeRes = bodyRanScope.merge(xx, loc, mergeOthers);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.replace(bodyRanScope);
      var catchCvOpt = erb.mustErMaybeUnreachable(catchCvOptResult);
      if (catchCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var catchCv = catchCvOpt.get();
      if (bodyType != null && catchCv.value() instanceof Opt.Some<Value>) {
        var convOpt = erb.mustErMaybeUnreachable(Eval.typecheckConvert(xx, loc, bodyType, catchCv));
        if (convOpt.isEmpty()) return EResult.errs(erb.errors());
        catchCv = convOpt.get();
      }
      if (!(resultValue instanceof Opt.Some<Value>)) {
        resultValue = catchCv.value();
      }
      if (catchCv.code().dynamic() == null && catchCv.value() instanceof Opt.Some<Value>) {
        erb.errPush("Try catch body must produce CodeDynamic (got Code.none)");
        return EResult.errs(erb.errors());
      }
      var payloadConsumeRes = compilerxyz.gen.GenAspectConsumeDynamic.get(xx, loc, bodyExcType);
      if (payloadConsumeRes.hasErrors()) return payloadConsumeRes.reerr();
      var payloadDesc =
          payloadConsumeRes
              .requireValue("try/catch payload AspectConsumeDynamic")
              .aspectConsumeDynamicJvmClass(xx);
      catch_ = Opt.some(new TryCatchClause(bindingId, payloadDesc, catchCv));
    }
    Opt<CodeDynamic> finally_ = Opt.none();
    if (e.finallyBlock() instanceof Opt.Some<Expr> finSome) {
      var finCvOpt = erb.mustErMaybeUnreachable(constEval(xx, finSome.value()));
      if (finCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var finCv = finCvOpt.get();
      var finCd = finCv.code().dynamic();
      if (finCd == null) {
        erb.errPush("Try finally body must produce CodeDynamic (got Code.none)");
        return EResult.errs(erb.errors());
      }
      // A finally block has no result: whatever value it produced is dropped, so every exit path
      // leaves the stack exactly as it found it.
      var finallyDropOpt = erb.mustEr(xx.target.targetDrop(xx, loc, finCv));
      if (finallyDropOpt.isEmpty()) return EResult.errs(erb.errors());
      Code finallyDropCode = finallyDropOpt.get();
      CodeDynamic combinedFinally;
      if (!finallyDropCode.isNone()) {
        var combinedOpt =
            erb.mustEr(
                xx.target.targetSeq(loc, TList.of(Code.of(loc, finCd), finallyDropCode)));
        if (combinedOpt.isEmpty()) return EResult.errs(erb.errors());
        var combinedCode = combinedOpt.get();
        if (combinedCode.dynamic() == null) {
          erb.errPush("Try finally combined must produce CodeDynamic (got Code.none)");
          return EResult.errs(erb.errors());
        }
        combinedFinally = combinedCode.dynamic();
      } else {
        combinedFinally = finCd;
      }
      finally_ = Opt.some(combinedFinally);
    }
    var tryOpt =
        erb.mustEr(xx.target.targetTryCatchFinally(xx, loc, bodyCv, catch_, finally_));
    if (tryOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(new CodeValue(Code.of(loc, tryOpt.get()),
        resultValue instanceof Opt.Some<Value> rvs ? EvalOutcome.ofValue(rvs.value()) : EvalOutcome.NO_VALUE));
  }

  private static EResult<CodeValue> constEvalStage(ExprContext xx, QExprId loc, Expr.Stage e) {
    var matchedLowers = new TList<Expr>();
    findMatchedLowersInner(e.expr(), 0, 0, matchedLowers);
    var matchedGrafts = new TList<Expr>();
    findMatchedGraftsInner(e.expr(), 0, matchedGrafts);
    if (matchedLowers.isEmpty() && matchedGrafts.isEmpty()) {

      return EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(e.expr())));
    }
    var erb = new Erb(loc);
    var lowerValues = new TList<CodeValue>(matchedLowers.size());
    for (var inner : matchedLowers) {
      var cvOpt = erb.mustEr(constEval(xx, inner));
      if (cvOpt.isEmpty()) return EResult.errs(erb.errors());
      lowerValues.add(cvOpt.get());
    }
    var graftValues = new TList<CodeValue>(matchedGrafts.size());
    for (var inner : matchedGrafts) {
      var cvOpt = erb.mustEr(constEval(xx, inner));
      if (cvOpt.isEmpty()) return EResult.errs(erb.errors());
      graftValues.add(cvOpt.get());
    }
    var staged = erb.mustEr(xx.target.targetStage(xx, loc, e.expr(), lowerValues, graftValues));
    if (staged.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(staged.get());
  }

  private static void findMatchedLowersInner(Expr expr, int sd, int ld, TList<Expr> out) {
    switch (expr.variant()) {
      case Expr.Lower lo -> {
        int newLd = ld + 1;
        if (newLd - sd == 1) {
          out.add(lo.expr());
        } else {
          findMatchedLowersInner(lo.expr(), sd, newLd, out);
        }
      }
      case Expr.Stage st -> findMatchedLowersInner(st.expr(), sd + 1, ld, out);
      case Expr.Graft.Pending p -> findMatchedLowersInner(p.expr(), sd, ld, out);
      case Expr.Graft.Evaluated ignored -> {}
      case Expr.Literal ignored -> {}
      case Expr.ScopeUse ignored -> {}
      case Expr.Labeled lb -> findMatchedLowersInner(lb.inner(), sd, ld, out);
      case Expr.Return rt -> {
        if (rt.value() instanceof Opt.Some<Expr> someV) findMatchedLowersInner(someV.value(), sd, ld, out);
      }
      case Expr.As as -> {
        findMatchedLowersInner(as.base(), sd, ld, out);
        findMatchedLowersInner(as.target(), sd, ld, out);
      }
      case Expr.Call c -> {
        findMatchedLowersInner(c.func(), sd, ld, out);
        findMatchedLowersInner(c.arg(), sd, ld, out);
      }
      case Expr.Bind b -> {
        findMatchedLowersInner(b.base(), sd, ld, out);
        findMatchedLowersInner(b.value(), sd, ld, out);
      }
      case Expr.Set s -> {
        findMatchedLowersInner(s.base(), sd, ld, out);
        findMatchedLowersInner(s.value(), sd, ld, out);
      }
      case Expr.Field f -> {
        findMatchedLowersInner(f.base(), sd, ld, out);
        findMatchedLowersInner(f.key(), sd, ld, out);
      }
      case Expr.Seq sq -> {
        for (var sub : sq.exprs()) findMatchedLowersInner(sub, sd, ld, out);
      }
      case Expr.Tuple t -> {
        for (var el : t.elements()) findMatchedLowersInner(el, sd, ld, out);
      }
      case Expr.Record r -> {
        for (var entry : r.entries()) {
          findMatchedLowersInner(entry.key(), sd, ld, out);
          findMatchedLowersInner(entry.value(), sd, ld, out);
        }
      }
      case Expr.If ifv -> {
        for (var b : ifv.conditions()) {
          findMatchedLowersInner(b.condition(), sd, ld, out);
          findMatchedLowersInner(b.body(), sd, ld, out);
        }
        if (ifv.else_() instanceof Opt.Some<Expr> el) {
          findMatchedLowersInner(el.value(), sd, ld, out);
        }
      }
      case Expr.OperatorLt o -> {
        findMatchedLowersInner(o.base(), sd, ld, out);
        findMatchedLowersInner(o.reference(), sd, ld, out);
      }
      case Expr.OperatorEq o -> {
        findMatchedLowersInner(o.base(), sd, ld, out);
        findMatchedLowersInner(o.reference(), sd, ld, out);
      }
      case Expr.OperatorAdd o -> {
        findMatchedLowersInner(o.base(), sd, ld, out);
        findMatchedLowersInner(o.reference(), sd, ld, out);
      }
      case Expr.OperatorMod o -> {
        findMatchedLowersInner(o.base(), sd, ld, out);
        findMatchedLowersInner(o.reference(), sd, ld, out);
      }
      case Expr.Foreach fe -> {
        findMatchedLowersInner(fe.base(), sd, ld, out);
        findMatchedLowersInner(fe.elementKey(), sd, ld, out);
        findMatchedLowersInner(fe.body(), sd, ld, out);
      }
      case Expr.Match m -> {
        findMatchedLowersInner(m.base(), sd, ld, out);
        findMatchedLowersInner(m.elementKey(), sd, ld, out);
        for (var c : m.cases()) {
          findMatchedLowersInner(c.condition(), sd, ld, out);
          findMatchedLowersInner(c.body(), sd, ld, out);
        }
        if (m.fallback() instanceof Opt.Some<Expr> f) {
          findMatchedLowersInner(f.value(), sd, ld, out);
        }
      }
      case Expr.Try tr -> {
        findMatchedLowersInner(tr.body(), sd, ld, out);
        if (tr.catchClause() instanceof Opt.Some<Expr.TryCatch> cs) {
          findMatchedLowersInner(cs.value().bindingName(), sd, ld, out);
          findMatchedLowersInner(cs.value().exceptionType(), sd, ld, out);
          findMatchedLowersInner(cs.value().body(), sd, ld, out);
        }
        if (tr.finallyBlock() instanceof Opt.Some<Expr> f) {
          findMatchedLowersInner(f.value(), sd, ld, out);
        }
      }
    }
  }

  private static void findMatchedGraftsInner(Expr expr, int sd, TList<Expr> out) {
    switch (expr.variant()) {
      case Expr.Graft.Pending p -> {
        if (sd == 0) out.add(p.expr());
        else findMatchedGraftsInner(p.expr(), sd, out);
      }
      case Expr.Graft.Evaluated ignored -> {}
      case Expr.Stage st -> findMatchedGraftsInner(st.expr(), sd + 1, out);
      case Expr.Lower lo -> findMatchedGraftsInner(lo.expr(), sd, out);
      case Expr.Literal ignored -> {}
      case Expr.ScopeUse ignored -> {}
      case Expr.Labeled lb -> findMatchedGraftsInner(lb.inner(), sd, out);
      case Expr.Return rt -> {
        if (rt.value() instanceof Opt.Some<Expr> someV) findMatchedGraftsInner(someV.value(), sd, out);
      }
      case Expr.As as -> {
        findMatchedGraftsInner(as.base(), sd, out);
        findMatchedGraftsInner(as.target(), sd, out);
      }
      case Expr.Call c -> {
        findMatchedGraftsInner(c.func(), sd, out);
        findMatchedGraftsInner(c.arg(), sd, out);
      }
      case Expr.Bind b -> {
        findMatchedGraftsInner(b.base(), sd, out);
        findMatchedGraftsInner(b.value(), sd, out);
      }
      case Expr.Set s -> {
        findMatchedGraftsInner(s.base(), sd, out);
        findMatchedGraftsInner(s.value(), sd, out);
      }
      case Expr.Field f -> {
        findMatchedGraftsInner(f.base(), sd, out);
        findMatchedGraftsInner(f.key(), sd, out);
      }
      case Expr.Seq sq -> {
        for (var sub : sq.exprs()) findMatchedGraftsInner(sub, sd, out);
      }
      case Expr.Tuple t -> {
        for (var el : t.elements()) findMatchedGraftsInner(el, sd, out);
      }
      case Expr.Record r -> {
        for (var entry : r.entries()) {
          findMatchedGraftsInner(entry.key(), sd, out);
          findMatchedGraftsInner(entry.value(), sd, out);
        }
      }
      case Expr.If ifv -> {
        for (var b : ifv.conditions()) {
          findMatchedGraftsInner(b.condition(), sd, out);
          findMatchedGraftsInner(b.body(), sd, out);
        }
        if (ifv.else_() instanceof Opt.Some<Expr> el) {
          findMatchedGraftsInner(el.value(), sd, out);
        }
      }
      case Expr.OperatorLt o -> {
        findMatchedGraftsInner(o.base(), sd, out);
        findMatchedGraftsInner(o.reference(), sd, out);
      }
      case Expr.OperatorEq o -> {
        findMatchedGraftsInner(o.base(), sd, out);
        findMatchedGraftsInner(o.reference(), sd, out);
      }
      case Expr.OperatorAdd o -> {
        findMatchedGraftsInner(o.base(), sd, out);
        findMatchedGraftsInner(o.reference(), sd, out);
      }
      case Expr.OperatorMod o -> {
        findMatchedGraftsInner(o.base(), sd, out);
        findMatchedGraftsInner(o.reference(), sd, out);
      }
      case Expr.Foreach fe -> {
        findMatchedGraftsInner(fe.base(), sd, out);
        findMatchedGraftsInner(fe.elementKey(), sd, out);
        findMatchedGraftsInner(fe.body(), sd, out);
      }
      case Expr.Match m -> {
        findMatchedGraftsInner(m.base(), sd, out);
        findMatchedGraftsInner(m.elementKey(), sd, out);
        for (var c : m.cases()) {
          findMatchedGraftsInner(c.condition(), sd, out);
          findMatchedGraftsInner(c.body(), sd, out);
        }
        if (m.fallback() instanceof Opt.Some<Expr> f) {
          findMatchedGraftsInner(f.value(), sd, out);
        }
      }
      case Expr.Try tr -> {
        findMatchedGraftsInner(tr.body(), sd, out);
        if (tr.catchClause() instanceof Opt.Some<Expr.TryCatch> cs) {
          findMatchedGraftsInner(cs.value().bindingName(), sd, out);
          findMatchedGraftsInner(cs.value().exceptionType(), sd, out);
          findMatchedGraftsInner(cs.value().body(), sd, out);
        }
        if (tr.finallyBlock() instanceof Opt.Some<Expr> f) {
          findMatchedGraftsInner(f.value(), sd, out);
        }
      }
    }
  }
}
