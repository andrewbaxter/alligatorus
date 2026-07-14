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
    var list = new TList<TypeIdSpec>();
    // Entries below have no matching runtime Type class (handwritten helpers with mismatched
    // names, or types not represented by a single Java class). All other TypeIds have been
    // migrated to @BuiltinTypeId annotations on their runtime class and are scanned by
    // BuiltinTypeIdScanner.
    list.add(new TypeIdSpec("type_any_const", 7011L));
    list.add(new TypeIdSpec("type_type_dynamic", 7012L));
    list.add(new TypeIdSpec("type_aspect_dynamic", 7013L));
    list.add(new TypeIdSpec("type_expr_context_dynamic", 7021L));
    list.add(new TypeIdSpec("type_qexpr_id_dynamic", 7022L));
    list.add(new TypeIdSpec("type_bind_result_dynamic", 7023L));
    list.add(new TypeIdSpec("type_any_target_dynamic", 7024L));
    list.add(new TypeIdSpec("type_any_binding_dynamic", 7025L));
    list.add(new TypeIdSpec("type_code_dyn_handle_dynamic", 7026L));
    list.add(new TypeIdSpec("type_source_object_const", 7045L));
    list.add(new TypeIdSpec("type_source_object_ref_dynamic", 7046L));
    list.add(new TypeIdSpec("type_source_object_new_const", 7047L));
    list.add(new TypeIdSpec("type_source_interface_dynamic", 7050L));
    list.add(new TypeIdSpec("type_source_union_const", 7051L));
    list.add(new TypeIdSpec("type_source_union_ref_dynamic", 7053L));
    list.add(new TypeIdSpec("type_source_union_new_const", 7054L));
    list.add(new TypeIdSpec("type_source_function_dynamic", 7057L));
    list.add(new TypeIdSpec("type_eresult_dynamic", 7101L));
    list.add(new TypeIdSpec("type_eresult_variant_constructor_dynamic", 7102L));
    list.add(new TypeIdSpec("type_code_dynamic", 7103L));
    list.add(new TypeIdSpec("type_source_dynamic", 7104L));
    list.add(new TypeIdSpec("type_eresult_constructor_const", 7107L));
    list.add(new TypeIdSpec("type_value_dynamic", 7109L));
    list.add(new TypeIdSpec("type_java_data_descriptor_dynamic", 7120L));
    list.add(new TypeIdSpec("type_type_object_dynamic", 7121L));
    list.add(new TypeIdSpec("type_type_source_interface_const", 7122L));
    return list;
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
