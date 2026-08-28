package compilerxyz.main.eval.value.zbase32;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.builtin.Zbase32;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.byte_.TypeByteConst;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.eval.value.list.TypeListConst;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinPath("zbase32.decode")
@BuiltinLight(uniqueId = 6153L)
public record TypeZbase32Decode() implements Type, AspectFunction, AspectInitialBind, ExporterAny {

    @Override
    public String typeDesc() {
        return "zbase32.decode";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_ZBASE32_DECODE_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "zbase32.decode: argument must have a value");
        }
        var argValue = argument.value().optUnwrap();
        var argType = argValue.valueType();
        var strConstOpt = GenAspectStringConst.getMaybe(xx, argType);
        if (strConstOpt.optIsSome()) {
            var s = strConstOpt.optUnwrap().aspectStringConstGet(argValue);
            byte[] bytes;
            try {
                bytes = Zbase32.decode(s);
            } catch (IllegalArgumentException e) {
                return EResult.err(loc, "zbase32.decode: " + e.getMessage());
            }
            var elements = new TList<Value>(bytes.length);
            for (var b : bytes) {
                elements.add(new ValueObjectConst(new TypeByteConst(), b));
            }
            var listType = new TypeListConst(new TypeByteDynamic());
            return EResult.ok(new CodeValue(
                    Code.concat(loc, self.code(), argument.code()),
                    EvalOutcome.ofValue(new ValueObjectConst(listType, elements))));
        }
        if (GenAspectStringDynamic.getMaybe(xx, argType).optIsSome()) {
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var argDynRes = consumeRes
                    .requireValue("zbase32.decode AspectConsumeDynamic")
                    .aspectConsumeDynamicToDynamic(xx, loc, argument);
            if (argDynRes.hasErrors()) return argDynRes.reerr();
            var seq = new JavaBytecodeSequence();
            seq.add(argDynRes.requireValue("zbase32.decode arg dynamic"));
            seq.add(JavaBytecodeUtils.callStaticMethod(loc, GenCodegenDescriptors.ZBASE32_DECODE_TO_STORE));
            var listType = GenLists.listDynamic(xx, new TypeByteDynamic());
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(listType, 1))));
        }
        return EResult.err(loc, "zbase32.decode: argument must be a string, got " + argType.typeDesc());
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
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_ZBASE32_DECODE_IDENTITY)
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
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
