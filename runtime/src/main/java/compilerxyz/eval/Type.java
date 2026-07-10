package compilerxyz.eval;

import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
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
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface Type extends Value {

  static Type dynamicType() {
    return TypeDynamicHolder.INSTANCE;
  }

  final class TypeDynamicHolder {
    static final TypeObjectDynamic INSTANCE = build();

    private TypeDynamicHolder() {}

    private static TypeObjectDynamic build() {
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.TYPE_TYPE_IDENTITY);
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_TYPE_DYNAMIC_TYPE_ID,
          "type",
          JavaInternalName.of("compilerxyz/eval/Type"),
          JavaDataDescriptor.fromClass(Type.class),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<TypeObjectCastRule>(),
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }

  String typeDesc();

  TypeId typeId();

  Opt<Aspect> typeAspect(ExprContext xx, Unique key);

  EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

  boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

  boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

  EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);
}
