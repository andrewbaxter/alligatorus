package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenInterfaceData;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataDefSetInterfaceCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6004L)
public record TypeSourceInterfaceDynamic(InterfaceData data)
        implements TypeAspect,
                Type,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectType {

    @Override
    public Unique typeAspectId() {
        if (data.aspectId.optIsNone()) {
            throw new IllegalStateException(
                    "TypeSourceInterfaceDynamic::typeAspectId: interface is not an aspect (no aspect id)");
        }
        return data.aspectId.optUnwrap();
    }

    @CodegenDescriptor
    public InterfaceData getInterfaceData() {
        return data;
    }

    public static Type dynamicType() {
        return TypeTypeSourceInterfaceDynamic.GENERIC;
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var binaryName = data.compiledBinaryName;
        if (binaryName == null) {
            return JavaDataDescriptor.fromClass(TypeSourceInterfaceDynamic.class);
        }
        return JavaDataDescriptor.fromInternalName(JavaInternalName.of(binaryName.replace('.', '/')));
    }

    @Override
    public String typeDesc() {
        return data.description.optUnwrapOr("interface");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(data.identity)
                || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectType.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_INTERFACE_DYNAMIC;
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var iface = MustCast.mustCast(TypeSourceInterfaceDynamic.class, self).data;
            var set = iface.set;
            if (set == null) {
                return EResult.err(
                        loc,
                        "TypeSourceInterfaceDynamic::exporterRootDelegateSelfExport: interface not attached to a"
                                + " definition set");
            }
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, iface.artifactIdCell, () -> {
                        var defSetArtRes = Artifactable.exportArtifact(xx, loc, set);
                        defSetArtRes.drainInto(errors);
                        var defSetArt = defSetArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var methodsOut = new TList<ArtifactDataDefSetInterfaceMethodData>();
                        for (var m : iface.methods) {
                            var argT = m.argType;
                            var retT = m.retType;
                            Artifact argArt;
                            {
                                var res = Artifactable.exportArtifact(xx, loc, argT);
                                res.drainInto(errors);
                                argArt = res.valueOpt().optUnwrapOr(nullArtifact());
                            }
                            Artifact retArt;
                            if (retT.optIsSome()) {
                                var res = Artifactable.exportArtifact(xx, loc, retT.optUnwrap());
                                res.drainInto(errors);
                                retArt = res.valueOpt().optUnwrapOr(nullArtifact());
                            } else {
                                retArt = nullArtifact();
                            }
                            methodsOut.add(new ArtifactDataDefSetInterfaceMethodData(
                                    m.name, argArt, retArt, TList.from(m.dataFieldNames)));
                        }
                        var body = new ArtifactDataDefSetInterface(
                                defSetArt,
                                set.defSetId,
                                iface.identity,
                                iface.description,
                                iface.isAspect,
                                iface.aspectId,
                                iface.builtinKind,
                                iface.compiledBinaryName,
                                iface.compiledBytecode,
                                methodsOut);
                        return makeBody.apply(ArtifactDataDefSetInterfaceCodec.toValue(body));
                    });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeSourceInterfaceDynamic requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataDefSetInterface body;
                        try {
                            body = ArtifactDataDefSetInterfaceCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeSourceInterfaceDynamic data: failed to parse ArtifactDataDefSetInterface: "
                                            + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var iface = new InterfaceData(Opt.some(body.defSetId()), body.identity(), body.builtinKind());
                        iface.compiledBinaryName = body.compiledBinaryName();
                        iface.compiledBytecode = body.compiledBytecode();
                        iface.description = body.description();
                        iface.isAspect = body.isAspect();
                        iface.aspectId = body.aspectId();
                        iface.artifactIdCell.set(idValue);
                        for (var m : body.methods()) {
                            var md = new InterfaceMethodData(m.name());
                            md.dataFieldNames.addAll(m.dataFieldNames());
                            iface.methods.add(md);
                        }
                        var imported = new TypeSourceInterfaceDynamic(iface);
                        var deps = new TList<Artifact>();
                        deps.add(body.defSet());
                        for (var m : body.methods()) {
                            deps.add(m.argType());
                            deps.add(m.retType());
                        }
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var defSetAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, body.defSet());
                            var defSetVal =
                                    defSetAny.requireValue("TypeSourceInterfaceDynamic::finalizer defSet artifact");
                            var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                            iface.initSetReflectively(set);
                            var methodsIt = body.methods().iterator();
                            for (var md : iface.methods) {
                                var mBody = methodsIt.next();
                                var argRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                        xx2, loc2, mBody.argType());
                                md.argType = argRes.requireValue("TypeSourceInterfaceDynamic::finalizer method arg");
                                var retRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                        xx2, loc2, mBody.retType());
                                md.retType = Opt.some(
                                        retRes.requireValue("TypeSourceInterfaceDynamic::finalizer method ret"));
                            }
                            return imported;
                        };
                        return new Result.Ok<>(
                                new ArtifactSupport.ImportIndependentResult(Opt.some(imported), deps, fin));
                    });
        }
    };

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        return EResult.err(loc, "TypeSourceInterfaceDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, data.identity).optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                        .optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY)
                        .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, data.identity).optIsSome()) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
                .optIsSome()) {
            var sourceCode = cv.code().codeDynamic();
            if (sourceCode == null) {
                return EResult.err(loc, "cast source_interface_dynamic → interface_ref: source has no runtime code");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(sourceCode);
            seq.add(JavaBytecodeUtils.callMethod(
                    loc, GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_DATA));
            return EResult.ok(new CodeValue(
                    Code.of(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
        }
        return EResult.err(loc, "Cannot cast interface_const to " + destType.typeDesc());
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, new TypeSourceInterfaceDynamic(data));
    }

    @Override
    public Type aspectTypeType(Value value) {
        return GenInterfaceData.dynamicType();
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, data.identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(aspectArtifactExporter());
    }
}
