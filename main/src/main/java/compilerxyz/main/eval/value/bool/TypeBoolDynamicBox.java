package compilerxyz.main.eval.value.bool;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.AspectDynamicConstOf;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectBoolConst;
import compilerxyz.main.gen.GenAspectBoolDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectDynamicUnboxed;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinLight(uniqueId = 6104L)
@BuiltinPath("types.bool_box")
public record TypeBoolDynamicBox() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "dynamic bool";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstOf(new TypeBoolConst()));
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindBoolDynamicBox(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_BOOL_DYNAMIC_BOX_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDynamicUnboxed.AspectId)) {
            return Opt.some(AspectDynamicUnboxedBoolDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicBoolDynamicBox.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BOOL_DYNAMIC_BOX_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectBoolConst.getMaybe(xx, sourceType).optIsSome()
                || GenAspectBoolDynamic.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeIsEqual(xx, this)
                || GenAspectBoolDynamic.getMaybe(xx, destType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeBoolDynamicBox::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        var boolConstOpt = GenAspectBoolConst.getMaybe(xx, sourceType);
        var b = new JavaBytecodeBuilder(loc);
        JavaBytecodeReg raw;
        if (boolConstOpt.optIsSome()) {
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            raw = b.constBool(
                    boolConstOpt.optUnwrap().aspectBoolConstGet(cv.value().optUnwrap()));
        } else if (sourceType instanceof TypeBoolDynamic) {
            var dynRes = DynUtil.valueReg(xx, loc, cv);
            if (dynRes.hasErrors()) return dynRes.reerr();
            raw = b.reg(dynRes.requireValue("TypeBoolDynamicBox::typeCastFrom realize"));
        } else {
            return EResult.err(loc, "TypeBoolDynamicBox::typeCastFrom: source is not a bool");
        }
        var boxed = b.invokeStatic(
                JavaInternalName.fromClass(Boolean.class),
                "valueOf",
                JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Boolean.class)),
                        TList.of(JavaDataDescriptor.DESC_BOOL)),
                raw);
        return EResult.ok(DynUtil.regToValue(b.code(), boxed, this));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeIsEqual(xx, this)) return EResult.ok(cv);
        var selfRes = DynUtil.valueReg(xx, loc, cv);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var raw = b.invokeVirtual(
                JavaInternalName.fromClass(Boolean.class),
                "booleanValue",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of()),
                b.reg(selfRes.requireValue("selfDyn")));
        return EResult.ok(DynUtil.regToValue(b.code(), raw, new TypeBoolDynamic()));
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.none();
    }
}
