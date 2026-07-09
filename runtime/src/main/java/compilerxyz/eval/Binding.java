package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface Binding {

  default CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    throw new AssertionError(
        "user did not provide a Binding::bindingUse_ method on " + getClass().getName());
  }

  static Type dynamicType() {
    return BindingDynamicType.INSTANCE;
  }

  final class BindingDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private BindingDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/eval/Binding");
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenBuiltinInterfaces.builtinBindingIfaceIdentity());
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_ANY_BINDING_DYNAMIC_TYPE_ID,
          "binding",
          jvmClass,
          JavaDataDescriptor.fromClass(Binding.class),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<compilerxyz.eval.value.object_.TypeObjectCastRule>(),
          Opt.<compilerxyz.eval.value.object_.ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
