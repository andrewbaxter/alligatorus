package compilerxyz.langhelp;

import compilerxyz.eval.SourceId;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;

public interface Sources {
  Result<SourceLookup, String> sourcesGet(SourceSpecAbsolute spec);

  record SourceLookup(SourceId id, SourceModule module) {}
}
