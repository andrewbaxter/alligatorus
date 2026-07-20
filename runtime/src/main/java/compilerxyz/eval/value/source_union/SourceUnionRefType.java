package compilerxyz.eval.value.source_union;

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
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrivialIdentityAspect(6007L)
@compilerxyz.annotations.BuiltinTypeId(7053L)
public final class SourceUnionRefType {

  private SourceUnionRefType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.SOURCE_UNION_REF_TYPE_IDENTITY);
      return new TypeObjectDynamic(
          GenTypeIds.SOURCE_UNION_REF_TYPE_TYPE_ID,
          new compilerxyz.eval.value.object_.TypeObjectMeta("union_ref", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          Object.class,
          new compilerxyz.eval.TypeId.Composite(compilerxyz.gen.GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID, compilerxyz.util.TList.of(GenTypeIds.SOURCE_UNION_REF_TYPE_TYPE_ID)),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
