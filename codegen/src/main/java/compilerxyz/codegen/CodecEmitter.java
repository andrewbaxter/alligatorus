package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.codegen.CodecsGen.Component;
import compilerxyz.codegen.CodecsGen.ParsedType;
import compilerxyz.codegen.CodecsGen.ParsedTypeKind;
import compilerxyz.codegen.CodecsGen.TypeIndex;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import java.io.IOException;

final class CodecEmitter {
    private CodecEmitter() {}

    public static final ClassName JSON_READER = ClassName.get("com.google.gson.stream", "JsonReader");
    public static final ClassName JSON_WRITER = ClassName.get("com.google.gson.stream", "JsonWriter");
    public static final ClassName JSON_VALUE = ClassName.get("compilerxyz.main.safejson", "JsonValue");
    public static final ClassName JSON_VALUES = ClassName.get("compilerxyz.main.safejson", "JsonValues");
    public static final ClassName JSON_VALUE_NULL = ClassName.get("compilerxyz.main.safejson", "JsonValueNull");
    public static final ClassName JSON_VALUE_BOOLEAN = ClassName.get("compilerxyz.main.safejson", "JsonValueBoolean");
    public static final ClassName JSON_VALUE_NUMBER_INT =
            ClassName.get("compilerxyz.main.safejson", "JsonValueNumberInt");
    public static final ClassName JSON_DESERIALIZATION_EXCEPTION =
            ClassName.get("compilerxyz.main.safejson", "Json", "DeserializationException");
    public static final ClassName JSON_VALUE_NUMBER_FLOAT =
            ClassName.get("compilerxyz.main.safejson", "JsonValueNumberFloat");
    public static final ClassName JSON_VALUE_STRING = ClassName.get("compilerxyz.main.safejson", "JsonValueString");
    public static final ClassName JSON_VALUE_ARRAY = ClassName.get("compilerxyz.main.safejson", "JsonValueArray");
    public static final ClassName JSON_VALUE_OBJECT = ClassName.get("compilerxyz.main.safejson", "JsonValueObject");

