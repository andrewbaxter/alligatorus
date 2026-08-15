package compilerxyz.eval.value.object_;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaVirtualMethodRef;
import compilerxyz.eval.value.ValueDynamic;

public sealed interface TypeObjectCastRule {

  boolean typeObjectCastRuleMatches(ExprContext xx, Type destType);

  EResult<CodeValue> typeObjectCastRuleApply(
      ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType);

  record RestampToIdentity(Unique identity) implements TypeObjectCastRule {
    @Override
    public boolean typeObjectCastRuleMatches(ExprContext xx, Type destType) {
      return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeObjectCastRuleApply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
  }

  /**
   * Casts by calling an instance method via INVOKEVIRTUAL on the source runtime value.
   * The method is expected to return a value assignable to destType (typically) or to
   * throw at runtime if the specific source instance cannot produce one.
   */
  record InvokeVirtualByDestIdentity(Unique identity, JavaVirtualMethodRef method)
      implements TypeObjectCastRule {
    @Override
    public boolean typeObjectCastRuleMatches(ExprContext xx, Type destType) {
      return destType.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeObjectCastRuleApply(
        ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
      var sourceCode = cv.code().codeDynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc,
            "cast " + source.typeDesc() + " → " + destType.typeDesc()
                + ": source has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(sourceCode);
      seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(method.owner)));
      seq.add(JavaBytecodeUtils.callMethod(loc, method));
      var retDesc = method.descriptor.value;
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
          new CodeValue(Code.of(loc, call), EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
  }

}
