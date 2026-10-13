package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenLists;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicListConst<T extends Type>(TypeListConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return switch (GenLists.listDynamic(xx, type.elementType)) {
            case TypeListSpecializedDynamic<?> spec ->
                GenAspectConsumeDynamic.getMaybe(xx, spec).optUnwrap().aspectConsumeDynamicJvmClass(xx);
            case TypeListBoxedDynamic<?> boxed ->
                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class));
        };
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return ConstHost.loadDynamic(
                loc, cv, aspectConsumeDynamicJvmClass(xx), GenLists.listDynamic(xx, type.elementType));
    }
}
