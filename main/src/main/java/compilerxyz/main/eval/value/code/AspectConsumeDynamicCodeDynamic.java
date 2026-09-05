package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
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
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicCodeDynamic(TypeCodeDynamic type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(Code.class);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (DynUtil.dynamicHasCode(cv)) {
            return DynUtil.dynamicConsumeToDynamic(loc, cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: CodeValue has no value");
        }
        if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc) || !(voc.data() instanceof Code codeObj)) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: value is not a compile-time Code");
        }
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, type, new External(Code.class, codeObj)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }
}
