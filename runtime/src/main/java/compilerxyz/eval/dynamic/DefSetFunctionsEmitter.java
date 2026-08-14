package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EvalContext;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public final class DefSetFunctionsEmitter {
  private DefSetFunctionsEmitter() {}

  public record ArgSlot(BindingId.Local id, JavaDataDescriptor jvmDesc) {}

  public record FunctionSpec(
      long funcIdx,
      TList<ArgSlot> argSlots,
      JavaDataDescriptor retDesc,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals) {}

  public record EmittedFunctions(
      String binaryName,
      String internalName,
      byte[] bytecode,
      TOrderedMap<String, Object> staticFieldValues) {}

  public static String functionsBinaryName(DefSetId defSet) {
    var compileId = defSet.compileId();
    return "compilerxyz.eval.gen.Functions$"
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
