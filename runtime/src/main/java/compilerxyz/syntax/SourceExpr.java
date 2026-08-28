package compilerxyz.syntax;

import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record SourceExpr(ExprId id, Variant variant) implements SafeJsonSerializable {

    public sealed interface Variant {}

    public record As(SourceExpr base, SourceExpr target) implements Variant {}

    public record Bind(String name, SourceExpr value) implements Variant {}

    public record Call(SourceExpr func, SourceExpr arg) implements Variant {}

    public record Field(SourceExpr base, SourceExpr key) implements Variant {}

    public record Foreach(SourceExpr base, SourceExpr elementKey, SourceExpr body) implements Variant {}

    public record Graft(SourceExpr expr) implements Variant {}

    public record If(TList<IfBranch> conditions, Opt<SourceExpr> fallback) implements Variant {}

    public record IfBranch(SourceExpr condition, SourceExpr body) {}

    public record Labeled(String label, SourceExpr inner) implements Variant {}

    public record Literal(SourceLiteral value) implements Variant {}

    public record Lower(SourceExpr expr) implements Variant {}

    public record Match(SourceExpr base, SourceExpr elementKey, TList<MatchBranch> cases, Opt<SourceExpr> fallback)
            implements Variant {}

    public record MatchBranch(SourceExpr condition, SourceExpr body) {}

    public record OperatorBinary(BinaryOperator op, SourceExpr base, SourceExpr reference) implements Variant {}

    public record OperatorUnary(UnaryOperator op, SourceExpr base) implements Variant {}

    public record Record(TList<RecordEntry> entries) implements Variant {}

    public record RecordEntry(SourceExpr key, SourceExpr value) {}

    public record Return(Opt<String> label, Opt<SourceExpr> value) implements Variant {}

    public record ScopeUse(SourceExpr key) implements Variant {}

    public record Seq(TList<SourceExpr> exprs) implements Variant {}

    public record Set(SourceExpr base, SourceExpr value) implements Variant {}

    public record Stage(SourceExpr expr) implements Variant {}

    public record Try(SourceExpr body, Opt<TryCatch> catchClause, Opt<SourceExpr> finallyBlock) implements Variant {}

    public record TryCatch(SourceExpr bindingName, SourceExpr exceptionType, SourceExpr body) {}

    public record Tuple(TList<SourceExpr> exprs) implements Variant {}
}
