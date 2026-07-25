package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.util.HashMap;
import java.util.Map;

public final class DynamicTypes {
  private DynamicTypes() {}

  private enum LeafKind {
    NULLARY_NEW,
    STATIC_FACTORY,
    DELEGATED
  }

  private record LeafEntry(LeafKind kind, TypeName valueClass, ClassName factoryOwner) {
    LeafEntry(LeafKind kind, TypeName valueClass) {
      this(kind, valueClass, null);
    }
  }

  private record CompositeEntry(int arity, TypeName valueClass) {}

  private static final Map<ClassName, LeafEntry> LEAVES = new HashMap<>();
  private static final Map<ClassName, CompositeEntry> COMPOSITES = new HashMap<>();

  private static void nullary(String pkg, String cls, TypeName valueClass) {
    var cn = ClassName.get(pkg, cls);
    LEAVES.put(cn, new LeafEntry(LeafKind.NULLARY_NEW, valueClass));
  }

  private static void nullary(String pkg, String cls) {
    var cn = ClassName.get(pkg, cls);
    LEAVES.put(cn, new LeafEntry(LeafKind.NULLARY_NEW, cn));
  }

  private static void factory(String pkg, String cls, TypeName valueClass) {
    var cn = ClassName.get(pkg, cls);
    LEAVES.put(cn, new LeafEntry(LeafKind.STATIC_FACTORY, valueClass));
  }

  private static void factory(String pkg, String cls) {
    var cn = ClassName.get(pkg, cls);
    LEAVES.put(cn, new LeafEntry(LeafKind.STATIC_FACTORY, cn));
  }

  /**
   * The value-class is {@code cls}, but the runtime {@code Type} descriptor comes from
   * {@code factoryOwner.dynamicType()}. Use when the source-facing Type descriptor lives on a
   * separate helper class from the value class.
   */
  private static void delegated(
      String valuePkg, String valueCls, String factoryPkg, String factoryCls) {
    var value = ClassName.get(valuePkg, valueCls);
    var factory = ClassName.get(factoryPkg, factoryCls);
    LEAVES.put(value, new LeafEntry(LeafKind.DELEGATED, value, factory));
  }

  private static void delegated(
      String valuePkg, String valueCls, String factoryPkg, String factoryCls,
      TypeName valueClass) {
    var key = ClassName.get(valuePkg, valueCls);
    var factory = ClassName.get(factoryPkg, factoryCls);
    LEAVES.put(key, new LeafEntry(LeafKind.DELEGATED, valueClass, factory));
  }

  private static void composite(String pkg, String cls, int arity, TypeName valueClass) {
    var cn = ClassName.get(pkg, cls);
    COMPOSITES.put(cn, new CompositeEntry(arity, valueClass));
  }

