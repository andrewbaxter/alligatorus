package compilerxyz.eval.value;

import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
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

  record StringDynamic() implements TypeRoot {
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

  record AspectDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<aspect-dynamic>";
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

  record AnyBindingDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<any-binding-dynamic>";
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

  record AnyTargetDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<any-target-dynamic>";
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

  record CodeDynCastFn() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "Code dyn cast fn type";
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

  record CodeDynHandleDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "Code dyn handle type (dynamic)";
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

  record CodeEnumDynamic() implements TypeRoot {
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

  record CodeStructDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "Code struct type (dynamic)";
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

  record SourceFunctionDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<definition_set_function_dynamic>";
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

  record SourceInterfaceConst() implements TypeRoot {
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

  record SourceInterfaceDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<interface_ref>";
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

  record SourceObjectConst() implements TypeRoot {
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

  record SourceObjectRefDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<definition_set_object_ref>";
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

  record SourceObjectNewDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<definition_set_object_new-dynamic>";
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

  record SourceUnionConst() implements TypeRoot {
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

  record ExprContextPushDynamic() implements TypeRoot {
    @Override
    public String typeDesc() {
      return "type<ExprContextPush-dynamic>";
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
