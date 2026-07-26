package compilerxyz.langhelp;

import compilerxyz.eval.SourceId;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;

public interface Sources {
  Result<SourceResolved, String> sourcesGet(SourceSpecAbsolute spec);

  sealed interface SourceResolved {
    record Module(SourceId id, SourceModule module, Opt<Compiles.LocalMeta> local)
        implements SourceResolved {}

    record Bundle(SourceSpecAbsolute.Remote base) implements SourceResolved {}
  }
}
