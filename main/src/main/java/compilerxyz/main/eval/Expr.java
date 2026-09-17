package compilerxyz.main.eval;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.BinaryOperator;
import compilerxyz.main.syntax.UnaryOperator;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record Expr(QExprId loc, Variant variant) implements Value {

    public static Type dynamicType() {
        return new TypeExpr();
    }

    public Type valueType() {
        return new TypeExpr();
    }

    public sealed interface Variant {}

    public record As(Expr base, Expr target) implements Variant {}

    public record Bind(Expr base, Expr value) implements Variant {}

    public record Call(Expr func, Expr arg) implements Variant {}

    public record ConditionBranch(Expr condition, Expr body) {}

    public record Field(Expr base, Expr key) implements Variant {}

    public record Foreach(Expr base, Expr elementKey, Expr body) implements Variant {}

    public sealed interface Graft extends Variant {
        record Pending(Expr expr) implements Graft {}

        record Evaluated(CodeValue value) implements Graft {}
    }

    public record If(TList<ConditionBranch> conditions, Opt<Expr> else_) implements Variant {}

    public record Labeled(String label, Expr inner) implements Variant {}

    public record Literal(LitValue value) implements Variant {}

    public record Lower(Expr expr) implements Variant {}

    public record Match(Expr base, TList<MatchBranch> cases, Expr elementKey, Opt<Expr> fallback) implements Variant {}

    public record MatchBranch(Expr condition, Expr body) {}

    public record OperatorBinary(BinaryOperator op, Expr base, Expr reference) implements Variant {}

    public record OperatorUnary(UnaryOperator op, Expr base) implements Variant {}

    public record Record(TList<RecordEntry> entries) implements Variant {}

    public record RecordEntry(Expr key, Expr value) {}

    public record Return(Opt<String> label, Opt<Expr> value) implements Variant {}

    public record ScopeUse(Expr key) implements Variant {}

    public record Seq(TList<Expr> exprs) implements Variant {}

    public record Set(Expr base, Expr value) implements Variant {}

    public record Stage(Expr expr) implements Variant {}

    public record Try(Expr body, Opt<TryCatch> catchClause, Opt<Expr> finallyBlock) implements Variant {}

    public record TryCatch(Expr bindingName, Expr exceptionType, Expr body) {}

    public record Tuple(TList<Expr> elements) implements Variant {}

    public static final class LitValue {
        final boolean once;
        Opt<Value> value;

        public LitValue(Value value, boolean once) {
            this.value = Opt.some(value);
            this.once = once;
        }

        public static LitValue shared(Value v) {
            return new LitValue(v, false);
        }

        public Opt<Value> litValueValue() {
            return value;
        }
    }

    public sealed interface Module {
        record V1(Expr expr) implements Module {}
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
