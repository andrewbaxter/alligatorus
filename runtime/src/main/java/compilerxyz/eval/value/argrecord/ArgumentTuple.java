package compilerxyz.eval.value.argrecord;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.stream.Collectors;

@BuiltinLight(uniqueId = 6101L)
public record ArgumentTuple(TList<Binding> elements, TList<Type> elementTypes)
    implements Type, Value, Aspect, ExporterAny {

  @Override
  public String typeDesc() {
    var inner = elementTypes.stream().map(Type::typeDesc).collect(Collectors.joining(", "));
    return "argument_tuple(" + inner + ")";
  }

                public Type valueType() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.ARGUMENT_TUPLE_IDENTITY)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    if (!(AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.ARGUMENT_TUPLE_IDENTITY, ArgumentTuple.class) instanceof Opt.Some<ArgumentTuple> oSome)) return false;
    var o = oSome.value();
    if (elementTypes.size() != o.elementTypes.size()) return false;
    for (int i = 0; i < elementTypes.size(); i++) {
      if (!elementTypes.get(i).typeIsEqual(xx, o.elementTypes.get(i))) return false;
    }
    return true;
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

  @Override
  public compilerxyz.eval.EResult<compilerxyz.artifact.Artifact> exportArtifact(
      compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc) {
    return compilerxyz.gen.Value.super.exportArtifact(xx, loc);
  }
}
