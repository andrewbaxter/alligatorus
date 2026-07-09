package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.Target;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class ExprContext {
  public final @Nullable Type currentExceptionType;
  public final ImportStateId importState;
  public final Scope scope;
  public final Target target;
  public final EvalContext vx;

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType) {
    this.scope = scope;
    this.target = target;
    this.vx = vx;
    this.importState = importState;
    this.currentExceptionType = currentExceptionType;
  }

  public ExprContext child(Scope childScope) {
    return new ExprContext(childScope, target, vx, importState, currentExceptionType);
  }

  public ExprContext withExceptionType(@Nullable Type newExceptionType) {
    return new ExprContext(scope, target, vx, importState, newExceptionType);
  }

  public static Type dynamicType() {
    return ExprContextDynamicType.INSTANCE;
  }

  static final class ExprContextDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private ExprContextDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/eval/ExprContext");
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.EXPR_CONTEXT_REF_TYPE_IDENTITY);
      var castFromRules = new TList<TypeObjectCastFromRule>();
      castFromRules.add(
          new TypeObjectCastFromRule.RestampFromIdentity(
              GenIdentities.EXPR_CONTEXT_REF_TYPE_IDENTITY));
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_EXPR_CONTEXT_DYNAMIC_TYPE_ID,
          "ExprContext",
          jvmClass,
          JavaDataDescriptor.fromClass(ExprContext.class),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<TypeObjectCastRule>(),
          castFromRules,
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
