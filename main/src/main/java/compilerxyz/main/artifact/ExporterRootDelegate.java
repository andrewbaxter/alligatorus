package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public interface ExporterRootDelegate {
    EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx, QExprId loc, Opt<Type> ofType, Object self, Function<JsonValue, ArtifactBody> makeBody);

    EResult<Imported> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data);
}
