package compilerxyz.codegen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class IdentitiesGenTest {
  private static final Formatter FORMATTER =
      Formatter.createFormatter(
          JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

  @Test
  void generates_all_identities(@TempDir Path tmp) throws IOException {
    IdentitiesGen.run(tmp, FORMATTER, new TSet<>(), new TSet<>());
    var path = tmp.resolve("compilerxyz/gen/GenIdentities.java");
    assertTrue(Files.exists(path), "GenIdentities.java not emitted");
    var src = Files.readString(path);

    String[][] entries = {
      {"EXPR_CONTEXT_REF_TYPE_IDENTITY", "6001"},
      {"CODE_VALUE_EXTERN_TYPE_IDENTITY", "6002"},
      {"TYPE_UNIQUE_IDENTITY", "6003"},
      {"INTERFACE_CONST_IDENTITY", "6004"},
      {"INTERFACE_REF_TYPE_IDENTITY", "6005"},
      {"TYPE_TYPE_IDENTITY", "6006"},
      {"UNION_REF_TYPE_IDENTITY", "6007"},
      {"TYPE_OPTION_IDENTITY", "6008"},
      {"TYPE_CODE_STRUCT_IDENTITY", "6009"},
      {"TYPE_CODE_ENUM_IDENTITY", "6010"},
      {"TYPE_CODE_DYN_HANDLE_IDENTITY", "6011"},
      {"TYPE_ASPECT_IDENTITY", "6012"},
      {"EXPR_TYPE_IDENTITY", "6013"},
      {"JAVA_DATA_DESCRIPTOR_TYPE_IDENTITY", "6014"},
    };
    for (var e : entries) {
      var needle = "public static final Unique " + e[0] + " = new Unique.Builtin(" + e[1] + "L)";
      assertTrue(src.contains(needle), "missing: " + needle + " in:\n" + src);
    }
  }
}
