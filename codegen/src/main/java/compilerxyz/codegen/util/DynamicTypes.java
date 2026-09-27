package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.TMap;

public final class DynamicTypes {
    private DynamicTypes() {}

    private record CompositeEntry(int arity, TypeName valueClass) {}

    public static final IMap<ClassName, TypeName> NULLARY;
    private static final IMap<ClassName, CompositeEntry> COMPOSITES;

    private static void nullary(TMap<ClassName, TypeName> nullary, String pkg, String cls, TypeName valueClass) {
        nullary.put(ClassName.get(pkg, cls), valueClass);
    }

    private static void nullary(TMap<ClassName, TypeName> nullary, String pkg, String cls) {
        var cn = ClassName.get(pkg, cls);
        nullary.put(cn, cn);
    }

    private static void composite(
            TMap<ClassName, CompositeEntry> composites, String pkg, String cls, int arity, TypeName valueClass) {
        composites.put(ClassName.get(pkg, cls), new CompositeEntry(arity, valueClass));
    }

    static {
        var nullary = new TMap<ClassName, TypeName>();
        var composites = new TMap<ClassName, CompositeEntry>();
        nullary(nullary, "compilerxyz.main.eval.value.string", "TypeStringDynamic", ClassName.get(String.class));
        nullary(nullary, "compilerxyz.main.eval.value.bool", "TypeBoolDynamic", TypeName.BOOLEAN);
        nullary(nullary, "compilerxyz.main.eval.value.int_", "TypeIntDynamic", TypeName.INT);
        nullary(nullary, "compilerxyz.main.eval.value.short_", "TypeShortDynamic", TypeName.SHORT);
        nullary(nullary, "compilerxyz.main.eval.value.long_", "TypeLongDynamic", TypeName.LONG);
        nullary(nullary, "compilerxyz.main.eval.value.float_", "TypeFloatDynamic", TypeName.FLOAT);
        nullary(nullary, "compilerxyz.main.eval.value.double_", "TypeDoubleDynamic", TypeName.DOUBLE);
        nullary(nullary, "compilerxyz.main.eval.value.ubyte", "TypeUbyteDynamic", TypeName.BYTE);
        nullary(nullary, "compilerxyz.main.eval.value.ushort", "TypeUshortDynamic", TypeName.SHORT);
        nullary(nullary, "compilerxyz.main.eval.value.uint", "TypeUintDynamic", TypeName.INT);
        nullary(nullary, "compilerxyz.main.eval.value.ulong", "TypeUlongDynamic", TypeName.LONG);
        nullary(nullary, "compilerxyz.main.eval.value.byte_", "TypeByteDynamic", TypeName.BYTE);
        nullary(nullary, "compilerxyz.main.eval.value.bool", "TypeBoolDynamicBox", TypeName.BOOLEAN.box());
        nullary(nullary, "compilerxyz.main.eval.value.int_", "TypeIntDynamicBox", TypeName.INT.box());
        nullary(nullary, "compilerxyz.main.eval.value.short_", "TypeShortDynamicBox", TypeName.SHORT.box());
        nullary(nullary, "compilerxyz.main.eval.value.long_", "TypeLongDynamicBox", TypeName.LONG.box());
        nullary(nullary, "compilerxyz.main.eval.value.float_", "TypeFloatDynamicBox", TypeName.FLOAT.box());
        nullary(nullary, "compilerxyz.main.eval.value.double_", "TypeDoubleDynamicBox", TypeName.DOUBLE.box());
        nullary(nullary, "compilerxyz.main.eval.value.ubyte", "TypeUbyteDynamicBox", TypeName.BYTE.box());
        nullary(nullary, "compilerxyz.main.eval.value.ushort", "TypeUshortDynamicBox", TypeName.SHORT.box());
        nullary(nullary, "compilerxyz.main.eval.value.uint", "TypeUintDynamicBox", TypeName.INT.box());
        nullary(nullary, "compilerxyz.main.eval.value.ulong", "TypeUlongDynamicBox", TypeName.LONG.box());
        nullary(nullary, "compilerxyz.main.eval.value.byte_", "TypeByteDynamicBox", TypeName.BYTE.box());
        nullary(nullary, "compilerxyz.main.eval.value.jvm_object", "TypeJvmObjectDynamic", ClassName.OBJECT);
        nullary(
                nullary,
                "compilerxyz.main.eval.value.code",
                "TypeCodeDynamic",
                ClassName.get("compilerxyz.main.eval", "Code"));
        nullary(
                nullary,
                "compilerxyz.main.eval.value.unique",
                "TypeUniqueDynamic",
                ClassName.get("compilerxyz.main.eval", "Unique"));
        nullary(
                nullary,
                "compilerxyz.main.eval.value.unique",
                "TypeUniqueConst",
                ClassName.get("compilerxyz.main.eval", "Unique"));
        nullary(
                nullary,
                "compilerxyz.main.eval.value.constkey",
                "TypeConstkeyDynamic",
                ClassName.get("compilerxyz.main.eval", "ConstKey"));
        nullary(
                nullary,
                "compilerxyz.main.eval.value.expr",
                "TypeExpr",
                ClassName.get("compilerxyz.main.eval.expr", "StagedExprCell"));
        nullary(nullary, "compilerxyz.main.eval.value.throw_", "TypeThrowConst");
        nullary(nullary, "compilerxyz.main.eval.value.panic", "TypePanicConst");
        nullary(nullary, "compilerxyz.main.eval.value.function", "TypeFunctionAspectConst");
        nullary(nullary, "compilerxyz.main.eval.value.function", "TypeFunctionMustReinterpretConst");
        nullary(nullary, "compilerxyz.main.eval.value.list", "TypeListConstructorConst");
        nullary(nullary, "compilerxyz.main.eval.value.map", "TypeMapConstructorConst");
        nullary(nullary, "compilerxyz.main.eval.value.option", "TypeOptionConstructorConst");
        nullary(nullary, "compilerxyz.main.eval.value.eresult", "TypeEResultConstructorConst");
        nullary(nullary, "compilerxyz.main.eval.value.zbase32", "TypeZbase32Decode");
        nullary(nullary, "compilerxyz.main.eval.value.source", "TypeImport");
        nullary(nullary, "compilerxyz.main.eval.value.source", "TypeSourceVirtual");
        nullary(nullary, "compilerxyz.main.eval.value.source", "TypeSourceLocal");
        nullary(nullary, "compilerxyz.main.eval.value.source", "TypeSourceRemote");
        nullary(nullary, "compilerxyz.main.eval.value.source", "TypeSourceSha256");
        composite(
                composites,
                "compilerxyz.main.eval.value.list",
                "TypeListBoxedDynamic",
                1,
                ClassName.get("compilerxyz.prelude.util", "TList"));
        composite(
                composites,
                "compilerxyz.main.eval.value.option",
                "TypeOptionDynamic",
                1,
                ClassName.get("compilerxyz.prelude.util", "Opt"));
        composite(
                composites,
                "compilerxyz.main.eval.value.eresult",
                "TypeEResultDynamic",
                1,
                ClassName.get("compilerxyz.main.eval", "EResult"));
        composite(
                composites,
                "compilerxyz.main.eval.value.map",
                "TypeMapDynamic",
                2,
                ClassName.get("compilerxyz.prelude.util", "TOrderedMap"));
        NULLARY = IMap.from(nullary);
        COMPOSITES = IMap.from(composites);
    }

