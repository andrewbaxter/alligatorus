package compilerxyz.main.eval.value.string;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
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
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorEq;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaCache;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;

@BuiltinPath("types.string")
@BuiltinFull(uniqueId = 6145L)
public record TypeStringDynamic() implements Aspect, Type {

    static final IMap<ConstKey, TypeStringMethodDynamic> METHOD_FIELDS = IMap.ofEntries(
            new Pair<>(new ConstKey.Str("toLowerCase"), new TypeStringMethodDynamic("toLowerCase")),
            new Pair<>(new ConstKey.Str("toUpperCase"), new TypeStringMethodDynamic("toUpperCase")));

    private static final MetaCache META = new MetaCache("string_dynamic");

    @Override
    public Meta typeMeta(ExprContext xx) {
        return META.get(xx, this);
    }

    @Override
    public String typeDesc() {
        return "dynamic string";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicStringDynamic(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectStringDynamic.AspectId)) {
            return Opt.some(AspectStringDynamicStringDynamic.INSTANCE);
        }
        if (key.equals(GenAspectOperatorEq.AspectId)) {
            return Opt.some(AspectOperatorEqStringDynamic.INSTANCE);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicStringDynamic(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(AspectLiftStringDynamic.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(AspectFieldStringDynamic.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicStringDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.STRING_DYNAMIC);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome()
                || GenAspectStringDynamic.getMaybe(xx, sourceType).optIsSome();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return GenAspectStringDynamic.getMaybe(xx, destType).optIsSome() || StringCastBytes.canCast(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeStringDynamic::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
        if (stringConstOpt.optIsSome()) {
            String strVal =
                    stringConstOpt.optUnwrap().aspectStringConstGet(cv.value().optUnwrap());
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var raw = b.constString(strVal);
            return EResult.ok(DynUtil.regToValue(b.code(), raw, this));
        }
        return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, this));
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (StringCastBytes.canCast(xx, destType)) {
            return StringCastBytes.cast(xx, loc, cv, destType);
        }
        return EResult.ok(cv);
    }
}
