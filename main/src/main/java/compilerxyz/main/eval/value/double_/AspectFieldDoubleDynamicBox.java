package compilerxyz.main.eval.value.double_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldDoubleDynamicBox implements AspectField {
    public static final AspectFieldDoubleDynamicBox INSTANCE = new AspectFieldDoubleDynamicBox();

    private AspectFieldDoubleDynamicBox() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var unboxed = new TypeDoubleDynamic();
        var selfRes = DynUtil.valueReg(loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var raw = Numerics.unbox(b, NumericKind.DOUBLE, b.reg(selfRes.requireValue("selfDyn")));
        var unboxedSelf = DynUtil.regToValue(b.code(), raw, unboxed);
        return GenAspectField.getMaybe(xx, unboxed).optUnwrap().aspectFieldField(xx, loc, unboxedSelf, key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, new TypeDoubleDynamic()).optUnwrap().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, new TypeDoubleDynamic()).optUnwrap().aspectFieldTypeOpt(xx, key);
    }
}
