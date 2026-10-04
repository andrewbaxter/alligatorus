package compilerxyz.main.langhelp;

import compilerxyz.main.eval.SourceId;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;

public interface Sources {
    Result<SourceResolved, String> sourcesGet(SourceSpecAbsolute spec);

    Result<SourceData, String> sourcesGetData(SourceSpecAbsolute spec);

    record SourceData(byte[] bytes, Opt<Compiles.LocalMeta> local) {}

    sealed interface SourceResolved {
        record Module(SourceId id, SourceModule module, Opt<Compiles.LocalMeta> local) implements SourceResolved {}

        record Bundle(SourceSpecAbsolute.Remote base) implements SourceResolved {}
    }
}
