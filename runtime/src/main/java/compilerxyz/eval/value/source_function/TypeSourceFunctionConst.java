package compilerxyz.eval.value.source_function;







import compilerxyz.annotations.BuiltinFull;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter.Root;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.Binding;
import compilerxyz.gen.BindingSetter;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactDataDefSetFunctionCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.MustCast;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.eval.dynamic.DynRuntime;

@BuiltinFull(uniqueId = 6022L)
public record TypeSourceFunctionConst(SourceFunctionData data)
    implements Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        AspectFunction,
        AspectType,
        ExporterAny {

  @Override
  public BindingDynamicBinding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }


  @Override
  public String typeDesc() {
    return data.description.optUnwrapOr("definition_set_function");
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_FUNCTION_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectType.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
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
                    + data.name.optUnwrapOr("(unnamed)")
                    + "' is not attached to a definition set");
          }
          var argTypeOpt = data.argType;
          if (argTypeOpt.optIsNone()) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + data.name.optUnwrapOr("(unnamed)")
                    + "' missing arg type");
          }
          var argType = argTypeOpt.optUnwrap();
          var retType = data.retType;
          if (data.compiledClassName.optIsNone()) {
            var finishRes = data.finish(xx, loc);
            if (finishRes.hasErrors()) {
              return finishRes.reerr();
            }
          }

          if (data.compiledClassName.optIsNone() && data.functionSpec.optIsSome()) {
            set.finishFunctions();
          }
          var compiledClassName = data.compiledClassName;
          var compiledBytecode = data.compiledBytecode;
          if (compiledClassName.optIsNone() || compiledBytecode.optIsNone()) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + data.name.optUnwrapOr("(unnamed)")
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
                    var argArtRes = argType.artifactableExportArtifact(xx, loc);
                    argArtRes.drainInto(errors);
                    var argArt =
                        argArtRes.valueOrNull() != null
                            ? argArtRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport arg")
                            : nullArtifact();
                    Artifact retArt;
                    if (retType.optIsSome()) {
                      var retArtRes = retType.optUnwrap().artifactableExportArtifact(xx, loc);
                      retArtRes.drainInto(errors);
                      retArt =
                          retArtRes.valueOrNull() != null
                              ? retArtRes.requireValue(
                                  "TypeSourceFunctionConst::exporterExport ret")
                              : nullArtifact();
                    } else {
                      retArt = nullArtifact();
                    }
                    var externalsRes =
                        ExternalCodec.exportExternals(xx, loc, data.capturedExternals);
                    externalsRes.drainInto(errors);
                    var externals =
                        externalsRes.valueOrNull() != null
                            ? externalsRes.requireValue(
                                "TypeSourceFunctionConst::exporterExport externals")
                            : TList.<ArtifactExternal>of();
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
                    return makeBody.apply(ArtifactDataDefSetFunctionCodec.toValue(body));
                  });
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
        }

        private static Artifact nullArtifact() {
          return new Artifact.Body(
              new ArtifactBody(
                  new JsonValueNull(),
                  new Root()));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(
                loc, "TypeSourceFunctionConst requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSetFunction body;
                try {
                  body = ArtifactDataDefSetFunctionCodec.fromValue(raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeSourceFunctionConst data: failed to parse "
                          + "ArtifactDataDefSetFunction: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var funcData = new SourceFunctionData(body.name(), DynRuntime.freshUnique(xx));
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
                    case ArtifactExternal.UniqueRef ignored -> {}
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
                      var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                      funcData.initSetReflectively(set);
                      var argRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.argType());
                      var argVal =
                          argRes.requireValue(
                              "TypeSourceFunctionConst::finalizer arg type");
                      funcData.argType = Opt.some(argVal);
                      var retRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.retType());
                      var retVal =
                          retRes.requireValue(
                              "TypeSourceFunctionConst::finalizer ret type");
                      funcData.retType = Opt.some(retVal);
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
    if (cv.value().optIsNone()) {
      return EResult.err(loc, "TypeSourceFunctionConst::bind: CodeValue has no compile-time value");
    }
    Value v = cv.value().optUnwrap();
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
    if (data.argType.optIsNone()) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: definition set function missing arg type");
    }
    var argType = data.argType.optUnwrap();
    var retType = data.retType;
    var klass =
        JavaInternalName.of(
            DefSetFunctionsEmitter.functionsInternalName(set.defSetId));
    var methodName =
        DefSetFunctionsEmitter.functionMethodName(data.funcIdx);
    var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeSourceFunctionConst::call converted arg");
    var argCodesRes = DynUtil.destructurePositional(xx, loc, argType, converted);
    if (argCodesRes.hasErrors()) return argCodesRes.reerr();
    var argCodes = argCodesRes.requireValue("TypeSourceFunctionConst::call arg codes");

    var descRes =
        TypeFunctionDynamic.descriptorFor(xx, loc, argType, retType, false);
    if (descRes.hasErrors()) return descRes.reerr();
    var invocationRes =
        TypeFunctionDynamic.emitStaticInvocation(
            xx,
            loc,
            klass,
            methodName,
            descRes.requireValue("TypeSourceFunctionConst::call descriptor"),
            false,
            argCodes);
    if (invocationRes.hasErrors()) return invocationRes.reerr();
    var invocation = invocationRes.requireValue("TypeSourceFunctionConst::call invocation");
    if (retType.optIsNone()) {
      return EResult.ok(new CodeValue(Code.of(loc, invocation), EvalOutcome.NO_VALUE));
    }
    var retStackRes =
        GenAspectStackValueDynamic.get(xx, loc, retType.optUnwrap());
    if (retStackRes.hasErrors()) return retStackRes.reerr();
    return EResult.ok(
        retStackRes
            .requireValue("TypeSourceFunctionConst::call ret stack")
            .aspectStackValueDynamicToValue(xx, loc, invocation));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
    return data.exceptionType;
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  private record SourceFunctionConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
      return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
    }

    @Override
    public Binding bindingFork() {
      return this;
    }



    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
      for (var other : others) {
        if (!this.equals(other)) {
          return EResult.err(loc,
              "cannot merge " + getClass().getSimpleName()
                  + " with a differently-typed binding across branches");
        }
      }
      return EResult.ok(this);
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
      return Binding.bindingDrop(xx, loc, this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
      return Binding.elementUseDynamicBind(dynamicKey);
    }
  }

  public record ImportedSourceFunctionConst(SourceFunctionData data) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeSourceFunctionConst(data));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeSourceFunctionConst(data)));
    }
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_FUNCTION_CONST_IDENTITY, TypeSourceFunctionConst.class);
    return oOpt.optIsSome() && data.identity.equals(oOpt.optUnwrap().data.identity);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
