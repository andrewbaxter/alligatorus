package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public record AspectFieldTypeSourceInterfaceDynamic(TypeTypeSourceInterfaceDynamic type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var iface = type.require("aspectFieldField");
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeTypeSourceInterfaceDynamic::field: key has no compile-time value");
        }
        var ckRes = Eval.extractConstKey(xx, key.value().optUnwrap());
        ConstKey ck;
        switch (ckRes) {
            case Result.Ok<ConstKey, String> ok -> ck = ok.value();
            case Result.Err<ConstKey, String> e -> {
                return EResult.err(loc, e.error());
            }
        }
        ConstKey.Str keyStr;
        switch (ck) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(loc, "Method " + ck.constKeyToCanonical() + " not found on interface");
            }
        }
        var keyName = keyStr.value();
        InterfaceMethodData match = null;
        for (var m : iface.cell().frozen().methods) {
            if (m.name.equals(keyName)) {
                match = m;
                break;
            }
        }
        if (match == null) {
            return EResult.err(loc, "Method '" + keyName + "' not found on interface");
        }
        var refType = new TypeInterfaceMethodDynamic(iface.cell(), match);
        Code carry = self.code();
        return EResult.ok(new CodeValue(carry, EvalOutcome.ofValue(new ValueObjectConst(refType, refType))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (type.type().optIsNone()) {
            return Opt.none();
        }
        ConstKey.Str s;
        switch (key) {
            case ConstKey.Str ks -> s = ks;
            case ConstKey.Uint u -> {
                return Opt.none();
            }
        }
        for (var m : type.type().optUnwrap().cell().frozen().methods) {
            if (m.name.equals(s.value())) {
                return Opt.some(
                        new TypeInterfaceMethodDynamic(type.type().optUnwrap().cell(), m));
            }
        }
        return Opt.none();
    }
}
