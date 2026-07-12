package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectUnion;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;
import org.jspecify.annotations.Nullable;

public final class TypeSourceUnionDynamic
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        AspectUnion,
        ExporterAny {

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final Counter nextTagId;
  public final TList<SourceUnionVariant> variants;
  public final compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder();

  public @Nullable String compiledBaseBinaryName;
  public @Nullable TList<String> compiledVariantBinaryNames;

  public TypeSourceUnionDynamic(DefinitionSet set, Unique identity) {
    this.set = set;
    this.identity = identity;
    this.nextTagId = new Counter();
    this.variants = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public TypeSourceUnionDynamic(Unique identity) {
    this.set = (DefinitionSet) null;
    this.identity = identity;
    this.nextTagId = new Counter();
    this.variants = new TList<>();
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = TypeSourceUnionDynamic.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "TypeSourceUnionDynamic.initSetReflectively: failed to write final 'set' field", e);
    }
  }

  public EResult<Void> finish(
      ExprContext xx,
      QExprId loc,
      long unionIndex,
      compilerxyz.eval.DefinitionSetClassLoader loader) {
    if (compiledBaseBinaryName != null) {
      return EResult.ok(null);
    }
    var emitted =
        compilerxyz.eval.dynamic.SourceUnionEmitter.emit(xx, set.defSetId, unionIndex, this);
    compiledBaseBinaryName = emitted.baseBinaryName();
    compiledVariantBinaryNames = emitted.variantBinaryNames();
    loader.registerClass(emitted.baseBinaryName(), emitted.baseBytecode());
    for (int i = 0; i < emitted.variantBinaryNames().size(); i++) {
      loader.registerClass(
          emitted.variantBinaryNames().get(i), emitted.variantBytecode().get(i));
    }
    return EResult.ok(null);
  }

  @Override
  public TypeId typeId() {
    return new TypeId.DefsetMember(set.defSetId, uniqueId(identity));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "union_dyn";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceUnionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(identity)
        || key.equals(GenIdentities.UNION_REF_TYPE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectUnion.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeSourceUnionDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public TList<AspectUnion.Variant> aspectUnionVariants() {
    var out = new TList<AspectUnion.Variant>();
    for (var v : variants) {
      out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
    }
    return out;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
