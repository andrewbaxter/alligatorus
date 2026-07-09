package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import javax.lang.model.element.Modifier;

public final class InterfacesGen {
  private InterfacesGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName SOURCE_INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName SOURCE_INTERFACE_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceMethodData");
  private static final ClassName BUILTIN_INTERFACE_KIND =
      ClassName.get("compilerxyz.eval.value.source_interface", "BuiltinInterfaceKind");

  private static final ClassName DEFINITION_SET = ClassName.get("compilerxyz.eval", "DefinitionSet");
  private static final ClassName DEF_SET_ID = ClassName.get("compilerxyz.eval", "DefSetId");
  private static final ClassName COMPILE_ID = ClassName.get("compilerxyz.eval", "CompileId");
  private static final ClassName SOURCE_ID = ClassName.get("compilerxyz.eval", "SourceId");
  private static final ClassName IMPORT_STATE_ID =
      ClassName.get("compilerxyz.eval", "ImportStateId");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR =
      ClassName.get("compilerxyz.eval", "ConstKey", "Str");

  private static final ClassName RECORD = ClassName.get("compilerxyz.eval.value.record", "Record");
  private static final ClassName RECORD_FIELD =
      ClassName.get("compilerxyz.eval.value.record", "RecordField");
  private static final ClassName RECORD_FIELD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
  private static final ClassName RECORD_BASE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordBaseDynamic");

  private static final ClassName TUPLE = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
  private static final ClassName TUPLE_ELEMENT =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement");
  private static final ClassName TUPLE_ELEMENT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement", "Dynamic");

  private static final ClassName TYPE_ANY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.any", "TypeAnyDynamic");

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");

  public static void run(
      Path outRoot,
      Formatter formatter,
      TList<InterfaceSpec> specs,
      TSet<Long> builtinUniqueIds,
      TSet<Long> scopeIds)
      throws IOException {
    validate(specs, builtinUniqueIds, scopeIds);
    writeHolder(outRoot, formatter, specs);
    for (var spec : specs) {
      if (spec.javaTraitName() != null) {
        writeTrait(outRoot, formatter, spec);
      }
    }
  }

