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

  private static void composite(String pkg, String cls, int arity, TypeName valueClass) {
    var cn = ClassName.get(pkg, cls);
    COMPOSITES.put(cn, new CompositeEntry(arity, valueClass));
  }

  static {
    // Nullary Type descriptors — value classes are unboxed / distinct where appropriate.
    nullary("compilerxyz.eval.value.string", "TypeStringDynamic", ClassName.get(String.class));
    nullary("compilerxyz.eval.value.bool_", "TypeBoolDynamic", TypeName.BOOLEAN);
    nullary("compilerxyz.eval.value.number", "TypeNumberDynamic", TypeName.DOUBLE);
    nullary("compilerxyz.eval.value.null_", "TypeNullDynamic", ClassName.OBJECT);
    nullary(
        "compilerxyz.eval.value.unique",
        "TypeUnique",
        ClassName.get("compilerxyz.eval", "Unique"));
    nullary(
        "compilerxyz.eval.value.code_value_extern",
        "TypeCodeValueExternDynamic",
        ClassName.get("compilerxyz.eval", "CodeValue"));
    nullary(
        "compilerxyz.eval.value.constkey",
        "TypeConstkeyDynamic",
        ClassName.get("compilerxyz.eval", "ConstKey"));
    nullary(
        "compilerxyz.eval.value.expr",
        "TypeExpr",
        ClassName.get("compilerxyz.eval.expr", "StagedExprCell"));
    nullary("compilerxyz.eval.value.interface_spec", "TypeInterfaceSpec");
    nullary("compilerxyz.eval.value.throw_", "TypeThrowDynamic");
    nullary("compilerxyz.eval.value.panic", "TypePanicDynamic");
    nullary("compilerxyz.eval.value.function", "TypeFunctionAspectDynamic");
    nullary("compilerxyz.eval.value.function", "TypeFunctionCastValueDynamic");
    nullary("compilerxyz.eval.value.list", "TypeListConstructorConst");
    nullary("compilerxyz.eval.value.map", "TypeMapConstructorConst");
    nullary("compilerxyz.eval.value.option", "TypeOptionConstructorConst");
    nullary("compilerxyz.eval.value.eresult", "TypeEResultConstructorConst");
    nullary(
        "compilerxyz.eval.value.typeid",
        "TypeTypeIdDynamic",
        ClassName.get("compilerxyz.eval", "TypeId"));

    // Static-factory descriptors — interfaces where value class == descriptor class.
    factory("compilerxyz.eval", "Type");
    factory("compilerxyz.eval", "ExprContext");
    factory("compilerxyz.eval", "QExprId");
    factory("compilerxyz.eval", "CodeValue");
    factory("compilerxyz.eval", "Code");
    factory("compilerxyz.eval", "ConstKey");
    factory("compilerxyz.eval", "Expr");
    factory("compilerxyz.eval.aspect", "Aspect");
    factory("compilerxyz.eval.push", "StagedExpr");
    factory("compilerxyz.eval.value.code", "CodeDynHandleType", ClassName.OBJECT);
    factory(
        "compilerxyz.eval.value.bind_result",
        "BindResultType",
        ClassName.get("compilerxyz.gen", "AspectBind", "BindResult"));
    factory(
        "compilerxyz.eval.value.object_",
        "ObjectConstOfType",
        ClassName.get("compilerxyz.eval", "Type"));
    factory("compilerxyz.eval.value.source_object", "SourceObjectRefType");
    factory("compilerxyz.eval.value.source_object_new", "SourceObjectNewRefType");
    factory("compilerxyz.eval.value.source_union", "SourceUnionRefType");
    factory("compilerxyz.eval.value.source_union_new", "SourceUnionNewRefType");
    factory("compilerxyz.eval.value.source_function", "SourceFunctionType");
    factory("compilerxyz.eval.value.source_interface", "InterfaceData");
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
        "compilerxyz.eval.value.source_function", "SourceFunctionType");
    delegated(
        "compilerxyz.eval.value.source_object", "TypeSourceObjectDynamic",
        "compilerxyz.eval.value.source_object", "SourceObjectRefType");
    delegated(
        "compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic",
        "compilerxyz.eval.value.source_union", "SourceUnionRefType");
    factory("compilerxyz.gen", "Value");
    factory("compilerxyz.gen", "Binding");
    factory("compilerxyz.gen", "Target");

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
    var entry = leafEntry(descriptor);
    return switch (entry.kind()) {
      case NULLARY_NEW -> CodeBlock.of("new $T()", descriptor);
      case STATIC_FACTORY -> CodeBlock.of("$T.dynamicType()", descriptor);
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
