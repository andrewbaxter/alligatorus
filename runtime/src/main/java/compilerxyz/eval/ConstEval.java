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
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeJump;
import compilerxyz.eval.dynamic.JavaBytecodeLand;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JumpKey;
import compilerxyz.eval.dynamic.JvmSort;
import compilerxyz.eval.value.ValueDynamic;
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
import compilerxyz.gen.GenAspectOperatorSub;
import compilerxyz.gen.GenAspectOperatorMul;
import compilerxyz.gen.GenAspectOperatorDiv;
import compilerxyz.gen.GenAspectOperatorMod;
import compilerxyz.gen.GenAspectOperatorAnd;
import compilerxyz.gen.GenAspectOperatorOr;
import compilerxyz.gen.GenAspectOperatorXor;
import compilerxyz.gen.GenAspectOperatorShl;
import compilerxyz.gen.GenAspectOperatorShr;
import compilerxyz.gen.GenAspectOperatorUshr;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.GenAspectOperatorNe;
import compilerxyz.gen.GenAspectOperatorLt;
import compilerxyz.gen.GenAspectOperatorLe;
import compilerxyz.gen.GenAspectOperatorGt;
import compilerxyz.gen.GenAspectOperatorGe;
import compilerxyz.gen.GenAspectOperatorNeg;
import compilerxyz.gen.GenAspectOperatorBitNot;
import compilerxyz.syntax.BinaryOperator;
import compilerxyz.syntax.UnaryOperator;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenAspectUnion;

public final class ConstEval {
  private ConstEval() {}

