package compilerxyz.eval.value;

import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.util.Opt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.PostDeserializeIdentity;

public sealed interface TypeRoot extends Type, AspectArtifact {

  Opt<ExporterRoot> typeRootArtifactExporterOpt();

  @Override
  default ArtifactExporter exporter() {
    if (typeRootArtifactExporterOpt() instanceof Opt.Some<ExporterRoot> some) {
      return some.value();
    }
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + ": no ArtifactExporter for this TypeRoot variant");
  }

  @Override
  default Opt<Aspect> typeAspect(ExprContext xx, compilerxyz.eval.Unique key) {
    if (key.equals(AspectArtifact.ID) && typeRootArtifactExporterOpt().optIsSome()) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  record StringConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_STRING_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<string-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.STRING_CONST);
    }
  }

  record NullConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NULL_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<null-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NULL_CONST);
    }
  }

  record NullDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NULL_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<null-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NULL_DYNAMIC);
    }
  }

  record BoolConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_BOOL_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<bool-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.BOOL_CONST);
    }
  }

  record NumberConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NUMBER_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<number-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NUMBER_CONST);
    }
  }

  record NumberDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NUMBER_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<number-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NUMBER_DYNAMIC);
    }
  }

  record NumberDynamicBox() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NUMBER_DYNAMIC_BOX_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<number-dynamic-box>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record BoolDynamicBox() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_BOOL_DYNAMIC_BOX_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<bool-dynamic-box>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record StringDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_STRING_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<string-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.STRING_DYNAMIC);
    }
  }

  record AnyConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_ANY_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<any-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record AnyDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_ANY_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<any-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.ANY_DYNAMIC);
    }
  }

  record ConstkeyDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_CONSTKEY_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<constkey>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record Unique() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_UNIQUE_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<unique>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.UNIQUE);
    }
  }

  record Expr() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_EXPR_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<expr>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.EXPR);
    }
  }

  record ExprStagedPlaceholder() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_EXPR_STAGED_PLACEHOLDER_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<stage result>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record FunctionDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_FUNCTION_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<function-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record FunctionAspectDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_FUNCTION_ASPECT_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<function-aspect-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record FunctionCastValueDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_FUNCTION_CAST_VALUE_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<function-cast-value-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record ListDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_LIST_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<list-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.LIST_DYNAMIC);
    }
  }

  record ListNewDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_LIST_NEW_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<list-new-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record ListConstructorConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_LIST_CONSTRUCTOR_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<list-constructor-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record MapDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_MAP_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<map-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.MAP_DYNAMIC);
    }
  }

  record MapNewDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_MAP_NEW_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<map-new-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record MapGetFnDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_MAP_GET_FN_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<map-get-fn-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record MapEntryDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_MAP_ENTRY_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<map-entry-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record MapConstructorConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_MAP_CONSTRUCTOR_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<map-constructor-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record OptionDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_OPTION_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<option-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.OPTION_DYNAMIC);
    }
  }

  record OptionVariantConstructorDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<option-variant-constructor-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.OPTION_VARIANT_CONSTRUCTOR_DYNAMIC);
    }
  }

  record OptionConstructorConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_OPTION_CONSTRUCTOR_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<option-constructor-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record PanicDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_PANIC_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<panic-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record ThrowDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_THROW_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<throw-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record BoolDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_BOOL_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<bool-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.BOOL_DYNAMIC);
    }
  }

  record CodeEnumDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_CODE_ENUM_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "Code enum type (dynamic)";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceFunctionConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_FUNCTION_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<definition_set_function>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_FUNCTION_CONST);
    }
  }

  record SourceInterfaceConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_INTERFACE_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<interface>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_INTERFACE_CONST);
    }
  }

  record SourceObjectConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_OBJECT_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<object>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_OBJECT_CONST);
    }
  }

  record SourceObjectDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_OBJECT_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<object-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceObjectNewConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_OBJECT_NEW_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<definition_set_object_new>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_OBJECT_NEW_CONST);
    }
  }

  record SourceUnionConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_UNION_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<union>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_UNION_CONST);
    }
  }

  record SourceUnionDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_UNION_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<union-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceUnionRefDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_UNION_REF_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<union-ref>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceUnionNewConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_UNION_NEW_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<union_new>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_UNION_NEW_CONST);
    }
  }

  record SourceUnionNewDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_UNION_NEW_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<union_new-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record CodeValueExternDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_CODE_VALUE_EXTERN_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<CodeValueExtern-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record EResultDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_ERESULT_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<eresult-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record EResultVariantConstructorDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_ERESULT_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<eresult-variant-constructor-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record EResultConstructorConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_ERESULT_CONSTRUCTOR_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<eresult-constructor-const>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceVariantConstructorDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<source-variant-constructor-dynamic>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record InterfaceSpec() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_INTERFACE_SPEC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<interface-spec>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
  }

  record SourceExportsLiftedConst() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_SOURCE_EXPORTS_LIFTED_CONST_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<definition_set_exports>";
    }

    @Override
    public Type valueTypeOf() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_EXPORTS_LIFTED_CONST);
    }
  }
}
