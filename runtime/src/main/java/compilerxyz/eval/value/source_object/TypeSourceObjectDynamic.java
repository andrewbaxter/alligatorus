package compilerxyz.eval.value.source_object;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinFull;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.External;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.TargetCode;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.function.FunctionParam;
import compilerxyz.eval.value.function.TypeSourceMethodDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.Binding;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.gen.GenAspect;
import compilerxyz.gen.GenCodegenDescriptors;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectFieldUseDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenBinding;
import compilerxyz.gen.GenCode;
import compilerxyz.gen.GenTarget;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenType;
import compilerxyz.gen.GenValue;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.Counter;
import compilerxyz.eval.dynamic.EmitCtx;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.TypeInsnNode;

@BuiltinFull(uniqueId = 6020L)
public final class TypeSourceObjectDynamic
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectType,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectLift,
        AspectStackValueDynamic,
        AspectFieldUseDynamic,
        ExporterAny {

  public Opt<DefSetId> setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Opt<String> name;
  public Opt<String> description = Opt.none();
  public Opt<Type> underlyingType = Opt.none();
  public final Unique identity;
  public int objIdx;
  public final TList<SourceDataMethodData> methods;
  public final TList<SourceObjectInterfaceImplData> interfaceImpls;
  public final ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new ArtifactSupport.ArtifactIdHolder();
  public final ArtifactSupport.ArtifactIdHolder newArtifactIdCell =
      new ArtifactSupport.ArtifactIdHolder();

  public Opt<String> compiledBinaryName = Opt.none();
  public Opt<byte[]> compiledBytecode = Opt.none();
  public Opt<TOrderedMap<String, Object>> compiledStaticFieldValues = Opt.none();

  public TypeSourceObjectDynamic(
      DefSetId setId, String name, Unique identity, DefinitionSet set) {
    this.setId = Opt.some(setId);
    this.set = set;
    this.name = Opt.some(name);
    this.identity = identity;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public TypeSourceObjectDynamic(
      Opt<DefSetId> setId, Opt<String> name, Unique identity) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.name = name;
    this.identity = identity;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
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
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    if (setId.optIsNone() || set == null) {
      return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }
    return JavaDataDescriptor.fromInternalName(
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objIdx)));
  }


  @Override
  public String typeDesc() {
    return description.optUnwrapOr("object(" + name.optUnwrapOr("?") + ")");
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(
      ExprContext xx, QExprId loc) {
    return new TypeTypeSourceObjectConst(this).artifactableExportArtifact(xx, loc);
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(identity)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)) {
      return Opt.some(this);
    }

    for (var impl : interfaceImpls) {
      var kindOpt = impl.interface_.builtinKind;
      if (kindOpt.optIsSome()) {
        boolean exposes = switch (kindOpt.optUnwrap()) {
          case ASPECT_FIELD -> key.equals(GenAspectField.AspectId);
          case ASPECT_LIFT -> key.equals(GenAspectLift.AspectId);
          case TYPE -> key.equals(GenAspectType.AspectId);
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
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, ValueDynamicSource source) {
    return EResult.ok(
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
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (cv.value().optIsNone()) {
      return EResult.err(loc, "TypeSourceObjectDynamic::toDynamic: no compile-time value");
    }
    if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc)) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::toDynamic: expected ValueObjectConst, got "
              + cv.value().optUnwrap().getClass().getName());
    }
    Object hostValue = voc.data();
    if (set != null) {
      xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    }
    var key = new JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(
            key, this, new External(hostValue.getClass(), hostValue)));
    return EResult.ok(
        BindingCode.bindingUse(
            new CodeLoc.Some(loc), xx.vx, id));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    var ck = ckOpt.optUnwrap();
    ConstKey.Str keyStr;
    switch (ck) {
      case ConstKey.Str s -> keyStr = s;
      case ConstKey.Uint u -> {
        return EResult.err(
            loc,
            "Field "
                + ck.constKeyToCanonical()
                + " not found on object (underlying-type Record fallback deferred)");
      }
    }
    String keyName = keyStr.value();
    String fieldName = null;
    Type argType = null;
    Opt<Type> retType = Opt.none();
    for (int i = 0; i < methods.size(); i++) {
      var m = methods.get(i);
      if (m.name.equals(keyName)) {
        retType = m.retType;
        argType = m.argType.optUnwrapOr(new TypeNullDynamic());
        fieldName = SourceObjectEmitter.methodMethodName(i);
        break;
      }
    }
    if (fieldName == null) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic::field: field '"
              + keyName
              + "' not found on object (underlying-type Record fallback deferred)");
    }
    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
      return EResult.err(loc, "TypeSourceObjectDynamic::field: receiver is not a ValueDynamic");
    }

    if (set != null) {
      xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    }
    JavaDataDescriptor retJvmDesc;
    if (retType.optIsSome()) {
      if (!(retType.optUnwrap() instanceof AspectConsumeDynamic retConsume)) {
        return EResult.err(loc, "TypeSourceObjectDynamic::field: retType does not implement AspectConsumeDynamic");
      }
      retJvmDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      retJvmDesc = JavaDataDescriptor.DESC_VOID;
    }
    var slotsRes = DynUtil.argSignatureSlots(xx, loc, argType);
    if (slotsRes.hasErrors()) return slotsRes.reerr();
    var slots = slotsRes.requireValue("TypeSourceObjectDynamic::field argSignatureSlots");
    var argDescs = new TList<JavaDataDescriptor>(slots.size());
    for (var s : slots) argDescs.add(s.jvmDesc());
    var descObjObj =
        JavaMethodDescriptor.fromParts(retJvmDesc, argDescs);
    if (setId.optIsNone()) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::field: setId is null on '"
              + name.optUnwrapOr("(unnamed)")
              + "'");
    }
    var objectInternalName =
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objIdx));
    var fieldType =
        new TypeSourceMethodDynamic(
            this,
            TList.of(new FunctionParam("arg", argType)),
            retType,
            objectInternalName,
            fieldName,
            descObjObj,
            false,
            false);
    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfMethodVd)) {
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
      ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
    return EResult.ok(new ValueObjectConst(
        new TypeSourceObjectConst(this), externalHostValue.optUnwrapOr(null)));
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
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.TYPE) {
          return true;
        }
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.CODE_DYN) {
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
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.TYPE) {
          hasTypeImpl = true;
          break;
        }
      }
      if (hasTypeImpl) {
        var sourceCode = cv.code().codeDynamic();
        if (sourceCode == null) {
          return EResult.err(loc, "cast " + typeDesc() + " -> any: source has no runtime code");
        }
        return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, GenType.dynamicType()));
      }
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.CODE_IDENTITY).optIsSome()) {
      for (var impl : interfaceImpls) {
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.CODE_DYN) {
          var codeSource = cv.code().codeDynamic();
          if (codeSource == null) {
            return EResult.err(
                loc, "cast " + typeDesc() + " -> code: source has no runtime code");
          }
          var wrapSeq = new JavaBytecodeSequence();
          wrapSeq.add(codeSource);
          wrapSeq.add(
              JavaBytecodeUtils.callStaticMethod(
                  new CodeLoc.Some(loc), GenCodegenDescriptors.CODE_TARGET));
          return EResult.ok(
              new CodeValue(
                  Code.of(
                      loc,
                      new CodeDynamic(new CodeLoc.Some(loc), wrapSeq)),
                  EvalOutcome.ofValue(
                      ValueDynamic.stack(GenCode.dynamicType(), 1))));
        }
      }
      return EResult.err(loc, "cast " + typeDesc() + " -> code: object does not implement code_dyn");
    }
    for (var impl : interfaceImpls) {
      if (destType.typeAspect(xx, impl.interface_.identity).optIsSome()) {
        var builtinKind = impl.interface_.builtinKind;
        Type resultType = null;
        if (builtinKind.optIsSome()) {
          resultType = switch (builtinKind.optUnwrap()) {
            case TYPE -> GenType.dynamicType();
            case TARGET -> GenTarget.dynamicType();
            case BINDING -> GenBinding.dynamicType();
            case VALUE -> GenValue.dynamicType();
            default -> null;
          };
        }
        if (resultType != null) {
          var sourceCode = cv.code().codeDynamic();
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
      var sourceCode = cv.code().codeDynamic();
      if (sourceCode == null) {
        return EResult.err(loc, "cast " + typeDesc() + " -> aspect: source has no runtime code");
      }
      return EResult.ok(emitBuiltinCast(xx, loc, sourceCode, GenAspect.dynamicType()));
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
        if (impl.interface_.builtinKind.optIsSome()
            && impl.interface_.builtinKind.optUnwrap() == BuiltinInterfaceKind.CODE_DYN) {
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
    return EResult.ok(new ValueObjectConst(new TypeTypeSourceObjectConst(this), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (value instanceof ValueObjectConst voc
        && voc.type() instanceof TypeTypeSourceObjectConst ttsoc) {
      return ttsoc.aspectTypeType(value);
    }
    return this;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    var codeSource = cv.code().codeDynamic();
    if (codeSource == null) {
      return EResult.err(loc, "cast code -> " + typeDesc() + ": source has no runtime code");
    }
    if (setId.optIsNone()) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::typeCastFrom: setId is null on '"
              + name.optUnwrapOr("(unnamed)")
              + "'");
    }
    var objectInternalName =
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objIdx));
    var unwrapSeq = new JavaBytecodeSequence();
    unwrapSeq.add(codeSource);
    unwrapSeq.add(
        JavaBytecodeUtils.callStaticMethod(
            new CodeLoc.Some(loc), GenCodegenDescriptors.CODE_TARGET_IMPL));
    unwrapSeq.add(
        JavaBytecodeUtils.wrapObj(
            new CodeLoc.Some(loc),
            new TypeInsnNode(
                Opcodes.CHECKCAST, objectInternalName.value)));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(new CodeLoc.Some(loc), unwrapSeq)),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
