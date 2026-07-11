package compilerxyz.eval.value.object_;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
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

  record CallByDestIdentity(Unique identity, DynRuntimeHelper helper)
      implements TypeObjectCastRule {
    @Override
    public boolean matches(ExprContext xx, Type destType) {
      return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return emitRuntimeCall(xx, source, loc, cv, destType, helper);
    }
  }

  record CallToInterfaceSpec(DynRuntimeHelper helper) implements TypeObjectCastRule {
    @Override
    public boolean matches(ExprContext xx, Type destType) {
      return destType instanceof TypeInterfaceSpec;
    }

    @Override
    public EResult<CodeValue> apply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return emitRuntimeCall(xx, source, loc, cv, destType, helper);
    }
  }

  record DynRuntimeHelper(
      JavaInternalName targetClass,
      String methodName,
      JavaMethodDescriptor descriptor,
      boolean needsExprContext) {}

  private static EResult<CodeValue> emitRuntimeCall(
      ExprContext xx,
      TypeObjectDynamic source,
      QExprId loc,
      CodeValue cv,
      Type destType,
      DynRuntimeHelper helper) {
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
    var seq = new JavaBytecodeSequence();
    if (helper.needsExprContext) {
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc,
              JavaInternalName.of(xx.vx.evalRootInternalName),
              EvalRootEmitter.EXPR_CONTEXT_FIELD,
              EvalRootEmitter.DESC_EXPR_CONTEXT));
    }
    seq.add(sourceCode);
    seq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            helper.targetClass,
            helper.methodName,
            helper.descriptor));
    var retDesc = helper.descriptor.value();
    var retStart = retDesc.indexOf(')') + 1;
    if (retDesc.charAt(retStart) == 'L'
        && !retDesc.substring(retStart).equals("Ljava/lang/Object;")) {
      seq.add(
          JavaBytecodeUtils.cast(
              loc, JavaDataDescriptor.fromInternalName(
                  JavaInternalName.of(retDesc.substring(retStart + 1, retDesc.length() - 1)))));
    }
    var call = new CodeDynamic(loc, seq);
    return EResult.ok(
        new CodeValue(Code.of(loc, call), Opt.some(ValueDynamic.stack(destType))));
  }
}
