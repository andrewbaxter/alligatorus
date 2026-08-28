package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.IList;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import javax.lang.model.element.Modifier;

/**
 * Emits the unboxed-primitive list machinery: one growable array store per JVM primitive kind
 * and the {@code GenLists} factory that dispatches a list element type to a {@code
 * TypeListSpecializedDynamic} instance parameterized with that primitive's store and identity
 * (falling back to {@code TypeListBoxedDynamic}).
 */
public final class ListsGen {
  private ListsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName LIST_SPECIALIZED =
      ClassName.get("compilerxyz.eval.value.list", "TypeListSpecializedDynamic");
  private static final ClassName LIST_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.list", "ListDynamic");
  private static final ClassName LIST_BOXED =
      ClassName.get("compilerxyz.eval.value.list", "TypeListBoxedDynamic");
  private static final ClassName GEN_IDENTITIES =
      ClassName.get(GEN_PACKAGE, "GenTrivialIdentityAspects");

  private record Store(String simple, TypeName javaPrim) {}

  /** Explicit uniqueIds paired with each primitive so the ids stay stable. */
  private record Prim(
      String name, String storeSimple, ClassName elementClass, long uniqueId) {}

  private static final IList<Store> STORES =
      IList.of(
          new Store("ListStoreInt", TypeName.INT),
          new Store("ListStoreLong", TypeName.LONG),
          new Store("ListStoreShort", TypeName.SHORT),
          new Store("ListStoreByte", TypeName.BYTE),
          new Store("ListStoreFloat", TypeName.FLOAT),
          new Store("ListStoreDouble", TypeName.DOUBLE),
          new Store("ListStoreBool", TypeName.BOOLEAN));

  private static final IList<Prim> PRIMS =
      IList.of(
          new Prim(
              "Int",
              "ListStoreInt",
              ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic"),
              6200L),
          new Prim(
              "Uint",
              "ListStoreInt",
              ClassName.get("compilerxyz.eval.value.uint_", "TypeUintDynamic"),
              6201L),
          new Prim(
              "Long",
              "ListStoreLong",
              ClassName.get("compilerxyz.eval.value.long_", "TypeLongDynamic"),
              6202L),
          new Prim(
              "Ulong",
              "ListStoreLong",
              ClassName.get("compilerxyz.eval.value.ulong_", "TypeUlongDynamic"),
              6203L),
          new Prim(
              "Short",
              "ListStoreShort",
              ClassName.get("compilerxyz.eval.value.short_", "TypeShortDynamic"),
              6204L),
          new Prim(
              "Ushort",
              "ListStoreShort",
              ClassName.get("compilerxyz.eval.value.ushort_", "TypeUshortDynamic"),
              6205L),
          new Prim(
              "Byte",
              "ListStoreByte",
              ClassName.get("compilerxyz.eval.value.byte_", "TypeByteDynamic"),
              6206L),
          new Prim(
              "Ubyte",
              "ListStoreByte",
              ClassName.get("compilerxyz.eval.value.ubyte_", "TypeUbyteDynamic"),
              6207L),
          new Prim(
              "Float",
              "ListStoreFloat",
              ClassName.get("compilerxyz.eval.value.float_", "TypeFloatDynamic"),
              6208L),
          new Prim(
              "Double",
              "ListStoreDouble",
              ClassName.get("compilerxyz.eval.value.double_", "TypeDoubleDynamic"),
              6209L),
          new Prim(
              "Bool",
              "ListStoreBool",
              ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic"),
              6210L));

  public static TList<IdentitiesGen.IdentitySpec> identitySpecs() {
    var out = new TList<IdentitiesGen.IdentitySpec>();
    for (var prim : PRIMS) {
      out.add(
          new IdentitiesGen.IdentitySpec(snake(typeSimple(prim)), prim.uniqueId(), true));
    }
    return out;
  }

  public static void run(Path outRoot, Formatter formatter) throws IOException {
    for (var store : STORES) {
      GenMain.write(
          outRoot, JavaFile.builder(GEN_PACKAGE, storeClass(store)).build(), formatter);
    }
    GenMain.write(outRoot, JavaFile.builder(GEN_PACKAGE, factoryClass()).build(), formatter);
  }

  private static TypeSpec storeClass(Store store) {
    var arrayType = ArrayTypeName.of(store.javaPrim());
    return TypeSpec.classBuilder(store.simple())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addField(
            FieldSpec.builder(
                    arrayType, "EMPTY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T[0]", store.javaPrim())
                .build())
        .addField(
            FieldSpec.builder(arrayType, "data", Modifier.PRIVATE)
                .initializer("EMPTY")
                .build())
        .addField(FieldSpec.builder(TypeName.INT, "size", Modifier.PRIVATE).build())
        .addMethod(
            MethodSpec.methodBuilder("push")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(store.javaPrim(), "value")
                .beginControlFlow("if (size == data.length)")
                .addStatement(
                    "data = $T.copyOf(data, $T.max(8, data.length * 2))",
                    Arrays.class,
                    Math.class)
                .endControlFlow()
                .addStatement("data[size] = value")
                .addStatement("size += 1")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("pop")
                .addModifiers(Modifier.PUBLIC)
                .returns(store.javaPrim())
                .addStatement("size -= 1")
                .addStatement("return data[size]")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .returns(store.javaPrim())
                .addParameter(TypeName.INT, "index")
                .beginControlFlow("if (index >= size)")
                .addStatement(
                    "throw new $T(index)", ArrayIndexOutOfBoundsException.class)
                .endControlFlow()
                .addStatement("return data[index]")
                .build())
        .addMethod(
            MethodSpec.methodBuilder("size")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return size")
                .build())
        .build();
  }

  private static String typeSimple(Prim prim) {
    return "TypeList" + prim.name() + "Dynamic";
  }

  private static TypeSpec factoryClass() {
    var method =
        MethodSpec.methodBuilder("listDynamic")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(LIST_DYNAMIC)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "elementType");
    for (var prim : PRIMS) {
      method.beginControlFlow(
          "if (elementType.typeAspect(xx, $T.$L).optIsSome())",
          GEN_IDENTITIES,
          constantName(snake(prim.elementClass().simpleName())));
      method.addStatement(
          "return new $T(new $T(), $T.fromClass($T.class), $T.$L)",
          LIST_SPECIALIZED,
          prim.elementClass(),
          JAVA_INTERNAL_NAME,
          ClassName.get(GEN_PACKAGE, prim.storeSimple()),
          GEN_IDENTITIES,
          constantName(snake(typeSimple(prim))));
      method.endControlFlow();
    }
    method.addStatement("return new $T(elementType)", LIST_BOXED);
    return TypeSpec.classBuilder("GenLists")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
        .addMethod(method.build())
        .build();
  }

  private static String snake(String camel) {
    var sb = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
  }

  private static String constantName(String snake) {
    return snake.toUpperCase(Locale.ROOT) + "_IDENTITY";
  }
}
