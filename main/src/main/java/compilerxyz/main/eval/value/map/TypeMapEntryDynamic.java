package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.DynUtil.DropChild;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6123L)
public record TypeMapEntryDynamic(Type keyType, Type valueType)
        implements Type,
                AspectBind,
                AspectBindDynamic,
                AspectInitialBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamic,
                AspectField {

    private static final JavaInternalName MAP_ENTRY_INAME = JavaInternalName.fromClass(Map.Entry.class);

    private record FieldSpec(Type type, String jvmGetter) {}

    private TMap<ConstKey, FieldSpec> fieldSpecs() {
        return TMap.ofEntries(
                new Pair<>(new ConstKey.Str("key"), new FieldSpec(keyType, "getKey")),
                new Pair<>(new ConstKey.Str("value"), new FieldSpec(valueType, "getValue")));
    }

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("TypeMapEntryDynamic::drop: called on absent CodeValue");
        }
        if (!(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            throw new IllegalStateException("TypeMapEntryDynamic::drop: expected ValueDynamic, got "
                    + cv.value().optUnwrap().getClass());
        }
        var descObjNoArgs = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of());
        var children = new TList<DropChild>(2);
        children.add(new DropChild(
                keyType, JavaBytecodeUtils.callInterfaceMethod(loc, MAP_ENTRY_INAME, "getKey", descObjNoArgs)));
        children.add(new DropChild(
                valueType, JavaBytecodeUtils.callInterfaceMethod(loc, MAP_ENTRY_INAME, "getValue", descObjNoArgs)));
        var _ddcSource = vd.source();
        boolean _ddcStackRooted = DynUtil.isStackRooted(_ddcSource);
        var _ddcChildDrops = new TList<CodeDynamic>();
        for (var _ddcChild : children) {
            var _ddcDropOpt = GenAspectDropDynamic.getMaybe(xx, _ddcChild.childType());
            if (_ddcDropOpt.optIsNone()) continue;
            var _ddcSeq = new JavaBytecodeSequence();
            _ddcSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            _ddcSeq.add(_ddcChild.access());
            var _ddcChildCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, _ddcSeq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, _ddcChild.childType())));
            var _ddcChildDropRes = _ddcDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, _ddcChildCv);
            if (_ddcChildDropRes.hasErrors()) return _ddcChildDropRes.reerr();
            var _ddcChildDynCast =
                    CodeDynamic.castOpt(loc, _ddcChildDropRes.requireValue("dropDestructiveComposite child drop"));
            if (_ddcChildDynCast.hasErrors()) return _ddcChildDynCast.reerr();
            var _ddcChildDyn = _ddcChildDynCast.requireValue("_ddcChildDyn");
            if (_ddcChildDyn.optIsSome()) _ddcChildDrops.add(_ddcChildDyn.optUnwrap());
        }
        if (_ddcChildDrops.isEmpty()) return EResult.ok(_ddcSource.valueDynamicSourceDrop(loc));
        var _ddcOut = new JavaBytecodeSequence();
        if (!_ddcStackRooted) _ddcOut.add(_ddcSource.valueDynamicSourceRealize(xx, loc));
        for (var _ddcD : _ddcChildDrops) _ddcOut.add(_ddcD);
        _ddcOut.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, _ddcOut)));
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Map.Entry.class));
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public String typeDesc() {
        return "map-entry(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectBind.AspectId) || key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_ENTRY_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamic.AspectId)
                || key.equals(GenAspectField.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeMapEntryDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var spec = fieldSpecs().getOrNull(ck);
        if (spec == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on map entry");
        }
        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeMapEntryDynamic::field: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(
                loc,
                MAP_ENTRY_INAME,
                spec.jvmGetter(),
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
        var fieldDesc = JvmSort.descriptor(xx, spec.type());
        if (fieldDesc.isReference() && !fieldDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            seq.add(JavaBytecodeUtils.cast(loc, fieldDesc));
        }
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, spec.type()))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(fieldSpecs().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var spec = fieldSpecs().getOrNull(key);
        return spec == null ? Opt.none() : Opt.some(spec.type());
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsNone()) {
            throw new IllegalStateException(
                    "TypeMapEntryDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
        }
        var dyn = dynCast.requireValue("dyn").optUnwrap();
        return EResult.ok(dyn);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_MAP_ENTRY_DYNAMIC_IDENTITY, TypeMapEntryDynamic.class);
        return oOpt.optIsSome()
                && keyType.typeIsEqual(xx, oOpt.optUnwrap().keyType)
                && valueType.typeIsEqual(xx, oOpt.optUnwrap().valueType);
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
