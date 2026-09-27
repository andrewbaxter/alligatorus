package compilerxyz.main.eval.value.value_source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.EvalRoot;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

public final class ValueSourceObjectConst {
    private ValueSourceObjectConst() {}

    public record ArtifactValueSourceObject(Artifact typeArtifact, Artifact underlying)
            implements SafeJsonSerializable {}

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof ValueObjectConst inst)) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected ValueObjectConst, got "
                                + self.getClass());
            }
            if (!(inst.type() instanceof TypeSourceObjectConst tsoc)) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected TypeSourceObjectConst type, got "
                                + inst.type().getClass());
            }
            var tsod = tsoc.dynamic();
            Object underlyingHost;
            try {
                var field = inst.data().getClass().getField(SourceObjectEmitter.DATA_INSTANCE_FIELD);
                underlyingHost = field.get(inst.data());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: source-object instance "
                                + inst.data().getClass().getName() + " missing "
                                + SourceObjectEmitter.DATA_INSTANCE_FIELD + " field");
            }
            if (tsod.underlyingType.optIsNone()) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: object underlying type not finalised");
            }
            var ut = tsod.underlyingType.optUnwrap();
            var underlyingLiftRes = GenAspectLift.get(xx, loc, ut);
            if (underlyingLiftRes.hasErrors()) return underlyingLiftRes.reerr();
            var underlyingLiftRes2 = underlyingLiftRes
                    .requireValue("ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift aspect")
                    .aspectLiftLift(xx, loc, underlyingHost);
            if (underlyingLiftRes2.hasErrors()) return underlyingLiftRes2.reerr();
            Value underlying = underlyingLiftRes2.requireValue(
                    "ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift");
            var errors = new TList<ExprErr>();
            var typeArtRes = Artifactable.exportArtifact(xx, loc, tsod);
            if (typeArtRes.hasErrors()) {
                typeArtRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var typeArt = typeArtRes.requireValue("ValueSourceObjectConst::exporterExport type artifact");
            var underArtRes = Artifactable.exportArtifact(xx, loc, underlying);
            if (underArtRes.hasErrors()) {
                underArtRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var underArt = underArtRes.requireValue("ValueSourceObjectConst::exporterExport underlying artifact");
            var body = new ArtifactValueSourceObject(typeArt, underArt);
            var artifact = new Artifact.Body(
                    makeBody.apply(ValueSourceObjectConstArtifactValueSourceObjectCodec.toValue(body)));
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactValueSourceObject body;
            try {
                body = ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfFromArtifactData: failed to parse "
                                + "ArtifactValueSourceObject: "
                                + e.getMessage());
            }
            var errors = new TList<ExprErr>();
            var typeRes = ArtifactSupport.importArtifact(xx, loc, body.typeArtifact());
            if (typeRes.hasErrors()) {
                typeRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var typeAny = typeRes.requireValue("ValueSourceObjectConst::exporterFromArtifactData type any");
            var dt = MustCast.mustCast(TypeSourceObjectDynamic.class, typeAny);
            var underRes = ArtifactSupport.importArtifact(xx, loc, body.underlying());
            if (underRes.hasErrors()) {
                underRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var underAny = underRes.requireValue("ValueSourceObjectConst::exporterFromArtifactData underlying any");
            var underVal = ArtifactSupport.importedAsValue(underAny);
            var underlying = underVal;
            var evalVx = new EvalContext(xx.vx.ix);
            var evalTarget = new TargetDynamic();
            var evalXx = new ExprContext(new Scope(evalVx), evalTarget, evalVx, xx.importState, Opt.none());
            var binaryName = EvalRoot.newBinaryName(evalXx);
            var newType = new TypeSourceObjectNewDynamic(dt);
            var newFunctionRes = GenAspectFunction.get(evalXx, loc, newType);
            if (newFunctionRes.hasErrors()) return newFunctionRes.reerr();
            var instanceCvRes = newFunctionRes
                    .requireValue("ValueSourceObjectConst::finalizer new function aspect")
                    .aspectFunctionCall(
                            evalXx,
                            loc,
                            new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ValueEmpty(newType))),
                            new CodeValue(Code.none(loc), EvalOutcome.ofValue(underlying)));
            if (instanceCvRes.hasErrors()) return instanceCvRes.reerr();
            var instanceCodeRes = DynUtil.materialize(
                    evalXx, loc, instanceCvRes.requireValue("ValueSourceObjectConst::finalizer new call"));
            if (instanceCodeRes.hasErrors()) return instanceCodeRes.reerr();
            var installRes = EvalRoot.installDefinitionSets(evalXx, loc);
            if (installRes.hasErrors()) return installRes.reerr();
            var instanceRes = EvalRoot.run(
                    evalXx,
                    loc,
                    binaryName,
                    instanceCodeRes.requireValue("ValueSourceObjectConst::finalizer materialize"),
                    installRes.requireValue("ValueSourceObjectConst::finalizer installDefinitionSets"));
            if (instanceRes.hasErrors()) return instanceRes.reerr();
            var instance = instanceRes.requireValue("ValueSourceObjectConst::finalizer run");
            Artifactable out = new ValueObjectConst(new TypeSourceObjectConst(dt), instance);
            if (errors.isEmpty()) return EResult.ok(out);
            return EResult.mixed(out, errors);
        }
    };
}
