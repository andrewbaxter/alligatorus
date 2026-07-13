package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceConst;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.eval.TypeId;
import org.jspecify.annotations.Nullable;

public final class TypeSourceObjectDynamic
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        compilerxyz.gen.AspectType,
        AspectInitialBind,
        AspectLift,
        ExporterAny {

  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final @Nullable String name;
  public @Nullable String description;
  public @Nullable Type underlyingType;
  public final Unique identity;
  public int objIdx;
  public final TList<SourceDataMethodData> methods;
  public final TList<SourceObjectInterfaceImplData> interfaceImpls;
  public final compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder();
  public final compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder newArtifactIdCell =
      new compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder();

  public @Nullable String compiledBinaryName;
  public byte @Nullable [] compiledBytecode;
  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;

  public TypeSourceObjectDynamic(
      DefSetId setId, @Nullable String name, Unique identity, DefinitionSet set) {
    this.setId = setId;
    this.set = set;
    this.name = name;
    this.identity = identity;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public TypeSourceObjectDynamic(
      @Nullable DefSetId setId, @Nullable String name, Unique identity) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.name = name;
    this.identity = identity;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
  }

  public EResult<Void> finish(
      compilerxyz.eval.ExprContext xx,
      QExprId loc,
      long objectIndex,
      DefinitionSetClassLoader loader) {
    if (compiledBinaryName != null) {
      return EResult.ok(null);
    }
    if (setId == null) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic.finish: setId is null for object '"
              + (name == null ? "(unnamed)" : name)
              + "'");
    }
    var emitted = SourceObjectEmitter.emit(xx, setId, objectIndex, this);
    compiledBinaryName = emitted.binaryName();
    compiledBytecode = emitted.bytecode();
    compiledStaticFieldValues = emitted.staticFieldValues();
    loader.registerClass(emitted.binaryName(), emitted.bytecode());
    var fields = new TOrderedMap<String, Object>();
    fields.putAll(emitted.staticFieldValues());
    fields.put(compilerxyz.eval.dynamic.EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
    loader.registerExternals(emitted.binaryName(), fields);
    return EResult.ok(null);
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = TypeSourceObjectDynamic.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "TypeSourceObjectDynamic.initSetReflectively: failed to write final 'set' field", e);
    }
  }

  @Override
  public TypeId typeId() {
    return new TypeId.DefsetMember(set.defSetId, uniqueId(identity));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    if (description != null) return description;
    return "object(" + (name != null ? name : "?") + ")";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceObjectDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(identity)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)) {
      return Opt.some(this);
    }

    for (var impl : interfaceImpls) {
      if (key.equals(impl.interface_.identity)) {
        return Opt.some(this);
      }
      var kind = impl.interface_.builtinKind;
      if (kind != null) {
        boolean exposes = switch (kind) {
          case ASPECT_BIND_DYNAMIC -> key.equals(AspectBindDynamic.ID);
          case ASPECT_CONSUME_DYNAMIC -> key.equals(AspectConsumeDynamic.ID);
          case ASPECT_DROP_DYNAMIC -> key.equals(compilerxyz.gen.AspectDropDynamic.ID);
          case ASPECT_FIELD -> key.equals(compilerxyz.gen.AspectField.ID);
          case ASPECT_FUNCTION -> key.equals(compilerxyz.gen.AspectFunction.ID);
          case BINDING -> key.equals(AspectBindDynamic.ID);
          case TYPE -> key.equals(compilerxyz.gen.AspectType.ID);
          case VALUE, TARGET, CODE_DYN -> false;
        };
        if (exposes) return Opt.some(this);
      }
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeSourceObjectDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(
          loc,
          "Field "
              + ck.toCanonical()
              + " not found on object (underlying-type Record fallback deferred)");
    }
    String keyName = keyStr.value();
    String fieldName = null;
    Type argType = null;
    Type retType = null;
    for (int i = 0; i < methods.size(); i++) {
      var m = methods.get(i);
      if (m.name.equals(keyName)) {
        if (m.retType == null) {
          throw new IllegalStateException("Data type method missing return type");
        }
        retType = m.retType;
        argType = m.argType != null ? m.argType : new TypeNullDynamic();
        fieldName = compilerxyz.eval.dynamic.SourceObjectEmitter.methodMethodName(i);
        break;
      }
    }
    if (fieldName == null) {
      outer:
      for (int implIdx = 0; implIdx < interfaceImpls.size(); implIdx++) {
        var impl = interfaceImpls.get(implIdx);
        for (int mIdx = 0; mIdx < impl.interface_.methods.size(); mIdx++) {
          var method = impl.interface_.methods.get(mIdx);
          if (method.name.equals(keyName)) {
            if (method.argType == null) {
              throw new IllegalStateException("Interface method missing arg type");
            }
            if (method.retType == null) {
              throw new IllegalStateException("Interface method missing return type");
            }
            argType = method.argType;
            retType = method.retType;
            fieldName =
                compilerxyz.eval.dynamic.SourceObjectEmitter.interfaceMethodMethodName(
                    implIdx, mIdx);
            break outer;
          }
        }
      }
    }
    if (fieldName == null) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::field: field '"
              + keyName
              + "' not found on object (underlying-type Record fallback deferred)");
    }
    if (!(self.value() instanceof Opt.Some<Value> selfSome)
        || !(selfSome.value() instanceof ValueDynamic selfVd)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: receiver is not a ValueDynamic");
    }

    if (set != null) {
      xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    }
    if (!(retType instanceof compilerxyz.gen.AspectConsumeDynamic retConsume)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: retType does not implement AspectConsumeDynamic");
    }
    var slotsRes = compilerxyz.eval.dynamic.DynUtil.argSignatureSlots(xx, loc, argType);
    if (slotsRes.hasErrors()) return slotsRes.reerr();
    var slots = slotsRes.requireValue("TypeSourceObjectDynamic::field argSignatureSlots");
    var argDescs =
        new TList<compilerxyz.eval.dynamic.JavaDataDescriptor>(slots.size());
    for (var s : slots) argDescs.add(s.jvmDesc());
    var descObjObj =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            retConsume.aspectConsumeDynamicJvmClass(xx),
            argDescs);
    if (setId == null) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::field: setId is null on '"
              + (name == null ? "(unnamed)" : name)
              + "'");
    }
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::field: this not in its owning set");
    }
    var objectInternalName =
        compilerxyz.eval.dynamic.JavaInternalName.of(
            compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(setId, objectIndex));
    var fieldType =
        new compilerxyz.eval.value.function.TypeMethodDynamic(
            this,
            argType,
            retType,
            objectInternalName,
            fieldName,
            descObjObj,
            false,
            false);
    return EResult.ok(new CodeValue(self.code(), Opt.some(ValueDynamic.stack(xx, fieldType))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @Nullable Object externalHostValue) {
    var ut = underlyingType;
    if (ut == null) {
      return EResult.err(
          loc, "TypeSourceObjectDynamic::lift: object underlying type not finalised");
    }
    var underlyingLiftOpt = AspectLift.getMaybe(xx, ut);
    if (underlyingLiftOpt.optIsNone()) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::lift: underlying type ("
              + ut.typeDesc()
              + ") missing AspectLift");
    }
    var underlyingLift = underlyingLiftOpt.optUnwrap();
    Object underlyingHost;

    if (externalHostValue instanceof SourceObjectInstance) {
      try {
        var field = externalHostValue.getClass().getField(SourceObjectEmitter.DATA_INSTANCE_FIELD);
        underlyingHost = field.get(externalHostValue);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new AssertionError(
            "TypeSourceObjectDynamic::lift: source-object instance "
                + externalHostValue.getClass().getName()
                + " missing "
                + SourceObjectEmitter.DATA_INSTANCE_FIELD
                + " field",
            e);
      }
    } else {
      underlyingHost = externalHostValue;
    }
    return underlyingLift
        .aspectLiftLift(xx, loc, underlyingHost)
        .map(underlying -> new compilerxyz.eval.value.object_.ValueObjectConst(
            new compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst(this), underlying));
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    if (destType.typeAspect(xx, identity).optIsSome()) {
      return true;
    }
    for (var impl : interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        return true;
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.TYPE) {
          return true;
        }
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_ASPECT_IDENTITY).optIsSome()) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, identity).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), Opt.some(ValueDynamic.stack(xx, this))));
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      boolean hasTypeImpl = false;
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.TYPE) {
          hasTypeImpl = true;
          break;
        }
      }
      if (hasTypeImpl) {
        var sourceCode = cv.code().dynamic();
        if (sourceCode == null) {
          return EResult.err(loc, "cast " + typeDesc() + " -> any: source has no runtime code");
        }
        return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, Type.dynamicType()));
      }
    }
    for (var impl : interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        var builtinKind = impl.interface_.builtinKind;
        Type resultType = null;
        if (builtinKind != null) {
          switch (builtinKind) {
            case TYPE -> resultType = Type.dynamicType();
            case TARGET -> resultType = compilerxyz.gen.Target.dynamicType();
            case BINDING -> resultType = compilerxyz.gen.Binding.dynamicType();
            case CODE_DYN -> {
              return EResult.err(
                  loc,
                  "TypeSourceObjectDynamic::typeCastTo: BuiltinInterfaceKind.CODE_DYN's "
                      + "invisible builtin data_instance_to_code is not yet ported");
            }
            case VALUE -> {
              return EResult.err(
                  loc,
                  "TypeSourceObjectDynamic::typeCastTo: BuiltinInterfaceKind.VALUE lowers through "
                      + "DATA_INSTANCE_TO_VALUE_FN to TypeValueDynamic; TypeValueDynamic is not "
                      + "yet ported on the JVM side (deferred)");
            }
            default -> {

            }
          }
        }
        if (resultType != null) {
          var sourceCode = cv.code().dynamic();
          if (sourceCode == null) {
            return EResult.err(
                loc, "cast " + typeDesc() + " -> interface: source has no runtime code");
          }
          return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, resultType));
        }
        return EResult.ok(
            new CodeValue(
                cv.code(),
                Opt.some(ValueDynamic.stack(xx, new TypeSourceInterfaceConst(impl.interface_)))));
      }
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_ASPECT_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(loc, "cast " + typeDesc() + " -> aspect: source has no runtime code");
      }
      return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, compilerxyz.eval.aspect.Aspect.dynamicType()));
    }
    return EResult.err(loc, "Cannot convert " + typeDesc());
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, identity).optIsSome();
  }

  private CodeValue emitBuiltinCast(
      ExprContext xx, QExprId loc, CodeDynamic sourceCode, Type resultType) {
    return new CodeValue(Code.of(loc, sourceCode), Opt.some(ValueDynamic.stack(xx, resultType)));
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
  public Type aspectTypeType(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst voc
        && voc.type() instanceof TypeTypeSourceObjectConst ttsoc) {
      return ttsoc.aspectTypeType(value);
    }
    return this;
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
}
