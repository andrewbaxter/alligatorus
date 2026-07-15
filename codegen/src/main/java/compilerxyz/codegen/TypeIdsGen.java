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
    // type_any_const (7011L) migrated: on ObjectConstOfType
    // type_type_dynamic (7012L) migrated: on Type
    // type_aspect_dynamic (7013L) migrated: on Aspect
    // type_expr_context_dynamic (7021L) migrated: on ExprContext
    // type_qexpr_id_dynamic (7022L) migrated: on QExprId
    // type_bind_result_dynamic (7023L) migrated: on BindResult
    // type_any_target_dynamic (7024L) migrated: on Target
    // type_any_binding_dynamic (7025L) migrated: on Binding
    // type_code_dyn_handle_dynamic (7026L) migrated: on CodeDynHandleType
    // type_source_object_const (7045L) migrated: on TypeTypeSourceObjectConst
    // type_source_object_ref_dynamic (7046L) migrated: on SourceObjectRefType
    // type_source_object_new_const (7047L) migrated: on TypeTypeSourceObjectNewConst
    list.add(new TypeIdSpec("type_source_interface_dynamic", 7050L));
    // type_source_union_const (7051L) migrated: on TypeTypeSourceUnionConst
    // type_source_union_ref_dynamic (7053L) migrated: on SourceUnionRefType
    // type_source_union_new_const (7054L) migrated: on TypeTypeSourceUnionNewConst
    list.add(new TypeIdSpec("type_source_function_dynamic", 7057L));
    // type_eresult_dynamic (7101L) migrated: on TypeEResultDynamic
    // type_eresult_variant_constructor_dynamic (7102L) migrated: on TypeEResultVariantConstructorDynamic
    list.add(new TypeIdSpec("type_code_dynamic", 7103L));
    list.add(new TypeIdSpec("type_source_dynamic", 7104L));
    // type_eresult_constructor_const (7107L) migrated: on TypeEResultConstructorConst
    // type_value_dynamic (7109L) migrated: on Value
    // type_java_data_descriptor_dynamic (7120L) migrated: on JavaDataDescriptor
    // type_type_object_dynamic (7121L) migrated: on TypeTypeObjectDynamic
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
