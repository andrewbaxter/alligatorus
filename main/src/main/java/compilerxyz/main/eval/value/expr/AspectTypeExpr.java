package compilerxyz.main.eval.value.expr;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public final class AspectTypeExpr implements AspectType {
    public static final AspectTypeExpr INSTANCE = new AspectTypeExpr();

    private AspectTypeExpr() {}

    @Override
    public Type aspectTypeType(Value value) {
        return new TypeExpr();
    }
}
