package compilerxyz.eval.dynamic;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.Eval;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;

public final class DynUtil {
    private DynUtil() {
    }

    public static JavaBytecode.LoweredSequence buildCallSequence(
            ExprContext xx, CodeDynamic funcCode, TList<CodeDynamic> args) {
        var elements = new TList<JavaBytecode.LoweredSequence.Element>();
        boolean staticDispatch = false;
        String staticClassName = null;
        String staticMethodName = null;
        if (funcCode.code() instanceof JavaBytecode.BindingUse bu
                && bu.id() instanceof BindingId.Def def
                && xx.vx.ix.staticFunctionDefs.contains(def)) {
            staticDispatch = true;
            staticClassName = DefSetFunctionsEmitter.functionsInternalName(def.defSet());
            staticMethodName = DefSetFunctionsEmitter.functionMethodName(def.index());
        }
        if (staticDispatch) {
            elements.add(new JavaBytecode.LoweredSequence.LoadExprContext());
            appendArgs(elements, args);
            elements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            JavaBytecodeUtils.callStaticMethod(
                                    -1,
                                    JavaInternalName.of(staticClassName),
                                    staticMethodName,
                                    JavaMethodDescriptor.fromParts(
                                            JvmConstants.DESC_OBJECT,
                                            JavaDataDescriptor.fromInternalName(
                                                    JavaInternalName.of("compilerxyz/eval/ExprContext")),
                                            JvmConstants.DESC_OBJECT))));
        } else {
            elements.add(new JavaBytecode.LoweredSequence.Code(funcCode));
            var contextualFunction = JavaInternalName.of("compilerxyz/eval/ContextualFunction");
            elements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(contextualFunction))));
            elements.add(new JavaBytecode.LoweredSequence.LoadExprContext());
            appendArgs(elements, args);
            elements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            JavaBytecodeUtils.callInterfaceMethod(
                                    -1,
                                    contextualFunction,
                                    "contextualFunctionApplyCtx",
                                    JavaMethodDescriptor.fromParts(
                                            JvmConstants.DESC_OBJECT,
                                            JavaDataDescriptor.fromInternalName(
                                                    JavaInternalName.of("compilerxyz/eval/ExprContext")),
                                            JvmConstants.DESC_OBJECT))));
        }
        return new JavaBytecode.LoweredSequence(elements);
    }

    private static void appendArgs(
            TList<JavaBytecode.LoweredSequence.Element> out, TList<CodeDynamic> args) {
        if (args.isEmpty()) {
            out.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            JavaBytecodeUtils.inst(org.objectweb.asm.Opcodes.ACONST_NULL)));
        } else if (args.size() == 1) {
            out.add(new JavaBytecode.LoweredSequence.Code(args.get(0)));
        } else {
            out.add(
                    new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalIntShortByte(args.size())));
            out.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            new JavaBytecodeInstruction.Obj(
                                    new org.objectweb.asm.tree.TypeInsnNode(
                                            org.objectweb.asm.Opcodes.ANEWARRAY,
                                            JvmConstants.INTNAME_OBJECT.value()))));
            for (int i = 0; i < args.size(); i++) {
                out.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.JBC_DUP));
                out.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalIntShortByte(i)));
                out.add(new JavaBytecode.LoweredSequence.Code(args.get(i)));
                out.add(
                        new JavaBytecode.LoweredSequence.Bytecode(
                                JavaBytecodeUtils.inst(org.objectweb.asm.Opcodes.AASTORE)));
            }
        }
    }

    public static JavaBytecode.LoweredSequence buildCallContextualMethodSequence(
            CodeDynamic baseCode, String methodKey, TList<CodeDynamic> args) {
        var elements = new TList<JavaBytecode.LoweredSequence.Element>();
        var contextualFunction = JavaInternalName.of("compilerxyz/eval/ContextualFunction");
        var dynRuntime = JavaInternalName.fromClass(DynRuntime.class);
        var fieldGetDesc =
                JavaMethodDescriptor.fromParts(
                        JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_STRING);
        var applyDesc =
                JavaMethodDescriptor.fromParts(
                        JvmConstants.DESC_OBJECT,
                        JavaDataDescriptor.fromInternalName(
                                JavaInternalName.of("compilerxyz/eval/ExprContext")),
                        JvmConstants.DESC_OBJECT);

        elements.add(new JavaBytecode.LoweredSequence.Code(baseCode));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.JBC_DUP));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalString(methodKey)));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(
                        JavaBytecodeUtils.callStaticMethod(-1, dynRuntime, "fieldGet", fieldGetDesc)));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(
                        JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(contextualFunction))));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.SWAP)));

        var totalCount = args.size() + 1;
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalIntShortByte(totalCount)));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(
                        new JavaBytecodeInstruction.Obj(
                                new org.objectweb.asm.tree.TypeInsnNode(
                                        Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value()))));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.DUP_X1)));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.SWAP)));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalIntShortByte(0)));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.SWAP)));
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.AASTORE)));
        for (int i = 0; i < args.size(); i++) {
            elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.JBC_DUP));
            elements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalIntShortByte(i + 1)));
            elements.add(new JavaBytecode.LoweredSequence.Code(args.get(i)));
            elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.AASTORE)));
        }

        elements.add(new JavaBytecode.LoweredSequence.LoadExprContext());
        elements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.SWAP)));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(
                        JavaBytecodeUtils.callInterfaceMethod(
                                -1, contextualFunction, "contextualFunctionApplyCtx", applyDesc)));

        return new JavaBytecode.LoweredSequence(elements);
    }

    public static JavaBytecode.LoweredSequence buildFieldGetSequence(CodeDynamic baseCode, String fieldKey) {
        var elements = new TList<JavaBytecode.LoweredSequence.Element>();
        elements.add(new JavaBytecode.LoweredSequence.Code(baseCode));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalString(fieldKey)));
        elements.add(
                new JavaBytecode.LoweredSequence.Bytecode(
                        JavaBytecodeUtils.callInterfaceMethod(
                                -1,
                                JvmConstants.INTNAME_MAP,
                                "get",
                                JavaMethodDescriptor.fromParts(
                                        JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT))));
        return new JavaBytecode.LoweredSequence(elements);
    }

    public static CodeDynamic renderSource(QExprId loc, ValueDynamic.Source src) {
        return switch (src) {
            case ValueDynamic.Source.Binding b ->
                new CodeDynamic(loc, new JavaBytecode.BindingUse(b.key()));
            case ValueDynamic.Source.Field f ->
                new CodeDynamic(loc, buildFieldGetSequence(renderSource(loc, f.base()), f.key()));
            case ValueDynamic.Source.MethodField mf ->
                new CodeDynamic(loc, buildFieldGetSequence(renderSource(loc, mf.base()), mf.key()));
            case ValueDynamic.Source.Expr e ->
                throw new IllegalStateException(
                    "ValueDynamic.Source.Expr has no reconstructable code");
        };
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (!(valueOpt instanceof Opt.Some<Value> some)) {
            throw new IllegalStateException(
                    "bind_via_target called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = some.value().valueTypeOf();
        if (cv.code().isNone()) {
            Binding binding = new BindingValue(some.value());
            return EResult.ok(new AspectBind.BindResult(cv.code(), binding));
        }
        var bindDynOpt = AspectBindDynamic.getMaybe(xx, vtype);
        if (!(bindDynOpt instanceof Opt.Some<AspectBindDynamic> bindDynSome)) {
            return EResult.err(loc, "Type (" + vtype.typeDesc() + ") is missing aspect AspectBindDynamic");
        }
        var subRes = bindDynSome.value().bindDynamic(xx, loc, cv);
        if (subRes.hasErrors()) {
            return subRes.reerr();
        }
        var subOpt = subRes.valueOrNull();
        if (subOpt == null) {
            throw new IllegalStateException("AspectBindDynamic.bindDynamic returned neither value nor errors");
        }
        if (!(subOpt instanceof Opt.Some<TargetBindSub> subSome)) {
            return EResult.err(
                    loc, "Type (" + vtype.typeDesc() + ") returned None from AspectBindDynamic");
        }
        var bound = xx.target.targetBind(loc, subSome.value());
        Code wrappingCode = Code.of(loc, bound.code());
        Binding binding = new BindingDynamicBinding(bound.id(), vtype);
        return EResult.ok(new AspectBind.BindResult(wrappingCode, binding));
    }

    public static EResult<Opt<TargetBindSub>> bindDynamicDefault(
            ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.code().isNone()) {
            return EResult.ok(Opt.none());
        }
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
            throw new IllegalStateException(
                    "bind_dynamic_default called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = some.value().valueTypeOf();
        var dynRes = dynamicConsumeToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.valueOrNull();
        if (dyn == null) {
            throw new IllegalStateException(
                    "dynamicConsumeToDynamic returned neither value nor errors");
        }
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    public static EResult<CodeDynamic> dynamicConsumeToDynamic(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().dynamic();
        if (dyn == null) {
            return EResult.err(loc, "Expression resulted in empty code");
        }
        return EResult.ok(dyn);
    }

    public static CodeValue dynamicStackToValue(QExprId loc, CodeDynamic value, Type type_) {
        var dynValue =
                new ValueDynamic(
                        new ValueDynamic.Source.Expr(), type_);
        return new CodeValue(Code.of(loc, value), Opt.some(dynValue));
    }

    public static CodeDynamic wrapWithCarry(QExprId loc, Code carry, CodeDynamic dynamicCode) {
        var carryDyn = carry.dynamic();
        if (carryDyn == null) {
            return dynamicCode;
        }
        var entries = new TList<JavaBytecode.BlockEntry>();
        if (carryDyn.code() instanceof JavaBytecode.Block block) {
            for (var e : block.entries()) {
                entries.add(e);
            }
        } else {
            entries.add(new JavaBytecode.BlockEntry.Code(carryDyn));
        }
        entries.add(new JavaBytecode.BlockEntry.Code(dynamicCode));
        return new CodeDynamic(loc, new JavaBytecode.Block(entries));
    }

    public static EResult<CodeValue> dynamicSet(
            ExprContext xx, QExprId loc, CodeValue self_, CodeValue value) {
        if (!(self_.value() instanceof Opt.Some<Value> selfSome)) {
            throw new IllegalStateException(
                    "dynamicSet called with absent value in self_; Rust unwraps unconditionally");
        }
        if (!(selfSome.value() instanceof ValueDynamic selfDyn)) {
            throw new IllegalStateException(
                    "dynamicSet: self_ value is not a ValueDynamic (got "
                            + selfSome.value().getClass().getName()
                            + ")");
        }
        Type targetType = selfDyn.type_();
        var convRes = Eval.typecheckConvert(xx, loc, targetType, value);
        if (convRes.hasErrors()) {
            return convRes.reerr();
        }
        var converted = convRes.valueOrNull();
        if (converted == null) {
            throw new IllegalStateException(
                    "dynamicSet: typecheckConvert returned neither value nor errors");
        }
        if (!(converted.value() instanceof Opt.Some<Value> convSome)) {
            return EResult.err(loc, "dynamicSet: converted value has no compile-time value");
        }
        Type valueType = convSome.value().valueTypeOf();
        var valueToDynRes = AspectConsumeDynamic.get(xx, loc, valueType);
        if (valueToDynRes.hasErrors()) {
            return valueToDynRes.reerr();
        }
        var valueToDyn = valueToDynRes.valueOrNull();
        if (valueToDyn == null) {
            throw new IllegalStateException(
                    "dynamicSet: AspectConsumeDynamic.get returned neither value nor errors");
        }
        var valueCodeRes = valueToDyn.toDynamic(xx, loc, converted);
        if (valueCodeRes.hasErrors()) {
            return valueCodeRes.reerr();
        }
        var valueCode = valueCodeRes.valueOrNull();
        if (valueCode == null) {
            throw new IllegalStateException(
                    "dynamicSet: AspectConsumeDynamic.toDynamic returned neither value nor errors");
        }
        var selfCode = self_.code().dynamic();
        var nullStackRes = AspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
        if (nullStackRes.hasErrors()) {
            return nullStackRes.reerr();
        }
        var nullStack = nullStackRes.valueOrNull();
        if (nullStack == null) {
            throw new IllegalStateException(
                    "dynamicSet: AspectStackValueDynamic.get returned neither value nor errors");
        }
        if (selfDyn.source() instanceof ValueDynamic.Source.Field fieldSrc) {
            if (selfCode == null) {
                return EResult.err(
                        loc,
                        "TODO assertion set left hand doesn't evaluate to dynamic code (empty)");
            }
            CodeDynamic baseCode = renderSource(loc, fieldSrc.base());
            var setElements = new TList<JavaBytecode.LoweredSequence.Element>();
            setElements.add(new JavaBytecode.LoweredSequence.Code(baseCode));
            setElements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.literalString(fieldSrc.key())));
            setElements.add(new JavaBytecode.LoweredSequence.Code(valueCode));
            setElements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(
                            JavaBytecodeUtils.callInterfaceMethod(
                                    -1,
                                    JavaInternalName.of("java/util/Map"),
                                    "put",
                                    JavaMethodDescriptor.fromParts(
                                            JvmConstants.DESC_OBJECT,
                                            JvmConstants.DESC_OBJECT,
                                            JvmConstants.DESC_OBJECT))));
            setElements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.POP)));
            setElements.add(
                    new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL)));
            var setJs = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(setElements));
            return EResult.ok(nullStack.toValue(loc, setJs));
        }
        if (selfDyn.source() instanceof ValueDynamic.Source.Binding bindingSrc) {
            var entries = new TList<JavaBytecode.BlockEntry>();
            if (selfCode != null) {
                entries.add(new JavaBytecode.BlockEntry.Code(selfCode));
            }
            entries.add(
                    new JavaBytecode.BlockEntry.Code(
                            new CodeDynamic(loc, new JavaBytecode.BindingSet(bindingSrc.key(), valueCode))));
            var setJs = new CodeDynamic(loc, new JavaBytecode.Block(entries));
            return EResult.ok(nullStack.toValue(loc, setJs));
        }
        return EResult.err(loc, "Cannot modify a temporary value");
    }
}
