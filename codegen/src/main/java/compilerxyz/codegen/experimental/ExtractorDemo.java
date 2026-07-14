package compilerxyz.codegen.experimental;

import java.nio.file.Path;

public final class ExtractorDemo {
  public static void main(String[] args) throws Exception {
    var file = Path.of(args.length > 0 ? args[0]
        : "runtime/src/main/java/compilerxyz/handwritten/AspectStackValueDynamic.java");
    var spec = JavaAspectExtractor.extract(file);
    System.out.println("Extracted InterfaceSpec:");
    System.out.println("  name              = " + spec.name());
    System.out.println("  uniqueId          = " + spec.builtinUniqueId());
    System.out.println("  scopeId           = " + spec.scopeId());
    System.out.println("  fieldPrefix       = " + spec.fieldPrefix());
    System.out.println("  kind              = " + spec.kind());
    System.out.println("  hostInterfaceFqn  = " + spec.effectiveHostInterfaceBinaryName());
    System.out.println("  methods           = " + spec.methods().size());
    for (var m : spec.methods()) {
      System.out.println("    - " + m.baseName() + " (source=" + m.sourceName() + ")");
      System.out.println("        ret  = " + m.retType().path());
      for (var a : m.args().fields()) {
        System.out.println("        arg  = " + a.name() + " : " + a.type().path());
      }
    }
  }
}
