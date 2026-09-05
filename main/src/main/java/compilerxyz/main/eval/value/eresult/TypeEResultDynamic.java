package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc.Some;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6017L)
public record TypeEResultDynamic<T extends Type>(T elementType) implements Aspect, Type, MatchDynamic {

    @Override
    public String typeDesc() {
        return "eresult(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(new AspectUnionEResultDynamic<>(this));
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldEResultDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindEResultDynamic<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY)
                || key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
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

    sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Factory(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    TMap<ConstKey, FieldSpec> fieldSpecs() {
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
