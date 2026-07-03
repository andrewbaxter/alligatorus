package compilerxyz.eval;

import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBoolConst;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectOperatorAdd;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.AspectOperatorLt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.AspectUnion;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.dynamic.Target;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.value.number.ValueNumberConst;
import compilerxyz.eval.value.null_.ValueNullConst;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.string.ValueStringConst;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.langhelp.SourceModuleToModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.langhelp.Sources;

public final class ConstEval {
  private ConstEval() {}

  public static EResult<CodeValue> constEval(ExprContext xx, Expr expr) {
    var loc = expr.loc();
    return switch (expr.variant()) {
      case Expr.Literal lit ->
          EResult.ok(new CodeValue(Code.none(loc), Opt.some(lit.value().consume())));
      case Expr.ScopeUse p -> {
        var erb = new Erb(loc);
        var keyR = constEval(xx, p.key());
        var keyOpt = erb.mustEr(keyR);
        if (keyOpt.isEmpty()) yield EResult.errs(erb.errors());
        var key = keyOpt.get();
        if (!(key.value() instanceof Opt.Some<Value> keyVal)) {
          erb.errPush("ScopeUse: key has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var keyConstRes = Eval.extractConstKey(xx, loc, keyVal.value());
        var keyConstOpt = erb.mustRes(keyConstRes);
        if (keyConstOpt.isEmpty()) yield EResult.errs(erb.errors());
        var ck = keyConstOpt.get();
        var at = xx.scope;
        Binding found = null;
        while (at != null) {
          var b = at.values.getOrNull(ck);
          if (b != null) {
            found = b;
            break;
          }
          at = at.parent;
        }
        if (found == null) {
          erb.errPush("No bound value with id [" + ck + "]");
          yield EResult.errs(erb.errors());
        }
        yield EResult.ok(found.bindingUse_(xx, loc, key.code()));
      }
      case Expr.Call p -> {
        var erb = new Erb(loc);
        var fopt = erb.mustEr(constEval(xx, p.func()));
        var aopt = erb.mustEr(constEval(xx, p.arg()));
        if (fopt.isEmpty() || aopt.isEmpty()) yield EResult.errs(erb.errors());
        var func = fopt.get();
        var arg = aopt.get();
        if (!(func.value() instanceof Opt.Some<Value> fv)) {
          erb.errPush("Call: function has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var funcType = fv.value().valueTypeOf();
        var aspectFnOpt = erb.mustEr(AspectFunction.get(xx, loc, funcType));
        if (aspectFnOpt.isEmpty()) yield EResult.errs(erb.errors());
        var aspectFn = aspectFnOpt.get();
        var calleeExc = aspectFn.exceptionType();
        var callOpt = erb.mustEr(aspectFn.call(xx, loc, func, arg));
        if (callOpt.isEmpty()) yield EResult.errs(erb.errors());
        var wrapped = erb.mustEr(Eval.maybeWrapCallException(xx, loc, calleeExc, callOpt.get()));
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
        if (!(base.value() instanceof Opt.Some<Value> bv)) {
          erb.errPush("Set: base has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var baseType = bv.value().valueTypeOf();
        var setOpt = erb.mustEr(AspectSet.get(xx, loc, baseType));
        if (setOpt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(setOpt.get().set(xx, loc, base, value));
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
        if (!(base.value() instanceof Opt.Some<Value> bv)) {
          erb.errPush("Field: base has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        if (!(field.value() instanceof Opt.Some<Value> fv)) {
          erb.errPush("Field: field key has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var baseType = bv.value().valueTypeOf();
        var aspectFieldOpt = erb.mustEr(AspectField.get(xx, loc, baseType));
        if (aspectFieldOpt.isEmpty()) yield EResult.errs(erb.errors());
        var aspectField = aspectFieldOpt.get();
        var extractedKeyOpt = erb.mustRes(Eval.extractConstKey(xx, loc, fv.value()));
        if (extractedKeyOpt.isEmpty()) yield EResult.errs(erb.errors());
        var extractedKey = extractedKeyOpt.get();
        var siblingDrops = new TList<Code>();
        boolean fieldFail = false;
        EResult<CodeValue> earlyReturn = null;
        for (var key : aspectField.fieldKeys(xx)) {
          if (key.equals(extractedKey)) continue;
          Value keyValue = switch (key) {
            case ConstKey.Str s -> new ValueStringConst(s.value());
            case ConstKey.Uint u -> new ValueNumberConst((double) u.value());
          };
          var keyCv = new CodeValue(Code.none(loc), Opt.some(keyValue));
          var fieldCvOpt = erb.mustEr(aspectField.field(xx, loc, base, keyCv));
          if (fieldCvOpt.isEmpty()) {
            earlyReturn = EResult.errs(erb.errors());
            fieldFail = true;
            break;
          }
          var fieldCv = fieldCvOpt.get();
          if (!(fieldCv.value() instanceof Opt.Some<Value> sib)) {
            erb.errPush(
                "Sibling field '" + key + "' listed in fieldKeys() but field() returned no value");
            earlyReturn = EResult.errs(erb.errors());
            fieldFail = true;
            break;
          }
          var fieldType = sib.value().valueTypeOf();
          var dropAspectOpt = AspectDropDynamic.getMaybe(xx, fieldType);
          if (dropAspectOpt instanceof Opt.Some<AspectDropDynamic> dropSome) {
            var dropOpt = erb.mustEr(dropSome.value().drop(xx, loc, fieldCv));
            if (dropOpt.isEmpty()) {
              earlyReturn = EResult.errs(erb.errors());
              fieldFail = true;
              break;
            }
            siblingDrops.add(dropOpt.get());
          }
        }
        if (fieldFail) yield earlyReturn;
        var primaryOpt = erb.mustEr(aspectField.field(xx, loc, base, field));
        if (primaryOpt.isEmpty()) yield EResult.errs(erb.errors());
        var primary = primaryOpt.get();
        if (siblingDrops.isEmpty()) {
          yield EResult.ok(primary);
        }
        siblingDrops.add(primary.code());
        var combinedOpt = erb.mustEr(xx.target.targetSeq(loc, siblingDrops));
        if (combinedOpt.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(new CodeValue(combinedOpt.get(), primary.value()));
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
        var elements = new TList<TupleElement>(t.elements().size());
        for (var elExpr : t.elements()) {
          elements.add(new TupleElement.EvalRes(constEval(xx, elExpr)));
        }
        yield EResult.ok(new CodeValue(Code.none(loc), Opt.some(new Tuple(elements))));
      }
      case Expr.Record r -> constEvalRecord(xx, loc, r);
      case Expr.OperatorLt e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        if (!(base.value() instanceof Opt.Some<Value> bv)) {
          erb.errPush("OperatorLt: base has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var aopt = erb.mustEr(AspectOperatorLt.get(xx, loc, bv.value().valueTypeOf()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().lt(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.OperatorEq e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        if (!(base.value() instanceof Opt.Some<Value> bv)) {
          erb.errPush("OperatorEq: base has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var aopt = erb.mustEr(AspectOperatorEq.get(xx, loc, bv.value().valueTypeOf()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().eq(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.OperatorAdd e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var ropt = erb.mustEr(constEval(xx, e.reference()));
        if (bopt.isEmpty() || ropt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        if (!(base.value() instanceof Opt.Some<Value> bv)) {
          erb.errPush("OperatorAdd: base has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var aopt = erb.mustEr(AspectOperatorAdd.get(xx, loc, bv.value().valueTypeOf()));
        if (aopt.isEmpty()) yield EResult.errs(erb.errors());
        var out = erb.mustEr(aopt.get().add(xx, loc, base, ropt.get()));
        if (out.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(out.get());
      }
      case Expr.Foreach e -> constEvalForeach(xx, loc, e);
      case Expr.Match e -> constEvalMatch(xx, loc, e);
      case Expr.Import e -> constEvalImport(xx, loc, e);
      case Expr.If e -> constEvalIf(xx, loc, e);
      case Expr.As e -> {
        var erb = new Erb(loc);
        var bopt = erb.mustEr(constEval(xx, e.base()));
        var topt = erb.mustEr(constEval(xx, e.target()));
        if (bopt.isEmpty() || topt.isEmpty()) yield EResult.errs(erb.errors());
        var base = bopt.get();
        var target = topt.get();
        if (!(target.value() instanceof Opt.Some<Value> tv)) {
          erb.errPush("As: target has no compile-time value");
          yield EResult.errs(erb.errors());
        }
        var targetMeta = tv.value().valueTypeOf();
        var aspectTypeOpt = erb.mustEr(AspectType.get(xx, loc, targetMeta));
        if (aspectTypeOpt.isEmpty()) yield EResult.errs(erb.errors());
        var targetType = aspectTypeOpt.get().type_(tv.value());
        var converted = erb.mustEr(Eval.typecheckConvert(xx, loc, targetType, base));
        if (converted.isEmpty()) yield EResult.errs(erb.errors());
        yield EResult.ok(converted.get());
      }
      case Expr.Try e -> constEvalTry(xx, loc, e);
    };
  }

  private static EResult<CodeValue> constEvalSeq(ExprContext xx, QExprId loc, Expr.Seq e) {
    var childScope = new Scope(xx.scope);
    var childXx = xx.child(childScope);
    var erb = new Erb(loc);

    class Frame {
      final TList<Code> codes = new TList<>();
      Code dropCode = Code.none(loc);
    }

    var frames = new TList<Frame>();
    frames.add(new Frame());
    Opt<Value> lastVal = Opt.some(new ValueNullConst());
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
        var dropOpt = erb.mustEr(childXx.target.targetDrop(childXx, sub.loc(), pendingDiscard));
        if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(dropOpt.get());
        // The pendingDiscard's return value is sitting on the JVM operand
        // stack from its emit — consume it. targetDrop is drop-semantics only
        // and does not POP.
        if (pendingDiscard.value() instanceof Opt.Some<Value> pdV
            && pdV.value().valueTypeOf() instanceof compilerxyz.gen.AspectConsumeDynamic) {
          frames
              .get(frames.size() - 1)
              .codes
              .add(
                  compilerxyz.eval.Code.of(
                      sub.loc(),
                      compilerxyz.eval.dynamic.JavaBytecodeUtils.inst(
                          sub.loc(), org.objectweb.asm.Opcodes.POP)));
        }
        pendingDiscard = null;
      }
      if (sub.variant() instanceof Expr.Bind bind) {
        var bopt = erb.mustEr(constEval(childXx, bind.base()));
        var vopt = erb.mustEr(constEval(childXx, bind.value()));
        if (bopt.isEmpty() || vopt.isEmpty()) return EResult.errs(erb.errors());
        var base = bopt.get();
        var value = vopt.get();
        if (!(base.value() instanceof Opt.Some<Value> baseV)) {
          erb.errPush("Bind: base has no compile-time value");
          return EResult.errs(erb.errors());
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(childXx, sub.loc(), baseV.value()));
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
        var valType = valV.value().valueTypeOf();
        var bindAspectOpt = erb.mustEr(AspectBind.get(childXx, sub.loc(), valType));
        if (bindAspectOpt.isEmpty()) return EResult.errs(erb.errors());
        var brOpt = erb.mustEr(bindAspectOpt.get().bind(childXx, sub.loc(), value));
        if (brOpt.isEmpty()) return EResult.errs(erb.errors());
        var br = brOpt.get();
        var bindingUseCv = br.binding().bindingUse_(childXx, sub.loc(), Code.none(sub.loc()));
        var dropOpt = erb.mustEr(childXx.target.targetDrop(childXx, sub.loc(), bindingUseCv));
        if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(br.code());
        var nf = new Frame();
        nf.dropCode = dropOpt.get();
        frames.add(nf);
        childXx.scope.values.put(ckOpt.get(), br.binding());
        lastVal = Opt.some(new ValueNullConst());
      } else {
        var ropt = erb.mustEr(constEval(childXx, sub));
        if (ropt.isEmpty()) return EResult.errs(erb.errors());
        var r = ropt.get();
        frames.get(frames.size() - 1).codes.add(r.code());
        if (r.value() instanceof Opt.None<Value>) {
          controlEnded = true;
          lastVal = Opt.none();
          continue;
        }
        if (idx + 1 < total) {
          pendingDiscard = r;
        } else {
          lastVal = r.value();
        }
      }
    }
    while (frames.size() > 1) {
      var inner = frames.remove(frames.size() - 1);
      if (inner.dropCode.isNone()) {
        frames.get(frames.size() - 1).codes.addAll(inner.codes);
      } else {
        var bodyOpt = erb.mustEr(childXx.target.targetSeq(loc, inner.codes));
        if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
        var wrappedOpt =
            erb.mustEr(childXx.target.targetTryFinally(loc, bodyOpt.get(), inner.dropCode));
        if (wrappedOpt.isEmpty()) return EResult.errs(erb.errors());
        frames.get(frames.size() - 1).codes.add(wrappedOpt.get());
      }
    }
    var outer = frames.remove(0);
    var seqOpt = erb.mustEr(childXx.target.targetSeq(loc, outer.codes));
    if (seqOpt.isEmpty()) return EResult.errs(erb.errors());
    var cv = new CodeValue(seqOpt.get(), lastVal);
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
      }
      var kCv = kRes.valueOrNull();
      if (kCv == null) continue;
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
        case Result.Ok<ConstKey, String> ok ->
            entries.put(ok.value(), constEval(xx, entry.value()));
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
    return EResult.ok(new CodeValue(code, Opt.some(new Record(Opt.none(), fields))));
  }

  private static EResult<CodeValue> constEvalForeach(
      ExprContext xx, QExprId loc, Expr.Foreach e) {
    var erb = new Erb(loc);
    var bopt = erb.mustEr(constEval(xx, e.base()));
    var ekopt = erb.mustEr(constEval(xx, e.elementKey()));
    if (bopt.isEmpty() || ekopt.isEmpty()) return EResult.errs(erb.errors());
    var base = bopt.get();
    var ekCv = ekopt.get();
    if (!(ekCv.value() instanceof Opt.Some<Value> ekv)) {
      erb.errPush("Foreach: element key has no compile-time value");
      return EResult.errs(erb.errors());
    }
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.isEmpty()) return EResult.errs(erb.errors());
    if (!(base.value() instanceof Opt.Some<Value> bv)) {
      erb.errPush("Foreach: base has no compile-time value");
      return EResult.errs(erb.errors());
    }
    var baseType = bv.value().valueTypeOf();
    var iterOpt = erb.mustEr(AspectIterable.get(xx, loc, baseType));
    if (iterOpt.isEmpty()) return EResult.errs(erb.errors());
    var iterable = iterOpt.get();
    var elementScopeId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var childScope = new Scope(xx.scope);
    var elementType = iterable.elementType();
    var ibOpt = erb.mustEr(AspectInitialBind.get(xx, loc, elementType));
    if (ibOpt.isEmpty()) return EResult.errs(erb.errors());
    childScope.values.put(ekOpt.get(), ibOpt.get().initialBind(elementScopeId));
    var childXx = xx.child(childScope);
    var bodyOpt = erb.mustEr(constEval(childXx, e.body()));
    if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
    var out = erb.mustEr(iterable.foreach(xx, loc, base, elementScopeId, bodyOpt.get()));
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
    if (!(ekCv.value() instanceof Opt.Some<Value> ekv)) {
      erb.errPush("Match: element key has no compile-time value");
      return EResult.errs(erb.errors());
    }
    var ekOpt = erb.mustRes(Eval.extractConstKey(xx, loc, ekv.value()));
    if (ekOpt.isEmpty()) return EResult.errs(erb.errors());
    var elementKey = ekOpt.get();
    if (!(base.value() instanceof Opt.Some<Value> bv)) {
      erb.errPush("Match: base has no compile-time value");
      return EResult.errs(erb.errors());
    }
    var baseType = bv.value().valueTypeOf();
    var unionOpt = erb.mustEr(AspectUnion.get(xx, loc, baseType));
    if (unionOpt.isEmpty()) return EResult.errs(erb.errors());
    var variants = unionOpt.get().variants();
    var caseTags = new TList<ConstKey>();
    for (var c : e.cases()) {
      var condOpt = erb.mustEr(constEval(xx, c.condition()));
      if (condOpt.isEmpty()) return EResult.errs(erb.errors());
      if (!(condOpt.get().value() instanceof Opt.Some<Value> cv)) {
        erb.errPush("Match: case condition has no compile-time value");
        return EResult.errs(erb.errors());
      }
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
    var matchCases = new TList<TargetCase>(e.cases().size());
    for (int i = 0; i < e.cases().size(); i++) {
      var tag = caseTags.get(i);
      AspectUnion.Variant info = null;
      for (var inf : variants) {
        if (inf.key().equals(tag)) {
          info = inf;
          break;
        }
      }
      matchCases.add(
          new TargetCase(info.jsTag(), info.type(), e.cases().get(i).body()));
    }
    var out = erb.mustEr(xx.target.targetMatch_(xx, loc, base, elementKey, matchCases, e.fallback()));
    if (out.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(out.get());
  }

  private static EResult<CodeValue> constEvalImport(ExprContext xx, QExprId loc, Expr.Import e) {
    var erb = new Erb(loc);
    var specOpt = erb.mustEr(constEval(xx, e.spec()));
    if (specOpt.isEmpty()) return EResult.errs(erb.errors());
    var specCv = specOpt.get();
    if (!(specCv.value() instanceof Opt.Some<Value> sv)) {
      erb.errPush("Import spec has no compile-time value");
      return EResult.errs(erb.errors());
    }
    var specKeyOpt = erb.mustRes(Eval.extractConstKey(xx, loc, sv.value()));
    if (specKeyOpt.isEmpty()) return EResult.errs(erb.errors());
    String specName;
    if (specKeyOpt.get() instanceof ConstKey.Str s) {
      specName = s.value();
    } else {
      erb.errPush("Import spec must be a string");
      return EResult.errs(erb.errors());
    }
    var sourceSpec = new SourceSpecAbsolute.Virtual(specName);
    var sourceLookup = xx.vx.ix.gx.sources.sourcesGet(sourceSpec);
    SourceId sourceId;
    switch (sourceLookup) {
      case Result.Ok<Sources.SourceLookup, String> ok -> sourceId = ok.value().id();
      case Result.Err<Sources.SourceLookup, String> err -> {
        erb.errPush(err.error());
        return EResult.errs(erb.errors());
      }
    }
    var compileId = new CompileId(xx.importState, sourceId);
    if (xx.vx.ix.compilePath.contains(compileId)) {
      erb.errPush("Import cycle detected");
      return EResult.errs(erb.errors());
    }
    var newCompilePath = new TList<CompileId>(xx.vx.ix.compilePath.size());
    newCompilePath.addAll(xx.vx.ix.compilePath);
    var sourceLookupOk = (Result.Ok<Sources.SourceLookup, String>) sourceLookup;
    var sourceModule = sourceLookupOk.value().module();
    GlobalContext gx = xx.vx.ix.gx;
    GlobalContext.CompileResultEntry entry;
    boolean owner;
    gx.compileResultsLock.lock();
    try {
      var existing = gx.compileResults.get(compileId);
      if (existing != null) {
        entry = existing;
        owner = false;
      } else {
        entry = new GlobalContext.CompileResultEntry();
        entry.lock.writeLock().lock();
        gx.compileResults.put(compileId, entry);
        owner = true;
      }
    } finally {
      gx.compileResultsLock.unlock();
    }
    CompileRes compileRes;
    if (owner) {
      try {
        var module = SourceModuleToModule.sourceModuleToModule(sourceId, sourceModule);
        compileRes = CompileImport.compileImportId(gx, xx.importState, module, newCompilePath);
        entry.value = compileRes;
      } finally {
        entry.lock.writeLock().unlock();
      }
    } else {
      entry.lock.readLock().lock();
      try {
        compileRes = entry.value;
        if (compileRes == null) {
          erb.errPush("Import: re-entrant compile slot has no result");
          return EResult.errs(erb.errors());
        }
      } finally {
        entry.lock.readLock().unlock();
      }
    }
    Artifact artifact;
    if (compileRes.result() instanceof CompileResResult.Ok okRes) {
      artifact = okRes.artifact();
    } else {
      var errRes = (CompileResResult.Err) compileRes.result();
      for (var ee : errRes.errors()) {
        erb.errors().add(ee);
      }
      erb.errPush("Import failed");
      return EResult.errs(erb.errors());
    }
    var exporterRes = erb.mustEr(ArtifactSupport.importArtifact(xx, loc, artifact));
    if (exporterRes.isEmpty()) return EResult.errs(erb.errors());
    var valueRes = erb.mustEr(exporterRes.get().exporterAnyAsValue());
    if (valueRes.isEmpty()) return EResult.errs(erb.errors());
    var cv = new CodeValue(Code.none(loc), Opt.some(valueRes.get()));
    var valueType = valueRes.get().valueTypeOf();
    var pdRes = erb.mustEr(AspectPostDeserialize.get(xx, loc, valueType));
    if (pdRes.isEmpty()) return EResult.errs(erb.errors());
    var finalRes = erb.mustEr(pdRes.get().postDeserialize(xx, loc, cv));
    if (finalRes.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(finalRes.get());
  }

  private static EResult<CodeValue> constEvalIf(ExprContext xx, QExprId loc, Expr.If e) {
    var erb = new Erb(loc);
    var conditionsEvaluated = new TList<IfEval>();
    boolean allConst = true;
    for (var branch : e.conditions()) {
      var condOpt = erb.mustEr(constEval(xx, branch.condition()));
      if (condOpt.isEmpty()) return EResult.errs(erb.errors());
      var cond = condOpt.get();
      if (!(cond.value() instanceof Opt.Some<Value> cv)) {
        erb.errPush("If: condition has no compile-time value");
        return EResult.errs(erb.errors());
      }
      var condType = cv.value().valueTypeOf();
      var boolAspect = AspectBoolConst.getMaybe(xx, condType);
      if (boolAspect instanceof Opt.Some<AspectBoolConst> some) {
        var val = some.value().get(cv.value());
        conditionsEvaluated.add(new IfEval(val, cond, branch.body()));
      } else {
        allConst = false;
        conditionsEvaluated.add(new IfEval(false, cond, branch.body()));
      }
    }
    if (allConst) {
      for (var ie : conditionsEvaluated) {
        if (ie.val) {
          var bodyOpt = erb.mustEr(constEval(xx, ie.body));
          if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
          return EResult.ok(bodyOpt.get());
        }
      }
      if (e.else_() instanceof Opt.Some<Expr> els) {
        var elseOpt = erb.mustEr(constEval(xx, els.value()));
        if (elseOpt.isEmpty()) return EResult.errs(erb.errors());
        return EResult.ok(elseOpt.get());
      }
      return EResult.ok(new CodeValue(Code.none(loc), Opt.some(new ValueNullConst())));
    } else {
      var branches = new TList<Target.IfBranch>(conditionsEvaluated.size());
      for (var ie : conditionsEvaluated) {
        branches.add(new Target.IfBranch(ie.cond, ie.body));
      }
      var out = erb.mustEr(xx.target.targetIf_(xx, loc, branches, e.else_()));
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
      if (!(etCv.value() instanceof Opt.Some<Value> etv)) {
        erb.errPush("Try: catch exception type has no compile-time value");
        return EResult.errs(erb.errors());
      }
      var etMeta = etv.value().valueTypeOf();
      var atOpt = erb.mustEr(AspectType.get(xx, loc, etMeta));
      if (atOpt.isEmpty()) return EResult.errs(erb.errors());
      bodyExcType = atOpt.get().type_(etv.value());
    } else {
      bodyExcType = xx.currentExceptionType;
    }
    var bodyXx = xx.withExceptionType(bodyExcType);
    var bodyOpt = erb.mustEr(constEval(bodyXx, e.body()));
    if (bodyOpt.isEmpty()) return EResult.errs(erb.errors());
    var bodyCv = bodyOpt.get();
    Type bodyType;
    if (bodyCv.value() instanceof Opt.Some<Value> bv2) {
      bodyType = bv2.value().valueTypeOf();
    } else {
      bodyType = null;
    }
    var bodyCd = bodyCv.code().dynamic();
    if (bodyCd == null) {
      erb.errPush("Try body must produce CodeDynamic (got Code.none)");
      return EResult.errs(erb.errors());
    }
    Opt<Value> resultValue = bodyCv.value();
    Opt<Target.TryCatchClause> catch_ = Opt.none();
    if (e.catchClause() instanceof Opt.Some<Expr.TryCatch> catchSome2) {
      var catchObj = catchSome2.value();
      var bnCvOpt = erb.mustEr(constEval(xx, catchObj.bindingName()));
      if (bnCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bnCv = bnCvOpt.get();
      if (!(bnCv.value() instanceof Opt.Some<Value> bnv)) {
        erb.errPush("Try: catch binding name has no compile-time value");
        return EResult.errs(erb.errors());
      }
      var bnCkOpt =
          erb.mustRes(Eval.extractConstKey(xx, catchObj.bindingName().loc(), bnv.value()));
      if (bnCkOpt.isEmpty()) return EResult.errs(erb.errors());
      var bindingId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
      var catchScope = new Scope(xx.scope);
      var ibMaybe = AspectInitialBind.getMaybe(xx, bodyExcType);
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        catchScope.values.put(bnCkOpt.get(), ibSome.value().initialBind(bindingId));
      }
      var catchXx = xx.child(catchScope);
      var catchCvOpt = erb.mustEr(constEval(catchXx, catchObj.body()));
      if (catchCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var catchCv = catchCvOpt.get();
      if (bodyType != null && catchCv.value() instanceof Opt.Some<Value>) {
        var convOpt = erb.mustEr(Eval.typecheckConvert(catchXx, loc, bodyType, catchCv));
        if (convOpt.isEmpty()) return EResult.errs(erb.errors());
        catchCv = convOpt.get();
      }
      if (!(resultValue instanceof Opt.Some<Value>)) {
        resultValue = catchCv.value();
      }
      var catchCd = catchCv.code().dynamic();
      if (catchCd == null) {
        erb.errPush("Try catch body must produce CodeDynamic (got Code.none)");
        return EResult.errs(erb.errors());
      }
      catch_ = Opt.some(new Target.TryCatchClause(bindingId, catchCd));
    }
    Opt<CodeDynamic> finally_ = Opt.none();
    if (e.finallyBlock() instanceof Opt.Some<Expr> finSome) {
      var finCvOpt = erb.mustEr(constEval(xx, finSome.value()));
      if (finCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var finCv = finCvOpt.get();
      var finCd = finCv.code().dynamic();
      if (finCd == null) {
        erb.errPush("Try finally body must produce CodeDynamic (got Code.none)");
        return EResult.errs(erb.errors());
      }
      Code finallyDropCode = Code.none(loc);
      if (finCv.value() instanceof Opt.Some<Value> finV) {
        var finType = finV.value().valueTypeOf();
        var dropMaybe = AspectDropDynamic.getMaybe(xx, finType);
        if (dropMaybe instanceof Opt.Some<AspectDropDynamic> dropSome) {
          var dropOpt = erb.mustEr(dropSome.value().drop(xx, loc, finCv));
          if (dropOpt.isEmpty()) return EResult.errs(erb.errors());
          finallyDropCode = dropOpt.get();
        }
      }
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
        erb.mustEr(xx.target.targetTryCatchFinally(loc, bodyCd, catch_, finally_));
    if (tryOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(new CodeValue(Code.of(loc, tryOpt.get()), resultValue));
  }

  private static EResult<CodeValue> constEvalStage(ExprContext xx, QExprId loc, Expr.Stage e) {
    var matchedLowers = new TList<Expr>();
    findMatchedLowersInner(e.expr(), 0, 0, matchedLowers);
    var matchedGrafts = new TList<Expr>();
    findMatchedGraftsInner(e.expr(), 0, matchedGrafts);
    if (matchedLowers.isEmpty() && matchedGrafts.isEmpty()) {

      return EResult.ok(new CodeValue(Code.none(loc), Opt.some(e.expr())));
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
      case Expr.Import ignored -> {}
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
      case Expr.Import ignored -> {}
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
