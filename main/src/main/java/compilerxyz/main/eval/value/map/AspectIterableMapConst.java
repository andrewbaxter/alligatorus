package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;

public record AspectIterableMapConst<K extends Type, V extends Type>(
        TypeMapConst<K, V> type, TypeMapDynamic<Type, Type> dynamic) implements AspectIterable {

    private AspectIterableMapDynamic<Type, Type> dynamicIterable() {
        return new AspectIterableMapDynamic<>(dynamic);
    }

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return dynamicIterable().aspectIterableElementAccess(xx, loc, loop);
    }

    @Override
    public Type aspectIterableElementType() {
        return dynamicIterable().aspectIterableElementType();
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var dynRes = new AspectConsumeDynamicMapConst<>(type).aspectConsumeDynamicToDynamic(xx, loc, base);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return dynamicIterable()
                .aspectIterableForeach(
                        xx, loc, dynRes.requireValue("TypeMapConst::foreach self"), loop, elementBind, body);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return dynamicIterable().aspectIterableLoopBinding(xx);
    }
}
