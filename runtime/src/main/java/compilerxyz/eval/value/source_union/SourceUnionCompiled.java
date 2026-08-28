package compilerxyz.eval.value.source_union;

import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class SourceUnionCompiled {
    public Opt<String> baseBinaryName = Opt.none();
    public Opt<TList<String>> variantBinaryNames = Opt.none();
    public Opt<byte[]> baseBytecode = Opt.none();
    public Opt<TList<byte[]>> variantBytecode = Opt.none();
}
