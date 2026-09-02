package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class DefSetFunctionsEmitter {
    private DefSetFunctionsEmitter() {}

    public record ArgSlot(BindingId id, JavaDataDescriptor jvmDesc) {}

    public record FunctionSpec(
            long funcIdx,
            TList<ArgSlot> argSlots,
            JavaDataDescriptor retDesc,
            CodeDynamic bodyCode,
            TList<EvalContext.ExternalEntry> externals) {}

    public record EmittedFunctions(
            String binaryName, String internalName, byte[] bytecode, TOrderedMap<String, Object> staticFieldValues) {}

    public static String functionsBinaryName(DefSetId defSet) {
        var compileId = defSet.compileId();
        return "compilerxyz.main.eval.gen.Functions$"
                + compileId.importState().value()
                + "_"
                + compileId.source().value()
                + "_"
                + defSet.index();
    }

    public static String functionsInternalName(DefSetId defSet) {
        return functionsBinaryName(defSet).replace('.', '/');
    }

    public static String functionMethodName(long funcIdx) {
        return "_" + funcIdx;
    }

    public static String functionFieldPrefix(long funcIdx) {
        return "_" + funcIdx + "_";
    }
}
