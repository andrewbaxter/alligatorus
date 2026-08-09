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
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
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

@BuiltinFull(uniqueId = 6003L)
public record TypeUniqueDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        compilerxyz.gen.AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectOperatorEq,
        AspectStackValueDynamic,
        AspectLift,
        AspectBindDynamic,
        ExporterAny {


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(compilerxyz.eval.Unique.class);
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
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
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
          if (self instanceof compilerxyz.eval.value.object_.ValueObjectConst v
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
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeUniqueDynamic());
          }
          if (data instanceof JsonValueObject obj) {
            var decoded = decodeUnique(loc, obj);
            if (decoded.hasErrors()) {
              return EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: malformed");
            }
            return EResult.ok(
                new compilerxyz.eval.value.object_.ValueObjectConst(
                    new TypeUniqueDynamic(),
                    decoded.requireValue(
                        "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData decoded")));
          }
          return EResult.err(loc, "TypeUniqueDynamic::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
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
    if (!(entry.getValue() instanceof JsonValueObject inner)) {
      return EResult.err(loc, "expected inner object");
    }
    return switch (tag) {
      case "builtin" -> {
        if (inner.entries().get("id") instanceof JsonValueNumber n) {
          yield EResult.ok(new Unique.Builtin((long) n.value()));
        }
        yield EResult.err(loc, "builtin: missing id");
      }
      case "source" -> {
        if (inner.entries().get("compile_id") instanceof JsonValueObject ci
            && inner.entries().get("id") instanceof JsonValueNumber n
            && ci.entries().get("import_state") instanceof JsonValueNumber is
            && ci.entries().get("source") instanceof JsonValueNumber src) {
          yield EResult.ok(
              new Unique.Source(
                  new CompileId(
                      new ImportStateId((long) is.value()), new SourceId((long) src.value())),
                  (long) n.value()));
        }
        yield EResult.err(loc, "source: malformed compile_id/id");
      }
      default -> EResult.err(loc, "unknown unique tag: " + tag);
    };
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeUniqueDynamic());
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    var u = compilerxyz.util.MustCast.mustCast(Unique.class, externalHostValue);
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(new TypeUniqueConst(), u));
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var b = extractUnique(base);
    var r = extractUnique(reference);
    if (b instanceof Opt.Some<Unique> bs && r instanceof Opt.Some<Unique> rs) {
      return EResult.ok(
          new CodeValue(Code.none(loc), EvalOutcome.ofValue(
              new compilerxyz.eval.value.object_.ValueObjectConst(
                  new compilerxyz.eval.value.bool_.TypeBoolConst(),
                  bs.value().equals(rs.value())))));
    }
    if (!(base.value() instanceof Opt.Some<Value> baseSome)
        || !(reference.value() instanceof Opt.Some<Value> refSome)) {
      return EResult.err(loc, "TypeUniqueDynamic::eq: operand has no value");
    }
    var baseAspRes = GenAspectConsumeDynamic.get(xx, loc, baseSome.value().valueType());
    if (baseAspRes.hasErrors()) return baseAspRes.reerr();
    var baseDyn = baseAspRes.requireValue("TypeUniqueDynamic::eq base aspect").aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: base lowering failed");
    var refAspRes = GenAspectConsumeDynamic.get(xx, loc, refSome.value().valueType());
    if (refAspRes.hasErrors()) return refAspRes.reerr();
    var refDyn = refAspRes.requireValue("TypeUniqueDynamic::eq ref aspect").aspectConsumeDynamicToDynamic(xx, loc, reference);
    if (refDyn.hasErrors()) return EResult.err(loc, "TypeUniqueDynamic::eq: reference lowering failed");
    var call =
        emitUniqueEqCall(
            loc,
            xx.vx,
            baseDyn.requireValue("TypeUniqueDynamic::eq base dynamic"),
            refDyn.requireValue("TypeUniqueDynamic::eq reference dynamic"));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, call),
            EvalOutcome.ofValue(ValueDynamic.stack(new TypeBoolDynamic(), 1))));
  }

  private static CodeDynamic emitUniqueEqCall(
      QExprId loc, compilerxyz.eval.EvalContext vx, CodeDynamic base, CodeDynamic reference) {
    var seq = new JavaBytecodeSequence();
    seq.add(base);
    seq.add(reference);
    seq.add(
        JavaBytecodeUtils.callMethod(
            loc,
            JavaInternalName.fromClass(Object.class),
            "equals",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_BOOL,
                TList.of(JavaDataDescriptor.fromClass(Object.class)))));
    return new CodeDynamic(loc, seq);
  }

  private static Opt<Unique> extractUnique(CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Unique u) {
      return Opt.some(u);
    }
    return Opt.none();
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY) instanceof Opt.Some<Aspect>;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.code().dynamic() != null) return EResult.ok(cv);
    if (!(cv.value() instanceof Opt.Some<Value> some)) return EResult.ok(cv);
    var srcType = some.value().valueType();
    if (srcType instanceof TypeUniqueDynamic) return EResult.ok(cv);
    var asp = GenAspectConsumeDynamic.getMaybe(xx, srcType);
    if (!(asp instanceof Opt.Some<AspectConsumeDynamic> aspSome)) return EResult.ok(cv);
    var dynRes = aspSome.value().aspectConsumeDynamicToDynamic(xx, loc, cv);
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.binding.BindingDynamicBinding(dynamicKey, this);
  }
}
