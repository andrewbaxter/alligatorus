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
   * Wrap a Java Type instance as a compile-time Value using its per-class META
   * (the TOC returned by {@link compilerxyz.eval.Type#dynamicType()}
   * .{@code aspectLiftLift}-style paired-const construction). This is the
   * standard replacement wherever a Type used to be passed as a Value when
   * Type extended Value.
   */
  public static Value ofType(Type t) {
    var td = (compilerxyz.eval.value.object_.TypeObjectDynamic) Type.dynamicType();
    var paired = new TypeObjectConst(td.meta(), td.hostClass(), compilerxyz.util.Opt.<Type>none());
    return new ValueObjectConst(paired, t);
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
