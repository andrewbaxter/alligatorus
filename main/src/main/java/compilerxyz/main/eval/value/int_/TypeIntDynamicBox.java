package compilerxyz.main.eval.value.int_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectDropDynamicLightDefault;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.numeric.AspectConsumeDynamicJvmIntBoxed;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicUnboxed;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6165L)
public record TypeIntDynamicBox() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "dynamic int";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicIntDynamicBox(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_INT_DYNAMIC_BOX_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicIntDynamicBox(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldIntDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectDynamicUnboxed.AspectId)) {
            return Opt.some(AspectDynamicUnboxedIntDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(AspectDropDynamicLightDefault.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicJvmIntBoxed.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_INT_DYNAMIC_BOX_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return Numerics.isNumeric(xx, sourceType);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return Numerics.isNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        var unboxedCast = Numerics.castTo(xx, loc, cv, NumericKind.INT, new TypeIntDynamic());
        if (unboxedCast.hasErrors()) return unboxedCast.reerr();
        var castCv = unboxedCast.requireValue("TypeIntDynamicBox::typeCastFrom cast");
        var seq = new JavaBytecodeSequence();
        var dynCast = CodeDynamic.castOpt(loc, castCv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        var dyn = dynCast.requireValue("dyn");
        if (dyn.optIsSome()) seq.add(dyn.optUnwrap());
        seq.add(Numerics.box(loc, NumericKind.INT));
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var selfDynRes =
                CodeDynamic.castRequired(loc, cv.code(), "TypeIntDynamicBox::typeCastTo: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(Numerics.unbox(loc, NumericKind.INT));
        var unboxed = new CodeValue(
                Code.target(new CodeDynamic(loc, seq)),
                EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeIntDynamic())));
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, unboxed, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeIntDynamicBox::typeCastTo: destination is not numeric");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