    public static final ClassName STRING_READER = ClassName.get("java.io", "StringReader");
    public static final ClassName FIELD_PATH = ClassName.get("compilerxyz.main.safejson", "Json", "FieldPath");
    public static final ClassName DESERIALIZATION_EXCEPTION =
            ClassName.get("compilerxyz.main.safejson", "Json", "DeserializationException");
    public static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);
    private static final ClassName BASE64 = ClassName.get("java.util", "Base64");
    public static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    public static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.prelude.util", "TOrderedMap");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    private static final ClassName OPT_SOME = ClassName.get("compilerxyz.prelude.util", "Opt", "Some");
    private static final String ARTIFACT_CODECS_REF = "compilerxyz.main.artifact.ArtifactCodecs";

    record CodecTarget(String klass, Opt<String> castTo) {}

    private static final IMap<String, CodecTarget> HAND_CODED_REFS = IMap.ofEntries(
            new Pair<>(
                    "compilerxyz.main.artifact.Artifact",
                    new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactCodec", Opt.none())),
            new Pair<>(
                    "compilerxyz.main.artifact.Artifact.Body",
                    new CodecTarget(
                            ARTIFACT_CODECS_REF + ".ArtifactCodec",
                            Opt.some("compilerxyz.main.artifact.Artifact.Body"))),
            new Pair<>(
                    "compilerxyz.main.artifact.Artifact.Ref",
                    new CodecTarget(
                            ARTIFACT_CODECS_REF + ".ArtifactCodec",
                            Opt.some("compilerxyz.main.artifact.Artifact.Ref"))));

    public static void resolveType(CodecsGen.ParsedType type, String ctxQname, TypeIndex index) {
        if (type.kind == CodecsGen.ParsedTypeKind.NAMED) {
            var resolved = index.resolve(type.name, ctxQname);
            if (resolved.optIsSome()) type.name = resolved.optUnwrap();
        }
        for (var ta : type.typeArgs) {
            resolveType(ta, ctxQname, index);
        }
    }

    public static CodeBlock encodeComponentValue(Component c) {
        return encodeStmts(c.type, CodeBlock.of("value.$L()", c.name));
    }

    private static CodeBlock encodeStmts(ParsedType type, CodeBlock valueExpr) {
        var b = CodeBlock.builder();
        if (type.kind == ParsedTypeKind.PRIMITIVE) {
            switch (type.name) {
                case "int", "long", "short", "byte" -> b.addStatement("writer.value($L)", valueExpr);
                case "double", "float" -> b.addStatement("writer.value((double) $L)", valueExpr);
                case "boolean" -> b.addStatement("writer.value($L)", valueExpr);
                case "char" -> b.addStatement("writer.value(String.valueOf($L))", valueExpr);
                default -> throw new IllegalStateException("Unsupported primitive: " + type.name);
            }
            return b.build();
        }
        if (type.kind == ParsedTypeKind.BYTE_ARRAY) {
            b.addStatement("writer.value($T.getEncoder().encodeToString($L))", BASE64, valueExpr);
            return b.build();
        }
        var qn = type.name;
        if (qn.equals("TList") || qn.equals("compilerxyz.prelude.util.TList")) {
            var elem = type.typeArgs.isEmpty() ? null : type.typeArgs.get(0);
            b.addStatement("writer.beginArray()");
            b.beginControlFlow("for (var __e : $L)", valueExpr);
            if (elem == null) {
                b.addStatement("writer.nullValue()");
            } else {
                b.add(encodeStmts(elem, CodeBlock.of("__e")));
            }
            b.endControlFlow();
            b.addStatement("writer.endArray()");
            return b.build();
        }
        if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.prelude.util.TOrderedMap")) {
            var keyT = type.typeArgs.size() > 0 ? type.typeArgs.get(0) : null;
            var valT = type.typeArgs.size() > 1 ? type.typeArgs.get(1) : null;
            b.addStatement("writer.beginArray()");
            b.beginControlFlow("for (var __me : $L.entrySet())", valueExpr);
            b.addStatement("writer.beginArray()");
            if (keyT == null) {
                b.addStatement("writer.nullValue()");
            } else {
                b.add(encodeStmts(keyT, CodeBlock.of("__me.getKey()")));
            }
            if (valT == null) {
                b.addStatement("writer.nullValue()");
            } else {
                b.add(encodeStmts(valT, CodeBlock.of("__me.getValue()")));
            }
            b.addStatement("writer.endArray()");
            b.endControlFlow();
            b.addStatement("writer.endArray()");
            return b.build();
        }
        if (qn.equals("Opt") || qn.equals("compilerxyz.prelude.util.Opt")) {
            var elem = type.typeArgs.isEmpty() ? null : type.typeArgs.get(0);
            b.addStatement("writer.beginObject()");
            b.beginControlFlow("if ($L instanceof $T<?> __some)", valueExpr, OPT_SOME);
            b.addStatement("writer.name($S)", "some");
            if (elem != null) {
                var elemJava = javaTypeOf(elem);
                b.addStatement("$L __sv = ($L) __some.value()", elemJava, elemJava);
                b.add(encodeStmts(elem, CodeBlock.of("__sv")));
            } else {
                b.addStatement("writer.nullValue()");
            }
            b.nextControlFlow("else");
            b.addStatement("writer.name($S)", "none");
            b.addStatement("writer.nullValue()");
            b.endControlFlow();
            b.addStatement("writer.endObject()");
            return b.build();
        }
        if (qn.equals("String") || qn.equals("java.lang.String")) {
            b.addStatement("writer.value($L)", valueExpr);
            return b.build();
        }
        if (qn.equals("Integer")
                || qn.equals("java.lang.Integer")
                || qn.equals("Long")
                || qn.equals("java.lang.Long")
                || qn.equals("Short")
                || qn.equals("java.lang.Short")
                || qn.equals("Byte")
                || qn.equals("java.lang.Byte")) {
            b.addStatement("writer.value($L.longValue())", valueExpr);
            return b.build();
        }
        if (qn.equals("Double")
                || qn.equals("java.lang.Double")
                || qn.equals("Float")
                || qn.equals("java.lang.Float")) {
            b.addStatement("writer.value($L.doubleValue())", valueExpr);
            return b.build();
        }
        if (qn.equals("Boolean") || qn.equals("java.lang.Boolean")) {
            b.addStatement("writer.value((boolean) $L)", valueExpr);
            return b.build();
        }

        var codecTarget = codecTargetFor(qn);
        b.addStatement("$L.encode(writer, $L)", codecTarget.klass(), valueExpr);
        return b.build();
    }

    public static CodeBlock decodeStmts(ParsedType type, String readerExpr, CodeBlock pathExpr, String targetVar) {
        var b = CodeBlock.builder();
        if (type.kind == ParsedTypeKind.PRIMITIVE) {
            switch (type.name) {
                case "int" -> b.addStatement("$L = $L.nextInt()", targetVar, readerExpr);
                case "long" -> b.addStatement("$L = $L.nextLong()", targetVar, readerExpr);
                case "short" -> b.addStatement("$L = (short) $L.nextInt()", targetVar, readerExpr);
                case "byte" -> b.addStatement("$L = (byte) $L.nextInt()", targetVar, readerExpr);
                case "double" -> b.addStatement("$L = $L.nextDouble()", targetVar, readerExpr);
                case "float" -> b.addStatement("$L = (float) $L.nextDouble()", targetVar, readerExpr);
                case "boolean" -> b.addStatement("$L = $L.nextBoolean()", targetVar, readerExpr);
                case "char" -> {
                    b.beginControlFlow("");
                    b.addStatement("String __s = $L.nextString()", readerExpr);
                    b.addStatement(
                            "if (__s.length() != 1) throw new $T($S + __s, $L)",
                            DESERIALIZATION_EXCEPTION,
                            "Expected single character, got: ",
                            pathExpr);
                    b.addStatement("$L = __s.charAt(0)", targetVar);
                    b.endControlFlow();
                }
                default -> throw new IllegalStateException("Unsupported primitive: " + type.name);
            }
            return b.build();
        }
        if (type.kind == ParsedTypeKind.BYTE_ARRAY) {
            b.addStatement("$L = $T.getDecoder().decode($L.nextString())", targetVar, BASE64, readerExpr);
            return b.build();
        }
        var qn = type.name;
        if (qn.equals("Opt") || qn.equals("compilerxyz.prelude.util.Opt")) {
            var elem = typeArg(type, 0);
            var elemJava = javaTypeOf(elem);
            b.beginControlFlow("");
            b.addStatement("$L.beginObject()", readerExpr);
            b.addStatement(
                    "if (!$L.hasNext()) throw new $T($S, $L)",
                    readerExpr,
                    DESERIALIZATION_EXCEPTION,
                    "Empty Opt object",
                    pathExpr);
            b.addStatement("String __tag = $L.nextName()", readerExpr);
            b.beginControlFlow("switch (__tag)");
            b.beginControlFlow("case $S ->", "some");
            b.addStatement("$L __sv", elemJava);
            b.add(decodeStmts(elem, readerExpr, CodeBlock.of("$L.field($S)", pathExpr, "some"), "__sv"));
            b.addStatement("$L = $T.some(__sv)", targetVar, OPT);
            b.endControlFlow();
            b.add("case $S -> { $L.nextNull(); $L = $T.none(); }\n", "none", readerExpr, targetVar, OPT);
            b.add(
                    "default -> throw new $T($S + __tag + $S, $L);\n",
                    DESERIALIZATION_EXCEPTION,
                    "Unknown Opt tag '",
                    "'",
                    pathExpr);
            b.endControlFlow();
            b.addStatement("$L.endObject()", readerExpr);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("TList") || qn.equals("compilerxyz.prelude.util.TList")) {
            var elem = typeArg(type, 0);
            var elemType = javaTypeOf(elem);
            b.beginControlFlow("");
            b.addStatement("$T<$L> __l = new $T<>()", TLIST, elemType, TLIST);
            b.addStatement("$L.beginArray()", readerExpr);
            b.addStatement("int __i = 0");
            b.beginControlFlow("while ($L.hasNext())", readerExpr);
            b.addStatement("$L __ev", elemType);
            b.add(decodeStmts(elem, readerExpr, CodeBlock.of("$L.index(__i)", pathExpr), "__ev"));
            b.addStatement("__l.add(__ev)");
            b.addStatement("__i++");
            b.endControlFlow();
            b.addStatement("$L.endArray()", readerExpr);
            b.addStatement("$L = __l", targetVar);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.prelude.util.TOrderedMap")) {
            var keyT = typeArg(type, 0);
            var valT = typeArg(type, 1);
            var keyType = javaTypeOf(keyT);
            var valType = javaTypeOf(valT);
            b.beginControlFlow("");
            b.addStatement("$T<$L, $L> __m = new $T<>()", TORDERED_MAP, keyType, valType, TORDERED_MAP);
            b.addStatement("$L.beginArray()", readerExpr);
            b.addStatement("int __mi = 0");
            b.beginControlFlow("while ($L.hasNext())", readerExpr);
            b.addStatement("$L.beginArray()", readerExpr);
            b.addStatement("$L __mk", keyType);
            b.add(decodeStmts(keyT, readerExpr, CodeBlock.of("$L.index(__mi)", pathExpr), "__mk"));
            b.addStatement("$L __mv", valType);
            b.add(decodeStmts(valT, readerExpr, CodeBlock.of("$L.index(__mi)", pathExpr), "__mv"));
            b.addStatement("$L.endArray()", readerExpr);
            b.addStatement("__m.put(__mk, __mv)");
            b.addStatement("__mi++");
            b.endControlFlow();
            b.addStatement("$L.endArray()", readerExpr);
            b.addStatement("$L = __m", targetVar);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("String") || qn.equals("java.lang.String")) {
            b.addStatement("$L = $L.nextString()", targetVar, readerExpr);
            return b.build();
        }
        if (qn.equals("Integer") || qn.equals("java.lang.Integer")) {
            b.addStatement("$L = $L.nextInt()", targetVar, readerExpr);
            return b.build();
        }
        if (qn.equals("Long") || qn.equals("java.lang.Long")) {
            b.addStatement("$L = $L.nextLong()", targetVar, readerExpr);
            return b.build();
        }
        if (qn.equals("Double") || qn.equals("java.lang.Double")) {
            b.addStatement("$L = $L.nextDouble()", targetVar, readerExpr);
            return b.build();
        }
        if (qn.equals("Float") || qn.equals("java.lang.Float")) {
            b.addStatement("$L = (float) $L.nextDouble()", targetVar, readerExpr);
            return b.build();
        }
        if (qn.equals("Boolean") || qn.equals("java.lang.Boolean")) {
            b.addStatement("$L = $L.nextBoolean()", targetVar, readerExpr);
            return b.build();
        }

        var codecTarget = codecTargetFor(qn);
        if (codecTarget.castTo().optIsSome()) {
            b.addStatement(
                    "$L = ($L) $L.decode($L, $L)",
                    targetVar,
                    codecTarget.castTo().optUnwrap(),
                    codecTarget.klass(),
                    readerExpr,
                    pathExpr);
        } else {
            b.addStatement("$L = $L.decode($L, $L)", targetVar, codecTarget.klass(), readerExpr, pathExpr);
        }
        return b.build();
    }

    public static CodeBlock toValueStmts(ParsedType type, CodeBlock valueExpr, String targetVar) {
        var b = CodeBlock.builder();
        if (type.kind == ParsedTypeKind.PRIMITIVE) {
            switch (type.name) {
                case "int", "long", "short", "byte" ->
                    b.addStatement(
                            "$T $L = new $T((long) $L)", JSON_VALUE, targetVar, JSON_VALUE_NUMBER_INT, valueExpr);
                case "double", "float" ->
                    b.addStatement(
                            "$T $L = new $T((double) $L)", JSON_VALUE, targetVar, JSON_VALUE_NUMBER_FLOAT, valueExpr);
                case "boolean" ->
                    b.addStatement("$T $L = new $T($L)", JSON_VALUE, targetVar, JSON_VALUE_BOOLEAN, valueExpr);
                case "char" ->
                    b.addStatement(
                            "$T $L = new $T(String.valueOf($L))", JSON_VALUE, targetVar, JSON_VALUE_STRING, valueExpr);
                default -> throw new IllegalStateException("Unsupported primitive: " + type.name);
            }
            return b.build();
        }
        if (type.kind == ParsedTypeKind.BYTE_ARRAY) {
            b.addStatement(
                    "$T $L = new $T($T.getEncoder().encodeToString($L))",
                    JSON_VALUE,
                    targetVar,
                    JSON_VALUE_STRING,
                    BASE64,
                    valueExpr);
            return b.build();
        }
        var qn = type.name;
        if (qn.equals("TList") || qn.equals("compilerxyz.prelude.util.TList")) {
            var elem = typeArg(type, 0);
            var arr = targetVar + "_a";
            var el = targetVar + "_e";
            var ev = targetVar + "_ev";
            b.addStatement("$T $L", JSON_VALUE, targetVar);
            b.beginControlFlow("");
            b.addStatement("$T<$T> $L = new $T<>()", TLIST, JSON_VALUE, arr, TLIST);
            b.beginControlFlow("for (var $L : $L)", el, valueExpr);
            b.add(toValueStmts(elem, CodeBlock.of("$L", el), ev));
            b.addStatement("$L.add($L)", arr, ev);
            b.endControlFlow();
            b.addStatement("$L = new $T($L)", targetVar, JSON_VALUE_ARRAY, arr);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.prelude.util.TOrderedMap")) {
            var keyT = typeArg(type, 0);
            var valT = typeArg(type, 1);
            var arr = targetVar + "_a";
            var me = targetVar + "_me";
            var pair = targetVar + "_p";
            var kv = targetVar + "_kv";
            var vv = targetVar + "_vv";
            b.addStatement("$T $L", JSON_VALUE, targetVar);
            b.beginControlFlow("");
            b.addStatement("$T<$T> $L = new $T<>()", TLIST, JSON_VALUE, arr, TLIST);
            b.beginControlFlow("for (var $L : $L.entrySet())", me, valueExpr);
            b.addStatement("$T<$T> $L = new $T<>()", TLIST, JSON_VALUE, pair, TLIST);
            b.add(toValueStmts(keyT, CodeBlock.of("$L.getKey()", me), kv));
            b.addStatement("$L.add($L)", pair, kv);
            b.add(toValueStmts(valT, CodeBlock.of("$L.getValue()", me), vv));
            b.addStatement("$L.add($L)", pair, vv);
            b.addStatement("$L.add(new $T($L))", arr, JSON_VALUE_ARRAY, pair);
            b.endControlFlow();
            b.addStatement("$L = new $T($L)", targetVar, JSON_VALUE_ARRAY, arr);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("Opt") || qn.equals("compilerxyz.prelude.util.Opt")) {
            var elem = typeArg(type, 0);
            var elemJava = javaTypeOf(elem);
            var obj = targetVar + "_o";
            var some = targetVar + "_some";
            var sv = targetVar + "_sv";
            var svv = targetVar + "_svv";
            b.addStatement("$T $L", JSON_VALUE, targetVar);
            b.beginControlFlow("");
            b.addStatement("$T<String, $T> $L = new $T<>()", TORDERED_MAP, JSON_VALUE, obj, TORDERED_MAP);
            b.beginControlFlow("if ($L instanceof $T<?> $L)", valueExpr, OPT_SOME, some);
            b.addStatement("$L $L = ($L) $L.value()", elemJava, sv, elemJava, some);
            b.add(toValueStmts(elem, CodeBlock.of("$L", sv), svv));
            b.addStatement("$L.put($S, $L)", obj, "some", svv);
            b.nextControlFlow("else");
            b.addStatement("$L.put($S, new $T())", obj, "none", JSON_VALUE_NULL);
            b.endControlFlow();
            b.addStatement("$L = new $T($L)", targetVar, JSON_VALUE_OBJECT, obj);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("String") || qn.equals("java.lang.String")) {
            b.addStatement("$T $L = new $T($L)", JSON_VALUE, targetVar, JSON_VALUE_STRING, valueExpr);
            return b.build();
        }
        if (qn.equals("Integer")
                || qn.equals("java.lang.Integer")
                || qn.equals("Long")
                || qn.equals("java.lang.Long")
                || qn.equals("Short")
                || qn.equals("java.lang.Short")
                || qn.equals("Byte")
                || qn.equals("java.lang.Byte")) {
            b.addStatement("$T $L = new $T($L.longValue())", JSON_VALUE, targetVar, JSON_VALUE_NUMBER_INT, valueExpr);
            return b.build();
        }
        if (qn.equals("Double")
                || qn.equals("java.lang.Double")
                || qn.equals("Float")
                || qn.equals("java.lang.Float")) {
            b.addStatement(
                    "$T $L = new $T($L.doubleValue())", JSON_VALUE, targetVar, JSON_VALUE_NUMBER_FLOAT, valueExpr);
            return b.build();
        }
        if (qn.equals("Boolean") || qn.equals("java.lang.Boolean")) {
            b.addStatement("$T $L = new $T((boolean) $L)", JSON_VALUE, targetVar, JSON_VALUE_BOOLEAN, valueExpr);
            return b.build();
        }
        var codecTarget = codecTargetFor(qn);
        b.addStatement("$T $L = $L.toValue($L)", JSON_VALUE, targetVar, codecTarget.klass(), valueExpr);
        return b.build();
    }

    public static CodeBlock fromValueStmts(ParsedType type, CodeBlock jsonExpr, CodeBlock pathExpr, String targetVar) {
        var b = CodeBlock.builder();
        if (type.kind == ParsedTypeKind.PRIMITIVE) {
            switch (type.name) {
                case "int" ->
                    b.addStatement("$L = (int) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
                case "long" ->
                    b.addStatement("$L = (long) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
                case "short" ->
                    b.addStatement("$L = (short) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
                case "byte" ->
                    b.addStatement("$L = (byte) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
                case "double", "float" ->
                    b.addStatement(
                            "$L = ($L) switch ($L) {\n"
                                    + "case $T __n -> __n.value();\n"
                                    + "default -> throw new $T(\"Expected float, got \" + $L.getClass().getSimpleName(), $L);\n"
                                    + "}",
                            targetVar,
                            type.name,
                            jsonExpr,
                            JSON_VALUE_NUMBER_FLOAT,
                            JSON_DESERIALIZATION_EXCEPTION,
                            jsonExpr,
                            pathExpr);
                case "boolean" -> b.addStatement("$L = $T.bool($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
                case "char" -> {
                    var sv = targetVar + "_s";
                    b.beginControlFlow("");
                    b.addStatement("String $L = $T.string($L, $L)", sv, JSON_VALUES, jsonExpr, pathExpr);
                    b.addStatement(
                            "if ($L.length() != 1) throw new $T($S + $L, $L)",
                            sv,
                            DESERIALIZATION_EXCEPTION,
                            "Expected single character, got: ",
                            sv,
                            pathExpr);
                    b.addStatement("$L = $L.charAt(0)", targetVar, sv);
                    b.endControlFlow();
                }
                default -> throw new IllegalStateException("Unsupported primitive: " + type.name);
            }
            return b.build();
        }
        if (type.kind == ParsedTypeKind.BYTE_ARRAY) {
            b.addStatement(
                    "$L = $T.getDecoder().decode($T.string($L, $L))",
                    targetVar,
                    BASE64,
                    JSON_VALUES,
                    jsonExpr,
                    pathExpr);
            return b.build();
        }
        var qn = type.name;
        if (qn.equals("Opt") || qn.equals("compilerxyz.prelude.util.Opt")) {
            var elem = typeArg(type, 0);
            var elemJava = javaTypeOf(elem);
            var obj = targetVar + "_o";
            var en = targetVar + "_en";
            var sv = targetVar + "_sv";
            b.beginControlFlow("");
            b.addStatement("var $L = $T.object($L, $L)", obj, JSON_VALUES, jsonExpr, pathExpr);
            b.addStatement(
                    "if ($L.size() != 1) throw new $T($S, $L)",
                    obj,
                    DESERIALIZATION_EXCEPTION,
                    "Opt object must have exactly one tag",
                    pathExpr);
            b.addStatement("var $L = $L.entrySet().iterator().next()", en, obj);
            b.beginControlFlow("switch ($L.getKey())", en);
            b.beginControlFlow("case $S ->", "some");
            b.addStatement("$L $L", elemJava, sv);
            b.add(fromValueStmts(
                    elem, CodeBlock.of("$L.getValue()", en), CodeBlock.of("$L.field($S)", pathExpr, "some"), sv));
            b.addStatement("$L = $T.some($L)", targetVar, OPT, sv);
            b.endControlFlow();
            b.beginControlFlow("case $S ->", "none");
            b.addStatement(
                    "if (!($L.getValue() instanceof $T)) throw new $T($S, $L)",
                    en,
                    JSON_VALUE_NULL,
                    DESERIALIZATION_EXCEPTION,
                    "Opt none must carry null",
                    pathExpr);
            b.addStatement("$L = $T.none()", targetVar, OPT);
            b.endControlFlow();
            b.add(
                    "default -> throw new $T($S + $L.getKey() + $S, $L);\n",
                    DESERIALIZATION_EXCEPTION,
                    "Unknown Opt tag '",
                    en,
                    "'",
                    pathExpr);
            b.endControlFlow();
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("TList") || qn.equals("compilerxyz.prelude.util.TList")) {
            var elem = typeArg(type, 0);
            var elemType = javaTypeOf(elem);
            var list = targetVar + "_l";
            var idx = targetVar + "_i";
            var je = targetVar + "_je";
            var ev = targetVar + "_ev";
            b.beginControlFlow("");
            b.addStatement("$T<$L> $L = new $T<>()", TLIST, elemType, list, TLIST);
            b.addStatement("int $L = 0", idx);
            b.beginControlFlow("for (var $L : $T.array($L, $L))", je, JSON_VALUES, jsonExpr, pathExpr);
            b.addStatement("$L $L", elemType, ev);
            b.add(fromValueStmts(elem, CodeBlock.of("$L", je), CodeBlock.of("$L.index($L)", pathExpr, idx), ev));
            b.addStatement("$L.add($L)", list, ev);
            b.addStatement("$L++", idx);
            b.endControlFlow();
            b.addStatement("$L = $L", targetVar, list);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.prelude.util.TOrderedMap")) {
            var keyT = typeArg(type, 0);
            var valT = typeArg(type, 1);
            var keyType = javaTypeOf(keyT);
            var valType = javaTypeOf(valT);
            var map = targetVar + "_m";
            var idx = targetVar + "_mi";
            var je = targetVar + "_je";
            var pair = targetVar + "_pair";
            var mk = targetVar + "_mk";
            var mv = targetVar + "_mv";
            b.beginControlFlow("");
            b.addStatement("$T<$L, $L> $L = new $T<>()", TORDERED_MAP, keyType, valType, map, TORDERED_MAP);
            b.addStatement("int $L = 0", idx);
            b.beginControlFlow("for (var $L : $T.array($L, $L))", je, JSON_VALUES, jsonExpr, pathExpr);
            b.addStatement("var $L = $T.array($L, $L.index($L))", pair, JSON_VALUES, je, pathExpr, idx);
            b.addStatement(
                    "if ($L.size() != 2) throw new $T($S, $L.index($L))",
                    pair,
                    DESERIALIZATION_EXCEPTION,
                    "Map entry must be a two element array",
                    pathExpr,
                    idx);
            b.addStatement("$L $L", keyType, mk);
            b.add(fromValueStmts(
                    keyT, CodeBlock.of("$L.get(0)", pair), CodeBlock.of("$L.index($L)", pathExpr, idx), mk));
            b.addStatement("$L $L", valType, mv);
            b.add(fromValueStmts(
                    valT, CodeBlock.of("$L.get(1)", pair), CodeBlock.of("$L.index($L)", pathExpr, idx), mv));
            b.addStatement("$L.put($L, $L)", map, mk, mv);
            b.addStatement("$L++", idx);
            b.endControlFlow();
            b.addStatement("$L = $L", targetVar, map);
            b.endControlFlow();
            return b.build();
        }
        if (qn.equals("String") || qn.equals("java.lang.String")) {
            b.addStatement("$L = $T.string($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
            return b.build();
        }
        if (qn.equals("Integer") || qn.equals("java.lang.Integer")) {
            b.addStatement("$L = (int) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
            return b.build();
        }
        if (qn.equals("Long") || qn.equals("java.lang.Long")) {
            b.addStatement("$L = (long) $T.numberInt($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
            return b.build();
        }
        if (qn.equals("Double") || qn.equals("java.lang.Double")) {
            b.addStatement(
                    "$L = ($L) switch ($L) {\n"
                            + "case $T __n -> __n.value();\n"
                            + "default -> throw new $T(\"Expected float, got \" + $L.getClass().getSimpleName(), $L);\n"
                            + "}",
                    targetVar,
                    "double",
                    jsonExpr,
                    JSON_VALUE_NUMBER_FLOAT,
                    JSON_DESERIALIZATION_EXCEPTION,
                    jsonExpr,
                    pathExpr);
            return b.build();
        }
        if (qn.equals("Float") || qn.equals("java.lang.Float")) {
            b.addStatement(
                    "$L = ($L) switch ($L) {\n"
                            + "case $T __n -> __n.value();\n"
                            + "default -> throw new $T(\"Expected float, got \" + $L.getClass().getSimpleName(), $L);\n"
                            + "}",
                    targetVar,
                    "float",
                    jsonExpr,
                    JSON_VALUE_NUMBER_FLOAT,
                    JSON_DESERIALIZATION_EXCEPTION,
                    jsonExpr,
                    pathExpr);
            return b.build();
        }
        if (qn.equals("Boolean") || qn.equals("java.lang.Boolean")) {
            b.addStatement("$L = $T.bool($L, $L)", targetVar, JSON_VALUES, jsonExpr, pathExpr);
            return b.build();
        }
        var codecTarget = codecTargetFor(qn);
        if (codecTarget.castTo().optIsSome()) {
            b.addStatement(
                    "$L = ($L) $L.fromValue($L, $L)",
                    targetVar,
                    codecTarget.castTo().optUnwrap(),
                    codecTarget.klass(),
                    jsonExpr,
                    pathExpr);
        } else {
            b.addStatement("$L = $L.fromValue($L, $L)", targetVar, codecTarget.klass(), jsonExpr, pathExpr);
        }
        return b.build();
    }

    private static ParsedType typeArg(ParsedType type, int index) {
        if (index >= type.typeArgs.size()) {
            throw new IllegalStateException("codec: raw type " + type.name + " has no type argument " + index);
        }
        return type.typeArgs.get(index);
    }

    static String javaTypeOf(ParsedType type) {
        if (type.kind == ParsedTypeKind.PRIMITIVE) return type.name;
        if (type.kind == ParsedTypeKind.BYTE_ARRAY) return "byte[]";
        var qn = type.name;
        if (type.typeArgs.isEmpty()) return qn;
        var sb = new StringBuilder(qn);
        sb.append("<");
        for (var i = 0; i < type.typeArgs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(javaTypeOf(type.typeArgs.get(i)));
        }
        sb.append(">");
        return sb.toString();
    }

    public static CodecTarget codecTargetFor(String qname) {
        var hand = HAND_CODED_REFS.getOpt(qname);
        if (hand.optIsSome()) return hand.optUnwrap();
        return new CodecTarget(CodecsGen.GEN_PACKAGE + "." + simpleNameOf(qname) + "Codec", Opt.none());
    }

    static String simpleNameOf(String qname) {
        var parts = qname.split("\\.");
        var sb = new StringBuilder();
        var seenType = false;
        for (var p : parts) {
            if (!seenType && !p.isEmpty() && Character.isUpperCase(p.charAt(0))) seenType = true;
            if (seenType) sb.append(p);
        }
        if (sb.length() == 0) return qname;
        return sb.toString();
    }

    public static String lastSegment(String qname) {
        var dot = qname.lastIndexOf('.');
        return dot < 0 ? qname : qname.substring(dot + 1);
    }

    static String toSnakeCase(String camelCase) {
        var result = new StringBuilder();
        for (var i = 0; i < camelCase.length(); i++) {
            var c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
