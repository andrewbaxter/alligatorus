package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6119L)
public record TypeListNewDynamic(Type elementType) implements Type, AspectFunction, AspectInitialBind {

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ").new";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_NEW_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

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
                var convertedRes = Eval.typecheckConvert(xx, loc, elementType, elemCv);
                if (convertedRes.hasErrors()) return convertedRes.reerr();
                var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
                if (consumeRes.hasErrors()) return consumeRes.reerr();
                var dynRes = consumeRes
                        .requireValue("AspectConsumeDynamic.get")
                        .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                dynamicElements.add(dynRes.requireValue("toDynamic"));
            }
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, elementType, argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var dynRes = consumeRes
                    .requireValue("AspectConsumeDynamic.get")
                    .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
            if (dynRes.hasErrors()) return dynRes.reerr();
            dynamicElements.add(dynRes.requireValue("toDynamic"));
        }
        Type instanceType = new TypeListBoxedDynamic(elementType);
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
        if (retStackRes.hasErrors()) {
            return retStackRes.reerr();
        }
        var tlistIntname = JavaInternalName.fromClass(TList.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var addDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));
        var seq = new JavaBytecodeSequence();
        var selfDyn = self.code().codeDynamic();
        if (selfDyn != null) seq.add(selfDyn);
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

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_LIST_NEW_DYNAMIC_IDENTITY, TypeListNewDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
