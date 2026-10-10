package compilerxyz.main.eval.value.function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6110L)
public record TypeFunctionConst(
        TList<FunctionParam> params, Opt<Type> ret, JavaInternalName klass, String methodName, boolean needsEvalContext)
        implements Aspect, Type {

    public Type arg() {
        return FunctionParam.argType(params);
    }

    public static EResult<JavaMethodDescriptor> descriptorFor(
            ExprContext xx, QExprId loc, Type arg, Opt<Type> ret, boolean needsEvalContext) {
        var argDescs = new TList<JavaDataDescriptor>();
        if (needsEvalContext) argDescs.add(JavaDataDescriptor.fromClass(EvalContext.class));
        var tupleOpt = GenAspectTuple.getMaybe(xx, arg);
        if (tupleOpt.optIsSome()) {
            for (var elem : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                var elemDescRes = jvmDescOf(xx, loc, elem);
                if (elemDescRes.hasErrors()) return elemDescRes.reerr();
                argDescs.add(elemDescRes.requireValue("TypeFunctionConst::descriptor tuple elem"));
            }
        } else {
            var recordOpt = GenAspectRecord.getMaybe(xx, arg);
            if (recordOpt.optIsSome()) {
                for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                    var fieldDescRes = jvmDescOf(xx, loc, entry.getValue());
                    if (fieldDescRes.hasErrors()) return fieldDescRes.reerr();
                    argDescs.add(fieldDescRes.requireValue("TypeFunctionConst::descriptor record field"));
                }
            } else {
                var argDescRes = jvmDescOf(xx, loc, arg);
                if (argDescRes.hasErrors()) return argDescRes.reerr();
                argDescs.add(argDescRes.requireValue("TypeFunctionConst::descriptor single arg"));
            }
        }
        JavaDataDescriptor retDesc;
        if (ret.optIsSome()) {
            var retDescRes = jvmDescOf(xx, loc, ret.optUnwrap());
            if (retDescRes.hasErrors()) return retDescRes.reerr();
            retDesc = retDescRes.requireValue("TypeFunctionConst::descriptor ret");
        } else {
            retDesc = JavaDataDescriptor.DESC_VOID;
        }
        return EResult.ok(JavaMethodDescriptor.fromParts(retDesc, argDescs));
    }

    private static EResult<JavaDataDescriptor> jvmDescOf(ExprContext xx, QExprId loc, Type t) {
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, t);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        return EResult.ok(
                consumeRes.requireValue("TypeFunctionConst::jvmDescOf consume").aspectConsumeDynamicJvmClass(xx));
    }

    @Override
    public String typeDesc() {
        return "dynamic func";
    }

    @Override
    public Meta typeMeta(ExprContext xx) {
        return MetaSupport.derive(xx, this, "function_const");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionFunctionConst(this));
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_FUNCTION_CONST_IDENTITY, TypeFunctionConst.class);
        return oOpt.optIsSome()
                && paramsEqual(xx, params, oOpt.optUnwrap().params)
                && retEqual(xx, ret, oOpt.optUnwrap().ret)
                && klass.equals(oOpt.optUnwrap().klass)
                && methodName.equals(oOpt.optUnwrap().methodName)
                && needsEvalContext == oOpt.optUnwrap().needsEvalContext;
    }

    static boolean paramsEqual(ExprContext xx, TList<FunctionParam> a, TList<FunctionParam> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).name().equals(b.get(i).name())) return false;
            if (!a.get(i).type().typeIsEqual(xx, b.get(i).type())) return false;
        }
        return true;
    }

    public static boolean retEqual(ExprContext xx, Opt<Type> a, Opt<Type> b) {
        if (a.optIsSome() && b.optIsSome()) {
            return a.optUnwrap().typeIsEqual(xx, b.optUnwrap());
        }
        return a.optIsNone() && b.optIsNone();
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
