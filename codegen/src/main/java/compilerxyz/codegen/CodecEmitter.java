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
    public static final ClassName JSON = ClassName.get("compilerxyz.main.safejson", "Json");
    public static final ClassName JSON_VALUE = ClassName.get("compilerxyz.main.safejson", "JsonValue");
    public static final ClassName JSON_VALUES = ClassName.get("compilerxyz.main.safejson", "JsonValues");
    public static final ClassName STRING_READER = ClassName.get("java.io", "StringReader");
    public static final ClassName STRING_WRITER = ClassName.get("java.io", "StringWriter");
    public static final ClassName JSON_STYLE = ClassName.get("compilerxyz.main.safejson", "Json", "Style");
    public static final ClassName FIELD_PATH = ClassName.get("compilerxyz.main.safejson", "Json", "FieldPath");
    public static final ClassName DESERIALIZATION_EXCEPTION =
            ClassName.get("compilerxyz.main.safejson", "Json", "DeserializationException");
    public static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);
    private static final ClassName BASE64 = ClassName.get("java.util", "Base64");
    private static final ClassName TLIST = ClassName.get("compilerxyz.prelude.util", "TList");
    private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.prelude.util", "TOrderedMap");
    public static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");
    private static final ClassName OPT_SOME = ClassName.get("compilerxyz.prelude.util", "Opt", "Some");
    private static final String ARTIFACT_CODECS_REF = "compilerxyz.main.artifact.ArtifactCodecs";

    record CodecTarget(String klass, Opt<String> castTo) {}

    private static final IMap<String, CodecTarget> HAND_CODED_REFS = IMap.ofEntries(
            new Pair<>(
                    "compilerxyz.main.safejson.JsonValue",
                    new CodecTarget("compilerxyz.main.safejson.JsonValues", Opt.none())),
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
                            Opt.some("compilerxyz.main.artifact.Artifact.Ref"))),
            new Pair<>(
                    "compilerxyz.main.artifact.ArtifactBody",
                    new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactBodyCodec", Opt.none())),
            new Pair<>(
                    "compilerxyz.main.artifact.ArtifactIdSource",
                    new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactIdSourceCodec", Opt.none())));

    public static void resolveType(CodecsGen.ParsedType type, String ctxQname, TypeIndex index) {
        if (type.kind == CodecsGen.ParsedTypeKind.NAMED) {
            var resolved = index.resolve(type.name, ctxQname);
            if (resolved.optIsSome()) type.name = resolved.optUnwrap();
        }
        for (var ta : type.typeArgs) {
            resolveType(ta, ctxQname, index);
        }
    }

    public static CodeBlock encodeFieldBlock(Component c) {
        var key = toSnakeCase(c.name);
        var b = CodeBlock.builder();
        b.addStatement("writer.name($S)", key);
        b.add(encodeStmts(c.type, CodeBlock.of("value.$L()", c.name)));
        return b.build();
    }

    private static CodeBlock encodeStmts(ParsedType type, CodeBlock valueExpr) {
        var b = CodeBlock.builder();
        if (type.kind == ParsedTypeKind.PRIMITIVE) {
            switch (type.name) {
                case "int", "long", "short", "byte" -> b.addStatement("writer.value($L)", valueExpr);
                case "double", "float" ->
                    b.addStatement("$T.serializeDouble(writer, (double) $L, style)", JSON, valueExpr);
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
            b.addStatement("$T.serializeDouble(writer, $L.doubleValue(), style)", JSON, valueExpr);
            return b.build();
        }
        if (qn.equals("Boolean") || qn.equals("java.lang.Boolean")) {
            b.addStatement("writer.value((boolean) $L)", valueExpr);
            return b.build();
        }

        var codecTarget = codecTargetFor(qn);
        b.addStatement("$L.encode(writer, $L, style)", codecTarget.klass(), valueExpr);
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
