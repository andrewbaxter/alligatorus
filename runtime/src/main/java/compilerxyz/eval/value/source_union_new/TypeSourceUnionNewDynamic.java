package compilerxyz.eval.value.source_union_new;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.SourceUnionEmitter;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.Binding;
import compilerxyz.gen.BindingSetter;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenType;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinLight(uniqueId = 6142L)
public record TypeSourceUnionNewDynamic(TypeSourceUnionDynamic type_, int variantIndex)
        implements Type, AspectBind, AspectFunction, ExporterAny {

    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    @Override
    public String typeDesc() {
        if (variantIndex < 0 || variantIndex >= type_.variants.size()) {
            return "union_new(?)";
        }
        return "union_new(" + type_.variants.get(variantIndex).tag.constKeyToCanonical() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBind.AspectId) || key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceUnionNewDynamic::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(Code.none(loc), new SourceUnionNewConstBinding(v)));
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        if (variantIndex < 0 || variantIndex >= type_.variants.size()) {
            return EResult.err(loc, "TypeSourceUnionNewDynamic::call: variant index " + variantIndex + " out of range");
        }
        var variant = type_.variants.get(variantIndex);
        Type underlyingType = variant.type;
        if (underlyingType == null) {
            return EResult.err(loc, "TypeSourceUnionNewDynamic::call: variant payload type not finalised");
        }
        xx.vx.definitionSets.putIfAbsent(type_.set.defSetId, type_.set);
        var variantBinaryName =
                SourceUnionEmitter.variantBinaryNameFor(type_.set.defSetId, type_.unionIdx, variantIndex);
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var underlyingJsRes = consumeRes
                .requireValue("TypeSourceUnionNewDynamic::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
        var underlyingJs = underlyingJsRes.requireValue("TypeSourceUnionNewDynamic::call toDynamic");
        Type resultType = type_.withActiveVariants(TList.of(variantIndex));
        var variantInternal = JavaInternalName.of(variantBinaryName.replace('.', '/'));
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, underlyingType);
        var ctorDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(payloadDesc));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.newInstance(loc, variantInternal));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
        seq.add(underlyingJs);
        seq.add(JavaBytecodeUtils.invokeSpecial(loc, variantInternal, "<init>", ctorDesc));
        var newObject = new CodeDynamic(loc, seq);
        return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, newObject, resultType, 1));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(new TypeTypeSourceUnionNewConst(type_(), variantIndex()), this));
    }

    private record SourceUnionNewConstBinding(Value value) implements Binding {
        @Override
        public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
            return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
        }

        @Override
        public Binding bindingFork() {
            return this;
        }

        @Override
        public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
            for (var other : others) {
                if (!this.equals(other)) {
                    return EResult.err(
                            loc,
                            "cannot merge " + getClass().getSimpleName()
                                    + " with a differently-typed binding across branches");
                }
            }
            return EResult.ok(this);
        }

        @Override
        public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
            return Binding.bindingDrop(xx, loc, this);
        }

        @Override
        public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
            return Binding.elementUseDynamicBind(dynamicKey);
        }
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
                GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY,
                TypeSourceUnionNewDynamic.class);
        return oOpt.optIsSome()
                && type_.typeIsEqual(xx, oOpt.optUnwrap().type_)
                && variantIndex == oOpt.optUnwrap().variantIndex;
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
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
