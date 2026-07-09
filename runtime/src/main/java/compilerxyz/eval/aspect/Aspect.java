package compilerxyz.eval.aspect;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface Aspect {

  static Type dynamicType() {
    return AspectDynamicType.INSTANCE;
  }

  final class AspectDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private AspectDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.TYPE_ASPECT_IDENTITY);
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_ASPECT_DYNAMIC_TYPE_ID,
          "aspect",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<TypeObjectCastRule>(),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
