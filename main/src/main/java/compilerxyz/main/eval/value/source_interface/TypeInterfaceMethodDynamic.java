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
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectConsumeDynamicObject;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenBuiltinInterfaces;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataDefSetInterfaceMethodCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6136L)
public record TypeInterfaceMethodDynamic(InterfaceCell iface, InterfaceMethodData method) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "interface method " + method.name;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicInterfaceMethodDynamic(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionInterfaceMethodDynamic(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(AspectConsumeDynamicObject.INSTANCE);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_INTERFACE_METHOD_DYNAMIC_IDENTITY,
                TypeInterfaceMethodDynamic.class);
        return oOpt.optIsSome()
                && iface.identity.equals(oOpt.optUnwrap().iface.identity)
                && method.name.equals(oOpt.optUnwrap().method.name);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.SOURCE_INTERFACE_METHOD);
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var ref = MustCast.mustCast(TypeInterfaceMethodDynamic.class, self);
            var iface = ref.iface;
            var method = ref.method;
            if (iface.set.optIsNone()) {
                var body = new ArtifactDataDefSetInterfaceMethod.Builtin(iface.identity, method.name);
                return EResult.ok(
                        new Artifact.Body(makeBody.apply(ArtifactDataDefSetInterfaceMethodCodec.toValue(body))));
            }
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, method.artifactIdCell, () -> {
                        var ifaceArtRes = Artifactable.exportArtifact(xx, loc, new TypeSourceInterfaceDynamic(iface));
                        ifaceArtRes.drainInto(errors);
                        var ifaceArt = ifaceArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        var argArtRes = Artifactable.exportArtifact(xx, loc, method.argType);
                        argArtRes.drainInto(errors);
                        var argArt = argArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        Artifact retArt;
                        if (method.retType.optIsSome()) {
                            var retArtRes = Artifactable.exportArtifact(xx, loc, method.retType.optUnwrap());
                            retArtRes.drainInto(errors);
                            retArt = retArtRes.valueOpt().optUnwrapOr(nullArtifact());
                        } else {
                            retArt = nullArtifact();
                        }
                        var dataFieldNames = new TList<String>();
                        for (var n : method.dataFieldNames) {
                            dataFieldNames.add(n);
                        }
                        var body = new ArtifactDataDefSetInterfaceMethod.User(
                                ifaceArt, method.name, argArt, retArt, dataFieldNames);
                        return makeBody.apply(ArtifactDataDefSetInterfaceMethodCodec.toValue(body));
                    });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDataDefSetInterfaceMethod body;
            try {
                body = ArtifactDataDefSetInterfaceMethodCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeInterfaceMethodDynamic data: failed to parse ArtifactDataDefSetInterfaceMethod: "
                                + e.getMessage());
            }
            return switch (body) {
                case ArtifactDataDefSetInterfaceMethod.Builtin b -> {
                    if (id.optIsSome()) {
                        yield EResult.err(
                                loc, "TypeInterfaceMethodDynamic: builtin interface method carries an artifact id");
                    }
                    var builtinData = GenBuiltinInterfaces.byIdentity(b.iface());
                    if (builtinData.optIsNone()) {
                        yield EResult.err(loc, "TypeInterfaceMethodDynamic: unknown builtin interface " + b.iface());
                    }
                    var cell = InterfaceCell.ofBuiltin(builtinData.optUnwrap());
                    for (var m : cell.frozen().methods) {
                        if (m.name.equals(b.name())) {
                            yield EResult.ok(new TypeInterfaceMethodDynamic(cell, m));
                        }
                    }
                    throw new IllegalStateException("TypeInterfaceMethodDynamic: builtin interface " + b.iface()
                            + " has no method '" + b.name() + "'");
                }
                case ArtifactDataDefSetInterfaceMethod.User u -> {
                    if (id.optIsNone()) {
                        yield EResult.err(
                                loc, "TypeInterfaceMethodDynamic: source interface method requires an artifact id");
                    }
                    var idValue = id.optUnwrap();
                    yield ArtifactSupport.implImportIndependentArtifact(
                            xx, loc, idValue, data, (raw) -> new Result.Ok<>(u), (user) -> {
                                var deps = new TList<Artifact>();
                                deps.add(user.iface());
                                deps.add(user.argType());
                                deps.add(user.retType());
                                ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                                    var ifaceAny =
                                            ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, user.iface());
                                    var ifaceVal = ifaceAny.requireValue("TypeInterfaceMethodDynamic::finalizer iface");
                                    var cell = MustCast.mustCast(TypeSourceInterfaceDynamic.class, ifaceVal)
                                            .cell();
                                    var argRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                            xx2, loc2, user.argType());
                                    var argVal = argRes.requireValue("TypeInterfaceMethodDynamic::finalizer arg");
                                    var retRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                            xx2, loc2, user.retType());
                                    var retVal = retRes.requireValue("TypeInterfaceMethodDynamic::finalizer ret");
                                    var method = new InterfaceMethodData(
                                            user.name(),
                                            argVal,
                                            Opt.some(retVal),
                                            IList.from(user.dataFieldNames()),
                                            Opt.none(),
                                            Opt.none());
                                    method.artifactIdCell.set(idValue);
                                    return new TypeInterfaceMethodDynamic(cell, method);
                                };
                                return new Result.Ok<>(
                                        new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                            });
                }
            };
        }
    };
}
