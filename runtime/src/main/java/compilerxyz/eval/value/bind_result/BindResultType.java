package compilerxyz.eval.value.bind_result;

import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTrivialIdentityAspect;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Binding;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinTrivialIdentityAspect(6019L)
@BuiltinPath("types.bind_result")
public final class BindResultType {

  private BindResultType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(AspectBind.BindResult.class);
      var dataFields = new TList<ObjectDataField>();
      dataFields.add(
          ObjectDataField.ofField(
              "code",
              "code",
              JavaDataDescriptor.fromClass(compilerxyz.eval.Code.class),
              compilerxyz.eval.Code.dynamicType()));
      dataFields.add(
          ObjectDataField.ofField(
              "binding",
              "binding",
              JavaDataDescriptor.fromClass(compilerxyz.gen.Binding.class),
              compilerxyz.gen.GenBinding.dynamicType()));
      var trivialIdentityAspects = new TList<compilerxyz.eval.Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.BIND_RESULT_TYPE_IDENTITY);
      return new TypeObjectDynamic(
          new compilerxyz.eval.value.object_.TypeObjectMeta(GenTypeIds.BIND_RESULT_TYPE_ID, "BindResult", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromClass(AspectBind.BindResult.class),
          AspectBind.BindResult.class,
          dataFields,
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
