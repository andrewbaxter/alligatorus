package compilerxyz.eval.value.argrecord;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectTuple;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.stream.Collectors;

public record ArgumentTuple(TList<Binding> elements, TList<Type> elementTypes)
    implements Type, Value, AspectTuple, ExporterAny {

  @Override
  public String typeDesc() {
    var inner = elementTypes.stream().map(Type::typeDesc).collect(Collectors.joining(", "));
    return "argument_tuple(" + inner + ")";
  }

  @Override
  public Type valueTypeOf() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    return key.equals(AspectTuple.ID) ? Opt.some(this) : Opt.none();
  }

  @Override
  public TList<Type> elementTypes() {
    return elementTypes;
  }

  @Override
  public TList<EResult<CodeValue>> takeElements(ExprContext xx, QExprId loc, Value value) {
    if (!(value instanceof ArgumentTuple at)) {
      throw new IllegalArgumentException(
          "ArgumentTuple::takeElements: expected ArgumentTuple value, got " + value.getClass());
    }
    var out = new TList<EResult<CodeValue>>(at.elements.size());
    for (var b : at.elements) {
      out.add(EResult.ok(b.bindingUse_(xx, loc, Code.none(loc))));
    }
    return out;
  }

  @Override
  public TypeId typeId() {
    var ids = new TList<TypeId>(elementTypes.size());
    for (var t : elementTypes) ids.add(t.typeId());
    return new TypeId.Composite(GenTypeIds.ARGUMENT_TUPLE_TYPE_ID, ids);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
