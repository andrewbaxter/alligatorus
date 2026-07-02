package compilerxyz.eval.builtin;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ContextualFunction;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.dynamic.StageLiftCell;
import compilerxyz.eval.expr.CodeValueExternCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_interface.SourceInterfaceRef;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenInvisibleBuiltins;
import compilerxyz.gen.Target;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public final class InvisibleBuiltinDispatch {
  private InvisibleBuiltinDispatch() {}

  public static TOrderedMap<String, Object> hostValues() {
    var t = new TOrderedMap<String, Object>();
    putBuiltin(t, GenInvisibleBuiltins.STAGE_FN_SCOPE_ID, stageBody());
    putBuiltin(t, GenInvisibleBuiltins.RECORD_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "record_to_type",
        "invisible_builtins.rs scope 7016 body: builds a TypeRecord from a v8 object via"
            + " dynamic_value_to_type — pending Type record + dynamic_value_to_type port"));
    putBuiltin(t, GenInvisibleBuiltins.TUPLE_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "tuple_to_type",
        "invisible_builtins.rs scope 7020 body: builds a TypeTuple from a v8 array via"
            + " dynamic_value_to_type — pending Type tuple + dynamic_value_to_type port"));
    putBuiltin(t, GenInvisibleBuiltins.CVE_EXTRACT_FN_SCOPE_ID, unportedBody(
        "cve_extract",
        "invisible_builtins.rs scope 7003 body: extracts a CodeValueExtern's inner CodeValue.value"
            + " and routes through AspectStringConst — pending DynamicExprContextSlot.xx +"
            + " aspect_string_const_get_maybe bridge"));
    putBuiltin(t, GenInvisibleBuiltins.CVE_GET_CODE_ENUM_FN_SCOPE_ID, unportedBody(
        "cve_get_code_enum",
        "invisible_builtins.rs scope 7004 body: returns the inner Code enum tag of a"
            + " CodeValueExtern — pending Code Dyn variant external-bridge + ConstKey-keyed v8"
            + " object equivalent on JVM"));
    putBuiltin(t, GenInvisibleBuiltins.CVE_GET_VALUE_FN_SCOPE_ID, unportedBody(
        "cve_get_value",
        "invisible_builtins.rs scope 7005 body: extracts the inner Value from a CodeValueExtern"
            + " — pending rc_value_to_dynamic_ext analogue on JVM"));
    putBuiltin(t, GenInvisibleBuiltins.VALUE_GET_TYPE_FN_SCOPE_ID, unportedBody(
        "value_get_type",
        "invisible_builtins.rs scope 7022 body: returns TypeRefPush wrapping the value's type ——"
            + " pending TypeRefPush + DynamicExprContextSlot.xx bridge"));
    putBuiltin(t, GenInvisibleBuiltins.ASPECT_LOOKUP_FN_SCOPE_ID, unportedBody(
        "aspect_lookup",
        "invisible_builtins.rs scope 7001 body: type.typeAspect(xx, unique) lookup — pending"
            + " TypeRefPush + AspectField + DynamicExprContextSlot.xx bridge"));
    putBuiltin(t, GenInvisibleBuiltins.CAST_VALUE_FN_SCOPE_ID, unportedBody(
        "cast_value",
        "invisible_builtins.rs scope 7002 body: unwraps a ValuePull's instance to the dynamic"
            + " bundle — pending ValuePull + TypeSourceObjectConst port"));
    putBuiltin(t, GenInvisibleBuiltins.DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "object_ref_to_type",
        "invisible_builtins.rs scope 7013 body: wraps a SourceObjectRef into a"
            + " TypeSourceObjectConst — pending object SourceObjectRef + TypeSourceObjectConst"
            + " port"));
    putBuiltin(t, GenInvisibleBuiltins.TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "union_ref_to_type",
        "invisible_builtins.rs scope 7019 body: wraps a SourceUnionRef into a"
            + " TypeSourceUnionConst — pending union SourceUnionRef + TypeSourceUnionConst"
            + " port"));
    putBuiltin(t, GenInvisibleBuiltins.INTERFACE_REF_TO_TYPE_FN_SCOPE_ID, interfaceRefToTypeBody());
    putBuiltin(t, GenInvisibleBuiltins.FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "function_const_to_type",
        "invisible_builtins.rs scope 7014 body: re-wraps a TypeSourceFunctionConst — pending"
            + " TypeSourceFunctionConst port"));
    putBuiltin(t, GenInvisibleBuiltins.DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "object_new_to_type",
        "invisible_builtins.rs scope 7012 body: re-wraps a TypeSourceObjectNewConst — pending"
            + " object_new TypeSourceObjectNewConst port"));
    putBuiltin(t, GenInvisibleBuiltins.TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID, unportedBody(
        "union_new_to_type",
        "invisible_builtins.rs scope 7018 body: builds a TypeSourceUnionNewConst from"
            + " SourceUnionNewRef — pending union_new SourceUnionNewRef +"
            + " TypeSourceUnionNewConst port"));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID,
        dataInstanceToHostBody(
            "data_instance_to_type", Type.class,  true));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID,
        dataInstanceToHostBody(
            "data_instance_to_target", Target.class,  true));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID,
        dataInstanceToHostBody(
            "data_instance_to_binding", Binding.class,  true));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID, unportedBody(
        "data_instance_to_value",
        "invisible_builtins.rs scope 7011 body: builds a ValuePull around a v8 instance Global —"
            + " pending value_pull::dynamic::ValuePull port"));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID,
        dataInstanceToHostBody(
            "data_instance_to_aspect",
            Aspect.class,
             false));
    putBuiltin(t, GenInvisibleBuiltins.DATA_INSTANCE_TO_CODE_FN_SCOPE_ID, unportedBody(
        "data_instance_to_code",
        "invisible_builtins.rs scope 7007 body: builds a CodePull around a v8 instance Global —"
            + " pending eval::CodePull port"));
    putBuiltin(t, GenInvisibleBuiltins.UNIQUE_EQ_FN_SCOPE_ID, uniqueEqBody());
    return t;
  }

  private static void putBuiltin(
      TOrderedMap<String, Object> map,
      BindingId id,
      ContextualFunction body) {
    var builtin = (BindingId.Builtin) id;
    map.put(Prefixes.BUILTIN_PREFIX + builtin.index(), body);
  }

  private static ContextualFunction dataInstanceToHostBody(
      String name, Class<?> hostInterface, boolean hasTemplate) {
    return (xx, arg) -> {
      Object instance;
      if (hasTemplate) {
        var args = (Object[]) arg;
        instance = args[1];
      } else {
        instance = arg;
      }
      if (!hostInterface.isInstance(instance)) {
        throw new IllegalStateException(
            "invisible builtin "
                + name
                + ": instance "
                + (instance == null ? "null" : instance.getClass().getName())
                + " does not implement "
                + hostInterface.getName()
                + " — generated source-object class must implement the host interface (see D-5c)");
      }
      return instance;
    };
  }

  private static ContextualFunction unportedBody(String name, String detail) {
    return (xx, arg) -> {
      throw new UnsupportedOperationException(
          "invisible builtin " + name + ": " + detail);
    };
  }

  private static ContextualFunction interfaceRefToTypeBody() {
    return (xx, arg) -> {
      var ref = (SourceInterfaceRef) arg;
      return new TypeSourceInterfaceConst(ref.data());
    };
  }

  private static ContextualFunction uniqueEqBody() {
    return (xx, arg) -> {
      var args = (Object[]) arg;
      var a = (Unique) args[0];
      var b = (Unique) args[1];
      return a.equals(b);
    };
  }

  private static ContextualFunction stageBody() {
    return (xx, arg) -> {
      var args = (Object[]) arg;
      var treeCell = (StagedExprCell) args[0];
      var treeValue = treeCell.take();
      if (!(treeValue instanceof Expr tree)) {
        throw new IllegalStateException(
            "__stage: tree cell held non-Expr value: " + treeValue.getClass());
      }
      var lowerCount = ((Number) args[1]).intValue();
      var values = new TList<StageLowerValue>(lowerCount);
      var i = 2;
      for (var k = 0; k < lowerCount; k++) {
        var liftArg = args[i];
        var valArg = args[i + 1];
        i += 2;
        if (liftArg == null) {
          var cell = (StagedExprCell) valArg;
          var cellValue = cell.take();
          if (cellValue instanceof Expr expr) {
            values.add(new StageLowerValue.Expr(expr));
          } else {
            values.add(new StageLowerValue.Val((Value) cellValue));
          }
        } else {
          var liftCell = (StageLiftCell) liftArg;
          var liftXx =
              new ExprContext(
                  liftCell.scope(),
                  liftCell.target(),
                  liftCell.vx(),
                  liftCell.importState(),
                  liftCell.currentExceptionType());
          var liftRes =
              AspectLift.get(liftXx, liftCell.loc(), liftCell.sourceType());
          if (liftRes.hasErrors()) {
            throw new IllegalStateException(
                "__stage: AspectLift lookup failed for source type "
                    + liftCell.sourceType().typeDesc()
                    + ": "
                    + liftRes.errorsOrNull());
          }
          var liftAspect = liftRes.valueOrNull();
          if (liftAspect == null) {
            throw new IllegalStateException(
                "__stage: AspectLift.get returned neither value nor errors");
          }
          var liftedRes = liftAspect.liftRuntime(liftXx, liftCell.loc(), valArg);
          if (liftedRes.hasErrors()) {
            throw new IllegalStateException(
                "__stage: AspectLift.liftRuntime failed for source type "
                    + liftCell.sourceType().typeDesc()
                    + ": "
                    + liftedRes.errorsOrNull());
          }
          var lifted = liftedRes.valueOrNull();
          if (lifted == null) {
            throw new IllegalStateException(
                "__stage: AspectLift.liftRuntime returned neither value nor errors");
          }
          values.add(new StageLowerValue.Val(lifted));
        }
      }
      var grafts = new TList<CodeValue>(args.length - i);
      while (i < args.length) {
        var cell = (CodeValueExternCell) args[i];
        grafts.add(cell.take());
        i += 1;
      }
      var valueIter = values.iterator();
      var graftIter = grafts.iterator();
      var newTree = cloneWithLowerSubstitutions(tree, 0, 0, valueIter, graftIter);
      return new StagedExprCell(newTree);
    };
  }

  private sealed interface StageLowerValue {
    record Expr(compilerxyz.eval.Expr expr) implements StageLowerValue {}

    record Val(Value value) implements StageLowerValue {}
  }

  private static Expr cloneWithLowerSubstitutions(
      Expr expr,
      int sd,
      int ld,
      java.util.Iterator<StageLowerValue> values,
      java.util.Iterator<CodeValue> grafts) {
    var loc = expr.loc();
    return switch (expr.variant()) {
      case Expr.Lower lower -> {
        var newLd = ld + 1;
        if (newLd - sd == 1) {
          if (!values.hasNext()) {
            throw new IllegalStateException(
                "__stage: not enough lower values for substitution");
          }
          yield switch (values.next()) {
            case StageLowerValue.Expr e -> e.expr();
            case StageLowerValue.Val v -> new Expr(
                loc,
                new Expr.Literal(
                    Expr.LitValue.once(v.value())));
          };
        }
        yield new Expr(
            loc,
            new Expr.Lower(
                cloneWithLowerSubstitutions(lower.expr(), sd, newLd, values, grafts)));
      }
      case Expr.Stage stage -> new Expr(
          loc,
          new Expr.Stage(
              cloneWithLowerSubstitutions(stage.expr(), sd + 1, ld, values, grafts)));
      case Expr.Graft graft -> {
        Expr.Graft next =
            switch (graft) {
              case Expr.Graft.Pending p -> {
                if (sd == 0) {
                  if (!grafts.hasNext()) {
                    throw new IllegalStateException(
                        "__stage: not enough graft values for substitution");
                  }
                  yield new Expr.Graft.Evaluated(grafts.next());
                }
                yield new Expr.Graft.Pending(
                    cloneWithLowerSubstitutions(p.expr(), sd, ld, values, grafts));
              }
              case Expr.Graft.Evaluated e -> e;
            };
        yield new Expr(loc, next);
      }
      case Expr.Literal lit -> expr;
      case Expr.ScopeUse u -> expr;
      case Expr.Import imp -> expr;
      case Expr.As a -> new Expr(
          loc,
          new Expr.As(
              cloneWithLowerSubstitutions(a.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(a.target(), sd, ld, values, grafts)));
      case Expr.Call c -> new Expr(
          loc,
          new Expr.Call(
              cloneWithLowerSubstitutions(c.func(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(c.arg(), sd, ld, values, grafts)));
      case Expr.Bind b -> new Expr(
          loc,
          new Expr.Bind(
              cloneWithLowerSubstitutions(b.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(b.value(), sd, ld, values, grafts)));
      case Expr.Set s -> new Expr(
          loc,
          new Expr.Set(
              cloneWithLowerSubstitutions(s.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(s.value(), sd, ld, values, grafts)));
      case Expr.Field f -> new Expr(
          loc,
          new Expr.Field(
              cloneWithLowerSubstitutions(f.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(f.key(), sd, ld, values, grafts)));
      case Expr.Seq seq -> {
        var newExprs = new TList<Expr>();
        for (var sub : seq.exprs()) {
          newExprs.add(cloneWithLowerSubstitutions(sub, sd, ld, values, grafts));
        }
        yield new Expr(loc, new Expr.Seq(newExprs));
      }
      case Expr.Tuple tup -> {
        var newElems = new TList<Expr>();
        for (var sub : tup.elements()) {
          newElems.add(cloneWithLowerSubstitutions(sub, sd, ld, values, grafts));
        }
        yield new Expr(loc, new Expr.Tuple(newElems));
      }
      case Expr.Record rec -> {
        var newEntries = new TList<Expr.RecordEntry>();
        for (var entry : rec.entries()) {
          newEntries.add(
              new Expr.RecordEntry(
                  cloneWithLowerSubstitutions(entry.key(), sd, ld, values, grafts),
                  cloneWithLowerSubstitutions(entry.value(), sd, ld, values, grafts)));
        }
        yield new Expr(loc, new Expr.Record(newEntries));
      }
      case Expr.If ifExpr -> {
        var newConds = new TList<Expr.ConditionBranch>();
        for (var branch : ifExpr.conditions()) {
          newConds.add(
              new Expr.ConditionBranch(
                  cloneWithLowerSubstitutions(branch.condition(), sd, ld, values, grafts),
                  cloneWithLowerSubstitutions(branch.body(), sd, ld, values, grafts)));
        }
        Opt<Expr> newElse;
        if (ifExpr.else_() instanceof Opt.Some<Expr> some) {
          newElse =
              Opt.some(
                  cloneWithLowerSubstitutions(some.value(), sd, ld, values, grafts));
        } else {
          newElse = Opt.none();
        }
        yield new Expr(loc, new Expr.If(newConds, newElse));
      }
      case Expr.OperatorLt op -> new Expr(
          loc,
          new Expr.OperatorLt(
              cloneWithLowerSubstitutions(op.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(op.reference(), sd, ld, values, grafts)));
      case Expr.OperatorEq op -> new Expr(
          loc,
          new Expr.OperatorEq(
              cloneWithLowerSubstitutions(op.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(op.reference(), sd, ld, values, grafts)));
      case Expr.OperatorAdd op -> new Expr(
          loc,
          new Expr.OperatorAdd(
              cloneWithLowerSubstitutions(op.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(op.reference(), sd, ld, values, grafts)));
      case Expr.Foreach fe -> new Expr(
          loc,
          new Expr.Foreach(
              cloneWithLowerSubstitutions(fe.base(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(fe.elementKey(), sd, ld, values, grafts),
              cloneWithLowerSubstitutions(fe.body(), sd, ld, values, grafts)));
      case Expr.Match m -> {
        var newCases = new TList<Expr.MatchBranch>();
        for (var c : m.cases()) {
          newCases.add(
              new Expr.MatchBranch(
                  cloneWithLowerSubstitutions(c.condition(), sd, ld, values, grafts),
                  cloneWithLowerSubstitutions(c.body(), sd, ld, values, grafts)));
        }
        Opt<Expr> newFallback;
        if (m.fallback() instanceof Opt.Some<Expr> some) {
          newFallback =
              Opt.some(
                  cloneWithLowerSubstitutions(some.value(), sd, ld, values, grafts));
        } else {
          newFallback = Opt.none();
        }
        yield new Expr(
            loc,
            new Expr.Match(
                cloneWithLowerSubstitutions(m.base(), sd, ld, values, grafts),
                newCases,
                cloneWithLowerSubstitutions(m.elementKey(), sd, ld, values, grafts),
                newFallback));
      }
      case Expr.Try t -> {
        Opt<Expr.TryCatch> newCatch;
        if (t.catchClause() instanceof Opt.Some<Expr.TryCatch> someCatch) {
          var c = someCatch.value();
          newCatch =
              Opt.some(
                  new Expr.TryCatch(
                      cloneWithLowerSubstitutions(c.bindingName(), sd, ld, values, grafts),
                      cloneWithLowerSubstitutions(c.exceptionType(), sd, ld, values, grafts),
                      cloneWithLowerSubstitutions(c.body(), sd, ld, values, grafts)));
        } else {
          newCatch = Opt.none();
        }
        Opt<Expr> newFinally;
        if (t.finallyBlock() instanceof Opt.Some<Expr> someFin) {
          newFinally =
              Opt.some(
                  cloneWithLowerSubstitutions(someFin.value(), sd, ld, values, grafts));
        } else {
          newFinally = Opt.none();
        }
        yield new Expr(
            loc,
            new Expr.Try(
                cloneWithLowerSubstitutions(t.body(), sd, ld, values, grafts),
                newCatch,
                newFinally));
      }
    };
  }
}
