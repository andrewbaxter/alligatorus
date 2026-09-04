package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class InterfaceBuilding {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public Opt<String> description = Opt.none();
    public boolean isAspect;
    public Opt<Unique> aspectId = Opt.none();
    public final TList<InterfaceMethodBuilding> methods = new TList<>();
}
