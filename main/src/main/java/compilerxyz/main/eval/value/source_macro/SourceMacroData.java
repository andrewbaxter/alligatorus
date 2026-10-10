package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.prelude.util.Opt;

public final class SourceMacroData {
    public final ArtifactIdHolder artifactIdCell = new ArtifactIdHolder();
    public final Unique identity;
    public final SourceFunctionData function;
    public Opt<Type> exceptionType = Opt.none();
    public Opt<String> description = Opt.none();

    public SourceMacroData(Unique identity, SourceFunctionData function) {
        this.identity = identity;
        this.function = function;
    }
}
