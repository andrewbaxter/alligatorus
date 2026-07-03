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

final class IdentitiesGen {
  private IdentitiesGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");

  private record IdentitySpec(String name, long builtinUniqueId) {}

  private static TList<IdentitySpec> specs() {
    return TList.of(
        new IdentitySpec("expr_context_ref_type_identity", 6001),
        new IdentitySpec("code_value_extern_type_identity", 6002),
        new IdentitySpec("type_unique_identity", 6003),
        new IdentitySpec("interface_const_identity", 6004),
        new IdentitySpec("interface_ref_type_identity", 6005),
        new IdentitySpec("type_type_identity", 6006),
        new IdentitySpec("union_ref_type_identity", 6007),
        new IdentitySpec("type_option_identity", 6008),
        new IdentitySpec("type_code_struct_identity", 6009),
        new IdentitySpec("type_code_enum_identity", 6010),
        new IdentitySpec("type_code_dyn_handle_identity", 6011),
        new IdentitySpec("type_aspect_identity", 6012),
        new IdentitySpec("expr_type_identity", 6013));
  }

  static void run(
      Path outRoot, Formatter formatter, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds)
      throws IOException {
    var specs = specs();
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
              TypeSpec.classBuilder("GenIdentities")
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    cls.addMethod(
        com.squareup.javapoet.MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build());

    for (var spec : specs) {
      cls.addField(
              FieldSpec.builder(
                              UNIQUE,
                              upper(spec.name),
                              Modifier.PUBLIC,
                              Modifier.STATIC,
                              Modifier.FINAL)
                      .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId)
                      .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static String upper(String snake) {
    return snake.toUpperCase(java.util.Locale.ROOT);
  }
}
