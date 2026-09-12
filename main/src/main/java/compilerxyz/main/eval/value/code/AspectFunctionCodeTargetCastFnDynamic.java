package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
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
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.prelude.util.Opt;
import org.objectweb.asm.tree.LdcInsnNode;

public record AspectFunctionCodeTargetCastFnDynamic(TypeCodeTargetCastFnDynamic type) implements AspectFunction {

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
        var selfDynRes = DynUtil.dynamicConsumeToDynamic(loc, self);
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
        var destRes = DynUtil.requireBoxedTypeArgument(xx, loc, "code cast target type", destType);
        if (destRes.hasErrors()) return destRes.reerr();
        Type resultType = new TypeEResultDynamic<>(destType);
        return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, seq), resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
