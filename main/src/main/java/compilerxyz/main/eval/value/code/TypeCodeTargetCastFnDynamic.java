package compilerxyz.main.eval.value.code;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import org.objectweb.asm.tree.LdcInsnNode;

@BuiltinFull(uniqueId = 6361L)
public record TypeCodeTargetCastFnDynamic() implements Type, AspectFunction {

    @Override
    public String typeDesc() {
        return "code.target.cast";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_CODE_TARGET_CAST_FN_DYNAMIC_IDENTITY)
                || key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "code.target.cast: argument must be a compile-time type");
        }
        var argValue = argument.value().optUnwrap();
        var aspectTypeRes = GenAspectType.get(xx, loc, argValue.valueType());
        if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
        var destType = aspectTypeRes.requireValue("code.target.cast AspectType").aspectTypeType(argValue);
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, destType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var destDesc = consumeRes.requireValue("code.target.cast dest consume").aspectConsumeDynamicJvmClass(xx);
        if (!destDesc.isReference() || destDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            return EResult.err(
                    loc,
                    "code.target.cast: destination type " + destType.typeDesc()
                            + " has no distinguishing host class; cast to a concrete object type");
        }
        var selfDynRes = CodeDynamic.castRequired(loc, self.code(), "code.target.cast: receiver has no dynamic code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var argDynCast = CodeDynamic.castOpt(loc, argument.code());
        if (argDynCast.hasErrors()) return argDynCast.reerr();
        var argDyn = argDynCast.requireValue("argDyn");

        var locKey = new BindingId();
        xx.vx.externals.add(
                new EvalContext.ExternalEntry(locKey, GenQExprId.dynamicType(), new External(QExprId.class, loc)));

        var seq = new JavaBytecodeSequence();
        seq.add(selfDynRes.requireValue("selfDyn"));
        if (argDyn.optIsSome()) seq.add(argDyn.optUnwrap());
        seq.add(BindingCode.bindingUse(new CodeLoc.Some(loc), locKey));
        seq.add(JavaBytecodeUtils.wrapObj(loc, new LdcInsnNode(org.objectweb.asm.Type.getType(destDesc.value()))));
        seq.add(JavaBytecodeUtils.callStaticMethod(new CodeLoc.Some(loc), GenCodegenDescriptors.CODE_CAST_TARGET_CODE));
        Type resultType = new TypeEResultDynamic<>(DynUtil.substituteBoxed(xx, destType));
        return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, seq), resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CODE_TARGET_CAST_FN_DYNAMIC_IDENTITY)
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
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
}
