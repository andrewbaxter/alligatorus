package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Map;

@BuiltinFull(uniqueId = 6124L)
public record TypeMapGetFnDynamic(Type keyType, Type valueType) implements Type, AspectFunction {

    @Override
    public String typeDesc() {
        return "map get fn";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_GET_FN_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var keyConvertedRes = Eval.typecheckConvert(xx, loc, keyType, argument);
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("get key converted");
        var keyConsumeRes = GenAspectConsumeDynamic.get(xx, loc, keyType);
        if (keyConsumeRes.hasErrors()) return keyConsumeRes.reerr();
        var keyDynRes = keyConsumeRes
                .requireValue("AspectConsumeDynamic.get key")
                .aspectConsumeDynamicToDynamic(xx, loc, keyConverted);
        if (keyDynRes.hasErrors()) return keyDynRes.reerr();
        var keyDyn = keyDynRes.requireValue("toDynamic key");

        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeMapGetFnDynamic::call: self has no dynamic code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");

        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, valueType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var mapName = JavaInternalName.fromClass(Map.class);
        var getDesc = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of(JvmConstants.DESC_OBJECT));

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(keyDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "get", getDesc));
        var valueConsumeRes = GenAspectConsumeDynamic.get(xx, loc, valueType);
        if (valueConsumeRes.hasErrors()) return valueConsumeRes.reerr();
        var valueDesc =
                valueConsumeRes.requireValue("AspectConsumeDynamic.get value").aspectConsumeDynamicJvmClass(xx);
        if (valueDesc.value().startsWith("L") && !valueDesc.value().equals("Ljava/lang/Object;")) {
            seq.add(JavaBytecodeUtils.cast(loc, valueDesc));
        }
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
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_MAP_GET_FN_DYNAMIC_IDENTITY, TypeMapGetFnDynamic.class);
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
