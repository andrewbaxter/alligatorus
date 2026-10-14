package compilerxyz.main.eval.value.value_source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
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
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.main.gen.GenAspectFunction;
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
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(loc, "a source object instance is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return importInstance(xx, loc, data);
        }
    };

    private static EResult<Object> importInstance(ExprContext xx, QExprId loc, JsonValue data) {
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
        var underlying = underAny instanceof Value v
                ? v
                : new ValueObjectConst(Exporters.constFormOf(xx, dt.underlyingType.optUnwrap()), underAny);
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
        var instanceCode = instanceCodeRes.requireValue("ValueSourceObjectConst::finalizer materialize");
        var instanceRes = EvalRoot.run(evalXx, loc, binaryName, instanceCode);
        if (instanceRes.hasErrors()) return instanceRes.reerr();
        return EResult.ok(instanceRes.requireValue("ValueSourceObjectConst::finalizer run"));
    }
}
