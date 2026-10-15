package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenLists;

public record AspectIterableListConst<T extends Type>(TypeListConst<T> type) implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var iterRes =
                GenAspectIterable.get(xx, loc, GenLists.listDynamic(xx, TypeForms.dynamicForm(xx, type.elementType)));
        if (iterRes.hasErrors()) return iterRes.reerr();
        return iterRes.requireValue("TypeListConst::element access AspectIterable")
                .aspectIterableElementAccess(xx, loc, loop);
    }

    @Override
    public Type aspectIterableElementType() {
        return type.elementType;
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var dynRes = type.dynamicCv(xx, loc, base);
        if (dynRes.hasErrors()) return dynRes.reerr();
        var iterRes =
                GenAspectIterable.get(xx, loc, GenLists.listDynamic(xx, TypeForms.dynamicForm(xx, type.elementType)));
        if (iterRes.hasErrors()) return iterRes.reerr();
        return iterRes.requireValue("TypeListConst::foreach AspectIterable")
                .aspectIterableForeach(
                        xx, loc, dynRes.requireValue("TypeListConst::foreach self"), loop, elementBind, body);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return GenAspectIterable.getMaybe(xx, GenLists.listDynamic(xx, TypeForms.dynamicForm(xx, type.elementType)))
                .optUnwrap()
                .aspectIterableLoopBinding(xx);
    }
}
