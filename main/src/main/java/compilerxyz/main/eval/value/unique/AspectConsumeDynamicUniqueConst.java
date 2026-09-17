package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.MustCast;

public record AspectConsumeDynamicUniqueConst(TypeUniqueConst type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(Unique.class);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && DynUtil.sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeUniqueConst::toDynamic: no compile-time value");
        }
        Object hostValue = cv.value().optUnwrap();
        if (hostValue instanceof ValueObjectConst wrap) {
            hostValue = wrap.data();
        }
        var u = MustCast.mustCast(Unique.class, hostValue);
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, type, new External(Unique.class, u)));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.hasErrors()) return carryRes.reerr();
        var loaded = b.load(id, JavaDataDescriptor.fromClass(Unique.class));
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
