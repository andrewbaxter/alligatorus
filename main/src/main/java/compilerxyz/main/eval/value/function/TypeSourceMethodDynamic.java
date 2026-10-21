package compilerxyz.main.eval.value.function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinLight(uniqueId = 6321L)
public record TypeSourceMethodDynamic(
        Type receiver,
        TList<FunctionParam> params,
        Opt<Type> ret,
        JavaInternalName klass,
        String methodName,
        JavaMethodDescriptor descriptor,
        boolean isInterface,
        boolean needsEvalContext,
        Opt<DefinitionSet> set)
        implements Aspect, Type {

    @Override
    public String toString() {
        return "TypeSourceMethodDynamic[" + klass.value + "." + methodName + descriptor.value() + "]";
    }

    public Type arg() {
        return FunctionParam.argType(params);
    }

    @Override
    public String typeDesc() {
        return "dynamic source method";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindSourceMethodDynamic(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_METHOD_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionSourceMethodDynamic(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicMethod(JavaDataDescriptor.fromInternalName(klass)));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_SOURCE_METHOD_DYNAMIC_IDENTITY,
                TypeSourceMethodDynamic.class);
        return oOpt.optIsSome()
                && receiver.typeIsEqual(xx, oOpt.optUnwrap().receiver)
                && TypeFunctionConst.paramsEqual(xx, params, oOpt.optUnwrap().params)
                && TypeFunctionConst.retEqual(xx, ret, oOpt.optUnwrap().ret)
                && klass.equals(oOpt.optUnwrap().klass)
                && methodName.equals(oOpt.optUnwrap().methodName)
                && isInterface == oOpt.optUnwrap().isInterface
                && needsEvalContext == oOpt.optUnwrap().needsEvalContext;
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
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.none();
    }
}
