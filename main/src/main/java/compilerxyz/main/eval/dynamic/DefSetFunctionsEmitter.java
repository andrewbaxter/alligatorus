package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class DefSetFunctionsEmitter {
    private DefSetFunctionsEmitter() {}

    public record ArgSlot(BindingId id, JavaDataDescriptor jvmDesc) {}

    public record FunctionSpec(
            String methodName,
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

    public static String functionFieldPrefix(String methodName) {
        return methodName + "_";
    }

    public static TOrderedMap<String, Object> rebuildStaticFieldValues(SourceFunctionData func) {
        var staticFieldValues = new TOrderedMap<String, Object>();
        var prefix = functionFieldPrefix(func.jvmMethodName);
        for (int i = 0; i < func.capturedExternals.size(); i++) {
            var entry = func.capturedExternals.get(i);
            staticFieldValues.put(
                    prefix + Prefixes.EXTERNAL_PREFIX + i, entry.external().externalHostValue());
        }
        return staticFieldValues;
    }
}
