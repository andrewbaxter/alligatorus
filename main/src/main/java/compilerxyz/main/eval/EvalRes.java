package compilerxyz.main.eval;

public record EvalRes(CodeValue cv, Scope scope) {
    public static EvalRes of(CodeValue cv, Scope scope) {
        return new EvalRes(cv, scope);
    }
}
