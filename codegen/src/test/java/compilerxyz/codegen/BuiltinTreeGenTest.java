package compilerxyz.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BuiltinTreeGenTest {
  private static final Formatter FORMATTER =
      Formatter.createFormatter(
          JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

  @Test
  void emits_gen_builtin_tree_with_canonical_spec(@TempDir Path tmp) throws IOException {
    BuiltinTreeGen.run(tmp, FORMATTER, new TSet<Long>(), new TSet<Long>());
    var path = tmp.resolve("compilerxyz/gen/GenBuiltinTree.java");
    assertTrue(Files.exists(path), "GenBuiltinTree.java not emitted");
    var raw = Files.readString(path);
    var src = raw.replaceAll("\\s+", " ");

    assertTrue(src.contains("public static BuiltinTree build("), src);
    assertTrue(src.contains("ImportStateId importState"), src);
    assertTrue(src.contains("GenBuiltinInterfaces interfaces"), src);
    assertTrue(!src.contains("BuiltinTreeFunctionLeafFactory"), src);

    assertTrue(src.contains("\"throw\""), src);
    assertTrue(src.contains("\"panic\""), src);
    assertTrue(src.contains("\"log\""), src);
    assertTrue(src.contains("\"define\""), src);
    assertTrue(src.contains("\"types\""), src);
    assertTrue(src.contains("\"eval\""), src);

    assertTrue(
        src.contains("new BindingId.Builtin(7025L)"),
        "throw InitialBind missing: " + src);
    assertTrue(
        src.contains("new BindingId.Builtin(7026L)"),
        "panic InitialBind missing: " + src);

    assertTrue(
        src.contains("new BuiltinTree.BuiltinLeaf.Function( new BindingId.Builtin(5001L)"),
        "log Function missing: " + src);
    assertTrue(
        src.contains("new BuiltinTree.BuiltinLeaf.Constructor( new BindingId.Builtin(5010L)"),
        "list Constructor missing: " + src);

    for (var name : TList.of("string", "number", "null", "bool", "any", "constkey", "expr",
        "aspect", "codedyn", "cve", "list", "map", "option")) {
      assertTrue(src.contains("\"" + name + "\""), "missing types branch entry: " + name);
    }

    assertTrue(
        src.contains("new BindingId.Builtin(7023L)"),
        "aspect_lookup InitialBind missing: " + src);
    assertTrue(
        src.contains("new BindingId.Builtin(7024L)"),
        "cast_value InitialBind missing: " + src);

    assertTrue(
        src.contains("new BuiltinTree.BuiltinLeaf.AspectId( new BindingId.Builtin(4001L)"),
        "aspect_string_dynamic AspectId missing: " + src);
    assertTrue(
        src.contains("new BuiltinTree.BuiltinLeaf.AspectId( new BindingId.Builtin(4002L)"),
        "aspect_function_id AspectId missing: " + src);
    assertTrue(
        src.contains("new BuiltinTree.BuiltinLeaf.AspectId( new BindingId.Builtin(4003L)"),
        "aspect_field_id AspectId missing: " + src);

    for (var scopeId : TList.of(3003, 3004, 3008, 3005, 3006, 3007)) {
      assertTrue(
          src.contains(
              "new BuiltinTree.BuiltinLeaf.Interface( new BindingId.Builtin(" + scopeId + "L)"),
          "missing Interface leaf for scope id " + scopeId + ": " + src);
    }

    assertTrue(
        src.contains("new External.FunctionExternal(Builtin_b5001::invoke)"),
        "missing External.FunctionExternal(Builtin_b5001::invoke) for log: " + src);
    assertTrue(
        Files.exists(tmp.resolve("compilerxyz/gen/Builtin_b5001.java")),
        "missing per-leaf helper Builtin_b5001.java");
    assertTrue(
        Files.exists(tmp.resolve("compilerxyz/gen/Builtin_b5008.java")),
        "missing per-leaf helper Builtin_b5008.java (const_eval)");
    assertTrue(
        Files.exists(tmp.resolve("compilerxyz/gen/Builtin_b5010.java")),
        "missing per-leaf helper Builtin_b5010.java (types.list constructor)");

    assertTrue(
        src.contains(
            "new External.SourceInterfaceRefExternal( new SourceInterfaceRef(interfaces.aspect_function))"),
        "interface leaf must inline the SourceInterfaceRefExternal: " + src);

    assertTrue(
        src.contains("new External.UniqueExternal(AspectStringDynamic.ID)"),
        "missing direct External.UniqueExternal for aspect_string_dynamic: " + src);
    assertTrue(
        src.contains("new External.UniqueExternal(AspectFunction.ID)"),
        "missing direct External.UniqueExternal for aspect_function_id: " + src);
    assertTrue(
        src.contains("new External.UniqueExternal(AspectField.ID)"),
        "missing direct External.UniqueExternal for aspect_field_id: " + src);
  }

  @Test
  void extra_interface_specs_match_builtin_tree_rs() {
    var extras = BuiltinTreeGen.extraInterfaceSpecs();
    var names = extras.stream().map(s -> s.name()).toList();
    assertEquals(
        TList.of(
            "aspect_function",
            "aspect_field",
            "aspect_drop_dynamic",
            "binding",
            "target",
            "code_dyn"),
        names);

    for (var s : extras) {
      switch (s.name()) {
        case "aspect_function" -> {
          assertEquals(1016, s.builtinUniqueId());
          assertEquals(3003, s.scopeId());
        }
        case "aspect_field" -> {
          assertEquals(1030, s.builtinUniqueId());
          assertEquals(3004, s.scopeId());
        }
        case "aspect_drop_dynamic" -> {
          assertEquals(2008, s.builtinUniqueId());
          assertEquals(3008, s.scopeId());
        }
        case "binding" -> {
          assertEquals(2005, s.builtinUniqueId());
          assertEquals(3005, s.scopeId());
          assertEquals("Binding", s.javaTraitName());
        }
        case "target" -> {
          assertEquals(2006, s.builtinUniqueId());
          assertEquals(3006, s.scopeId());
          assertEquals("Target", s.javaTraitName());
        }
        case "code_dyn" -> {
          assertEquals(2007, s.builtinUniqueId());
          assertEquals(3007, s.scopeId());
        }
        default -> throw new AssertionError("unexpected spec: " + s.name());
      }
    }
  }

  @Test
  void claims_compile_scope_markers_in_shared_set(@TempDir Path tmp) throws IOException {
    var shared = new TSet<Long>();
    BuiltinTreeGen.run(tmp, FORMATTER, new TSet<Long>(), shared);

    assertTrue(shared.contains(BuiltinTreeGen.ASPECT_COMPILE_SCOPE_ID));
    assertTrue(shared.contains(BuiltinTreeGen.CAST_VALUE_COMPILE_SCOPE_ID));
    assertTrue(shared.contains(BuiltinTreeGen.THROW_COMPILE_SCOPE_ID));
    assertTrue(shared.contains(BuiltinTreeGen.PANIC_COMPILE_SCOPE_ID));
  }

  @Test
  void rejects_pre_claimed_compile_scope_marker(@TempDir Path tmp) {
    var shared = new TSet<Long>();
    shared.add(BuiltinTreeGen.THROW_COMPILE_SCOPE_ID);
    assertThrows(
        IllegalStateException.class,
        () -> BuiltinTreeGen.run(tmp, FORMATTER, new TSet<Long>(), shared));
  }

  @Test
  void rejects_pre_claimed_function_scope_id(@TempDir Path tmp) {
    var shared = new TSet<Long>();
    shared.add(5001L);
    assertThrows(
        IllegalStateException.class,
        () -> BuiltinTreeGen.run(tmp, FORMATTER, new TSet<Long>(), shared));
  }
}
