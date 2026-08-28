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
import compilerxyz.artifact.Artifact;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.value.object_.ValueObjectConst;

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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.ARGUMENT_TUPLE_IDENTITY, ArgumentTuple.class);
    if (oOpt.optIsNone()) return false;
    var o = oOpt.optUnwrap();
    if (elementTypes.size() != o.elementTypes.size()) return false;
    for (int i = 0; i < elementTypes.size(); i++) {
      if (!elementTypes.get(i).typeIsEqual(xx, o.elementTypes.get(i))) return false;
    }
    return true;
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(
      ExprContext xx, QExprId loc) {
    return Value.exportArtifact(xx, loc, this);
  }
}
