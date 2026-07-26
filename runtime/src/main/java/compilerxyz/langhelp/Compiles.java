package compilerxyz.langhelp;

import compilerxyz.eval.CompileId;
import compilerxyz.eval.CompileRes;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Supplier;

public interface Compiles {
  void compilesPrepare(CompileId root);

  CompileRes compilesGetOrCompute(CompileId id, Supplier<CompileProduct> compute);

  TList<CompileRes> compilesResults();

  record CompileProduct(CompileRes res, Opt<LocalMeta> local, TList<CompileId> localDeps) {}

  record LocalMeta(String path, long timestampMs) {}
}
