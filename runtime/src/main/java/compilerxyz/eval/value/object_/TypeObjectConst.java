package compilerxyz.eval.value.object_;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.value.function.TypeMethodDynamic;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeInterfaceMethodBuiltin;
import compilerxyz.gen.AspectAspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.util.IList;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectAspect;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.TypeAspect;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.safejson.JsonValue;
import compilerxyz.eval.Eval;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.gen.GenType;
import compilerxyz.util.Result;

@SuppressWarnings("rawtypes")
@BuiltinLight(uniqueId = 6129L)
public record TypeObjectConst(
        TypeObjectMeta meta,
        Class externalHostClass,
        Opt<ExporterRoot> exporter)
        implements
        Type,
        TypeAspect,
        AspectArtifact,
        ArtifactExporter,
        AspectAspect,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectFunction,
        AspectInitialBind,
        AspectLift,
        AspectType {

    public static TypeObjectConst metaOf(Class<?> hostClass, String descSuffix) {
        var trivialIdentityAspects = new TList<Unique>();
        trivialIdentityAspects.add(GenTrivialIdentityAspects.TYPE_IDENTITY);
        var castRules = new TList<TypeObjectCastRule>();
        castRules.add(new TypeObjectCastRule.RestampToIdentity(GenTrivialIdentityAspects.TYPE_IDENTITY));
        var meta = new TypeObjectMeta("type<" + descSuffix + ">", IList.from(trivialIdentityAspects), IList.from(castRules), IList.<TypeObjectCastFromRule>of(), Opt.<ObjectAspectSource>none());
        return new TypeObjectConst(
                meta,
                hostClass,
                Opt.<ExporterRoot>none());
    }

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
        var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY, TypeObjectConst.class);
        return oOpt.optIsSome()
                && externalHostClass.getName().equals(oOpt.optUnwrap().externalHostClass.getName())
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
            case ObjectAspectSource.FromInterfaceData ignored -> throw new IllegalStateException(typeDesc() + "::id: aspect id is not statically known");
        }
        return staticSrc.id();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectType.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectFunction.AspectId)
                || key.equals(GenAspectLift.AspectId)
                || key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        if (aspectSource().optIsSome() && key.equals(GenAspectAspect.AspectId)) {
            return Opt.some(this);
        }
        for (var idAspect : meta.trivialIdentityAspects()) {
            if (key.equals(idAspect)) return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public Unique aspectAspectGetUnique(InterfaceData value) {
        var aspSrcOpt2 = aspectSource();
        if (aspSrcOpt2.optIsNone()) {
            throw new IllegalStateException(typeDesc() + "::getUnique: no aspect source");
        }
        return switch (aspSrcOpt2.optUnwrap()) {
            case ObjectAspectSource.Static staticSrc -> staticSrc.id();
            case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
        };
    }

    @Override
    public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
        var aspSrcOpt3 = aspectSource();
        if (aspSrcOpt3.optIsNone()) {
            throw new UnsupportedOperationException(typeDesc() + "::getIfaceData: no aspect source");
        }
        return switch (aspSrcOpt3.optUnwrap()) {
            case ObjectAspectSource.Static ignored -> throw new UnsupportedOperationException(
                    typeDesc() + "::getIfaceData: static aspect has no InterfaceData");
            case ObjectAspectSource.FromInterfaceData ignored -> value;
        };
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        for (var rule : castRules()) if (rule.typeObjectCastRuleMatches(xx, destType)) return true;
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        for (var rule : castRules()) {
            if (rule.typeObjectCastRuleMatches(xx, destType)) {
                CodeValue materialized = cv;
                if (cv.code().codeDynamic() == null) {
                    var dynRes = aspectConsumeDynamicToDynamic(xx, loc, cv);
                    if (dynRes.hasErrors()) return dynRes.reerr();
                    materialized =
                            new CodeValue(
                                    Code.of(loc, dynRes.requireValue(typeDesc() + "::typeCastTo materialize")),
                                    cv.outcome());
                }
                return rule.typeObjectCastRuleApply(xx, null, loc, materialized, destType);
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
                return rule.typeObjectCastFromRuleApply(xx, null, loc, cv, sourceType);
            }
        }
        return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(externalHostClass);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        if (cv.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::toDynamic: no compile-time value");
        }
        Object hostValue = cv.value().optUnwrap();
        if (hostValue instanceof ValueObjectConst wrap) {
            hostValue = wrap.data();
        }
        var key = new JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        xx.vx.externals.add(
                new EvalContext.ExternalEntry(
                        key, this, new External(externalHostClass, hostValue)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(new BindResult(
                    Code.none(loc), new BindingValue(cv.value().optUnwrap())));
        }
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
            ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(Opt.none());
        }
        return DynUtil.bindDynamicDefault(xx, loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(xx, loc, cv);
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return exporter.optIsSome() ? exporter.optUnwrap() : this;
    }

    @Override
    public EResult<Artifact> artifactExporterExport(
            ExprContext xx, QExprId loc, ExporterAny self) {
        if (self instanceof ValueObjectConst wrapper
                && wrapper.data() instanceof Type innerType) {
            return innerType.artifactableExportArtifact(xx, loc);
        }
        return EResult.err(
                loc, typeDesc() + "::artifactExporterExport: expected ValueObjectConst-of-Type, got " + self.getClass());
    }

    @Override
    public EResult<ExporterAny> artifactExporterImportFromData(
            ExprContext xx,
            QExprId loc,
            Opt<ArtifactIdSource> id,
            JsonValue data) {
        return EResult.err(loc, typeDesc() + "::artifactExporterImportFromData: should not be invoked");
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var innerRes = extractInnerType(loc, self, "field");
        if (innerRes.hasErrors()) return innerRes.reerr();
        var inner = innerRes.requireValue(typeDesc() + "::field inner");
        if (key.value().optIsSome()) {
            var ckRes = Eval.extractConstKey(xx, loc, key.value().optUnwrap());
            switch (ckRes) {
                case Result.Ok<ConstKey, String> ok -> {
                    if (GenType.Holder.INSTANCE.aspectFieldTypeOpt(xx, ok.value()).optIsSome()) {
                        CodeValue selfLifted = self;
                        if (self.code().codeIsNone()) {
                            var liftRes = aspectConsumeDynamicToDynamic(xx, loc, self);
                            if (liftRes.hasErrors()) return liftRes.reerr();
                            selfLifted =
                                    new CodeValue(
                                            Code.of(loc, liftRes.requireValue(typeDesc() + "::field lift")),
                                            self.outcome());
                        }
                        return GenType.Holder.INSTANCE.aspectFieldField(xx, loc, selfLifted, key);
                    }
                }
                case Result.Err<ConstKey, String> ignored -> {
                }
            }
        }
        if (inner instanceof TypeObjectDynamic tod && tod.interfaceRepresented().optIsSome()) {
            if (key.value().optIsNone()) {
                return EResult.err(loc, typeDesc() + "::field: key has no compile-time value");
            }
            var ckRes = Eval.extractConstKey(xx, loc, key.value().optUnwrap());
            ConstKey ck;
            switch (ckRes) {
                case Result.Ok<ConstKey, String> ok -> ck = ok.value();
                case Result.Err<ConstKey, String> e -> {
                    return EResult.err(loc, e.error());
                }
            }
            ConstKey.Str keyStr;
            switch (ck) {
                case ConstKey.Str s -> keyStr = s;
                case ConstKey.Uint u -> {
                    return EResult.err(
                            loc, "Method " + ck.constKeyToCanonical() + " not found on interface");
                }
            }
            var keyName = keyStr.value();
            for (var m : tod.methods()) {
                if (m.langName().equals(keyName)) {
                    if (!(m.funcType() instanceof TypeMethodDynamic mt)) {
                        return EResult.err(
                                loc,
                                typeDesc() + "::field: interface method '" + keyName
                                        + "' funcType is not TypeMethodDynamic");
                    }
                    var refType = new TypeInterfaceMethodBuiltin(tod, keyName, mt);
                    return EResult.ok(
                            new CodeValue(
                                    self.code(), EvalOutcome.ofValue(new ValueObjectConst(refType, refType))));
                }
            }
            return EResult.err(loc, "Method '" + keyName + "' not found on interface");
        }
        var asp = GenAspectField.get(xx, loc, inner);
        if (asp.hasErrors()) return asp.reerr();
        return asp.requireValue(typeDesc() + "::field inner AspectField").aspectFieldField(xx, loc, self, key);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(
            ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var innerRes = extractInnerType(loc, self, "call");
        if (innerRes.hasErrors()) return innerRes.reerr();
        var inner = innerRes.requireValue(typeDesc() + "::call inner");
        var asp = GenAspectFunction.get(xx, loc, inner);
        if (asp.hasErrors()) return asp.reerr();
        return asp.requireValue(typeDesc() + "::call inner AspectFunction")
                .aspectFunctionCall(xx, loc, self, argument);
    }

    private EResult<Type> extractInnerType(QExprId loc, CodeValue self, String op) {
        if (self.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::" + op + ": self has no compile-time value");
        }
        var v = self.value().optUnwrap();
        if (v instanceof ValueObjectConst wrap) {
            return EResult.ok((Type) wrap.data());
        }
        if (v instanceof Type t) {
            return EResult.ok(t);
        }
        return EResult.err(
                loc,
                typeDesc() + "::" + op + ": expected Type or ValueObjectConst, got " + v.getClass());
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        var innerRes = extractInnerType(loc, self, "exception_type");
        if (!innerRes.hasErrors()
                && innerRes.requireValue(typeDesc() + "::exception_type inner")
                instanceof AspectFunction innerAsp) {
            return innerAsp.aspectFunctionExceptionType(xx, loc, self);
        }
        return Opt.none();
    }

    @Override
    public Type aspectTypeType(Value value) {
        if (value instanceof ValueObjectConst v) {
            return (Type) v.data();
        }
        if (value instanceof Type t) {
            return t;
        }
        throw new IllegalArgumentException(
                typeDesc() + "::type_: expected Type or ValueObjectConst, got " + value.getClass());
    }

    @Override
    public EResult<Value> aspectLiftLift(
            ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(this, externalHostValue.optUnwrap()));
    }


    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
