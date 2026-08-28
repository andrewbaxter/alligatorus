package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefSetId;

public final class SourceInterfaceEmitter {
    private SourceInterfaceEmitter() {}

    public static String binaryNameFor(DefSetId defSet, long interfaceIndex) {
        var compileId = defSet.compileId();
        return "compilerxyz.eval.gen.Interface$"
                + compileId.importState().value()
                + "_"
                + compileId.source().value()
                + "_"
                + defSet.index()
                + "_"
                + interfaceIndex;
    }
}
