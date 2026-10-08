package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6025L)
public final class EvalContext {

    public final CompileImportContext ix;

    public final Counter dropStateCounter = new Counter();

    public final Counter emissionCounter = new Counter();

    public final TList<LogEntry> logs = new TList<>();

    public Opt<String> evalRootInternalName = Opt.none();

    public EvalContext(CompileImportContext ix) {
        this.ix = ix;
    }

    public record ExternalEntry(BindingId key, Type type, External external) {}
}
