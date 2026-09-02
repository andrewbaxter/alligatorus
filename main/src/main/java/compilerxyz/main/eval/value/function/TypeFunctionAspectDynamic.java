package compilerxyz.main.eval.value.function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.option.TypeOptionDynamic;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinPath("eval.aspect_lookup")
@BuiltinFull(uniqueId = 6108L)
public record TypeFunctionAspectDynamic() implements Type, AspectFunction, AspectInitialBind {

    private static final JavaInternalName INTNAME_TYPE = JavaInternalName.fromClass(Type.class);
    private static final JavaMethodDescriptor DESC_TYPE_ASPECT = JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromClass(compilerxyz.prelude.util.Opt.class),
            JavaDataDescriptor.fromClass(ExprContext.class),
            JavaDataDescriptor.fromClass(compilerxyz.main.eval.Unique.class));

    @Override
    public String typeDesc() {
        return "fn-aspect";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_ASPECT_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

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
        if (aspectTypeVal instanceof TypeSourceInterfaceDynamic si && !si.data().isAspect) {
            return EResult.err(loc, "aspect_lookup: interface is not an aspect (set_is_aspect was not called)");
        }
        var auq = aspTa.typeAspectId();

        var anyDyn = GenType.dynamicType();
        var subjectConvertedRes = Eval.typecheckConvert(xx, loc, anyDyn, subjectCv);
        if (subjectConvertedRes.hasErrors()) return subjectConvertedRes.reerr();
        var subjectConverted = subjectConvertedRes.requireValue("aspect_lookup subject convert");
        var subjectConsumeRes = GenAspectConsumeDynamic.get(xx, loc, anyDyn);
        if (subjectConsumeRes.hasErrors()) return subjectConsumeRes.reerr();
        var subjectDynRes = subjectConsumeRes
                .requireValue("aspect_lookup subject consume")
                .aspectConsumeDynamicToDynamic(xx, loc, subjectConverted);
        if (subjectDynRes.hasErrors()) return subjectDynRes.reerr();
        var subjectDyn = subjectDynRes.requireValue("aspect_lookup subject toDynamic");
        var xxDyn = xxCv.code().codeDynamic();
        if (xxDyn == null) {
            return EResult.err(loc, "aspect_lookup: xx has no runtime code");
        }

        var uniqueKey = new JavaBytecodeBindingKey();
        var uniqueId = new BindingId.Local(uniqueKey);
        xx.vx.externals.add(new EvalContext.ExternalEntry(uniqueKey, this, new External(Unique.class, auq)));
        var uniqueLoad = BindingCode.bindingUse(new CodeLoc.Some(loc), uniqueId);

        var seq = new JavaBytecodeSequence();
        seq.add(subjectDyn);
        seq.add(xxDyn);
        seq.add(uniqueLoad);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, INTNAME_TYPE, "typeAspect", DESC_TYPE_ASPECT));

        return EResult.ok(new CodeValue(
                Code.of(new CodeDynamic(new CodeLoc.Some(loc), seq)),
                EvalOutcome.ofValue(
                        ValueDynamic.stack(xx, new TypeOptionDynamic(DynUtil.substituteBoxed(xx, aspTa))))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_FUNCTION_ASPECT_DYNAMIC_IDENTITY)
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
