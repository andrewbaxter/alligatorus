package compilerxyz.main.eval.value.expr;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
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
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeExpr::toDynamic: CodeValue has no value to stage");
        }
        var value = cv.value().optUnwrap();
        if (DynUtil.sourceOf(value).optIsSome()) {
            return EResult.ok(cv);
        }
        var cell = new StagedExprCell(cv.value().optUnwrap());
        var entry = new EvalContext.ExternalEntry(new BindingId(), type, new External(StagedExprCell.class, cell));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
