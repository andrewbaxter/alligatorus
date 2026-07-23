package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@Builtin(uniqueId = 6018L)
@BuiltinTypeId(7022L)
public record QExprId(SourceId source, long expr) {

  public static Type dynamicType() {
    return QExprIdDynamicType.INSTANCE;
  }

  static final class QExprIdDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private QExprIdDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(QExprId.class);
      var trivialIdentityAspects = new TList<compilerxyz.eval.Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.Q_EXPR_ID_IDENTITY);
      return new TypeObjectDynamic(
          new TypeObjectMeta(GenTypeIds.Q_EXPR_ID_TYPE_ID, "QExprId", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromClass(QExprId.class),
          QExprId.class,
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
