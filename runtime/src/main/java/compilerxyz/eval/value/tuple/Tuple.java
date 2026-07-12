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
import compilerxyz.gen.Binding;
import compilerxyz.eval.dynamic.CompositeClassEmitter;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectElementUseConst;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CompositeSlotSort;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.gen.Target;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TupleBindSlot;
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

public record Tuple(Opt<TupleBaseDynamic> base, TList<TupleElement> elements)
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
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    var res = accessOrIndex(xx, loc, cv, Opt.none());
    if (res.hasErrors()) return res.reerr();
    return EResult.ok(res.requireValue("Tuple::drop").code());
  }

  /** Unified element-access + drop for tuples. See Record.accessOrDrop javadoc. */
  public EResult<CodeValue> accessOrIndex(
      ExprContext xx, QExprId loc, CodeValue selfCv, Opt<Integer> accessIndex) {
    if (!(selfCv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Tuple::accessOrIndex: CodeValue has no value");
    }
    var v = some.value();
    ValueDynamic.Source baseSource;
    Tuple tuple;
    if (v instanceof ValueDynamic vd) {
      baseSource = vd.source();
      tuple = this;
    } else if (v instanceof Tuple t) {
      tuple = t;
      if (t.base instanceof Opt.Some<TupleBaseDynamic> baseSome) {
        baseSource = baseSome.value().source();
      } else {
        return accessOrIndexSourceless(xx, loc, tuple, accessIndex);
      }
    } else {
      throw new IllegalStateException(
          "Tuple::accessOrIndex: expected Tuple or ValueDynamic, got " + v.getClass());
    }
    return accessOrIndexSourced(xx, loc, tuple, baseSource, accessIndex);
  }

  private EResult<CodeValue> accessOrIndexSourceless(
      ExprContext xx, QExprId loc, Tuple tuple, Opt<Integer> accessIndex) {
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    Integer accessed = accessIndex instanceof Opt.Some<Integer> aiSome ? aiSome.value() : null;
    CodeValue accessedCv = null;
    for (int i = tuple.elements.size() - 1; i >= 0; i--) {
      boolean isAccessed = accessed != null && accessed == i;
      var e = tuple.elements.get(i);
      if (!(e instanceof TupleElement.EvalRes er)) {
        throw new IllegalStateException(
            "Tuple::accessOrIndexSourceless: element " + i + " is not EvalRes");
      }
      var res = er.result();
      if (isAccessed) {
        if (res.hasErrors()) {
          return EResult.err(loc, "Tuple element " + i + " evaluated to error");
        }
        accessedCv = res.requireValue("Tuple::accessOrIndexSourceless accessed");
        continue;
      }
      if (res.hasErrors()) continue;
      var elemCv = res.requireValue("Tuple::accessOrIndexSourceless sibling");
      if (!(elemCv.value() instanceof Opt.Some<Value> elemSome)) continue;
      var elemType = elemSome.value().valueType();
      var elemDropOpt = compilerxyz.gen.AspectDropDynamic.getMaybe(xx, elemType);
      if (!(elemDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamic> elemDropSome))
        continue;
      var elemDropRes = elemDropSome.value().aspectDropDynamicDrop(xx, loc, elemCv);
      if (elemDropRes.hasErrors()) return elemDropRes.reerr();
      var elemDrop =
          elemDropRes.requireValue("Tuple::accessOrIndexSourceless sibling drop").dynamic();
      if (elemDrop != null) seq.add(elemDrop);
    }
    if (accessed != null) {
      if (accessedCv == null) {
        return EResult.err(loc, "Tuple element " + accessed + " not found");
      }
      var accessedDyn = accessedCv.code().dynamic();
      if (accessedDyn != null) seq.add(accessedDyn);
      return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), accessedCv.value()));
    }
    return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), Opt.none()));
  }

  private EResult<CodeValue> accessOrIndexSourced(
      ExprContext xx,
      QExprId loc,
      Tuple tuple,
      ValueDynamic.Source baseSource,
      Opt<Integer> accessIndex) {
    var elementTypesUnsorted = new TList<Type>(elements.size());
    for (var e : elements) {
      if (!(e instanceof TupleElement.Dynamic d)) {
        throw new IllegalStateException(
            "Tuple::accessOrIndexSourced: sourced tuple must have Dynamic elements");
      }
      elementTypesUnsorted.add(d.type());
    }
    var slotInfo = sortTupleSlots(elementTypesUnsorted);
    var sortedTypes = slotInfo.sortedTypes();
    var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
    boolean stackRooted = DynUtil.isStackRooted(baseSource);

    Integer accessedIdx = accessIndex instanceof Opt.Some<Integer> aiSome ? aiSome.value() : null;
    Type accessedType = null;
    int accessedSlot = -1;
    if (accessedIdx != null) {
      if (accessedIdx < 0 || accessedIdx >= elementTypesUnsorted.size()) {
        return EResult.err(loc, "Tuple element index " + accessedIdx + " out of range");
      }
      accessedType = elementTypesUnsorted.get(accessedIdx);
      accessedSlot = slotInfo.slotOfKey().get(accessedIdx);
    }

    // Fast path: pure element-access on non-stack source.
    if (accessedIdx != null && !stackRooted) {
      var loadSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      loadSeq.add(baseSource.realize(xx, loc));
      loadSeq.add(
          JavaBytecodeUtils.accessField(
              loc,
              klass,
              CompositeClassEmitter.slotFieldName(accessedSlot),
              CompositeClassEmitter.slotDescriptor(xx, accessedType)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, loadSeq)),
              Opt.some(ValueDynamic.stack(xx, accessedType))));
    }

    var out = new compilerxyz.eval.dynamic.JavaBytecodeSequence();

    if (accessedIdx == null) {
      // Full drop.
      if (!stackRooted) {
        var srcDrop = baseSource.drop(loc).dynamic();
        if (srcDrop != null) out.add(srcDrop);
        return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), Opt.none()));
      }
      out.add(baseSource.realize(xx, loc));
      boolean anyFullDrop = false;
      for (var t : elementTypesUnsorted) {
        if (compilerxyz.gen.AspectDropDynamic.getMaybe(xx, t)
            instanceof Opt.Some<compilerxyz.gen.AspectDropDynamic>) {
          anyFullDrop = true;
          break;
        }
      }
      if (!anyFullDrop) {
        out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), Opt.none()));
      }
      // Reverse iterate by definition index (matches Record's reverse-canonical policy for
      // tuples: definition order tie-broken by index).
      for (int i = elementTypesUnsorted.size() - 1; i >= 0; i--) {
        var t = elementTypesUnsorted.get(i);
        int slotIdx = slotInfo.slotOfKey().get(i);
        var siblingDropRes = tupleChildDropCode(xx, loc, klass, slotIdx, t, i);
        if (siblingDropRes.hasErrors()) return siblingDropRes.reerr();
        out.add(siblingDropRes.requireValue("Tuple::accessOrIndex sibling"));
      }
      out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
      return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), Opt.none()));
    }

    // Element access with stack-rooted base.
    for (int i = elementTypesUnsorted.size() - 1; i >= 0; i--) {
      if (accessedIdx == i) continue;
      var t = elementTypesUnsorted.get(i);
      int slotIdx = slotInfo.slotOfKey().get(i);
      var siblingDropRes = tupleChildDropCode(xx, loc, klass, slotIdx, t, i);
      if (siblingDropRes.hasErrors()) return siblingDropRes.reerr();
      out.add(siblingDropRes.requireValue("Tuple::accessOrIndex sibling"));
    }
    var accessedConsume = compilerxyz.gen.AspectConsumeDynamic.getMaybe(xx, accessedType);
    if (!(accessedConsume
        instanceof Opt.Some<compilerxyz.gen.AspectConsumeDynamic> accessedConsumeSome)) {
      return EResult.err(
          loc,
          "Tuple::accessOrIndex: accessed element type '"
              + accessedType.typeDesc()
              + "' is not AspectConsumeDynamic");
    }
    var acd = accessedConsumeSome.value();
    var accessedLocalKey = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
    out.add(
        JavaBytecodeUtils.accessField(
            loc,
            klass,
            CompositeClassEmitter.slotFieldName(accessedSlot),
            CompositeClassEmitter.slotDescriptor(xx, accessedType)));
    out.add(acd.aspectConsumeDynamicEmitStore(xx, loc, accessedLocalKey));
    out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
    out.add(acd.aspectConsumeDynamicEmitLoad(xx, loc, accessedLocalKey));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(loc, out)),
            Opt.some(ValueDynamic.stack(xx, accessedType))));
  }

  private EResult<CodeDynamic> tupleChildDropCode(
      ExprContext xx, QExprId loc, JavaInternalName klass, int slotIdx, Type childType,
      int elementIndex) {
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
    seq.add(
        JavaBytecodeUtils.accessField(
            loc,
            klass,
            CompositeClassEmitter.slotFieldName(slotIdx),
            CompositeClassEmitter.slotDescriptor(xx, childType)));
    var childCv =
        new CodeValue(
            Code.of(loc, new CodeDynamic(loc, new compilerxyz.eval.dynamic.JavaBytecodeSequence())),
            Opt.some(ValueDynamic.stack(xx, childType)));
    var fullDropOpt = compilerxyz.gen.AspectDropDynamic.getMaybe(xx, childType);
    if (fullDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamic> fullSome) {
      var res = fullSome.value().aspectDropDynamicDrop(xx, loc, childCv);
      if (res.hasErrors()) return res.reerr();
      var d = res.requireValue("Tuple::tupleChildDropCode drop").dynamic();
      if (d != null) seq.add(d);
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    var lightDropOpt = compilerxyz.gen.AspectDropDynamicLight.getMaybe(xx, childType);
    if (lightDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamicLight> lightSome) {
      var res = lightSome.value().aspectDropDynamicLightDropDynamicLight(xx, loc, childCv);
      if (res.hasErrors()) return res.reerr();
      var d = res.requireValue("Tuple::tupleChildDropCode light drop").dynamic();
      if (d != null) seq.add(d);
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    return EResult.err(
        loc,
        "Tuple element " + elementIndex + " type '" + childType.typeDesc()
            + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
  }

  private static CompositeSlotSort.Result<Integer> sortTupleSlots(
      TList<Type> types) {
    var entries = new TOrderedMap<Integer, Type>();
    for (int i = 0; i < types.size(); i++) entries.put(i, types.get(i));
    return CompositeSlotSort.sort(entries, Integer::compare);
  }

  private static TList<Type> sortTupleTypes(TList<Type> types) {
    return sortTupleSlots(types).sortedTypes();
  }

  private static TList<Integer> slotOfIndex(TList<Type> types) {
    var slotOf = sortTupleSlots(types).slotOfKey();
    var out = new TList<Integer>(types.size());
    for (int i = 0; i < types.size(); i++) out.add(slotOf.get(i));
    return out;
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
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
  public Type valueType() {

    var slotTypes = new TList<TupleElement>(elements.size());
    for (var e : elements) {
      Type slot =
          switch (e) {
            case TupleElement.Dynamic d -> d.type();
            case TupleElement.EvalRes r -> {
              var er = r.result();
              if (!er.hasErrors()
                  && er.requireValue("Tuple::valueType element").value()
                      instanceof Opt.Some<Value> some) {
                yield some.value().valueType();
              }
              yield new Tuple(Opt.none(), new TList<>());
            }
          };
      slotTypes.add(new TupleElement.Dynamic(slot));
    }
    return new Tuple(base, slotTypes);
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
          return Opt.none();
        }
        var elemLiftOpt = AspectLift.getMaybe(xx, d.type());
        if (!(elemLiftOpt instanceof Opt.Some<AspectLift> elemLiftSome)) {
          return Opt.none();
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
                JavaBytecodeUtils.literalNull(loc)),
            Opt.some(
                compilerxyz.eval.value.ValueDynamic.stack(xx, destType))));
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    var sourceTuple = AspectTuple.getMaybe(xx, sourceType);
    if (!sourceTuple.optIsSome()) {
      return false;
    }
    return ((Opt.Some<AspectTuple>) sourceTuple).value().aspectTupleElementTypes().size() == elements.size();
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
              Opt.some(ValueDynamic.stack(xx, this))));
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
          var destRes = er.result();
          if (destRes.hasErrors()) {
            return EResult.err(loc, "Tuple::typeCastFrom: dest element " + i + " missing");
          }
          var destCv = destRes.requireValue("Tuple::typeCastFrom dest element");
          if (!(destCv.value() instanceof Opt.Some<Value> destSome)) {
            return EResult.err(loc, "Tuple::typeCastFrom: dest element " + i + " missing");
          }
          destElemType = destSome.value().valueType();
        }
        default -> throw new IllegalStateException();
      }
      TupleElement converted;
      switch (elem) {
        case TupleElement.EvalRes elemRes -> {
          var innerRes = elemRes.result();
          if (innerRes.hasErrors()) {
            var errs = innerRes.errorsOrNull();
            converted =
                new TupleElement.EvalRes(
                    EResult.errs(errs != null ? errs : new TList<>()));
          } else {
            var elemCv = innerRes.requireValue("Tuple::typeCastFrom source element");
            var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destElemType, elemCv));
            if (convOpt.isEmpty()) {
              return EResult.errs(erb.errors());
            }
            converted = new TupleElement.EvalRes(EResult.ok(convOpt.get()));
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
    return EResult.ok(new CodeValue(cv.code(), Opt.some(new Tuple(Opt.none(), convertedElements))));
  }

  @Override
  public TList<Type> aspectTupleElementTypes() {
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
  public TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value) {
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
  public ArtifactExporter aspectArtifactExporter() {
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
          var cell = new ArtifactSupport.ArtifactIdHolder();
          TList<ExprErr> errors = new TList<>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  cell,
                  () -> {
                    var elementArtifacts = new TList<Artifact>(tuple.elements.size());
                    for (var e : tuple.elements) {
                      Type elemType;
                      switch (e) {
                        case TupleElement.Dynamic d -> elemType = d.type();
                        case TupleElement.EvalRes r -> {
                          var innerRes = r.result();
                          if (innerRes.hasErrors()) {
                            innerRes.drainInto(errors);
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
                          var cv = innerRes.requireValue("Tuple::exporterExport element");
                          if (!(cv.value() instanceof Opt.Some<Value> some)) {
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
                          elemType = some.value().valueType();
                        }
                        default -> throw new IllegalStateException();
                      }
                      var artRes = ArtifactSupport.exportArtifact(xx, loc, elemType);
                      artRes.drainInto(errors);
                      if (artRes.valueOrNull() != null) {
                        elementArtifacts.add(
                            artRes.requireValue("Tuple::exporterExport element artifact"));
                      } else {
                        elementArtifacts.add(
                            new Artifact.Body(
                                new ArtifactBody(
                                    new JsonValueNull(),
                                    new ArtifactBodyExporter.Root())));
                      }
                    }
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
                return new Result.Ok<>(decoded.requireValue("Tuple::exporterFromArtifactData elements"));
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
                        if (t.hasErrors()) {
                          throw new IllegalStateException(
                              "Failed to deserialize tuple element type");
                        }
                        slots.add(
                            new TupleElement.Dynamic(
                                t.requireValue("Tuple::finalizer element type")));
                      }
                      holder[0] = new Tuple(Opt.none(), slots);
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(
                        holderProxy, elementArts, fin));
              });
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    var subRes = bindSub(xx, loc, cv);
    if (subRes.hasErrors()) {
      return subRes.reerr();
    }
    var sub = subRes.requireValue("Tuple::bind bindSub");
    if (sub.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
      var bound = xx.target.targetBind(xx, loc, targetSome.value());
      return EResult.ok(
          new BindResult(
              Code.of(loc, bound.code()),
              new BindingTupleBound(sub.scopeFields(), Opt.some(bound.id()))));
    }
    return EResult.ok(
        new BindResult(
            Code.none(loc), new BindingTupleBound(sub.scopeFields(), Opt.none())));
  }

  private record BindSub(
      TList<TupleScopeField> scopeFields, Opt<TargetBindSub> targetSub) {}

  private EResult<BindSub> bindSub(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Tuple::bindSub: CodeValue has no value");
    }
    if (!(some.value() instanceof Tuple tuple)) {
      throw new IllegalStateException(
          "Tuple::bindSub: expected Tuple value, got " + some.value().getClass());
    }
    var carry = cv.code();

    if (base instanceof Opt.Some<TupleBaseDynamic> baseSome) {
      Type tupleType = this;
      var scopeFields = new TList<TupleScopeField>(elements.size());
      for (var e : elements) {
        if (!(e instanceof TupleElement.Dynamic d)) {
          throw new IllegalStateException("Tuple::bindSub: bind on non-type tuple");
        }
        scopeFields.add(new TupleScopeField.Dynamic(d.type()));
      }
      var subRes = baseSome.value().tupleBaseBindSub(xx, loc, carry, tupleType);
      if (subRes.hasErrors()) {
        return subRes.reerr();
      }
      var sub = subRes.requireValue("Tuple::bindSub base.bindSub");
      return EResult.ok(new BindSub(scopeFields, Opt.some(sub)));
    }

    TList<ExprErr> errors = null;
    record ElemBindResult(Opt<TargetBindSub> targetSub, Opt<CodeValue> constCv) {}
    var bindResults = new TList<ElemBindResult>(tuple.elements.size());
    boolean hasTarget = false;
    for (int i = 0; i < tuple.elements.size(); i++) {
      var elem = tuple.elements.get(i);
      switch (elem) {
        case TupleElement.Dynamic d -> {
          if (errors == null) errors = new TList<>();
          errors.add(
              new ExprErr(
                  loc,
                  TList.of(
                      new ExprSubErr.Opaque(
                          "Tuple element " + i
                              + " is dynamic-only, cannot bind directly"))));
        }
        case TupleElement.EvalRes elemRes -> {
          var inner = elemRes.result();
          if (inner.hasErrors()) {
            if (errors == null) errors = new TList<>();
            inner.drainInto(errors);
            continue;
          }
          var elemCv = inner.requireValue("Tuple::bindSub element value");
          if (elemCv.value() instanceof Opt.Some<Value> elemSome) {
            var elemType = elemSome.value().valueType();
            var boxedType = DynUtil.substituteBoxed(xx, elemType);
            if (!boxedType.equals(elemType)) {
              var boxedRes = boxedType.typeCastFrom(xx, loc, elemCv);
              if (boxedRes.hasErrors()) {
                if (errors == null) errors = new TList<>();
                boxedRes.drainInto(errors);
                continue;
              }
              elemCv = boxedRes.requireValue("Tuple::bindSub boxed element");
            }
          }
          var savedValue = elemCv.value();
          var subRes = xx.target.targetMaybeBindSub(xx, loc, elemCv);
          if (subRes.hasErrors()) {
            if (errors == null) errors = new TList<>();
            subRes.drainInto(errors);
            continue;
          }
          var subOpt = subRes.requireValue("Tuple::bindSub targetMaybeBindSub");
          if (subOpt instanceof Opt.Some<TargetBindSub> subSome) {
            hasTarget = true;
            bindResults.add(new ElemBindResult(Opt.some(subSome.value()), Opt.none()));
          } else {
            bindResults.add(
                new ElemBindResult(
                    Opt.none(), Opt.some(new CodeValue(Code.none(loc), savedValue))));
          }
        }
      }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    if (!hasTarget) {
      var scopeFields = new TList<TupleScopeField>(bindResults.size());
      for (var br : bindResults) {
        if (!(br.constCv() instanceof Opt.Some<CodeValue> constSome)) {
          throw new IllegalStateException(
              "Tuple::bindSub: !has_target but bindResults contains a target sub");
        }
        var elemCv = constSome.value();
        if (!(elemCv.value() instanceof Opt.Some<Value> elemSome)) {
          throw new IllegalStateException(
              "Tuple::bindSub: const element CodeValue has no value");
        }
        var elemType = elemSome.value().valueType();
        var bindRes = AspectBind.get(xx, loc, elemType);
        if (bindRes.hasErrors()) {
          scopeFields.add(new TupleScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
        var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
        if (elemBindRes.hasErrors()) {
          scopeFields.add(new TupleScopeField.Error(elemBindRes.errorsOrNull()));
          continue;
        }
        var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
        scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
      }
      return EResult.ok(new BindSub(scopeFields, Opt.none()));
    }
    var scopeFields = new TList<TupleScopeField>(bindResults.size());
    var targetSlots = new TList<TupleBindSlot>(bindResults.size());
    for (var br : bindResults) {
      if (br.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
        scopeFields.add(new TupleScopeField.Dynamic(targetSome.value().type()));
        targetSlots.add(new TupleBindSlot(targetSome.value()));
      } else if (br.constCv() instanceof Opt.Some<CodeValue> constSome) {
        var elemCv = constSome.value();
        if (!(elemCv.value() instanceof Opt.Some<Value> elemSome)) {
          throw new IllegalStateException(
              "Tuple::bindSub: const element CodeValue has no value");
        }
        var elemType = elemSome.value().valueType();
        var bindRes = AspectBind.get(xx, loc, elemType);
        if (bindRes.hasErrors()) {
          scopeFields.add(new TupleScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
        var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
        if (elemBindRes.hasErrors()) {
          scopeFields.add(new TupleScopeField.Error(elemBindRes.errorsOrNull()));
          continue;
        }
        var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
        scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
      } else {
        throw new IllegalStateException("Tuple::bindSub: bind result has neither target nor const");
      }
    }
    var combined = xx.target.targetBindSubTuple(xx, loc, carry, targetSlots);
    return EResult.ok(new BindSub(scopeFields, Opt.some(combined)));
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
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
        continue;
      }
      var elemCv = elemRes.requireValue("Tuple::toDynamic element value");
      var lookup = AspectConsumeDynamic.get(xx, loc, elemType);
      if (lookup.hasErrors()) {
        if (errors == null) errors = new TList<>();
        lookup.drainInto(errors);
        continue;
      }
      var toDyn = lookup.requireValue("Tuple::toDynamic consume aspect");
      var lowered = toDyn.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
      if (lowered.hasErrors()) {
        if (errors == null) errors = new TList<>();
        lowered.drainInto(errors);
        continue;
      }
      var loweredCode = lowered.requireValue("Tuple::toDynamic element lowered");
      dynamicElements.add(loweredCode);
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    var slotOf = slotOfIndex(types);
    var sortedTypes = sortTupleTypes(types);
    @SuppressWarnings("unchecked")
    CodeDynamic[] sortedCodesArr = new CodeDynamic[types.size()];
    for (int i = 0; i < types.size(); i++) {
      sortedCodesArr[slotOf.get(i)] = dynamicElements.get(i);
    }
    var sortedCodes = new TList<CodeDynamic>(types.size());
    for (var c : sortedCodesArr) sortedCodes.add(c);
    var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
    var argSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    for (var c : sortedCodes) argSeq.add(c);
    var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
    return EResult.ok(
        DynUtil.wrapWithCarry(
            loc,
            carry,
            JavaBytecodeUtils.instantiate(
                loc, klass, ctorDesc, new CodeDynamic(loc, argSeq))));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    var elementTypes = new TList<Type>(elements.size());
    for (int i = 0; i < elements.size(); i++) {
      var e = elements.get(i);
      if (!(e instanceof TupleElement.Dynamic d)) {
        throw new IllegalStateException(
            "Tuple::initialBind: element " + i + " is not Dynamic-only");
      }
      elementTypes.add(d.type());
    }
    return new BindingTupleDynamic(dynamicKey, elementTypes);
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
  public EResult<Value> aspectLiftLift(
        ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {

      if (externalHostValue == null && elementLifts.isEmpty()) {
        return EResult.ok(new Tuple(Opt.none(), new TList<>()));
      }
      var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortTupleTypes(elementTypes));
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
        var slotOf = slotOfIndex(elementTypes);
        for (int i = 0; i < fields.length; i++) {
          int slot = slotOf.get(i);
          try {
            fields[i] = expectedClass.getField(CompositeClassEmitter.slotFieldName(slot));
          } catch (NoSuchFieldException e) {
            throw new AssertionError(
                "Tuple TupleLift::lift: generated class missing slot field _" + slot, e);
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
        var lifted = elemLift.aspectLiftLift(xx, loc, slotValue);
        if (lifted.hasErrors()) {
          if (errors == null) errors = new TList<>();
          lifted.drainInto(errors);
          continue;
        }
        var liftedValue = lifted.requireValue("Tuple TupleLift::lift element value");
        out.add(
            new TupleElement.EvalRes(
                EResult.ok(new CodeValue(Code.none(loc), Opt.some(liftedValue)))));
      }
      if (errors != null && !errors.isEmpty()) {
        return EResult.errs(errors);
      }
      return EResult.ok(new Tuple(Opt.none(), out));
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

  @Override
  public TypeId typeId() {
    var ids = new TList<TypeId>(elements.size());
    for (var e : elements) {
      Type t = switch (e) {
        case TupleElement.Dynamic d -> d.type();
        case TupleElement.EvalRes r -> {
          var er = r.result();
          if (!er.hasErrors()
              && er.requireValue("Tuple::typeId slot").value() instanceof Opt.Some<Value> some) {
            yield some.value().valueType();
          }
          yield (Type) new Tuple(Opt.none(), new TList<>());
        }
      };
      ids.add(t.typeId());
    }
    ids.sort(TypeId.CANONICAL_ORDER);
    return new TypeId.Composite(GenTypeIds.TUPLE_TYPE_ID, ids);
  }
}
