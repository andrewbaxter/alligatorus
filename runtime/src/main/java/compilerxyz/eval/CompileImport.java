package compilerxyz.eval;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.gen.Value;
import static compilerxyz.util.MustCast.mustCast;

public final class CompileImport {
  private CompileImport() {}

  public record CompileImportResult(CompileRes res, TList<CompileId> localDeps) {}

  public static CompileImportResult compileImportId(
      GlobalContext gx,
      ImportStateId importState,
      Expr.Module source,
      TList<CompileId> compilePath,
      SourceSpecAbsolute currentSpec) {
    Expr rootExpr =
        switch (source) {
          case Expr.Module.V1 v1 -> v1.expr();
        };
    var rootLoc = rootExpr.loc();
    var sourceId = rootLoc.source();
    var extendedPath = new TList<CompileId>(compilePath.size() + 1);
    extendedPath.addAll(compilePath);
    extendedPath.add(new CompileId(importState, sourceId));
    var cix = new CompileImportContext(gx, importState, sourceId, extendedPath, currentSpec);

    var excess = maxLowerExcess(rootExpr, 0, 0);
    for (var i = 0; i < excess; i++) {
      rootExpr = new Expr(rootLoc, new Expr.Stage(rootExpr));
    }

    var allLogs = new TList<LogEntry>();
    while (true) {
      var isStage = rootExpr.variant() instanceof Expr.Stage;
      var evalResult = eval(cix, importState, rootExpr);
      switch (evalResult) {
        case EvalOutcome.Err err -> {
          return new CompileImportResult(
              new CompileRes(allLogs, new CompileResResult.Err(err.errors())),
              cix.localDependencies);
        }
        case EvalOutcome.Ok ok -> {
          allLogs.addAll(ok.logs());
          if (!isStage) {
            if (compilePath.isEmpty() && ok.producedDynamicRoot()) {
              return new CompileImportResult(
                  new CompileRes(
                      allLogs,
                      new CompileResResult.Ok(
                          new Artifact.Body(
                              new ArtifactBody(
                                  new JsonValueNull(),
                                  new ArtifactBodyExporter.Root())))),
                  cix.localDependencies);
            }
            var vx = new EvalContext(cix);
            var rootScope = new Scope(vx);
            var target = new TargetDynamic(vx.nextLocalId);
            var xx =
                new ExprContext(
                    rootScope, target, vx, importState, new TypeStringDynamic());
            var artifactRes = ArtifactSupport.exportArtifact(xx, rootLoc, ok.value());
            if (artifactRes.hasErrors()) {
              var artifactErrors = new TList<ExprErr>();
              artifactRes.drainInto(artifactErrors);
              return new CompileImportResult(
                  new CompileRes(allLogs, new CompileResResult.Err(artifactErrors)),
                  cix.localDependencies);
            }
            var artifact = artifactRes.requireValue("CompileImport::compile artifact export");
            return new CompileImportResult(
                new CompileRes(allLogs, new CompileResResult.Ok(artifact)), cix.localDependencies);
          }
          rootExpr = mustCast(Expr.class, ok.value());
        }
      }
    }
  }

  private static EvalOutcome eval(
      CompileImportContext cix, ImportStateId importState, Expr expr) {
    return Eval.eval(cix, importState, expr);
  }

