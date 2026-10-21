package compilerxyz.main.eval.value.constkey;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinLight(uniqueId = 6015L)
@BuiltinPath("types.constkey")
public record TypeConstkeyDynamic() implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "constkey";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindConstkeyDynamic(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicConstkeyDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome()
                || GenAspectStringDynamic.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeConstkeyDynamic::typeCastFrom: CodeValue has no value");
        }
        var sourceValue = cv.value().optUnwrap();
        var sourceType = sourceValue.valueType();
        var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
        var strInternal = JavaInternalName.fromClass(ConstKey.Str.class);
        var ctorDesc = JavaMethodDescriptor.fromConstructorParts(TList.of(JavaDataDescriptor.fromClass(String.class)));
        var b = new JavaBytecodeBuilder(loc);
        JavaBytecodeReg str;
        if (stringConstOpt.optIsSome()) {
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            str = b.constString(stringConstOpt.optUnwrap().aspectStringConstGet(sourceValue));
        } else {
            if (GenAspectStringDynamic.getMaybe(xx, sourceType).optIsNone()) {
                return EResult.err(loc, "Cannot convert " + sourceType.typeDesc() + " to constkey");
            }
            var dynRes = DynUtil.materialize(xx, loc, cv);
            if (dynRes.hasErrors()) return dynRes.reerr();
            str = b.reg(dynRes.requireValue("constkey from dynamic string"));
        }
        var out = b.newInstance(strInternal, ctorDesc, str);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeConstkeyDynamic()));
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)
                .optIsSome()) {
            return EResult.ok(cv);
        }
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.none();
    }
}
