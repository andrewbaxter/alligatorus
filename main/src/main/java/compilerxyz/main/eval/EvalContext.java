package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

@BuiltinFull(uniqueId = 6025L)
public final class EvalContext {

    public final TMap<DefSetId, DefinitionSet> definitionSets = new TMap<>();
    public final TList<ExternalEntry> externals = new TList<>();
    public final CompileImportContext ix;
    public final Counter nextLocalId = new Counter();

    public final Counter uniqueCounter = new Counter();

    public final Counter dropStateCounter = new Counter();

    public final Counter emissionCounter = new Counter();

    public final TList<LogEntry> logs = new TList<>();

    public String evalRootInternalName;

    public EvalContext(CompileImportContext ix) {
        this.ix = ix;
    }

    public record ExternalEntry(JavaBytecodeBindingKey key, Type type, External external) {}
}
