package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldObjectDynamic(TypeObjectDynamic type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, type.typeDesc() + "::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        ConstKey.Str ckStr;
        switch (ck) {
            case ConstKey.Str s -> ckStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(loc, type.typeDesc() + ": unknown field " + ck.constKeyToCanonical());
            }
        }
        var keyStr = ckStr.value();
        var dynRes = DynUtil.valueReg(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        for (var f : type.dataFields()) {
            if (f.langName().equals(keyStr)) {
                var b = new JavaBytecodeBuilder(loc);
                var selfReg = b.reg(dynRes.requireValue("dyn"));
                var out =
                        switch (f.access()) {
                            case ObjectDataField.Access.FieldGet fg ->
                                b.getField(type.jvmClass, fg.jvmFieldName(), fg.jvmFieldDesc(), selfReg);
                            case ObjectDataField.Access.MethodCall mc ->
                                b.invokeVirtual(type.jvmClass, mc.jvmMethodName(), mc.jvmMethodDesc(), selfReg);
                        };
                return EResult.ok(DynUtil.regToValue(b.code(), out, f.fieldType()));
            }
        }
        if (type.interfaceRepresented.optIsNone()) {
            for (var m : type.methods()) {
                if (m.langName().equals(keyStr)) {
                    return EResult.ok(DynUtil.methodRefOn(self, m.funcType()));
                }
            }
        }
        return EResult.err(loc, type.typeDesc() + ": unknown field '" + keyStr);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        ConstKey.Str keyStr;
        switch (key) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return Opt.none();
            }
        }
        var name = keyStr.value();
        for (var f : type.dataFields()) {
            if (f.langName().equals(name)) return Opt.some(f.fieldType());
        }
        if (type.interfaceRepresented.optIsNone()) {
            for (var m : type.methods()) {
                if (m.langName().equals(name)) return Opt.some(m.funcType());
            }
        }
        return Opt.none();
    }
}
