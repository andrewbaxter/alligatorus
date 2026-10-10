package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class TypeListSpecializedDynamic<T extends Type> implements Aspect, ListDynamic {

    public final T elementType;
    public final JavaInternalName storeName;
    public final Unique identity;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeListSpecializedDynamic(T elementType, JavaInternalName storeName, Unique identity) {
        this.elementType = elementType;
        this.storeName = storeName;
        this.identity = identity;
    }

    @Override
    public ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell() {
        return artifactIdCell;
    }

    public JavaDataDescriptor elementDescriptor(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, elementType).optUnwrap().aspectConsumeDynamicJvmClass(xx);
    }

    public JavaMethodDescriptor pushDescriptor(ExprContext xx) {
        return JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(elementDescriptor(xx)));
    }

    public JavaDataDescriptor storeDescriptor() {
        return JavaDataDescriptor.fromInternalName(storeName);
    }

    public CodeDynamicReg emitNew(ExprContext xx, QExprId loc, TList<CodeDynamicReg> elements) {
        var pushDesc = pushDescriptor(xx);
        var b = new JavaBytecodeBuilder(loc);
        var regs = new TList<JavaBytecodeReg>(elements.size());
        for (var element : elements) regs.add(b.reg(element));
        var list = b.newInstance(storeName, JavaMethodDescriptor.fromConstructorParts(TList.of()));
        for (var reg : regs) b.invokeVirtualVoid(storeName, "push", pushDesc, list, reg);
        return b.code(list);
    }

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Meta typeMeta(ExprContext xx) {
        return MetaSupport.derive(xx, this, "list_specialized_dynamic");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectLift.AspectId)) {
            return switch (GenAspectLift.getMaybe(xx, elementType)) {
                case Opt.Some<AspectLift> elementLift ->
                    Opt.some(new AspectLiftListSpecializedDynamic<>(this, elementLift.value()));
                case Opt.None<AspectLift> none -> Opt.none();
            };
        }
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicListSpecializedDynamic<>(this));
        }
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicListSpecializedDynamic<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType) || ListCastFrom.canCast(xx, loc, elementType, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && typeIsEqual(xx, cv.value().optUnwrap().valueType())) {
            return EResult.ok(cv);
        }
        return ListCastFrom.cast(
                xx, loc, this, elementType, new ListCastFrom.Store(storeName, "push", pushDescriptor(xx)), cv);
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
        return new AspectDynamicConstListSpecializedDynamic<>(this)
                .aspectDynamicConstConstForm()
                .artifactableExporter(xx, loc);
    }
}
