package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

final class AspectsGen {
  private AspectsGen() {}

  private static final ClassName ASPECT =
      ClassName.get("compilerxyz.eval.aspect", "Aspect");
  private static final ClassName ASPECT_LOOKUP =
      ClassName.get("compilerxyz.eval.aspect", "AspectLookup");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName VALUE = ClassName.get("compilerxyz.eval", "Value");
  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");
  private static final ClassName CODE = ClassName.get("compilerxyz.eval", "Code");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName BINDING_ID = ClassName.get("compilerxyz.eval", "BindingId");
  private static final ClassName BINDING = ClassName.get("compilerxyz.gen", "Binding");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName CODE_DYNAMIC =
      ClassName.get("compilerxyz.eval.dynamic", "CodeDynamic");
  private static final ClassName DYN_UTIL =
      ClassName.get("compilerxyz.eval.dynamic", "DynUtil");
  private static final ClassName TARGET_BIND_SUB =
      ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub");
  private static final ClassName ARTIFACT_EXPORTER =
      ClassName.get("compilerxyz.artifact", "ArtifactExporter");
  private static final ClassName SOURCE_INTERFACE_DATA =
      ClassName.get(
          "compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName VALUE_DYNAMIC_SOURCE =
      ClassName.get("compilerxyz.eval.value", "ValueDynamic", "Source");

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  /**
   * Aspects whose Java trait methods are prefixed with the interface name (per the naming
   * convention). Add each aspect here once its impls and callers have been renamed.
   */
  private static final TSet<String> PREFIXED_ASPECTS = TSet.of(
      "PostDeserialize");

  private static final TypeName EVAL_RES = ParameterizedTypeName.get(ERESULT, CODE_VALUE);

  private static final TypeName OPT_TYPE = ParameterizedTypeName.get(OPT, TYPE);

  private record Param(String name, TypeName type, boolean nullable) {
    Param(String name, TypeName type) {
      this(name, type, false);
    }
  }

  private static final ClassName NULLABLE =
      ClassName.get("org.jspecify.annotations", "Nullable");

  private record AspectMethod(
      String javadoc, String name, TList<Param> params, TypeName ret) {}

  private record NestedRecord(String javadoc, String name, TList<Param> components) {}

  private record AspectSpec(
      String javadoc,
      String name,
      long builtinUniqueId,
      TList<NestedRecord> nestedRecords,
      TList<AspectMethod> methods) {
    AspectSpec(String javadoc, String name, long id, TList<AspectMethod> methods) {
      this(javadoc, name, id, TList.of(), methods);
    }
  }

  static void run(
      Path outRoot, Formatter formatter, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds)
      throws IOException {
    var seenNames = new TSet<String>();
    var specs = specs();
    for (var spec : specs) {
      if (!seenNames.add(spec.name)) {
        throw new IllegalStateException("duplicate aspect name: " + spec.name);
      }
      if (!builtinUniqueIds.add(spec.builtinUniqueId)) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId + " (in aspects)");
      }
      writeAspect(outRoot, formatter, spec);
    }
  }

  private static void writeAspect(Path outRoot, Formatter formatter, AspectSpec spec)
      throws IOException {
    var fullName = "Aspect" + spec.name;
    var self = ClassName.get(GEN_PACKAGE, fullName);

    var typeBuilder =
        TypeSpec.interfaceBuilder(fullName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ASPECT);
    if (spec.javadoc != null && !spec.javadoc.isEmpty()) {
      typeBuilder.addJavadoc(spec.javadoc + "\n");
    }
    typeBuilder.addJavadoc(
        "<p>Generated by {@code AspectsGen}; port of"
            + " {@code source/buildsrc/aspects.rs}'s {@code Aspect" + spec.name
            + "} entry (Rust builtin_unique_id " + spec.builtinUniqueId + ").\n");

    typeBuilder.addField(
            FieldSpec.builder(UNIQUE, "ID", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId)
                    .build());

    for (var nested : spec.nestedRecords) {
      typeBuilder.addType(buildNestedRecord(nested));
    }

    var prefixed = PREFIXED_ASPECTS.contains(spec.name);
    var prefix = "aspect" + spec.name;
    for (var m : spec.methods) {
      var javaName = m.name;
      if (prefixed) {
        javaName = InterfacesGen.escapeJavaKeyword(
            prefix + Character.toUpperCase(m.name.charAt(0)) + m.name.substring(1));
      }
      var method =
          MethodSpec.methodBuilder(javaName)
              .returns(m.ret)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
      if (m.javadoc != null && !m.javadoc.isEmpty()) {
        method.addJavadoc(m.javadoc + "\n");
      }
      for (var p : m.params) {
        var pb = ParameterSpec.builder(p.type, InterfacesGen.escapeJavaKeyword(p.name));
        if (p.nullable) {
          pb.addAnnotation(AnnotationSpec.builder(NULLABLE).build());
        }
        method.addParameter(pb.build());
      }
      typeBuilder.addMethod(method.build());
    }

    var getMaybe =
            MethodSpec.methodBuilder("getMaybe")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(OPT, self))
                    .addParameter(EXPR_CONTEXT, "xx")
                    .addParameter(TYPE, "t")
                    .addStatement("return $T.getMaybe(xx, t, ID, $T.class)", ASPECT_LOOKUP, self)
                    .build();
    var get =
            MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ERESULT, self))
                    .addParameter(EXPR_CONTEXT, "xx")
                    .addParameter(QEXPR_ID, "loc")
                    .addParameter(TYPE, "t")
                    .addStatement("return $T.get(xx, loc, t, ID, $T.class)", ASPECT_LOOKUP, self)
                    .build();
    typeBuilder.addMethod(getMaybe);
    typeBuilder.addMethod(get);

    var file = JavaFile.builder(GEN_PACKAGE, typeBuilder.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static TypeSpec buildNestedRecord(NestedRecord nested) {
    var cls =
        TypeSpec.classBuilder(nested.name)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    if (nested.javadoc != null && !nested.javadoc.isEmpty()) {
      cls.addJavadoc(nested.javadoc + "\n");
    }
    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    for (var c : nested.components) {
      cls.addField(
          FieldSpec.builder(c.type, c.name, Modifier.PUBLIC, Modifier.FINAL).build());
      ctor.addParameter(ParameterSpec.builder(c.type, c.name).build());
      ctor.addStatement("this.$N = $N", c.name, c.name);
    }
    cls.addMethod(ctor.build());
    for (var c : nested.components) {
      cls.addMethod(
          MethodSpec.methodBuilder(c.name)
              .addModifiers(Modifier.PUBLIC)
              .returns(c.type)
              .addStatement("return this.$N", c.name)
              .build());
    }
    return cls.build();
  }

  private static String snake(String camel) {
    var out = new StringBuilder();
    for (var i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) {
        out.append('_');
      }
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }

  private static TList<AspectSpec> specs() {
    var xxParam = new Param("xx", EXPR_CONTEXT);
    var locParam = new Param("loc", QEXPR_ID);
    return TList.of(
        new AspectSpec("Identity aspect for null", "Null", 1001, TList.of()),
        new AspectSpec(
            "Identity aspect for record",
            "Record",
            1002,
            TList.of(
                new AspectMethod(
                    "Get the record's field name to type mapping.",
                    "recordFieldTypes",
                    TList.of(),
                    ParameterizedTypeName.get(TORDERED_MAP, CONST_KEY, TYPE)),
                new AspectMethod(
                    "",
                    "recordTakeFields",
                    TList.of(xxParam, locParam, new Param("value", VALUE)),
                    ParameterizedTypeName.get(TORDERED_MAP, CONST_KEY, EVAL_RES)))),
        new AspectSpec(
            "Identity aspect for dynamic number", "NumberDynamic", 1003, TList.of()),
        new AspectSpec("Identity aspect for dynamic bool", "BoolDynamic", 1004, TList.of()),
        new AspectSpec(
            "Identity aspect for dynamic string", "StringDynamic", 1005, TList.of()),
        new AspectSpec(
            "Identity aspect for const bool",
            "BoolConst",
            1006,
            TList.of(
                new AspectMethod(
                    "",
                    "get",
                    TList.of(new Param("value", VALUE)),
                    TypeName.BOOLEAN))),
        new AspectSpec(
            "Identity aspect for const number",
            "NumberConst",
            1008,
            TList.of(
                new AspectMethod(
                    "",
                    "get",
                    TList.of(new Param("value", VALUE)),
                    TypeName.DOUBLE))),
        new AspectSpec(
            "Identity aspect for const str",
            "StringConst",
            1009,
            TList.of(
                new AspectMethod(
                    "",
                    "get",
                    TList.of(new Param("value", VALUE)),
                    ClassName.get(String.class)))),
        new AspectSpec(
            "Converts a known type Value into a type rust value",
            "Type",
            1010,
            TList.of(
                new AspectMethod(
                    "",
                    "type_",
                    TList.of(new Param("value", VALUE)),
                    TYPE))),
        new AspectSpec(
            "Identity aspect for dynamic list", "ListDynamic", 1011, TList.of()),
        new AspectSpec(
            "Marker aspect implemented by unboxed primitive dynamic types (e.g."
                + " TypeNumberDynamic, TypeBoolDynamic) to point to their JVM boxed"
                + " counterpart. Container constructors (list, map, tuple) call this at"
                + " type-construction time to substitute the boxed element type, so that"
                + " values flowing through Object-shaped containers are typed as boxed"
                + " and go through the normal typecheck-convert path (which emits the"
                + " actual {@code valueOf} via the box type's {@code typeCastFrom}).",
            "DynamicBoxed",
            1033,
            TList.of(
                new AspectMethod(
                    "Returns the JVM boxed counterpart of this unboxed primitive type.",
                    "boxedForm",
                    TList.of(),
                    TYPE))),
        new AspectSpec(
            "Marker aspect implemented by JVM boxed dynamic types (e.g."
                + " TypeNumberDynamicBox, TypeBoolDynamicBox) to point to their unboxed"
                + " primitive counterpart. Used by iteration sites that want to load"
                + " and unbox to the primitive form.",
            "DynamicUnboxed",
            1034,
            TList.of(
                new AspectMethod(
                    "Returns the unboxed primitive counterpart of this boxed type.",
                    "unboxedForm",
                    TList.of(),
                    TYPE))),
        new AspectSpec(
            "Identity aspect for dynamic map",
            "MapDynamic",
            1012,
            TList.of(
                new AspectMethod("", "keyType", TList.of(), TYPE),
                new AspectMethod("", "valueType", TList.of(), TYPE))),
        new AspectSpec(
            "Identity aspect for tuple",
            "Tuple",
            1013,
            TList.of(
                new AspectMethod(
                    "Get the types of elements",
                    "elementTypes",
                    TList.of(),
                    ParameterizedTypeName.get(TLIST, TYPE)),
                new AspectMethod(
                    "",
                    "takeElements",
                    TList.of(xxParam, locParam, new Param("value", VALUE)),
                    ParameterizedTypeName.get(TLIST, EVAL_RES)))),
        new AspectSpec(
            "Identity aspect for union",
            "Union",
            1014,
            TList.of(
                new NestedRecord(
                    "One union variant (tag, runtime js-tag string, payload type).",
                    "Variant",
                    TList.of(
                        new Param("key", CONST_KEY),
                        new Param("jsTag", ClassName.get(String.class)),
                        new Param("type", TYPE)))),
            TList.of(
                new AspectMethod(
                    "",
                    "variants",
                    TList.of(),
                    ParameterizedTypeName.get(
                        TLIST,
                        ClassName.get(GEN_PACKAGE, "AspectUnion", "Variant"))))),
        new AspectSpec(
            "Allows field expressions to work on the value of this type",
            "Field",
            1015,
            TList.of(
                new AspectMethod(
                    "Return a value representing the access of this field. Error if the field"
                        + " doesn't exist. Emits only field-access code; sibling-field drop policy"
                        + " lives in {@link AspectDropDynamic#drop} for the base type.",
                    "field",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("self_", CODE_VALUE),
                        new Param("key", CODE_VALUE)),
                    EVAL_RES),
                new AspectMethod(
                    "Same as {@code field} but return a None if the field doesn't exist.",
                    "fieldTypeOpt",
                    TList.of(xxParam, new Param("key", CONST_KEY)),
                    OPT_TYPE),
                new AspectMethod(
                    "Enumerate the known field keys on this type. Used to identify sibling fields"
                        + " whose values need to be dropped when one field is extracted. Return"
                        + " empty if the type does not own its fields (e.g. records that borrow).",
                    "fieldKeys",
                    TList.of(xxParam),
                    ParameterizedTypeName.get(TLIST, CONST_KEY)))),
        new AspectSpec(
            "Allows the value of this type to be called (as a function)",
            "Function",
            1116,
            TList.of(
                new AspectMethod(
                    "Make a call using the function referred to by the value.",
                    "call",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("self_", CODE_VALUE),
                        new Param("argument", CODE_VALUE)),
                    EVAL_RES),
                new AspectMethod(
                    "Returns the declared exception type of this function, or None if not"
                        + " declared.",
                    "exceptionType",
                    TList.of(),
                    OPT_TYPE))),
        new AspectSpec(
            "Allows the value to be assigned new data (mutated).",
            "Set",
            1017,
            TList.of(
                new AspectMethod(
                    "Mutate the value with the data from a new value.",
                    "set",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("self_", CODE_VALUE),
                        new Param("value", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "",
            "OperatorAdd",
            1018,
            TList.of(
                new AspectMethod(
                    "",
                    "add",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("base", CODE_VALUE),
                        new Param("reference", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "",
            "OperatorEq",
            1019,
            TList.of(
                new AspectMethod(
                    "",
                    "eq",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("base", CODE_VALUE),
                        new Param("reference", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "",
            "OperatorLt",
            1020,
            TList.of(
                new AspectMethod(
                    "",
                    "lt",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("base", CODE_VALUE),
                        new Param("reference", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "Creates bindings for pre-existing values. Namely function arguments and global"
                + " scope values that were externally injected.",
            "InitialBind",
            1021,
            TList.of(
                new AspectMethod(
                    "",
                    "initialBind",
                    TList.of(new Param("dynamicKey", BINDING_ID)),
                    BINDING))),
        new AspectSpec(
            "Creates code to bind a value, and returns the binding (binding).",
            "Bind",
            1022,
            TList.of(
                new NestedRecord(
                    "Result of {@code bind}: the emitted binding code and the resulting binding.",
                    "BindResult",
                    TList.of(new Param("code", CODE), new Param("binding", BINDING)))),
            TList.of(
                new AspectMethod(
                    "",
                    "bind",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(
                        ERESULT,
                        ClassName.get(GEN_PACKAGE, "AspectBind", "BindResult"))))),
        new AspectSpec(
            "Creates code to prepare a value for binding, used by the Dynamic target (call tree"
                + " rooted in Bind). This may be called recursively, e.g. in records, to prepare"
                + " elements. If None, the value does not require dynamic binding - this is used"
                + " by records/tuples to determine if the containing record/tuple also needs"
                + " dynamic binding.",
            "BindDynamic",
            1023,
            TList.of(
                new AspectMethod(
                    "",
                    "bindDynamic",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(
                        ERESULT, ParameterizedTypeName.get(OPT, TARGET_BIND_SUB))))),
        new AspectSpec(
            "Used to ensure a value is fully dynamic (turns const values into dynamic, does"
                + " nothing for dynamic values).",
            "ConsumeDynamic",
            1024,
            TList.of(
                new AspectMethod(
                    "Convert the value to dynamic and return the code that this produced.",
                    "toDynamic",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(ERESULT, CODE_DYNAMIC)),
                new AspectMethod(
                    "The JVM data descriptor for the value {@code toDynamic}'s produced code"
                        + " leaves on the stack. Primitive types (D, I, J, Z, F) as well as"
                        + " reference types (L…;) are both valid; drives field descriptors"
                        + " and CHECKCAST decisions. The presence of this aspect already"
                        + " implies the type has a host stack representation, so this is"
                        + " infallible.",
                    "consumeDynamicJvmClass",
                    TList.of(),
                    ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor")))),
        new AspectSpec(
            "",
            "StackValueDynamic",
            1025,
            TList.of(
                new AspectMethod(
                    "",
                    "toValue",
                    TList.of(locParam, new Param("self_", CODE_DYNAMIC)),
                    CODE_VALUE))),
        new AspectSpec(
            "Called on imported values to convert them from const to dynamic form when"
                + " appropriate (e.g. instances of source data/union types). Record/tuple types"
                + " apply PostDeserialize to their elements recursively. Default: no-op.",
            "PostDeserialize",
            1031,
            TList.of(
                new AspectMethod(
                    "",
                    "postDeserialize",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(ERESULT, CODE_VALUE)))),
        new AspectSpec(
            "Called to turn a dynamic host-runtime value back into a typed {@link Value}. Mirrors"
                + " Rust's {@code AspectLift::lift(&self, &ExprContext, &v8::PinScope,"
                + " v8::Local<v8::Value>) -> Result<Rc<dyn Value>, String>}, with the v8"
                + " scope/value pair replaced by the boxed Java object the bytecode produced.",
            "Lift",
            1026,
            TList.of(
                new AspectMethod(
                    "",
                    "lift",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param(
                            "externalHostValue", ClassName.get(Object.class), true)),
                    ParameterizedTypeName.get(ERESULT, VALUE)))),
        new AspectSpec(
            "Allows foreach expressions to be run on the value.",
            "Iterable",
            1027,
            TList.of(
                new AspectMethod("", "elementType", TList.of(), TYPE),
                new AspectMethod(
                    "",
                    "foreach",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("base", CODE_VALUE),
                        new Param("elementKey", BINDING_ID),
                        new Param("body", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "Allows a value to be exported from a module.",
            "Artifact",
            1028,
            TList.of(
                new AspectMethod(
                    "Returns the exporter", "exporter", TList.of(), ARTIFACT_EXPORTER))),
        new AspectSpec(
            "Allows a value to emit cleanup (drop) code when its scope ends, when it is"
                + " discarded as a Seq entry, or when a sibling field is not extracted."
                + " Implementations typically delegate to {@link"
                + " compilerxyz.eval.dynamic.DynUtil#dropDefault} which emits a POP when the"
                + " value's source root is a stack Expr; types with additional cleanup"
                + " prepend that before delegating.",
            "DropDynamic",
            1032,
            TList.of(
                new AspectMethod(
                    "",
                    "drop",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(ERESULT, CODE)))),
        new AspectSpec(
            "Light drop for values that only need to clean up their stack footprint (POP)"
                + " when discarded — no per-type cleanup. Implementers typically delegate"
                + " to {@link compilerxyz.eval.dynamic.DynUtil#dropDefault}.",
            "DropDynamicLight",
            1035,
            TList.of(
                new AspectMethod(
                    "Emit the drop code for this value.",
                    "dropDynamicLight",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(ERESULT, CODE)))),
        new AspectSpec(
            "Called on the type of a field's value to produce the CodeValue that a field"
                + " expression evaluates to when the base is const. Implementations typically"
                + " return the child CodeValue unmodified.",
            "FieldUseConst",
            1036,
            TList.of(
                new AspectMethod(
                    "",
                    "fieldUseConst",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "Called on the type of a field's value to produce the CodeValue that a field"
                + " expression evaluates to when the base is dynamic. Receives the base's"
                + " Source and the JVM field name owned by the parent's runtime class."
                + " Implementations typically delegate to {@link"
                + " compilerxyz.eval.dynamic.DynUtil#fieldUseDefaultDynamic} to wrap this"
                + " type as a ValueDynamic backed by {@link ValueDynamic.Source.Field}.",
            "FieldUseDynamic",
            1037,
            TList.of(
                new AspectMethod(
                    "",
                    "fieldUseDynamic",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("baseSource", VALUE_DYNAMIC_SOURCE),
                        new Param("javaFieldName", ClassName.get(String.class))),
                    EVAL_RES))),
        new AspectSpec(
            "Called on the type of an element to produce the CodeValue that an element"
                + " expression evaluates to when the container is const. Implementations"
                + " typically return the child CodeValue unmodified.",
            "ElementUseConst",
            1038,
            TList.of(
                new AspectMethod(
                    "",
                    "elementUseConst",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    EVAL_RES))),
        new AspectSpec(
            "Called on the type of an element to produce the CodeValue that an element"
                + " expression evaluates to when the container is dynamic. Receives the fully"
                + " constructed element Source and the carry Code (load-base bytecode)."
                + " Implementations typically delegate to {@link"
                + " compilerxyz.eval.dynamic.DynUtil#elementUseDefaultDynamic} to wrap this"
                + " type as a ValueDynamic with the given source.",
            "ElementUseDynamic",
            1039,
            TList.of(
                new AspectMethod(
                    "",
                    "elementUseDynamic",
                    TList.of(
                        xxParam,
                        locParam,
                        new Param("elementSource", VALUE_DYNAMIC_SOURCE),
                        new Param("carry", CODE)),
                    EVAL_RES))),
        new AspectSpec(
            "Creates the loop-variable Binding for an element read out of a homogeneous"
                + " collection (foreach). Same shape as {@link AspectInitialBind} but"
                + " specifically for the element-as-loop-variable case; the returned"
                + " Binding drives {@code bindingUse_} for references to the loop"
                + " variable inside the foreach body.",
            "ElementUseDynamicBind",
            1040,
            TList.of(
                new AspectMethod(
                    "",
                    "elementUseDynamicBind",
                    TList.of(new Param("dynamicKey", BINDING_ID)),
                    BINDING))),
        new AspectSpec(
            "Identity aspect for aspects (types representing values holding aspect"
                + " implementations).",
            "Aspect",
            1029,
            TList.of(
                new AspectMethod(
                    "",
                    "getUnique",
                    TList.of(new Param("value", SOURCE_INTERFACE_DATA)),
                    UNIQUE),
                new AspectMethod(
                    "",
                    "getIfaceData",
                    TList.of(new Param("value", SOURCE_INTERFACE_DATA)),
                    SOURCE_INTERFACE_DATA))));
  }
}
