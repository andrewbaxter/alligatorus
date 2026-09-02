package compilerxyz.main.eval.value.object_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.TypeImmutable;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.main.eval.value.source_interface.BuiltinInterfaceRegistry;
import compilerxyz.main.eval.value.source_interface.InterfaceData;
import compilerxyz.main.gen.AspectAspect;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectAspect;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenDynamicTypes;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
@BuiltinFull(uniqueId = 6130L)
public final class TypeObjectDynamic
        implements TypeImmutable,
                AspectFieldUseDynamic,
                TypeAspect,
                AspectAspect,
                AspectField,
                AspectConsumeDynamic,
                AspectDropDynamic,
                AspectDropDynamicLight,
                AspectBind,
                AspectBindDynamic,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectLift,
                AspectStackValueDynamic {

    private final TypeObjectMeta meta;
    private final JavaInternalName jvmClass;
    private final JavaDataDescriptor jvmClassDesc;
    private final Class hostClass;
    private final IList<ObjectDataField> dataFields;
    private final IList<ObjectMethod> methods;
    private final ObjectDropKind dropKind;
    private final Opt<JavaInternalName> interfaceRepresented;

    public TypeObjectDynamic(
            TypeObjectMeta meta,
            JavaInternalName jvmClass,
            JavaDataDescriptor jvmClassDesc,
            Class hostClass,
            ObjectDropKind dropKind,
            Opt<JavaInternalName> interfaceRepresented) {
        this.meta = meta;
        this.jvmClass = jvmClass;
        this.jvmClassDesc = jvmClassDesc;
        this.hostClass = hostClass;
        this.dataFields = null;
        this.methods = null;
        this.dropKind = dropKind;
        this.interfaceRepresented = interfaceRepresented;
    }

    public void linkInit(IList<ObjectDataField> dataFields, IList<ObjectMethod> methods) {
        if (this.dataFields != null || this.methods != null) {
            throw new IllegalStateException("TypeObjectDynamic.linkInit: already initialized");
        }
        try {
            var df = TypeObjectDynamic.class.getDeclaredField("dataFields");
            df.setAccessible(true);
            df.set(this, dataFields);
            var m = TypeObjectDynamic.class.getDeclaredField("methods");
            m.setAccessible(true);
            m.set(this, methods);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("TypeObjectDynamic.linkInit: failed to set final list fields", e);
        }
    }

    public IList<ObjectMethod> methods() {
        return methods;
    }

    public Opt<JavaInternalName> interfaceRepresented() {
        return interfaceRepresented;
    }

    public String typeDesc() {
        return meta.typeDesc();
    }

    public IList<Unique> trivialIdentityAspects() {
        return meta.trivialIdentityAspects();
    }

    public IList<TypeObjectCastRule> castRules() {
        return meta.castRules();
    }

    public IList<TypeObjectCastFromRule> castFromRules() {
        return meta.castFromRules();
    }

    public Opt<ObjectAspectSource> aspectSource() {
        return meta.aspectSource();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY, TypeObjectDynamic.class);
        return oOpt.optIsSome()
                && hostClass.getName().equals(oOpt.optUnwrap().hostClass.getName())
                && meta.typeDesc().equals(oOpt.optUnwrap().meta.typeDesc());
    }

    @Override
    public Unique typeAspectId() {
        var aspSrcOpt = aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::id: TypeObjectDynamic is not an aspect metatype");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static s -> staticSrc = s;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(typeDesc() + "::id: aspect id is not statically known");
        }
        return staticSrc.id();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.OBJECT_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeObjectDynamic obj)) {
                return EResult.err(
                        loc,
                        "TypeObjectDynamic::exporterRootDelegateSelfExport: expected TypeObjectDynamic,"
                                + " got "
                                + self.getClass());
            }
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueString(obj.hostClass.getName()))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            JsonValueString s;
            switch (data) {
                case JsonValueString js -> s = js;
                case JsonValueNull ignored -> {
                    return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data);
                }
                case JsonValueBoolean ignored -> {
                    return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data);
                }
                case JsonValueNumber ignored -> {
                    return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data);
                }
                case JsonValueArray ignored -> {
                    return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data);
                }
                case JsonValueObject ignored -> {
                    return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data);
                }
            }
            var found = GenDynamicTypes.byClassName(s.value());
            if (found.optIsNone()) {
                return EResult.err(
                        loc, "TypeObjectDynamic::import: no registered dynamic type for host class " + s.value());
            }
            return EResult.ok(MustCast.mustCast(Artifactable.class, found.optUnwrap()));
        }
    };

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(this);
        }
        if ((!dataFields.isEmpty() || (!methods.isEmpty() && interfaceRepresented.optIsNone()))
                && key.equals(GenAspectField.AspectId)) {
            return Opt.some(this);
        }
        if (aspectSource().optIsSome() && key.equals(GenAspectAspect.AspectId)) {
            return Opt.some(this);
        }
        switch (dropKind) {
            case DYNAMIC -> {
                if (key.equals(GenAspectDropDynamic.AspectId)) return Opt.some(this);
            }
            case DYNAMIC_LIGHT -> {
                if (key.equals(GenAspectDropDynamicLight.AspectId)) return Opt.some(this);
            }
        }
        for (var idAspect : trivialIdentityAspects()) {
            if (key.equals(idAspect)) return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.of(DynUtil.renderSource(xx, loc, source)), EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public Unique aspectAspectGetUnique(InterfaceData value) {
        var aspSrcOpt3 = aspectSource();
        if (aspSrcOpt3.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::getUnique: TypeObjectDynamic does not represent an aspect");
        }
        return switch (aspSrcOpt3.optUnwrap()) {
            case ObjectAspectSource.Static staticSrc -> staticSrc.id();
            case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
        };
    }

    @Override
    public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
        var aspSrcOpt4 = aspectSource();
        if (aspSrcOpt4.optIsNone()) {
            throw new UnsupportedOperationException(
                    typeDesc() + "::getIfaceData: TypeObjectDynamic has no aspect source");
        }
        return switch (aspSrcOpt4.optUnwrap()) {
            case ObjectAspectSource.Static ignored ->
                throw new UnsupportedOperationException(
                        typeDesc() + "::getIfaceData: static aspect has no InterfaceData");
            case ObjectAspectSource.FromInterfaceData ignored -> value;
        };
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        for (var rule : castFromRules()) {
            if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) return true;
        }
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        for (var rule : castFromRules()) {
            if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) {
                return rule.typeObjectCastFromRuleApply(xx, this, cv);
            }
        }
        return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (interfaceRepresented.optIsSome()
                && destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                        .optIsSome()) {
            return true;
        }
        for (var idAspect : trivialIdentityAspects()) {
            if (destType.typeAspect(xx, idAspect).optIsSome()) return true;
        }
        for (var rule : castRules()) {
            if (rule.typeObjectCastRuleMatches(xx, destType)) return true;
        }
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (interfaceRepresented.optIsSome()
                && destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                        .optIsSome()) {
            var sourceCode = cv.code().codeDynamic();
            if (sourceCode == null) {
                return EResult.err(
                        loc, "cast " + typeDesc() + " → " + destType.typeDesc() + ": source has no runtime code");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(sourceCode);
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.TYPE_OBJECT_DYNAMIC_GET_INTERFACE_DATA));
            var call = new CodeDynamic(loc, seq);
            return EResult.ok(new CodeValue(Code.of(call), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
        for (var idAspect : trivialIdentityAspects()) {
            if (destType.typeAspect(xx, idAspect).optIsSome()) {
                return EResult.ok(cv);
            }
        }
        for (var rule : castRules()) {
            if (rule.typeObjectCastRuleMatches(xx, destType)) {
                return rule.typeObjectCastRuleApply(xx, this, loc, cv, destType);
            }
        }
        return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        ConstKey.Str ckStr;
        switch (ck) {
            case ConstKey.Str s -> ckStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(loc, typeDesc() + ": unknown field " + ck.constKeyToCanonical());
            }
        }
        var keyStr = ckStr.value();
        var dyn = self_.code().codeDynamic();
        if (dyn == null) {
            return EResult.err(loc, typeDesc() + ": self has no dynamic code");
        }
        for (var f : dataFields) {
            if (f.langName().equals(keyStr)) {
                var loadSeq = new JavaBytecodeSequence();
                loadSeq.add(dyn);
                switch (f.access()) {
                    case ObjectDataField.Access.FieldGet fg ->
                        loadSeq.add(JavaBytecodeUtils.accessField(loc, jvmClass, fg.jvmFieldName(), fg.jvmFieldDesc()));
                    case ObjectDataField.Access.MethodCall mc ->
                        loadSeq.add(
                                JavaBytecodeUtils.callMethod(loc, jvmClass, mc.jvmMethodName(), mc.jvmMethodDesc()));
                }
                return EResult.ok(new CodeValue(
                        Code.of(new CodeDynamic(loc, loadSeq)),
                        EvalOutcome.ofValue(ValueDynamic.stack(xx, f.fieldType()))));
            }
        }
        if (interfaceRepresented.optIsNone()) {
            for (var m : methods) {
                if (m.langName().equals(keyStr)) {
                    return EResult.ok(
                            new CodeValue(self_.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, m.funcType()))));
                }
            }
        }
        return EResult.err(loc, typeDesc() + ": unknown field '" + keyStr);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        ConstKey.Str keyStr;
        switch (key) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return Opt.none();
            }
        }
        var name = keyStr.value();
        for (var f : dataFields) {
            if (f.langName().equals(name)) return Opt.some(f.fieldType());
        }
        if (interfaceRepresented.optIsNone()) {
            for (var m : methods) {
                if (m.langName().equals(name)) return Opt.some(m.funcType());
            }
        }
        return Opt.none();
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dynamicConsumeToDynamic(loc, cv);
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return jvmClassDesc;
    }

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(pairedConst(), externalHostValue.optUnwrap()));
    }

    public TypeObjectConst pairedConst() {
        return new TypeObjectConst(meta, hostClass);
    }

    @CodegenDescriptor
    public InterfaceData getInterfaceData() {
        var aspSrcOpt = aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::getInterfaceData: missing aspect source");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static s -> staticSrc = s;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(typeDesc() + "::getInterfaceData: missing static aspect source");
        }
        long _v =
                switch (staticSrc.id()) {
                    case Unique.Builtin b -> b.id();
                    case Unique.Source s ->
                        throw new IllegalStateException(typeDesc() + "::getInterfaceData: not a builtin interface");
                };
        BuiltinInterfaceKind kind;
        if (_v == 1015L) kind = BuiltinInterfaceKind.ASPECT_FIELD;
        else if (_v == 1037L) kind = BuiltinInterfaceKind.ASPECT_FIELD_USE_DYNAMIC;
        else if (_v == 1039L) kind = BuiltinInterfaceKind.ASPECT_ELEMENT_USE_DYNAMIC;
        else if (_v == 1017L) kind = BuiltinInterfaceKind.ASPECT_SET;
        else if (_v == 1018L) kind = BuiltinInterfaceKind.ASPECT_OPERATOR_ADD;
        else if (_v == 1020L) kind = BuiltinInterfaceKind.ASPECT_OPERATOR_LT;
        else if (_v == 1042L) kind = BuiltinInterfaceKind.ASPECT_OPERATOR_MOD;
        else if (_v == 1022L) kind = BuiltinInterfaceKind.ASPECT_BIND;
        else if (_v == 1023L) kind = BuiltinInterfaceKind.ASPECT_BIND_DYNAMIC;
        else if (_v == 1024L) kind = BuiltinInterfaceKind.ASPECT_CONSUME_DYNAMIC;
        else if (_v == 1032L) kind = BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC;
        else if (_v == 1035L) kind = BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC_LIGHT;
        else if (_v == 1021L) kind = BuiltinInterfaceKind.ASPECT_INITIAL_BIND;
        else if (_v == 1026L) kind = BuiltinInterfaceKind.ASPECT_LIFT;
        else if (_v == 1116L) kind = BuiltinInterfaceKind.ASPECT_FUNCTION;
        else if (_v == 2001L) kind = BuiltinInterfaceKind.VALUE;
        else if (_v == 2002L || _v == 6006L) kind = BuiltinInterfaceKind.TYPE;
        else if (_v == 2005L) kind = BuiltinInterfaceKind.BINDING;
        else if (_v == 2006L) kind = BuiltinInterfaceKind.TARGET;
        else if (_v == 2007L) kind = BuiltinInterfaceKind.CODE_DYN;
        else throw new IllegalStateException(typeDesc() + "::getInterfaceData: unrecognized builtin id " + _v);
        return BuiltinInterfaceRegistry.resolve(kind);
    }
}
