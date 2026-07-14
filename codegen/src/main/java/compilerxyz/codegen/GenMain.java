package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;

public final class GenMain {
  private GenMain() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IllegalArgumentException(
          "Expected two args: output source root, runtime source root");
    }
    var outRoot = Path.of(args[0]);
    var runtimeSourceRoot = Path.of(args[1]);
    Files.createDirectories(outRoot);

    var formatter = Formatter.createFormatter(
        JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

    var builtinUniqueIds = new TSet<Long>();
    var builtinTypeIds = new TSet<Long>();
    var scopeIds = new TSet<Long>();

    var scannedLeaves =
        compilerxyz.codegen.experimental.DynamicTypeScanner.scanRoot(runtimeSourceRoot);
    AspectsGen.setScannedLeafDescriptors(scannedLeaves);

    var scan = compilerxyz.codegen.experimental.JavaAspectExtractor.scanRoot(runtimeSourceRoot);
    AspectsGen.run(outRoot, formatter, builtinUniqueIds, scopeIds, scan.aspects());
    BuiltinTreeGen.setScannedInterfaceSpecs(scan.sourceInterfaces());

    var interfaceSpecs = new TList<compilerxyz.codegen.util.InterfaceSpec>();
    interfaceSpecs.addAll(TraitsGen.specs());
    interfaceSpecs.addAll(BuiltinTreeGen.extraInterfaceSpecs());
    InterfacesGen.run(outRoot, formatter, interfaceSpecs, builtinUniqueIds, scopeIds);
    IdentitiesGen.run(outRoot, formatter, builtinUniqueIds, scopeIds);
    var scannedTypeIds =
        compilerxyz.codegen.experimental.BuiltinTypeIdScanner.scanRoot(runtimeSourceRoot);
    var typeIdExtras = new TList<TypeIdsGen.TypeIdSpec>();
    for (var s : scannedTypeIds) {
      typeIdExtras.add(new TypeIdsGen.TypeIdSpec(s.name(), s.id()));
    }
    TypeIdsGen.run(outRoot, formatter, builtinTypeIds, scopeIds, typeIdExtras);

    PushStructsGen.run(
        outRoot, formatter, PushStructsGen.specs(TraitsGen.specs()), builtinUniqueIds, scopeIds);
    BuiltinTreeGen.run(outRoot, formatter, builtinUniqueIds, scopeIds);
    CodecsGen.run(outRoot, formatter, runtimeSourceRoot);
  }

  static void write(Path outRoot, JavaFile file, Formatter formatter) throws IOException {
    var raw = file.toString();
    String formatted;
    try {
      formatted = formatter.formatSource(raw);
    } catch (FormatterException e) {
      throw new IOException("palantir-java-format failed for " + file.typeSpec.name, e);
    }
    var pkgDir = outRoot.resolve(file.packageName.replace('.', '/'));
    Files.createDirectories(pkgDir);
    var path = pkgDir.resolve(file.typeSpec.name + ".java");
    Files.writeString(
        path, formatted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}
