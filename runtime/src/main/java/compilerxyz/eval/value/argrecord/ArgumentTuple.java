package compilerxyz.eval.value.argrecord;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.stream.Collectors;

public record ArgumentTuple(TList<Binding> elements, TList<Type> elementTypes)
    implements Type, Value, ExporterAny {

  @Override
  public String typeDesc() {
    var inner = elementTypes.stream().map(Type::typeDesc).collect(Collectors.joining(", "));
    return "argument_tuple(" + inner + ")";
  }

  @Override
  public Type valueType() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    return Opt.none();
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
