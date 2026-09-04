package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class TypeMethodShared {
    private TypeMethodShared() {}

    public static EResult<CodeValue> callDynamic(
            ExprContext xx,
            QExprId loc,
            CodeValue self,
            CodeValue argument,
            String what,
            Type argType,
            Opt<Type> ret,
            JavaInternalName klass,
            String methodName,
            JavaMethodDescriptor descriptor,
            boolean isInterface,
            boolean needsEvalContext) {
        var receiverDynRes = CodeDynamic.castRequired(loc, self.code(), what + "::call: receiver has no dynamic code");
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        var receiverDyn = receiverDynRes.requireValue("receiverDyn");
        EResult<TList<CodeDynamic>> argsDynRes;
        if (DynUtil.zeroArgSignature(xx, argType)) {
            argsDynRes = EResult.ok(new TList<>());
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var converted = convertedRes.requireValue(what + "::call converted arg");
            argsDynRes = DynUtil.destructurePositional(xx, loc, argType, converted);
            if (argsDynRes.hasErrors()) return argsDynRes.reerr();
        }
        var seq = new JavaBytecodeSequence();
        seq.add(receiverDyn);
        if (needsEvalContext) {
            seq.add(EvalRootEmitter.loadEvalContext(loc, xx));
        }
        var args = argsDynRes.requireValue(what + "::call args");
        var paramDescs = new TList<JavaDataDescriptor>();
        {
            var _pd = descriptor.value();
            if (_pd.isEmpty() || _pd.charAt(0) != '(') {
                throw new IllegalStateException("JavaMethodDescriptor missing '(': " + _pd);
            }
            int _pdi = 1;
            while (_pdi < _pd.length() && _pd.charAt(_pdi) != ')') {
                int _pdStart = _pdi;
                while (_pdi < _pd.length() && _pd.charAt(_pdi) == '[') _pdi++;
                char _pdc = _pd.charAt(_pdi);
                if (_pdc == 'L') {
                    while (_pdi < _pd.length() && _pd.charAt(_pdi) != ';') _pdi++;
                    _pdi++;
                } else {
                    _pdi++;
                }
                paramDescs.add(JavaDataDescriptor.create(_pd.substring(_pdStart, _pdi)));
            }
        }
        var paramOffset = needsEvalContext ? 1 : 0;
        for (int i = 0; i < args.size(); i++) {
            seq.add(args.get(i));
            var pi = i + paramOffset;
            if (pi < paramDescs.size()) {
                var expected = paramDescs.get(pi);
                if (expected.value().startsWith("L") && !expected.value().equals("Ljava/lang/Object;")) {
                    seq.add(JavaBytecodeUtils.cast(loc, expected));
                }
            }
        }
        if (isInterface) {
            seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, klass, methodName, descriptor));
        } else {
            seq.add(JavaBytecodeUtils.callMethod(loc, klass, methodName, descriptor));
        }
        var seqCode = new CodeDynamic(loc, seq);
        if (ret.optIsNone()) {
            return EResult.ok(new CodeValue(Code.target(seqCode), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret.optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(
                retStackRes.requireValue(what + "::call ret stack").aspectStackValueDynamicToValue(xx, loc, seqCode));
    }

    public static EResult<CodeDynamic> toDynamic(QExprId loc, CodeValue cv, String what) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsSome())
            return EResult.ok(dynCast.requireValue("dyn").optUnwrap());
        return EResult.err(loc, what + "::toDynamic: CodeValue has no dynamic code");
    }
}
