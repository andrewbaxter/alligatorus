package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.option.TypeOptionDynamic;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionFunctionAspectConst(TypeFunctionAspectConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "aspect_lookup: arg has no compile-time value");
        }
        var argVal = argument.value().optUnwrap();
        var argType = argVal.valueType();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsNone()) {
            return EResult.err(loc, "aspect_lookup: expected 3-tuple arg (subject, aspect_type, xx)");
        }
        var elemCvs = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argVal);
        if (elemCvs.size() != 3) {
            return EResult.err(
                    loc, "aspect_lookup: expected 3 elements (subject, aspect_type, xx), got " + elemCvs.size());
        }
        if (elemCvs.get(0).hasErrors()) return elemCvs.get(0).reerr();
        if (elemCvs.get(1).hasErrors()) return elemCvs.get(1).reerr();
        if (elemCvs.get(2).hasErrors()) return elemCvs.get(2).reerr();
        var subjectCv = elemCvs.get(0).requireValue("aspect_lookup subject");
        var aspectTypeCv = elemCvs.get(1).requireValue("aspect_lookup aspect_type");
        var xxCv = elemCvs.get(2).requireValue("aspect_lookup xx");

        if (aspectTypeCv.value().optIsNone()) {
            return EResult.err(loc, "aspect_lookup: aspect_type has no compile-time value");
        }
        Object aspectTypeVal = aspectTypeCv.value().optUnwrap();
        if (aspectTypeVal instanceof ValueObjectConst wrap) {
            aspectTypeVal = wrap.data();
        }
        if (!(aspectTypeVal instanceof TypeAspect aspTa)) {
            return EResult.err(
                    loc, "aspect_lookup: aspect_type must implement TypeAspect, got " + aspectTypeVal.getClass());
        }
        if (aspectTypeVal instanceof TypeSourceInterfaceDynamic si && !si.cell().isAspect()) {
            return EResult.err(loc, "aspect_lookup: interface is not an aspect (set_is_aspect was not called)");
        }
        var auq = aspTa.typeAspectId();

        var anyDyn = GenType.dynamicType();
        var subjectConvertedRes = Eval.typecheckConvert(xx, loc, anyDyn, subjectCv);
        if (subjectConvertedRes.hasErrors()) return subjectConvertedRes.reerr();
        var subjectConverted = subjectConvertedRes.requireValue("aspect_lookup subject convert");
        var subjectDynRes = DynUtil.materialize(xx, loc, subjectConverted);
        if (subjectDynRes.hasErrors()) return subjectDynRes.reerr();
        var xxDynRes = DynUtil.valueReg(loc, xxCv);
        if (xxDynRes.hasErrors()) return xxDynRes.reerr();

        var uniqueEntry = new EvalContext.ExternalEntry(new BindingId(), type, new External(Unique.class, auq));

        var b = new JavaBytecodeBuilder(loc);
        var argumentAdd = b.addCode(loc, argument.code());
        if (argumentAdd.optIsSome()) return EResult.errs(argumentAdd.optUnwrap());
        var subject = b.reg(subjectDynRes.requireValue("aspect_lookup subject toDynamic"));
        var xxReg = b.reg(xxDynRes.requireValue("xxDyn"));
        var uniqueLoad = b.external(uniqueEntry);
        var out = b.invokeInterface(
                TypeFunctionAspectConst.INTNAME_TYPE,
                "typeAspect",
                TypeFunctionAspectConst.DESC_TYPE_ASPECT,
                subject,
                xxReg,
                uniqueLoad);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeOptionDynamic(aspTa)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