  private static void validate(
      TList<InterfaceSpec> specs, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds) {
    var names = new TSet<String>();
    var prefixes = new TSet<String>();
    var kinds = new TSet<String>();
    for (var spec : specs) {
      if (!names.add(spec.name())) {
        throw new IllegalStateException("duplicate interface name: " + spec.name());
      }
      if (!prefixes.add(spec.fieldPrefix())) {
        throw new IllegalStateException("duplicate field_prefix: " + spec.fieldPrefix());
      }
      if (!kinds.add(spec.kind())) {
        throw new IllegalStateException("duplicate kind: " + spec.kind());
      }
      if (!builtinUniqueIds.add((long) spec.builtinUniqueId())) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId() + " (in interfaces)");
      }
      if (!scopeIds.add((long) spec.scopeId())) {
        throw new IllegalStateException(
            "duplicate scope_id " + spec.scopeId() + " (in interfaces)");
      }
      var methodNames = new TSet<String>();
      for (var m : spec.methods()) {
        if (!methodNames.add(m.name())) {
          throw new IllegalStateException(
              "duplicate method name on iface " + spec.name() + ": " + m.name());
        }
      }
    }
  }

  private static void writeHolder(Path outRoot, Formatter formatter, TList<InterfaceSpec> specs)
      throws IOException {
      var holder =
              TypeSpec.classBuilder("GenBuiltinInterfaces")
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    for (var spec : specs) {
      holder.addField(
              FieldSpec.builder(
                              SOURCE_INTERFACE_DATA,
                              fieldNameForSpec(spec),
                              Modifier.PUBLIC,
                              Modifier.FINAL)
                      .build());

      var ifaceUpper = upper(spec.name());
      for (var i = 0; i < spec.methods().size(); i++) {
        var m = spec.methods().get(i);
        var fieldName = "_im_" + spec.fieldPrefix() + i;
        var constName = "IM_" + ifaceUpper + "_" + upper(m.name()) + "_FIELD";
        holder.addField(
                FieldSpec.builder(
                                String.class,
                                constName,
                                Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL)
                        .initializer("$S", fieldName)
                        .build());
      }

      holder.addMethod(
              MethodSpec.methodBuilder(identityMethodName(spec))
                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                      .returns(UNIQUE)
                      .addStatement(
                              "return new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId())
                      .build());
    }

    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);
    for (var spec : specs) {
      var fname = fieldNameForSpec(spec);
      ctor.addParameter(ParameterSpec.builder(SOURCE_INTERFACE_DATA, fname).build());
      ctor.addStatement("this.$N = $N", fname, fname);
    }
    holder.addMethod(ctor.build());

    holder.addMethod(buildFactory(specs));

    var file = JavaFile.builder(GEN_PACKAGE, holder.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static MethodSpec buildFactory(TList<InterfaceSpec> specs) {
      var build =
              MethodSpec.methodBuilder("build")
                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                      .returns(ClassName.get(GEN_PACKAGE, "GenBuiltinInterfaces"))
                      .addParameter(ParameterSpec.builder(IMPORT_STATE_ID, "importState").build());

    build.addStatement(
        "var builtinDefSet = new $T(new $T(new $T(importState, new $T(Long.MAX_VALUE)),"
            + " Long.MAX_VALUE))",
        DEFINITION_SET,
        DEF_SET_ID,
        COMPILE_ID,
        SOURCE_ID);

    var ctorArgs = new TList<String>();
    for (var spec : specs) {
      var fname = fieldNameForSpec(spec);
      ctorArgs.add(fname);
      build.addStatement(
          "var $N = new $T(builtinDefSet.defSetId, new $T($LL), $T.$L, builtinDefSet)",
          fname,
          SOURCE_INTERFACE_DATA,
          UNIQUE_BUILTIN,
          spec.builtinUniqueId(),
          BUILTIN_INTERFACE_KIND,
          camelToScreamingSnake(spec.kind()));
      var description = spec.description() != null ? spec.description() : spec.name();
      build.addStatement("$N.description = $S", fname, description);

      for (var i = 0; i < spec.methods().size(); i++) {
        var m = spec.methods().get(i);
        var fieldName = "_im_" + spec.fieldPrefix() + i;
        var methodVar = fname + "Method" + i;
        build.addStatement(
            "var $N = new $T($S, $S)",
            methodVar,
            SOURCE_INTERFACE_METHOD_DATA,
            m.name(),
            fieldName);

        for (var dfn : dataFieldNames(m)) {
          build.addStatement("$N.dataFieldNames.add($S)", methodVar, dfn);
        }

        switch (m.args()) {
          case MethodArgs.Source src -> {
            build.addStatement(
                "$N.argType = new $T()", methodVar, TYPE_ANY_DYNAMIC);
            build.addCode(buildSourceArgRecordType(methodVar, src));
          }
          case MethodArgs.Rust rs -> {
            var dyns = new TList<CodeBlock>();
            for (var p : rs.sig().context()) dyns.add(p.type().dynamicType());
            for (var p : rs.sig().data()) dyns.add(p.type().dynamicType());
            build.addStatement(
                "$N.argType = $L", methodVar, argDynamicTypeExpr(dyns));
            build.addStatement("$N.argRecordType = null", methodVar);
          }
        }

        build.addStatement(
            "$N.retType = $L", methodVar, asTypeExpr(m.retType().dynamicType()));

        build.addStatement("$N.methods.add($N)", fname, methodVar);
      }
      build.addStatement("builtinDefSet.interfaces.add($N)", fname);
    }

    var ctorCall = new StringBuilder("return new $T(");
    for (var i = 0; i < ctorArgs.size(); i++) {
      if (i > 0) ctorCall.append(", ");
      ctorCall.append("$N");
    }
    ctorCall.append(")");
    var args = new TList<Object>();
    args.add(ClassName.get(GEN_PACKAGE, "GenBuiltinInterfaces"));
    args.addAll(ctorArgs);
    build.addStatement(ctorCall.toString(), args.toArray());

    return build.build();
  }

  private static CodeBlock buildSourceArgRecordType(String methodVar, MethodArgs.Source src) {
    var b = CodeBlock.builder();
    var mapVar = methodVar + "ArgFields";
    b.addStatement(
        "var $N = new $T<$T, $T>()", mapVar, TORDERED_MAP, CONST_KEY, RECORD_FIELD);
    for (var field : src.fields()) {
      b.addStatement(
          "$N.put(new $T($S), new $T($L))",
          mapVar,
          CONST_KEY_STR,
          field.name(),
          RECORD_FIELD_DYNAMIC,
          asTypeExpr(field.type().dynamicType()));
    }
    b.addStatement(
        "$N.argRecordType = new $T($T.none(), $N)",
        methodVar,
        RECORD,
        OPT,
        mapVar);
    return b.build();
  }

  private static CodeBlock argDynamicTypeExpr(TList<CodeBlock> dyns) {
    if (dyns.size() == 1) {
      return asTypeExpr(dyns.get(0));
    }
    var elements = CodeBlock.builder();
    elements.add("$T.<$T>of(", TLIST, TUPLE_ELEMENT);
    for (var i = 0; i < dyns.size(); i++) {
      if (i > 0) elements.add(", ");
      elements.add("new $T($L)", TUPLE_ELEMENT_DYNAMIC, asTypeExpr(dyns.get(i)));
    }
    elements.add(")");
    return CodeBlock.of("new $T($T.none(), $L)", TUPLE, OPT, elements.build());
  }

  private static CodeBlock asTypeExpr(CodeBlock dyn) {
    return CodeBlock.of("($T) $L", TYPE, dyn);
  }

  private static TList<String> dataFieldNames(Method m) {
    var out = new TList<String>();
    switch (m.args()) {
      case MethodArgs.Source src -> {
        for (var f : src.fields()) out.add(stripTrailingUnderscore(f.name()));
      }
      case MethodArgs.Rust rs -> {
        for (var p : rs.sig().context()) out.add(stripTrailingUnderscore(p.name()));
        for (var p : rs.sig().data()) out.add(stripTrailingUnderscore(p.name()));
      }
    }
    return out;
  }

  private static void writeTrait(Path outRoot, Formatter formatter, InterfaceSpec spec)
      throws IOException {
      var trait =
              TypeSpec.interfaceBuilder(spec.javaTraitName())
                      .addModifiers(Modifier.PUBLIC);

    for (var m : spec.methods()) {
      switch (m.args()) {
        case MethodArgs.Source ignored -> {}
        case MethodArgs.Rust rs -> {
          var method =
              MethodSpec.methodBuilder(javaMethodName(m.name()))
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                  .returns(m.retType().path());
          for (var p : rs.sig().context()) {
            method.addParameter(ParameterSpec.builder(p.type().path(), p.name()).build());
          }
          for (var p : rs.sig().data()) {
            method.addParameter(ParameterSpec.builder(p.type().path(), p.name()).build());
          }
          trait.addMethod(method.build());
        }
      }
    }

    if (spec.javaDynamicTypeClass() != null) {
      trait.addMethod(
              MethodSpec.methodBuilder("dynamicType")
                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                      .returns(TYPE)
                      .addStatement("return new $T()", spec.javaDynamicTypeClass())
                      .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, trait.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static String fieldNameForSpec(InterfaceSpec spec) {
    return escapeJavaKeyword(spec.name().replace('-', '_'));
  }

  private static String identityMethodName(InterfaceSpec spec) {
    return "builtin" + camelUpper(spec.name()) + "IfaceIdentity";
  }

  private static String camelUpper(String name) {
    var out = new StringBuilder();
    var nextUpper = true;
    for (var c : name.toCharArray()) {
      if (c == '-' || c == '_') {
        nextUpper = true;
        continue;
      }
      out.append(nextUpper ? Character.toUpperCase(c) : c);
      nextUpper = false;
    }
    return out.toString();
  }

  private static String javaMethodName(String name) {
    return escapeJavaKeyword(stripTrailingUnderscore(name));
  }

  private static String stripTrailingUnderscore(String s) {
    var i = s.length();
    while (i > 0 && s.charAt(i - 1) == '_') i--;
    return s.substring(0, i);
  }

  private static String upper(String s) {
    return stripTrailingUnderscore(s).toUpperCase(Locale.ROOT).replace('-', '_');
  }

  private static String camelToScreamingSnake(String s) {
    var out = new StringBuilder();
    for (var i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (i > 0
          && Character.isUpperCase(c)
          && Character.isLowerCase(s.charAt(i - 1))) out.append('_');
      out.append(Character.toUpperCase(c));
    }
    return out.toString();
  }

  private static String escapeJavaKeyword(String s) {
    return switch (s) {
      case "type",
              "class",
              "interface",
              "if",
              "else",
              "for",
              "while",
              "do",
              "return",
              "switch",
              "case",
              "default",
              "break",
              "continue",
              "new",
              "this",
              "super",
              "null",
              "true",
              "false",
              "void",
              "int",
              "long",
              "short",
              "byte",
              "char",
              "boolean",
              "float",
              "double",
              "static",
              "final",
              "public",
              "private",
              "protected",
              "abstract",
              "package",
              "import",
              "throws",
              "throw",
              "try",
              "catch",
              "finally",
              "enum",
              "record",
              "instanceof",
              "extends",
              "implements" -> s + "_";
      default -> s;
    };
  }

  @SuppressWarnings("unused")
  private static TList<NamedTypeRef> sourceFieldList(MethodArgs.Source src) {
    return src.fields();
  }
}
