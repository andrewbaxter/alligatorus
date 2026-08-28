package compilerxyz.eval.value.function;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

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
            boolean needsExprContext) {
        var receiverDyn = self.code().codeDynamic();
        if (receiverDyn == null) {
            return EResult.err(loc, what + "::call: receiver has no dynamic code");
        }
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
        if (needsExprContext) {
            seq.add(JavaBytecodeUtils.accessStaticField(
                    loc,
                    JavaInternalName.of(xx.vx.evalRootInternalName),
                    EvalRootEmitter.EXPR_CONTEXT_FIELD,
                    EvalRootEmitter.DESC_EXPR_CONTEXT));
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
        var paramOffset = needsExprContext ? 1 : 0;
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
            return EResult.ok(new CodeValue(Code.of(loc, seqCode), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret.optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(
                retStackRes.requireValue(what + "::call ret stack").aspectStackValueDynamicToValue(xx, loc, seqCode));
    }

    public static EResult<CodeDynamic> toDynamic(QExprId loc, CodeValue cv, String what) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        return EResult.err(loc, what + "::toDynamic: CodeValue has no dynamic code");
    }
}
