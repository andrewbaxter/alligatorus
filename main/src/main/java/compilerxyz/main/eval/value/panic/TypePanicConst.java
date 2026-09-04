package compilerxyz.main.eval.value.panic;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

@BuiltinPath("panic")
@BuiltinFull(uniqueId = 6133L)
public record TypePanicConst() implements Type, AspectFunction {

    @Override
    public String typeDesc() {
        return "panic";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_PANIC_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        Type stringType = new TypeStringDynamic();
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, stringType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var toDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, stringType));
        if (toDynamicOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var msgCodeOpt = erb.mustEr(toDynamicOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, converted));
        if (msgCodeOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var msgCode = msgCodeOpt.optUnwrap();
        var panicInternal = JavaInternalName.fromClass(DynPanic.class);
        var ctorDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(String.class)));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.newInstance(loc, panicInternal));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(msgCode);
        seq.add(JavaBytecodeUtils.invokeSpecial(loc, panicInternal, "<init>", ctorDesc));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
        var throwCode = new CodeDynamic(loc, seq);
        return EResult.ok(new CodeValue(Code.target(throwCode), EvalOutcome.CONTROL_ENDED));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_PANIC_CONST_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
