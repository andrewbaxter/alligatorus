package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.gen.AspectConsumeDynamic;
import java.util.Map;

public final class AspectConsumeDynamicMapPutFnDynamic implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicMapPutFnDynamic INSTANCE = new AspectConsumeDynamicMapPutFnDynamic();

    private AspectConsumeDynamicMapPutFnDynamic() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Map.class));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynRes = CodeDynamic.castRequired(loc, cv.code(), "TypeMapPutFnDynamic::toDynamic: no dynamic code");
        if (dynRes.hasErrors()) return dynRes.reerr();
        var dyn = dynRes.requireValue("dyn");
        return EResult.ok(dyn);
    }
}
