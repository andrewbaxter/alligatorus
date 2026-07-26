package compilerxyz.eval.value.source_object;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.Builtin;

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
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.eval.dynamic.DynUtil;
import org.jspecify.annotations.Nullable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectType;

@Builtin(uniqueId = 6020L)
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
        AspectStackValueDynamic,
        compilerxyz.gen.AspectFieldUseDynamic,
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
      compilerxyz.eval.ExprContext xx, QExprId loc, long objectIndex) {
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    if (setId == null || set == null) {
      return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
          compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
    }
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
          compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
    }
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.of(
            compilerxyz.eval.dynamic.SourceObjectEmitter.internalNameFor(setId, objectIndex)));
  }

  @Override
  public String typeDesc() {
    if (description != null) return description;
    return "object(" + (name != null ? name : "?") + ")";
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.artifact.Artifact> exportArtifact(
      ExprContext xx, QExprId loc) {
    return new TypeTypeSourceObjectConst(this).exportArtifact(xx, loc);
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeSourceObjectDynamic.class, "source_object_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(identity)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }

    for (var impl : interfaceImpls) {
      var kind = impl.interface_.builtinKind;
      if (kind != null) {
        boolean exposes = switch (kind) {
          case ASPECT_FIELD -> key.equals(compilerxyz.gen.GenAspectField.AspectId);
          case ASPECT_LIFT -> key.equals(GenAspectLift.AspectId);
          case TYPE -> key.equals(compilerxyz.gen.GenAspectType.AspectId);
          case ASPECT_BIND, ASPECT_BIND_DYNAMIC, ASPECT_CONSUME_DYNAMIC,
              ASPECT_DROP_DYNAMIC_LIGHT, ASPECT_FIELD_USE_DYNAMIC,
              ASPECT_ELEMENT_USE_DYNAMIC, ASPECT_INITIAL_BIND, BINDING,
              ASPECT_DROP_DYNAMIC, ASPECT_FUNCTION,
              ASPECT_OPERATOR_ADD, ASPECT_OPERATOR_MOD, ASPECT_SET,
              VALUE, TARGET, CODE_DYN -> false;
        };
        if (exposes) return Opt.some(this);
      }
    }
    return Opt.none();
  }

  @Override
  public compilerxyz.eval.EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, compilerxyz.eval.value.ValueDynamicSource source) {
    return compilerxyz.eval.EResult.ok(
        new CodeValue(
            Code.of(loc, DynUtil.renderSource(xx, loc, source)),
            EvalOutcome.ofValue(new ValueDynamic(source, this))));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
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
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::toDynamic: no compile-time value");
    }
    if (!(some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst voc)) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::toDynamic: expected ValueObjectConst, got "
              + some.value().getClass().getName());
    }
    Object hostValue = voc.data();
    if (set != null) {
      xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    }
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new compilerxyz.eval.EvalContext.ExternalEntry(
            key, this, new compilerxyz.eval.External(hostValue.getClass(), hostValue)));
    return EResult.ok(
        compilerxyz.eval.dynamic.BindingCode.bindingUse(
            new compilerxyz.eval.dynamic.CodeLoc.Some(loc), xx.vx, id));
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
    if (!(self.value() instanceof Opt.Some<Value> selfMethodSome)
        || !(selfMethodSome.value() instanceof ValueDynamic selfMethodVd)) {
      return EResult.err(
          loc, "TypeSourceObjectDynamic::field: method receiver has no dynamic source");
    }
    return EResult.ok(
        new CodeValue(
            self.code(),
            EvalOutcome.ofValue(new ValueDynamic(selfMethodVd.source(), fieldType))));
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(
        new TypeSourceObjectConst(this), externalHostValue));
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
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.TYPE) {
          return true;
        }
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.CODE_DYN) {
          return true;
        }
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, identity).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
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
        return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, compilerxyz.gen.GenType.dynamicType()));
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.CODE_DYN) {
          var codeSource = cv.code().dynamic();
          if (codeSource == null) {
            return EResult.err(
                loc, "cast " + typeDesc() + " -> code: source has no runtime code");
          }
          var wrapSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
          wrapSeq.add(codeSource);
          wrapSeq.add(
              compilerxyz.eval.dynamic.JavaBytecodeUtils.callStaticInterfaceMethod(
                  new compilerxyz.eval.dynamic.CodeLoc.Some(loc),
                  compilerxyz.eval.dynamic.JavaInternalName.fromClass(Code.class),
                  "target",
                  compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                      compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Code.class),
                      TList.of(
                          compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(
                              compilerxyz.eval.TargetCode.class)))));
          return EResult.ok(
              new CodeValue(
                  Code.of(
                      loc,
                      new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), wrapSeq)),
                  EvalOutcome.ofValue(
                      ValueDynamic.stack(compilerxyz.gen.GenCode.dynamicType(), 1))));
        }
      }
      return EResult.err(loc, "cast " + typeDesc() + " -> code: object does not implement code_dyn");
    }
    for (var impl : interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        var builtinKind = impl.interface_.builtinKind;
        Type resultType = null;
        if (builtinKind != null) {
          switch (builtinKind) {
            case TYPE -> resultType = compilerxyz.gen.GenType.dynamicType();
            case TARGET -> resultType = compilerxyz.gen.GenTarget.dynamicType();
            case BINDING -> resultType = compilerxyz.gen.GenBinding.dynamicType();

            case VALUE -> resultType = compilerxyz.gen.GenValue.dynamicType();
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
                EvalOutcome.ofValue(ValueDynamic.stack(new TypeSourceInterfaceDynamic(impl.interface_), 1))));
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.ASPECT_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(loc, "cast " + typeDesc() + " -> aspect: source has no runtime code");
      }
      return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, compilerxyz.gen.GenAspect.dynamicType()));
    }
    return EResult.err(
        loc, "Cannot convert " + typeDesc() + " to " + destType.typeDesc());
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, identity).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    if (sourceType.typeAspect(xx, identity).optIsSome()) {
      return true;
    }
    if (sourceType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind == BuiltinInterfaceKind.CODE_DYN) {
          return true;
        }
      }
    }
    return false;
  }

  private CodeValue emitBuiltinCast(
      ExprContext xx, QExprId loc, CodeDynamic sourceCode, Type resultType) {
    return new CodeValue(Code.of(loc, sourceCode), EvalOutcome.ofValue(ValueDynamic.stack(resultType, 1)));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
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
    var codeSource = cv.code().dynamic();
    if (codeSource == null) {
      return EResult.err(loc, "cast code -> " + typeDesc() + ": source has no runtime code");
    }
    if (setId == null) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::typeCastFrom: setId is null on '"
              + (name == null ? "(unnamed)" : name)
              + "'");
    }
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      throw new AssertionError("TypeSourceObjectDynamic::typeCastFrom: this not in its owning set");
    }
    var objectInternalName =
        compilerxyz.eval.dynamic.JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId, objectIndex));
    var unwrapSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    unwrapSeq.add(codeSource);
    unwrapSeq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.callStaticInterfaceMethod(
            new compilerxyz.eval.dynamic.CodeLoc.Some(loc),
            compilerxyz.eval.dynamic.JavaInternalName.fromClass(Code.class),
            "targetImpl",
            compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(
                    compilerxyz.eval.TargetCode.class),
                TList.of(
                    compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Code.class)))));
    unwrapSeq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.wrapObj(
            new compilerxyz.eval.dynamic.CodeLoc.Some(loc),
            new org.objectweb.asm.tree.TypeInsnNode(
                org.objectweb.asm.Opcodes.CHECKCAST, objectInternalName.value)));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), unwrapSeq)),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
  }
}
