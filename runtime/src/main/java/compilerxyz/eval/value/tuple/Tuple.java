package compilerxyz.eval.value.tuple;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactJson;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.dynamic.TupleClassEmitter;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
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
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Tuple(TList<TupleElement> elements)
    implements
        Type,
        Value,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectInitialBind,
        AspectStackValueDynamic,
        AspectTuple,
        ExporterAny {

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
    var inner =
        elements.stream()
            .map(
                e ->
                    switch (e) {
                      case TupleElement.Dynamic d -> d.type().typeDesc();
                      case TupleElement.EvalRes r -> "?";
                    })
            .collect(Collectors.joining(", "));
    return "tuple(" + inner + ")";
  }

  @Override
  public Type valueTypeOf() {

    var slotTypes = new TList<TupleElement>(elements.size());
    for (var e : elements) {
      Type slot =
          switch (e) {
            case TupleElement.Dynamic d -> d.type();
            case TupleElement.EvalRes r -> {
              var er = r.result();
              if (er.valueOrNull() instanceof CodeValue cv
                  && cv.value() instanceof Opt.Some<Value> some) {
                yield some.value().valueTypeOf();
              }

              yield new Tuple(new TList<>());
            }
          };
      slotTypes.add(new TupleElement.Dynamic(slot));
    }
    return new Tuple(slotTypes);
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectTuple.ID)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectLift.ID)) {

      var elementLifts = new TList<AspectLift>(elements.size());
      var elementTypes = new TList<Type>(elements.size());
      for (var e : elements) {
        if (!(e instanceof TupleElement.Dynamic d)) {
          throw new IllegalStateException("Tuple::aspect(LIFT): non-type tuple");
        }
        var elemLiftOpt = AspectLift.getMaybe(xx, d.type());
        if (!(elemLiftOpt instanceof Opt.Some<AspectLift> elemLiftSome)) {
          throw new IllegalStateException(
              "Tuple element type (" + d.type().typeDesc() + ") missing AspectLift");
        }
        elementLifts.add(elemLiftSome.value());
        elementTypes.add(d.type());
      }
      return Opt.some(new TupleLift(elementLifts, elementTypes));
    }
    return Opt.none();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    // The empty tuple `()` is the language-level unit value and converts to null.
    return elements.isEmpty()
        && compilerxyz.gen.AspectNull.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    // Empty tuple → null: produce a fresh null CodeValue of the destination type.
    // Skip AspectNull dispatch on the source (Tuple isn't AspectNull), and skip
    // typeCastFrom on the dest (which would itself route through AspectNull).
    return EResult.ok(
        new compilerxyz.eval.CodeValue(
            compilerxyz.eval.Code.of(
                loc,
                compilerxyz.eval.dynamic.JavaBytecodeUtils.literalNull(loc)),
            Opt.some(
                new compilerxyz.eval.value.ValueDynamic(
                    new compilerxyz.eval.value.ValueDynamic.Source.Expr(), destType))));
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    var sourceTuple = AspectTuple.getMaybe(xx, sourceType);
    if (!sourceTuple.optIsSome()) {
      return false;
    }
    return ((Opt.Some<AspectTuple>) sourceTuple).value().elementTypes().size() == elements.size();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Tuple::typeCastFrom: CodeValue has no value");
    }
    if (some.value() instanceof ValueDynamic) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(new ValueDynamic(
                  new ValueDynamic.Source.Expr(), this))));
    }
    if (!(some.value() instanceof Tuple tuple)) {
      throw new IllegalStateException(
          "Tuple::typeCastFrom: expected Tuple value, got " + some.value().getClass());
    }
    var erb = new Erb(loc);
    var convertedElements = new TList<TupleElement>(tuple.elements.size());
    for (int i = 0; i < tuple.elements.size(); i++) {
      var elem = tuple.elements.get(i);
      var destSlot = elements.get(i);
      Type destElemType;
      switch (destSlot) {
        case TupleElement.Dynamic d -> destElemType = d.type();
        case TupleElement.EvalRes er -> {
          var destCv = er.result().valueOrNull();
          if (destCv == null || !(destCv.value() instanceof Opt.Some<Value> destSome)) {
            return EResult.err(loc, "Tuple::typeCastFrom: dest element " + i + " missing");
          }
          destElemType = destSome.value().valueTypeOf();
        }
        default -> throw new IllegalStateException();
      }
      TupleElement converted;
      switch (elem) {
        case TupleElement.EvalRes elemRes -> {
          var elemCv = elemRes.result().valueOrNull();
          if (elemCv != null) {
            var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destElemType, elemCv));
            if (convOpt.isEmpty()) {
              return EResult.errs(erb.errors());
            }
            converted = new TupleElement.EvalRes(EResult.ok(convOpt.get()));
          } else {
            var errs = elemRes.result().errorsOrNull();
            if (errs != null) {
              converted = new TupleElement.EvalRes(EResult.errs(errs));
            } else {
              converted = new TupleElement.EvalRes(EResult.errs(new TList<>()));
            }
          }
        }
        case TupleElement.Dynamic d -> converted = new TupleElement.Dynamic(destElemType);
        default -> throw new IllegalStateException();
      }
      convertedElements.add(converted);
    }
    if (!erb.errors().isEmpty()) {
      return EResult.errs(erb.errors());
    }
    return EResult.ok(new CodeValue(cv.code(), Opt.some(new Tuple(convertedElements))));
  }

  @Override
  public TList<Type> elementTypes() {
    var out = new TList<Type>(elements.size());
    for (var e : elements) {
      if (!(e instanceof TupleElement.Dynamic d)) {
        throw new IllegalStateException("Tuple::elementTypes: slot is not Dynamic-only");
      }
      out.add(d.type());
    }
    return out;
  }

  @Override
  public TList<EResult<CodeValue>> takeElements(Value value) {
    if (!(value instanceof Tuple t)) {
      throw new IllegalArgumentException(
          "Tuple::takeElements: expected Tuple value, got " + value.getClass());
    }
    var out = new TList<EResult<CodeValue>>(t.elements.size());
    for (var e : t.elements) {
      if (!(e instanceof TupleElement.EvalRes r)) {
        throw new IllegalStateException("Tuple::takeElements: slot is not EvalRes-form");
      }
      out.add(r.result());
    }
    return out;
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.TUPLE;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof Tuple tuple)) {
            return EResult.err(
                loc, "Tuple::exporterRootDelegateSelfExport: expected Tuple self, got " + self.getClass());
          }
          var cell = INDEPENDENT_CELLS.computeIfAbsent(
              System.identityHashCode(tuple), k -> new ArtifactSupport.ArtifactIdHolder());
          TList<ExprErr> errors = new TList<>();
          var elementArtifacts = new TList<Artifact>(tuple.elements.size());
          for (var e : tuple.elements) {
            Type elemType;
            switch (e) {
              case TupleElement.Dynamic d -> elemType = d.type();
              case TupleElement.EvalRes r -> {
                var cv = r.result().valueOrNull();
                if (cv == null || !(cv.value() instanceof Opt.Some<Value> some)) {
                  errors.add(
                      new ExprErr(
                          loc,
                          TList.of(
                              new ExprSubErr.Opaque(
                                  "Tuple element has no value to serialize"))));
                  elementArtifacts.add(
                      new Artifact.Body(
                          new ArtifactBody(
                              new JsonValueNull(),
                              new ArtifactBodyExporter.Root())));
                  continue;
                }
                elemType = some.value().valueTypeOf();
              }
              default -> throw new IllegalStateException();
            }
            var artRes = ArtifactSupport.exportArtifact(xx, loc, elemType);
            artRes.drainInto(errors);
            var artVal = artRes.valueOrNull();
            if (artVal == null) {
              elementArtifacts.add(
                  new Artifact.Body(
                      new ArtifactBody(
                          new JsonValueNull(),
                          new ArtifactBodyExporter.Root())));
            } else {
              elementArtifacts.add(artVal);
            }
          }
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  cell,
                  () -> {
                    var data = new TOrderedMap<String, JsonValue>();
                    data.put("elements", ArtifactJson.encodeArtifactList(elementArtifacts));
                    return makeBody.apply(new JsonValueObject(data));
                  });
          if (!errors.isEmpty()) {
            return EResult.mixed(artifact, errors);
          }
          return EResult.ok(artifact);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(loc, "Tuple requires an artifact id to deserialize");
          }
          var holder = new Tuple[] {null};
          var elementArtifactsHolder = new TList<TList<Artifact>>();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idSome.value(),
              data,
              (raw) -> {
                if (!(raw instanceof JsonValueObject obj)) {
                  return new Result.Err<>("Tuple data: expected object");
                }
                var elementsJson = obj.entries().get("elements");
                if (elementsJson == null) {
                  return new Result.Err<>("Tuple data: missing 'elements'");
                }
                var decoded = ArtifactJson.decodeArtifactList(loc, elementsJson);
                if (decoded.hasErrors()) {
                  return new Result.Err<>("Tuple data: malformed elements");
                }
                return new Result.Ok<>(decoded.valueOrNull());
              },
              (elementArts) -> {
                elementArtifactsHolder.add(elementArts);
                var holderProxy =
                    new ExporterAny() {
                      @Override
                      public EResult<Type> exporterAnyAsType() {
                        var t = holder[0];
                        if (t == null) {
                          throw new IllegalStateException("Tuple holder not populated");
                        }
                        return EResult.ok(t);
                      }

                      @Override
                      public EResult<Value> exporterAnyAsValue() {
                        var t = holder[0];
                        if (t == null) {
                          throw new IllegalStateException("Tuple holder not populated");
                        }
                        return EResult.ok(t);
                      }
                    };
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var slots = new TList<TupleElement>(elementArts.size());
                      for (var ea : elementArts) {
                        var t =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, ea);
                        if (t.hasErrors() || t.valueOrNull() == null) {
                          throw new IllegalStateException(
                              "Failed to deserialize tuple element type");
                        }
                        slots.add(new TupleElement.Dynamic(t.valueOrNull()));
                      }
                      holder[0] = new Tuple(slots);
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(
                        holderProxy, elementArts, fin));
              });
        }
      };

  private static final java.util.Map<Integer, ArtifactSupport.ArtifactIdHolder> INDEPENDENT_CELLS =
      new java.util.concurrent.ConcurrentHashMap<>();

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.err(
        loc, "Tuple::bind: Target.bind_sub_tuple / code_to_bind_sub not yet ported");
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var carry = cv.code();
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Tuple::toDynamic: CodeValue has no value");
    }

    if (some.value() instanceof ValueDynamic) {
      return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
    }
    if (!(some.value() instanceof Tuple tuple)) {
      throw new IllegalStateException(
          "Tuple::toDynamic: expected Tuple value, got " + some.value().getClass());
    }
    var types = new TList<Type>(elements.size());
    for (var e : elements) {
      if (!(e instanceof TupleElement.Dynamic d)) {
        throw new IllegalStateException("Tuple::toDynamic: self has non-Dynamic element");
      }
      types.add(d.type());
    }
    if (tuple.elements.size() != types.size()) {
      throw new IllegalStateException("Tuple::toDynamic: arity mismatch between value and type");
    }
    TList<ExprErr> errors = null;
    var dynamicElements = new TList<CodeDynamic>(tuple.elements.size());
    for (int i = 0; i < tuple.elements.size(); i++) {
      var elem = tuple.elements.get(i);
      var elemType = types.get(i);
      if (!(elem instanceof TupleElement.EvalRes er)) {
        throw new IllegalStateException("Tuple::toDynamic: value tuple has Dynamic-only element");
      }
      var elemRes = er.result();
      if (elemRes.hasErrors()) {
        if (errors == null) errors = new TList<>();
        elemRes.drainInto(errors);
      }
      var elemCv = elemRes.valueOrNull();
      if (elemCv == null) continue;
      var lookup = AspectConsumeDynamic.get(xx, loc, elemType);
      if (lookup.hasErrors()) {
        if (errors == null) errors = new TList<>();
        lookup.drainInto(errors);
        continue;
      }
      var toDyn = lookup.valueOrNull();
      if (toDyn == null) continue;
      var lowered = toDyn.toDynamic(xx, loc, elemCv);
      if (lowered.hasErrors()) {
        if (errors == null) errors = new TList<>();
        lowered.drainInto(errors);
      }
      var loweredCode = lowered.valueOrNull();
      if (loweredCode != null) {
        dynamicElements.add(loweredCode);
      }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(TupleClassEmitter.internalNameFor(types));
    return EResult.ok(
        DynUtil.wrapWithCarry(
            loc,
            carry,
            compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiateVarargs(
                loc, klass, dynamicElements)));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    throw new UnsupportedOperationException(
        "Tuple::initialBind: dynamic binding sites for tuples not yet exercised");
  }

  public static final class TupleLift implements AspectLift {
    private final TList<AspectLift> elementLifts;
    private final TList<Type> elementTypes;
    private volatile java.lang.reflect.Field @org.jspecify.annotations.Nullable [] cachedFields;

    public TupleLift(TList<AspectLift> elementLifts, TList<Type> elementTypes) {
      this.elementLifts = elementLifts;
      this.elementTypes = elementTypes;
    }

    @Override
  public EResult<Value> lift(
        ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {

      if (externalHostValue == null && elementLifts.isEmpty()) {
        return EResult.ok(new Tuple(new TList<>()));
      }
      var expectedClass = TupleClassEmitter.getOrEmit(elementTypes);
      if (externalHostValue == null || externalHostValue.getClass() != expectedClass) {
        return EResult.err(
            loc,
            "Tuple TupleLift::lift: expected "
                + expectedClass.getName()
                + ", got "
                + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
      }
      var fields = cachedFields;
      if (fields == null) {
        fields = new java.lang.reflect.Field[elementLifts.size()];
        for (int i = 0; i < fields.length; i++) {
          try {
            fields[i] = expectedClass.getField(TupleClassEmitter.slotFieldName(i));
          } catch (NoSuchFieldException e) {
            throw new AssertionError(
                "Tuple TupleLift::lift: generated class missing slot field _" + i, e);
          }
        }
        cachedFields = fields;
      }
      var out = new TList<TupleElement>(elementLifts.size());
      TList<ExprErr> errors = null;
      for (int i = 0; i < elementLifts.size(); i++) {
        var elemLift = elementLifts.get(i);
        Object slotValue;
        try {
          slotValue = fields[i].get(externalHostValue);
        } catch (IllegalAccessException e) {
          throw new AssertionError(
              "Tuple TupleLift::lift: cannot read slot field _" + i, e);
        }
        var lifted = elemLift.lift(xx, loc, slotValue);
        if (lifted.hasErrors()) {
          if (errors == null) errors = new TList<>();
          lifted.drainInto(errors);
          continue;
        }
        var liftedValue = lifted.valueOrNull();
        if (liftedValue == null) {
          throw new IllegalStateException(
              "Tuple TupleLift::lift: element lift returned neither value nor errors");
        }
        out.add(
            new TupleElement.EvalRes(
                EResult.ok(new CodeValue(Code.none(loc), Opt.some(liftedValue)))));
      }
      if (errors != null && !errors.isEmpty()) {
        return EResult.errs(errors);
      }
      return EResult.ok(new Tuple(out));
    }
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
