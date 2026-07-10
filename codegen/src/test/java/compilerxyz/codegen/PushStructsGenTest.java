package compilerxyz.codegen;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.JavaFormatterOptions;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.codegen.PushStructsGen.PushField;
import compilerxyz.codegen.PushStructsGen.PushStructSpec;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PushStructsGenTest {
  private static final Formatter FORMATTER =
      Formatter.createFormatter(
          JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());

  private static final ClassName ANY_DYN = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName STRING_DYN =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName BOOL_DYN =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");

  private static TypeRef ref(ClassName cn) {
    return new TypeRef(cn, CodeBlock.of("new $T()", cn));
  }

  @Test
  void emits_data_class_and_type_sentinel(@TempDir Path tmp) throws IOException {
    var spec =
        new PushStructSpec(
            "DemoBuilder",
            9001L,
            TList.of(new NamedTypeRef("inner", ref(ANY_DYN))),
            TList.of(new PushField("type_ref_", "inner", ref(ANY_DYN), CodeBlock.of("self.inner"))),
            TList.of(
                new PushMethod(
                    "build",
                    TList.of(new NamedTypeRef("name", ref(STRING_DYN))),
                    "new compilerxyz.eval.value.bool_.TypeBoolDynamic()",
                    CodeBlock.of("/* body */"))));
    PushStructsGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());

    var dataClass = tmp.resolve("compilerxyz/gen/DemoBuilder.java");
    var sentinel = tmp.resolve("compilerxyz/gen/TypeDemoBuilderDynamic.java");
    assertTrue(Files.exists(dataClass), "data class missing");
    assertTrue(Files.exists(sentinel), "type sentinel missing");

    var dataSrc = Files.readString(dataClass);
    assertTrue(dataSrc.contains("public final class DemoBuilder"), dataSrc);
    assertTrue(dataSrc.contains("public final Type inner;"), dataSrc);
    assertTrue(dataSrc.contains("public DemoBuilder(Type inner)"), dataSrc);

    var sentinelSrc = Files.readString(sentinel);
    assertTrue(
        sentinelSrc.contains("public final class TypeDemoBuilderDynamic"), sentinelSrc);
    assertTrue(
        sentinelSrc.contains("public static final Unique IDENTITY = new Unique.Builtin(9001L)"),
        sentinelSrc);
    assertTrue(sentinelSrc.contains("implements Type, AspectField"), sentinelSrc);

    assertTrue(
        sentinelSrc.contains("DynUtil.buildFieldGetSequence(dyn, \"_0\")"),
        "missing field _0 dispatch: " + sentinelSrc);
    assertTrue(
        sentinelSrc.contains("DynUtil.buildFieldGetSequence(dyn, \"_1\")"),
        "missing method _1 dispatch: " + sentinelSrc);

    assertTrue(
        sentinelSrc.contains("keyStr.equals(\"type_ref\")"),
        "field key match missing: " + sentinelSrc);
    assertTrue(
        sentinelSrc.contains("keyStr.equals(\"build\")"),
        "method key match missing: " + sentinelSrc);
    assertTrue(sentinelSrc.contains("TypeFunctionDynamic"), sentinelSrc);
  }

  @Test
  void multi_arg_method_arg_type_wraps_in_tuple(@TempDir Path tmp) throws IOException {
    var spec =
        new PushStructSpec(
            "MultiArg",
            9002L,
            TList.of(),
            TList.of(),
            TList.of(
                new PushMethod(
                    "doIt",
                    TList.of(
                        new NamedTypeRef("a", ref(ANY_DYN)),
                        new NamedTypeRef("b", ref(BOOL_DYN))),
                    "()",
                    CodeBlock.of(""))));
    PushStructsGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());
    var src = Files.readString(tmp.resolve("compilerxyz/gen/TypeMultiArgDynamic.java"));
    assertTrue(src.contains("new Tuple"), "missing arg-tuple wrap: " + src);
    assertTrue(src.contains("TupleElement.Dynamic"), src);
  }

  @Test
  void zero_arg_method_ret_unit_becomes_empty_tuple(@TempDir Path tmp) throws IOException {
    var spec =
        new PushStructSpec(
            "UnitRet",
            9003L,
            TList.of(),
            TList.of(),
            TList.of(new PushMethod("noop", TList.of(), "()", CodeBlock.of(""))));
    PushStructsGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>());
    var src = Files.readString(tmp.resolve("compilerxyz/gen/TypeUnitRetDynamic.java"));

    assertTrue(src.contains("TList.<TupleElement>of()"), src);
  }

  @Test
  void rejects_duplicate_spec_name(@TempDir Path tmp) {
    var a = new PushStructSpec("Dup", 9004L, TList.of(), TList.of(), TList.of());
    var b = new PushStructSpec("Dup", 9005L, TList.of(), TList.of(), TList.of());
    assertThrows(
        IllegalStateException.class,
        () -> PushStructsGen.run(tmp, FORMATTER, TList.of(a, b), new TSet<Long>(), new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_builtin_id(@TempDir Path tmp) {
    var a = new PushStructSpec("A", 9006L, TList.of(), TList.of(), TList.of());
    var b = new PushStructSpec("B", 9006L, TList.of(), TList.of(), TList.of());
    assertThrows(
        IllegalStateException.class,
        () -> PushStructsGen.run(tmp, FORMATTER, TList.of(a, b), new TSet<Long>(), new TSet<Long>()));
  }

  @Test
  void rejects_duplicate_member_name(@TempDir Path tmp) {
    var spec =
        new PushStructSpec(
            "Conflict",
            9007L,
            TList.of(),
            TList.of(new PushField("clash", "clash", ref(ANY_DYN), CodeBlock.of("null"))),
            TList.of(new PushMethod("clash", TList.of(), "()", CodeBlock.of(""))));
    assertThrows(
        IllegalStateException.class,
        () -> PushStructsGen.run(tmp, FORMATTER, TList.of(spec), new TSet<Long>(), new TSet<Long>()));
  }
}
