package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;

public record AspectIterableArrayConst<T extends Type>(TypeArrayConst<T> type) implements AspectIterable {

    private AspectIterableArrayDynamic<T> dynamicIterable() {
        return new AspectIterableArrayDynamic<>(new TypeArrayDynamic<>(type.elementType));
    }

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return dynamicIterable().aspectIterableElementAccess(xx, loc, loop);
    }

    @Override
    public Type aspectIterableElementType() {
        return type.elementType;
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var dynRes = new AspectConsumeDynamicArrayConst<>(type).aspectConsumeDynamicToDynamic(xx, loc, base);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return dynamicIterable()
                .aspectIterableForeach(
                        xx, loc, dynRes.requireValue("TypeArrayConst::foreach self"), loop, elementBind, body);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return dynamicIterable().aspectIterableLoopBinding(xx);
    }
}
