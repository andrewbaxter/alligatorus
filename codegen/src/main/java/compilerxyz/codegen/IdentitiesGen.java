package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class IdentitiesGen {
  private IdentitiesGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");

  public record IdentitySpec(String name, long builtinUniqueId) {}

  private static TList<IdentitySpec> specs() {
    // All 14 identities migrated to @BuiltinTrivialIdentityAspect annotations or deleted as dead.
    // Dead: type_code_struct_identity (6009), type_code_enum_identity (6010) — zero refs.
    return TList.of();
  }

  public static void run(
      Path outRoot,
      Formatter formatter,
      TSet<Long> builtinUniqueIds,
      TSet<Long> scopeIds,
      TList<IdentitySpec> extras)
      throws IOException {
    var specs = new TList<IdentitySpec>();
    specs.addAll(specs());
    specs.addAll(extras);
    var seenNames = new TSet<String>();
    for (var spec : specs) {
      if (!seenNames.add(spec.name)) {
        throw new IllegalStateException("duplicate identity name: " + spec.name);
      }
      if (!builtinUniqueIds.add(spec.builtinUniqueId)) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId + " (in identities)");
      }
    }

      var cls =
              TypeSpec.classBuilder("GenTrivialIdentityAspects")
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    cls.addMethod(
        com.squareup.javapoet.MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build());

    for (var spec : specs) {
      cls.addField(
              FieldSpec.builder(
                              UNIQUE,
                              constantName(spec.name),
                              Modifier.PUBLIC,
                              Modifier.STATIC,
                              Modifier.FINAL)
                      .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId)
                      .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static String constantName(String snake) {
    var s = snake;
    if (s.endsWith("_identity")) s = s.substring(0, s.length() - "_identity".length());
    return s.toUpperCase(java.util.Locale.ROOT) + "_IDENTITY";
  }
}
