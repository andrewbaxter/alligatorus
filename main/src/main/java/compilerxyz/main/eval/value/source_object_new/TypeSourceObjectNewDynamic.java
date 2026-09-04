package compilerxyz.main.eval.value.source_object_new;

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
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6139L)
public record TypeSourceObjectNewDynamic(TypeSourceObjectDynamic type_)
        implements Type, AspectBind, AspectBindDynamic, AspectFunction {

    @Override
    public String typeDesc() {
        return type_.description.optUnwrapOr("definition_set_object_new");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_NEW_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(Code.none(loc), new SourceObjectNewConstBinding(v)));
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        var set = type_.set;
        if (set == null) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::call: object has no definition set reference");
        }
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        if (type_.underlyingType.optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::call: object underlying type not finalised");
        }
        Type underlyingType = type_.underlyingType.optUnwrap();
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var underlyingJsRes = consumeRes
                .requireValue("TypeSourceObjectNewDynamic::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
        var underlyingJs = underlyingJsRes.requireValue("TypeSourceObjectNewDynamic::call toDynamic");
        Type resultType = type_;

        var objInternalName = type_.internalName();
        var klass = JavaInternalName.of(objInternalName);
        var underlyingDesc = consumeRes
                .requireValue("TypeSourceObjectNewDynamic::call underlying desc")
                .aspectConsumeDynamicJvmClass(xx);
        var ctorDesc = JavaMethodDescriptor.fromConstructorParts(TList.of(underlyingDesc));
        var newInstance = JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
        return EResult.ok(DynUtil.dynamicStackToValue(xx, newInstance, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    private record SourceObjectNewConstBinding(Value value) implements Binding {
        @Override
        public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
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
                GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_NEW_DYNAMIC_IDENTITY,
                TypeSourceObjectNewDynamic.class);
        return oOpt.optIsSome() && type_.typeIsEqual(xx, oOpt.optUnwrap().type_);
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
