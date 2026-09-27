package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.TypeImmutable;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.gen.GenAspectAspect;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenBuiltinInterfaces;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenDynamicTypes;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
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
import java.util.function.Function;

@SuppressWarnings("rawtypes")
@BuiltinFull(uniqueId = 6130L)
public final class TypeObjectDynamic implements Aspect, TypeImmutable, TypeAspect {

    private final TypeObjectMeta meta;
    final JavaInternalName jvmClass;
    final JavaDataDescriptor jvmClassDesc;
    private final Class hostClass;
    final Opt<IList<ObjectDataField>> dataFields;
    final Opt<IList<ObjectMethod>> methods;
    private final ObjectDropKind dropKind;
    final Opt<JavaInternalName> interfaceRepresented;

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
        this.dataFields = Opt.none();
        this.methods = Opt.none();
        this.dropKind = dropKind;
        this.interfaceRepresented = interfaceRepresented;
    }

    public void linkInit(IList<ObjectDataField> dataFields, IList<ObjectMethod> methods) {
        if (this.dataFields.optIsSome() || this.methods.optIsSome()) {
            throw new IllegalStateException("TypeObjectDynamic.linkInit: already initialized");
        }
        try {
            var df = TypeObjectDynamic.class.getDeclaredField("dataFields");
            df.setAccessible(true);
            df.set(this, Opt.some(dataFields));
            var m = TypeObjectDynamic.class.getDeclaredField("methods");
            m.setAccessible(true);
            m.set(this, Opt.some(methods));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("TypeObjectDynamic.linkInit: failed to set final list fields", e);
        }
    }

    public boolean isLinked() {
        return methods.optIsSome();
    }

    public IList<ObjectDataField> dataFields() {
        return dataFields.optUnwrap();
    }

    public IList<ObjectMethod> methods() {
        return methods.optUnwrap();
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
        if (other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY)
                .optIsNone()) return false;
        for (var idAspect : trivialIdentityAspects()) {
            if (other.typeAspect(xx, idAspect).optIsNone()) return false;
        }
        return true;
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
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicObjectDynamic(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicObjectDynamic(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(new AspectLiftObjectDynamic(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicObjectDynamic(this));
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicObjectDynamic(this));
        }
        if ((!dataFields().isEmpty() || (!methods().isEmpty() && interfaceRepresented.optIsNone()))
                && key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldObjectDynamic(this));
        }
        if (aspectSource().optIsSome() && key.equals(GenAspectAspect.AspectId)) {
            return Opt.some(new AspectAspectObjectDynamic(this));
        }
        if (meta.identityEq() && key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqObjectIdentity.INSTANCE);
        }
        switch (dropKind) {
            case DYNAMIC -> {
                if (key.equals(GenAspectDropDynamic.AspectId)) {
                    return Opt.some(AspectDropDynamicObjectDynamic.INSTANCE);
                }
            }
            case NONE -> {}
        }
        for (var idAspect : trivialIdentityAspects()) {
            if (key.equals(idAspect)) return Opt.some(this);
        }
        return Opt.none();
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
                return rule.typeObjectCastFromRuleApply(this, cv);
            }
        }
        return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (interfaceRepresented.optIsSome()
                && destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
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
                && destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
                        .optIsSome()) {
            var sourceRes = DynUtil.valueReg(loc, cv);
            if (sourceRes.hasErrors()) return sourceRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeVirtual(
                    GenCodegenDescriptors.TYPE_OBJECT_DYNAMIC_GET_INTERFACE_CELL,
                    b.reg(sourceRes.requireValue("sourceCode")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
        for (var idAspect : trivialIdentityAspects()) {
            if (destType.typeAspect(xx, idAspect).optIsSome()) {
                return EResult.ok(cv);
            }
        }
        for (var rule : castRules()) {
            if (rule.typeObjectCastRuleMatches(xx, destType)) {
                return rule.typeObjectCastRuleApply(xx, loc, cv, destType);
            }
        }
        return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
    }

    public TypeObjectConst pairedConst() {
        return new TypeObjectConst(meta, hostClass);
    }

    @CodegenDescriptor
    public InterfaceCell getInterfaceCell() {
        var aspSrcOpt = aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::getInterfaceCell: missing aspect source");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static st -> staticSrc = st;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(typeDesc() + "::getInterfaceCell: missing static aspect source");
        }
        var data = GenBuiltinInterfaces.byIdentity(staticSrc.identity());
        if (data.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::getInterfaceCell: not a builtin interface type");
        }
        return InterfaceCell.ofBuiltin(data.optUnwrap());
    }
}
