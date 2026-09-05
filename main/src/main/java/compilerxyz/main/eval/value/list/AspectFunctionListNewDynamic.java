package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectFunctionListNewDynamic(TypeListNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeListNewDynamic::call: argument has no compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var argType = argValue.valueType();
        var dynamicElements = new TList<CodeDynamic>();
        var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (argTupleOpt.optIsSome()) {
            var elements = argTupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
            for (var elemRes : elements) {
                if (elemRes.hasErrors()) return elemRes.reerr();
                var elemCv = elemRes.requireValue("takeElements");
                var convertedRes = Eval.typecheckConvert(xx, loc, type.elementType(), elemCv);
                if (convertedRes.hasErrors()) return convertedRes.reerr();
                var consumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
                if (consumeRes.hasErrors()) return consumeRes.reerr();
                var dynRes = consumeRes
                        .requireValue("AspectConsumeDynamic.get")
                        .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                dynamicElements.add(dynRes.requireValue("toDynamic"));
            }
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, type.elementType(), argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var dynRes = consumeRes
                    .requireValue("AspectConsumeDynamic.get")
                    .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
            if (dynRes.hasErrors()) return dynRes.reerr();
            dynamicElements.add(dynRes.requireValue("toDynamic"));
        }
        Type instanceType = new TypeListBoxedDynamic(type.elementType());
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
        if (retStackRes.hasErrors()) {
            return retStackRes.reerr();
        }
        var tlistIntname = JavaInternalName.fromClass(TList.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var addDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));
        var seq = new JavaBytecodeSequence();
        var selfDynCast = CodeDynamic.castOpt(loc, self.code());
        if (selfDynCast.hasErrors()) return selfDynCast.reerr();
        var selfDyn = selfDynCast.requireValue("selfDyn");
        if (selfDyn.optIsSome()) seq.add(selfDyn.optUnwrap());
        seq.add(JavaBytecodeUtils.instantiate(
                loc,
                tlistIntname,
                JavaMethodDescriptor.fromConstructorParts(TList.of()),
                new CodeDynamic(loc, new JavaBytecodeSequence())));
        for (var element : dynamicElements) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            seq.add(element);
            seq.add(JavaBytecodeUtils.callMethod(loc, tlistIntname, "add", addDesc));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        }
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
