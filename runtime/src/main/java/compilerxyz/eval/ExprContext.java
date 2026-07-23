package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinTypeId;

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
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

@Builtin(uniqueId = 6001L)
@BuiltinTypeId(7021L)
public final class ExprContext {
  public final @Nullable Type currentExceptionType;
  public final ImportStateId importState;
  public final Scope scope;
  public final Target target;
  public final EvalContext vx;
  public final TList<ReturnFrame> returnStack;

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType) {
    this(scope, target, vx, importState, currentExceptionType, new TList<>());
  }

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      @Nullable Type currentExceptionType,
      TList<ReturnFrame> returnStack) {
    this.scope = scope;
    this.target = target;
    this.vx = vx;
    this.importState = importState;
    this.currentExceptionType = currentExceptionType;
    this.returnStack = returnStack;
  }

  public ExprContext withExceptionType(@Nullable Type newExceptionType) {
    return new ExprContext(scope, target, vx, importState, newExceptionType, returnStack);
  }

  public static Type dynamicType() {
    return ExprContextDynamicType.INSTANCE;
  }

  static final class ExprContextDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private ExprContextDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(ExprContext.class);
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.EXPR_CONTEXT_IDENTITY);
      var castFromRules = new TList<TypeObjectCastFromRule>();
      castFromRules.add(
          new TypeObjectCastFromRule.RestampFromIdentity(
              GenTrivialIdentityAspects.EXPR_CONTEXT_IDENTITY));
      return new TypeObjectDynamic(
          new compilerxyz.eval.value.object_.TypeObjectMeta(GenTypeIds.EXPR_CONTEXT_TYPE_ID, "ExprContext", trivialIdentityAspects, new TList<TypeObjectCastRule>(), castFromRules, Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromClass(ExprContext.class),
          ExprContext.class,
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
}
