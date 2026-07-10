package compilerxyz.eval.value.source_union;

import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class SourceUnionRefType {

  private SourceUnionRefType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectDynamic INSTANCE = build();

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.UNION_REF_TYPE_IDENTITY);
      TypeObjectLift lift =
          (xx, loc, host) -> {
            if (!(host instanceof TypeSourceUnionDynamic ut)) {
              return EResult.err(
                  loc,
                  "union_ref::lift: expected TypeSourceUnionDynamic, got "
                      + (host == null ? "null" : host.getClass().getName()));
            }
            return EResult.ok(new ValueObjectConst(new TypeTypeSourceUnionConst(ut)));
          };
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_SOURCE_UNION_REF_DYNAMIC_TYPE_ID,
          "union_ref",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<TypeObjectCastRule>(),
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>some(lift),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
