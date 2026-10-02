package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectFunctionFunctionReinterpretConst(TypeFunctionReinterpretConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var elementsRes = ReinterpretArgs.elements(xx, loc, "reinterpret", "(loc, value, type)", argument);
        if (elementsRes.hasErrors()) return elementsRes.reerr();
        var elements = elementsRes.requireValue("reinterpret elements");
        var errLocRes = DynUtil.materialize(xx, loc, GenQExprId.dynamicType(), elements.get(0));
        if (errLocRes.hasErrors()) return errLocRes.reerr();
        var argsRes = ReinterpretArgs.parse(xx, loc, "reinterpret", elements.get(1), elements.get(2));
        if (argsRes.hasErrors()) return argsRes.reerr();
        var args = argsRes.requireValue("reinterpret args");
        var eresultDesc = JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
        var b = new JavaBytecodeBuilder(loc);
        var argumentAdd = b.addCode(loc, argument.code());
        if (argumentAdd.optIsSome()) return EResult.errs(argumentAdd.optUnwrap());
        var errLoc = b.reg(errLocRes.requireValue("reinterpret loc"));
        var value = b.reg(args.value());
        var isTarget = b.instanceOf(args.targetJvmClass().internalName(), value);
        var mismatchKey = new JumpKey();
        var doneKey = new JumpKey();
        b.branch(Opcodes.IFEQ, mismatchKey, isTarget);
        var ok = b.invokeStatic(
                JavaInternalName.fromClass(EResult.class),
                "ok",
                JavaMethodDescriptor.fromParts(eresultDesc, TList.of(JvmConstants.DESC_OBJECT)),
                b.checkcast(args.targetJvmClass(), value));
        b.jump(doneKey, ok);
        b.land(mismatchKey);
        var err = b.invokeStatic(
                JavaInternalName.fromClass(EResult.class),
                "err",
                JavaMethodDescriptor.fromParts(
                        eresultDesc,
                        TList.of(
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(QExprId.class)),
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(String.class)))),
                errLoc,
                b.constString("reinterpret: value is not a " + args.targetType().typeDesc()));
        b.jump(doneKey, err);
        var out = new JavaBytecodeReg(eresultDesc);
        b.land(doneKey, out);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeEResultDynamic<>(args.targetType())));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
