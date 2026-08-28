package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefSetId;

public final class SourceInterfaceEmitter {
    private SourceInterfaceEmitter() {}

    public static String binaryNameFor(DefSetId defSet, long interfaceIndex) {
        var compileId = defSet.compileId();
        return "compilerxyz.main.eval.gen.Interface$"
                + compileId.importState().value()
                + "_"
                + compileId.source().value()
                + "_"
                + defSet.index()
                + "_"
                + interfaceIndex;
    }
}
