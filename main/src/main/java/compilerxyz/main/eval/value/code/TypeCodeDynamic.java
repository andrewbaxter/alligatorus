package compilerxyz.main.eval.value.code;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.MatchDynamic;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

@BuiltinPath("types.code")
@BuiltinFull(uniqueId = 6300L)
public record TypeCodeDynamic() implements Aspect, Type, MatchDynamic {

    private static final JavaInternalName CODE_INTNAME = JavaInternalName.fromClass(Code.class);

    @Override
    public String typeDesc() {
        return "code";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(AspectUnionCodeDynamic.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftCodeDynamic.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicCodeDynamic(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                || key.equals(GenTrivialIdentityAspects.CODE_IDENTITY)
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
    public EResult<CodeDynamicReg> matchDynamicTest(QExprId loc, JavaBytecodeReg base, int variantIndex) {
        var desc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of());
        var b = new JavaBytecodeBuilder(loc);
        var isNone = b.invokeInterface(CODE_INTNAME, "codeIsNone", desc, base);
        if (variantIndex == 1) {
            isNone = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, isNone, b.constBool(true));
        }
        return EResult.ok(b.code(isNone));
    }

    @Override
    public EResult<CodeDynamicReg> matchDynamicExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType) {
        var b = new JavaBytecodeBuilder(loc);
        if (variantIndex == 0) {
            return EResult.ok(b.code(b.constNull()));
        }
        return EResult.ok(b.code(b.invokeStatic(GenCodegenDescriptors.CODE_TARGET_IMPL, base)));
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.CODE_DYNAMIC);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CODE_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType
                .typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
}
