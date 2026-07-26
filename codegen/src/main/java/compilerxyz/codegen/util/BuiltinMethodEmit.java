package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.jspecify.annotations.Nullable;

/**
 * Shared method-emission machinery used by every builtin-interface generator
 * (aspects, traits, and any other codegen that needs to attach source-visible
 * methods to a {@code TypeObjectDynamic}).
 *
 * <p>Each generator supplies a canonical {@link Method} model per method plus a
 * receiver expression / methods-collection variable name; this helper emits the
 * {@code new TypeMethodDynamic(...) / methods.add(new ObjectMethod(...))}
 * plumbing uniformly.
 */
public final class BuiltinMethodEmit {
  private BuiltinMethodEmit() {}

  public record Param(String name, TypeName type) {}

  public record Method(
      String sourceName,     // source-visible name, e.g. "can_cast_from"
      String hostMethodName, // Java method name on the host interface
      TList<Param> params,
      TypeName ret,
      boolean sourceVisible) {}

  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR = ClassName.get("compilerxyz.eval", "ConstKey", "Str");
  private static final ClassName RECORD_CLASS = ClassName.get("compilerxyz.eval.value.record", "Record");
  private static final ClassName FUNCTION_PARAM =
      ClassName.get("compilerxyz.eval.value.function", "FunctionParam");
  private static final ClassName RECORD_FIELD = ClassName.get("compilerxyz.eval.value.record", "RecordField");
  private static final ClassName RECORD_FIELD_DYNAMIC = ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final ClassName TYPE_METHOD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");

  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");
  private static final ClassName TYPE_LIST_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.list", "TypeListDynamic");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_INT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic");
  private static final ClassName TYPE_LONG_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.long_", "TypeLongDynamic");
  private static final ClassName TYPE_NULL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.null_", "TypeNullDynamic");

  private static final TMap<ClassName, ClassName> COMPOSITE_DESCRIPTORS = compositeDescriptors();

  private static TMap<ClassName, ClassName> compositeDescriptors() {
    var m = new TMap<ClassName, ClassName>();
    m.put(ERESULT, TYPE_ERESULT_DYNAMIC);
    m.put(OPT, TYPE_OPTION_DYNAMIC);
    m.put(TLIST, TYPE_LIST_DYNAMIC);
    m.put(ClassName.get("compilerxyz.util", "TMap"), TYPE_MAP_DYNAMIC);
    return m;
  }

  /**
   * Emit statements that build a {@code TypeMethodDynamic} for {@code method}
   * and append an {@code ObjectMethod} to {@code methodsVar}.
   *
   * @param b builder receiving the statements
   * @param hostClass the Java class of the host interface (used for JVM
   *     descriptor construction)
   * @param receiverVar the variable name holding the receiver TOD to bind the
   *     method to (typically {@code "instance"})
   * @param methodsVar the variable name of the {@code TList<ObjectMethod>}
   *     collection to append to
   * @param leafDescriptors mapping of leaf class → {@code Type.dynamicType()}
   *     expression, used to resolve param/ret runtime type descriptors
   * @return {@code true} if the method was emitted; {@code false} if any
   *     param/ret type couldn't be resolved to a descriptor (caller should
   *     treat as a skip)
   */
  public static boolean emitObjectMethod(
      MethodSpec.Builder b,
      ClassName hostClass,
      String receiverVar,
      String methodsVar,
      Method method,
      TMap<ClassName, CodeBlock> leafDescriptors) {
    var retDesc = resolveDescriptor(method.ret, leafDescriptors);
    if (retDesc == null) return false;
    var paramDescs = new TList<CodeBlock>();
    var paramRawClasses = new TList<TypeName>();
    for (var p : method.params) {
      var pd = resolveDescriptor(p.type, leafDescriptors);
      if (pd == null) return false;
      paramDescs.add(pd);
      var raw = rawClass(p.type);
      if (raw == null) return false;
      paramRawClasses.add(raw);
    }
    var retRaw = rawClass(method.ret);
    if (retRaw == null) return false;

    var suffix = method.hostMethodName;

    var argRecordName = "params_" + suffix;
    var paramsExpr = CodeBlock.builder();
    paramsExpr.add("$T.<$T>of(", TLIST, FUNCTION_PARAM);
    for (int i = 0; i < method.params.size(); i++) {
      var p = method.params.get(i);
      if (i > 0) paramsExpr.add(", ");
      paramsExpr.add("new $T($S, $L)", FUNCTION_PARAM, p.name, paramDescs.get(i));
    }
    paramsExpr.add(")");
    b.addStatement("var $L = $L", argRecordName, paramsExpr.build());

    var retTypeName = "retType_" + suffix;
    b.addStatement("var $L = $L", retTypeName, retDesc);

    var descName = "desc_" + suffix;
    var argClassBuilder = CodeBlock.builder();
    for (int i = 0; i < paramRawClasses.size(); i++) {
      if (i > 0) argClassBuilder.add(", ");
      argClassBuilder.add(
          "$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, paramRawClasses.get(i));
    }
    b.addStatement(
        "var $L = $T.fromParts($T.fromClass($T.class), $T.of($L))",
        descName, JAVA_METHOD_DESCRIPTOR, JAVA_DATA_DESCRIPTOR, retRaw, TLIST,
        argClassBuilder.build());

    var jvmClassExpr = CodeBlock.of("$T.fromClass($T.class)",
        ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName"), hostClass);
    var methodVar = "method_" + suffix;
    b.addStatement(
        "var $L = new $T($N, $L, $L, $L, $S, $L, $T.class.isInterface(), false)",
        methodVar,
        TYPE_METHOD_DYNAMIC,
        receiverVar,
        argRecordName,
        retTypeName,
        jvmClassExpr,
        method.hostMethodName,
        descName,
        hostClass);
    b.addStatement(
        "$L.add(new $T($S, $L))", methodsVar, OBJECT_METHOD, method.sourceName, methodVar);
    return true;
  }

  /** Resolve a Java {@link TypeName} to a runtime-type descriptor expression. */
  public static @Nullable CodeBlock resolveDescriptor(
      TypeName tn, TMap<ClassName, CodeBlock> leafDescriptors) {
    if (tn.equals(TypeName.BOOLEAN)) return CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC);
    if (tn.equals(TypeName.LONG)) return CodeBlock.of("new $T()", TYPE_LONG_DYNAMIC);
    if (tn.equals(TypeName.VOID)) return CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC);
    if (tn instanceof ClassName cn) {
      if (cn.equals(ClassName.get(String.class))) {
        return CodeBlock.of(
            "new $T()", ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic"));
      }
      return leafDescriptors.get(cn);
    }
    if (tn instanceof ParameterizedTypeName pt) {
      var wrapper = COMPOSITE_DESCRIPTORS.get(pt.rawType);
      if (wrapper == null) return null;
      var parts = new TList<CodeBlock>();
      for (var arg : pt.typeArguments) {
        var sub = resolveDescriptor(arg, leafDescriptors);
        if (sub == null) return null;
        parts.add(sub);
      }
      var joined = CodeBlock.builder();
      for (int i = 0; i < parts.size(); i++) {
        if (i > 0) joined.add(", ");
        joined.add(parts.get(i));
      }
      return CodeBlock.of("new $T($L)", wrapper, joined.build());
    }
    return null;
  }

  /** Raw class name of a type (drops type parameters). Primitive types map to their JVM class. */
  public static @Nullable TypeName rawClass(TypeName tn) {
    if (tn.equals(TypeName.BOOLEAN)) return TypeName.BOOLEAN;
    if (tn.equals(TypeName.LONG)) return TypeName.LONG;
    if (tn.equals(TypeName.VOID)) return TypeName.VOID;
    if (tn instanceof ClassName cn) return cn;
    if (tn instanceof ParameterizedTypeName pt) return pt.rawType;
    return null;
  }
}
