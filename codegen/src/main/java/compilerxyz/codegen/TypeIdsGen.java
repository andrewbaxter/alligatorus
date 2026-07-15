package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class TypeIdsGen {
  private TypeIdsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName TYPE_ID = ClassName.get("compilerxyz.eval", "TypeId");
  private static final ClassName TYPE_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "TypeId", "Builtin");

  public record TypeIdSpec(String name, long id) {}

  private static TList<TypeIdSpec> specs() {
    // All TypeIds now scanned from @BuiltinTypeId annotations on their owning classes.
    return TList.of();
  }

  public static void run(
      Path outRoot,
      Formatter formatter,
      TSet<Long> builtinTypeIds,
      TSet<Long> scopeIds,
      TList<TypeIdSpec> extras)
      throws IOException {
    var specs = new TList<TypeIdSpec>();
    specs.addAll(specs());
    specs.addAll(extras);
    var seenNames = new TSet<String>();
    for (var spec : specs) {
      if (!seenNames.add(spec.name)) {
        throw new IllegalStateException("duplicate type-id name: " + spec.name);
      }
      if (!builtinTypeIds.add(spec.id)) {
        throw new IllegalStateException(
            "duplicate builtin type id " + spec.id + " (in TypeIdsGen)");
      }
    }

    var cls = TypeSpec.classBuilder("GenTypeIds").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    for (var spec : specs) {
      cls.addField(
          FieldSpec.builder(
                  TYPE_ID,
                  upper(spec.name) + "_TYPE_ID",
                  Modifier.PUBLIC,
                  Modifier.STATIC,
                  Modifier.FINAL)
              .initializer("new $T($LL)", TYPE_ID_BUILTIN, spec.id)
              .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static String upper(String snake) {
    return snake.toUpperCase(java.util.Locale.ROOT);
  }
}
