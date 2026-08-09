package compilerxyz.eval.value.object_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueObjectConst(Type type, Object data) implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return type;
  }

  /**
   * The type-of-type of a plain type — a type with no specialized type-of-type
   * (primitives, list/map/option, functions, …). It is the const form of the
   * builtin {@code Type} descriptor. Source kinds (object/union/interface) name
   * their own {@code TypeTypeSource…} type-of-type at the wrap site instead.
   */
  public static Type plainTypeType() {
    var td = (TypeObjectDynamic) compilerxyz.gen.GenType.dynamicType();
    return new TypeObjectConst(
        td.meta(),
        td.hostClass(),
        compilerxyz.util.Opt.<Type>none(),
        compilerxyz.util.Opt.<compilerxyz.artifact.ExporterRoot>none());
  }


  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException(
        "ValueObjectConst::exporterAnyAsType: a wrapped compile-time value is not itself a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
