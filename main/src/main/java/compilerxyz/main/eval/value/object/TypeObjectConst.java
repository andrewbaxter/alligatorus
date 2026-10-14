package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectArtifactObjectConst;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectAspect;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenDynamicTypes;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
@BuiltinFull(uniqueId = 6129L)
public record TypeObjectConst(TypeObjectMeta meta, Class externalHostClass) implements Aspect, Type, TypeAspect {

    public String typeDesc() {
        return meta.typeDesc();
    }

    public IList<TypeObjectCastRule> castRules() {
        return meta.castRules();
    }

    public IList<TypeObjectCastFromRule> castFromRules() {
        return meta.castFromRules();
    }

    public Opt<ObjectAspectSource> aspectSource() {
        return meta.aspectSource();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY, TypeObjectConst.class);
        return oOpt.optIsSome()
                && externalHostClass
                        .getName()
                        .equals(oOpt.optUnwrap().externalHostClass.getName())
                && meta.typeDesc().equals(oOpt.optUnwrap().meta.typeDesc());
    }

    @Override
    public Unique typeAspectId() {
        var aspSrcOpt = aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::id: not an aspect metatype");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static s -> staticSrc = s;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(typeDesc() + "::id: aspect id is not statically known");
        }
        return staticSrc.id();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindObjectConst(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(new AspectTypeObjectConst(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            return Opt.some(new AspectLiftObjectConst(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldObjectConst(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicObjectConst(this));
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicObjectConst.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindObjectConst.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactObjectConst.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        if (aspectSource().optIsSome() && key.equals(GenAspectAspect.AspectId)) {
            return Opt.some(new AspectAspectObjectConst(this));
        }
        for (var idAspect : meta.trivialIdentityAspects()) {
            if (key.equals(idAspect)) return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (AspectLookup.getMaybe(
                        xx, destType, GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY, TypeObjectConst.class)
                .optIsSome()) {
            return false;
        }
        for (var rule : castRules()) if (rule.typeObjectCastRuleMatches(xx, destType)) return true;
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        for (var rule : castRules()) {
            if (rule.typeObjectCastRuleMatches(xx, destType)) {
                CodeValue materialized = cv;
                var codeCast = CodeDynamic.castOpt(loc, cv.code());
                if (codeCast.hasErrors()) return codeCast.reerr();
                if (codeCast.requireValue("code").optIsNone()) {
                    var dynRes = new AspectConsumeDynamicObjectConst(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
                    if (dynRes.hasErrors()) return dynRes.reerr();
                    materialized = dynRes.requireValue(typeDesc() + "::typeCastTo materialize");
                }
                return rule.typeObjectCastRuleApply(xx, loc, materialized, destType);
            }
        }
        return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        for (var rule : castFromRules()) if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) return true;
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
        }
        var sourceType = cv.value().optUnwrap().valueType();
        for (var rule : castFromRules()) {
            if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) {
                return rule.typeObjectCastFromRuleApply(xx, loc, this, cv);
            }
        }
        return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
    }

    EResult<Type> extractInnerType(QExprId loc, CodeValue self, String op) {
        if (self.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::" + op + ": self has no compile-time value");
        }
        var v = self.value().optUnwrap();
        if (v instanceof ValueObjectConst wrap) {
            if (!(wrap.data() instanceof Type inner)) {
                var dataDesc = wrap.data() instanceof ValueObjectConst innerWrap
                        ? "value_object_const of " + innerWrap.type().typeDesc() + " over "
                                + innerWrap.data().getClass().getName()
                        : wrap.data().getClass().getName();
                throw new IllegalStateException(typeDesc() + "::" + op + ": type value of "
                        + wrap.type().typeDesc() + " does not hold a Type: " + dataDesc);
            }
            return EResult.ok(inner);
        }
        return EResult.err(loc, typeDesc() + "::" + op + ": expected ValueObjectConst, got " + v.getClass());
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.TYPE_OBJECT_CONST);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var type = MustCast.mustCast(TypeObjectConst.class, self);
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueString(type.externalHostClass.getName()))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            String className;
            switch (data) {
                case JsonValueString s -> className = s.value();
                case JsonValueNull ignored -> {
                    return EResult.err(loc, "TypeObjectConst::import: expected host class name string, got " + data);
                }
                case JsonValueBoolean ignored -> {
                    return EResult.err(loc, "TypeObjectConst::import: expected host class name string, got " + data);
                }
                case JsonValueNumber ignored -> {
                    return EResult.err(loc, "TypeObjectConst::import: expected host class name string, got " + data);
                }
                case JsonValueArray ignored -> {
                    return EResult.err(loc, "TypeObjectConst::import: expected host class name string, got " + data);
                }
                case JsonValueObject ignored -> {
                    return EResult.err(loc, "TypeObjectConst::import: expected host class name string, got " + data);
                }
            }
            var found = GenDynamicTypes.byClassName(className);
            if (found.optIsNone()) {
                return EResult.err(
                        loc, "TypeObjectConst::import: no registered dynamic type for host class " + className);
            }
            var td = MustCast.mustCast(TypeObjectDynamic.class, found.optUnwrap());
            return EResult.ok(td.pairedConst());
        }
    };
}
