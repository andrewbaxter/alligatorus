package compilerxyz.eval.value.source_union_new;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class SourceUnionNewRefType {

  private SourceUnionNewRefType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_SOURCE_UNION_NEW_DYNAMIC_TYPE_ID,
          new compilerxyz.eval.value.object_.TypeObjectMeta("union_new_ref", new compilerxyz.util.TList<compilerxyz.eval.Unique>(), new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          Object.class,
          new compilerxyz.eval.TypeId.Composite(compilerxyz.gen.GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID, compilerxyz.util.TList.of(GenTypeIds.TYPE_SOURCE_UNION_NEW_DYNAMIC_TYPE_ID)),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
