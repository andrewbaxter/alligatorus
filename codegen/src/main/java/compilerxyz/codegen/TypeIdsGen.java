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
    long id = 7001L;
    String[] names = {
      "type_number_const",
      "type_number_dynamic",
      "type_number_dynamic_box",
      "type_bool_const",
      "type_bool_dynamic",
      "type_bool_dynamic_box",
      "type_string_const",
      "type_string_dynamic",
      "type_null_const",
      "type_null_dynamic",
      "type_any_const",
      "type_any_dynamic",
      "type_aspect_dynamic",
      "type_constkey_dynamic",
      "type_unique",
      "type_expr",
      "type_expr_staged_placeholder",
      "type_throw_dynamic",
      "type_panic_dynamic",
      "type_code_value_extern_dynamic",
      "type_expr_context_dynamic",
      "type_qexpr_id_dynamic",
      "type_bind_result_dynamic",
      "type_any_target_dynamic",
      "type_any_binding_dynamic",
      "type_code_enum_dynamic",
      "type_code_dyn_handle_dynamic",
      "type_list_dynamic",
      "type_list_new_dynamic",
      "type_list_constructor_const",
      "type_map_dynamic",
      "type_map_new_dynamic",
      "type_map_constructor_const",
      "type_map_entry_dynamic",
      "type_map_get_fn_dynamic",
      "type_option_dynamic",
      "type_option_constructor_const",
      "type_option_variant_constructor_dynamic",
      "type_function_dynamic",
      "type_function_aspect_dynamic",
      "type_function_cast_value_dynamic",
      "type_method_dynamic",
      "record",
      "tuple",
      "type_source_object_dynamic",
      "type_source_object_const",
      "type_source_object_ref_dynamic",
      "type_source_object_new_const",
      "type_source_object_new_dynamic",
      "type_source_interface_const",
      "type_source_interface_dynamic",
      "type_source_union_const",
      "type_source_union_dynamic",
      "type_source_union_ref_dynamic",
      "type_source_union_new_const",
      "type_source_union_new_dynamic",
      "type_source_function_const",
      "type_source_function_dynamic",
      "type_source_exports_lifted_const",
      "type_root_string_const",
      "type_root_null_const",
      "type_root_null_dynamic",
      "type_root_bool_const",
      "type_root_bool_dynamic",
      "type_root_number_const",
      "type_root_number_dynamic",
      "type_root_number_dynamic_box",
      "type_root_bool_dynamic_box",
      "type_root_string_dynamic",
      "type_root_any_const",
      "type_root_any_dynamic",
      "type_root_constkey_dynamic",
      "type_root_unique",
      "type_root_expr",
      "type_root_expr_staged_placeholder",
      "type_root_function_dynamic",
      "type_root_function_aspect_dynamic",
      "type_root_function_cast_value_dynamic",
      "type_root_list_dynamic",
      "type_root_list_new_dynamic",
      "type_root_list_constructor_const",
      "type_root_map_dynamic",
      "type_root_map_new_dynamic",
      "type_root_map_get_fn_dynamic",
      "type_root_map_entry_dynamic",
      "type_root_map_constructor_const",
      "type_root_option_dynamic",
      "type_root_option_variant_constructor_dynamic",
      "type_root_option_constructor_const",
      "type_root_panic_dynamic",
      "type_root_throw_dynamic",
      "type_root_code_enum_dynamic",
      "type_root_source_function_const",
      "type_root_source_interface_const",
      "type_root_source_object_const",
      "type_root_source_object_dynamic",
      "type_root_source_object_new_const",
      "type_root_source_union_const",
      "type_root_source_union_dynamic",
      "type_root_source_union_new_const",
      "type_root_code_value_extern_dynamic",
      "type_root_source_exports_lifted_const",
      "argument_tuple",
      "type_eresult_dynamic",
      "type_eresult_variant_constructor_dynamic",
      "type_code_dynamic",
      "type_source_dynamic",
      "type_source_variant_constructor_dynamic",
      "type_root_eresult_dynamic",
      "type_root_eresult_variant_constructor_dynamic",
      "type_root_source_variant_constructor_dynamic",
      "type_eresult_constructor_const",
      "type_root_eresult_constructor_const",
      "type_value_dynamic",
      "type_interface_spec",
      "type_root_interface_spec"
    };
    for (var n : names) {
      list.add(new TypeIdSpec(n, id++));
    }
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
