package compilerxyz.eval.value.bind_result;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class BindResultType {

  private BindResultType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/gen/AspectBind$BindResult");
      var dataFields = new TList<ObjectDataField>();
      dataFields.add(
          new ObjectDataField(
              "code",
              "code",
              JavaDataDescriptor.fromClass(compilerxyz.eval.Code.class),
              compilerxyz.eval.Code.dynamicType()));
      dataFields.add(
          new ObjectDataField(
              "binding",
              "binding",
              JavaDataDescriptor.fromClass(compilerxyz.eval.Binding.class),
              compilerxyz.eval.Binding.dynamicType()));
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_BIND_RESULT_DYNAMIC_TYPE_ID,
          "BindResult",
          jvmClass,
          JavaDataDescriptor.fromClass(AspectBind.BindResult.class),
          dataFields,
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