  public static EResult<EvalRes> constEval(ExprContext xx, Expr expr) {
    var loc = expr.loc();
    return switch (expr.variant()) {
      case Expr.Literal lit ->
          EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(lit.value().consume()))));
      case Expr.ScopeUse p -> {
        var erb = new Erb(loc);
        var keyOpt = erb.mustCv(constEval(xx, p.key()));
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
        yield EResult.ok(EvalRes.of(found.bindingUse(xx, loc, key.code())));
      }
      case Expr.Call p -> {
        var erb = new Erb(loc);
        var fopt = erb.mustCv(constEval(xx, p.func()));
        var aopt = erb.mustCv(constEval(xx, p.arg()));
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
        yield EResult.ok(EvalRes.of(wrapped.get()));
      }
      case Expr.Set e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(constEval(xx, e.base()));
        var vopt = erb.mustCv(constEval(xx, e.value()));
        if (bopt.isEmpty() || vopt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var value = vopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var baseType = bv.value().valueType();
        var setOpt = erb.mustEr(GenAspectSet.get(xx, loc, baseType));
        if (setOpt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(setOpt.get().aspectSetSet(xx, loc, base, value));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.get()));
      }
      case Expr.Field e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(constEval(xx, e.base()));
        var fopt = erb.mustCv(constEval(xx, e.key()));
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
        yield EResult.ok(EvalRes.of(primaryOpt.get()));
      }
      case Expr.Bind ignored ->
          EResult.err(loc, "Bind expression must be directly inside a block");
      case Expr.Seq e -> constEvalSeq(xx, loc, e);
      case Expr.Stage e -> constEvalStage(xx, loc, e);
      case Expr.Lower lo -> constEval(xx, lo.expr());
      case Expr.Graft.Evaluated ev -> EResult.ok(EvalRes.of(ev.value()));
      case Expr.Graft.Pending ignored ->
          EResult.err(loc, "Graft outside of stage processing — its inner was never evaluated");
      case Expr.Tuple t -> {
        var erb = new Erb(loc);
        var elements = new TList<TupleElement>(t.elements().size());
        for (var elExpr : t.elements()) {
          var elRes = elementResult(constEval(xx, elExpr));
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
        yield EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(new Tuple(Opt.none(), elements)))));
      }
      case Expr.Record r -> constEvalRecord(xx, loc, r);
      case Expr.OperatorBinary e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(constEval(xx, e.base()));
        var ropt = erb.mustCv(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var out =
            erb.mustEr(constEvalBinaryOp(xx, loc, e.op(), bv.value().valueType(), base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.get()));
      }
      case Expr.OperatorUnary e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(constEval(xx, e.base()));
        if (bopt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var bv = (Opt.Some<Value>) base.value();
        var out = erb.mustEr(constEvalUnaryOp(xx, loc, e.op(), bv.value().valueType(), base));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.get()));
      }
      case Expr.Foreach e -> {
        int baseDepth = xx.scope.depth();
        yield boundaryClose(xx, loc, constEvalForeach(xx, loc, e), true, Opt.none(), baseDepth, Opt.none());
      }
      case Expr.Match e -> {
        int baseDepth = xx.scope.depth();
        yield boundaryClose(xx, loc, constEvalMatch(xx, loc, e), true, Opt.none(), baseDepth, Opt.none());
      }
      case Expr.If e -> {
        int baseDepth = xx.scope.depth();
        yield boundaryClose(xx, loc, constEvalIf(xx, loc, e), true, Opt.none(), baseDepth, Opt.none());
      }
      case Expr.As e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(constEval(xx, e.base()));
        var topt = erb.mustCv(constEval(xx, e.target()));
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
        yield EResult.ok(EvalRes.of(converted.get()));
      }
      case Expr.Try e -> constEvalTry(xx, loc, e);
      case Expr.Labeled e -> {
        int baseDepth = xx.scope.depth();
        yield boundaryClose(
            xx, loc, constEval(xx, e.inner()), true, Opt.some(e.label()), baseDepth, Opt.none());
      }
      case Expr.Return e -> constEvalReturn(xx, loc, e);
    };
  }

  /** Element/operand-position result: pending exits are errors, value shape preserved. */
  private static EResult<CodeValue> elementResult(EResult<EvalRes> r) {
    var v = r.valueOrNull();
    if (v == null) return r.reerr();
    if (!v.exits().isEmpty()) {
      var errs = new TList<ExprErr>();
      r.drainInto(errs);
      for (var exit : v.exits()) errs.add(Erb.exitCrossError(exit));
      return EResult.errs(errs);
    }
    var errs = r.errorsOrNull();
    if (errs == null || errs.isEmpty()) return EResult.ok(v.cv());
    var copied = new TList<ExprErr>();
    copied.addAll(errs);
    return EResult.mixed(v.cv(), copied);
  }

  /**
   * Close a control construct over the exits its body produced. Consumed exits
   * (matching label, or unlabeled when {@code consumeUnlabeled}) converge with
   * the fall-through path at a land appended after the body; their scope forks
   * merge with the fall-through scope. Exits that pass onward drop this
   * construct's scope levels — using the exit's own scope fork, so the drop
   * code reflects the state at the exit point — run {@code passThroughCode}
   * (a try's finally), and jump on under a fresh key with the dropped levels
   * popped from their fork.
   */
  private static EResult<EvalRes> boundaryClose(
      ExprContext xx,
      QExprId loc,
      EResult<EvalRes> innerResult,
      boolean consumeUnlabeled,
      Opt<String> consumeLabel,
      int baseDepth,
      Opt<CodeDynamic> passThroughCode) {
    if (innerResult.valueOrNull() == null) return innerResult;
    var erb = new Erb(loc);
    innerResult.drainInto(erb.errors());
    var inner = innerResult.requireValue("boundaryClose inner");
    var innerCv = inner.cv();

    var consumed = new TList<Exit>();
    var passed = new TList<Exit>();
    var trampolines = new JavaBytecodeSequence();
    boolean anyTrampoline = false;
    var consKey = new JumpKey();
    var consumedLandKeys = new TList<JumpKey>();

    for (var exit : inner.exits()) {
      boolean matches =
          exit.label instanceof Opt.Some<String> lbl
              ? (consumeLabel instanceof Opt.Some<String> cl && cl.value().equals(lbl.value()))
              : consumeUnlabeled;
      boolean needsDrops = exit.scope.depth() > baseDepth;
      boolean needsCode = needsDrops || passThroughCode instanceof Opt.Some<CodeDynamic>;
      if (!matches && !needsCode) {
        passed.add(exit);
        continue;
      }
      if (matches && !needsCode) {
        consumedLandKeys.add(exit.key);
        consumed.add(exit);
        continue;
      }
      var tramp = new JavaBytecodeSequence();
      tramp.add(new CodeDynamic(loc, new JavaBytecodeLand(exit.key)));
      JavaBytecodeBindingKey parkKey = null;
      Type parkType = null;
      if (exit.stackCarried) {
        parkType = exit.value instanceof ValueDynamic vd ? vd.type_() : exit.value.valueType();
        parkKey = new JavaBytecodeBindingKey();
        tramp.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(JvmSort.storeOpcode(xx, parkType), parkKey)));
      }
      for (int i = exit.scope.depth() - 1; i >= baseDepth; i--) {
        var dropRes = exit.scope.entries.get(i).bindingDrop(xx, exit.loc);
        if (dropRes.hasErrors()) {
          dropRes.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        var dropDyn = dropRes.requireValue("boundaryClose exit drop").dynamic();
        if (dropDyn != null) tramp.add(dropDyn);
      }
      if (passThroughCode instanceof Opt.Some<CodeDynamic> pt) {
        tramp.add(pt.value());
      }
      if (parkKey != null) {
        tramp.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(JvmSort.loadOpcode(xx, parkType), parkKey)));
      }
      exit.scope.truncate(baseDepth);
      if (matches) {
        tramp.add(new CodeDynamic(loc, new JavaBytecodeJump(consKey)));
        consumed.add(exit);
      } else {
        var newKey = new JumpKey();
        tramp.add(new CodeDynamic(loc, new JavaBytecodeJump(newKey)));
        passed.add(new Exit(exit.loc, exit.label, newKey, exit.value, exit.stackCarried, exit.scope));
      }
      trampolines.add(new CodeDynamic(loc, tramp));
      anyTrampoline = true;
    }

    if (!consumed.isEmpty()) {
      var exitScopes = new TList<Scope>(consumed.size());
      for (var c : consumed) exitScopes.add(c.scope);
      var mergeRes = xx.scope.merge(xx, loc, exitScopes);
      if (mergeRes.hasErrors()) {
        mergeRes.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
    }

    boolean fallsThrough = !(innerCv.outcome() instanceof EvalOutcome.ControlEnded);
    var seq = new JavaBytecodeSequence();
    var innerDyn = innerCv.code().dynamic();
    if (innerDyn != null) seq.add(innerDyn);
    if (anyTrampoline) {
      if (fallsThrough) {
        seq.add(new CodeDynamic(loc, new JavaBytecodeJump(consKey)));
      }
      seq.add(new CodeDynamic(loc, trampolines));
    }
    for (var k : consumedLandKeys) {
      seq.add(new CodeDynamic(loc, new JavaBytecodeLand(k)));
    }
    seq.add(new CodeDynamic(loc, new JavaBytecodeLand(consKey)));

    Opt<Value> resultValue =
        consumed.isEmpty() ? innerCv.value() : Opt.some(consumed.get(0).value);
    EvalOutcome outcome;
    if (resultValue instanceof Opt.Some<Value> s) {
      outcome = EvalOutcome.ofValue(s.value());
    } else if (innerCv.outcome() instanceof EvalOutcome.ControlEnded) {
      outcome = EvalOutcome.CONTROL_ENDED;
    } else {
      outcome = EvalOutcome.NO_VALUE;
    }
    var cv = new CodeValue(Code.of(new CodeDynamic(loc, seq)), outcome);
    var res = new EvalRes(cv, passed);
    if (erb.errors().isEmpty()) return EResult.ok(res);
    return EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalReturn(ExprContext xx, QExprId loc, Expr.Return e) {
    var erb = new Erb(loc);
    CodeValue valCv;
    var exits = new TList<Exit>();
    if (e.value() instanceof Opt.Some<Expr> someV) {
      var vres = constEval(xx, someV.value());
      if (vres.valueOrNull() == null) return vres;
      vres.drainInto(erb.errors());
      var vev = vres.requireValue("constEvalReturn value");
      exits.addAll(vev.exits());
      valCv = vev.cv();
    } else {
      valCv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(emptyTupleValue()));
    }
    valCv = xx.target.targetReturn(xx, loc, valCv);
    if (!(valCv.outcome() instanceof EvalOutcome.HasValue someVal)) {
      var cv = new CodeValue(valCv.code(), EvalOutcome.CONTROL_ENDED);
      var res = new EvalRes(cv, exits);
      return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
    }
    var key = new JumpKey();
    var seq = new JavaBytecodeSequence();
    var valDyn = valCv.code().dynamic();
    if (valDyn != null) seq.add(valDyn);
    seq.add(new CodeDynamic(loc, new JavaBytecodeJump(key)));
    boolean stackCarried = someVal.value() instanceof ValueDynamic;
    exits.add(new Exit(loc, e.label(), key, someVal.value(), stackCarried, xx.scope.fork()));
    var cv = new CodeValue(Code.of(new CodeDynamic(loc, seq)), EvalOutcome.CONTROL_ENDED);
    var res = new EvalRes(cv, exits);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static Value emptyTupleValue() {
    return new Tuple(Opt.none(), new TList<>());
  }

  private static EResult<EvalRes> constEvalSeq(ExprContext xx, QExprId loc, Expr.Seq e) {
    var erb = new Erb(loc);
    var initialScopeDepth = xx.scope.depth();

    class Frame {
      final TList<Code> codes = new TList<>();
      @org.jspecify.annotations.Nullable Binding segmentSnapshot = null;
      int scopeIndex = -1;
    }

    var frames = new TList<Frame>();
    frames.add(new Frame());
    var exits = new TList<Exit>();
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
        var bopt = erb.mustCv(constEval(xx, bind.base()));
        var vres = constEval(xx, bind.value());
        if (vres.valueOrNull() == null) {
          vres.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        vres.drainInto(erb.errors());
        var vev = vres.requireValue("constEvalSeq bind value");
        exits.addAll(vev.exits());
        if (bopt.isEmpty()) return EResult.errs(erb.errors());
        var base = bopt.get();
        var value = vev.cv();
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
        frames.add(nf);
        xx.scope.pushNamed(ckOpt.get(), br.binding());
        nf.scopeIndex = xx.scope.depth() - 1;
        nf.segmentSnapshot = br.binding().bindingFork();
        lastVal = Opt.some(emptyTupleValue());
      } else {
        var rres = constEval(xx, sub);
        if (rres.valueOrNull() == null) {
          rres.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        rres.drainInto(erb.errors());
        var rev = rres.requireValue("constEvalSeq entry");
        exits.addAll(rev.exits());
        var r = rev.cv();
        frames.get(frames.size() - 1).codes.add(r.code());
        if (r.outcome() instanceof EvalOutcome.ControlEnded) {
          controlEnded = true;
          lastVal = Opt.none();
          continue;
        }
        pendingDiscard = r;
      }
      // Per-fork drop regions: when a binding's drop-relevant state changed
      // during this entry, close the exception-drop regions covering the code
      // so far so a throw before the change still drops with the pre-change
      // state. Regions close from the innermost frame down to the outermost
      // changed one; each closed chunk nests into its parent's open region.
      int firstChanged = -1;
      for (int fi = 1; fi < frames.size(); fi++) {
        var fr = frames.get(fi);
        var current = xx.scope.entries.get(fr.scopeIndex);
        if (!current.bindingEquals(xx, sub.loc(), fr.segmentSnapshot)) {
          firstChanged = fi;
          break;
        }
      }
      if (firstChanged >= 0) {
        for (int fi = frames.size() - 1; fi >= firstChanged; fi--) {
          var fr = frames.get(fi);
          var chunkRes = frameSegmentClose(xx, loc, fr.codes, fr.segmentSnapshot);
          if (chunkRes.hasErrors()) {
            chunkRes.drainInto(erb.errors());
            return EResult.errs(erb.errors());
          }
          frames.get(fi - 1).codes.add(chunkRes.requireValue("constEvalSeq segment chunk"));
          fr.codes.clear();
          fr.segmentSnapshot = xx.scope.entries.get(fr.scopeIndex).bindingFork();
        }
      }
    }
    if (pendingDiscard != null) {
      var dropOpt = erb.mustErMaybeUnreachable(xx.target.targetDrop(xx, loc, pendingDiscard));
      if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
      frames.get(frames.size() - 1).codes.add(dropOpt.get());
      pendingDiscard = null;
    }
    while (frames.size() > 1) {
      var fr = frames.remove(frames.size() - 1);
      var parent = frames.get(frames.size() - 1);
      var chunkRes = frameSegmentClose(xx, loc, fr.codes, fr.segmentSnapshot);
      if (chunkRes.hasErrors()) {
        chunkRes.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      parent.codes.add(chunkRes.requireValue("constEvalSeq unwind chunk"));
      // Normal-path end-of-scope drop from the live (final) binding state.
      var dropRes = erb.mustErMaybeUnreachable(
          xx.scope.entries.get(fr.scopeIndex).bindingDrop(xx, loc));
      if (dropRes.isEmpty()) return EResult.errs(erb.errors());
      parent.codes.add(dropRes.get());
    }
    xx.scope.truncate(initialScopeDepth);
    var outer = frames.remove(0);
    var seqOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, outer.codes));
    if (seqOpt.isEmpty()) return EResult.errs(erb.errors());
    var innerCv = new CodeValue(seqOpt.get(),
        lastVal instanceof Opt.Some<Value> lvs ? EvalOutcome.ofValue(lvs.value()) : (controlEnded ? EvalOutcome.CONTROL_ENDED : EvalOutcome.NO_VALUE));
    var innerRes = new EvalRes(innerCv, exits);
    return boundaryClose(
        xx,
        loc,
        erb.errors().isEmpty() ? EResult.ok(innerRes) : EResult.mixed(innerRes, erb.errors()),
        true,
        Opt.none(),
        initialScopeDepth,
        Opt.none());
  }

  /**
   * Wrap one drop region: the codes run under an exception handler that drops
   * with the segment's snapshotted binding state and rethrows. The normal path
   * falls through without dropping — the end-of-scope drop is emitted
   * separately from the final state.
   */
  private static EResult<Code> frameSegmentClose(
      ExprContext xx, QExprId loc, TList<Code> codes, Binding snapshot) {
    var erb = new Erb(loc);
    var dropOpt = erb.mustErMaybeUnreachable(snapshot.bindingDrop(xx, loc));
    if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
    var bodyOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, codes));
    if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
    var wrappedOpt = erb.mustErMaybeUnreachable(
        xx.target.targetTryDropOnThrow(loc, bodyOpt.get(), dropOpt.get()));
    if (wrappedOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(wrappedOpt.get());
  }

  private static EResult<CodeValue> constEvalBinaryOp(
      ExprContext xx, QExprId loc, BinaryOperator op, Type baseType, CodeValue base, CodeValue reference) {
    var erb = new Erb(loc);
    return switch (op) {
      case ADD -> {
        var aopt = erb.mustEr(GenAspectOperatorAdd.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorAddAdd(xx, loc, base, reference);
      }
      case SUB -> {
        var aopt = erb.mustEr(GenAspectOperatorSub.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorSubSub(xx, loc, base, reference);
      }
      case MUL -> {
        var aopt = erb.mustEr(GenAspectOperatorMul.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorMulMul(xx, loc, base, reference);
      }
      case DIV -> {
        var aopt = erb.mustEr(GenAspectOperatorDiv.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorDivDiv(xx, loc, base, reference);
      }
      case MOD -> {
        var aopt = erb.mustEr(GenAspectOperatorMod.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorModMod(xx, loc, base, reference);
      }
      case AND -> {
        var aopt = erb.mustEr(GenAspectOperatorAnd.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorAndAnd(xx, loc, base, reference);
      }
      case OR -> {
        var aopt = erb.mustEr(GenAspectOperatorOr.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorOrOr(xx, loc, base, reference);
      }
      case XOR -> {
        var aopt = erb.mustEr(GenAspectOperatorXor.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorXorXor(xx, loc, base, reference);
      }
      case SHL -> {
        var aopt = erb.mustEr(GenAspectOperatorShl.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorShlShl(xx, loc, base, reference);
      }
      case SHR -> {
        var aopt = erb.mustEr(GenAspectOperatorShr.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorShrShr(xx, loc, base, reference);
      }
      case USHR -> {
        var aopt = erb.mustEr(GenAspectOperatorUshr.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorUshrUshr(xx, loc, base, reference);
      }
      case EQ -> {
        var aopt = erb.mustEr(GenAspectOperatorEq.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorEqEq(xx, loc, base, reference);
      }
      case NE -> {
        var aopt = erb.mustEr(GenAspectOperatorNe.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorNeNe(xx, loc, base, reference);
      }
      case LT -> {
        var aopt = erb.mustEr(GenAspectOperatorLt.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorLtLt(xx, loc, base, reference);
      }
      case LE -> {
        var aopt = erb.mustEr(GenAspectOperatorLe.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorLeLe(xx, loc, base, reference);
      }
      case GT -> {
        var aopt = erb.mustEr(GenAspectOperatorGt.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorGtGt(xx, loc, base, reference);
      }
      case GE -> {
        var aopt = erb.mustEr(GenAspectOperatorGe.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorGeGe(xx, loc, base, reference);
      }
    };
  }

  private static EResult<CodeValue> constEvalUnaryOp(
      ExprContext xx, QExprId loc, UnaryOperator op, Type baseType, CodeValue base) {
    var erb = new Erb(loc);
    return switch (op) {
      case NEG -> {
        var aopt = erb.mustEr(GenAspectOperatorNeg.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorNegNeg(xx, loc, base);
      }
      case BITNOT -> {
        var aopt = erb.mustEr(GenAspectOperatorBitNot.get(xx, loc, baseType));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        yield aopt.get().aspectOperatorBitNotBitNot(xx, loc, base);
      }
    };
  }


  private static EResult<EvalRes> constEvalRecord(ExprContext xx, QExprId loc, Expr.Record r) {
    var entries = new TOrderedMap<ConstKey, EResult<CodeValue>>();
    var keyCodes = new TList<CodeDynamic>();
    var errors = new TList<ExprErr>();
    for (var entry : r.entries()) {
      var kRes = elementResult(constEval(xx, entry.key()));
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
          var valRes = elementResult(constEval(xx, entry.value()));
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
      var seq = new JavaBytecodeSequence();
      for (var c : keyCodes) seq.add(c);
      code = Code.of(loc, new CodeDynamic(loc, seq));
    }
    var fields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : entries.entrySet()) {
      fields.put(entry.getKey(), new RecordField.EvalRes(entry.getValue()));
    }
    return EResult.ok(EvalRes.of(new CodeValue(code, EvalOutcome.ofValue(new Record(Opt.none(), fields)))));
  }

  private static EResult<EvalRes> constEvalForeach(
      ExprContext xx, QExprId loc, Expr.Foreach e) {
    var erb = new Erb(loc);
    var bopt = erb.mustCv(constEval(xx, e.base()));
    var ekopt = erb.mustCv(constEval(xx, e.elementKey()));
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
    var elementScopeId = new BindingId.Local(new JavaBytecodeBindingKey());
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
    // iterations; an exit escaping the body would leave the iterator
    // dangling. Pushing a synthetic BindingIteratorStack (whose bindingDrop
    // emits POP) has the exit's trampoline drops restore the operand stack
    // before jumping on. The element key is pushed as a named entry
    // immediately after so it shadows any outer name; the iterator sits below.
    xx.scope.push(new compilerxyz.eval.binding.BindingIteratorStack());
    xx.scope.pushNamed(ekOpt.get(), elementBinding);
    EResult<EvalRes> bodyResult;
    try {
      bodyResult = constEval(xx, e.body());
    } finally {
      xx.scope.truncate(initialScopeDepth);
    }
    if (bodyResult.valueOrNull() == null) {
      bodyResult.drainInto(erb.errors());
      return EResult.errs(erb.errors());
    }
    bodyResult.drainInto(erb.errors());
    var bodyEv = bodyResult.requireValue("constEvalForeach body");
    // Merge the body-ran scope with the body-never-ran (initial) scope.
    var bodyRanScope = xx.scope.fork();
    var others = new TList<Scope>();
    others.add(initialScope);
    var mergeRes = bodyRanScope.merge(xx, loc, others);
    if (mergeRes.hasErrors()) return mergeRes.reerr();
    xx.scope.replace(bodyRanScope);
    var out = erb.mustErMaybeUnreachable(iterable.aspectIterableForeach(xx, loc, base, elementScopeId, bodyEv.cv()));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    var res = new EvalRes(out.get(), bodyEv.exits());
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalMatch(ExprContext xx, QExprId loc, Expr.Match e) {
    var erb = new Erb(loc);
    var bopt = erb.mustCv(constEval(xx, e.base()));
    var ekopt = erb.mustCv(constEval(xx, e.elementKey()));
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
      var condOpt = erb.mustCv(constEval(xx, c.condition()));
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
    // Fork/merge scope across match cases + fallback so any type mutations
    // in one case don't leak into siblings and incompatible mutations
    // across branches surface as a compile error at the merge.
    var initialScope = xx.scope.fork();
    var branchScopes = new TList<Scope>();
    var exits = new TList<Exit>();
    compilerxyz.eval.Type resultType = null;
    boolean allControlEnded = true;
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
      var payloadBinding = new BindingId.Local(new JavaBytecodeBindingKey());
      xx.scope.replace(initialScope.fork());
      var ibMaybe = GenAspectInitialBind.getMaybe(xx, variant.type);
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        xx.scope.pushNamed(
            elementKey, ibSome.value().aspectInitialBindInitialBind(payloadBinding));
      }
      EResult<EvalRes> bodyResult;
      try {
        bodyResult = constEval(xx, e.cases().get(i).body());
      } finally {
        xx.scope.truncate(initialScope.depth());
      }
      branchScopes.add(xx.scope.fork());
      if (bodyResult.valueOrNull() == null) {
        bodyResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      bodyResult.drainInto(erb.errors());
      var bodyEv = bodyResult.requireValue("constEvalMatch case body");
      exits.addAll(bodyEv.exits());
      var bodyCv = bodyEv.cv();
      if (bodyCv.value() instanceof Opt.Some<Value> bodyVal) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = bodyVal.value().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, bodyCv));
          if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
          bodyCv = convertedOpt.get();
        }
      }
      matchCases.add(
          new TargetCase(variant.jsTag, variant.type, variantBinaryName, payloadBinding, bodyCv));
    }
    Opt<CodeValue> fallbackCv = Opt.none();
    if (e.fallback() instanceof Opt.Some<Expr> someFallback) {
      xx.scope.replace(initialScope.fork());
      var fallbackResult = constEval(xx, someFallback.value());
      branchScopes.add(xx.scope.fork());
      if (fallbackResult.valueOrNull() == null) {
        fallbackResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      fallbackResult.drainInto(erb.errors());
      var fallbackEv = fallbackResult.requireValue("constEvalMatch fallback");
      exits.addAll(fallbackEv.exits());
      var fbCv = fallbackEv.cv();
      if (fbCv.value() instanceof Opt.Some<Value> fbVal) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = fbVal.value().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, fbCv));
          if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
          fbCv = convertedOpt.get();
        }
      }
      fallbackCv = Opt.some(fbCv);
    } else if (!allControlEnded) {
      // No fallback: the "no case matched" runtime path (which shouldn't fire
      // for an exhaustive match) is treated as the untouched initial scope.
      branchScopes.add(initialScope.fork());
    }
    // Merge branch scopes back into xx.scope.
    if (branchScopes.isEmpty()) {
      xx.scope.replace(initialScope);
    } else {
      var merged = branchScopes.get(0);
      var rest = new TList<Scope>(branchScopes.size() - 1);
      for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
      var mergeRes = merged.merge(xx, loc, rest);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.replace(merged);
    }
    var out = erb.mustErMaybeUnreachable(xx.target.targetMatch(xx, loc, base, matchCases, fallbackCv));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    var res = new EvalRes(out.get(), exits);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalIf(ExprContext xx, QExprId loc, Expr.If e) {
    var erb = new Erb(loc);
    var conditionsEvaluated = new TList<IfEval>();
    boolean allConst = true;
    for (var branch : e.conditions()) {
      var condOpt = erb.mustCv(constEval(xx, branch.condition()));
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
          var bodyResult = constEval(xx, ie.body);
          if (bodyResult.valueOrNull() == null) {
            bodyResult.drainInto(erb.errors());
            return EResult.errs(erb.errors());
          }
          bodyResult.drainInto(erb.errors());
          var bodyEv = bodyResult.requireValue("constEvalIf const body");
          var body = bodyEv.cv();
          var res = new EvalRes(
              new CodeValue(Code.concat(loc, carried, body.code()), body.outcome()),
              bodyEv.exits());
          return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
        }
      }
      if (e.else_() instanceof Opt.Some<Expr> els) {
        var elseResult = constEval(xx, els.value());
        if (elseResult.valueOrNull() == null) {
          elseResult.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        elseResult.drainInto(erb.errors());
        var elseEv = elseResult.requireValue("constEvalIf const else");
        var elseCv = elseEv.cv();
        var res = new EvalRes(
            new CodeValue(Code.concat(loc, carried, elseCv.code()), elseCv.outcome()),
            elseEv.exits());
        return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
      }
      return EResult.ok(EvalRes.of(new CodeValue(carried, EvalOutcome.ofValue(new compilerxyz.eval.value.object_.ValueObjectConst(new compilerxyz.eval.value.null_.TypeNullConst(), null)))));
    }
    // Fork/merge scope across branches so any type mutations inside one
    // branch don't leak into siblings, and incompatible mutations across
    // branches surface as a compile error at the merge.
    var initialScope = xx.scope.fork();
    var branchScopes = new TList<Scope>();
    var exits = new TList<Exit>();
    compilerxyz.eval.Type resultType = null;
    var branches = new TList<IfBranch>(conditionsEvaluated.size());
    for (var ie : conditionsEvaluated) {
      xx.scope.replace(initialScope.fork());
      var bodyResult = constEval(xx, ie.body);
      branchScopes.add(xx.scope.fork());
      if (bodyResult.valueOrNull() == null) {
        bodyResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      bodyResult.drainInto(erb.errors());
      var bodyEv = bodyResult.requireValue("constEvalIf branch body");
      exits.addAll(bodyEv.exits());
      var bodyCv = bodyEv.cv();
      if (bodyCv.value() instanceof Opt.Some<Value> bodyVal) {
        if (resultType == null) {
          resultType = bodyVal.value().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, bodyCv));
          if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
          bodyCv = convertedOpt.get();
        }
      }
      branches.add(new IfBranch(ie.cond, bodyCv));
    }
    Opt<CodeValue> elseCv = Opt.none();
    if (e.else_() instanceof Opt.Some<Expr> someElse) {
      xx.scope.replace(initialScope.fork());
      var elseResult = constEval(xx, someElse.value());
      branchScopes.add(xx.scope.fork());
      if (elseResult.valueOrNull() == null) {
        elseResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      elseResult.drainInto(erb.errors());
      var elseEv = elseResult.requireValue("constEvalIf else body");
      exits.addAll(elseEv.exits());
      var eCv = elseEv.cv();
      if (eCv.value() instanceof Opt.Some<Value> elseVal) {
        if (resultType == null) {
          resultType = elseVal.value().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, eCv));
          if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
          eCv = convertedOpt.get();
        }
      }
      elseCv = Opt.some(eCv);
    } else {
      // No else: any condition may fall through — the "no branch matched"
      // path reaches the end with an implicit null value, and its scope
      // is the initial (un-mutated) scope.
      branchScopes.add(initialScope.fork());
    }
    // Merge branch scopes back into xx.scope. The first branch's fork
    // becomes the merge receiver; the rest are passed to bindingMerge.
    if (branchScopes.isEmpty()) {
      xx.scope.replace(initialScope);
    } else {
      var merged = branchScopes.get(0);
      var rest = new TList<Scope>(branchScopes.size() - 1);
      for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
      var mergeRes = merged.merge(xx, loc, rest);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.replace(merged);
    }
    var out = erb.mustErMaybeUnreachable(xx.target.targetIf(xx, loc, branches, elseCv));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    var res = new EvalRes(out.get(), exits);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private record IfEval(boolean val, CodeValue cond, Expr body) {}

  private static EResult<EvalRes> constEvalTry(ExprContext xx, QExprId loc, Expr.Try e) {
    if (!(e.catchClause() instanceof Opt.Some<Expr.TryCatch>)
        && !(e.finallyBlock() instanceof Opt.Some<Expr>)) {
      return EResult.err(loc, "Try requires at least one of catch/finally");
    }
    var erb = new Erb(loc);
    var tryBaseDepth = xx.scope.depth();
    var exits = new TList<Exit>();
    Type bodyExcType;
    if (e.catchClause() instanceof Opt.Some<Expr.TryCatch> catchSome) {
      var catchObj = catchSome.value();
      var etCvOpt = erb.mustCv(constEval(xx, catchObj.exceptionType()));
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
    var bodyResult = constEval(bodyXx, e.body());
    if (bodyResult.valueOrNull() == null) {
      bodyResult.drainInto(erb.errors());
      return EResult.errs(erb.errors());
    }
    bodyResult.drainInto(erb.errors());
    var bodyEv = bodyResult.requireValue("constEvalTry body");
    exits.addAll(bodyEv.exits());
    var bodyRanScope = xx.scope.fork();
    var bodyCv = bodyEv.cv();
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
      var bnCvOpt = erb.mustCv(constEval(xx, catchObj.bindingName()));
      if (bnCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bnCv = bnCvOpt.get();
      var bnv = (Opt.Some<Value>) bnCv.value();
      var bnCkOpt =
          erb.mustRes(Eval.extractConstKey(xx, catchObj.bindingName().loc(), bnv.value()));
      if (bnCkOpt.isEmpty()) return EResult.errs(erb.errors());
      var bindingId = new BindingId.Local(new JavaBytecodeBindingKey());
      xx.scope.replace(initialScope.fork());
      var catchInitialDepth = xx.scope.depth();
      var ibMaybe = GenAspectInitialBind.getMaybe(xx, bodyExcType);
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        xx.scope.pushNamed(
            bnCkOpt.get(), ibSome.value().aspectInitialBindInitialBind(bindingId));
      }
      EResult<EvalRes> catchResult;
      try {
        catchResult = constEval(xx, catchObj.body());
      } finally {
        xx.scope.truncate(catchInitialDepth);
      }
      var catchRanScope = xx.scope.fork();
      var mergeOthers = new TList<Scope>();
      mergeOthers.add(catchRanScope);
      var mergeRes = bodyRanScope.merge(xx, loc, mergeOthers);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.replace(bodyRanScope);
      if (catchResult.valueOrNull() == null) {
        catchResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      catchResult.drainInto(erb.errors());
      var catchEv = catchResult.requireValue("constEvalTry catch body");
      exits.addAll(catchEv.exits());
      var catchCv = catchEv.cv();
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
      var finCvOpt = erb.mustCvMaybeUnreachable(constEval(xx, finSome.value()));
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
    var innerCv = new CodeValue(Code.of(loc, tryOpt.get()),
        resultValue instanceof Opt.Some<Value> rvs ? EvalOutcome.ofValue(rvs.value()) : EvalOutcome.NO_VALUE);
    var innerRes = new EvalRes(innerCv, exits);
    // Exits escaping the body or catch pass through the finally: each gets a
    // trampoline that drops the try's inner scope levels (from the exit's own
    // fork) and runs the finally before jumping on.
    return boundaryClose(
        xx,
        loc,
        erb.errors().isEmpty() ? EResult.ok(innerRes) : EResult.mixed(innerRes, erb.errors()),
        false,
        Opt.none(),
        tryBaseDepth,
        finally_);
  }

  private static EResult<EvalRes> constEvalStage(ExprContext xx, QExprId loc, Expr.Stage e) {
    var matchedLowers = new TList<Expr>();
    findMatchedLowersInner(e.expr(), 0, 0, matchedLowers);
    var matchedGrafts = new TList<Expr>();
    findMatchedGraftsInner(e.expr(), 0, matchedGrafts);
    if (matchedLowers.isEmpty() && matchedGrafts.isEmpty()) {

      return EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(e.expr()))));
    }
    var erb = new Erb(loc);
    var lowerValues = new TList<CodeValue>(matchedLowers.size());
    for (var inner : matchedLowers) {
      var cvOpt = erb.mustCv(constEval(xx, inner));
      if (cvOpt.isEmpty()) return EResult.errs(erb.errors());
      lowerValues.add(cvOpt.get());
    }
    var graftValues = new TList<CodeValue>(matchedGrafts.size());
    for (var inner : matchedGrafts) {
      var cvOpt = erb.mustCv(constEval(xx, inner));
      if (cvOpt.isEmpty()) return EResult.errs(erb.errors());
      graftValues.add(cvOpt.get());
    }
    var staged = erb.mustEr(xx.target.targetStage(xx, loc, e.expr(), lowerValues, graftValues));
    if (staged.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(EvalRes.of(staged.get()));
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
      case Expr.OperatorBinary o -> {
        findMatchedLowersInner(o.base(), sd, ld, out);
        findMatchedLowersInner(o.reference(), sd, ld, out);
      }
      case Expr.OperatorUnary o -> findMatchedLowersInner(o.base(), sd, ld, out);
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
      case Expr.OperatorBinary o -> {
        findMatchedGraftsInner(o.base(), sd, out);
        findMatchedGraftsInner(o.reference(), sd, out);
      }
      case Expr.OperatorUnary o -> findMatchedGraftsInner(o.base(), sd, out);
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
