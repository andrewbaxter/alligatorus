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
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrivialIdentityAspect(6012L)
@compilerxyz.annotations.BuiltinTypeId(7013L)
@compilerxyz.annotations.BuiltinPath("types.aspect")
public interface Aspect {

  static Type dynamicType() {
    return AspectDynamicType.INSTANCE;
  }

  final class AspectDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private AspectDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.ASPECT_IDENTITY);
      var meta = new compilerxyz.eval.value.object_.TypeObjectMeta(GenTypeIds.ASPECT_TYPE_ID, "aspect", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<compilerxyz.eval.value.object_.TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none());
      return new TypeObjectDynamic(
          meta,
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          Object.class,
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
