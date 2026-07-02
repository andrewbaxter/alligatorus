package compilerxyz.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.MethodSig;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InterfacesGenTest {
  private static final Formatter FORMATTER =
      Formatter.createFormatter(
          JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

  private static final ClassName ANY_DYN =
      ClassName.get("compilerxyz.eval.value.any", "TypeAnyDynamic");

  private static TypeRef opaqueAny() {
    return new TypeRef(ANY_DYN, CodeBlock.of("new $T()", ANY_DYN));
  }

  @Test
  void generates_holder_and_trait_for_rust_spec(@TempDir Path tmp) throws IOException {
    var any = opaqueAny();
    var sig =
        new MethodSig(
            TList.of(new NamedTypeRef("xx", any)), TList.of(new NamedTypeRef("value", any)));
    var spec =
        new InterfaceSpec(
            "demo",
            9001,
            9101,
            "d",
            "VALUE",
            "Demo iface description",
            false,
            "DemoIface",
            TList.of(new Method("do_thing", any, new MethodArgs.Rust(sig))));
    InterfacesGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());

    var holder = tmp.resolve("compilerxyz/gen/GenBuiltinInterfaces.java");
    var trait = tmp.resolve("compilerxyz/gen/DemoIface.java");
    assertTrue(Files.exists(holder), "holder file missing");
    assertTrue(Files.exists(trait), "trait file missing");

    var holderSrc = Files.readString(holder);
    assertTrue(holderSrc.contains("public final SourceInterfaceData demo;"), holderSrc);
    assertTrue(
        holderSrc.contains("IM_DEMO_DO_THING_FIELD = \"_im_d0\""),
        "missing field-name constant: " + holderSrc);
    assertTrue(
        holderSrc.contains("builtinDemoIfaceIdentity"),
        "missing identity helper: " + holderSrc);
    assertTrue(
        holderSrc.contains("BuiltinInterfaceKind.VALUE"),
        "missing builtin kind ref: " + holderSrc);
    assertTrue(holderSrc.contains("\"Demo iface description\""), holderSrc);

    var traitSrc = Files.readString(trait);
    assertTrue(traitSrc.contains("public interface DemoIface"), traitSrc);

    assertTrue(traitSrc.contains("do_thing"), "missing trait method: " + traitSrc);
  }

  @Test
  void generates_source_arg_record(@TempDir Path tmp) throws IOException {
    var any = opaqueAny();
    var spec =
        new InterfaceSpec(
            "demo2",
            9002,
            9102,
            "e",
            "TYPE",
            null,
            false,
            null,
            TList.of(
                new Method(
                    "do2",
                    any,
                    new MethodArgs.Source(TList.of(new NamedTypeRef("alpha", any))))));
    InterfacesGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());
    var src = Files.readString(tmp.resolve("compilerxyz/gen/GenBuiltinInterfaces.java"));
    assertTrue(src.contains("argRecordType = new Record"), src);
    assertTrue(src.contains("ConstKey.Str(\"alpha\")"), src);
    assertTrue(src.contains("dataFieldNames.add(\"alpha\")"), src);
  }

  @Test
  void rejects_duplicate_interface_name(@TempDir Path tmp) {
    var any = opaqueAny();
    var s1 =
        new InterfaceSpec(
            "dup", 9101, 9201, "p", "VALUE", null, false, null, TList.of());
    var s2 =
        new InterfaceSpec(
            "dup", 9102, 9202, "q", "TYPE", null, false, null, TList.of());
    assertThrows(
        IllegalStateException.class, () -> InterfacesGen.run(tmp, FORMATTER, TList.of(s1, s2), new TSet<Long>(), new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_field_prefix(@TempDir Path tmp) {
    var s1 =
        new InterfaceSpec(
            "a", 9103, 9203, "x", "VALUE", null, false, null, TList.of());
    var s2 =
        new InterfaceSpec("b", 9104, 9204, "x", "TYPE", null, false, null, TList.of());
    assertThrows(
        IllegalStateException.class, () -> InterfacesGen.run(tmp, FORMATTER, TList.of(s1, s2), new TSet<Long>(), new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_method_name(@TempDir Path tmp) {
    var any = opaqueAny();
    var spec =
        new InterfaceSpec(
            "c",
            9105,
            9205,
            "z",
            "VALUE",
            null,
            false,
            null,
            TList.of(
                new Method("m", any, new MethodArgs.Source(TList.of())),
                new Method("m", any, new MethodArgs.Source(TList.of()))));
    assertThrows(
        IllegalStateException.class,
        () -> InterfacesGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>()));
  }

  @Test
  void multi_param_rust_arg_wraps_in_tuple(@TempDir Path tmp) throws IOException {
    var any = opaqueAny();
    var sig =
        new MethodSig(
            TList.of(),
            TList.of(new NamedTypeRef("p1", any), new NamedTypeRef("p2", any)));
    var spec =
        new InterfaceSpec(
            "multi",
            9106,
            9206,
            "m",
            "BINDING",
            null,
            false,
            null,
            TList.of(new Method("call", any, new MethodArgs.Rust(sig))));
    InterfacesGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());
    var src = Files.readString(tmp.resolve("compilerxyz/gen/GenBuiltinInterfaces.java"));
    assertTrue(src.contains("new Tuple"), "expected multi-arg wrapping: " + src);
    assertTrue(src.contains("TupleElement.Dynamic"), src);
  }

  @Test
  void java_keyword_field_name_escaped(@TempDir Path tmp) throws IOException {
    var any = opaqueAny();
    var spec =
        new InterfaceSpec(
            "type",
            9107,
            9207,
            "t",
            "TYPE",
            null,
            false,
            null,
            TList.of(new Method("m", any, new MethodArgs.Source(TList.of()))));
    InterfacesGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());
    var src = Files.readString(tmp.resolve("compilerxyz/gen/GenBuiltinInterfaces.java"));
    assertTrue(src.contains("public final SourceInterfaceData type_;"), src);

    assertTrue(src.contains("builtinTypeIfaceIdentity"), src);
    assertEquals(1, count(src, "IM_TYPE_M_FIELD"));
  }

  private static int count(String haystack, String needle) {
    var n = 0;
    var i = 0;
    while ((i = haystack.indexOf(needle, i)) != -1) {
      n++;
      i += needle.length();
    }
    return n;
  }
}
