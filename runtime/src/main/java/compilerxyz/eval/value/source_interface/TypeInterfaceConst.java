package compilerxyz.eval.value.source_interface;

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
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;

public record TypeInterfaceConst(InterfaceData data)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectType,
        ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.DefsetMember(data.set.defSetId, uniqueId(data.identity));
  }

  public static compilerxyz.eval.Type fromInterfaceData(
      compilerxyz.eval.ExprContext xx, InterfaceData data) {
    return new TypeInterfaceConst(data);
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(
        compilerxyz.eval.value.source_interface.InterfaceData.class);
  }

  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "interface";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceInterfaceConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(data.identity)
        || key.equals(GenIdentities.INTERFACE_CONST_IDENTITY)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
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
    return ExporterRoot.SOURCE_INTERFACE_CONST;
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
          if (!(self instanceof TypeInterfaceConst ic)) {
            return EResult.err(
                loc,
                "TypeInterfaceConst::exporterRootDelegateSelfExport: expected"
                    + " TypeInterfaceConst, got " + self.getClass());
          }
          var iface = ic.data;
          var set = iface.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeInterfaceConst::exporterRootDelegateSelfExport: interface not attached to a"
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
                                "TypeInterfaceConst::exporterExport defSet")
                            : nullArtifact();
                    var methodsOut = new TList<ArtifactDataDefSetInterfaceMethodData>();
                    for (var m : iface.methods) {
                      var argT = m.argType;
                      var retT = m.retType;
                      Artifact argArt;
                      if (argT == null) {
                        argArt = nullArtifact();
                      } else {
                        var res = ArtifactSupport.exportArtifact(xx, loc, argT);
                        res.drainInto(errors);
                        argArt =
                            res.valueOrNull() != null
                                ? res.requireValue(
                                    "TypeInterfaceConst::exporterExport method arg")
                                : nullArtifact();
                      }
                      Artifact retArt;
                      if (retT == null) {
                        retArt = nullArtifact();
                      } else {
                        var res = ArtifactSupport.exportArtifact(xx, loc, retT);
                        res.drainInto(errors);
                        retArt =
                            res.valueOrNull() != null
                                ? res.requireValue(
                                    "TypeInterfaceConst::exporterExport method ret")
                                : nullArtifact();
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
                            iface.builtinKind,
                            iface.compiledBinaryName,
                            methodsOut);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(
                loc, "TypeInterfaceConst requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
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
                      "TypeInterfaceConst data: failed to parse ArtifactDataDefSetInterface: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var iface =
                    new InterfaceData(body.defSetId(), body.identity(), body.builtinKind());
                iface.compiledBinaryName = body.compiledBinaryName();
                iface.description = body.description();
                iface.isAspect = body.isAspect();
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
                              "TypeInterfaceConst::finalizer defSet artifact");
                      if (!(defSetVal instanceof DefinitionSet set)) {
                        throw new IllegalStateException(
                            "TypeInterfaceConst finalizer: defSet artifact resolved to "
                                + defSetVal.getClass());
                      }
                      iface.initSetReflectively(set);
                      var methodsIt = body.methods().iterator();
                      for (var md : iface.methods) {
                        var mBody = methodsIt.next();
                        var argRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, mBody.argType());
                        md.argType =
                            argRes.requireValue(
                                "TypeInterfaceConst::finalizer method arg");
                        var retRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, mBody.retType());
                        md.retType =
                            retRes.requireValue(
                                "TypeInterfaceConst::finalizer method ret");
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
      return EResult.ok(new TypeInterfaceConst(data));
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new TypeInterfaceConst(data));
    }
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeInterfaceConst::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on interface");
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
    var argType = match.argType;
    var retType = match.retType;
    if (argType == null || retType == null) {
      return EResult.err(
          loc, "Interface method '" + keyName + "' is missing arg or return type");
    }
    var fieldType = new TypeInterfaceMethodDynamic(data, match);
    return EResult.ok(
        new CodeValue(
            self.code(),
            Opt.some(ValueDynamic.stack(fieldType))));
  }

  @Override
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    for (var m : data.methods) {
      if (m.name.equals(s.value())) {
        var argType = m.argType;
        var retType = m.retType;
        if (argType != null && retType != null) {
          return Opt.some(new TypeInterfaceMethodDynamic(data, m));
        }
        return Opt.none();
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
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeInterfaceConst::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType instanceof compilerxyz.eval.value.interface_spec.TypeInterfaceSpec
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.TYPE_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType instanceof compilerxyz.eval.value.interface_spec.TypeInterfaceSpec) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast interface_const → interface_spec: source has no runtime code");
      }
      var call = emitInterfaceRefToInterfaceSpecCall(loc, sourceCode);
      return EResult.ok(
          new CodeValue(
              Code.of(loc, call),
              Opt.some(
                  ValueDynamic.stack(
                      new compilerxyz.eval.value.interface_spec.TypeInterfaceSpec()))));
    }
    if (destType.typeAspect(xx, compilerxyz.gen.GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, compilerxyz.gen.GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), Opt.some(ValueDynamic.stack(destType))));
    }
    return EResult.err(loc, "Cannot cast interface_const to " + destType.typeDesc());
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeInterfaceConst(data));
  }

  @Override
  public Type aspectTypeType(Value value) {
    return InterfaceData.dynamicType();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }

  static CodeDynamic emitInterfaceRefToInterfaceSpecCall(QExprId loc, CodeDynamic source) {
    var seq = new JavaBytecodeSequence();
    seq.add(source);
    seq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            JavaInternalName.fromClass(DynRuntime.class),
            "interfaceRefToInterfaceSpec",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(
                    compilerxyz.eval.value.interface_spec.TypeInterfaceSpec.class),
                TList.of(JavaDataDescriptor.fromClass(InterfaceData.class)))));
    return new CodeDynamic(loc, seq);
  }
}
