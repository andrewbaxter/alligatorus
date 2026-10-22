package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefinitionSetClassLoader;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.HashMap;
import java.util.Map;

public final class CompileImportContextDynamic implements CompileImportContextTarget {
    private final GlobalContext gx;

    private Opt<DefinitionSetClassLoader> classLoader = Opt.none();

    public final Map<String, Class<?>> compositeCache = new HashMap<>();

    public CompileImportContextDynamic(GlobalContext gx) {
        this.gx = gx;
    }

    public static CompileImportContextDynamic of(CompileImportContextTarget target) {
        return switch (target) {
            case CompileImportContextDynamic d -> d;
        };
    }

    public Opt<DefinitionSetClassLoader> classLoaderOpt() {
        return classLoader;
    }

    public DefinitionSetClassLoader classLoaderOrCreate(TList<DefinitionSetClassLoader> dependencies) {
        if (classLoader.optIsNone()) {
            classLoader = Opt.some(
                    new DefinitionSetClassLoader(gx, DefinitionSetClassLoader.class.getClassLoader(), dependencies));
        }
        return classLoader.optUnwrap();
    }
}
