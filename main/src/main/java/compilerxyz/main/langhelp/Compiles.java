package compilerxyz.main.langhelp;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileRes;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Supplier;

public interface Compiles {
    void compilesPrepare(CompileId root);

    CompileRes compilesGetOrCompute(CompileId id, SourceSpecAbsolute spec, Supplier<CompileProduct> compute);

    TList<CompileRes> compilesResults();

    record CompileProduct(
            CompileRes res, Opt<LocalMeta> local, TList<CompileId> localDeps, TList<LocalMeta> localData) {}

    record LocalMeta(String path, long timestampMs) {}
}
