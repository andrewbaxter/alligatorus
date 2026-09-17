package compilerxyz.main.eval.value.short_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.numeric.AspectConsumeDynamicJvmShortBoxed;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicUnboxed;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6169L)
@BuiltinPath("types.short_box")
public record TypeShortDynamicBox() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "dynamic short";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicShortDynamicBox(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_SHORT_DYNAMIC_BOX_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicShortDynamicBox(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldShortDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectDynamicUnboxed.AspectId)) {
            return Opt.some(AspectDynamicUnboxedShortDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicJvmShortBoxed.INSTANCE);
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
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SHORT_DYNAMIC_BOX_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return Numerics.isNumeric(xx, sourceType);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeIsEqual(xx, this) || Numerics.isNumeric(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        var unboxedCast = Numerics.castTo(xx, loc, cv, NumericKind.SHORT, new TypeShortDynamic());
        if (unboxedCast.hasErrors()) return unboxedCast.reerr();
        var castRes = DynUtil.valueReg(loc, unboxedCast.requireValue("TypeShortDynamicBox::typeCastFrom cast"));
        if (castRes.hasErrors()) return castRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var boxed = Numerics.box(b, NumericKind.SHORT, b.reg(castRes.requireValue("cast")));
        return EResult.ok(DynUtil.regToValue(b.code(), boxed, this));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeIsEqual(xx, this)) return EResult.ok(cv);
        var selfRes = DynUtil.valueReg(loc, cv);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var raw = Numerics.unbox(b, NumericKind.SHORT, b.reg(selfRes.requireValue("selfDyn")));
        var unboxed = DynUtil.regToValue(b.code(), raw, new TypeShortDynamic());
        var kkOpt = Numerics.kindOf(xx, destType);
        if (kkOpt.optIsSome()) {
            return Numerics.castTo(xx, loc, unboxed, kkOpt.optUnwrap(), destType);
        }
        return EResult.err(loc, "TypeShortDynamicBox::typeCastTo: destination is not numeric");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
