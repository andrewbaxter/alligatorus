package compilerxyz.main.eval.value.expr;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;

public final class AspectLiftStagedExprCell implements AspectLift {
    public static final AspectLiftStagedExprCell INSTANCE = new AspectLiftStagedExprCell();

    private AspectLiftStagedExprCell() {}

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {

        if (!(externalHostValue instanceof StagedExprCell cell)) {
            return EResult.err(
                    loc,
                    "AspectLiftStagedExprCell::lift: expected StagedExprCell, got "
                            + externalHostValue.getClass().getName());
        }
        var inner = cell.value();
        if (!(inner instanceof Value value)) {
            return EResult.err(
                    loc,
                    "AspectLiftStagedExprCell::lift: StagedExprCell contained non-Value: "
                            + inner.getClass().getName());
        }
        return EResult.ok(value);
    }
}
