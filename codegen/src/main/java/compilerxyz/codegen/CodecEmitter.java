package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.CodecsGen.Component;
import compilerxyz.codegen.CodecsGen.ParsedType;
import compilerxyz.codegen.CodecsGen.ParsedTypeKind;
import compilerxyz.codegen.CodecsGen.TypeIndex;
import compilerxyz.codegen.CodecsGen.TypeInfo;
import compilerxyz.codegen.CodecsGen.TypeKind;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedSet;
import java.io.IOException;
import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Map;
import compilerxyz.util.IMap;
import compilerxyz.util.Pair;

final class CodecEmitter {
  private CodecEmitter() {}

  private static final ClassName JSON_READER =
      ClassName.get("com.google.gson.stream", "JsonReader");
  private static final ClassName JSON_TOKEN =
      ClassName.get("com.google.gson.stream", "JsonToken");
  private static final ClassName JSON_WRITER =
      ClassName.get("com.google.gson.stream", "JsonWriter");
  private static final ClassName JSON = ClassName.get("compilerxyz.safejson", "Json");
  private static final ClassName JSON_VALUE = ClassName.get("compilerxyz.safejson", "JsonValue");
  private static final ClassName JSON_VALUES = ClassName.get("compilerxyz.safejson", "JsonValues");
  private static final ClassName STRING_READER = ClassName.get("java.io", "StringReader");
  private static final ClassName STRING_WRITER = ClassName.get("java.io", "StringWriter");
  private static final ClassName JSON_STYLE = ClassName.get("compilerxyz.safejson", "Json", "Style");
  private static final ClassName FIELD_PATH =
      ClassName.get("compilerxyz.safejson", "Json", "FieldPath");
  private static final ClassName DESERIALIZATION_EXCEPTION =
      ClassName.get("compilerxyz.safejson", "Json", "DeserializationException");
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);
  private static final ClassName BASE64 = ClassName.get("java.util", "Base64");
  private static final ClassName TSET = ClassName.get("compilerxyz.util", "TSet");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName OPT_SOME = ClassName.get("compilerxyz.util", "Opt", "Some");
  private static final String ARTIFACT_CODECS_REF = "compilerxyz.artifact.ArtifactCodecs";

  record CodecTarget(String klass, Opt<String> castTo) {}

  private static final IMap<String, CodecTarget> HAND_CODED_REFS =
      IMap.ofEntries(
          new Pair<>(
              "compilerxyz.safejson.JsonValue",
              new CodecTarget("compilerxyz.safejson.JsonValues", Opt.none())),
          new Pair<>(
              "compilerxyz.artifact.Artifact",
              new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactCodec", Opt.none())),
          new Pair<>(
              "compilerxyz.artifact.Artifact.Body",
              new CodecTarget(
                  ARTIFACT_CODECS_REF + ".ArtifactCodec",
                  Opt.some("compilerxyz.artifact.Artifact.Body"))),
          new Pair<>(
              "compilerxyz.artifact.Artifact.Ref",
              new CodecTarget(
                  ARTIFACT_CODECS_REF + ".ArtifactCodec",
                  Opt.some("compilerxyz.artifact.Artifact.Ref"))),
          new Pair<>(
              "compilerxyz.artifact.ArtifactBody",
              new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactBodyCodec", Opt.none())),
          new Pair<>(
              "compilerxyz.artifact.ArtifactIdSource",
              new CodecTarget(ARTIFACT_CODECS_REF + ".ArtifactIdSourceCodec", Opt.none())));
  private static void resolveNames(TypeInfo info, String ctxQname, TypeIndex index) {
    for (var c : info.components) {
      resolveType(c.type, ctxQname, index);
    }
    var resolvedPerms = new TList<String>();
    for (var perm : info.permittedSubclasses) {

      if (perm.contains(".") && index.byQname.containsKey(perm)) {
        resolvedPerms.add(perm);
      } else {
        var resolved = index.resolve(perm, ctxQname);
        resolvedPerms.add(resolved.optUnwrapOr(perm));
      }
    }
    info.permittedSubclasses.clear();
    info.permittedSubclasses.addAll(resolvedPerms);
  }

  private static void resolveType(
      CodecsGen.ParsedType type, String ctxQname, TypeIndex index) {
    if (type.kind == CodecsGen.ParsedTypeKind.NAMED) {
      var resolved = index.resolve(type.name, ctxQname);
      if (resolved.optIsSome()) type.name = resolved.optUnwrap();
    }
    for (var ta : type.typeArgs) {
      resolveType(ta, ctxQname, index);
    }
  }

  static String emit(String qname, TypeInfo info, TypeIndex index) {
    resolveNames(info, qname, index);
    var simpleName = simpleNameOf(qname);
    var codecClassName = simpleName + "Codec";
    var target = classNameOf(qname);

    var cls =
        TypeSpec.classBuilder(codecClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    if (info.kind == TypeKind.RECORD) {
      cls.addMethod(buildRecordEncode(qname, target, info));
      cls.addMethod(buildRecordDecode(qname, target, info));
    } else if (info.kind == TypeKind.SEALED) {
      cls.addMethod(buildSealedEncode(qname, target, info));
      cls.addMethod(buildSealedDecode(qname, target, info));
    } else if (info.kind == TypeKind.ENUM) {
      cls.addMethod(buildEnumEncode(target));
      cls.addMethod(buildEnumDecode(qname, target));
    } else {
      throw new IllegalStateException("Unsupported kind: " + info.kind + " for " + qname);
    }

    if (info.implementsMarker) {
      cls.addMethod(buildToValue(target));
      cls.addMethod(buildFromValue(target));
      cls.addMethod(buildDeserialize(target));
    }

    var javaFile =
        JavaFile.builder(CodecsGen.GEN_PACKAGE, cls.build()).skipJavaLangImports(true).build();
    return javaFile.toString();
  }

  private static MethodSpec buildToValue(TypeName target) {
    var body = CodeBlock.builder();
    body.beginControlFlow("try");
    body.addStatement("$T __sw = new $T()", STRING_WRITER, STRING_WRITER);
    body.addStatement("$T __jw = new $T(__sw)", JSON_WRITER, JSON_WRITER);
    body.addStatement("__jw.setSerializeNulls(true)");
    body.addStatement("__jw.setIndent($S)", "");
    body.addStatement("encode(__jw, value, $T.COMPACT)", JSON_STYLE);
    body.addStatement("__jw.close()");
    body.addStatement("return $T.parse(__sw.toString())", JSON_VALUES);
    body.nextControlFlow("catch ($T __e)", IO_EXCEPTION);
    body.addStatement("throw new AssertionError($S, __e)", "In-memory IO failed");
    body.endControlFlow();
    return MethodSpec.methodBuilder("toValue")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(JSON_VALUE)
        .addParameter(target, "value")
        .addCode(body.build())
        .build();
  }

  private static MethodSpec buildFromValue(TypeName target) {
    var body = CodeBlock.builder();
    body.beginControlFlow("try");
    body.addStatement("return deserialize($T.serialize(value))", JSON_VALUES);
    body.nextControlFlow("catch ($T __e)", IO_EXCEPTION);
    body.addStatement("throw new AssertionError($S, __e)", "In-memory IO failed");
    body.endControlFlow();
    return MethodSpec.methodBuilder("fromValue")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(target)
        .addParameter(JSON_VALUE, "value")
        .addCode(body.build())
        .build();
  }

  private static MethodSpec buildDeserialize(TypeName target) {
    var body = CodeBlock.builder();
    body.addStatement("$T __reader = new $T(new $T(json))", JSON_READER, JSON_READER, STRING_READER);
    body.addStatement("return decode(__reader, $T.root())", FIELD_PATH);
    return MethodSpec.methodBuilder("deserialize")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(target)
        .addParameter(String.class, "json")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
  }

  private static MethodSpec buildRecordEncode(String qname, TypeName target, TypeInfo info) {
    var body = CodeBlock.builder();
    body.addStatement("writer.beginObject()");
    body.beginControlFlow("if (style == $T.CANONICAL)", JSON_STYLE);
    for (var c : sortedBySnake(info.components)) {
      body.add(encodeFieldBlock(c));
    }
    body.nextControlFlow("else");
    for (var c : info.components) {
      body.add(encodeFieldBlock(c));
    }
    body.endControlFlow();
    body.addStatement("writer.endObject()");
    return MethodSpec.methodBuilder("encode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(void.class)
        .addParameter(JSON_WRITER, "writer")
        .addParameter(target, "value")
        .addParameter(JSON_STYLE, "style")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
  }

  private static CodeBlock encodeFieldBlock(Component c) {
    var key = toSnakeCase(c.name);
    var b = CodeBlock.builder();
    b.addStatement("writer.name($S)", key);
    b.add(encodeStmts(c.type, CodeBlock.of("value.$L()", c.name)));
    return b.build();
  }

  private static CodeBlock encodeStmts(ParsedType type, CodeBlock valueExpr) {
    var b = CodeBlock.builder();
    if (type.nullable) {
      b.beginControlFlow("if ($L == null)", valueExpr);
      b.addStatement("writer.nullValue()");
      b.nextControlFlow("else");
      var nonNullType = new ParsedType(type.kind, type.name, type.typeArgs, false);
      b.add(encodeStmts(nonNullType, valueExpr));
      b.endControlFlow();
      return b.build();
    }
    if (type.kind == ParsedTypeKind.PRIMITIVE) {
      switch (type.name) {
        case "int", "long", "short", "byte" ->
            b.addStatement("writer.value($L)", valueExpr);
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
    if (qn.equals("TList") || qn.equals("compilerxyz.util.TList")) {
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
    if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.util.TOrderedMap")) {
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
    if (qn.equals("Opt") || qn.equals("compilerxyz.util.Opt")) {
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
    if (qn.equals("Integer") || qn.equals("java.lang.Integer")
        || qn.equals("Long") || qn.equals("java.lang.Long")
        || qn.equals("Short") || qn.equals("java.lang.Short")
        || qn.equals("Byte") || qn.equals("java.lang.Byte")) {
      b.addStatement("writer.value($L.longValue())", valueExpr);
      return b.build();
    }
    if (qn.equals("Double") || qn.equals("java.lang.Double")
        || qn.equals("Float") || qn.equals("java.lang.Float")) {
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

  private static MethodSpec buildRecordDecode(String qname, TypeName target, TypeInfo info) {
    var body = CodeBlock.builder();
    for (var c : info.components) {
      body.addStatement("$L __$L = $L", javaTypeOf(c.type), c.name, defaultValueExpr(c.type));
    }
    body.addStatement("$T<String> __seen = new $T<>()", TSET, TSET);
    body.addStatement("reader.beginObject()");
    body.beginControlFlow("while (reader.hasNext())");
    body.addStatement("String __fname = reader.nextName()");
    body.beginControlFlow("switch (__fname)");
    for (var c : info.components) {
      var key = toSnakeCase(c.name);
      body.beginControlFlow("case $S ->", key);
      body.addStatement("__seen.add($S)", key);
      body.addStatement("$T __p = path.field($S)", FIELD_PATH, key);
      body.add(decodeStmts(c.type, "reader", CodeBlock.of("__p"), "__" + c.name));
      body.endControlFlow();
    }
    body.add(
        "default -> throw new $T($S + __fname + $S, path.field(__fname));\n",
        DESERIALIZATION_EXCEPTION,
        "Unknown field '",
        "' in " + qname);
    body.endControlFlow();
    body.endControlFlow();
    body.addStatement("reader.endObject()");
    for (var c : info.components) {
      if (!c.type.nullable) {
        var key = toSnakeCase(c.name);
        body.addStatement(
            "if (!__seen.contains($S)) throw new $T($S, path.field($S))",
            key,
            DESERIALIZATION_EXCEPTION,
            "Missing required field '" + c.name + "'",
            key);
      }
    }
    var ctorArgs = CodeBlock.builder();
    for (var i = 0; i < info.components.size(); i++) {
      if (i > 0) ctorArgs.add(", ");
      ctorArgs.add("__$L", info.components.get(i).name);
    }
    body.addStatement("return new $L($L)", qname, ctorArgs.build());
    return MethodSpec.methodBuilder("decode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(target)
        .addParameter(JSON_READER, "reader")
        .addParameter(FIELD_PATH, "path")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
  }

  private static CodeBlock decodeStmts(
      ParsedType type, String readerExpr, CodeBlock pathExpr, String targetVar) {
    var b = CodeBlock.builder();
    if (type.nullable) {
      b.beginControlFlow("if ($L.peek() == $T.NULL)", readerExpr, JSON_TOKEN);
      b.addStatement("$L.nextNull()", readerExpr);
      b.addStatement("$L = null", targetVar);
      b.nextControlFlow("else");
      var nonNull = new ParsedType(type.kind, type.name, type.typeArgs, false);
      b.add(decodeStmts(nonNull, readerExpr, pathExpr, targetVar));
      b.endControlFlow();
      return b.build();
    }
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
      b.addStatement(
          "$L = $T.getDecoder().decode($L.nextString())", targetVar, BASE64, readerExpr);
      return b.build();
    }
    var qn = type.name;
    if (qn.equals("Opt") || qn.equals("compilerxyz.util.Opt")) {
      var elem = type.typeArgs.isEmpty() ? null : type.typeArgs.get(0);
      var elemJava = elem == null ? "Object" : javaTypeOf(elem);
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
      if (elem != null) {
        b.addStatement("$L __sv = $L", elemJava, defaultForNamed(elem));
        b.add(
            decodeStmts(
                elem,
                readerExpr,
                CodeBlock.of("$L.field($S)", pathExpr, "some"),
                "__sv"));
        b.addStatement("$L = $T.some(__sv)", targetVar, OPT);
      } else {
        b.addStatement("$L.skipValue()", readerExpr);
        b.addStatement("$L = $T.none()", targetVar, OPT);
      }
      b.endControlFlow();
      b.add(
          "case $S -> { $L.nextNull(); $L = $T.none(); }\n",
          "none",
          readerExpr,
          targetVar,
          OPT);
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
    if (qn.equals("TList") || qn.equals("compilerxyz.util.TList")) {
      var elem = type.typeArgs.isEmpty() ? null : type.typeArgs.get(0);
      var elemType = elem == null ? "Object" : javaTypeOf(elem);
      b.beginControlFlow("");
      b.addStatement("$T<$L> __l = new $T<>()", TLIST, elemType, TLIST);
      b.addStatement("$L.beginArray()", readerExpr);
      b.addStatement("int __i = 0");
      b.beginControlFlow("while ($L.hasNext())", readerExpr);
      b.addStatement("$L __ev = $L", elemType, defaultForNamed(elem));
      if (elem != null) {
        b.add(
            decodeStmts(
                elem, readerExpr, CodeBlock.of("$L.index(__i)", pathExpr), "__ev"));
      } else {
        b.addStatement("$L.skipValue()", readerExpr);
      }
      b.addStatement("__l.add(__ev)");
      b.addStatement("__i++");
      b.endControlFlow();
      b.addStatement("$L.endArray()", readerExpr);
      b.addStatement("$L = __l", targetVar);
      b.endControlFlow();
      return b.build();
    }
    if (qn.equals("TOrderedMap") || qn.equals("compilerxyz.util.TOrderedMap")) {
      var keyT = type.typeArgs.size() > 0 ? type.typeArgs.get(0) : null;
      var valT = type.typeArgs.size() > 1 ? type.typeArgs.get(1) : null;
      var keyType = keyT == null ? "Object" : javaTypeOf(keyT);
      var valType = valT == null ? "Object" : javaTypeOf(valT);
      b.beginControlFlow("");
      b.addStatement("$T<$L, $L> __m = new $T<>()", TORDERED_MAP, keyType, valType, TORDERED_MAP);
      b.addStatement("$L.beginArray()", readerExpr);
      b.addStatement("int __mi = 0");
      b.beginControlFlow("while ($L.hasNext())", readerExpr);
      b.addStatement("$L.beginArray()", readerExpr);
      b.addStatement("$L __mk = $L", keyType, defaultForNamed(keyT));
      if (keyT != null) {
        b.add(decodeStmts(keyT, readerExpr, CodeBlock.of("$L.index(__mi)", pathExpr), "__mk"));
      } else {
        b.addStatement("$L.skipValue()", readerExpr);
      }
      b.addStatement("$L __mv = $L", valType, defaultForNamed(valT));
      if (valT != null) {
        b.add(decodeStmts(valT, readerExpr, CodeBlock.of("$L.index(__mi)", pathExpr), "__mv"));
      } else {
        b.addStatement("$L.skipValue()", readerExpr);
      }
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
      b.addStatement(
          "$L = $L.decode($L, $L)", targetVar, codecTarget.klass(), readerExpr, pathExpr);
    }
    return b.build();
  }

  private static String defaultForNamed(ParsedType elem) {
    if (elem == null) return "null";
    if (elem.kind == ParsedTypeKind.PRIMITIVE) {
      return defaultValueExpr(elem);
    }
    return "null";
  }

  private static MethodSpec buildSealedEncode(String qname, TypeName target, TypeInfo info) {
    var body = CodeBlock.builder();
    body.addStatement("writer.beginObject()");
    for (var perm : info.permittedSubclasses) {
      var key = toSnakeCase(lastSegment(perm));
      body.beginControlFlow("if (value instanceof $L __v)", perm);
      body.addStatement("writer.name($S)", key);
      body.addStatement("$L.encode(writer, __v, style)", codecTargetFor(perm).klass());
      body.nextControlFlow("else");
    }
    body.addStatement(
        "throw new IllegalStateException($S + value.getClass())",
        "Unhandled variant of " + qname + ": ");
    for (var i = 0; i < info.permittedSubclasses.size(); i++) {
      body.endControlFlow();
    }
    body.addStatement("writer.endObject()");
    return MethodSpec.methodBuilder("encode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(void.class)
        .addParameter(JSON_WRITER, "writer")
        .addParameter(target, "value")
        .addParameter(JSON_STYLE, "style")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
  }

  private static MethodSpec buildSealedDecode(String qname, TypeName target, TypeInfo info) {
    var body = CodeBlock.builder();
    body.addStatement("reader.beginObject()");
    body.addStatement(
        "if (!reader.hasNext()) throw new $T($S, path)",
        DESERIALIZATION_EXCEPTION,
        "Empty object for sealed " + qname);
    body.addStatement("String __tag = reader.nextName()");
    body.addStatement("$L __out", qname);
    body.beginControlFlow("switch (__tag)");
    for (var perm : info.permittedSubclasses) {
      var key = toSnakeCase(lastSegment(perm));
      body.add(
          "case $S -> __out = $L.decode(reader, path.field($S));\n",
          key,
          codecTargetFor(perm).klass(),
          key);
    }
    body.add(
        "default -> throw new $T($S + __tag + $S, path);\n",
        DESERIALIZATION_EXCEPTION,
        "Unknown variant '",
        "' for " + qname);
    body.endControlFlow();
    body.addStatement(
        "if (reader.hasNext()) throw new $T($S, path)",
        DESERIALIZATION_EXCEPTION,
        "Sealed " + qname + " must have exactly one variant");
    body.addStatement("reader.endObject()");
    body.addStatement("return __out");
    return MethodSpec.methodBuilder("decode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(target)
        .addParameter(JSON_READER, "reader")
        .addParameter(FIELD_PATH, "path")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
  }

  private static MethodSpec buildEnumEncode(TypeName target) {
    return MethodSpec.methodBuilder("encode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(void.class)
        .addParameter(JSON_WRITER, "writer")
        .addParameter(target, "value")
        .addParameter(JSON_STYLE, "style")
        .addException(IO_EXCEPTION)
        .addStatement("writer.value(value.name().toLowerCase(java.util.Locale.ROOT))")
        .build();
  }

  private static MethodSpec buildEnumDecode(String qname, TypeName target) {
    var body = CodeBlock.builder();
    body.addStatement("String __s = reader.nextString()");
    body.addStatement("String __u = __s.toUpperCase(java.util.Locale.ROOT)");
    body.beginControlFlow("for ($L __c : $L.values())", qname, qname);
    body.addStatement("if (__c.name().equals(__u)) return __c");
    body.endControlFlow();
    body.addStatement(
        "throw new $T($S + __s + $S, path)",
        DESERIALIZATION_EXCEPTION,
        "Unknown enum constant '",
        "' for " + qname);
    return MethodSpec.methodBuilder("decode")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(target)
        .addParameter(JSON_READER, "reader")
        .addParameter(FIELD_PATH, "path")
        .addException(IO_EXCEPTION)
        .addCode(body.build())
        .build();
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

  static String defaultValueExpr(ParsedType type) {
    if (type.kind == ParsedTypeKind.PRIMITIVE) {
      return switch (type.name) {
        case "boolean" -> "false";
        case "char" -> "'\\u0000'";
        case "float" -> "0f";
        case "double" -> "0d";
        case "long" -> "0L";
        default -> "0";
      };
    }
    return "null";
  }

  private static CodecTarget codecTargetFor(String qname) {
    var hand = HAND_CODED_REFS.getOrNull(qname);
    if (hand != null) return hand;
    return new CodecTarget(
        CodecsGen.GEN_PACKAGE + "." + simpleNameOf(qname) + "Codec", Opt.none());
  }

  private static TypeName classNameOf(String qname) {
    var lastDot = qname.lastIndexOf('.');
    if (lastDot < 0) {
      return ClassName.bestGuess(qname);
    }
    var pkg = qname.substring(0, lastDot);
    var rest = qname.substring(lastDot + 1);
    var parts = rest.split("\\.");
    return ClassName.get(pkg, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
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

  private static String lastSegment(String qname) {
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

  private static TList<Component> sortedBySnake(TList<Component> cs) {
    var out = TList.from(cs);
    out.sort((a, b) -> toSnakeCase(a.name).compareTo(toSnakeCase(b.name)));
    return out;
  }

}
