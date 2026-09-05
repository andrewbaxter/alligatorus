package compilerxyz.main.eval.value.expr;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicExpr(TypeExpr type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.fromClass(compilerxyz.main.eval.expr.StagedExprCell.class));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (DynUtil.dynamicHasCode(cv)) {
            return DynUtil.dynamicConsumeToDynamic(loc, cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeExpr::toDynamic: CodeValue has no value to stage");
        }
        var cell = new StagedExprCell(cv.value().optUnwrap());
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, type, new External(StagedExprCell.class, cell)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }
}
