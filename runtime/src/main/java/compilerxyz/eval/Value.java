package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface Value {

  default Type valueTypeOf() {
    throw new AssertionError(
        "user did not provide a Value::valueTypeOf method on " + getClass().getName());
  }

  static Type dynamicType() {
    return ValueDynamicType.INSTANCE;
  }

  final class ValueDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private ValueDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/eval/Value");
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_VALUE_DYNAMIC_TYPE_ID,
          "value",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          new TList<Unique>(),
          new TList<TypeObjectCastRule>(),
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
