package compilerxyz.eval.value.unique;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.bool_.ValueBoolConst;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.util.Opt;
import java.util.function.Function;

public record TypeUnique()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectInitialBind,
        AspectOperatorEq,
        ExporterAny {

  public static final BindingId UNIQUE_EQ_FN_SCOPE_ID = new BindingId.Builtin(7021);

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaInternalName consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT;
  }

  @Override
  public String typeDesc() {
    return "unique";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.Unique();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.TYPE_UNIQUE_IDENTITY)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectOperatorEq.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
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
          if (self instanceof ValueUnique v) {
            data = encodeUnique(v.unique());
          } else if (self instanceof ValueTypeUnique || self instanceof TypeUnique) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeUnique::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new ValueTypeUnique());
          }
          if (data instanceof JsonValueObject obj) {
            var decoded = decodeUnique(loc, obj);
            if (decoded.hasErrors()) {
              return EResult.err(loc, "TypeUnique::exporterRootDelegateSelfFromArtifactData: malformed");
            }
            return EResult.ok(new ValueUnique(decoded.valueOrNull()));
          }
          return EResult.err(loc, "TypeUnique::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
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
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeUnique::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      return EResult.err(loc, "TypeUnique::toDynamic: CodeValue has no dynamic code");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeUnique());
  }

  @Override
  public EResult<CodeValue> eq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var b = extractUnique(base);
    var r = extractUnique(reference);
    if (b instanceof Opt.Some<Unique> bs && r instanceof Opt.Some<Unique> rs) {
      return EResult.ok(
          new CodeValue(Code.none(loc), Opt.some(new ValueBoolConst(bs.value().equals(rs.value())))));
    }
    var baseDyn = toDynamic(xx, loc, base);
    if (baseDyn.hasErrors()) return EResult.err(loc, "TypeUnique::eq: base lowering failed");
    var refDyn = toDynamic(xx, loc, reference);
    if (refDyn.hasErrors()) return EResult.err(loc, "TypeUnique::eq: reference lowering failed");
    var call =
        compilerxyz.eval.dynamic.DynRuntime.UNIQUE_EQ_TYPE.emitInvocation(
            loc, xx.vx, TList.of(baseDyn.valueOrNull(), refDyn.valueOrNull()));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, call),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), new TypeBoolDynamic()))));
  }

  private static Opt<Unique> extractUnique(CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some && some.value() instanceof ValueUnique v) {
      return Opt.some(v.unique());
    }
    return Opt.none();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenIdentities.TYPE_UNIQUE_IDENTITY) instanceof Opt.Some<Aspect>;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(cv);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeUnique());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueTypeUnique());
  }
}
