package compilerxyz.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import compilerxyz.codegen.InvisibleBuiltinsGen.InvisibleBuiltinSpec;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InvisibleBuiltinsGenTest {
  private static final Formatter FORMATTER =
      Formatter.createFormatter(
          JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

  @Test
  void generates_all_invisible_builtins(@TempDir Path tmp) throws IOException {
    InvisibleBuiltinsGen.run(tmp, FORMATTER, new TSet<Long>(), new TSet<Long>());
    var path = tmp.resolve("compilerxyz/gen/GenInvisibleBuiltins.java");
    assertTrue(Files.exists(path), "GenInvisibleBuiltins.java not emitted");
    var src = Files.readString(path);

    String[][] entries = {
      {"STAGE_FN_SCOPE_ID", "7017"},
      {"RECORD_TO_TYPE_FN_SCOPE_ID", "7016"},
      {"TUPLE_TO_TYPE_FN_SCOPE_ID", "7020"},
      {"CVE_EXTRACT_FN_SCOPE_ID", "7003"},
      {"CVE_GET_CODE_ENUM_FN_SCOPE_ID", "7004"},
      {"CVE_GET_VALUE_FN_SCOPE_ID", "7005"},
      {"VALUE_GET_TYPE_FN_SCOPE_ID", "7022"},
      {"ASPECT_LOOKUP_FN_SCOPE_ID", "7001"},
      {"CAST_VALUE_FN_SCOPE_ID", "7002"},
      {"DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID", "7013"},
      {"TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID", "7019"},
      {"INTERFACE_REF_TO_TYPE_FN_SCOPE_ID", "7015"},
      {"FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID", "7014"},
      {"DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", "7012"},
      {"TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", "7018"},
      {"DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID", "7010"},
      {"DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID", "7009"},
      {"DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID", "7008"},
      {"DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID", "7011"},
      {"DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID", "7006"},
      {"DATA_INSTANCE_TO_CODE_FN_SCOPE_ID", "7007"},
      {"UNIQUE_EQ_FN_SCOPE_ID", "7021"},
    };
    assertEquals(22, entries.length, "spec count mismatch with invisible_builtins.rs");
    for (var e : entries) {
      var needle =
          "public static final BindingId " + e[0] + " = new BindingId.Builtin(" + e[1] + "L)";
      assertTrue(src.contains(needle), "missing: " + needle + " in:\n" + src);
    }
  }

  @Test
  void rejects_duplicate_scope_id(@TempDir Path tmp) {
    var specs =
        TList.of(
            new InvisibleBuiltinSpec("a", "A_FN_SCOPE_ID", 9001),
            new InvisibleBuiltinSpec("b", "B_FN_SCOPE_ID", 9001));
    assertThrows(
        IllegalStateException.class,
        () -> InvisibleBuiltinsGen.run(tmp, FORMATTER, specs, new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_name(@TempDir Path tmp) {
    var specs =
        TList.of(
            new InvisibleBuiltinSpec("dup", "A_FN_SCOPE_ID", 9001),
            new InvisibleBuiltinSpec("dup", "B_FN_SCOPE_ID", 9002));
    assertThrows(
        IllegalStateException.class,
        () -> InvisibleBuiltinsGen.run(tmp, FORMATTER, specs, new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_fn_scope_id(@TempDir Path tmp) {
    var specs =
        TList.of(
            new InvisibleBuiltinSpec("a", "DUP_FN_SCOPE_ID", 9001),
            new InvisibleBuiltinSpec("b", "DUP_FN_SCOPE_ID", 9002));
    assertThrows(
        IllegalStateException.class,
        () -> InvisibleBuiltinsGen.run(tmp, FORMATTER, specs, new TSet<Long>()));
  }

  @Test
  void rejects_non_screaming_snake(@TempDir Path tmp) {
    var specs = TList.of(new InvisibleBuiltinSpec("a", "lower_fn_scope_id", 9001));
    assertThrows(
        IllegalStateException.class,
        () -> InvisibleBuiltinsGen.run(tmp, FORMATTER, specs, new TSet<Long>()));
  }

  @Test
  void rejects_cross_generator_scope_id_collision(@TempDir Path tmp) {
    var existing = new TSet<Long>();
    existing.add(7017L);
    assertThrows(
        IllegalStateException.class,
        () ->
            InvisibleBuiltinsGen.run(
                tmp,
                FORMATTER,
                TList.of(new InvisibleBuiltinSpec("stage", "STAGE_FN_SCOPE_ID", 7017)),
                existing));
  }
}