    public static CodeBlock leaf(ClassName descriptor, TMap<ClassName, CodeBlock> scannedLeaves) {
        if (NULLARY.containsKey(descriptor)) {
            return CodeBlock.of("new $T()", descriptor);
        }
        var scanned = scannedLeaves.getOpt(descriptor);
        if (scanned.optIsSome()) return scanned.optUnwrap();
        if (descriptor.packageName().equals("compilerxyz.main.gen")) {
            return CodeBlock.of("$T.dynamicType()", descriptor);
        }
        throw new IllegalArgumentException("DynamicTypes has no descriptor for " + descriptor.canonicalName());
    }

    public static CodeBlock composite(ClassName wrapper, CodeBlock... params) {
        var spec = compositeEntry(wrapper);
        if (spec.arity() != params.length) {
            throw new IllegalArgumentException("DynamicTypes composite "
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
            throw new IllegalArgumentException("DynamicTypes composite "
                    + wrapper.canonicalName()
                    + " expects "
                    + spec.arity()
                    + " params, got "
                    + params.length);
        }
        return ParameterizedTypeName.get((ClassName) spec.valueClass(), params);
    }

    private static CompositeEntry compositeEntry(ClassName wrapper) {
        var spec = COMPOSITES.getOpt(wrapper);
        if (spec.optIsNone()) {
            throw new IllegalArgumentException("DynamicTypes has no composite entry for " + wrapper.canonicalName());
        }
        return spec.optUnwrap();
    }
}
