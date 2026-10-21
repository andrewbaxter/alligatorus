package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6025L)
public final class EvalContext {

    final CompileImportContext ix;

    private final Counter dropStateCounter = new Counter();

    private final Counter emissionCounter = new Counter();

    final TList<LogEntry> logs = new TList<>();

    public EvalContext(CompileImportContext ix) {
        this.ix = ix;
    }

    public long nextDropState() {
        return dropStateCounter.next();
    }

    public long nextEmission() {
        return emissionCounter.next();
    }

    public void log(String message) {
        logs.add(new LogEntry(System.currentTimeMillis(), message));
    }

    public record ExternalEntry(BindingId key, Type type, External external) {}
}
