package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaVirtualMethodRef;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6406L)
public sealed interface TypeObjectCastRule {

    boolean typeObjectCastRuleMatches(ExprContext xx, Type destType);

    EResult<CodeValue> typeObjectCastRuleApply(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

    record RestampToIdentity(Unique identity) implements TypeObjectCastRule {
        @Override
        public boolean typeObjectCastRuleMatches(ExprContext xx, Type destType) {
            return destType.typeAspect(xx, identity).optIsSome();
        }

        @Override
        public EResult<CodeValue> typeObjectCastRuleApply(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, destType));
        }
    }

    record InvokeVirtualByDestIdentity(Unique identity, JavaVirtualMethodRef method) implements TypeObjectCastRule {
        @Override
        public boolean typeObjectCastRuleMatches(ExprContext xx, Type destType) {
            return destType.typeAspect(xx, identity).optIsSome();
        }

        @Override
        public EResult<CodeValue> typeObjectCastRuleApply(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
            var sourceRes = DynUtil.valueReg(loc, cv);
            if (sourceRes.hasErrors()) return sourceRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var receiver = b.checkcast(
                    JavaDataDescriptor.fromInternalName(method.owner), b.reg(sourceRes.requireValue("sourceCode")));
            var out = b.invokeVirtual(method, receiver);
            var retDesc = method.descriptor.returnDescriptor();
            if (retDesc.value.charAt(0) == 'L' && !retDesc.value.equals("Ljava/lang/Object;")) {
                out = b.checkcast(retDesc, out);
            }
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
    }
}
