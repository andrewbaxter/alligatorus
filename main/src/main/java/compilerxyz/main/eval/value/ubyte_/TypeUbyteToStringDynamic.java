package compilerxyz.main.eval.value.ubyte_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6183L)
public record TypeUbyteToStringDynamic()
        implements Type, AspectInitialBindDynamic, AspectConsumeDynamic, AspectFunction {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsSome())
            return EResult.ok(dynCast.requireValue("dyn").optUnwrap());
        return EResult.err(loc, "TypeUbyteToStringDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "ubyte.to_string";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_UBYTE_TO_STRING_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeUbyteToStringDynamic::call: self has no dynamic code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var retType = new TypeStringDynamic();
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callStaticMethod(loc, GenCodegenDescriptors.NUMERIC_FORMAT_FORMAT_UNSIGNED_BYTE));
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UBYTE_TO_STRING_DYNAMIC_IDENTITY)
                .optIsSome();
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
