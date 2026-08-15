package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinLight;
import compilerxyz.annotations.CodegenDescriptor;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.ExprErr;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Result;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenCodegenDescriptors;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;
import compilerxyz.eval.TypeAspect;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenInterfaceData;
import compilerxyz.util.MustCast;

@BuiltinLight(uniqueId = 6004L)
public record TypeSourceInterfaceDynamic(InterfaceData data)
    implements
        TypeAspect,
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectType,
        ExporterAny {

  @Override
  public Unique typeAspectId() {
    if (data.aspectId.optIsNone()) {
      throw new IllegalStateException(
          "TypeSourceInterfaceDynamic::typeAspectId: interface is not an aspect (no aspect id)");
    }
    return data.aspectId.optUnwrap();
  }


  @CodegenDescriptor
  public InterfaceData getInterfaceData() {
    return data;
  }

  public static Type dynamicType() {
    return TypeTypeSourceInterfaceDynamic.GENERIC;
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    var binaryName = data.compiledBinaryName;
    if (binaryName == null) {
      return JavaDataDescriptor.fromClass(TypeSourceInterfaceDynamic.class);
    }
    return JavaDataDescriptor.fromInternalName(
        JavaInternalName.of(binaryName.replace('.', '/')));
  }


  @Override
  public String typeDesc() {
    return data.description.optUnwrapOr("interface");
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(data.identity)
        || key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
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
    return ExporterRoot.SOURCE_INTERFACE_DYNAMIC;
  }

  private static Artifact nullArtifact() {
    return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeSourceInterfaceDynamic ic)) {
            return EResult.err(
                loc,
                "TypeSourceInterfaceDynamic::exporterRootDelegateSelfExport: expected"
                    + " TypeSourceInterfaceDynamic, got " + self.getClass());
          }
          var iface = ic.data;
          var set = iface.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeSourceInterfaceDynamic::exporterRootDelegateSelfExport: interface not attached to a"
                    + " definition set");
          }
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  iface.artifactIdCell,
                  () -> {
                    var defSetArtRes =
                        ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
                    defSetArtRes.drainInto(errors);
                    var defSetArt =
                        defSetArtRes.valueOrNull() != null
                            ? defSetArtRes.requireValue(
                                "TypeSourceInterfaceDynamic::exporterExport defSet")
                            : nullArtifact();
                    var methodsOut = new TList<ArtifactDataDefSetInterfaceMethodData>();
                    for (var m : iface.methods) {
                      var argT = m.argType;
                      var retT = m.retType;
                      Artifact argArt;
                      {
                        var res = argT.artifactableExportArtifact(xx, loc);
                        res.drainInto(errors);
                        argArt =
                            res.valueOrNull() != null
                                ? res.requireValue(
                                    "TypeSourceInterfaceDynamic::exporterExport method arg")
                                : nullArtifact();
                      }
                      Artifact retArt;
                      if (retT.optIsSome()) {
                        var res = retT.optUnwrap().artifactableExportArtifact(xx, loc);
                        res.drainInto(errors);
                        retArt =
                            res.valueOrNull() != null
                                ? res.requireValue(
                                    "TypeSourceInterfaceDynamic::exporterExport method ret")
                                : nullArtifact();
                      } else {
                        retArt = nullArtifact();
                      }
                      methodsOut.add(
                          new ArtifactDataDefSetInterfaceMethodData(
                              m.name, argArt, retArt, TList.from(m.dataFieldNames)));
                    }
                    var body =
                        new ArtifactDataDefSetInterface(
                            defSetArt,
                            set.defSetId,
                            iface.identity,
                            iface.description,
                            iface.isAspect,
                            iface.aspectId,
                            iface.builtinKind,
                            iface.compiledBinaryName,
                            iface.compiledBytecode,
                            methodsOut);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(
                loc, "TypeSourceInterfaceDynamic requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataDefSetInterface body;
                try {
                  body = JsonValues.fromValue(ArtifactDataDefSetInterface.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeSourceInterfaceDynamic data: failed to parse ArtifactDataDefSetInterface: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var iface =
                    new InterfaceData(Opt.some(body.defSetId()), body.identity(), body.builtinKind());
                iface.compiledBinaryName = body.compiledBinaryName();
                iface.compiledBytecode = body.compiledBytecode();
                iface.description = body.description();
                iface.isAspect = body.isAspect();
                iface.aspectId = body.aspectId();
                iface.artifactIdCell.set(idValue);
                for (var m : body.methods()) {
                  var md = new InterfaceMethodData(m.name());
                  md.dataFieldNames.addAll(m.dataFieldNames());
                  iface.methods.add(md);
                }
                var imported = new ImportedInterfaceConst(iface);
                var deps = new TList<Artifact>();
                deps.add(body.defSet());
                for (var m : body.methods()) {
                  deps.add(m.argType());
                  deps.add(m.retType());
                }
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var defSetAny =
                          ArtifactSupport.independentFinalizeImportArtifact(
                              xx2, loc2, body.defSet());
                      var defSetVal =
                          defSetAny.requireValue(
                              "TypeSourceInterfaceDynamic::finalizer defSet artifact");
                      var set = MustCast.mustCast(DefinitionSet.class, defSetVal);
                      iface.initSetReflectively(set);
                      var methodsIt = body.methods().iterator();
                      for (var md : iface.methods) {
                        var mBody = methodsIt.next();
                        var argRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, mBody.argType());
                        md.argType =
                            argRes.requireValue(
                                "TypeSourceInterfaceDynamic::finalizer method arg");
                        var retRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, mBody.retType());
                        md.retType =
                            Opt.some(
                                retRes.requireValue(
                                    "TypeSourceInterfaceDynamic::finalizer method ret"));
                      }
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(imported, deps, fin));
              });
        }
      };

  public record ImportedInterfaceConst(InterfaceData data) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(new TypeSourceInterfaceDynamic(data));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new ValueObjectConst(new TypeTypeSourceInterfaceDynamic(Opt.some(new TypeSourceInterfaceDynamic(data))), new TypeSourceInterfaceDynamic(data)));
    }
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, "TypeSourceInterfaceDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    var ck = ckOpt.optUnwrap();
    ConstKey.Str keyStr;
    switch (ck) {
      case ConstKey.Str s -> keyStr = s;
      case ConstKey.Uint u -> { return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on interface"); }
    }
    String keyName = keyStr.value();
    InterfaceMethodData match = null;
    for (var m : data.methods) {
      if (m.name.equals(keyName)) {
        match = m;
        break;
      }
    }
    if (match == null) {
      return EResult.err(loc, "Field '" + keyName + "' not found on interface");
    }
    var fieldType = new TypeInterfaceMethodDynamic(data, match);
    if (self.value().optIsNone()
        || !(self.value().optUnwrap() instanceof ValueDynamic ifmVd)) {
      return EResult.err(
          loc, "TypeSourceInterfaceDynamic::field: method receiver has no dynamic source");
    }
    return EResult.ok(
        new CodeValue(
            self.code(),
            EvalOutcome.ofValue(
                new ValueDynamic(ifmVd.source(), fieldType))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    ConstKey.Str s;
    switch (key) {
      case ConstKey.Str ks -> s = ks;
      case ConstKey.Uint u -> { return Opt.none(); }
    }
    for (var m : data.methods) {
      if (m.name.equals(s.value())) {
        return Opt.some(new TypeInterfaceMethodDynamic(data, m));
      }
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeSourceInterfaceDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY).optIsSome()
        || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().codeDynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast source_interface_dynamic → interface_ref: source has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(sourceCode);
      seq.add(
          JavaBytecodeUtils.callMethod(
              loc, GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_DATA));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
    return EResult.err(loc, "Cannot cast interface_const to " + destType.typeDesc());
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeSourceInterfaceDynamic(data));
  }

  @Override
  public Type aspectTypeType(Value value) {
    return GenInterfaceData.dynamicType();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(new TypeTypeSourceInterfaceDynamic(Opt.some(this)), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, data.identity).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }


  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
