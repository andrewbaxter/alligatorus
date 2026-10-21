package compilerxyz.main.eval.value;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 1067L)
@BuiltinPath("types.value_dynamic")
public record ValueDynamic(ValueDynamicSource source, Type type) implements Value {
    public Type valueType() {
        return type;
    }

    @Override
    public Opt<ArtifactExporter> valueArtifactExporter(ExprContext xx) {
        return Opt.none();
    }
}
