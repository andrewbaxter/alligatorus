package compilerxyz.eval.dynamic;

import compilerxyz.gen.Binding;
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

    public record ArgSignatureSlot(Type elementType, JavaDataDescriptor jvmDesc) {}

    public static EResult<TList<ArgSignatureSlot>> argSignatureSlots(
            ExprContext xx, QExprId loc, Type argType) {
        var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            var elemTypes = tupleSome.value().aspectTupleElementTypes();
            var out = new TList<ArgSignatureSlot>(elemTypes.size());
            for (var et : elemTypes) {
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et,
                        descRes.requireValue("DynUtil::argSignatureSlots tuple element")));
            }
            return EResult.ok(out);
        }
        var recordOpt = compilerxyz.gen.AspectRecord.getMaybe(xx, argType);
        if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
            var fieldTypes = recordSome.value().aspectRecordRecordFieldTypes();
            var out = new TList<ArgSignatureSlot>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                var et = entry.getValue();
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et,
                        descRes.requireValue("DynUtil::argSignatureSlots record field")));
            }
            return EResult.ok(out);
        }
        var descRes = jvmDescFor(xx, loc, argType);
        if (descRes.hasErrors()) return descRes.reerr();
        return EResult.ok(TList.of(new ArgSignatureSlot(argType,
                descRes.requireValue("DynUtil::argSignatureSlots scalar"))));
    }

    private static EResult<JavaDataDescriptor> jvmDescFor(ExprContext xx, QExprId loc, Type t) {
        var consumeRes = AspectConsumeDynamic.get(xx, loc, t);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        return EResult.ok(consumeRes.requireValue("DynUtil::jvmDescFor").aspectConsumeDynamicConsumeDynamicJvmClass());
    }

    public record CallArgElement(CodeDynamic code, JavaDataDescriptor jvmDesc) {}

    /**
     * Destructure a call-site argument into per-slot (code, jvmType) elements matching the
     * callee's argType shape.
     *
     * <ul>
     *   <li>argType is a Tuple: caller's argument must be a matching Tuple. Element-wise
     *       destructure to N positional elements.
     *   <li>argType is a Record: caller's argument must be a matching Record. Field-wise
     *       destructure to N positional elements in field-declaration order.
     *   <li>Otherwise (scalar): one element containing the argument as-is.
     * </ul>
     */
    public static EResult<TList<CallArgElement>> destructureCallArg(
            ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
        var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructureCallArg: tuple arg has no compile-time value");
            }
            var elementTypes = tupleSome.value().aspectTupleElementTypes();
            var elemCvs = tupleSome.value().aspectTupleTakeElements(xx, loc, some.value());
            if (elementTypes.size() != elemCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructureCallArg: tuple elementTypes/takeElements size mismatch");
            }
            var out = new TList<CallArgElement>(elementTypes.size());
            for (int i = 0; i < elementTypes.size(); i++) {
                var elemCvRes = elemCvs.get(i);
                if (elemCvRes.hasErrors()) return elemCvRes.reerr();
                var elemCv = elemCvRes.requireValue("DynUtil::destructureCallArg element");
                var pair = elementToDyn(xx, loc, elementTypes.get(i), elemCv);
                if (pair.hasErrors()) return pair.reerr();
                out.add(pair.requireValue("DynUtil::destructureCallArg tuple elem"));
            }
            return EResult.ok(out);
        }
        var recordOpt = compilerxyz.gen.AspectRecord.getMaybe(xx, argType);
        if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructureCallArg: record arg has no compile-time value");
            }
            var fieldTypes = recordSome.value().aspectRecordRecordFieldTypes();
            var fieldCvs = recordSome.value().aspectRecordRecordTakeFields(xx, loc, some.value());
            var out = new TList<CallArgElement>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                var fkey = entry.getKey();
                var ftype = entry.getValue();
                var fieldCvRes = fieldCvs.get(fkey);
                if (fieldCvRes == null) {
                    return EResult.err(loc, "DynUtil::destructureCallArg: missing field " + fkey.toCanonical());
                }
                if (fieldCvRes.hasErrors()) return fieldCvRes.reerr();
                var fieldCv = fieldCvRes.requireValue("DynUtil::destructureCallArg field");
                var pair = elementToDyn(xx, loc, ftype, fieldCv);
                if (pair.hasErrors()) return pair.reerr();
                out.add(pair.requireValue("DynUtil::destructureCallArg record field"));
            }
            return EResult.ok(out);
        }
        // Scalar.
        var pair = elementToDyn(xx, loc, argType, converted);
        if (pair.hasErrors()) return pair.reerr();
        return EResult.ok(TList.of(pair.requireValue("DynUtil::destructureCallArg scalar")));
    }

    private static EResult<CallArgElement> elementToDyn(
            ExprContext xx, QExprId loc, Type elemType, CodeValue elemCv) {
        var consumeRes = AspectConsumeDynamic.get(xx, loc, elemType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var consume = consumeRes.requireValue("DynUtil::elementToDyn consume");
        var dynRes = consume.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return EResult.ok(
                new CallArgElement(
                        dynRes.requireValue("DynUtil::elementToDyn toDynamic"),
                        consume.aspectConsumeDynamicConsumeDynamicJvmClass()));
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

    /**
     * Shared JVM foreach-loop skeleton. Callers provide the type-specific
     * pieces (iterator setup, element decode, element slot, store opcode, body)
     * and this emits the label-based loop wrapping them. The loop return value
     * is a stack null wrapped as a TypeNullDynamic ValueDynamic — no
     * AspectStackValueDynamic lookup involved.
     */
    public static CodeValue foreachSkeleton(
            QExprId loc,
            CodeDynamic iteratorSetup,
            CodeDynamic elementDecode,
            compilerxyz.eval.BindingId.Local elementSlot,
            int elementStoreOp,
            Code body) {
        var loopLabel = new org.objectweb.asm.tree.LabelNode();
        var doneLabel = new org.objectweb.asm.tree.LabelNode();
        var seq = new JavaBytecodeSequence();
        seq.add(iteratorSetup);
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
        var iteratorName = JavaInternalName.of("java/util/Iterator");
        seq.add(
                JavaBytecodeUtils.callInterfaceMethod(
                        loc,
                        iteratorName,
                        "hasNext",
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of())));
        seq.add(
                new CodeDynamic(
                        loc,
                        new JavaBytecodeInstructionObj(
                                new org.objectweb.asm.tree.JumpInsnNode(
                                        org.objectweb.asm.Opcodes.IFEQ, doneLabel))));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
        seq.add(
                JavaBytecodeUtils.callInterfaceMethod(
                        loc,
                        iteratorName,
                        "next",
                        JavaMethodDescriptor.fromParts(
                                JavaDataDescriptor.fromClass(Object.class), TList.of())));
        seq.add(elementDecode);
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(elementStoreOp, elementSlot.key())));
        var bodyDyn = body.dynamic();
        if (bodyDyn != null) {
            seq.add(bodyDyn);
            seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        }
        seq.add(
                new CodeDynamic(
                        loc,
                        new JavaBytecodeInstructionObj(
                                new org.objectweb.asm.tree.JumpInsnNode(
                                        org.objectweb.asm.Opcodes.GOTO, loopLabel))));
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        seq.add(JavaBytecodeUtils.literalNull(loc));
        return dynamicStackToValue(loc, new CodeDynamic(loc, seq), new TypeNullDynamic());
    }

    public static EResult<CodeValue> fieldUseDefaultDynamic(
            ExprContext xx, QExprId loc, Type self, ValueDynamic.Source baseSource, String javaFieldName) {
        var childSource = new ValueDynamic.Source.Field(baseSource, javaFieldName);
        var code = compilerxyz.eval.Code.of(loc, renderSource(xx, loc, childSource));
        return EResult.ok(new CodeValue(code, Opt.some(new ValueDynamic(childSource, self))));
    }

    public static EResult<CodeValue> elementUseDefaultDynamic(
            ExprContext xx, QExprId loc, Type self, ValueDynamic.Source elementSource, Code carry) {
        return EResult.ok(new CodeValue(carry, Opt.some(new ValueDynamic(elementSource, self))));
    }

    public static CodeDynamic renderSource(
            ExprContext xx, QExprId loc, ValueDynamic.Source src) {
        if (src instanceof ValueDynamic.Source.Expr) {
            throw new IllegalStateException(
                "ValueDynamic.Source.Expr has no reconstructable code");
        }
        return src.realize(xx, loc);
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (!(valueOpt instanceof Opt.Some<Value> some)) {
            throw new IllegalStateException(
                    "bind_via_target called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = some.value().valueType();
        if (cv.code().isNone()) {
            Binding binding = new BindingValue(some.value());
            return EResult.ok(new AspectBind.BindResult(cv.code(), binding));
        }
        var bindDynOpt = AspectBindDynamic.getMaybe(xx, vtype);
        if (!(bindDynOpt instanceof Opt.Some<AspectBindDynamic> bindDynSome)) {
            return EResult.err(loc, "Type (" + vtype.typeDesc() + ") is missing aspect AspectBindDynamic");
        }
        var subRes = bindDynSome.value().aspectBindDynamicBindDynamic(xx, loc, cv);
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
        var vtype = some.value().valueType();
        var dynRes = dynamicConsumeToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault dynamicConsumeToDynamic");
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    /**
     * Destructuring drop for stack-allocated composite (record/tuple-like) types.
     * The base sits on the JVM stack (source is {@code Source.Expr}). For each
     * child field whose type has {@code AspectDropDynamic} we emit
     * {@code DUP, <field-access>, <child-drop>} then finish with a single {@code POP}
     * to discard the base object reference. If no child needs a drop or the
     * source isn't stack-rooted the emission collapses to {@link #dropDefault}.
     *
     * @param children the destructured child accesses in emission order; each
     *                 entry pairs the child type (queried for AspectDropDynamic)
     *                 with the bytecode that pops the DUP'd base and pushes the
     *                 child value.
     */
    public static EResult<Code> dropDestructiveComposite(
            ExprContext xx, QExprId loc, ValueDynamic.Source source, TList<DropChild> children) {
        boolean stackRooted = isStackRooted(source);
        TList<CodeDynamic> childDrops = new TList<>();
        for (var child : children) {
            var childDropOpt = compilerxyz.gen.AspectDropDynamic.getMaybe(xx, child.childType());
            if (!(childDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamic> childDropSome)) {
                continue;
            }
            var seq = new JavaBytecodeSequence();
            seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
            seq.add(child.access());
            var childCv =
                    new CodeValue(
                            Code.of(loc, new CodeDynamic(loc, seq)),
                            Opt.some(ValueDynamic.stack(child.childType())));
            var childDropRes = childDropSome.value().aspectDropDynamicDrop(xx, loc, childCv);
            if (childDropRes.hasErrors()) {
                return childDropRes.reerr();
            }
            var childDropCode = childDropRes.requireValue("dropDestructiveComposite child drop");
            var childDyn = childDropCode.dynamic();
            if (childDyn != null) {
                childDrops.add(childDyn);
            }
        }
        if (childDrops.isEmpty()) {
            return EResult.ok(source.drop(loc));
        }
        var out = new JavaBytecodeSequence();
        if (!stackRooted) {
            out.add(source.realize(xx, loc));
        }
        for (var d : childDrops) out.add(d);
        out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        return EResult.ok(Code.of(loc, new CodeDynamic(loc, out)));
    }

    /**
     * A child entry for {@link #dropDestructiveComposite}. {@code access} is
     * bytecode that consumes a base object reference from the stack and pushes
     * the corresponding child value; typical shape: {@code GETFIELD} or
     * {@code INVOKEINTERFACE} on a getter.
     */
    public record DropChild(Type childType, CodeDynamic access) {}

    public static EResult<Code> dropDefault(ExprContext xx, QExprId loc, CodeValue cv) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
            return EResult.ok(Code.none(loc));
        }
        if (!(some.value() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        return EResult.ok(vd.source().drop(loc));
    }

    private static boolean isStackRooted(ValueDynamic.Source source) {
        return switch (source) {
            case ValueDynamic.Source.Expr expr -> true;
            case ValueDynamic.Source.Binding binding -> false;
            case ValueDynamic.Source.Field field -> isStackRooted(field.base());
            case ValueDynamic.Source.Dup dup -> isStackRooted(dup.base());
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
            return boxedSome.value().aspectDynamicBoxedBoxedForm();
        }
        return type;
    }

    public static CodeValue dynamicStackToValue(QExprId loc, CodeDynamic value, Type type_) {
        var dynValue =
                ValueDynamic.stack(type_);
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
        Type valueType = convSome.value().valueType();
        var valueToDynRes = AspectConsumeDynamic.get(xx, loc, valueType);
        if (valueToDynRes.hasErrors()) {
            return valueToDynRes.reerr();
        }
        var valueToDyn = valueToDynRes.requireValue("DynUtil::dynamicSet consume aspect");
        var valueCodeRes = valueToDyn.aspectConsumeDynamicToDynamic(xx, loc, converted);
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
            return EResult.ok(nullStack.aspectStackValueDynamicToValue(loc, setJs));
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
            return EResult.ok(nullStack.aspectStackValueDynamicToValue(loc, setJs));
        }
        return EResult.err(loc, "Cannot modify a temporary value");
    }

    /**
     * If {@code argType} is a tuple, destructure the compile-time value into per-element
     * CodeDynamic pushes (each element lowered via its own AspectConsumeDynamic). Otherwise
     * lowers the whole value via {@code argType}'s AspectConsumeDynamic and returns a
     * singleton list. Used at call sites whose JVM descriptor takes destructured positional args.
     */
    public static EResult<TList<CodeDynamic>> destructurePositional(
            ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
        var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var elementTypes = tupleSome.value().aspectTupleElementTypes();
            var elemCvs = tupleSome.value().aspectTupleTakeElements(xx, loc, some.value());
            if (elementTypes.size() != elemCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructurePositional: elementTypes/takeElements size mismatch");
            }
            return destructureFromElements(xx, loc, elementTypes, elemCvs);
        }
        var recordOpt = compilerxyz.gen.AspectRecord.getMaybe(xx, argType);
        if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var fieldTypes = recordSome.value().aspectRecordRecordFieldTypes();
            var fieldCvs = recordSome.value().aspectRecordRecordTakeFields(xx, loc, some.value());
            if (fieldTypes.size() != fieldCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructurePositional: record fieldTypes/takeFields size mismatch");
            }
            var elemTypes = new TList<Type>(fieldTypes.size());
            var elemCvs = new TList<EResult<CodeValue>>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                elemTypes.add(entry.getValue());
                var cvRes = fieldCvs.get(entry.getKey());
                if (cvRes == null) {
                    return EResult.err(loc,
                            "DynUtil::destructurePositional: record missing field '"
                                    + entry.getKey().toCanonical() + "'");
                }
                elemCvs.add(cvRes);
            }
            return destructureFromElements(xx, loc, elemTypes, elemCvs);
        }
        var consumeRes = AspectConsumeDynamic.get(xx, loc, argType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDyn =
                consumeRes
                        .requireValue("DynUtil::destructurePositional arg consume")
                        .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (argDyn.hasErrors()) return argDyn.reerr();
        return EResult.ok(TList.of(argDyn.requireValue("DynUtil::destructurePositional arg")));
    }

    private static EResult<TList<CodeDynamic>> destructureFromElements(
            ExprContext xx,
            QExprId loc,
            TList<Type> elementTypes,
            TList<EResult<CodeValue>> elemCvs) {
        var out = new TList<CodeDynamic>(elementTypes.size());
        for (int i = 0; i < elementTypes.size(); i++) {
            var elemCvRes = elemCvs.get(i);
            if (elemCvRes.hasErrors()) return elemCvRes.reerr();
            var elemCv = elemCvRes.requireValue("DynUtil::destructurePositional element");
            var consumeRes = AspectConsumeDynamic.get(xx, loc, elementTypes.get(i));
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var elemDyn =
                    consumeRes
                            .requireValue("DynUtil::destructurePositional consume")
                            .aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (elemDyn.hasErrors()) return elemDyn.reerr();
            out.add(elemDyn.requireValue("DynUtil::destructurePositional elem toDynamic"));
        }
        return EResult.ok(out);
    }

    /**
     * Same as {@link #destructurePositional} but packs the results into a single Object:
     * 0 elements → ACONST_NULL; 1 element → the element as-is; 2+ elements →
     * Object[] via {@link JavaBytecodeUtils#buildObjectArray}. Used at call sites whose JVM
     * descriptor takes a single packed Object payload (e.g. invisible builtins, constructor
     * wrappers).
     */
    public static EResult<CodeDynamic> destructurePackedObject(
            ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
        var positional = destructurePositional(xx, loc, argType, converted);
        if (positional.hasErrors()) return positional.reerr();
        var elemCodes = positional.requireValue("DynUtil::destructurePackedObject elements");
        if (elemCodes.isEmpty()) {
            return EResult.ok(JavaBytecodeUtils.literalNull(loc));
        }
        if (elemCodes.size() == 1) {
            return EResult.ok(elemCodes.get(0));
        }
        return EResult.ok(JavaBytecodeUtils.buildObjectArray(loc, elemCodes));
    }
}
