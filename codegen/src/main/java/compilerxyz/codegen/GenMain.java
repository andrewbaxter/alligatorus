package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import compilerxyz.codegen.experimental.BuiltinIdentityScanner;
import compilerxyz.codegen.experimental.BuiltinPathScanner;
import compilerxyz.codegen.experimental.BuiltinPushStructScanner;
import compilerxyz.codegen.experimental.CodegenDescriptorScanner;
import compilerxyz.codegen.experimental.DynamicTypeScanner;
import compilerxyz.codegen.experimental.JavaAspectExtractor;
import compilerxyz.codegen.util.DynamicTypes;
import java.util.HashSet;

public final class GenMain {
  private GenMain() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      throw new IllegalArgumentException(
          "Expected three args: output source root, runtime source root, util source root");
    }
    var outRoot = Path.of(args[0]);
    var runtimeSourceRoot = Path.of(args[1]);
    var utilSourceRoot = Path.of(args[2]);
    if (Files.exists(outRoot)) {
      try (var walk = Files.walk(outRoot)) {
        walk.sorted(Comparator.reverseOrder())
            .forEach(
                p -> {
                  try {
                    Files.delete(p);
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                });
      }
    }
    Files.createDirectories(outRoot);

    var formatter = Formatter.createFormatter(
        JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

    var aspectIds = new TSet<Long>();
    var scopeIds = new TSet<Long>();

    var scannedLeaves =
        DynamicTypeScanner.scanRoot(runtimeSourceRoot);
    BuiltinInterfacesGen.setScannedLeafDescriptors(scannedLeaves);
    DynamicTypes.setScannedLeaves(scannedLeaves);
    DynamicTypesGen.run(outRoot, formatter, scannedLeaves);

    var scan = JavaAspectExtractor.scanRoot(runtimeSourceRoot);
    BuiltinInterfacesGen.run(outRoot, formatter, aspectIds, scan.builtins());
    var hostNames = new HashSet<String>();
    var links = new TList<String>();
    for (var s : scan.builtins()) {
      hostNames.add(s.pkg() + "." + s.simpleName());
      links.add("Gen" + s.simpleName());
    }
    BuiltinTreeGen.setBuiltinInterfaceHostNames(hostNames);
    BuiltinTreeGen.setLinkClassNames(links);
    var pathEntries =
        BuiltinPathScanner.scanRoot(runtimeSourceRoot);
    BuiltinTreeGen.setScannedPathEntries(pathEntries);
    var scannedIdentities =
        BuiltinIdentityScanner.scanRoot(runtimeSourceRoot);
    var identityExtras = new TList<IdentitiesGen.IdentitySpec>();
    for (var s : scannedIdentities) {
      identityExtras.add(new IdentitiesGen.IdentitySpec(s.name(), s.value(), s.standalone()));
    }
    identityExtras.addAll(ListsGen.identitySpecs());
    IdentitiesGen.run(outRoot, formatter, aspectIds, identityExtras);
    ListsGen.run(outRoot, formatter);

    var scannedPushStructs =
        BuiltinPushStructScanner.scanRoot(runtimeSourceRoot);
    PushStructsGen.run(
        outRoot,
        formatter,
        PushStructsGen.specs(),
        scannedPushStructs);
    BuiltinTreeGen.run(outRoot, formatter, scopeIds);
    CodecsGen.run(outRoot, formatter, runtimeSourceRoot);

    var codegenDescriptors =
        CodegenDescriptorScanner.scanRoot(runtimeSourceRoot);
    codegenDescriptors.addAll(CodegenDescriptorScanner.scanRoot(utilSourceRoot));
    CodegenDescriptorGen.run(outRoot, formatter, codegenDescriptors);
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
