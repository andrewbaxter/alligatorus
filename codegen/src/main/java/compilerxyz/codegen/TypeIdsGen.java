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

final class TypeIdsGen {
  private TypeIdsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName TYPE_ID = ClassName.get("compilerxyz.eval", "TypeId");
  private static final ClassName TYPE_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "TypeId", "Builtin");

  private record TypeIdSpec(String name, long id) {}

  private static TList<TypeIdSpec> specs() {
    var list = new TList<TypeIdSpec>();
    list.add(new TypeIdSpec("type_number_const", 7001L));
    list.add(new TypeIdSpec("type_number_dynamic", 7002L));
    list.add(new TypeIdSpec("type_number_dynamic_box", 7003L));
    list.add(new TypeIdSpec("type_bool_const", 7004L));
    list.add(new TypeIdSpec("type_bool_dynamic", 7005L));
    list.add(new TypeIdSpec("type_bool_dynamic_box", 7006L));
    list.add(new TypeIdSpec("type_string_const", 7007L));
    list.add(new TypeIdSpec("type_string_dynamic", 7008L));
    list.add(new TypeIdSpec("type_null_const", 7009L));
    list.add(new TypeIdSpec("type_null_dynamic", 7010L));
    list.add(new TypeIdSpec("type_any_const", 7011L));
    list.add(new TypeIdSpec("type_type_dynamic", 7012L));
    list.add(new TypeIdSpec("type_aspect_dynamic", 7013L));
    list.add(new TypeIdSpec("type_constkey_dynamic", 7014L));
    list.add(new TypeIdSpec("type_unique", 7015L));
    list.add(new TypeIdSpec("type_expr", 7016L));
    list.add(new TypeIdSpec("type_expr_staged_placeholder", 7017L));
    list.add(new TypeIdSpec("type_throw_dynamic", 7018L));
    list.add(new TypeIdSpec("type_panic_dynamic", 7019L));
    list.add(new TypeIdSpec("type_code_value_extern_dynamic", 7020L));
    list.add(new TypeIdSpec("type_expr_context_dynamic", 7021L));
    list.add(new TypeIdSpec("type_qexpr_id_dynamic", 7022L));
    list.add(new TypeIdSpec("type_bind_result_dynamic", 7023L));
    list.add(new TypeIdSpec("type_any_target_dynamic", 7024L));
    list.add(new TypeIdSpec("type_any_binding_dynamic", 7025L));
    list.add(new TypeIdSpec("type_code_dyn_handle_dynamic", 7026L));
    list.add(new TypeIdSpec("type_list_dynamic", 7027L));
    list.add(new TypeIdSpec("type_list_new_dynamic", 7028L));
    list.add(new TypeIdSpec("type_list_constructor_const", 7029L));
    list.add(new TypeIdSpec("type_map_dynamic", 7030L));
    list.add(new TypeIdSpec("type_map_new_dynamic", 7031L));
    list.add(new TypeIdSpec("type_map_constructor_const", 7032L));
    list.add(new TypeIdSpec("type_map_entry_dynamic", 7033L));
    list.add(new TypeIdSpec("type_map_get_fn_dynamic", 7034L));
    list.add(new TypeIdSpec("type_option_dynamic", 7035L));
    list.add(new TypeIdSpec("type_option_constructor_const", 7036L));
    list.add(new TypeIdSpec("type_option_variant_constructor_dynamic", 7037L));
    list.add(new TypeIdSpec("type_function_dynamic", 7038L));
    list.add(new TypeIdSpec("type_function_aspect_dynamic", 7039L));
    list.add(new TypeIdSpec("type_function_cast_value_dynamic", 7040L));
    list.add(new TypeIdSpec("type_method_dynamic", 7041L));
    list.add(new TypeIdSpec("record", 7042L));
    list.add(new TypeIdSpec("tuple", 7043L));
    list.add(new TypeIdSpec("type_source_object_dynamic", 7044L));
    list.add(new TypeIdSpec("type_source_object_const", 7045L));
    list.add(new TypeIdSpec("type_source_object_ref_dynamic", 7046L));
    list.add(new TypeIdSpec("type_source_object_new_const", 7047L));
    list.add(new TypeIdSpec("type_source_object_new_dynamic", 7048L));
    list.add(new TypeIdSpec("type_source_interface_const", 7049L));
    list.add(new TypeIdSpec("type_source_interface_dynamic", 7050L));
    list.add(new TypeIdSpec("type_source_union_const", 7051L));
    list.add(new TypeIdSpec("type_source_union_dynamic", 7052L));
    list.add(new TypeIdSpec("type_source_union_ref_dynamic", 7053L));
    list.add(new TypeIdSpec("type_source_union_new_const", 7054L));
    list.add(new TypeIdSpec("type_source_union_new_dynamic", 7055L));
    list.add(new TypeIdSpec("type_source_function_const", 7056L));
    list.add(new TypeIdSpec("type_source_function_dynamic", 7057L));
    list.add(new TypeIdSpec("type_root_string_const", 7059L));
    list.add(new TypeIdSpec("type_root_null_const", 7060L));
    list.add(new TypeIdSpec("type_root_null_dynamic", 7061L));
    list.add(new TypeIdSpec("type_root_bool_const", 7062L));
    list.add(new TypeIdSpec("type_root_bool_dynamic", 7063L));
    list.add(new TypeIdSpec("type_root_number_const", 7064L));
    list.add(new TypeIdSpec("type_root_number_dynamic", 7065L));
    list.add(new TypeIdSpec("type_root_number_dynamic_box", 7066L));
    list.add(new TypeIdSpec("type_root_bool_dynamic_box", 7067L));
    list.add(new TypeIdSpec("type_root_string_dynamic", 7068L));
    list.add(new TypeIdSpec("type_root_constkey_dynamic", 7070L));
    list.add(new TypeIdSpec("type_root_unique", 7071L));
    list.add(new TypeIdSpec("type_root_expr", 7072L));
    list.add(new TypeIdSpec("type_root_expr_staged_placeholder", 7073L));
    list.add(new TypeIdSpec("type_root_function_dynamic", 7074L));
    list.add(new TypeIdSpec("type_root_function_aspect_dynamic", 7075L));
    list.add(new TypeIdSpec("type_root_function_cast_value_dynamic", 7076L));
    list.add(new TypeIdSpec("type_root_list_dynamic", 7077L));
    list.add(new TypeIdSpec("type_root_list_new_dynamic", 7078L));
    list.add(new TypeIdSpec("type_root_list_constructor_const", 7079L));
    list.add(new TypeIdSpec("type_root_map_dynamic", 7080L));
    list.add(new TypeIdSpec("type_root_map_new_dynamic", 7081L));
    list.add(new TypeIdSpec("type_root_map_get_fn_dynamic", 7082L));
    list.add(new TypeIdSpec("type_root_map_entry_dynamic", 7083L));
    list.add(new TypeIdSpec("type_root_map_constructor_const", 7084L));
    list.add(new TypeIdSpec("type_root_option_dynamic", 7085L));
    list.add(new TypeIdSpec("type_root_option_variant_constructor_dynamic", 7086L));
    list.add(new TypeIdSpec("type_root_option_constructor_const", 7087L));
    list.add(new TypeIdSpec("type_root_panic_dynamic", 7088L));
    list.add(new TypeIdSpec("type_root_throw_dynamic", 7089L));
    list.add(new TypeIdSpec("type_root_source_function_const", 7090L));
    list.add(new TypeIdSpec("type_root_source_interface_const", 7091L));
    list.add(new TypeIdSpec("type_root_source_object_const", 7092L));
    list.add(new TypeIdSpec("type_root_source_object_dynamic", 7093L));
    list.add(new TypeIdSpec("type_root_source_object_new_const", 7094L));
    list.add(new TypeIdSpec("type_root_source_union_const", 7095L));
    list.add(new TypeIdSpec("type_root_source_union_dynamic", 7096L));
    list.add(new TypeIdSpec("type_root_source_union_new_const", 7097L));
    list.add(new TypeIdSpec("type_root_code_value_extern_dynamic", 7098L));
    list.add(new TypeIdSpec("argument_tuple", 7100L));
    list.add(new TypeIdSpec("type_eresult_dynamic", 7101L));
    list.add(new TypeIdSpec("type_eresult_variant_constructor_dynamic", 7102L));
    list.add(new TypeIdSpec("type_code_dynamic", 7103L));
    list.add(new TypeIdSpec("type_source_dynamic", 7104L));
    list.add(new TypeIdSpec("type_root_eresult_dynamic", 7105L));
    list.add(new TypeIdSpec("type_root_eresult_variant_constructor_dynamic", 7106L));
    list.add(new TypeIdSpec("type_eresult_constructor_const", 7107L));
    list.add(new TypeIdSpec("type_root_eresult_constructor_const", 7108L));
    list.add(new TypeIdSpec("type_value_dynamic", 7109L));
    list.add(new TypeIdSpec("type_interface_spec", 7110L));
    list.add(new TypeIdSpec("type_root_interface_spec", 7111L));
    list.add(new TypeIdSpec("type_type_id_dynamic", 7112L));
    list.add(new TypeIdSpec("type_list_push_dynamic", 7113L));
    list.add(new TypeIdSpec("type_list_join_dynamic", 7114L));
    list.add(new TypeIdSpec("type_number_to_string_dynamic", 7115L));
    list.add(new TypeIdSpec("type_root_list_push_dynamic", 7116L));
    list.add(new TypeIdSpec("type_root_list_join_dynamic", 7117L));
    list.add(new TypeIdSpec("type_root_number_to_string_dynamic", 7118L));
    list.add(new TypeIdSpec("type_interface_method_dynamic", 7119L));
    list.add(new TypeIdSpec("type_java_data_descriptor_dynamic", 7120L));
    return list;
  }

  static void run(Path outRoot, Formatter formatter, TSet<Long> builtinTypeIds, TSet<Long> scopeIds)
      throws IOException {
    var specs = specs();
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
