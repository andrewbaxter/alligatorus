package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

@BuiltinLight(uniqueId = 6196L)
public record TypeTypeSourceInterfaceDynamic(Opt<TypeSourceInterfaceDynamic> type_)
        implements Type,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectInitialBind,
                AspectLift,
                AspectType,
                ExporterAny {

    public static final TypeTypeSourceInterfaceDynamic GENERIC = new TypeTypeSourceInterfaceDynamic(Opt.none());

    private TypeSourceInterfaceDynamic require(String op) {
        if (type_.optIsSome()) return type_.optUnwrap();
        throw new IllegalStateException(
                "TypeTypeSourceInterfaceDynamic::" + op + " on the generic (interface-agnostic) descriptor");
    }

    @Override
    public String typeDesc() {
        if (type_.optIsSome()) return type_.optUnwrap().typeDesc();
        return "interface";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)
                || key.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectType.AspectId)) {
            return Opt.some(this);
        }
        if (type_.optIsSome() && key.equals(type_.optUnwrap().data().identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                        .optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY)
                        .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                .optIsSome()) {
            var sourceCode = cv.code().codeDynamic();
            if (sourceCode == null) {
                return EResult.err(loc, "cast interface → interface_data: source has no runtime code");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(sourceCode);
            seq.add(JavaBytecodeUtils.callMethod(
                    loc, GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_DATA));
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
        return EResult.err(loc, "Cannot cast interface to " + destType.typeDesc());
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        var host = (TypeSourceInterfaceDynamic) externalHostValue.optUnwrap();
        return EResult.ok(new ValueObjectConst(new TypeTypeSourceInterfaceDynamic(Opt.some(host)), host));
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(TypeSourceInterfaceDynamic.class);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        return EResult.err(loc, "TypeTypeSourceInterfaceDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var type = require("aspectFieldField");
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeTypeSourceInterfaceDynamic::field: key has no compile-time value");
        }
        var ckRes = Eval.extractConstKey(xx, key.value().optUnwrap());
        ConstKey ck;
        switch (ckRes) {
            case Result.Ok<ConstKey, String> ok -> ck = ok.value();
            case Result.Err<ConstKey, String> e -> {
                return EResult.err(loc, e.error());
            }
        }
        ConstKey.Str keyStr;
        switch (ck) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(loc, "Method " + ck.constKeyToCanonical() + " not found on interface");
            }
        }
        var keyName = keyStr.value();
        InterfaceMethodData match = null;
        for (var m : type.data().methods) {
            if (m.name.equals(keyName)) {
                match = m;
                break;
            }
        }
        if (match == null) {
            return EResult.err(loc, "Method '" + keyName + "' not found on interface");
        }
        var refType = new TypeInterfaceMethodDynamic(type.data(), match);
        Code carry = self.code();
        if (carry.codeDynamic() != null) {
            var dropRes = DynUtil.dropDefault(loc, self);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var seq = new JavaBytecodeSequence();
            seq.add(carry.codeDynamic());
            var dropDyn = dropRes.requireValue("TypeTypeSourceInterfaceDynamic::field drop")
                    .codeDynamic();
            if (dropDyn != null) seq.add(dropDyn);
            carry = Code.of(new CodeDynamic(loc, seq));
        }
        return EResult.ok(new CodeValue(carry, EvalOutcome.ofValue(new ValueObjectConst(refType, refType))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (type_.optIsNone()) {
            return Opt.none();
        }
        ConstKey.Str s;
        switch (key) {
            case ConstKey.Str ks -> s = ks;
            case ConstKey.Uint u -> {
                return Opt.none();
            }
        }
        for (var m : type_.optUnwrap().data().methods) {
            if (m.name.equals(s.value())) {
                return Opt.some(new TypeInterfaceMethodDynamic(type_.optUnwrap().data(), m));
            }
        }
        return Opt.none();
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, new TypeTypeSourceInterfaceDynamic(type_));
    }

    @Override
    public Type aspectTypeType(Value value) {
        if (value instanceof ValueObjectConst voc && voc.data() instanceof Type t) {
            return t;
        }
        return require("aspectTypeType");
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
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
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY,
                TypeTypeSourceInterfaceDynamic.class);
        if (oOpt.optIsNone()) {
            return false;
        }
        if (type_.optIsSome() && oOpt.optUnwrap().type_.optIsSome()) {
            return type_.optUnwrap().typeIsEqual(xx, oOpt.optUnwrap().type_.optUnwrap());
        }
        return type_.optIsNone() && oOpt.optUnwrap().type_.optIsNone();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
