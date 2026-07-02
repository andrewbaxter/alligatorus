package compilerxyz.eval;

import compilerxyz.eval.value.expr.TypeExpr;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

/**
 * Port of {@code eval::expr::Expr} ({@code source/src/eval/expr.rs:13}). Carries a
 * {@link QExprId} location plus a tagged {@link Variant}. Variants and their helper records are
 * kept nested to mirror the Rust enum layout one-to-one, matching the convention established by
 * {@link compilerxyz.syntax.SourceExpr}.
 *
 * <p>The Rust struct stores its variants behind {@code Rc<Expr>} pointers. In Java we hold
 * direct {@link Expr} references — they are immutable records and shared by identity, so an
 * extra reference-counted wrapper buys nothing.
 */
public record Expr(QExprId loc, Variant variant) implements Value {

  /**
   * Port of {@code impl Push for expr::Expr::dynamic_type} ({@code eval/push.rs:281}). Returns
   * {@link TypeExpr}.
   */
  public static Type dynamicType() {
    return new TypeExpr();
  }

  /**
   * Port of {@code impl Value for Expr} ({@code source/src/eval/value/type_expr/mod.rs:343}). An
   * {@link Expr} carried as a compile-time value has type {@link TypeExpr}; aspect lookup on the
   * value routes through {@link TypeExpr} via the usual {@link Type#aspect} path.
   */
  @Override
  public Type valueTypeOf() {
    return new TypeExpr();
  }

  public sealed interface Variant {}

  public record As(Expr base, Expr target) implements Variant {}

  public record Bind(Expr base, Expr value) implements Variant {}

  public record Call(Expr func, Expr arg) implements Variant {}

  public record ConditionBranch(Expr condition, Expr body) {}

  public record Field(Expr base, Expr key) implements Variant {}

  public record Foreach(Expr base, Expr elementKey, Expr body) implements Variant {}

  /**
   * Port of {@code eval::expr::ExprGraft}. Either the unresolved sub-expression (Pending) or the
   * {@link CodeValue} that staging produced (Evaluated). Stage processing mutates the surrounding
   * {@link Expr.Variant.Graft} by replacing it; the variant itself is immutable so the mutation
   * happens by allocating a new {@link Expr} with the Evaluated form.
   */
  public sealed interface Graft extends Variant {
    record Pending(Expr expr) implements Graft {}

    record Evaluated(CodeValue value) implements Graft {}
  }

  public record If(TList<ConditionBranch> conditions, Opt<Expr> else_) implements Variant {}

  public record Import(Expr spec) implements Variant {}

  public record Literal(LitValue value) implements Variant {}

  public record Lower(Expr expr) implements Variant {}

  public record Match(
      Expr base, TList<MatchBranch> cases, Expr elementKey, Opt<Expr> fallback)
      implements Variant {}

  public record MatchBranch(Expr condition, Expr body) {}

  public record OperatorAdd(Expr base, Expr reference) implements Variant {}

  public record OperatorEq(Expr base, Expr reference) implements Variant {}

  public record OperatorLt(Expr base, Expr reference) implements Variant {}

  public record Record(TList<RecordEntry> entries) implements Variant {}

  public record RecordEntry(Expr key, Expr value) {}

  public record ScopeUse(Expr key) implements Variant {}

  public record Seq(TList<Expr> exprs) implements Variant {}

  public record Set(Expr base, Expr value) implements Variant {}

  public record Stage(Expr expr) implements Variant {}

  public record Try(Expr body, Opt<TryCatch> catchClause, Opt<Expr> finallyBlock)
      implements Variant {}

  public record TryCatch(Expr bindingName, Expr exceptionType, Expr body) {}

  public record Tuple(TList<Expr> elements) implements Variant {}

  /**
   * Port of {@code eval::expr::LitValue} ({@code source/src/eval/expr.rs:181}). A literal holds
   * either a sharable {@link Value} (re-readable every time) or a once-only cell that hands its
   * value out exactly once. The latter exists for values that must not be cloned (move-once
   * semantics in the staged-eval pipeline).
   */
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

    /**
     * Take this literal's value. For shared literals returns the value every time; for once
     * literals returns the value the first call and panics on subsequent calls (mirrors the Rust
     * {@code expect("Once Literal already consumed")}).
     */
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

    /** Non-destructive peek — returns the value if still present, otherwise {@link Opt#none()}. */
    public Opt<Value> peek() {
      return value == null ? Opt.none() : Opt.some(value);
    }
  }

  /**
   * Port of {@code eval::expr::Module} ({@code source/src/eval/expr.rs:210}). Currently a single
   * variant — versioned so older module artifacts can be discriminated when more versions land.
   */
  public sealed interface Module {
    record V1(Expr expr) implements Module {}
  }
}
