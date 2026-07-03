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
import java.util.Locale;
import javax.lang.model.element.Modifier;

public final class InvisibleBuiltinsGen {
  private InvisibleBuiltinsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName BINDING_ID = ClassName.get("compilerxyz.eval", "BindingId");
  private static final ClassName BINDING_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "BindingId", "Builtin");

  public record InvisibleBuiltinSpec(String name, String fnScopeId, long scopeId) {}

  static TList<InvisibleBuiltinSpec> specs() {
    return TList.of(
        new InvisibleBuiltinSpec("stage", "STAGE_FN_SCOPE_ID", 7017),
        new InvisibleBuiltinSpec("record_to_type", "RECORD_TO_TYPE_FN_SCOPE_ID", 7016),
        new InvisibleBuiltinSpec("tuple_to_type", "TUPLE_TO_TYPE_FN_SCOPE_ID", 7020),
        new InvisibleBuiltinSpec("cve_extract", "CVE_EXTRACT_FN_SCOPE_ID", 7003),
        new InvisibleBuiltinSpec("cve_get_code_enum", "CVE_GET_CODE_ENUM_FN_SCOPE_ID", 7004),
        new InvisibleBuiltinSpec("cve_get_value", "CVE_GET_VALUE_FN_SCOPE_ID", 7005),
        new InvisibleBuiltinSpec("value_get_type", "VALUE_GET_TYPE_FN_SCOPE_ID", 7022),
        new InvisibleBuiltinSpec("aspect_lookup", "ASPECT_LOOKUP_FN_SCOPE_ID", 7001),
        new InvisibleBuiltinSpec("cast_value", "CAST_VALUE_FN_SCOPE_ID", 7002),
        new InvisibleBuiltinSpec(
            "object_ref_to_type", "DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID", 7013),
        new InvisibleBuiltinSpec(
            "union_ref_to_type", "TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID", 7019),
        new InvisibleBuiltinSpec(
            "interface_ref_to_type", "INTERFACE_REF_TO_TYPE_FN_SCOPE_ID", 7015),
        new InvisibleBuiltinSpec(
            "function_const_to_type", "FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID", 7014),
        new InvisibleBuiltinSpec(
            "object_new_to_type", "DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", 7012),
        new InvisibleBuiltinSpec(
            "union_new_to_type", "TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", 7018),
        new InvisibleBuiltinSpec(
            "data_instance_to_type", "DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID", 7010),
        new InvisibleBuiltinSpec(
            "data_instance_to_target", "DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID", 7009),
        new InvisibleBuiltinSpec(
            "data_instance_to_binding", "DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID", 7008),
        new InvisibleBuiltinSpec(
            "data_instance_to_value", "DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID", 7011),
        new InvisibleBuiltinSpec(
            "data_instance_to_aspect", "DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID", 7006),
        new InvisibleBuiltinSpec(
            "data_instance_to_code", "DATA_INSTANCE_TO_CODE_FN_SCOPE_ID", 7007),
        new InvisibleBuiltinSpec("unique_eq", "UNIQUE_EQ_FN_SCOPE_ID", 7021));
  }

  public static void run(
      Path outRoot, Formatter formatter, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds)
      throws IOException {
    run(outRoot, formatter, specs(), scopeIds);
  }

  static void run(
      Path outRoot,
      Formatter formatter,
      TList<InvisibleBuiltinSpec> specs,
      TSet<Long> scopeIds)
      throws IOException {
    validate(specs, scopeIds);

      var cls =
              TypeSpec.classBuilder("GenInvisibleBuiltins")
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    cls.addMethod(
        MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    for (var spec : specs) {
      cls.addField(
              FieldSpec.builder(
                              BINDING_ID,
                              spec.fnScopeId(),
                              Modifier.PUBLIC,
                              Modifier.STATIC,
                              Modifier.FINAL)
                      .initializer("new $T($LL)", BINDING_ID_BUILTIN, spec.scopeId())
                      .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static void validate(TList<InvisibleBuiltinSpec> specs, TSet<Long> scopeIds) {
    var seenNames = new TSet<String>();
    var seenFnScopeIds = new TSet<String>();
    for (var spec : specs) {
      if (!seenNames.add(spec.name())) {
        throw new IllegalStateException("duplicate invisible-builtin name: " + spec.name());
      }
      if (!seenFnScopeIds.add(spec.fnScopeId())) {
        throw new IllegalStateException(
            "duplicate invisible-builtin fn_scope_id: " + spec.fnScopeId());
      }
      if (!spec.fnScopeId().equals(spec.fnScopeId().toUpperCase(Locale.ROOT))) {
        throw new IllegalStateException(
            "invisible-builtin fn_scope_id must be SCREAMING_SNAKE_CASE: " + spec.fnScopeId());
      }
      if (!scopeIds.add(spec.scopeId())) {
        throw new IllegalStateException(
            "duplicate scope_id "
                + spec.scopeId()
                + " (in invisible_builtins, for "
                + spec.name()
                + ")");
      }
    }
  }
}
