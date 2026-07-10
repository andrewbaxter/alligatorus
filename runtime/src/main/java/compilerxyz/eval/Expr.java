package compilerxyz.eval;

import compilerxyz.eval.value.expr.TypeExpr;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;
import compilerxyz.gen.Value;

public record Expr(QExprId loc, Variant variant) implements Value {

    public static Type dynamicType() {
        return new TypeExpr();
    }

    @Override
    public Type valueType() {
        return new TypeExpr();
    }

    public sealed interface Variant {
    }

    public record As(Expr base, Expr target) implements Variant {
    }

    public record Bind(Expr base, Expr value) implements Variant {
    }

    public record Call(Expr func, Expr arg) implements Variant {
    }

    public record ConditionBranch(Expr condition, Expr body) {
    }

    public record Field(Expr base, Expr key) implements Variant {
    }

    public record Foreach(Expr base, Expr elementKey, Expr body) implements Variant {
    }

    public sealed interface Graft extends Variant {
        record Pending(Expr expr) implements Graft {
        }

        record Evaluated(CodeValue value) implements Graft {
        }
    }

    public record If(TList<ConditionBranch> conditions, Opt<Expr> else_) implements Variant {
    }

    public record Import(Expr spec) implements Variant {
    }

    public record Literal(LitValue value) implements Variant {
    }

    public record Lower(Expr expr) implements Variant {
    }

    public record Match(
            Expr base, TList<MatchBranch> cases, Expr elementKey, Opt<Expr> fallback)
            implements Variant {
    }

    public record MatchBranch(Expr condition, Expr body) {
    }

    public record OperatorAdd(Expr base, Expr reference) implements Variant {
    }

    public record OperatorEq(Expr base, Expr reference) implements Variant {
    }

    public record OperatorLt(Expr base, Expr reference) implements Variant {
    }

    public record Record(TList<RecordEntry> entries) implements Variant {
    }

    public record RecordEntry(Expr key, Expr value) {
    }

    public record ScopeUse(Expr key) implements Variant {
    }

    public record Seq(TList<Expr> exprs) implements Variant {
    }

    public record Set(Expr base, Expr value) implements Variant {
    }

    public record Stage(Expr expr) implements Variant {
    }

    public record Try(Expr body, Opt<TryCatch> catchClause, Opt<Expr> finallyBlock)
            implements Variant {
    }

    public record TryCatch(Expr bindingName, Expr exceptionType, Expr body) {
    }

    public record Tuple(TList<Expr> elements) implements Variant {
    }

    public static final class LitValue {
        private final boolean once;
        private @Nullable Value value;

        private LitValue(Value value, boolean once) {
            this.value = value;
            this.once = once;
        }

        public static LitValue shared(Value v) {
            return new LitValue(v, false);
        }

        public static LitValue once(Value v) {
            return new LitValue(v, true);
        }

        public Value consume() {
            if (!once) {
                return value;
            }
            var v = value;
            if (v == null) {
                throw new IllegalStateException("Once Literal already consumed");
            }
            value = null;
            return v;
        }

        public Opt<Value> peek() {
            return value == null ? Opt.none() : Opt.some(value);
        }
    }

    public sealed interface Module {
        record V1(Expr expr) implements Module {
        }
    }
}
