package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.main.syntax.BinaryOperator;
import compilerxyz.main.syntax.UnaryOperator;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactExpr(ArtifactQExprId loc, Variant variant) implements SafeJsonSerializable {

    public record ArtifactQExprId(long source, Opt<Long> expr, Opt<ArtifactQExprId> site) {
        public static ArtifactQExprId fromQExprId(QExprId loc) {
            return new ArtifactQExprId(
                    loc.source().value(), loc.expr(), loc.site().optMap(ArtifactQExprId::fromQExprId));
        }

        public QExprId toQExprId() {
            return new QExprId(new SourceId(source), expr, site.optMap(ArtifactQExprId::toQExprId));
        }
    }

    public record CondBranch(ArtifactExpr condition, ArtifactExpr body) {}

    public record MatchBranchA(ArtifactExpr condition, ArtifactExpr body) {}

    public record TryCatchA(ArtifactExpr bindingName, ArtifactExpr exceptionType, ArtifactExpr body) {}

    public sealed interface Variant {

        record As(ArtifactExpr base, ArtifactExpr target) implements Variant {}

        record Bind(ArtifactExpr base, ArtifactExpr value) implements Variant {}

        record Call(ArtifactExpr func, ArtifactExpr arg) implements Variant {}

        record Pipe(ArtifactExpr self, ArtifactExpr func, ArtifactExpr arg) implements Variant {}

        record Field(ArtifactExpr base, ArtifactExpr key) implements Variant {}

        record Foreach(ArtifactExpr base, ArtifactExpr elementKey, ArtifactExpr body) implements Variant {}

        record Graft(ArtifactExpr expr) implements Variant {}

        record If(TList<CondBranch> conditions, Opt<ArtifactExpr> else_) implements Variant {}

        record Labeled(String label, ArtifactExpr inner) implements Variant {}

        record Loop(ArtifactExpr body) implements Variant {}

        record Literal(Artifact value) implements Variant {}

        record Lower(ArtifactExpr expr) implements Variant {}

        record Match(ArtifactExpr base, TList<MatchBranchA> cases, ArtifactExpr elementKey, Opt<ArtifactExpr> fallback)
                implements Variant {}

        record OperatorBinary(BinaryOperator op, ArtifactExpr base, ArtifactExpr reference) implements Variant {}

        record OperatorUnary(UnaryOperator op, ArtifactExpr base) implements Variant {}

        record Record(TList<RecordEntryA> entries) implements Variant {}

        record Return(Opt<String> label, Opt<ArtifactExpr> value) implements Variant {}

        record ScopeUse(ArtifactExpr key) implements Variant {}

        record Seq(TList<ArtifactExpr> exprs) implements Variant {}

        record Set(ArtifactExpr base, ArtifactExpr value) implements Variant {}

        record Stage(ArtifactExpr expr) implements Variant {}

        record Try(ArtifactExpr body, Opt<TryCatchA> catchClause, Opt<ArtifactExpr> finallyBlock) implements Variant {}

        record Tuple(TList<ArtifactExpr> elements) implements Variant {}
    }

    public record RecordEntryA(ArtifactExpr key, ArtifactExpr value) {}
}