  private static int maxLowerExcess(Expr expr, int sd, int ld) {
    return switch (expr.variant()) {
      case Expr.Lower l -> {
        var newLd = ld + 1;
        var excess = Math.max(0, newLd - sd);
        yield Math.max(excess, maxLowerExcess(l.expr(), sd, newLd));
      }
      case Expr.Stage s -> maxLowerExcess(s.expr(), sd + 1, ld);
      case Expr.Graft g ->
          switch (g) {
            case Expr.Graft.Pending p -> maxLowerExcess(p.expr(), sd, ld);
            case Expr.Graft.Evaluated e -> 0;
          };
      case Expr.Literal lit -> 0;
      case Expr.ScopeUse u -> 0;
      case Expr.Labeled lb -> maxLowerExcess(lb.inner(), sd, ld);
      case Expr.Return rt ->
          rt.value() instanceof Opt.Some<Expr> someV ? maxLowerExcess(someV.value(), sd, ld) : 0;
      case Expr.As a -> Math.max(maxLowerExcess(a.base(), sd, ld), maxLowerExcess(a.target(), sd, ld));
      case Expr.Call c -> Math.max(maxLowerExcess(c.func(), sd, ld), maxLowerExcess(c.arg(), sd, ld));
      case Expr.Bind b -> Math.max(maxLowerExcess(b.base(), sd, ld), maxLowerExcess(b.value(), sd, ld));
      case Expr.Set s -> Math.max(maxLowerExcess(s.base(), sd, ld), maxLowerExcess(s.value(), sd, ld));
      case Expr.Field f -> Math.max(maxLowerExcess(f.base(), sd, ld), maxLowerExcess(f.key(), sd, ld));
      case Expr.Seq s -> {
        var m = 0;
        for (var sub : s.exprs()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
        yield m;
      }
      case Expr.Tuple t -> {
        var m = 0;
        for (var sub : t.elements()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
        yield m;
      }
      case Expr.Record r -> {
        var m = 0;
        for (var entry : r.entries()) {
          m = Math.max(m, maxLowerExcess(entry.key(), sd, ld));
          m = Math.max(m, maxLowerExcess(entry.value(), sd, ld));
        }
        yield m;
      }
      case Expr.If i -> {
        var m = 0;
        for (var branch : i.conditions()) {
          m = Math.max(m, maxLowerExcess(branch.condition(), sd, ld));
          m = Math.max(m, maxLowerExcess(branch.body(), sd, ld));
        }
        var el = i.else_();
        if (el instanceof Opt.Some<Expr> some) {
          m = Math.max(m, maxLowerExcess(some.value(), sd, ld));
        }
        yield m;
      }
      case Expr.OperatorBinary op ->
          Math.max(maxLowerExcess(op.base(), sd, ld), maxLowerExcess(op.reference(), sd, ld));
      case Expr.OperatorUnary op -> maxLowerExcess(op.base(), sd, ld);
      case Expr.Foreach f -> {
        var m = maxLowerExcess(f.base(), sd, ld);
        m = Math.max(m, maxLowerExcess(f.elementKey(), sd, ld));
        m = Math.max(m, maxLowerExcess(f.body(), sd, ld));
        yield m;
      }
      case Expr.Match m -> {
        var x = maxLowerExcess(m.base(), sd, ld);
        x = Math.max(x, maxLowerExcess(m.elementKey(), sd, ld));
        for (var c : m.cases()) {
          x = Math.max(x, maxLowerExcess(c.condition(), sd, ld));
          x = Math.max(x, maxLowerExcess(c.body(), sd, ld));
        }
        var fb = m.fallback();
        if (fb instanceof Opt.Some<Expr> some) {
          x = Math.max(x, maxLowerExcess(some.value(), sd, ld));
        }
        yield x;
      }
      case Expr.Try t -> {
        var m = maxLowerExcess(t.body(), sd, ld);
        var cc = t.catchClause();
        if (cc instanceof Opt.Some<Expr.TryCatch> some) {
          var c = some.value();
          m = Math.max(m, maxLowerExcess(c.bindingName(), sd, ld));
          m = Math.max(m, maxLowerExcess(c.exceptionType(), sd, ld));
          m = Math.max(m, maxLowerExcess(c.body(), sd, ld));
        }
        var fb = t.finallyBlock();
        if (fb instanceof Opt.Some<Expr> some) {
          m = Math.max(m, maxLowerExcess(some.value(), sd, ld));
        }
        yield m;
      }
    };
  }

  public sealed interface EvalOutcome {

    record Ok(Value value, TList<LogEntry> logs, boolean producedDynamicRoot)
        implements EvalOutcome {}

    record Err(TList<ExprErr> errors) implements EvalOutcome {}
  }
}
