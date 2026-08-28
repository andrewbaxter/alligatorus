package compilerxyz.eval.value.unique;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinFull;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;
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
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import java.util.function.Function;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.value.bool_.TypeBoolConst;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.util.MustCast;

@BuiltinFull(uniqueId = 6003L)
public record TypeUniqueDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectOperatorEq,
        AspectStackValueDynamic,
        AspectLift,
        AspectBindDynamic,
        ExporterAny {


  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromClass(compilerxyz.eval.Unique.class);
  }

  @Override
  public String typeDesc() {
    return "unique";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectOperatorEq.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.UNIQUE;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          JsonValue data;
          if (self instanceof ValueObjectConst v
              && v.data() instanceof Unique u) {
            data = encodeUnique(u);
          } else if (self instanceof TypeUniqueDynamic) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeUniqueDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull n -> EResult.ok(new TypeUniqueDynamic());
            case JsonValueObject obj -> {
              var decoded = decodeUnique(loc, obj);
              if (decoded.hasErrors()) {
                yield EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: malformed");
              }
              yield EResult.ok(
                  new ValueObjectConst(
                      new TypeUniqueDynamic(),
                      decoded.requireValue(
                          "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData decoded")));
            }
            case JsonValueBoolean b -> EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            case JsonValueNumber n -> EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            case JsonValueString s -> EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
            case JsonValueArray a -> EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  private static JsonValue encodeUnique(Unique u) {
    var m = new TOrderedMap<String, JsonValue>();
    switch (u) {
      case Unique.Builtin b -> {
        var inner = new TOrderedMap<String, JsonValue>();
        inner.put("id", new JsonValueNumber(b.id()));
        m.put("builtin", new JsonValueObject(inner));
      }
      case Unique.Source s -> {
        var inner = new TOrderedMap<String, JsonValue>();
        var ci = new TOrderedMap<String, JsonValue>();
        ci.put("import_state", new JsonValueNumber(s.compileId().importState().value()));
        ci.put("source", new JsonValueNumber(s.compileId().source().value()));
        inner.put("compile_id", new JsonValueObject(ci));
        inner.put("id", new JsonValueNumber(s.id()));
        m.put("source", new JsonValueObject(inner));
      }
    }
    return new JsonValueObject(m);
  }

  private static EResult<Unique> decodeUnique(QExprId loc, JsonValueObject obj) {
    if (obj.entries().size() != 1) return EResult.err(loc, "expected tagged object");
    var entry = obj.entries().entrySet().iterator().next();
    var tag = entry.getKey();
    JsonValueObject inner;
    switch (entry.getValue()) {
      case JsonValueObject o -> inner = o;
      case JsonValueNull jn -> { return EResult.err(loc, "expected inner object"); }
      case JsonValueBoolean b -> { return EResult.err(loc, "expected inner object"); }
      case JsonValueNumber n -> { return EResult.err(loc, "expected inner object"); }
      case JsonValueString s -> { return EResult.err(loc, "expected inner object"); }
      case JsonValueArray a -> { return EResult.err(loc, "expected inner object"); }
    }
    return switch (tag) {
      case "builtin" -> {
        var idRaw = inner.entries().get("id");
        if (idRaw == null) yield EResult.err(loc, "builtin: missing id");
        else yield switch (idRaw) {
          case JsonValueNumber n -> EResult.ok(new Unique.Builtin((long) n.value()));
          case JsonValueNull jn -> EResult.err(loc, "builtin: missing id");
          case JsonValueBoolean b -> EResult.err(loc, "builtin: missing id");
          case JsonValueString s -> EResult.err(loc, "builtin: missing id");
          case JsonValueArray a -> EResult.err(loc, "builtin: missing id");
          case JsonValueObject o -> EResult.err(loc, "builtin: missing id");
        };
      }
      case "source" -> {
        var ciRaw = inner.entries().get("compile_id");
        var idRaw = inner.entries().get("id");
        if (ciRaw == null || idRaw == null) yield EResult.err(loc, "source: malformed compile_id/id");
        JsonValueObject ci;
        switch (ciRaw) {
          case JsonValueObject o -> ci = o;
          case JsonValueNull jn -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueBoolean b -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueNumber n -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueString s -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueArray a -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
        }
        long nVal;
        switch (idRaw) {
          case JsonValueNumber n -> nVal = (long) n.value();
          case JsonValueNull jn -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueBoolean b -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueString s -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueArray a -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueObject o -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
        }
        var isRaw = ci.entries().get("import_state");
        var srcRaw = ci.entries().get("source");
        if (isRaw == null || srcRaw == null) yield EResult.err(loc, "source: malformed compile_id/id");
        long isVal;
        switch (isRaw) {
          case JsonValueNumber n -> isVal = (long) n.value();
          case JsonValueNull jn -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueBoolean b -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueString s -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueArray a -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueObject o -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
        }
        long srcVal;
        switch (srcRaw) {
          case JsonValueNumber n -> srcVal = (long) n.value();
          case JsonValueNull jn -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueBoolean b -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueString s -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueArray a -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
          case JsonValueObject o -> { yield EResult.err(loc, "source: malformed compile_id/id"); }
        }
        yield EResult.ok(
            new Unique.Source(
                new CompileId(
                    new ImportStateId(isVal), new SourceId(srcVal)),
                nVal));
      }
      default -> EResult.err(loc, "unknown unique tag: " + tag);
    };
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeUniqueDynamic());
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
    var u = MustCast.mustCast(Unique.class, externalHostValue.optUnwrap());
    return EResult.ok(new ValueObjectConst(new TypeUniqueConst(), u));
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var b = extractUnique(base);
    var r = extractUnique(reference);
    if (b.optIsSome() && r.optIsSome()) {
      return EResult.ok(
          new CodeValue(Code.none(loc), EvalOutcome.ofValue(
              new ValueObjectConst(
                  new TypeBoolConst(),
                  b.optUnwrap().equals(r.optUnwrap())))));
    }
    if (base.value().optIsNone() || reference.value().optIsNone()) {
      return EResult.err(loc, "TypeUniqueDynamic::eq: operand has no value");
    }
    var baseAspRes = GenAspectConsumeDynamic.get(xx, loc, base.value().optUnwrap().valueType());
    if (baseAspRes.hasErrors()) return baseAspRes.reerr();
    var baseDyn = baseAspRes.requireValue("TypeUniqueDynamic::eq base aspect").aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: base lowering failed");
    var refAspRes = GenAspectConsumeDynamic.get(xx, loc, reference.value().optUnwrap().valueType());
    if (refAspRes.hasErrors()) return refAspRes.reerr();
    var refDyn = refAspRes.requireValue("TypeUniqueDynamic::eq ref aspect").aspectConsumeDynamicToDynamic(xx, loc, reference);
    if (refDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: reference lowering failed");
    var _eueBase = baseDyn.requireValue("TypeUniqueDynamic::eq base dynamic");
    var _eueRef = refDyn.requireValue("TypeUniqueDynamic::eq reference dynamic");
    var _eueSeq = new JavaBytecodeSequence();
    _eueSeq.add(_eueBase);
    _eueSeq.add(_eueRef);
    _eueSeq.add(JavaBytecodeUtils.callMethod(
        loc,
        JavaInternalName.fromClass(Object.class),
        "equals",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.DESC_BOOL,
            TList.of(JavaDataDescriptor.fromClass(Object.class)))));
    var call = new CodeDynamic(loc, _eueSeq);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, call),
            EvalOutcome.ofValue(ValueDynamic.stack(new TypeBoolDynamic(), 1))));
  }

  private static Opt<Unique> extractUnique(CodeValue cv) {
    if (cv.value().optIsSome()
        && cv.value().optUnwrap() instanceof ValueObjectConst v
        && v.data() instanceof Unique u) {
      return Opt.some(u);
    }
    return Opt.none();
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.code().codeDynamic() != null) return EResult.ok(cv);
    if (cv.value().optIsNone()) return EResult.ok(cv);
    var srcType = cv.value().optUnwrap().valueType();
    if (srcType instanceof TypeUniqueDynamic) return EResult.ok(cv);
    var asp = GenAspectConsumeDynamic.getMaybe(xx, srcType);
    if (asp.optIsNone()) return EResult.ok(cv);
    var dynRes = asp.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv);
    if (dynRes.hasErrors()) return dynRes.reerr();
    return EResult.ok(new CodeValue(Code.of(loc, dynRes.requireValue("TypeUniqueDynamic::typeCastFrom")), cv.outcome()));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(cv);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeUniqueDynamic());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
