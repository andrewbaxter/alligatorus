package compilerxyz.main.eval.value.object_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;

public record ValueObjectConst(Type type, Object data) implements Value, ExporterAny {
    @Override
    public Type valueType() {
        return type;
    }

    public static Type plainTypeType() {
        var td = (TypeObjectDynamic) GenType.dynamicType();
        return new TypeObjectConst(td.meta(), td.hostClass(), Opt.<ExporterRoot>none());
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        throw new UnsupportedOperationException(
                "ValueObjectConst::exporterAnyAsType: a wrapped compile-time value is not itself a type");
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Value.exportArtifact(xx, loc, this);
    }
}
