package compilerxyz.eval.value.source_exports;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectField;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.ExprErr;
import compilerxyz.safejson.JsonValues;
import compilerxyz.eval.TypeId;

public record TypeSourceExportsLiftedConst(SourceExportsData set)
    implements Type, AspectArtifact, AspectBind, AspectField, ExporterAny {

  @Override
  public TypeId typeId() {
    if (set.setId == null) {
      throw new IllegalStateException(
          "TypeSourceExportsLiftedConst::typeId: SourceExportsData has no def-set id");
    }
    return new TypeId.DefsetMember(set.setId, 0L);
  }


  @Override
  public String typeDesc() {
    return "definition_set_exports";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceExportsLiftedConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectField.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_EXPORTS_LIFTED_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeSourceExportsLiftedConst exports)) {
            return EResult.err(
                loc,
                "TypeSourceExportsLiftedConst::exporterRootDelegateSelfExport: expected "
                    + "TypeSourceExportsLiftedConst, got "
                    + self.getClass());
          }
          var setId = exports.set.setId;
          if (setId == null) {
            return EResult.err(
                loc,
                "TypeSourceExportsLiftedConst::exporterRootDelegateSelfExport: SourceExportsData has no def-set id");
          }
          var liveSet = xx.vx.definitionSets.get(setId);
          if (liveSet == null) {
            return EResult.err(
                loc,
                "TypeSourceExportsLiftedConst::exporterRootDelegateSelfExport: def-set "
                    + setId
                    + " not registered in EvalContext.definitionSets");
          }
          var errors = new TList<ExprErr>();
          var defSetArtRes = ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, liveSet);
          if (defSetArtRes.hasErrors()) {
            defSetArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var defSetArt =
              defSetArtRes.requireValue(
                  "TypeSourceExportsLiftedConst::exporterExport defSet artifact");
          var body = new ArtifactDataDefSetExportsLifted(defSetArt, setId);
          var artifact =
              new Artifact.Body(
                  makeBody.apply(JsonValues.toValue(body)));
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          ArtifactDataDefSetExportsLifted body;
          try {
            body =
                JsonValues.fromValue(
                    ArtifactDataDefSetExportsLifted.class, data);
          } catch (RuntimeException e) {
            return EResult.err(
                loc,
                "TypeSourceExportsLiftedConst::exporterRootDelegateSelfFromArtifactData: failed to parse "
                    + "ArtifactDataDefSetExportsLifted: "
                    + e.getMessage());
          }
          var errors = new TList<ExprErr>();
          var importRes =
              ArtifactSupport.importArtifact(xx, loc, body.defSet());
          if (importRes.hasErrors()) {
            importRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var imported =
              importRes.requireValue(
                  "TypeSourceExportsLiftedConst::exporterFromArtifactData imported");
          if (!(imported instanceof DefinitionSet ds)) {
            return EResult.err(
                loc,
                "TypeSourceExportsLiftedConst::exporterRootDelegateSelfFromArtifactData: def-set artifact "
                    + "resolved to "
                    + imported.getClass()
                    + " (expected DefinitionSet)");
          }
          xx.vx.definitionSets.putIfAbsent(ds.defSetId, ds);
          var exportsData = new SourceExportsData(ds.defSetId, ds.functions, ds.objects);
          ExporterAny out = new TypeSourceExportsLiftedConst(exportsData);
          if (errors.isEmpty()) {
            return EResult.ok(out);
          }
          return EResult.mixed(out, errors);
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceExportsLiftedConst::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceExportsConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeSourceExportsLiftedConst::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, ck.toCanonical() + " not found in definition set");
    }
    String keyName = keyStr.value();
    for (var funcData : set.functions) {
      if (keyName.equals(funcData.name)) {
        Type funcType = new TypeSourceFunctionConst(funcData);
        return EResult.ok(
            new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(funcType))));
      }
    }
    for (var objData : set.objects) {
      if (keyName.equals(objData.name)) {
        Type ctorType = new TypeSourceObjectNewDynamic(objData);
        return EResult.ok(
            new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(ctorType))));
      }
    }
    return EResult.err(loc, "'" + keyName + "' not found in definition set");
  }

  @Override
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    for (var funcData : set.functions) {
      if (s.value().equals(funcData.name)) {
        return Opt.some(new TypeSourceFunctionConst(funcData));
      }
    }
    for (var objData : set.objects) {
      if (s.value().equals(objData.name)) {
        return Opt.some(new TypeSourceObjectNewDynamic(objData));
      }
    }
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  private record SourceExportsConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
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
