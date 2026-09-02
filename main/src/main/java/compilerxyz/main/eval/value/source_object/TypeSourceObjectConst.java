package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
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
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6137L)
public record TypeSourceObjectConst(TypeSourceObjectDynamic dynamic)
        implements Type,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectInitialBind,
                AspectType {

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
    }

    @Override
    public Type aspectTypeType(Value value) {
        if (!(value instanceof ValueObjectConst voc)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: expected ValueObjectConst, got "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        if (!(voc.data() instanceof Type t)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: wrapped data is not a Type, got "
                    + (voc.data() == null ? "null" : voc.data().getClass().getName()));
        }
        return t;
    }

    @Override
    public String typeDesc() {
        return dynamic.typeDesc();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY)
                || key.equals(dynamic.identity)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            for (var impl : dynamic.interfaceImpls) {
                if (impl.interface_.builtinKind.optIsSome()
                        && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.TYPE) {
                    return Opt.some(this);
                }
            }
            return Opt.none();
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return dynamic.typeAspect(xx, key);
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return dynamic.aspectConsumeDynamicToDynamic(xx, loc, cv);
    }

    @Override
    public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        TypeSourceObjectDynamic dynType = dynamic;
        if (!self.code().codeIsNone()) {
            return dynType.aspectFieldField(xx, loc, self, key);
        }
        var selfCodeRes = aspectConsumeDynamicToDynamic(xx, loc, self);
        if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
        var selfCode = selfCodeRes.requireValue("TypeSourceObjectConst::field toDynamic");
        var selfDynamic = new CodeValue(Code.of(selfCode), EvalOutcome.ofValue(ValueDynamic.stack(xx, dynType)));
        return dynType.aspectFieldField(xx, loc, selfDynamic, key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeSourceObjectConst.class);
        return oOpt.optIsSome() && dynamic.typeIsEqual(xx, oOpt.optUnwrap().dynamic);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return dynamic.typeCanCastFrom(xx, loc, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return dynamic.typeCastFrom(xx, loc, cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return dynamic.typeCanCastTo(xx, loc, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return dynamic.typeCastTo(xx, loc, cv, destType);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return dynamic.artifactableExporter(xx, loc);
    }
}
