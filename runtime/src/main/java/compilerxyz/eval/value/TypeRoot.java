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
  default ArtifactExporter aspectArtifactExporter() {
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.STRING_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NULL_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NULL_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.BOOL_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NUMBER_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.NUMBER_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.STRING_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.UNIQUE);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.EXPR);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.LIST_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.MAP_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.OPTION_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.OPTION_VARIANT_CONSTRUCTOR_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.BOOL_DYNAMIC);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_FUNCTION_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_INTERFACE_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_OBJECT_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_OBJECT_NEW_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_UNION_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.some(ExporterRoot.SOURCE_UNION_NEW_CONST);
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
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
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
  }

  record ListPushDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_LIST_PUSH_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<list-push-dynamic>";
    }

    @Override
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
  }

  record ListJoinDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_LIST_JOIN_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<list-join-dynamic>";
    }

    @Override
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
  }

  record NumberToStringDynamic() implements TypeRoot {
    @Override
    public TypeId typeId() { return GenTypeIds.TYPE_ROOT_NUMBER_TO_STRING_DYNAMIC_TYPE_ID; }

    @Override
    public String typeDesc() {
      return "type<number-to-string-dynamic>";
    }

    @Override
    public Type valueType() {
      return this;
    }

    @Override
    public Opt<ExporterRoot> typeRootArtifactExporterOpt() {
      return Opt.none();
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }
    @Override
    public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
      return false;
    }
    @Override
    public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
      return false;
    }
    @Override
    public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
      throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }
  }
}
