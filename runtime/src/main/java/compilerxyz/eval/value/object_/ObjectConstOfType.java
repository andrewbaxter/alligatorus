package compilerxyz.eval.value.object_;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.GenTrivialIdentityAspects;
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
      castRules.add(new TypeObjectCastRule.RestampToIdentity(GenTrivialIdentityAspects.TYPE_IDENTITY));
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.TYPE_IDENTITY);
      var meta = new TypeObjectMeta("type", trivialIdentityAspects, castRules, new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none());
      return new TypeObjectConst(
          GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID,
          meta,
          Type.class,
          Opt.<Type>none());
    }
  }
}
