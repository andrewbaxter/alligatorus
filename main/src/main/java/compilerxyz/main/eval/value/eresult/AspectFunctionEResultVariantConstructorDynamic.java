package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.eresult.TypeEResultVariantConstructorDynamic.Kind;
import compilerxyz.main.eval.value.list.TypeListDynamic;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionEResultVariantConstructorDynamic(TypeEResultVariantConstructorDynamic type)
        implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var erb = new Erb(loc);
        Type resultType = new TypeEResultDynamic(type.elementType());
        if (type.kind() == Kind.OK) {
            var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, type.elementType(), argument));
            if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
            var innerRes = DynUtil.materialize(xx, loc, convertedOpt.optUnwrap());
            if (innerRes.hasErrors()) return innerRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeStatic(
                    JavaInternalName.fromClass(EResult.class),
                    "ok",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                            TList.of(JvmConstants.DESC_OBJECT)),
                    b.reg(innerRes.requireValue("eresult ok value")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
        }
        if (type.kind() == Kind.ERRS) {
            var errsArgType = TypeListDynamic.ofObject(GenExprErr.dynamicType());
            var convertedErrsOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, errsArgType, argument));
            if (convertedErrsOpt.optIsNone()) return EResult.errs(erb.errors());
            var listRes = DynUtil.materialize(xx, loc, convertedErrsOpt.optUnwrap());
            if (listRes.hasErrors()) return listRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeStatic(
                    JavaInternalName.fromClass(EResult.class),
                    "errs",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                            TList.of(JavaDataDescriptor.fromInternalName(
                                    JavaInternalName.fromClass(compilerxyz.prelude.util.TList.class)))),
                    b.reg(listRes.requireValue("eresult errs list")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
        }
        var argTupleType = new Tuple(IList.of(
                new TupleElement.Dynamic(GenQExprId.dynamicType()), new TupleElement.Dynamic(new TypeStringDynamic())));
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, argTupleType, argument));
        if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
        var slotsRes = DynUtil.destructurePositional(xx, loc, argTupleType, convertedOpt.optUnwrap());
        if (slotsRes.hasErrors()) return slotsRes.reerr();
        var positional = slotsRes.requireValue("TypeEResultVariantConstructorDynamic::call destructure");
        var slots = positional.args();
        if (slots.size() != 2) {
            throw new AssertionError(
                    "TypeEResultVariantConstructorDynamic::call err: expected 2 slots, got " + slots.size());
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryAdd = b.addCode(loc, positional.carry());
        if (carryAdd.optIsSome()) return EResult.errs(carryAdd.optUnwrap());
        var locReg = b.reg(slots.get(0));
        var msgReg = b.reg(slots.get(1));
        var out = b.invokeStatic(
                JavaInternalName.fromClass(EResult.class),
                "err",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                        TList.of(
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(QExprId.class)),
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(String.class)))),
                locReg,
                msgReg);
        return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