  static {
    // Nullary Type descriptors — value classes are unboxed / distinct where appropriate.
    nullary("compilerxyz.eval.value.string", "TypeStringDynamic", ClassName.get(String.class));
    nullary("compilerxyz.eval.value.bool_", "TypeBoolDynamic", TypeName.BOOLEAN);
    nullary("compilerxyz.eval.value.int_", "TypeIntDynamic", TypeName.LONG);
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

    // Static-factory descriptors — interfaces where value class == descriptor class.
    factory("compilerxyz.eval", "Type");
    factory("compilerxyz.eval", "ExprContext");
    factory("compilerxyz.eval", "QExprId");
    factory("compilerxyz.eval", "CodeValue");
    delegated("compilerxyz.eval", "Code", "compilerxyz.gen", "GenCode");
    factory("compilerxyz.eval", "ConstKey");
    factory("compilerxyz.eval", "Expr");
    delegated("compilerxyz.eval.aspect", "Aspect", "compilerxyz.gen", "GenAspect");
    factory("compilerxyz.eval.push", "StagedExpr");
    delegated(
        "compilerxyz.eval.value", "ValueDynamicSource",
        "compilerxyz.gen", "GenValueDynamicSource");
    factory("compilerxyz.eval.value.source_interface", "InterfaceData");
    factory("compilerxyz.eval.value.source_interface", "TypeSourceInterfaceDynamic");
    factory("compilerxyz.eval.dynamic", "JavaDataDescriptor");
    factory("compilerxyz.eval.dynamic.target", "TargetBindSub");
    factory("compilerxyz.eval.dynamic.target", "BindResult");
    factory("compilerxyz.eval.dynamic.target", "RecordBindField");
    factory("compilerxyz.eval.dynamic.target", "TupleBindSlot");
    factory("compilerxyz.eval.dynamic.target", "IfBranch");
    factory("compilerxyz.eval.dynamic.target", "TryCatchClause");
    factory("compilerxyz.eval.dynamic.target", "TargetCase");
    factory("compilerxyz.eval.dynamic", "CodeDynamic");
    factory("compilerxyz.eval", "BindingId");
    factory("compilerxyz.eval.value.source_object_new", "TypeSourceObjectNewDynamic");
    factory("compilerxyz.eval.value.source_union_new", "TypeSourceUnionNewDynamic");
    factory("compilerxyz.eval", "DefinitionSet");
    factory("compilerxyz.eval.value.source_object", "SourceDataMethodData");
    factory("compilerxyz.eval.value.source_interface", "InterfaceMethodData");
    delegated(
        "compilerxyz.eval.value.source_function", "TypeSourceFunctionConst",
        "compilerxyz.gen", "GenTypeSourceFunctionConst");
    delegated(
        "compilerxyz.eval.value.source_object", "TypeSourceObjectDynamic",
        "compilerxyz.gen", "GenTypeSourceObjectDynamic");
    delegated(
        "compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic",
        "compilerxyz.gen", "GenTypeSourceUnionDynamic");
    factory("compilerxyz.gen", "Value");
    LEAVES.put(
        ClassName.get("compilerxyz.gen", "AspectBind", "BindResult"),
        new LeafEntry(
            LeafKind.DELEGATED,
            ClassName.get("compilerxyz.gen", "AspectBind", "BindResult"),
            ClassName.get("compilerxyz.gen", "GenAspectBindBindResult")));
    factory("compilerxyz.gen", "Binding");
    factory("compilerxyz.gen", "Target");
    // Aspects: dynamicType() lives on the GenAspectXxx companion class.
    for (var name : new String[] {
        "AspectFunction", "AspectField", "AspectBind", "AspectBindDynamic",
        "AspectConsumeDynamic", "AspectDropDynamic", "AspectDropDynamicLight",
        "AspectInitialBind", "AspectStackValueDynamic", "AspectLift", "AspectType",
        "AspectArtifact", "AspectAspect", "AspectOperatorAdd", "AspectOperatorMod", "AspectSet",
        "AspectInt"}) {
      delegated("compilerxyz.gen", name, "compilerxyz.gen", "Gen" + name);
    }

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

  private static final java.util.Set<ClassName> GEN_TRAITS =
      java.util.Set.of(
          ClassName.get("compilerxyz.gen", "Value"),
          ClassName.get("compilerxyz.gen", "Binding"),
          ClassName.get("compilerxyz.gen", "Target"),
          ClassName.get("compilerxyz.gen", "CodeDyn"),
          ClassName.get("compilerxyz.eval", "Type"));

  public static CodeBlock leaf(ClassName descriptor) {
    var entry = leafEntry(descriptor);
    return switch (entry.kind()) {
      case NULLARY_NEW -> CodeBlock.of("new $T()", descriptor);
      case STATIC_FACTORY -> {
        if (GEN_TRAITS.contains(descriptor)) {
          // Trait interfaces (Value/Binding/Target/Type/CodeDyn) no longer
          // carry a handwritten dynamicType() — route through the codegen
          // companion GenX.dynamicType() instead.
          var gen = ClassName.get("compilerxyz.gen", "Gen" + descriptor.simpleName());
          yield CodeBlock.of("$T.dynamicType()", gen);
        }
        yield CodeBlock.of("$T.dynamicType()", descriptor);
      }
      case DELEGATED -> CodeBlock.of("$T.dynamicType()", entry.factoryOwner());
    };
  }

  public static TypeName leafValueClass(ClassName descriptor) {
    return leafEntry(descriptor).valueClass();
  }

  private static LeafEntry leafEntry(ClassName descriptor) {
    var entry = LEAVES.get(descriptor);
    if (entry != null) return entry;
    if (descriptor.packageName().equals("compilerxyz.gen")) {
      return new LeafEntry(LeafKind.STATIC_FACTORY, descriptor);
    }
    throw new IllegalArgumentException(
        "DynamicTypes has no leaf entry for " + descriptor.canonicalName());
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
    return com.squareup.javapoet.ParameterizedTypeName.get((ClassName) spec.valueClass(), params);
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
