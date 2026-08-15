package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import compilerxyz.util.TMap;
import java.util.HashMap;
import java.util.Map;
import com.squareup.javapoet.ParameterizedTypeName;

/**
 * Runtime {@code Type}-descriptor lookup for codegen. The <em>descriptor
 * expression</em> for any type discovered by {@link
 * compilerxyz.codegen.experimental.DynamicTypeScanner} — every {@code @Builtin}
 * type (routed to its generated {@code GenX.dynamicType()}) and any type that
 * hand-declares a nullary {@code dynamicType()} — comes from that scan; this
 * class is not a second registry of them.
 *
 * <p>What lives here is only what the scan can't derive: the primitive/leaf
 * <em>value classes</em> (the JVM class a {@code TypeXxxDynamic} represents,
 * e.g. {@code String} for {@code TypeStringDynamic}), built with {@code new
 * TypeXxxDynamic()}, and the composite wrappers ({@code TList}/{@code
 * Opt}/{@code EResult}/{@code TMap}).
 */
public final class DynamicTypes {
  private DynamicTypes() {}

  private record CompositeEntry(int arity, TypeName valueClass) {}

  /** Value types built with {@code new X()}, mapped to the JVM class they represent. */
  private static final Map<ClassName, TypeName> NULLARY = new HashMap<>();
  private static final Map<ClassName, CompositeEntry> COMPOSITES = new HashMap<>();

  /** Descriptor expressions discovered by the scan; injected before any lookup. */
  private static Map<ClassName, CodeBlock> scannedLeaves = Map.of();

  public static void setScannedLeaves(TMap<ClassName, CodeBlock> scanned) {
    scannedLeaves = new HashMap<>();
    for (var e : scanned.entrySet()) scannedLeaves.put(e.getKey(), e.getValue());
  }

  private static void nullary(String pkg, String cls, TypeName valueClass) {
    NULLARY.put(ClassName.get(pkg, cls), valueClass);
  }

  private static void nullary(String pkg, String cls) {
    var cn = ClassName.get(pkg, cls);
    NULLARY.put(cn, cn);
  }

  private static void composite(String pkg, String cls, int arity, TypeName valueClass) {
    COMPOSITES.put(ClassName.get(pkg, cls), new CompositeEntry(arity, valueClass));
  }

