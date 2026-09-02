package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6106L)
public record TypeEResultVariantConstructorDynamic(Type elementType, Kind kind) implements Type, AspectFunction {

    public enum Kind {
        OK,
        ERR,
        ERRS
    }

    @Override
    public String typeDesc() {
        return "eresult("
                + elementType.typeDesc()
                + ")."
                + switch (kind) {
                    case OK -> "ok";
                    case ERR -> "err";
                    case ERRS -> "errs";
                };
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var erb = new Erb(loc);
        Type resultType = new TypeEResultDynamic(DynUtil.substituteBoxed(xx, elementType));
        if (kind == Kind.OK) {
            var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, elementType, argument));
            if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var innerJsRes = consumeRes
                    .requireValue("TypeEResultVariantConstructorDynamic::call ok consume")
                    .aspectConsumeDynamicToDynamic(xx, loc, convertedOpt.optUnwrap());
            if (innerJsRes.hasErrors()) return innerJsRes.reerr();
            var innerJs = innerJsRes.requireValue("TypeEResultVariantConstructorDynamic::call ok toDynamic");
            var okSeq = new JavaBytecodeSequence();
            okSeq.add(innerJs);
            okSeq.add(JavaBytecodeUtils.callStaticMethod(
                    loc,
                    JavaInternalName.fromClass(EResult.class),
                    "ok",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                            TList.of(JvmConstants.DESC_OBJECT))));
            return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, okSeq), resultType));
        }
        if (kind == Kind.ERRS) {
            var errsArgType = new TypeListBoxedDynamic(GenExprErr.dynamicType());
            var convertedErrsOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, errsArgType, argument));
            if (convertedErrsOpt.optIsNone()) return EResult.errs(erb.errors());
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, errsArgType);
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var listJsRes = consumeRes
                    .requireValue("TypeEResultVariantConstructorDynamic::call errs consume")
                    .aspectConsumeDynamicToDynamic(xx, loc, convertedErrsOpt.optUnwrap());
            if (listJsRes.hasErrors()) return listJsRes.reerr();
            var errsSeq = new JavaBytecodeSequence();
            errsSeq.add(listJsRes.requireValue("TypeEResultVariantConstructorDynamic::call errs toDynamic"));
            errsSeq.add(JavaBytecodeUtils.callStaticMethod(
                    loc,
                    JavaInternalName.fromClass(EResult.class),
                    "errs",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                            TList.of(JavaDataDescriptor.fromInternalName(
                                    JavaInternalName.fromClass(compilerxyz.prelude.util.TList.class))))));
            return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, errsSeq), resultType));
        }
        var argTupleType = new Tuple(
                Opt.none(),
                TList.of(
                        new TupleElement.Dynamic(GenQExprId.dynamicType()),
                        new TupleElement.Dynamic(new TypeStringDynamic())));
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, argTupleType, argument));
        if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
        var slotsRes = DynUtil.destructurePositional(xx, loc, argTupleType, convertedOpt.optUnwrap());
        if (slotsRes.hasErrors()) return slotsRes.reerr();
        var slots = slotsRes.requireValue("TypeEResultVariantConstructorDynamic::call destructure");
        if (slots.size() != 2) {
            throw new AssertionError(
                    "TypeEResultVariantConstructorDynamic::call err: expected 2 slots, got " + slots.size());
        }
        var locJs = slots.get(0);
        var msgJs = slots.get(1);
        var errSeq = new JavaBytecodeSequence();
        errSeq.add(locJs);
        errSeq.add(msgJs);
        errSeq.add(JavaBytecodeUtils.callStaticMethod(
                loc,
                JavaInternalName.fromClass(EResult.class),
                "err",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class)),
                        TList.of(
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(QExprId.class)),
                                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(String.class))))));
        return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, errSeq), resultType));
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
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_E_RESULT_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY,
                TypeEResultVariantConstructorDynamic.class);
        return oOpt.optIsSome()
                && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType)
                && kind == oOpt.optUnwrap().kind;
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
