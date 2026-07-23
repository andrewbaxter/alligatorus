package compilerxyz.eval.value.source_object;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

/**
 * Compile-time type of a source-object *instance* — the value produced by lifting a runtime
 * source-object into a compile-time Value. Paired with {@link TypeSourceObjectDynamic} which
 * describes the same source-object at codegen/runtime time.
 *
 * <p>Distinct from {@link TypeTypeSourceObjectConst} which is the meta-type of type-descriptor
 * values (i.e., the type of TypeSourceObjectDynamic-valued expressions themselves).
 */
@BuiltinTypeId(7238L)
public record TypeSourceObjectConst(TypeSourceObjectDynamic dynamic)
    implements Type,
        compilerxyz.gen.AspectArtifact,
        compilerxyz.gen.AspectType,
        compilerxyz.artifact.ExporterAny {

  @Override
  public compilerxyz.artifact.ArtifactExporter aspectArtifactExporter() {
    return compilerxyz.artifact.ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
  }

  @Override
  public Type aspectTypeType(compilerxyz.gen.Value value) {
    if (!(value instanceof compilerxyz.eval.value.object_.ValueObjectConst voc)) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: expected ValueObjectConst, got "
              + (value == null ? "null" : value.getClass().getName()));
    }
    if (!(voc.data() instanceof Type t)) {
      throw new IllegalStateException(
          "TypeSourceObjectConst::type_: wrapped data is not a Type, got "
              + (voc.data() == null ? "null" : voc.data().getClass().getName()));
    }
    return t;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(dynamic);
  }

  @Override
  public EResult<compilerxyz.gen.Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(dynamic));
  }


  @Override
  public TypeId hasTypeIdTypeId() {
    return new TypeId.Composite(
        compilerxyz.gen.GenTypeIds.TYPE_SOURCE_OBJECT_CONST_TYPE_ID,
        compilerxyz.util.TList.of(dynamic.hasTypeIdTypeId()));
  }

  @Override
  public String typeDesc() {
    return dynamic.typeDesc();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(compilerxyz.gen.GenAspectArtifact.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.GenAspectType.AspectId)) {
      for (var impl : dynamic.interfaceImpls) {
        if (impl.interface_.builtinKind
            == compilerxyz.eval.value.source_interface.BuiltinInterfaceKind.TYPE) {
          return Opt.some(this);
        }
      }
      return Opt.none();
    }
    return dynamic.typeAspect(xx, key);
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.Type other) {
    return other instanceof TypeSourceObjectConst o && dynamic.typeIsEqual(o.dynamic);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return dynamic.typeCanCastFrom(xx, loc, sourceType);
  }

  @Override
  public EResult<compilerxyz.eval.CodeValue> typeCastFrom(ExprContext xx, QExprId loc, compilerxyz.eval.CodeValue cv) {
    return dynamic.typeCastFrom(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return dynamic.typeCanCastTo(xx, loc, destType);
  }

  @Override
  public EResult<compilerxyz.eval.CodeValue> typeCastTo(ExprContext xx, QExprId loc, compilerxyz.eval.CodeValue cv, Type destType) {
    return dynamic.typeCastTo(xx, loc, cv, destType);
  }
}
