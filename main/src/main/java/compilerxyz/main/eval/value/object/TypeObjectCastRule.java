package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaVirtualMethodRef;
import compilerxyz.main.eval.value.ValueDynamic;

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
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
    }

    record InvokeVirtualByDestIdentity(Unique identity, JavaVirtualMethodRef method) implements TypeObjectCastRule {
        @Override
        public boolean typeObjectCastRuleMatches(ExprContext xx, Type destType) {
            return destType.typeAspect(xx, identity).optIsSome();
        }

        @Override
        public EResult<CodeValue> typeObjectCastRuleApply(
                ExprContext xx, TypeObjectDynamic source, QExprId loc, CodeValue cv, Type destType) {
            var sourceCodeCast = CodeDynamic.castOpt(loc, cv.code());
            if (sourceCodeCast.hasErrors()) return sourceCodeCast.reerr();
            if (sourceCodeCast.requireValue("sourceCode").optIsNone()) {
                return EResult.err(
                        loc,
                        "cast " + source.typeDesc() + " → " + destType.typeDesc() + ": source has no runtime code");
            }
            var sourceCode = sourceCodeCast.requireValue("sourceCode").optUnwrap();
            var seq = new JavaBytecodeSequence();
            seq.add(sourceCode);
            seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(method.owner)));
            seq.add(JavaBytecodeUtils.callMethod(loc, method));
            var retDesc = method.descriptor.value;
            var retStart = retDesc.indexOf(')') + 1;
            if (retDesc.charAt(retStart) == 'L' && !retDesc.substring(retStart).equals("Ljava/lang/Object;")) {
                seq.add(JavaBytecodeUtils.cast(
                        loc,
                        JavaDataDescriptor.fromInternalName(
                                JavaInternalName.of(retDesc.substring(retStart + 1, retDesc.length() - 1)))));
            }
            var call = new CodeDynamic(loc, seq);
            return EResult.ok(new CodeValue(Code.target(call), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
    }
}
