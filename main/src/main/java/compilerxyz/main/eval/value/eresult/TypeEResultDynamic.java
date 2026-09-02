package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.eval.dynamic.CodeLoc.Some;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.AspectUnion.Variant;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6017L)
public record TypeEResultDynamic<T extends Type>(T elementType)
        implements Type,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectField,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectStackValueDynamic,
                AspectUnion,
                MatchDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
    }

    @Override
    public String typeDesc() {
        return "eresult(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public TList<Variant> aspectUnionVariants() {
        var out = new TList<Variant>();
        out.add(new Variant(new ConstKey.Str("ok"), "ok", elementType));
        out.add(new Variant(new ConstKey.Str("errs"), "errs", new TypeListBoxedDynamic(GenExprErr.dynamicType())));
        return out;
    }

    @Override
    public EResult<Void> matchDynamicPrepare(ExprContext xx) {
        return EResult.ok(null);
    }

    @Override
    public EResult<CodeDynamic> matchDynamicTest(QExprId loc, int variantIndex) {
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_HAS_ERRORS));
        if (variantIndex == 0) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_1));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.IXOR));
        }
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    @Override
    public EResult<CodeDynamic> matchDynamicExtract(ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
        var seq = new JavaBytecodeSequence();
        if (variantIndex == 0) {
            seq.add(JavaBytecodeUtils.literalString(new Some(loc), "eresult match ok"));
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE));
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("TypeEResultDynamic::matchExtract ok consume")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                seq.add(JavaBytecodeUtils.cast(loc, elemDesc));
            }
        } else {
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_ERRORS_OPT));
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.OPT_OPT_UNWRAP));
            seq.add(JavaBytecodeUtils.cast(
                    loc, JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class))));
        }
        return EResult.ok(new CodeDynamic(loc, seq));
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
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    private sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Factory(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    private TMap<ConstKey, FieldSpec> fieldSpecs() {
        return TMap.ofEntries(
                new Pair<>(
                        new ConstKey.Str("ok"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.OK))),
                new Pair<>(
                        new ConstKey.Str("err"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.ERR))),
                new Pair<>(
                        new ConstKey.Str("errs"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.ERRS))));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeEResultDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var ckStr =
                switch (ck) {
                    case ConstKey.Str s -> Opt.some(s);
                    case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
                };
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("has_errors")) {
            var selfDyn = self.code().codeDynamic();
            if (selfDyn == null) {
                return EResult.err(loc, "TypeEResultDynamic::aspectFieldField(has_errors): self has no runtime code");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(selfDyn);
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_HAS_ERRORS));
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(new Some(loc), seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
        }
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
            var selfDyn = self.code().codeDynamic();
            if (selfDyn == null) {
                return EResult.err(loc, "TypeEResultDynamic::aspectFieldField(value): self has no runtime code");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(selfDyn);
            seq.add(JavaBytecodeUtils.literalString(new Some(loc), "eresult.value"));
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE));
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("TypeEResultDynamic::field value AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                seq.add(JavaBytecodeUtils.cast(loc, elemDesc));
            }
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(new Some(loc), seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, elementType))));
        }
        var spec = fieldSpecs().getOrNull(ck);
        if (spec == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on eresult");
        }
        return switch (spec) {
            case FieldSpec.Factory f ->
                EResult.ok(new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(fieldSpecs().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var spec = fieldSpecs().getOrNull(key);
        return spec == null ? Opt.none() : Opt.some(spec.fieldSpecFieldType());
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn == null) {
            throw new IllegalStateException("TypeEResultDynamic::toDynamic: dynamic type must carry dynamic code");
        }
        return EResult.ok(dyn);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY, TypeEResultDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType);
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
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
