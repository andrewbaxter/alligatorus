package compilerxyz.main.eval.value.source_union;

import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class SourceUnionCompiled {
    public Opt<byte[]> baseBytecode = Opt.none();
    public Opt<TList<byte[]>> variantBytecode = Opt.none();
}
