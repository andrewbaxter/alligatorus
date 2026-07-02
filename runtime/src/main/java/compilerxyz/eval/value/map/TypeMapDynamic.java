package compilerxyz.eval.value.map;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.empty.ValueEmptyConst;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.eval.value.number.ValueNumberConst;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.string.ValueStringConst;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.function.Function;

public record TypeMapDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.MapDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectIterable.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.MAP_DYNAMIC;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          return EResult.err(
              loc,
              "TypeMapDynamic::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port"
                  + " deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeMapDynamic::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact"
                  + " (eval/artifact.rs) port deferred");
        }
      };

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeMapDynamic::field: key has no compile-time value");
    }
    var keyMetaType = keySome.value().valueTypeOf();
    var stringConstRes = AspectStringConst.get(xx, loc, keyMetaType);
    if (stringConstRes.hasErrors()) {
      return stringConstRes.reerr();
    }
    var stringConst = stringConstRes.valueOrNull();
    if (stringConst == null) {
      throw new IllegalStateException("AspectStringConst.get returned neither value nor errors");
    }
    String keyName = stringConst.get(keySome.value());
    if (keyName.equals("new")) {
      Type newType = new TypeMapNewDynamic(keyType, valueType);
      return EResult.ok(
          new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(newType))));
    }
    var fieldTypeOpt = fieldTypeOpt(xx, new ConstKey.Str(keyName));
    if (!(fieldTypeOpt instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field '" + keyName + "' not found on map");
    }
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
    }
    if (keyName.equals("get")) {
      return EResult.ok(
          new CodeValue(
              Code.of(loc, selfDyn),
              Opt.some(new ValueEmptyConst(new TypeMapGetFnDynamic(keyType, valueType)))));
    }
    var fieldUse = new CodeDynamic(loc, DynUtil.buildFieldGetSequence(selfDyn, keyName));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, fieldUse),
            Opt.some(
                new ValueDynamic(
                    new ValueDynamic.Source.Field(selfDyn, keyName), fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    return switch (s.value()) {
      case "new" -> Opt.some(new TypeMapNewDynamic(keyType, valueType));
      case "get" -> Opt.some(new TypeFunctionDynamic(keyType, valueType));
      default -> Opt.some(valueType);
    };
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeMapDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type elementType() {
    var fields =
        new TOrderedMap<ConstKey, RecordField>();
    fields.put(
        new ConstKey.Str("key"), new RecordField.Dynamic(keyType));
    fields.put(
        new ConstKey.Str("value"),
        new RecordField.Dynamic(valueType));
    return new Record(Opt.none(), fields);
  }

  @Override
  public EResult<CodeValue> foreach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = AspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) {
      return baseConsumeRes.reerr();
    }
    var baseDynRes = baseConsumeRes.valueOrNull().toDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) {
      return baseDynRes.reerr();
    }
    CodeDynamic bodyDyn = body.code().dynamic();
    if (bodyDyn == null) {
      bodyDyn =
          new CodeDynamic(loc, new JavaBytecode.LoweredSequence(JavaBytecodeUtils.jsonLiteral(new JsonValueNull())));
    }
    var foreachDyn =
        new CodeDynamic(loc, new JavaBytecode.ForeachMap(baseDynRes.valueOrNull(), elementScopeId, bodyDyn));
    Type nullType = new TypeNullDynamic();
    var stackRes = AspectStackValueDynamic.get(xx, loc, nullType);
    if (stackRes.hasErrors()) {
      return stackRes.reerr();
    }
    return EResult.ok(stackRes.valueOrNull().toValue(loc, foreachDyn));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    if (sourceType instanceof TypeMapDynamic sourceMap) {
      if (!sourceMap.keyType.getClass().equals(keyType.getClass())) {
        return false;
      }
      return typesConvertible(xx, loc, sourceMap.valueType, valueType);
    }
    if (sourceType instanceof Record
        && AspectConsumeDynamic.getMaybe(xx, sourceType) instanceof Opt.Some<?>) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeMapDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    if (sourceType instanceof TypeMapDynamic sourceMap) {
      if (!sourceMap.keyType.getClass().equals(keyType.getClass())) {
        return EResult.err(
            loc,
            "Incompatible map key types: "
                + sourceMap.keyType.typeDesc()
                + " cannot convert to "
                + keyType.typeDesc());
      }
      if (!typesConvertible(xx, loc, sourceMap.valueType, valueType)) {
        return EResult.err(
            loc,
            "Incompatible map value types: "
                + sourceMap.valueType.typeDesc()
                + " cannot convert to "
                + valueType.typeDesc());
      }
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
    }
    if (!(some.value() instanceof Record record)) {
      return EResult.err(
          loc, "TypeMapDynamic::typeCastFrom: expected Record source, got " + some.value().getClass());
    }
    var erb = new Erb(loc);
    var valueToDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, valueType));
    if (valueToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var valueToDynamic = valueToDynamicOpt.get();
    var keyToDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, keyType));
    if (keyToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var keyToDynamic = keyToDynamicOpt.get();
    var torderedMapIntname = JavaInternalName.of("compilerxyz/util/TOrderedMap");
    var putDesc =
        JavaMethodDescriptor.fromParts(
            JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
    var seqElements = new TList<JavaBytecode.LoweredSequence.Element>();
    seqElements.addAll(
        JavaBytecodeUtils.instantiate(
            -1, torderedMapIntname, JavaMethodDescriptor.VOID_NOARGS, new TList<>()));
    for (var entry : record.fields().entrySet()) {
      var fieldKey = entry.getKey();
      var fieldVal = entry.getValue();
      CodeValue keyCv;
      switch (fieldKey) {
        case ConstKey.Str s ->
            keyCv = new CodeValue(Code.none(loc), Opt.some(new ValueStringConst(s.value())));
        case ConstKey.Uint u ->
            keyCv =
                new CodeValue(Code.none(loc), Opt.some(new ValueNumberConst((double) u.value())));
      }
      var keyConvOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, keyType, keyCv));
      if (keyConvOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      var keyCodeOpt = erb.mustEr(keyToDynamic.toDynamic(xx, loc, keyConvOpt.get()));
      if (keyCodeOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      CodeValue entryCv;
      switch (fieldVal) {
        case RecordField.EvalRes er -> {
          var inner = er.result();
          var innerErrs = inner.errorsOrNull();
          if (innerErrs != null) {
            erb.errors().addAll(innerErrs);
          }
          var innerVal = inner.valueOrNull();
          if (innerVal == null) {
            erb.errPush(
                "Record entry '" + fieldKey.toCanonical() + "' failed to evaluate");
            return EResult.errs(erb.errors());
          }
          entryCv = innerVal;
        }
        case RecordField.Dynamic d -> {
          erb.errPush(
              "Record entry '"
                  + fieldKey.toCanonical()
                  + "' is dynamic-only, cannot consume directly to dynamic");
          return EResult.errs(erb.errors());
        }
      }
      var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, valueType, entryCv));
      if (convertedOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      var jsValOpt = erb.mustEr(valueToDynamic.toDynamic(xx, loc, convertedOpt.get()));
      if (jsValOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      seqElements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.DUP)));
      seqElements.add(new JavaBytecode.LoweredSequence.Code(keyCodeOpt.get()));
      seqElements.add(new JavaBytecode.LoweredSequence.Code(jsValOpt.get()));
      seqElements.add(
          new JavaBytecode.LoweredSequence.Bytecode(
              JavaBytecodeUtils.callMethod(-1, torderedMapIntname, "put", putDesc)));
      seqElements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.POP)));
    }
    var newMap = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(seqElements));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, newMap),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
  }

  private static boolean typesConvertible(
      ExprContext xx, QExprId loc, Type sourceType, Type destType) {
    return sourceType.typeCanCastTo(xx, loc, destType) || destType.typeCanCastFrom(xx, loc, sourceType);
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
