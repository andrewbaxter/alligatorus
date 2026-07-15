package compilerxyz.eval.value.object_;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTypeId(7011L)
public final class ObjectConstOfType {

  private ObjectConstOfType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectConst INSTANCE = build();

    private static TypeObjectConst build() {
      var castRules = new TList<TypeObjectCastRule>();
      castRules.add(new TypeObjectCastRule.RestampToIdentity(GenIdentities.TYPE_IDENTITY));
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.TYPE_IDENTITY);
      return new TypeObjectConst(
          GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID,
          "type",
          Type.class,
          Opt.<Type>none(),
          identityAspects,
          castRules,
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none());
    }
  }
}
