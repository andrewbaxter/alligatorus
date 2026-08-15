package compilerxyz.eval.value.source_object;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.Value;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectType;

/**
 * Compile-time type of a source-object *instance* — the value produced by lifting a runtime
 * source-object into a compile-time Value. Paired with {@link TypeSourceObjectDynamic} which
 * describes the same source-object at codegen/runtime time.
 *
 * <p>Distinct from {@link TypeTypeSourceObjectConst} which is the meta-type of type-descriptor
 * values (i.e., the type of TypeSourceObjectDynamic-valued expressions themselves).
 */
@BuiltinLight(uniqueId = 6137L)
public record TypeSourceObjectConst(TypeSourceObjectDynamic dynamic)
    implements Type,
        AspectArtifact,
        AspectType,
        ExporterAny {

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_OBJECT_VALUE_CONST;
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (!(value instanceof ValueObjectConst voc)) {
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
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(new TypeTypeSourceObjectConst(dynamic), dynamic));
  }


  @Override
  public String typeDesc() {
    return dynamic.typeDesc();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectType.AspectId)) {
      for (var impl : dynamic.interfaceImpls) {
        if (impl.interface_.builtinKind.optUnwrapOr(null)
            == BuiltinInterfaceKind.TYPE) {
          return Opt.some(this);
        }
      }
      return Opt.none();
    }
    return dynamic.typeAspect(xx, key);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeSourceObjectConst.class);
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
}
