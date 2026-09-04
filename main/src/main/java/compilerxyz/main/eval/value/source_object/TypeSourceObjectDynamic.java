package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter.Root;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr.Opaque;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.code.TypeCodeDynamic;
import compilerxyz.main.eval.value.function.FunctionParam;
import compilerxyz.main.eval.value.function.TypeSourceMethodDynamic;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.source_function.ExternalCodec;
import compilerxyz.main.eval.value.source_interface.TypeInterfaceMethodDynamic;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspect;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataDefSetObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6020L)
public final class TypeSourceObjectDynamic
        implements Type,
                AspectInitialBindDynamic,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectType,
                AspectLift,
                AspectStackValueDynamic,
                AspectFieldUseDynamic {

    public DefSetId setId;

    @SuppressWarnings("NullAway.Init")
    public final DefinitionSet set;

    public final Opt<String> name;
    public Opt<String> description = Opt.none();
    public Opt<Type> underlyingType = Opt.none();
    public final Unique identity;
    public final String binaryName;

    public final TList<SourceDataMethodData> methods;
    public final TList<SourceObjectInterfaceImplData> interfaceImpls;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();
    public final ArtifactSupport.ArtifactIdHolder newArtifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public Opt<byte[]> compiledBytecode = Opt.none();
    public Opt<TOrderedMap<String, Object>> compiledStaticFieldValues = Opt.none();

    public TypeSourceObjectDynamic(DefSetId setId, String name, Unique identity, DefinitionSet set, String binaryName) {
        this.setId = setId;
        this.set = set;
        this.name = Opt.some(name);
        this.identity = identity;
        this.binaryName = binaryName;
        this.methods = new TList<>();
        this.interfaceImpls = new TList<>();
    }

    @SuppressWarnings("NullAway")
    public TypeSourceObjectDynamic(DefSetId setId, Opt<String> name, Unique identity, String binaryName) {
        this.setId = setId;
        this.set = (DefinitionSet) null;
        this.name = name;
        this.identity = identity;
        this.binaryName = binaryName;
        this.methods = new TList<>();
        this.interfaceImpls = new TList<>();
    }

    public String internalName() {
        return binaryName.replace('.', '/');
    }

    public void initSetReflectively(DefinitionSet set) {
        try {
            var f = TypeSourceObjectDynamic.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(this, set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "TypeSourceObjectDynamic.initSetReflectively: failed to write final 'set' field", e);
        }
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        if (set == null) {
            return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
        }
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        return JavaDataDescriptor.fromInternalName(JavaInternalName.of(internalName()));
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return description.optUnwrapOr("object(" + name.optUnwrapOr("?") + ")");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_OBJECT_CONST);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)
                || key.equals(GenAspectFieldUseDynamic.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(this);
        }

        for (var impl : interfaceImpls) {
            var ifaceIdentity = impl.interface_.identity;
            boolean exposes = (ifaceIdentity.equals(GenAspectField.IdentityId) && key.equals(GenAspectField.AspectId))
                    || (ifaceIdentity.equals(GenAspectLift.IdentityId) && key.equals(GenAspectLift.AspectId))
                    || (ifaceIdentity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)
                            && key.equals(GenAspectType.AspectId));
            if (exposes) return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.target(DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsSome())
            return EResult.ok(dynCast.requireValue("dyn").optUnwrap());
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectDynamic::toDynamic: no compile-time value");
        }
        if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc)) {
            return EResult.err(
                    loc,
                    "TypeSourceObjectDynamic::toDynamic: expected ValueObjectConst, got "
                            + cv.value().optUnwrap().getClass().getName());
        }
        Object hostValue = voc.data();
        if (set != null) {
            xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        }
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, this, new External(hostValue.getClass(), hostValue)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        ConstKey.Str keyStr;
        switch (ck) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(
                        loc,
                        "Field "
                                + ck.constKeyToCanonical()
                                + " not found on object (underlying-type Record fallback deferred)");
            }
        }
        String keyName = keyStr.value();
        String fieldName = null;
        Type argType = null;
        Opt<Type> retType = Opt.none();
        for (var m : methods) {
            if (m.name.equals(keyName)) {
                retType = m.retType;
                argType = m.argType.optUnwrapOr(new TypeNullDynamic());
                fieldName = m.jvmMethodName;
                break;
            }
        }
        if (fieldName == null) {
            return EResult.err(
                    loc,
                    "TypeSourceObjectDynamic::field: field '"
                            + keyName
                            + "' not found on object (underlying-type Record fallback deferred)");
        }
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: receiver is not a ValueDynamic");
        }

        if (set != null) {
            xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        }
        JavaDataDescriptor retJvmDesc;
        if (retType.optIsSome()) {
            if (!(retType.optUnwrap() instanceof AspectConsumeDynamic retConsume)) {
                return EResult.err(
                        loc, "TypeSourceObjectDynamic::field: retType does not implement AspectConsumeDynamic");
            }
            retJvmDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
        } else {
            retJvmDesc = JavaDataDescriptor.DESC_VOID;
        }
        var slotsRes = DynUtil.argSignatureSlots(xx, loc, argType);
        if (slotsRes.hasErrors()) return slotsRes.reerr();
        var slots = slotsRes.requireValue("TypeSourceObjectDynamic::field argSignatureSlots");
        var argDescs = new TList<JavaDataDescriptor>(slots.size());
        for (var s : slots) argDescs.add(s.jvmDesc());
        var descObjObj = JavaMethodDescriptor.fromParts(retJvmDesc, argDescs);
        var objectInternalName = JavaInternalName.of(internalName());
        var fieldType = new TypeSourceMethodDynamic(
                this,
                TList.of(new FunctionParam("arg", argType)),
                retType,
                objectInternalName,
                fieldName,
                descObjObj,
                false,
                false);
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfMethodVd)) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: method receiver has no dynamic source");
        }
        return EResult.ok(
                new CodeValue(self.code(), EvalOutcome.ofValue(new ValueDynamic(selfMethodVd.source(), fieldType))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
        return EResult.ok(new ValueObjectConst(new TypeSourceObjectConst(this), externalHostValue.optUnwrapOr(null)));
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (destType.typeAspect(xx, identity).optIsSome()) {
            return true;
        }
        for (var impl : interfaceImpls) {
            if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
                return true;
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
                    return true;
                }
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.CODE_DYN_IDENTITY)) {
                    return true;
                }
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
            return true;
        }
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, identity).optIsSome()) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            boolean hasTypeImpl = false;
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
                    hasTypeImpl = true;
                    break;
                }
            }
            if (hasTypeImpl) {
                var sourceCodeRes = CodeDynamic.castRequired(
                        loc, cv.code(), "cast " + typeDesc() + " -> any: source has no runtime code");
                if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
                var sourceCode = sourceCodeRes.requireValue("sourceCode");
                return EResult.ok(emitBuiltinCast(xx, sourceCode, GenType.dynamicType()));
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
            for (var impl : interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.CODE_DYN_IDENTITY)) {
                    var codeSourceRes = CodeDynamic.castRequired(
                            loc, cv.code(), "cast " + typeDesc() + " -> code: source has no runtime code");
                    if (codeSourceRes.hasErrors()) return codeSourceRes.reerr();
                    var codeSource = codeSourceRes.requireValue("codeSource");
                    var wrapSeq = new JavaBytecodeSequence();
                    wrapSeq.add(codeSource);
                    wrapSeq.add(JavaBytecodeUtils.callStaticMethod(
                            new CodeLoc.Some(loc), GenCodegenDescriptors.CODE_TARGET));
                    return EResult.ok(new CodeValue(
                            Code.target(new CodeDynamic(new CodeLoc.Some(loc), wrapSeq)),
                            EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeCodeDynamic()))));
                }
            }
            return EResult.err(loc, "cast " + typeDesc() + " -> code: object does not implement code_dyn");
        }
        for (var impl : interfaceImpls) {
            if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
                var frozen = impl.interface_.frozen();
                Type resultType = null;
                if (frozen.builtinType.optIsSome() && !frozen.isAspect) {
                    resultType = frozen.builtinType.optUnwrap();
                }
                if (resultType != null) {
                    var sourceCodeRes = CodeDynamic.castRequired(
                            loc, cv.code(), "cast " + typeDesc() + " -> interface: source has no runtime code");
                    if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
                    var sourceCode = sourceCodeRes.requireValue("sourceCode");
                    return EResult.ok(emitBuiltinCast(xx, sourceCode, resultType));
                }
                return EResult.ok(new CodeValue(
                        cv.code(),
                        EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeSourceInterfaceDynamic(impl.interface_)))));
            }
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
            var sourceCodeRes = CodeDynamic.castRequired(
                    loc, cv.code(), "cast " + typeDesc() + " -> aspect: source has no runtime code");
            if (sourceCodeRes.hasErrors()) return sourceCodeRes.reerr();
            var sourceCode = sourceCodeRes.requireValue("sourceCode");
            return EResult.ok(emitBuiltinCast(xx, sourceCode, GenAspect.dynamicType()));
        }
        return EResult.err(loc, "Cannot convert " + typeDesc() + " to " + destType.typeDesc());
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return sourceType.typeAspect(xx, identity).optIsSome();
    }

    private CodeValue emitBuiltinCast(ExprContext xx, CodeDynamic sourceCode, Type resultType) {
        return new CodeValue(Code.target(sourceCode), EvalOutcome.ofValue(ValueDynamic.stack(xx, resultType)));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public Type aspectTypeType(Value value) {
        return this;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var dt = MustCast.mustCast(TypeSourceObjectDynamic.class, self);
            var set = dt.set;
            if (set == null) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                                + dt.name.optUnwrapOr("(unnamed)")
                                + "' is not attached to a definition set");
            }
            if (dt.underlyingType.optIsNone()) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceObjectConst::exporterRootDelegateSelfExport: object '"
                                + dt.name.optUnwrapOr("(unnamed)")
                                + "' missing underlying type");
            }
            var errors = new TList<ExprErr>();
            var underlyingFinal = dt.underlyingType.optUnwrap();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, dt.artifactIdCell, () -> {
                        var defSetArtRes = Artifactable.exportArtifact(xx, loc, set);
                        defSetArtRes.drainInto(errors);
                        var defSetArt = defSetArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var underlyingArtRes = Artifactable.exportArtifact(xx, loc, underlyingFinal);
                        underlyingArtRes.drainInto(errors);
                        var underlyingArt = underlyingArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var methods = new TList<ArtifactDataDefSetMethod>();
                        for (var m : dt.methods) {
                            var argT = m.argType;
                            var retT = m.retType;
                            if (argT.optIsNone()) {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object '"
                                                + dt.name.optUnwrapOr("(unnamed)")
                                                + "' method '"
                                                + m.name
                                                + "' missing arg/ret type"))));
                                continue;
                            }
                            if (m.compiledBodyCode.optIsNone()) {
                                var objInternalName = dt.internalName();
                                var finishRes = m.finish(
                                        xx,
                                        loc,
                                        dt.name.optUnwrapOr("(unnamed)"),
                                        dt,
                                        underlyingFinal,
                                        objInternalName);
                                finishRes.drainInto(errors);
                            }
                            if (m.compiledBodyCode.optIsNone()) {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object method '"
                                                + m.name
                                                + "' has not been compiled"))));
                                continue;
                            }
                            var argArtRes = Artifactable.exportArtifact(xx, loc, argT.optUnwrap());
                            argArtRes.drainInto(errors);
                            var argArt = argArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            Artifact retArt;
                            if (retT.optIsSome()) {
                                var retArtRes = Artifactable.exportArtifact(xx, loc, retT.optUnwrap());
                                retArtRes.drainInto(errors);
                                retArt = retArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            } else {
                                retArt = nullArtifact();
                            }
                            var extRes = ExternalCodec.exportExternals(xx, loc, m.capturedExternals);
                            extRes.drainInto(errors);
                            var ext = extRes.valueOpt().optUnwrapOr(TList.<ArtifactExternal>of());
                            methods.add(new ArtifactDataDefSetMethod(m.name, argArt, retArt, m.jvmMethodName, ext));
                        }
                        var ifaceImpls = new TList<ArtifactDataDefSetInterfaceImpl>();
                        for (var impl : dt.interfaceImpls) {
                            var ifaceArtRes = Artifactable.exportArtifact(
                                    xx, loc, new TypeSourceInterfaceDynamic(impl.interface_));
                            ifaceArtRes.drainInto(errors);
                            var ifaceArt = ifaceArtRes.valueOpt().optUnwrapOr(nullArtifact());
                            var methodImpls = new TList<ArtifactDataDefSetInterfaceMethodImpl>();
                            for (var mi : impl.methodImpls) {
                                if (mi.compiledBodyCode.optIsNone()) {
                                    var finishRes = mi.finish(xx, loc, dt, underlyingFinal);
                                    finishRes.drainInto(errors);
                                }
                                if (mi.compiledBodyCode.optIsNone()) {
                                    errors.add(new ExprErr(
                                            loc,
                                            TList.of(new Opaque(
                                                    "TypeTypeSourceObjectConst::exporterExport: iface method impl '"
                                                            + mi.interfaceMethod.name
                                                            + "' has not been compiled"))));
                                    continue;
                                }
                                var methodArtRes = Artifactable.exportArtifact(
                                        xx, loc, new TypeInterfaceMethodDynamic(impl.interface_, mi.interfaceMethod));
                                methodArtRes.drainInto(errors);
                                var methodArt = methodArtRes.valueOpt().optUnwrapOr(nullArtifact());
                                var extRes = ExternalCodec.exportExternals(xx, loc, mi.capturedExternals);
                                extRes.drainInto(errors);
                                var ext = extRes.valueOpt().optUnwrapOr(TList.<ArtifactExternal>of());
                                methodImpls.add(
                                        new ArtifactDataDefSetInterfaceMethodImpl(methodArt, mi.jvmMethodName, ext));
                            }
                            ifaceImpls.add(new ArtifactDataDefSetInterfaceImpl(ifaceArt, methodImpls));
                        }
                        if (dt.compiledBytecode.optIsNone()) {
                            errors.add(new ExprErr(
                                    loc,
                                    TList.of(new Opaque("TypeTypeSourceObjectConst::exporterExport: object '"
                                            + dt.name.optUnwrapOr("(unnamed)")
                                            + "' has not been compiled — its definition set must be"
                                            + " finished and installed before export"))));
                        }
                        var body = new ArtifactDataDefSetObject(
                                defSetArt,
                                set.defSetId,
                                dt.name,
                                dt.identity,
                                underlyingArt,
                                methods,
                                ifaceImpls,
                                dt.binaryName,
                                dt.compiledBytecode);
                        return makeBody.apply(ArtifactDataDefSetObjectCodec.toValue(body));
                    });
            if (errors.isEmpty()) {
                return EResult.ok(artifact);
            }
            return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
            return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new Root()));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeTypeSourceObjectConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataDefSetObject body;
                        try {
                            body = ArtifactDataDefSetObjectCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeTypeSourceObjectConst type_: failed to parse "
                                    + "ArtifactDataDefSetObject: "
                                    + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var dt = new TypeSourceObjectDynamic(
                                body.defSetId(), body.name(), body.identity(), body.binaryName());
                        dt.artifactIdCell.set(idValue);
                        dt.compiledBytecode = body.compiledBytecode();
                        for (var m : body.methods()) {
                            var md = new SourceDataMethodData(m.name(), m.jvmMethodName());
                            dt.methods.add(md);
                        }

                        var deps = new TList<Artifact>();
                        deps.add(body.defSet());
                        deps.add(body.underlyingType());
                        for (var m : body.methods()) {
                            deps.add(m.argType());
                            deps.add(m.retType());
                            for (var ce : m.capturedExternals()) {
                                switch (ce) {
                                    case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                                    case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                                    case ArtifactExternal.StagedExpr ignored -> {}
                                    case ArtifactExternal.UniqueRef ignored -> {}
                                    case ArtifactExternal.Loc ignored -> {}
                                }
                            }
                        }
                        for (var impl : body.interfaceImpls()) {
                            deps.add(impl.iface());
                            for (var mi : impl.methodImpls()) {
                                deps.add(mi.interfaceMethod());
                                for (var ce : mi.capturedExternals()) {
                                    switch (ce) {
                                        case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                                        case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                                        case ArtifactExternal.StagedExpr ignored -> {}
                                        case ArtifactExternal.UniqueRef ignored -> {}
                                        case ArtifactExternal.Loc ignored -> {}
                                    }
                                }
                            }
                        }
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var defSetAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.defSet());
                            var defSetVal =
                                    defSetAny.requireValue("TypeTypeSourceObjectConst::finalizer defSet artifact");
                            var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                            dt.initSetReflectively(set);
                            dt.setId = set.defSetId;
                            var underlyingRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.underlyingType());
                            var underlyingVal =
                                    underlyingRes.requireValue("TypeTypeSourceObjectConst::finalizer underlying type");
                            dt.underlyingType = Opt.some(underlyingVal);
                            var bodyMethodsIt = body.methods().iterator();
                            for (var md : dt.methods) {
                                var sm = bodyMethodsIt.next();
                                var argRes =
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, sm.argType());
                                var argVal =
                                        argRes.requireValue("TypeTypeSourceObjectConst::finalizer method arg type");
                                md.argType = Opt.some(argVal);
                                var retRes =
                                        ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, sm.retType());
                                var retVal =
                                        retRes.requireValue("TypeTypeSourceObjectConst::finalizer method ret type");
                                md.retType = Opt.some(retVal);
                                var extRes = ExternalCodec.importExternals(xx2, loc2, sm.capturedExternals());
                                var extVal =
                                        extRes.requireValue("TypeTypeSourceObjectConst::finalizer method externals");
                                md.capturedExternals.addAll(extVal);
                            }
                            for (var impl : body.interfaceImpls()) {
                                var ifaceAny =
                                        ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, impl.iface());
                                var ifaceVal =
                                        ifaceAny.requireValue("TypeTypeSourceObjectConst::finalizer iface artifact");
                                var iface = MustCast.mustCast(TypeSourceInterfaceDynamic.class, ifaceVal)
                                        .cell();
                                var implData = new SourceObjectInterfaceImplData(iface);
                                for (var mi : impl.methodImpls()) {
                                    var methodAny = ArtifactSupport.independentFinalizeImportArtifact(
                                            xx2, loc2, mi.interfaceMethod());
                                    var methodVal = methodAny.requireValue(
                                            "TypeTypeSourceObjectConst::finalizer iface method artifact");
                                    var mid = new SourceObjectInterfaceMethodImplData(
                                            MustCast.mustCast(TypeInterfaceMethodDynamic.class, methodVal)
                                                    .method(),
                                            mi.jvmMethodName());
                                    var extRes = ExternalCodec.importExternals(xx2, loc2, mi.capturedExternals());
                                    var extVal = extRes.requireValue(
                                            "TypeTypeSourceObjectConst::finalizer iface method externals");
                                    mid.capturedExternals.addAll(extVal);
                                    implData.methodImpls.add(mid);
                                }
                                dt.interfaceImpls.add(implData);
                            }
                            dt.compiledStaticFieldValues = Opt.some(SourceObjectEmitter.rebuildStaticFieldValues(dt));
                            return dt;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(dt), deps, fin));
                    });
        }
    };
}
