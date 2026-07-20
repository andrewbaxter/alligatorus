package compilerxyz.eval.value.object_;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Meta-type: the compile-time type of values that are TypeObjectDynamic instances.
 *
 * <p>Cast to InterfaceData is implemented as INVOKEVIRTUAL of
 * {@link TypeObjectDynamic#getInterfaceData()} on the loaded runtime value. The specific
 * TypeObjectDynamic instance decides at runtime whether it represents an interface
 * (returns InterfaceData) or not (throws).
 */
@compilerxyz.annotations.BuiltinTypeId(7121L)
public final class TypeTypeObjectDynamic {
  private TypeTypeObjectDynamic() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectConst INSTANCE = build();

    private static TypeObjectConst build() {
      var castRules = new TList<TypeObjectCastRule>();
      castRules.add(new TypeObjectCastRule.RestampToIdentity(GenTrivialIdentityAspects.TYPE_IDENTITY));
      castRules.add(new TypeObjectCastRule.InvokeVirtualByDestIdentity(
          GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY,
          JavaInternalName.fromClass(TypeObjectDynamic.class),
          "getInterfaceData",
          JavaMethodDescriptor.fromParts(
              JavaDataDescriptor.fromClass(InterfaceData.class),
              TList.of())));
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.TYPE_IDENTITY);
      var meta = new TypeObjectMeta("type_object_dynamic_meta", trivialIdentityAspects, castRules, new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none());
      return new TypeObjectConst(
          GenTypeIds.TYPE_TYPE_OBJECT_DYNAMIC_TYPE_ID,
          meta,
          TypeObjectDynamic.class,
          Opt.<Type>none());
    }
  }
}
