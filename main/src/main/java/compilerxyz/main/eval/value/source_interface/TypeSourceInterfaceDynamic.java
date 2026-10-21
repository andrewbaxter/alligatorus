package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.TypeAspect;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectBindDynamicDefault;
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenBuiltinInterfaces;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataDefSetInterfaceCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6004L)
public record TypeSourceInterfaceDynamic(InterfaceCell cell) implements Aspect, TypeAspect, Type {

    @Override
    public Unique typeAspectId() {
        var aspectId = cell.aspectId();
        if (aspectId.optIsNone()) {
            throw new IllegalStateException(
                    "TypeSourceInterfaceDynamic::typeAspectId: interface is not an aspect (no aspect id)");
        }
        return aspectId.optUnwrap();
    }

    @CodegenDescriptor
    public InterfaceCell getInterfaceCell() {
        return cell;
    }

    public static Type dynamicType() {
        return TypeTypeSourceInterfaceDynamic.GENERIC;
    }

    @Override
    public String typeDesc() {
        return cell.description().optUnwrapOr("interface");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(AspectTypeSourceInterfaceDynamic.INSTANCE);
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicSourceInterfaceDynamic(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(AspectBindDynamicDefault.INSTANCE);
        }
        if (key.equals(cell.identity) || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var cell = MustCast.mustCast(TypeSourceInterfaceDynamic.class, self).cell;
            if (cell.set.optIsNone()) {
                var body = new ArtifactDataDefSetInterface.Builtin(cell.identity);
                return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactDataDefSetInterfaceCodec.toValue(body))));
            }
            var set = cell.set.optUnwrap();
            var frozenOpt = cell.frozenOpt();
            if (frozenOpt.optIsNone()) {
                return EResult.err(
                        loc,
                        "TypeSourceInterfaceDynamic::export: interface '"
                                + cell.description().optUnwrapOr("(unnamed)") + "' definition set is not finished");
            }
            var frozen = frozenOpt.optUnwrap();
            return ArtifactSupport.implExportIndependentArtifact(xx, cell.artifactIdCell, () -> {
                var erb = new Erb(loc);
                var defSetArt =
                        erb.mustErMaybeUnreachable(ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set));
                var methodsOut = new TList<Artifact>();
                for (var m : frozen.methods) {
                    var mArt = erb.mustErMaybeUnreachable(
                            Exporters.exportType(xx, loc, new TypeInterfaceMethodDynamic(cell, m)));
                    if (mArt.optIsSome()) methodsOut.add(mArt.optUnwrap());
                }
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactDataDefSetInterface.User(
                        defSetArt.optUnwrap(),
                        set.defSetId,
                        cell.identity,
                        frozen.description,
                        frozen.isAspect,
                        frozen.aspectId,
                        cell.binaryName,
                        cell.compiledBytecode,
                        methodsOut);
                return EResult.ok(makeBody.apply(ArtifactDataDefSetInterfaceCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDataDefSetInterface body;
            try {
                body = ArtifactDataDefSetInterfaceCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeSourceInterfaceDynamic data: failed to parse ArtifactDataDefSetInterface: "
                                + e.getMessage());
            }
            return switch (body) {
                case ArtifactDataDefSetInterface.Builtin b -> {
                    if (id.optIsSome()) {
                        yield EResult.err(loc, "TypeSourceInterfaceDynamic: builtin interface carries an artifact id");
                    }
                    var builtinData = GenBuiltinInterfaces.byIdentity(b.identity());
                    if (builtinData.optIsNone()) {
                        yield EResult.err(loc, "TypeSourceInterfaceDynamic: unknown builtin interface " + b.identity());
                    }
                    yield EResult.ok(new TypeSourceInterfaceDynamic(InterfaceCell.ofBuiltin(builtinData.optUnwrap())));
                }
                case ArtifactDataDefSetInterface.User u -> {
                    if (id.optIsNone()) {
                        yield EResult.err(loc, "TypeSourceInterfaceDynamic: source interface requires an artifact id");
                    }
                    var idValue = id.optUnwrap();
                    yield ArtifactSupport.implImportIndependentArtifact(
                            xx, loc, idValue, data, (raw) -> new Result.Ok<>(u), (user) -> {
                                var building = new InterfaceBuilding();
                                building.description = user.description();
                                building.isAspect = user.isAspect();
                                building.aspectId = user.aspectId();
                                var cell = new InterfaceCell(
                                        Opt.none(),
                                        user.identity(),
                                        user.binaryName(),
                                        new InterfaceCell.State.Building(building));
                                cell.compiledBytecode = user.compiledBytecode();
                                cell.artifactIdCell.set(idValue);
                                var imported = new TypeSourceInterfaceDynamic(cell);
                                var deps = new TList<Artifact>();
                                deps.add(user.defSet());
                                deps.addAll(user.methods());
                                ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                                    var defSetAny =
                                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, user.defSet());
                                    var defSetVal = defSetAny.requireValue(
                                            "TypeSourceInterfaceDynamic::finalizer defSet artifact");
                                    cell.initSetReflectively(MustCast.mustCast(DefinitionSet.class, defSetVal));
                                    var methods = new TList<InterfaceMethodData>();
                                    for (var mArt : user.methods()) {
                                        var mAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, mArt);
                                        var mVal = mAny.requireValue(
                                                "TypeSourceInterfaceDynamic::finalizer method artifact");
                                        methods.add(MustCast.mustCast(TypeInterfaceMethodDynamic.class, mVal)
                                                .method());
                                    }
                                    cell.freeze(new InterfaceData(
                                            user.description(),
                                            user.isAspect(),
                                            user.aspectId(),
                                            IList.from(methods),
                                            Opt.none()));
                                    return imported;
                                };
                                return new Result.Ok<>(
                                        new ArtifactSupport.ImportIndependentResult(Opt.some(imported), deps, fin));
                            });
                }
            };
        }
    };

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, cell.identity).optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
                        .optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY)
                        .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, cell.identity).optIsSome()) {
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, this));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, destType));
        }
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_CELL_IDENTITY)
                .optIsSome()) {
            var sourceRes = DynUtil.valueReg(xx, loc, cv);
            if (sourceRes.hasErrors()) return sourceRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeVirtual(
                    GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_CELL,
                    b.reg(sourceRes.requireValue("sourceCode")));
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
        return EResult.err(loc, "Cannot cast interface_const to " + destType.typeDesc());
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, cell.identity).optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.SOURCE_INTERFACE_DYNAMIC);
    }
}
