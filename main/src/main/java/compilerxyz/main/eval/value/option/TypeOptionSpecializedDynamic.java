package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaCache;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class TypeOptionSpecializedDynamic<T extends Type> implements Aspect, OptionDynamic {
    public final T elementType;
    public final JavaInternalName storeName;
    public final Unique identity;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeOptionSpecializedDynamic(T elementType, JavaInternalName storeName, Unique identity) {
        this.elementType = elementType;
        this.storeName = storeName;
        this.identity = identity;
    }

    @Override
    public ArtifactSupport.ArtifactIdHolder optionDynamicArtifactIdCell() {
        return artifactIdCell;
    }

    @Override
    public Type optionDynamicElementType() {
        return elementType;
    }

    public JavaDataDescriptor storeDescriptor() {
        return JavaDataDescriptor.fromInternalName(storeName);
    }

    public JavaDataDescriptor elementDescriptor(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, elementType).optUnwrap().aspectConsumeDynamicJvmClass(xx);
    }

    @Override
    public CodeDynamicReg optionDynamicConstruct(ExprContext xx, QExprId loc, Opt<JavaBytecodeReg> payload) {
        var b = new JavaBytecodeBuilder(loc);
        if (payload.optIsSome()) {
            return b.code(b.invokeStatic(
                    storeName,
                    "some",
                    JavaMethodDescriptor.fromParts(storeDescriptor(), TList.of(elementDescriptor(xx))),
                    payload.optUnwrap()));
        }
        return b.code(b.invokeStatic(storeName, "none", JavaMethodDescriptor.fromParts(storeDescriptor(), TList.of())));
    }

    @Override
    public String typeDesc() {
        return "option(" + elementType.typeDesc() + ")";
    }

    private final MetaCache meta = new MetaCache("option_specialized_dynamic");

    @Override
    public Meta typeMeta(ExprContext xx) {
        return meta.get(xx, this);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicOptionDynamic(this));
        }
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(new AspectUnionOptionSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldOptionSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicOptionSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicOptionDynamic(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindOptionDynamic(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(new AspectSetOptionDynamic(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return switch (GenAspectLift.getMaybe(xx, elementType)) {
                case Opt.Some<AspectLift> elementLift ->
                    Opt.some(new AspectLiftOptionSpecializedDynamic<>(this, elementLift.value()));
                case Opt.None<AspectLift> none -> Opt.none();
            };
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicOptionDynamic(this));
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
    }

    private Opt<TypeOptionDynamic<?>> boxedTwin(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_OPTION_DYNAMIC_IDENTITY, TypeOptionDynamic.class);
        if (oOpt.optIsNone()) return Opt.none();
        var boxedElement = DynUtil.substituteBoxed(xx, elementType);
        if (!boxedElement.typeIsEqual(xx, oOpt.optUnwrap().elementType)) return Opt.none();
        return Opt.some(oOpt.optUnwrap());
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType) || boxedTwin(xx, sourceType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && typeIsEqual(xx, cv.value().optUnwrap().valueType())) {
            return EResult.ok(cv);
        }
        var sourceRes = DynUtil.valueReg(loc, cv);
        if (sourceRes.hasErrors()) return sourceRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var out = b.invokeStatic(
                storeName,
                "fromOpt",
                JavaMethodDescriptor.fromParts(storeDescriptor(), TList.of(JavaDataDescriptor.fromClass(Opt.class))),
                b.reg(sourceRes.requireValue("TypeOptionSpecializedDynamic::typeCastFrom source")));
        return EResult.ok(DynUtil.regToValue(b.code(), out, this));
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return boxedTwin(xx, destType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var twin = boxedTwin(xx, destType);
        if (twin.optIsNone()) {
            return EResult.err(loc, "Cannot convert " + typeDesc() + " to " + destType.typeDesc());
        }
        var sourceRes = DynUtil.valueReg(loc, cv);
        if (sourceRes.hasErrors()) return sourceRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var out = b.invokeVirtual(
                storeName,
                "toOpt",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Opt.class), TList.of()),
                b.reg(sourceRes.requireValue("TypeOptionSpecializedDynamic::typeCastTo source")));
        return EResult.ok(DynUtil.regToValue(b.code(), out, twin.optUnwrap()));
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.OPTION_DYNAMIC);
    }
}
