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
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
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
import java.util.IdentityHashMap;
import java.util.Map;
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
  public TypeId typeId() {
    if (!(data.bindingId instanceof BindingId.Def def)) {
      throw new IllegalStateException(
          "TypeSourceFunctionConst::typeId: bindingId is not BindingId.Def for '"
              + data.name
              + "'");
    }
    return new TypeId.DefsetMember(def.defSet(), def.index());
  }


  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "definition_set_function";
  }

  @Override
  public Type valueTypeOf() {
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

  private static final Map<SourceFunctionData, ArtifactSupport.ArtifactIdHolder> INDEPENDENT_CELLS =
      java.util.Collections.synchronizedMap(new IdentityHashMap<>());

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
          var bindingId = data.bindingId;
          if (!(bindingId instanceof BindingId.Def def)) {
            return EResult.err(
                loc,
                "TypeSourceFunctionConst::exporterRootDelegateSelfExport: function '"
                    + (data.name == null ? "(unnamed)" : data.name)
                    + "' has no BindingId.Def registration (got "
                    + bindingId
                    + ")");
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
            ImportInstall.registerStaticFunctionDefs(xx, set);
            set.finishFunctions(xx.vx.ix.builtinDynamics, xx.vx.ix.staticFunctionDefs);
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
          var defSetArtRes = ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
          if (defSetArtRes.hasErrors()) {
            defSetArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var defSetArt =
              defSetArtRes.requireValue(
                  "TypeSourceFunctionConst::exporterExport defSet artifact");
          var argArtRes = ArtifactSupport.exportArtifact(xx, loc, argType);
          if (argArtRes.hasErrors()) {
            argArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var argArt =
              argArtRes.requireValue("TypeSourceFunctionConst::exporterExport arg artifact");
          var retArtRes = ArtifactSupport.exportArtifact(xx, loc, retType);
          if (retArtRes.hasErrors()) {
            retArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var retArt =
              retArtRes.requireValue("TypeSourceFunctionConst::exporterExport ret artifact");
          var externalsRes = ExternalCodec.exportExternals(xx, loc, data.capturedExternals);
          if (externalsRes.hasErrors()) {
            externalsRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var externals =
              externalsRes.requireValue(
                  "TypeSourceFunctionConst::exporterExport externals");
          var body =
              new ArtifactDataDefSetFunction(
                  defSetArt,
                  def.defSet(),
                  def.index(),
                  data.name,
                  argArt,
                  retArt,
                  compiledClassName,
                  compiledBytecode,
                  externals);
          var cell =
              INDEPENDENT_CELLS.computeIfAbsent(
                  data, k -> new ArtifactSupport.ArtifactIdHolder());
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  cell,
                  () -> makeBody.apply(JsonValues.toValue(body)));
          if (errors.isEmpty()) {
            return EResult.ok(artifact);
          }
          return EResult.mixed(artifact, errors);
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
                funcData.bindingId = new BindingId.Def(body.defSetId(), body.entryId());
                funcData.compiledClassName = body.compiledClassName();
                funcData.compiledBytecode = body.compiledBytecode();
                var fc = new ImportedSourceFunctionConst(funcData);
                var deps = new TList<Artifact>();
                deps.add(body.defSet());
                deps.add(body.argType());
                deps.add(body.retType());
                for (var ext : body.capturedExternals()) {
                  deps.add(ext.typeArtifact());
                  deps.add(ext.valueArtifact());
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
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeSourceFunctionConst::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceFunctionConstBinding(v)));
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    BindingId scopeId = data.bindingId;
    if (scopeId == null) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: definition set function not registered");
    }
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
    if (!(scopeId instanceof BindingId.Def def)) {
      return EResult.err(
          loc, "TypeSourceFunctionConst::call: bindingId is not a Def");
    }
    var klass =
        compilerxyz.eval.dynamic.JavaInternalName.of(
            compilerxyz.eval.dynamic.DefSetFunctionsEmitter.functionsInternalName(def.defSet()));
    var methodName =
        compilerxyz.eval.dynamic.DefSetFunctionsEmitter.functionMethodName(def.index());
    var convertedRes = compilerxyz.eval.Eval.typecheckConvert(xx, loc, argType, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeSourceFunctionConst::call converted arg");
    var argElementsRes = DynUtil.destructureCallArg(xx, loc, argType, converted);
    if (argElementsRes.hasErrors()) return argElementsRes.reerr();
    var argElements = argElementsRes.requireValue("TypeSourceFunctionConst::call arg elements");

    var argDescs = new TList<compilerxyz.eval.dynamic.JavaDataDescriptor>(argElements.size());
    for (var e : argElements) argDescs.add(e.jvmDesc());
    var retDescRes = jvmDescOf(xx, loc, retType);
    if (retDescRes.hasErrors()) return retDescRes.reerr();
    var retDesc = retDescRes.requireValue("TypeSourceFunctionConst::call retDesc");
    var descriptor =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(retDesc, argDescs);
    var retStackRes =
        compilerxyz.gen.AspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var argCodes = new TList<compilerxyz.eval.dynamic.CodeDynamic>(argElements.size());
    for (var e : argElements) argCodes.add(e.code());
    var fnType = new TypeFunctionDynamic(argType, retType, klass, methodName, descriptor, false);
    var invocation = fnType.emitInvocation(loc, xx.vx, argCodes);
    return EResult.ok(
        retStackRes
            .requireValue("TypeSourceFunctionConst::call ret stack")
            .toValue(loc, invocation));
  }

  private static EResult<compilerxyz.eval.dynamic.JavaDataDescriptor> jvmDescOf(
      ExprContext xx, QExprId loc, Type t) {
    var consumeRes =
        compilerxyz.gen.AspectConsumeDynamic.get(xx, loc, t);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    return EResult.ok(
        consumeRes.requireValue("jvmDescOf consume").consumeDynamicJvmClass());
  }

  @Override
  public Opt<Type> exceptionType() {
    return data.exceptionType == null ? Opt.none() : Opt.some(data.exceptionType);
  }

  @Override
  public Type type_(Value value) {
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
      return EResult.ok(new ValueObjectConst(new TypeSourceFunctionConst(data)));
    }
  }
}
