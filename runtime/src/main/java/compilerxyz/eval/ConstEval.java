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
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.gen.Target;
import compilerxyz.gen.IfBranch;
import compilerxyz.gen.TargetCase;
import compilerxyz.gen.TryCatchClause;
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
import compilerxyz.eval.ScopeEntrySetter;
import compilerxyz.eval.Type;
import compilerxyz.eval.binding.BindingIteratorStack;
import compilerxyz.eval.value.null_.TypeNullConst;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.BindingSetter;

public final class ConstEval {
  private ConstEval() {}

  public static EResult<EvalRes> constEval(ExprContext xx, Expr expr) {
    var loc = expr.loc();
    return switch (expr.variant()) {
      case Expr.Literal lit ->
          EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(lit.value().consume())), xx.scope));
      case Expr.ScopeUse p -> {
        var erb = new Erb(loc);
        var keyOpt = erb.mustCv(xx, constEvalOperand(xx, p.key()));
        if (keyOpt.optIsNone()) yield EResult.errs(erb.errors());
        var key = keyOpt.optUnwrap();
        var keyVal = (Opt.Some<Value>) key.value();
        var keyConstRes = Eval.extractConstKey(xx, loc, keyVal.value());
        var keyConstOpt = erb.mustRes(keyConstRes);
        if (keyConstOpt.optIsNone()) yield EResult.errs(erb.errors());
        var ck = keyConstOpt.optUnwrap();
        var found = xx.scope.lookupEntry(ck);
        if (found == null) {
          erb.errPush("No bound value with id [" + ck + "]");
          yield EResult.errs(erb.errors());
        }
        var setter = new ScopeEntrySetter(xx.scope, found);
        yield EResult.ok(
            EvalRes.of(
                found.binding.bindingUse(
                    xx, loc, Opt.<BindingSetter>some(setter), key.code()),
                xx.scope));
      }
      case Expr.Call p -> {
        var erb = new Erb(loc);
        var fopt = erb.mustCv(xx, constEvalOperand(xx, p.func()));
        var aopt = erb.mustCv(xx, constEvalOperand(xx, p.arg()));
        if (fopt.optIsNone() || aopt.optIsNone()) yield EResult.errs(erb.errors());
        var func = fopt.optUnwrap();
        var arg = aopt.optUnwrap();
        var fv = (Opt.Some<Value>) func.value();
        var funcType = fv.value().valueType();
        var aspectFnOpt = erb.mustEr(GenAspectFunction.get(xx, loc, funcType));
        if (aspectFnOpt.optIsNone()) yield EResult.errs(erb.errors());
        var aspectFn = aspectFnOpt.optUnwrap();
        var calleeExc = aspectFn.aspectFunctionExceptionType(xx, loc, func);
        var callOpt = erb.mustErMaybeUnreachable(aspectFn.aspectFunctionCall(xx, loc, func, arg));
        if (callOpt.optIsNone()) yield EResult.errs(erb.errors());
        var wrapped = erb.mustErMaybeUnreachable(Eval.maybeWrapCallException(xx, loc, calleeExc, callOpt.optUnwrap()));
        if (wrapped.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(wrapped.optUnwrap(), xx.scope));
      }
      case Expr.Set e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
        var vopt = erb.mustCv(xx, constEvalOperand(xx, e.value()));
        if (bopt.optIsNone() || vopt.optIsNone()) yield EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var value = vopt.optUnwrap();
        var bv = (Opt.Some<Value>) base.value();
        var baseType = bv.value().valueType();
        var setOpt = erb.mustEr(GenAspectSet.get(xx, loc, baseType));
        if (setOpt.optIsNone()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(setOpt.optUnwrap().aspectSetSet(xx, loc, base, value));
        if (out.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.optUnwrap(), xx.scope));
      }
      case Expr.Field e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
        var fopt = erb.mustCv(xx, constEvalOperand(xx, e.key()));
        if (bopt.optIsNone() || fopt.optIsNone()) yield EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var field = fopt.optUnwrap();
        var bv = (Opt.Some<Value>) base.value();
        var baseType = bv.value().valueType();
        var aspectFieldOpt = erb.mustEr(GenAspectField.get(xx, loc, baseType));
        if (aspectFieldOpt.optIsNone()) yield EResult.errs(erb.errors());
        var aspectFieldField = aspectFieldOpt.optUnwrap();
        var primaryOpt = erb.mustEr(aspectFieldField.aspectFieldField(xx, loc, base, field));
        if (primaryOpt.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(primaryOpt.optUnwrap(), xx.scope));
      }
      case Expr.Bind ignored ->
          EResult.err(loc, "Bind expression must be directly inside a block");
      case Expr.Seq e -> constEvalSeq(xx, loc, e);
      case Expr.Stage e -> constEvalStage(xx, loc, e);
      case Expr.Lower lo -> constEval(xx, lo.expr());
      case Expr.Graft.Evaluated ev -> EResult.ok(EvalRes.of(ev.value(), xx.scope));
      case Expr.Graft.Pending ignored ->
          EResult.err(loc, "Graft outside of stage processing — its inner was never evaluated");
      case Expr.Tuple t -> {
        var erb = new Erb(loc);
        var elements = new TList<TupleElement>(t.elements().size());
        for (var elExpr : t.elements()) {
          var elRes = elementResult(xx, constEvalOperand(xx, elExpr));
          if (!elRes.hasErrors() && elRes.valueOrNull().optIsSome()
              && elRes.valueOrNull().optUnwrap().value().optIsNone()) {
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
        yield EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(new Tuple(Opt.none(), elements))), xx.scope));
      }
      case Expr.Record r -> constEvalRecord(xx, loc, r);
      case Expr.OperatorBinary e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
        var ropt = erb.mustCv(xx, constEvalOperand(xx, e.reference()));
        if (bopt.optIsNone() || ropt.optIsNone()) yield EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var bv = (Opt.Some<Value>) base.value();
        var out =
            erb.mustEr(constEvalBinaryOp(xx, loc, e.op(), bv.value().valueType(), base, ropt.optUnwrap()));
        if (out.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.optUnwrap(), xx.scope));
      }
      case Expr.OperatorUnary e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
        if (bopt.optIsNone()) yield EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var bv = (Opt.Some<Value>) base.value();
        var out = erb.mustEr(constEvalUnaryOp(xx, loc, e.op(), bv.value().valueType(), base));
        if (out.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(out.optUnwrap(), xx.scope));
      }
      case Expr.Foreach e -> {
        int baseLevelCount = xx.scope.levelCount();
        var level = xx.scope.pushLabeled(Opt.<String>none());
        yield labeledConsume(xx, loc, level, baseLevelCount, constEvalForeach(xx, loc, e));
      }
      case Expr.Match e -> {
        int baseLevelCount = xx.scope.levelCount();
        var level = xx.scope.pushLabeled(Opt.<String>none());
        yield labeledConsume(xx, loc, level, baseLevelCount, constEvalMatch(xx, loc, e));
      }
      case Expr.If e -> {
        int baseLevelCount = xx.scope.levelCount();
        var level = xx.scope.pushLabeled(Opt.<String>none());
        yield labeledConsume(xx, loc, level, baseLevelCount, constEvalIf(xx, loc, e));
      }
      case Expr.As e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
        var topt = erb.mustCv(xx, constEvalOperand(xx, e.target()));
        if (bopt.optIsNone() || topt.optIsNone()) yield EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var target = topt.optUnwrap();
        var tv = (Opt.Some<Value>) target.value();
        var targetMeta = tv.value().valueType();
        var aspectTypeOpt = erb.mustEr(GenAspectType.get(xx, loc, targetMeta));
        if (aspectTypeOpt.optIsNone()) yield EResult.errs(erb.errors());
        var targetType = aspectTypeOpt.optUnwrap().aspectTypeType(tv.value());
        var converted = erb.mustEr(Eval.typecheckConvert(xx, loc, targetType, base));
        if (converted.optIsNone()) yield EResult.errs(erb.errors());
        yield EResult.ok(EvalRes.of(converted.optUnwrap(), xx.scope));
      }
      case Expr.Try e -> constEvalTry(xx, loc, e);
      case Expr.Labeled e -> {
        int baseLevelCount = xx.scope.levelCount();
        var level = xx.scope.pushLabeled(Opt.some(e.label()));
        yield labeledConsume(xx, loc, level, baseLevelCount, constEval(xx, e.inner()));
      }
      case Expr.Return e -> constEvalReturn(xx, loc, e);
    };
  }

  /**
   * Element/operand-position result: value shape preserved, and the
   * default-exit scope tree is installed into the context.
   */
  private static EResult<CodeValue> elementResult(ExprContext xx, EResult<EvalRes> r) {
    var vOpt = r.valueOrNull();
    if (vOpt.optIsNone()) return r.reerr();
    var v = vOpt.optUnwrap();
    xx.scope.adopt(v.scope());
    var errs = r.errorsOrNull();
    if (errs.optIsNone() || errs.optUnwrap().isEmpty()) return EResult.ok(v.cv());
    var copied = new TList<ExprErr>();
    copied.addAll(errs.optUnwrap());
    return EResult.mixed(v.cv(), copied);
  }

  /**
   * Operand-position evaluation. A control exit (return/throw) out of an
   * operand is rejected by the operand consumers' fall-through checks (an
   * operand's value is needed on the stack, so it cannot abandon the
   * expression); nothing structural is needed here. The level-count is
   * restored so a construct that aborted mid-eval on an error path does not
   * leave its levels for a sibling operand.
   */
  private static EResult<EvalRes> constEvalOperand(ExprContext xx, Expr expr) {
    int before = xx.scope.levelCount();
    var res = constEval(xx, expr);
    xx.scope.popToLevelCount(before);
    return res;
  }

  /** Pop the topmost level, which must share {@code level}'s lineage (fork copies allowed). */
  private static void popByBox(Scope scope, ScopeLevel level) {
    var top = scope.levels.remove(scope.levels.size() - 1);
    if (top.scopeLevelEmission() != level.scopeLevelEmission()) {
      throw new AssertionError("scope level pop does not match the owning level");
    }
  }

  /**
   * Emit a level's accumulated chain blocks, jumped over by the normal path.
   * Blocks are cleared so shared boxes never emit twice. Null on error.
   */
  private static Code blocksSplice(ExprContext xx, QExprId loc, TList<Code> blocks, Erb erb) {
    if (blocks.isEmpty()) return Code.none(loc);
    var skip = new JumpKey();
    var jumpOpt = erb.mustEr(xx.target.targetJump(loc, skip));
    var landOpt = erb.mustEr(xx.target.targetLand(loc, skip));
    if (jumpOpt.optIsNone() || landOpt.optIsNone()) return null;
    var code = jumpOpt.optUnwrap();
    for (var b : blocks) code = Code.concat(loc, code, b);
    blocks.clear();
    return Code.concat(loc, code, landOpt.optUnwrap());
  }

  /**
   * Normal-path close of a binding entry: drop from the current (final)
   * state, then the level's chain blocks. Null on error.
   */
  private static Code entryClose(ExprContext xx, QExprId loc, ScopeLevel.Entry entry, Erb erb) {
    var dropRes = entry.binding.bindingDrop(xx, loc);
    if (dropRes.hasErrors()) {
      dropRes.drainInto(erb.errors());
      return null;
    }
    var code = dropRes.requireValue("entryClose drop");
    var splice = blocksSplice(xx, loc, entry.scopeLevelEmission().blocks, erb);
    if (splice == null) return null;
    return Code.concat(loc, code, splice);
  }

  /**
   * Close a return-consuming construct over its level: converge arrivals
   * with the fall-through path at the level's end, merge their scope forks,
   * and splice the level's pending blocks.
   */
  private static EResult<EvalRes> labeledConsume(
      ExprContext xx,
      QExprId loc,
      ScopeLevel.Labeled level,
      int baseLevelCount,
      EResult<EvalRes> innerResult) {
    if (innerResult.valueOrNull().optIsNone()) {
      xx.scope.popToLevelCount(baseLevelCount);
      return innerResult;
    }
    var erb = new Erb(loc);
    innerResult.drainInto(erb.errors());
    var inner = innerResult.requireValue("labeledConsume inner");
    xx.scope.adopt(inner.scope());
    var topLevelRaw = xx.scope.levels.get(xx.scope.levels.size() - 1);
    ScopeLevel.Labeled top;
    switch (topLevelRaw) {
      case ScopeLevel.Labeled lb -> top = lb;
      case ScopeLevel.Entry en -> throw new AssertionError("labeledConsume: consumer level is not on top");
      case ScopeLevel.Temp t -> throw new AssertionError("labeledConsume: consumer level is not on top");
      case ScopeLevel.Catch c -> throw new AssertionError("labeledConsume: consumer level is not on top");
      case ScopeLevel.Finally f -> throw new AssertionError("labeledConsume: consumer level is not on top");
    }
    if (top.scopeLevelEmission() != level.scopeLevelEmission()) {
      throw new AssertionError("labeledConsume: consumer level is not on top");
    }
    xx.scope.levels.remove(xx.scope.levels.size() - 1);
    var innerCv = inner.cv();
    var arrivals = level.arrivals;
    var blocks = level.scopeLevelEmission().blocks;
    boolean fallsThrough = switch (innerCv.outcome()) {
      case EvalOutcome.ControlEnded ce -> false;
      case EvalOutcome.NoValue nv -> true;
      case EvalOutcome.HasValue hv -> true;
    };
    if (!arrivals.isEmpty()) {
      var arrivalScopes = new TList<Scope>(arrivals.size());
      for (var a : arrivals) {
        a.scope().popToLevelCount(baseLevelCount);
        arrivalScopes.add(a.scope());
      }
      if (fallsThrough) {
        var mergeRes = xx.scope.merge(xx, loc, arrivalScopes);
        if (mergeRes.hasErrors()) {
          mergeRes.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
      } else {
        var first = arrivalScopes.get(0);
        var rest = new TList<Scope>(arrivalScopes.size() - 1);
        for (int i = 1; i < arrivalScopes.size(); i++) rest.add(arrivalScopes.get(i));
        var mergeRes = first.merge(xx, loc, rest);
        if (mergeRes.hasErrors()) {
          mergeRes.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        xx.scope.adopt(first);
      }
    }
    var code = innerCv.code();
    if (!arrivals.isEmpty() || !blocks.isEmpty()) {
      if (fallsThrough && !blocks.isEmpty()) {
        var jumpOpt = erb.mustEr(xx.target.targetJump(loc, level.end));
        if (jumpOpt.optIsNone()) return EResult.errs(erb.errors());
        code = Code.concat(loc, code, jumpOpt.optUnwrap());
      }
      for (var b : blocks) code = Code.concat(loc, code, b);
      blocks.clear();
      var landOpt = erb.mustEr(xx.target.targetLand(loc, level.end));
      if (landOpt.optIsNone()) return EResult.errs(erb.errors());
      code = Code.concat(loc, code, landOpt.optUnwrap());
    }
    Opt<Value> resultValue =
        arrivals.isEmpty() ? innerCv.value() : Opt.some(arrivals.get(0).value());
    EvalOutcome outcome;
    if (resultValue.optIsSome()) {
      outcome = EvalOutcome.ofValue(resultValue.optUnwrap());
    } else {
      outcome = switch (innerCv.outcome()) {
        case EvalOutcome.ControlEnded ce -> EvalOutcome.CONTROL_ENDED;
        case EvalOutcome.NoValue nv -> EvalOutcome.NO_VALUE;
        case EvalOutcome.HasValue hv -> EvalOutcome.NO_VALUE;
      };
    }
    var cv = new CodeValue(code, outcome);
    var res = EvalRes.of(cv, xx.scope);
    if (erb.errors().isEmpty()) return EResult.ok(res);
    return EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalReturn(ExprContext xx, QExprId loc, Expr.Return e) {
    var erb = new Erb(loc);
    CodeValue valCv;
    if (e.value().optIsSome()) {
      var vres = constEval(xx, e.value().optUnwrap());
      if (vres.valueOrNull().optIsNone()) return vres;
      vres.drainInto(erb.errors());
      var vev = vres.requireValue("constEvalReturn value");
      xx.scope.adopt(vev.scope());
      valCv = vev.cv();
    } else {
      valCv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(emptyTupleValue()));
    }
    valCv = xx.target.targetReturn(xx, loc, valCv);
    EvalOutcome.HasValue someVal;
    switch (valCv.outcome()) {
      case EvalOutcome.HasValue hv -> someVal = hv;
      case EvalOutcome.ControlEnded ce -> {
        var cv = new CodeValue(valCv.code(), EvalOutcome.CONTROL_ENDED);
        var res = EvalRes.of(cv, xx.scope);
        return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
      }
      case EvalOutcome.NoValue nv -> {
        var cv = new CodeValue(valCv.code(), EvalOutcome.CONTROL_ENDED);
        var res = EvalRes.of(cv, xx.scope);
        return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
      }
    }
    // Target: innermost Labeled level, name-matched for labeled returns. A
    // barrier on the way rejects the exit — control may not leave an
    // expression-operand position.
    ScopeLevel.Labeled target = null;
    for (int i = xx.scope.levels.size() - 1; i >= 0 && target == null; i--) {
      var level = xx.scope.levels.get(i);
      switch (level) {
        case ScopeLevel.Labeled lb -> {
          if (e.label().optIsSome()) {
            if (lb.label.optIsSome() && lb.label.optUnwrap().equals(e.label().optUnwrap())) {
              target = lb;
            }
          } else {
            target = lb;
          }
        }
        case ScopeLevel.Entry entry -> {}
        case ScopeLevel.Temp temp -> {}
        case ScopeLevel.Catch catch_ -> {}
        case ScopeLevel.Finally finally_ -> {}
      }
    }
    if (target == null) {
      erb.errPush(
          e.label().optIsSome()
              ? "return: no enclosing scope with label '" + e.label().optUnwrap() + "'"
              : "return: no enclosing control scope");
      return EResult.errs(erb.errors());
    }
    boolean stackCarried = someVal.value() instanceof ValueDynamic;
    Opt<Type> carryType =
        someVal.value() instanceof ValueDynamic vd ? Opt.some(vd.type_()) : Opt.<Type>none();
    var destRes = ScopeChains.exitDest(xx, loc, target, carryType);
    if (destRes.hasErrors()) {
      destRes.drainInto(erb.errors());
      return EResult.errs(erb.errors());
    }
    var dest = destRes.requireValue("constEvalReturn dest");
    target.arrivals.add(
        new ScopeLevel.Arrival(loc, someVal.value(), stackCarried, xx.scope.fork()));
    var jumpOpt = erb.mustEr(xx.target.targetJump(loc, dest));
    if (jumpOpt.optIsNone()) return EResult.errs(erb.errors());
    var cv =
        new CodeValue(
            Code.concat(loc, valCv.code(), jumpOpt.optUnwrap()), EvalOutcome.CONTROL_ENDED);
    var res = EvalRes.of(cv, xx.scope);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static Value emptyTupleValue() {
    return new Tuple(Opt.none(), new TList<>());
  }

  private static EResult<EvalRes> constEvalSeq(ExprContext xx, QExprId loc, Expr.Seq e) {
    var erb = new Erb(loc);
    int baseLevelCount = xx.scope.levelCount();
    var consumer = xx.scope.pushLabeled(Opt.<String>none());
    var codes = new TList<Code>();
    var entries = new TList<ScopeLevel.Entry>();
    Opt<Value> lastVal = Opt.some(emptyTupleValue());
    CodeValue pendingDiscard = null;
    boolean controlEnded = false;
    for (var sub : e.exprs()) {
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
        var dropOpt = erb.mustErMaybeUnreachable(Eval.dropValue(xx, sub.loc(), pendingDiscard));
        if (dropOpt.optIsNone()) return EResult.errs(erb.errors());
        codes.add(dropOpt.optUnwrap());
        pendingDiscard = null;
      }
      Opt<Expr.Bind> bindOpt = switch (sub.variant()) {
        case Expr.Bind b -> Opt.some(b);
        case Expr.As a -> Opt.none();
        case Expr.Call c -> Opt.none();
        case Expr.Field f -> Opt.none();
        case Expr.Foreach f -> Opt.none();
        case Expr.Graft.Pending p -> Opt.none();
        case Expr.Graft.Evaluated g -> Opt.none();
        case Expr.If i -> Opt.none();
        case Expr.Labeled l -> Opt.none();
        case Expr.Literal l -> Opt.none();
        case Expr.Lower l -> Opt.none();
        case Expr.Match m -> Opt.none();
        case Expr.OperatorBinary o -> Opt.none();
        case Expr.OperatorUnary o -> Opt.none();
        case Expr.Record r -> Opt.none();
        case Expr.Return r -> Opt.none();
        case Expr.ScopeUse s -> Opt.none();
        case Expr.Seq s -> Opt.none();
        case Expr.Set s -> Opt.none();
        case Expr.Stage s -> Opt.none();
        case Expr.Try t -> Opt.none();
        case Expr.Tuple t -> Opt.none();
      };
      if (bindOpt.optIsSome()) {
        var bind = bindOpt.optUnwrap();
        var bopt = erb.mustCv(xx, constEvalOperand(xx, bind.base()));
        var vres = constEval(xx, bind.value());
        if (vres.valueOrNull().optIsNone()) {
          vres.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        vres.drainInto(erb.errors());
        var vev = vres.requireValue("constEvalSeq bind value");
        xx.scope.adopt(vev.scope());
        if (bopt.optIsNone()) return EResult.errs(erb.errors());
        var base = bopt.optUnwrap();
        var value = vev.cv();
        if (base.value().optIsNone()) {
          erb.errPush("Bind: base has no compile-time value");
          return EResult.errs(erb.errors());
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, sub.loc(), base.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        codes.add(base.code());
        if (value.value().optIsNone()) {
          erb.errors()
              .add(
                  new ExprErr(
                      sub.loc(),
                      TList.of(new ExprSubErr.Opaque(
                          "Cannot bind result of expression that ends control flow"))));
          return EResult.errs(erb.errors());
        }
        var valType = value.value().optUnwrap().valueType();
        var bindAspectOpt = erb.mustEr(GenAspectBind.get(xx, sub.loc(), valType));
        if (bindAspectOpt.optIsNone()) return EResult.errs(erb.errors());
        var brOpt = erb.mustEr(bindAspectOpt.optUnwrap().aspectBindBind(xx, sub.loc(), value));
        if (brOpt.optIsNone()) return EResult.errs(erb.errors());
        var br = brOpt.optUnwrap();
        codes.add(br.code());
        entries.add(xx.scope.pushNamed(ckOpt.optUnwrap(), br.binding()));
        lastVal = Opt.some(emptyTupleValue());
      } else {
        var rres = constEval(xx, sub);
        if (rres.valueOrNull().optIsNone()) {
          rres.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        rres.drainInto(erb.errors());
        var rev = rres.requireValue("constEvalSeq entry");
        xx.scope.adopt(rev.scope());
        var r = rev.cv();
        codes.add(r.code());
        switch (r.outcome()) {
          case EvalOutcome.ControlEnded ce -> {
            controlEnded = true;
            lastVal = Opt.none();
            continue;
          }
          case EvalOutcome.NoValue nv -> {}
          case EvalOutcome.HasValue hv -> {}
        }
        pendingDiscard = r;
      }
    }
    if (pendingDiscard != null) {
      var dropOpt = erb.mustErMaybeUnreachable(Eval.dropValue(xx, loc, pendingDiscard));
      if (dropOpt.optIsNone()) return EResult.errs(erb.errors());
      codes.add(dropOpt.optUnwrap());
      pendingDiscard = null;
    }
    // Unwind binding entries innermost-first: normal drop from the final
    // state, then each level's chain blocks. Branch merges adopt fork copies
    // into the scope, so the live level on top — not the object remembered at
    // bind — carries the current binding state.
    for (int i = entries.size() - 1; i >= 0; i--) {
      var top = xx.scope.levels.get(xx.scope.levels.size() - 1);
      ScopeLevel.Entry current;
      switch (top) {
        case ScopeLevel.Entry en -> current = en;
        case ScopeLevel.Labeled lb -> throw new AssertionError("seq unwind: top level is not the expected entry");
        case ScopeLevel.Temp t -> throw new AssertionError("seq unwind: top level is not the expected entry");
        case ScopeLevel.Catch c -> throw new AssertionError("seq unwind: top level is not the expected entry");
        case ScopeLevel.Finally f -> throw new AssertionError("seq unwind: top level is not the expected entry");
      }
      if (current.scopeLevelEmission() != entries.get(i).scopeLevelEmission()) {
        throw new AssertionError("seq unwind: top level is not the expected entry");
      }
      var closeCode = entryClose(xx, loc, current, erb);
      if (closeCode == null) return EResult.errs(erb.errors());
      codes.add(closeCode);
      xx.scope.levels.remove(xx.scope.levels.size() - 1);
    }
    var seqOpt = erb.mustErMaybeUnreachable(xx.target.targetSeq(loc, codes));
    if (seqOpt.optIsNone()) return EResult.errs(erb.errors());
    var innerCv = new CodeValue(seqOpt.optUnwrap(),
        lastVal.optIsSome()
            ? EvalOutcome.ofValue(lastVal.optUnwrap())
            : (controlEnded ? EvalOutcome.CONTROL_ENDED : EvalOutcome.NO_VALUE));
    var innerRes = EvalRes.of(innerCv, xx.scope);
    return labeledConsume(
        xx,
        loc,
        consumer,
        baseLevelCount,
        erb.errors().isEmpty() ? EResult.ok(innerRes) : EResult.mixed(innerRes, erb.errors()));
  }

  private static EResult<CodeValue> constEvalBinaryOp(
      ExprContext xx, QExprId loc, BinaryOperator op, Type baseType, CodeValue base, CodeValue reference) {
    var erb = new Erb(loc);
    return switch (op) {
      case ADD -> {
        var aopt = erb.mustEr(GenAspectOperatorAdd.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorAddAdd(xx, loc, base, reference);
      }
      case SUB -> {
        var aopt = erb.mustEr(GenAspectOperatorSub.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorSubSub(xx, loc, base, reference);
      }
      case MUL -> {
        var aopt = erb.mustEr(GenAspectOperatorMul.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorMulMul(xx, loc, base, reference);
      }
      case DIV -> {
        var aopt = erb.mustEr(GenAspectOperatorDiv.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorDivDiv(xx, loc, base, reference);
      }
      case MOD -> {
        var aopt = erb.mustEr(GenAspectOperatorMod.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorModMod(xx, loc, base, reference);
      }
      case AND -> {
        var aopt = erb.mustEr(GenAspectOperatorAnd.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorAndAnd(xx, loc, base, reference);
      }
      case OR -> {
        var aopt = erb.mustEr(GenAspectOperatorOr.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorOrOr(xx, loc, base, reference);
      }
      case XOR -> {
        var aopt = erb.mustEr(GenAspectOperatorXor.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorXorXor(xx, loc, base, reference);
      }
      case SHL -> {
        var aopt = erb.mustEr(GenAspectOperatorShl.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorShlShl(xx, loc, base, reference);
      }
      case SHR -> {
        var aopt = erb.mustEr(GenAspectOperatorShr.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorShrShr(xx, loc, base, reference);
      }
      case USHR -> {
        var aopt = erb.mustEr(GenAspectOperatorUshr.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorUshrUshr(xx, loc, base, reference);
      }
      case EQ -> {
        var aopt = erb.mustEr(GenAspectOperatorEq.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorEqEq(xx, loc, base, reference);
      }
      case NE -> {
        var aopt = erb.mustEr(GenAspectOperatorNe.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorNeNe(xx, loc, base, reference);
      }
      case LT -> {
        var aopt = erb.mustEr(GenAspectOperatorLt.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorLtLt(xx, loc, base, reference);
      }
      case LE -> {
        var aopt = erb.mustEr(GenAspectOperatorLe.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorLeLe(xx, loc, base, reference);
      }
      case GT -> {
        var aopt = erb.mustEr(GenAspectOperatorGt.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorGtGt(xx, loc, base, reference);
      }
      case GE -> {
        var aopt = erb.mustEr(GenAspectOperatorGe.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorGeGe(xx, loc, base, reference);
      }
    };
  }

  private static EResult<CodeValue> constEvalUnaryOp(
      ExprContext xx, QExprId loc, UnaryOperator op, Type baseType, CodeValue base) {
    var erb = new Erb(loc);
    return switch (op) {
      case NEG -> {
        var aopt = erb.mustEr(GenAspectOperatorNeg.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorNegNeg(xx, loc, base);
      }
      case BITNOT -> {
        var aopt = erb.mustEr(GenAspectOperatorBitNot.get(xx, loc, baseType));
        if (aopt.optIsNone()) yield EResult.errs(erb.errors());
        yield aopt.optUnwrap().aspectOperatorBitNotBitNot(xx, loc, base);
      }
    };
  }


  private static EResult<EvalRes> constEvalRecord(ExprContext xx, QExprId loc, Expr.Record r) {
    var entries = new TOrderedMap<ConstKey, EResult<CodeValue>>();
    var keyCodes = new TList<Code>();
    var errors = new TList<ExprErr>();
    for (var entry : r.entries()) {
      var kRes = elementResult(xx, constEvalOperand(xx, entry.key()));
      if (kRes.hasErrors()) {
        kRes.drainInto(errors);
        continue;
      }
      var kCv = kRes.requireValue("ConstEval::record key");
      if (!kCv.code().codeIsNone()) {
        keyCodes.add(kCv.code());
      }
      if (kCv.value().optIsNone()) {
        errors.add(
            new ExprErr(
                loc,
                TList.of(new ExprSubErr.Opaque("Record: key has no compile-time value"))));
        continue;
      }
      var keyRes = Eval.extractConstKey(xx, loc, kCv.value().optUnwrap());
      switch (keyRes) {
        case Result.Ok<ConstKey, String> ok -> {
          var valRes = elementResult(xx, constEvalOperand(xx, entry.value()));
          if (!valRes.hasErrors() && valRes.valueOrNull().optIsSome()
              && valRes.valueOrNull().optUnwrap().value().optIsNone()) {
            errors.add(
                new ExprErr(
                    entry.value().loc(),
                    TList.of(new ExprSubErr.Opaque(
                        "Record field '" + ok.value().constKeyToCanonical()
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
    Code code = Code.none(loc);
    for (var c : keyCodes) code = Code.concat(loc, code, c);
    var fields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : entries.entrySet()) {
      fields.put(entry.getKey(), new RecordField.EvalRes(entry.getValue()));
    }
    return EResult.ok(EvalRes.of(new CodeValue(code, EvalOutcome.ofValue(new Record(Opt.none(), fields))), xx.scope));
  }

  private static EResult<EvalRes> constEvalForeach(
      ExprContext xx, QExprId loc, Expr.Foreach e) {
    var erb = new Erb(loc);
    var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
    var ekopt = erb.mustCv(xx, constEvalOperand(xx, e.elementKey()));
    if (bopt.optIsNone() || ekopt.optIsNone()) return EResult.errs(erb.errors());
    var base = bopt.optUnwrap();
    var ekCv = ekopt.optUnwrap();
    var ekv = (Opt.Some<Value>) ekCv.value();
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.optIsNone()) return EResult.errs(erb.errors());
    var bv = (Opt.Some<Value>) base.value();
    var baseType = bv.value().valueType();
    var iterOpt = erb.mustEr(GenAspectIterable.get(xx, loc, baseType));
    if (iterOpt.optIsNone()) return EResult.errs(erb.errors());
    var iterable = iterOpt.optUnwrap();
    var elementType = iterable.aspectIterableElementType();
    var elemBindRes = GenAspectElementUseDynamicBind.get(xx, loc, elementType);
    if (elemBindRes.hasErrors()) return elemBindRes.reerr();
    var elementScopeId = BindingId.localFresh();
    var elementBinding =
        elemBindRes
            .requireValue("Foreach: AspectElementUseDynamicBind")
            .aspectElementUseDynamicBindElementUseDynamicBind(elementScopeId);
    // The body runs on its own fork of the scope tree — the zero-iteration
    // case (body never runs) keeps the incoming tree, and the two merge at
    // the loop's default exit.
    var bodyScope = xx.scope.fork();
    // The loop skeleton keeps the iterator on the operand stack across body
    // iterations; a return escaping the body would leave the iterator
    // dangling. A stack-cleanup entry (BindingIteratorStack drops with a POP)
    // has the exit's trampoline restore the operand stack before jumping on;
    // exceptional unwinds discard the stack anyway. The element key is pushed
    // as a named entry immediately after so it shadows any outer name; the
    // iterator sits below.
    var iterEntry = bodyScope.pushStackCleanup(new BindingIteratorStack());
    var elemEntry = bodyScope.pushNamed(ekOpt.optUnwrap(), elementBinding);
    var bodyXx = new ExprContext(
        bodyScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
    var bodyResult = constEval(bodyXx, e.body());
    if (bodyResult.valueOrNull().optIsNone()) {
      bodyResult.drainInto(erb.errors());
      return EResult.errs(erb.errors());
    }
    bodyResult.drainInto(erb.errors());
    var bodyEv = bodyResult.requireValue("constEvalForeach body");
    // Merge the body-ran scope with the body-never-ran (incoming) scope.
    var bodyRanScope = bodyEv.scope();
    var elemSplice = blocksSplice(xx, loc, elemEntry.scopeLevelEmission().blocks, erb);
    if (elemSplice == null) return EResult.errs(erb.errors());
    var iterSplice = blocksSplice(xx, loc, iterEntry.scopeLevelEmission().blocks, erb);
    if (iterSplice == null) return EResult.errs(erb.errors());
    popByBox(bodyRanScope, elemEntry);
    popByBox(bodyRanScope, iterEntry);
    var others = new TList<Scope>();
    others.add(xx.scope);
    var mergeRes = bodyRanScope.merge(xx, loc, others);
    if (mergeRes.hasErrors()) return mergeRes.reerr();
    xx.scope.adopt(bodyRanScope);
    var out = erb.mustErMaybeUnreachable(iterable.aspectIterableForeach(xx, loc, base, elementScopeId, bodyEv.cv()));
    if (out.optIsNone()) return EResult.errs(erb.errors());
    var outCv = out.optUnwrap();
    outCv = new CodeValue(
        Code.concat(loc, outCv.code(), Code.concat(loc, elemSplice, iterSplice)),
        outCv.outcome());
    var res = EvalRes.of(outCv, xx.scope);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalMatch(ExprContext xx, QExprId loc, Expr.Match e) {
    var erb = new Erb(loc);
    var bopt = erb.mustCv(xx, constEvalOperand(xx, e.base()));
    var ekopt = erb.mustCv(xx, constEvalOperand(xx, e.elementKey()));
    if (bopt.optIsNone() || ekopt.optIsNone()) return EResult.errs(erb.errors());
    var base = bopt.optUnwrap();
    var ekCv = ekopt.optUnwrap();
    var ekv = (Opt.Some<Value>) ekCv.value();
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.optIsNone()) return EResult.errs(erb.errors());
    var elementKey = ekOpt.optUnwrap();
    var bv = (Opt.Some<Value>) base.value();
    var baseType = bv.value().valueType();
    var unionOpt = erb.mustEr(GenAspectUnion.get(xx, loc, baseType));
    if (unionOpt.optIsNone()) return EResult.errs(erb.errors());
    var variants = unionOpt.optUnwrap().aspectUnionVariants();
    var caseTags = new TList<ConstKey>();
    for (var c : e.cases()) {
      var condOpt = erb.mustCv(xx, constEvalOperand(xx, c.condition()));
      if (condOpt.optIsNone()) return EResult.errs(erb.errors());
      var cv = (Opt.Some<Value>) condOpt.optUnwrap().value();
      var tagOpt = erb.mustRes(Eval.extractConstKey(xx, loc, cv.value()));
      if (tagOpt.optIsNone()) return EResult.errs(erb.errors());
      var tag = tagOpt.optUnwrap();
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
    if (!exhaustive && e.fallback().optIsNone()) {
      for (var info : variants) {
        if (!caseTags.contains(info.key())) {
          erb.errPush("Non-exhaustive match: missing case for tag '" + info.key() + "'");
          return EResult.errs(erb.errors());
        }
      }
    }
    if (exhaustive && e.fallback().optIsSome()) {
      erb.errPush("Exhaustive match must not have a fallback");
      return EResult.errs(erb.errors());
    }
    // Fork/merge scope across match cases + fallback so any type mutations
    // in one case don't leak into siblings and incompatible mutations
    // across branches surface as a compile error at the merge.
    var initialScope = xx.scope.fork();
    var branchScopes = new TList<Scope>();
    Type resultType = null;
    boolean allControlEnded = true;
    var matchCases = new TList<TargetCase>(e.cases().size());
    for (int i = 0; i < e.cases().size(); i++) {
      var tag = caseTags.get(i);
      int variantIdx = -1;
      for (int j = 0; j < variants.size(); j++) {
        if (variants.get(j).key().equals(tag)) {
          variantIdx = j;
          break;
        }
      }
      if (variantIdx < 0) {
        erb.errPush("Match: tag not found in union variants: " + tag.constKeyToCanonical());
        return EResult.errs(erb.errors());
      }
      var variant = variants.get(variantIdx);
      var payloadBinding = BindingId.localFresh();
      var caseScope = initialScope.fork();
      ScopeLevel.Entry payloadEntry = null;
      var ibMaybe = GenAspectInitialBind.getMaybe(xx, variant.type());
      if (ibMaybe.optIsSome()) {
        payloadEntry = caseScope.pushNamed(
            elementKey, ibMaybe.optUnwrap().aspectInitialBindInitialBind(payloadBinding));
      }
      var caseXx = new ExprContext(
          caseScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
      var bodyResult = constEval(caseXx, e.cases().get(i).body());
      if (bodyResult.valueOrNull().optIsNone()) {
        bodyResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      bodyResult.drainInto(erb.errors());
      var bodyEv = bodyResult.requireValue("constEvalMatch case body");
      var caseRanScope = bodyEv.scope();
      var bodyCv = bodyEv.cv();
      if (payloadEntry != null) {
        // The case body consumes the payload; no normal-path drop — only the
        // level's chain blocks.
        var payloadSplice =
            blocksSplice(caseXx, loc, payloadEntry.scopeLevelEmission().blocks, erb);
        if (payloadSplice == null) return EResult.errs(erb.errors());
        bodyCv = new CodeValue(
            Code.concat(loc, bodyCv.code(), payloadSplice), bodyCv.outcome());
        popByBox(caseRanScope, payloadEntry);
      }
      branchScopes.add(caseRanScope);
      if (bodyCv.value().optIsSome()) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = bodyCv.value().optUnwrap().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(caseXx, loc, resultType, bodyCv));
          if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
          bodyCv = convertedOpt.optUnwrap();
        }
      }
      matchCases.add(
          new TargetCase(variant.jsTag(), variant.type(), variantIdx, payloadBinding, bodyCv));
    }
    Opt<CodeValue> fallbackCv = Opt.none();
    if (e.fallback().optIsSome()) {
      var fallbackXx = new ExprContext(
          initialScope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
      var fallbackResult = constEval(fallbackXx, e.fallback().optUnwrap());
      if (fallbackResult.valueOrNull().optIsNone()) {
        fallbackResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      fallbackResult.drainInto(erb.errors());
      var fallbackEv = fallbackResult.requireValue("constEvalMatch fallback");
      branchScopes.add(fallbackEv.scope());
      var fbCv = fallbackEv.cv();
      if (fbCv.value().optIsSome()) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = fbCv.value().optUnwrap().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(fallbackXx, loc, resultType, fbCv));
          if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
          fbCv = convertedOpt.optUnwrap();
        }
      }
      fallbackCv = Opt.some(fbCv);
    } else if (!allControlEnded) {
      // No fallback: the "no case matched" runtime path (which shouldn't fire
      // for an exhaustive match) is treated as the untouched initial scope.
      branchScopes.add(initialScope.fork());
    }
    // Merge case default-exit scopes.
    if (branchScopes.isEmpty()) {
      xx.scope.adopt(initialScope);
    } else {
      var merged = branchScopes.get(0);
      var rest = new TList<Scope>(branchScopes.size() - 1);
      for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
      var mergeRes = merged.merge(xx, loc, rest);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.adopt(merged);
    }
    var out = erb.mustErMaybeUnreachable(xx.target.targetMatch(xx, loc, base, matchCases, fallbackCv));
    if (out.optIsNone()) return EResult.errs(erb.errors());
    var outCv = out.optUnwrap();
    var res = EvalRes.of(outCv, xx.scope);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalIf(ExprContext xx, QExprId loc, Expr.If e) {
    var erb = new Erb(loc);
    var conditionsEvaluated = new TList<IfEval>();
    boolean allConst = true;
    for (var branch : e.conditions()) {
      var condOpt = erb.mustCv(xx, constEvalOperand(xx, branch.condition()));
      if (condOpt.optIsNone()) return EResult.errs(erb.errors());
      var cond = condOpt.optUnwrap();
      var cv = (Opt.Some<Value>) cond.value();
      var condType = cv.value().valueType();
      var boolAspect = GenAspectBoolConst.getMaybe(xx, condType);
      if (boolAspect.optIsSome()) {
        var val = boolAspect.optUnwrap().aspectBoolConstGet(cv.value());
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
          if (bodyResult.valueOrNull().optIsNone()) {
            bodyResult.drainInto(erb.errors());
            return EResult.errs(erb.errors());
          }
          bodyResult.drainInto(erb.errors());
          var bodyEv = bodyResult.requireValue("constEvalIf const body");
          var body = bodyEv.cv();
          var res = EvalRes.of(
              new CodeValue(Code.concat(loc, carried, body.code()), body.outcome()),
              bodyEv.scope());
          return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
        }
      }
      if (e.else_().optIsSome()) {
        var elseResult = constEval(xx, e.else_().optUnwrap());
        if (elseResult.valueOrNull().optIsNone()) {
          elseResult.drainInto(erb.errors());
          return EResult.errs(erb.errors());
        }
        elseResult.drainInto(erb.errors());
        var elseEv = elseResult.requireValue("constEvalIf const else");
        var elseCv = elseEv.cv();
        var res = EvalRes.of(
            new CodeValue(Code.concat(loc, carried, elseCv.code()), elseCv.outcome()),
            elseEv.scope());
        return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
      }
      return EResult.ok(EvalRes.of(new CodeValue(carried, EvalOutcome.ofValue(new ValueObjectConst(new TypeNullConst(), null))), xx.scope));
    }
    // Fork/merge scope across branches so any type mutations inside one
    // branch don't leak into siblings, and incompatible mutations across
    // branches surface as a compile error at the merge.
    var initialScope = xx.scope.fork();
    var branchScopes = new TList<Scope>();
    Type resultType = null;
    var branches = new TList<IfBranch>(conditionsEvaluated.size());
    for (var ie : conditionsEvaluated) {
      var branchXx = new ExprContext(
          initialScope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
      var bodyResult = constEval(branchXx, ie.body);
      if (bodyResult.valueOrNull().optIsNone()) {
        bodyResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      bodyResult.drainInto(erb.errors());
      var bodyEv = bodyResult.requireValue("constEvalIf branch body");
      branchScopes.add(bodyEv.scope());
      var bodyCv = bodyEv.cv();
      if (bodyCv.value().optIsSome()) {
        if (resultType == null) {
          resultType = bodyCv.value().optUnwrap().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(branchXx, loc, resultType, bodyCv));
          if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
          bodyCv = convertedOpt.optUnwrap();
        }
      }
      branches.add(new IfBranch(ie.cond, bodyCv));
    }
    Opt<CodeValue> elseCv = Opt.none();
    if (e.else_().optIsSome()) {
      var elseXx = new ExprContext(
          initialScope.fork(), xx.target, xx.vx, xx.importState, xx.currentExceptionType);
      var elseResult = constEval(elseXx, e.else_().optUnwrap());
      if (elseResult.valueOrNull().optIsNone()) {
        elseResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      elseResult.drainInto(erb.errors());
      var elseEv = elseResult.requireValue("constEvalIf else body");
      branchScopes.add(elseEv.scope());
      var eCv = elseEv.cv();
      if (eCv.value().optIsSome()) {
        if (resultType == null) {
          resultType = eCv.value().optUnwrap().valueType();
        } else {
          var convertedOpt = erb.mustEr(Eval.typecheckConvert(elseXx, loc, resultType, eCv));
          if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
          eCv = convertedOpt.optUnwrap();
        }
      }
      elseCv = Opt.some(eCv);
    } else {
      // No else: any condition may fall through — the "no branch matched"
      // path reaches the end with an implicit null value, and its scope
      // is the initial (un-mutated) scope.
      branchScopes.add(initialScope.fork());
    }
    // Merge branch default-exit scopes. The first branch's tree becomes the
    // merge receiver; the rest are passed to bindingMerge.
    if (branchScopes.isEmpty()) {
      xx.scope.adopt(initialScope);
    } else {
      var merged = branchScopes.get(0);
      var rest = new TList<Scope>(branchScopes.size() - 1);
      for (int i = 1; i < branchScopes.size(); i++) rest.add(branchScopes.get(i));
      var mergeRes = merged.merge(xx, loc, rest);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      xx.scope.adopt(merged);
    }
    var out = erb.mustErMaybeUnreachable(xx.target.targetIf(xx, loc, branches, elseCv));
    if (out.optIsNone()) return EResult.errs(erb.errors());
    var outCv = out.optUnwrap();
    var res = EvalRes.of(outCv, xx.scope);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private record IfEval(boolean val, CodeValue cond, Expr body) {}

  private static EResult<EvalRes> constEvalTry(ExprContext xx, QExprId loc, Expr.Try e) {
    if (e.catchClause().optIsNone()
        && e.finallyBlock().optIsNone()) {
      return EResult.err(loc, "Try requires at least one of catch/finally");
    }
    var erb = new Erb(loc);
    Opt<Type> bodyExcType;
    if (e.catchClause().optIsSome()) {
      var catchObj = e.catchClause().optUnwrap();
      var etCvOpt = erb.mustCv(xx, constEvalOperand(xx, catchObj.exceptionType()));
      if (etCvOpt.optIsNone()) return EResult.errs(erb.errors());
      var etCv = etCvOpt.optUnwrap();
      var etv = (Opt.Some<Value>) etCv.value();
      var etMeta = etv.value().valueType();
      var atOpt = erb.mustEr(GenAspectType.get(xx, loc, etMeta));
      if (atOpt.optIsNone()) return EResult.errs(erb.errors());
      bodyExcType = Opt.some(atOpt.optUnwrap().aspectTypeType(etv.value()));
    } else {
      bodyExcType = xx.currentExceptionType;
    }
    // Levels: the finally participates in every chain crossing the try; the
    // catch terminates the exceptional chain for the body's extent only.
    ScopeLevel.Finally finallyLevel = null;
    if (e.finallyBlock().optIsSome()) {
      finallyLevel = xx.scope.pushFinally();
    }
    ScopeLevel.Catch catchLevel = null;
    JumpKey handlerKey = null;
    if (e.catchClause().optIsSome()) {
      handlerKey = new JumpKey();
      catchLevel = xx.scope.pushCatch(handlerKey);
    }
    // The body and the catch run in exclusive control-flow branches (either
    // the body completes without throwing or the catch fires), each on its
    // own fork of the scope tree; their default-exit scopes merge into one
    // post-try scope.
    var bodyXx = new ExprContext(
        xx.scope.fork(), xx.target, xx.vx, xx.importState, bodyExcType);
    var bodyResult = constEval(bodyXx, e.body());
    if (bodyResult.valueOrNull().optIsNone()) {
      bodyResult.drainInto(erb.errors());
      return EResult.errs(erb.errors());
    }
    bodyResult.drainInto(erb.errors());
    var bodyEv = bodyResult.requireValue("constEvalTry body");
    var bodyRanScope = bodyEv.scope();
    var bodyCv = bodyEv.cv();
    Type bodyType;
    if (bodyCv.value().optIsSome()) {
      bodyType = bodyCv.value().optUnwrap().valueType();
    } else {
      bodyType = null;
    }
    Opt<Value> resultValue = bodyCv.value();
    // The catch level covers only the body: pop it before the catch clause
    // evaluates, from the live scope and the body's fork alike.
    if (catchLevel != null) {
      popByBox(xx.scope, catchLevel);
      popByBox(bodyRanScope, catchLevel);
    }
    Opt<TryCatchClause> catch_ = Opt.none();
    if (e.catchClause().optIsSome()) {
      var catchObj = e.catchClause().optUnwrap();
      var bnCvOpt = erb.mustCv(xx, constEvalOperand(xx, catchObj.bindingName()));
      if (bnCvOpt.optIsNone()) return EResult.errs(erb.errors());
      var bnCv = bnCvOpt.optUnwrap();
      var bnv = (Opt.Some<Value>) bnCv.value();
      var bnCkOpt =
          erb.mustRes(Eval.extractConstKey(xx, catchObj.bindingName().loc(), bnv.value()));
      if (bnCkOpt.optIsNone()) return EResult.errs(erb.errors());
      var bindingId = BindingId.localFresh();
      var catchScope = xx.scope.fork();
      ScopeLevel.Entry payloadEntry = null;
      var ibMaybe = GenAspectInitialBind.getMaybe(xx, bodyExcType.optUnwrap());
      if (ibMaybe.optIsSome()) {
        payloadEntry = catchScope.pushNamed(
            bnCkOpt.optUnwrap(), ibMaybe.optUnwrap().aspectInitialBindInitialBind(bindingId));
      }
      var catchXx = new ExprContext(
          catchScope, xx.target, xx.vx, xx.importState, xx.currentExceptionType);
      var catchResult = constEval(catchXx, catchObj.body());
      if (catchResult.valueOrNull().optIsNone()) {
        catchResult.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      catchResult.drainInto(erb.errors());
      var catchEv = catchResult.requireValue("constEvalTry catch body");
      var catchRanScope = catchEv.scope();
      var catchCv = catchEv.cv();
      if (payloadEntry != null) {
        var payloadSplice =
            blocksSplice(catchXx, loc, payloadEntry.scopeLevelEmission().blocks, erb);
        if (payloadSplice == null) return EResult.errs(erb.errors());
        catchCv = new CodeValue(
            Code.concat(loc, catchCv.code(), payloadSplice), catchCv.outcome());
        popByBox(catchRanScope, payloadEntry);
      }
      var mergeOthers = new TList<Scope>();
      mergeOthers.add(catchRanScope);
      var mergeRes = bodyRanScope.merge(xx, loc, mergeOthers);
      if (mergeRes.hasErrors()) return mergeRes.reerr();
      if (bodyType != null && catchCv.value().optIsSome()) {
        var convOpt = erb.mustErMaybeUnreachable(Eval.typecheckConvert(xx, loc, bodyType, catchCv));
        if (convOpt.optIsNone()) return EResult.errs(erb.errors());
        catchCv = convOpt.optUnwrap();
      }
      if (resultValue.optIsNone()) {
        resultValue = catchCv.value();
      }
      catch_ = Opt.some(new TryCatchClause(bindingId, bodyExcType.optUnwrap(), catchCv));
    }
    xx.scope.adopt(bodyRanScope);
    Opt<Code> finallyNormal = Opt.none();
    if (finallyLevel != null) {
      // The finally's own code must not chain through itself: pop the level
      // before evaluating the body (source order: after body and catch).
      popByBox(xx.scope, finallyLevel);
      var finSome = (Opt.Some<Expr>) e.finallyBlock();
      // A finally expression must complete normally: a throw or return (any
      // non-fall-through control exit) out of a finally is invalid.
      var finCvOpt = erb.mustCv(xx, constEvalOperand(xx, finSome.value()));
      if (finCvOpt.optIsNone()) return EResult.errs(erb.errors());
      var finCv = finCvOpt.optUnwrap();
      // A finally block has no result: whatever value it produced is dropped, so every exit path
      // leaves the stack exactly as it found it.
      var finallyDropOpt = erb.mustEr(Eval.dropValue(xx, loc, finCv));
      if (finallyDropOpt.optIsNone()) return EResult.errs(erb.errors());
      Code finallyDropCode = finallyDropOpt.optUnwrap();
      Code combinedFinally;
      if (!finallyDropCode.codeIsNone()) {
        var combinedOpt =
            erb.mustEr(xx.target.targetSeq(loc, TList.of(finCv.code(), finallyDropCode)));
        if (combinedOpt.optIsNone()) return EResult.errs(erb.errors());
        combinedFinally = combinedOpt.optUnwrap();
      } else {
        combinedFinally = finCv.code();
      }
      finallyLevel.body = combinedFinally;
      var genRes = ScopeChains.finallyGenerate(xx, loc, finallyLevel);
      if (genRes.hasErrors()) {
        genRes.drainInto(erb.errors());
        return EResult.errs(erb.errors());
      }
      finallyNormal = Opt.some(combinedFinally);
    }
    var tryOpt =
        erb.mustErMaybeUnreachable(
            xx.target.targetTryCatchFinally(
                xx,
                loc,
                bodyCv,
                catch_,
                handlerKey == null ? Opt.<JumpKey>none() : Opt.some(handlerKey),
                finallyNormal));
    if (tryOpt.optIsNone()) return EResult.errs(erb.errors());
    Code tryCode = tryOpt.optUnwrap();
    if (finallyLevel != null) {
      var finSplice = blocksSplice(xx, loc, finallyLevel.scopeLevelEmission().blocks, erb);
      if (finSplice == null) return EResult.errs(erb.errors());
      tryCode = Code.concat(loc, tryCode, finSplice);
    }
    if (catchLevel != null) {
      var catchSplice = blocksSplice(xx, loc, catchLevel.scopeLevelEmission().blocks, erb);
      if (catchSplice == null) return EResult.errs(erb.errors());
      tryCode = Code.concat(loc, tryCode, catchSplice);
    }
    var innerCv = new CodeValue(tryCode,
        resultValue.optIsSome()
            ? EvalOutcome.ofValue(resultValue.optUnwrap())
            : EvalOutcome.NO_VALUE);
    var res = EvalRes.of(innerCv, xx.scope);
    return erb.errors().isEmpty() ? EResult.ok(res) : EResult.mixed(res, erb.errors());
  }

  private static EResult<EvalRes> constEvalStage(ExprContext xx, QExprId loc, Expr.Stage e) {
    var matchedLowers = new TList<Expr>();
    findMatchedLowersInner(e.expr(), 0, 0, matchedLowers);
    var matchedGrafts = new TList<Expr>();
    findMatchedGraftsInner(e.expr(), 0, matchedGrafts);
    if (matchedLowers.isEmpty() && matchedGrafts.isEmpty()) {

      return EResult.ok(EvalRes.of(new CodeValue(Code.none(loc), EvalOutcome.ofValue(e.expr())), xx.scope));
    }
    var erb = new Erb(loc);
    var lowerValues = new TList<CodeValue>(matchedLowers.size());
    for (var inner : matchedLowers) {
      var cvOpt = erb.mustCv(xx, constEvalOperand(xx, inner));
      if (cvOpt.optIsNone()) return EResult.errs(erb.errors());
      lowerValues.add(cvOpt.optUnwrap());
    }
    var graftValues = new TList<CodeValue>(matchedGrafts.size());
    for (var inner : matchedGrafts) {
      var cvOpt = erb.mustCv(xx, constEvalOperand(xx, inner));
      if (cvOpt.optIsNone()) return EResult.errs(erb.errors());
      graftValues.add(cvOpt.optUnwrap());
    }
    var staged = erb.mustEr(xx.target.targetStage(xx, loc, e.expr(), lowerValues, graftValues));
    if (staged.optIsNone()) return EResult.errs(erb.errors());
    return EResult.ok(EvalRes.of(staged.optUnwrap(), xx.scope));
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
        if (rt.value().optIsSome()) findMatchedLowersInner(rt.value().optUnwrap(), sd, ld, out);
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
        if (ifv.else_().optIsSome()) {
          findMatchedLowersInner(ifv.else_().optUnwrap(), sd, ld, out);
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
        if (m.fallback().optIsSome()) {
          findMatchedLowersInner(m.fallback().optUnwrap(), sd, ld, out);
        }
      }
      case Expr.Try tr -> {
        findMatchedLowersInner(tr.body(), sd, ld, out);
        if (tr.catchClause().optIsSome()) {
          var cs = tr.catchClause().optUnwrap();
          findMatchedLowersInner(cs.bindingName(), sd, ld, out);
          findMatchedLowersInner(cs.exceptionType(), sd, ld, out);
          findMatchedLowersInner(cs.body(), sd, ld, out);
        }
        if (tr.finallyBlock().optIsSome()) {
          findMatchedLowersInner(tr.finallyBlock().optUnwrap(), sd, ld, out);
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
        if (rt.value().optIsSome()) findMatchedGraftsInner(rt.value().optUnwrap(), sd, out);
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
        if (ifv.else_().optIsSome()) {
          findMatchedGraftsInner(ifv.else_().optUnwrap(), sd, out);
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
        if (m.fallback().optIsSome()) {
          findMatchedGraftsInner(m.fallback().optUnwrap(), sd, out);
        }
      }
      case Expr.Try tr -> {
        findMatchedGraftsInner(tr.body(), sd, out);
        if (tr.catchClause().optIsSome()) {
          var cs = tr.catchClause().optUnwrap();
          findMatchedGraftsInner(cs.bindingName(), sd, out);
          findMatchedGraftsInner(cs.exceptionType(), sd, out);
          findMatchedGraftsInner(cs.body(), sd, out);
        }
        if (tr.finallyBlock().optIsSome()) {
          findMatchedGraftsInner(tr.finallyBlock().optUnwrap(), sd, out);
        }
      }
    }
  }
}
