package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.gen.Target;
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
import org.jspecify.annotations.Nullable;

@compilerxyz.annotations.BuiltinIdentity(6001L)
@compilerxyz.annotations.BuiltinTypeId(7021L)
public final class ExprContext {
  public final @Nullable Type currentExceptionType;
  public final ImportStateId importState;
  public final Scope scope;
  public final Target target;
  public final EvalContext vx;
  public final TList<ReturnFrame> returnStack;
  public final TList<Code> dropStack;

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType) {
    this(scope, target, vx, importState, currentExceptionType, new TList<>(), new TList<>());
  }

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType,
      TList<ReturnFrame> returnStack,
      TList<Code> dropStack) {
    this.scope = scope;
    this.target = target;
    this.vx = vx;
    this.importState = importState;
    this.currentExceptionType = currentExceptionType;
    this.returnStack = returnStack;
    this.dropStack = dropStack;
  }

  public ExprContext child(Scope childScope) {
    return new ExprContext(childScope, target, vx, importState, currentExceptionType, returnStack, dropStack);
  }

  public ExprContext withExceptionType(@Nullable Type newExceptionType) {
    return new ExprContext(scope, target, vx, importState, newExceptionType, returnStack, dropStack);
  }

  public static Type dynamicType() {
    return ExprContextDynamicType.INSTANCE;
  }

  static final class ExprContextDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private ExprContextDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(ExprContext.class);
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.EXPR_CONTEXT_IDENTITY);
      var castFromRules = new TList<TypeObjectCastFromRule>();
      castFromRules.add(
          new TypeObjectCastFromRule.RestampFromIdentity(
              GenIdentities.EXPR_CONTEXT_IDENTITY));
      return new TypeObjectDynamic(
          GenTypeIds.EXPR_CONTEXT_TYPE_ID,
          "ExprContext",
          jvmClass,
          JavaDataDescriptor.fromClass(ExprContext.class),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<TypeObjectCastRule>(),
          castFromRules,
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }
}
