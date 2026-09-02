package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.function.TypeMethodDynamic;
import compilerxyz.main.eval.value.object_.TypeObjectDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6197L)
public record TypeInterfaceMethodBuiltin(TypeObjectDynamic receiverType, String name, TypeMethodDynamic method)
        implements Type, AspectFunction, AspectInitialBind {

    @Override
    public String typeDesc() {
        return "interface method " + name;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_BUILTIN_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var splitRes =
                DynUtil.methodRefSplitArgument(xx, loc, method.arg(), argument, "interface method '" + name + "'");
        if (splitRes.hasErrors()) return splitRes.reerr();
        var split = splitRes.requireValue("TypeInterfaceMethodBuiltin split");
        var receiverRes = Eval.typecheckConvert(xx, loc, receiverType, split.receiver());
        if (receiverRes.hasErrors()) return receiverRes.reerr();
        var receiverCv = receiverRes.requireValue("TypeInterfaceMethodBuiltin receiver");
        var receiverDynRes = receiverType.aspectConsumeDynamicToDynamic(xx, loc, receiverCv);
        if (receiverDynRes.hasErrors()) return receiverDynRes.reerr();
        var receiverDyn = receiverDynRes.requireValue("TypeInterfaceMethodBuiltin receiver toDynamic");
        return method.aspectFunctionCall(
                xx, loc, new CodeValue(Code.of(receiverDyn), receiverCv.outcome()), split.methodArgs());
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
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_BUILTIN_IDENTITY,
                TypeInterfaceMethodBuiltin.class);
        return oOpt.optIsSome()
                && receiverType.typeIsEqual(xx, oOpt.optUnwrap().receiverType)
                && name.equals(oOpt.optUnwrap().name);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
