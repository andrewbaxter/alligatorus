package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefinitionSetClassLoader;
import compilerxyz.prelude.util.Opt;
import java.util.HashMap;
import java.util.Map;

public final class CompileImportContextDynamic {
    public Opt<DefinitionSetClassLoader> classLoader = Opt.none();

    public final Map<String, Class<?>> compositeCache = new HashMap<>();
}