  static {
    // Nullary value types — value classes are unboxed / distinct where appropriate.
    nullary("compilerxyz.eval.value.string", "TypeStringDynamic", ClassName.get(String.class));
    nullary("compilerxyz.eval.value.bool_", "TypeBoolDynamic", TypeName.BOOLEAN);
    nullary("compilerxyz.eval.value.int_", "TypeIntDynamic", TypeName.INT);
    nullary("compilerxyz.eval.value.short_", "TypeShortDynamic", TypeName.SHORT);
    nullary("compilerxyz.eval.value.long_", "TypeLongDynamic", TypeName.LONG);
    nullary("compilerxyz.eval.value.float_", "TypeFloatDynamic", TypeName.FLOAT);
    nullary("compilerxyz.eval.value.double_", "TypeDoubleDynamic", TypeName.DOUBLE);
    nullary("compilerxyz.eval.value.ubyte_", "TypeUbyteDynamic", TypeName.BYTE);
    nullary("compilerxyz.eval.value.ushort_", "TypeUshortDynamic", TypeName.SHORT);
    nullary("compilerxyz.eval.value.uint_", "TypeUintDynamic", TypeName.INT);
    nullary("compilerxyz.eval.value.ulong_", "TypeUlongDynamic", TypeName.LONG);
    nullary("compilerxyz.eval.value.byte_", "TypeByteDynamic", TypeName.BYTE);
    nullary("compilerxyz.eval.value.null_", "TypeNullDynamic", ClassName.OBJECT);
    nullary(
        "compilerxyz.eval.value.unique",
        "TypeUniqueDynamic",
        ClassName.get("compilerxyz.eval", "Unique"));
    nullary(
        "compilerxyz.eval.value.unique",
        "TypeUniqueConst",
        ClassName.get("compilerxyz.eval", "Unique"));
    nullary(
        "compilerxyz.eval.value.constkey",
        "TypeConstkeyDynamic",
        ClassName.get("compilerxyz.eval", "ConstKey"));
    nullary(
        "compilerxyz.eval.value.expr",
        "TypeExpr",
        ClassName.get("compilerxyz.eval.expr", "StagedExprCell"));
    nullary("compilerxyz.eval.value.throw_", "TypeThrowDynamic");
    nullary("compilerxyz.eval.value.panic", "TypePanicDynamic");
    nullary("compilerxyz.eval.value.function", "TypeFunctionAspectDynamic");
    nullary("compilerxyz.eval.value.function", "TypeFunctionCastValueDynamic");
    nullary("compilerxyz.eval.value.function", "TypeFunctionMustReinterpretConst");
    nullary("compilerxyz.eval.value.list", "TypeListConstructorConst");
    nullary("compilerxyz.eval.value.map", "TypeMapConstructorConst");
    nullary("compilerxyz.eval.value.option", "TypeOptionConstructorConst");
    nullary("compilerxyz.eval.value.eresult", "TypeEResultConstructorConst");
    nullary("compilerxyz.eval.value.zbase32", "TypeZbase32Decode");
    nullary("compilerxyz.eval.value.source", "TypeImport");
    nullary("compilerxyz.eval.value.source", "TypeSourceVirtual");
    nullary("compilerxyz.eval.value.source", "TypeSourceLocal");
    nullary("compilerxyz.eval.value.source", "TypeSourceRemote");
    nullary("compilerxyz.eval.value.source", "TypeSourceSha256");

    // Composite descriptors.
    composite(
        "compilerxyz.eval.value.list",
        "TypeListDynamic",
        1,
        ClassName.get("compilerxyz.util", "TList"));
    composite(
        "compilerxyz.eval.value.option",
        "TypeOptionDynamic",
        1,
        ClassName.get("compilerxyz.util", "Opt"));
    composite(
        "compilerxyz.eval.value.eresult",
        "TypeEResultDynamic",
        1,
        ClassName.get("compilerxyz.eval", "EResult"));
    composite(
        "compilerxyz.eval.value.map",
        "TypeMapDynamic",
        2,
        ClassName.get("compilerxyz.util", "TMap"));
  }

  public static CodeBlock leaf(ClassName descriptor) {
    if (NULLARY.containsKey(descriptor)) {
      return CodeBlock.of("new $T()", descriptor);
    }
    var scanned = scannedLeaves.get(descriptor);
    if (scanned != null) return scanned;
    if (descriptor.packageName().equals("compilerxyz.gen")) {
      // A gen-package companion is its own descriptor source.
      return CodeBlock.of("$T.dynamicType()", descriptor);
    }
    throw new IllegalArgumentException(
        "DynamicTypes has no descriptor for " + descriptor.canonicalName());
  }

  public static TypeName leafValueClass(ClassName descriptor) {
    var value = NULLARY.get(descriptor);
    // Every other type is its own value class (the JVM class is the type).
    return value != null ? value : descriptor;
  }

  public static CodeBlock composite(ClassName wrapper, CodeBlock... params) {
    var spec = compositeEntry(wrapper);
    if (spec.arity() != params.length) {
      throw new IllegalArgumentException(
          "DynamicTypes composite "
              + wrapper.canonicalName()
              + " expects "
              + spec.arity()
              + " params, got "
              + params.length);
    }
    var joined = CodeBlock.builder();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) joined.add(", ");
      joined.add(params[i]);
    }
    return CodeBlock.of("new $T($L)", wrapper, joined.build());
  }

  public static TypeName compositeValueClass(ClassName wrapper, TypeName... params) {
    var spec = compositeEntry(wrapper);
    if (spec.arity() != params.length) {
      throw new IllegalArgumentException(
          "DynamicTypes composite "
              + wrapper.canonicalName()
              + " expects "
              + spec.arity()
              + " params, got "
              + params.length);
    }
    return ParameterizedTypeName.get((ClassName) spec.valueClass(), params);
  }

  private static CompositeEntry compositeEntry(ClassName wrapper) {
    var spec = COMPOSITES.get(wrapper);
    if (spec == null) {
      throw new IllegalArgumentException(
          "DynamicTypes has no composite entry for " + wrapper.canonicalName());
    }
    return spec;
  }
}
