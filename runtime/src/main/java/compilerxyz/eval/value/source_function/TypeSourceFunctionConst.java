package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.gen.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.TypeId;

public record TypeSourceFunctionConst(SourceFunctionData data)
    implements Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectFunction,
        AspectType,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() {
    var setId = data.setId;
    if (setId == null) {
      throw new IllegalStateException(
          "TypeSourceFunctionConst::typeId: function '" + data.name + "' has no setId");
    }
    return new TypeId.DefsetMember(setId, data.funcIdx);
  }


  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "definition_set_function";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceFunctionConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectFunction.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_FUNCTION_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeSourceFunctionConst fc)) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: expected TypeSourceFunctionConst, got "
                    + self.getClass());
          }
          var data = fc.data;
          var set = data.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + (data.name == null ? "(unnamed)" : data.name)
                    + "' is not attached to a definition set");
          }
          var argType = data.argType;
          if (argType == null) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + (data.name == null ? "(unnamed)" : data.name)
                    + "' missing arg type");
          }
          var retType = data.retType;
          if (retType == null) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + (data.name == null ? "(unnamed)" : data.name)
                    + "' missing ret type");
          }
          if (data.compiledClassName == null) {
            var finishRes = data.finish(xx, loc);
            if (finishRes.hasErrors()) {
              return finishRes.reerr();
            }
          }

          if (data.compiledClassName == null && data.functionSpec != null) {
            set.finishFunctions();
          }
          var compiledClassName = data.compiledClassName;
          var compiledBytecode = data.compiledBytecode;
          if (compiledClassName == null || compiledBytecode == null) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + (data.name == null ? "(unnamed)" : data.name)
                    + "' has not been compiled (finish() returned no source)");
          }
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  data.artifactIdCell,
                  () -> {
                    var defSetArtRes =
                        ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
                    defSetArtRes.drainInto(errors);
                    var defSetArt =
                        defSetArtRes.valueOrNull() != null
                            ? defSetArtRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport defSet")
                            : nullArtifact();
                    var argArtRes = ArtifactSupport.exportArtifact(xx, loc, argType);
                    argArtRes.drainInto(errors);
                    var argArt =
                        argArtRes.valueOrNull() != null
                            ? argArtRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport arg")
                            : nullArtifact();
                    var retArtRes = ArtifactSupport.exportArtifact(xx, loc, retType);
                    retArtRes.drainInto(errors);
                    var retArt =
                        retArtRes.valueOrNull() != null
                            ? retArtRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport ret")
                            : nullArtifact();
                    var externalsRes =
                        ExternalCodec.exportExternals(xx, loc, data.capturedExternals);
                    externalsRes.drainInto(errors);
                    var externals =
                        externalsRes.valueOrNull() != null
                            ? externalsRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport externals")
                            : compilerxyz.util.TList.<ArtifactExternal>of();
                    var body =
                        new ArtifactDataDefSetFunction(
                            defSetArt,
                            set.defSetId,
                            data.name,
                            argArt,
                            retArt,
                            compiledClassName,
                            compiledBytecode,
                            externals);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
          return new Artifact.Body(
              new ArtifactBody(
                  new compilerxyz.safejson.JsonValueNull(),
                  new compilerxyz.artifact.ArtifactBodyExporter.Root()));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(
                loc, "TypeSourceFunctionConst requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSetFunction body;
                try {
                  body = JsonValues.fromValue(ArtifactDataDefSetFunction.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeSourceFunctionConst data: failed to parse "
                          + "ArtifactDataDefSetFunction: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var funcData = new SourceFunctionData(body.defSetId(), body.name());
                funcData.artifactIdCell.set(idValue);
                funcData.compiledClassName = body.compiledClassName();
                funcData.compiledBytecode = body.compiledBytecode();
                var fc = new ImportedSourceFunctionConst(funcData);
                var deps = new TList<Artifact>();
                deps.add(body.defSet());
                deps.add(body.argType());
                deps.add(body.retType());
                for (var ext : body.capturedExternals()) {
                  switch (ext) {
                    case ArtifactExternal.StageLift sl -> deps.add(sl.sourceTypeArtifact());
                    case ArtifactExternal.Type te -> deps.add(te.typeArtifact());
                    case ArtifactExternal.StagedExpr ignored -> {}
                    case ArtifactExternal.Unique ignored -> {}
                  }
                }
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var defSetAny =
                          ArtifactSupport.independentFinalizeImportArtifact(
                              xx2, loc2, body.defSet());
                      var defSetVal =
                          defSetAny.requireValue(
                              "TypeSourceFunctionConst::finalizer defSet artifact");
                      if (!(defSetVal instanceof DefinitionSet set)) {
                        throw new IllegalStateException(
                            "TypeSourceFunctionConst finalizer: def-set artifact resolved to "
                                + defSetVal.getClass()
                                + " (expected DefinitionSet)");
                      }
                      funcData.initSetReflectively(set);
                      var argRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.argType());
                      var argVal =
                          argRes.requireValue(
                              "TypeSourceFunctionConst::finalizer arg type");
                      funcData.argType = argVal;
                      var retRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.retType());
                      var retVal =
                          retRes.requireValue(
                              "TypeSourceFunctionConst::finalizer ret type");
                      funcData.retType = retVal;
                      var extRes =
                          ExternalCodec.importExternals(xx2, loc2, body.capturedExternals());
                      var extVal =
                          extRes.requireValue(
                              "TypeSourceFunctionConst::finalizer externals");
                      funcData.capturedExternals.addAll(extVal);
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(fc, deps, fin));
              });
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeSourceFunctionConst::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceFunctionConstBinding(v)));
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    var set = data.set;
    if (set == null) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: definition set function has no set reference");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    Type argType = data.argType;
    if (argType == null) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: definition set function missing arg type");
    }
    Type retType = data.retType;
    if (retType == null) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: definition set function missing return type");
    }
    var klass =
        compilerxyz.eval.dynamic.JavaInternalName.of(
            compilerxyz.eval.dynamic.DefSetFunctionsEmitter.functionsInternalName(set.defSetId));
    var methodName =
        compilerxyz.eval.dynamic.DefSetFunctionsEmitter.functionMethodName(data.funcIdx);
    var convertedRes = compilerxyz.eval.Eval.typecheckConvert(xx, loc, argType, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeSourceFunctionConst::call converted arg");
    var argCodesRes = DynUtil.destructurePositional(xx, loc, argType, converted);
    if (argCodesRes.hasErrors()) return argCodesRes.reerr();
    var argCodes = argCodesRes.requireValue("TypeSourceFunctionConst::call arg codes");

    var retStackRes =
        compilerxyz.gen.AspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var fnType = new TypeFunctionDynamic(argType, retType, klass, methodName, false);
    var invocationRes = fnType.emitInvocation(xx, loc, argCodes);
    if (invocationRes.hasErrors()) return invocationRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeSourceFunctionConst::call ret stack")
            .aspectStackValueDynamicToValue(
                xx, loc,
                invocationRes.requireValue("TypeSourceFunctionConst::call invocation")));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return data.exceptionType == null ? Opt.none() : Opt.some(data.exceptionType);
  }

  @Override
  public Type aspectTypeType(Value value) {
    return new TypeSourceFunctionConst(data);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  private record SourceFunctionConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
  }

  public record ImportedSourceFunctionConst(SourceFunctionData data) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeSourceFunctionConst(data));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new ValueObjectConst(compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType(), new TypeSourceFunctionConst(data)));
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
