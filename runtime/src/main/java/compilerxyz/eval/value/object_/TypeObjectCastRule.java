package compilerxyz.eval.value.object_;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.interface_spec.TypeInterfaceSpec;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public sealed interface TypeObjectCastRule {

  boolean matches(ExprContext xx, Type destType);

  EResult<CodeValue> apply(
      ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType);

  record RestampToIdentity(Unique identity) implements TypeObjectCastRule {
    @Override
    public boolean matches(ExprContext xx, Type destType) {
      return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return EResult.ok(new CodeValue(cv.code(), Opt.some(ValueDynamic.stack(destType))));
    }
  }

  record CallByDestIdentity(Unique identity, TypeFunctionDynamic method)
      implements TypeObjectCastRule {
    @Override
    public boolean matches(ExprContext xx, Type destType) {
      return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return emitRuntimeCall(xx, source, loc, cv, destType, method);
    }
  }

  record CallToInterfaceSpec(TypeFunctionDynamic method) implements TypeObjectCastRule {
    @Override
    public boolean matches(ExprContext xx, Type destType) {
      return destType instanceof TypeInterfaceSpec;
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return emitRuntimeCall(xx, source, loc, cv, destType, method);
    }
  }

private static EResult<CodeValue> emitRuntimeCall(
      ExprContext xx,
      TypeObjectDynamic source,
      QExprId loc,
      CodeValue cv,
      Type destType,
      TypeFunctionDynamic method) {
    var sourceCode = cv.code().dynamic();
    if (sourceCode == null) {
      return EResult.err(
          loc,
          "cast "
              + source.typeDesc()
              + " → "
              + destType.typeDesc()
              + ": source has no runtime code");
    }
    var args = new TList<CodeDynamic>();
    args.add(sourceCode);
    var call = method.emitInvocation(loc, xx.vx, args);
    return EResult.ok(
        new CodeValue(Code.of(loc, call), Opt.some(ValueDynamic.stack(destType))));
  }
}
