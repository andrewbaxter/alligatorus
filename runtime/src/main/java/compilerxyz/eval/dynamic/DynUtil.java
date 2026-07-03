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


    public static CodeDynamic buildFieldGetSequence(QExprId loc, CodeDynamic baseCode, String fieldKey) {
        var seq = new JavaBytecodeSequence();
        seq.add(baseCode);
        seq.add(JavaBytecodeUtils.literalString(loc, fieldKey));
        seq.add(
                JavaBytecodeUtils.callInterfaceMethod(
                        loc,
                        JvmConstants.INTNAME_MAP,
                        "get",
                        JavaMethodDescriptor.fromParts(
                                JvmConstants.DESC_OBJECT,
                                TList.of(JvmConstants.DESC_OBJECT))));
        return new CodeDynamic(loc, seq);
    }

    public static CodeDynamic renderSource(
            ExprContext xx, QExprId loc, ValueDynamic.Source src) {
        return switch (src) {
            case ValueDynamic.Source.Binding b ->
                BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, b.key());
            case ValueDynamic.Source.Field f ->
                buildFieldGetSequence(loc, renderSource(xx, loc, f.base()), f.key());
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
        var subOpt = subRes.requireValue("DynUtil::bindViaTarget bindDynamic");
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
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault dynamicConsumeToDynamic");
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    public static EResult<Code> dropDefault(ExprContext xx, QExprId loc, CodeValue cv) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
            return EResult.ok(Code.none(loc));
        }
        if (!(some.value() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        if (!isStackRooted(vd.source())) {
            return EResult.ok(Code.none(loc));
        }
        // Wide primitives (double, long) occupy two operand-stack slots — use POP2.
        int popOp = org.objectweb.asm.Opcodes.POP;
        if (vd.type_() instanceof compilerxyz.gen.AspectConsumeDynamic consume) {
            var d = consume.consumeDynamicJvmClass().value();
            if (!d.isEmpty() && (d.charAt(0) == 'D' || d.charAt(0) == 'J')) {
                popOp = org.objectweb.asm.Opcodes.POP2;
            }
        }
        return EResult.ok(Code.of(loc, JavaBytecodeUtils.inst(loc, popOp)));
    }

    private static boolean isStackRooted(ValueDynamic.Source source) {
        return switch (source) {
            case ValueDynamic.Source.Expr expr -> true;
            case ValueDynamic.Source.Binding binding -> false;
            case ValueDynamic.Source.Field field -> isStackRooted(field.base());
        };
    }

    public static EResult<CodeDynamic> dynamicConsumeToDynamic(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().dynamic();
        if (dyn == null) {
            return EResult.err(loc, "Expression resulted in empty code");
        }
        return EResult.ok(dyn);
    }

    /**
     * If {@code type} carries AspectDynamicBoxed (i.e. is an unboxed primitive
     * dynamic type with a boxed counterpart), returns the boxed counterpart type.
     * Otherwise returns {@code type} unchanged. Used at container (list/map/tuple)
     * type-construction sites so element types are always boxed when flowing
     * through Object-shaped runtime containers.
     */
    public static Type substituteBoxed(ExprContext xx, Type type) {
        var boxedOpt = compilerxyz.gen.AspectDynamicBoxed.getMaybe(xx, type);
        if (boxedOpt instanceof Opt.Some<compilerxyz.gen.AspectDynamicBoxed> boxedSome) {
            return boxedSome.value().boxedForm();
        }
        return type;
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
        var seq = new JavaBytecodeSequence();
        if (carryDyn.code() instanceof JavaBytecodeSequence carrySeq) {
            for (var child : carrySeq.children) seq.add(child);
        } else {
            seq.add(carryDyn);
        }
        seq.add(dynamicCode);
        return new CodeDynamic(loc, seq);
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
        var converted = convRes.requireValue("DynUtil::dynamicSet typecheckConvert");
        if (!(converted.value() instanceof Opt.Some<Value> convSome)) {
            return EResult.err(loc, "dynamicSet: converted value has no compile-time value");
        }
        Type valueType = convSome.value().valueTypeOf();
        var valueToDynRes = AspectConsumeDynamic.get(xx, loc, valueType);
        if (valueToDynRes.hasErrors()) {
            return valueToDynRes.reerr();
        }
        var valueToDyn = valueToDynRes.requireValue("DynUtil::dynamicSet consume aspect");
        var valueCodeRes = valueToDyn.toDynamic(xx, loc, converted);
        if (valueCodeRes.hasErrors()) {
            return valueCodeRes.reerr();
        }
        var valueCode = valueCodeRes.requireValue("DynUtil::dynamicSet consume.toDynamic");
        var selfCode = self_.code().dynamic();
        var nullStackRes = AspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
        if (nullStackRes.hasErrors()) {
            return nullStackRes.reerr();
        }
        var nullStack = nullStackRes.requireValue("DynUtil::dynamicSet null stack aspect");
        if (selfDyn.source() instanceof ValueDynamic.Source.Field fieldSrc) {
            if (selfCode == null) {
                return EResult.err(
                        loc,
                        "TODO assertion set left hand doesn't evaluate to dynamic code (empty)");
            }
            CodeDynamic baseCode = renderSource(xx, loc, fieldSrc.base());
            var setSeq = new JavaBytecodeSequence();
            setSeq.add(baseCode);
            setSeq.add(JavaBytecodeUtils.literalString(loc, fieldSrc.key()));
            setSeq.add(valueCode);
            setSeq.add(
                    JavaBytecodeUtils.callInterfaceMethod(
                            loc,
                            JavaInternalName.of("java/util/Map"),
                            "put",
                            JavaMethodDescriptor.fromParts(
                                    JvmConstants.DESC_OBJECT,
                                    TList.of(
                                            JvmConstants.DESC_OBJECT,
                                            JvmConstants.DESC_OBJECT))));
            setSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
            setSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, setSeq);
            return EResult.ok(nullStack.toValue(loc, setJs));
        }
        if (selfDyn.source() instanceof ValueDynamic.Source.Binding bindingSrc) {
            var seq = new JavaBytecodeSequence();
            if (bindingSrc.key() instanceof compilerxyz.eval.BindingId.Local local) {
                // Local slot: pick the typed store opcode from the binding's type
                // so a primitive-typed slot uses DSTORE / ISTORE / etc.
                seq.add(valueCode);
                seq.add(
                    new CodeDynamic(
                        new CodeLoc.Some(loc),
                        new JavaBytecodeStoreLoad(
                            JvmSort.storeOpcode(targetType), local.key())));
            } else {
                seq.add(BindingCode.bindingSet(new CodeLoc.Some(loc), xx.vx, bindingSrc.key(), valueCode));
            }
            // set expression's language type is null → push a matching Object null
            // so downstream consumers see a stack value.
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, seq);
            return EResult.ok(nullStack.toValue(loc, setJs));
        }
        return EResult.err(loc, "Cannot modify a temporary value");
    }
}
