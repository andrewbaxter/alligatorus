package compilerxyz.eval;

import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTrivialIdentityAspect;
import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
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

@BuiltinTypeId(7103L)
@BuiltinTrivialIdentityAspect(6016L)
@BuiltinPath("types.code")
public sealed interface Code permits Code.None, Code.Target {

  record None(QExprId loc) implements Code {
  }

  record Target(TargetCode impl) implements Code {
  }

  default CodeDynamic dynamic() {
    if (this instanceof Target t && t.impl() instanceof CodeDynamic cd) return cd;
    return null;
  }

  default boolean isNone() {
    return this instanceof None;
  }

  static Code none(QExprId loc) {
    return new None(loc);
  }

  static Code of(CodeDynamic dynamic) {
    return new Target(dynamic);
  }

  static Code of(QExprId loc, CodeDynamic dynamic) {
    return new Target(dynamic);
  }

  static Type dynamicType() {
    return CodeDynamicType.INSTANCE;
  }

  final class CodeDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private CodeDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(Code.class);
      var trivialIdentityAspects = new TList<compilerxyz.eval.Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.CODE_IDENTITY);
      return new TypeObjectDynamic(
          new compilerxyz.eval.value.object_.TypeObjectMeta(GenTypeIds.CODE_TYPE_ID, "code", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          Code.class,
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
