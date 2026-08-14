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
import compilerxyz.eval.value.function.TypeMethodDynamic;
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

  public EResult<Void> finish(
      ExprContext xx, QExprId loc, long objectIndex) {
    if (compiledBinaryName.optIsSome()) {
      return EResult.ok(null);
    }
    if (setId.optIsNone()) {
      return EResult.err(
          loc,
          "TypeSourceObjectDynamic.finish: setId is null for object '"
              + name.optUnwrapOr("(unnamed)")
              + "'");
    }
    {
      var _soeBinaryName = SourceObjectEmitter.binaryNameFor(setId.optUnwrap(), objectIndex);
      var _soeInternalNameStr = _soeBinaryName.replace('.', '/');
      var _soeInternalName = JavaInternalName.of(_soeInternalNameStr);
      var _soeClaimedKinds = new java.util.HashSet<BuiltinInterfaceKind>();
      for (var _soeImpl : interfaceImpls) {
        if (_soeImpl.interface_.builtinKind.optIsSome()) {
          _soeClaimedKinds.add(_soeImpl.interface_.builtinKind.optUnwrap());
        }
      }
      var _soeImplementsInternalNames = new TOrderedSet<String>();
      _soeImplementsInternalNames.add(SourceObjectEmitter.INTNAME_SOURCE_OBJECT_INSTANCE.value());
      for (var _soeImpl : interfaceImpls) {
        var _soeIface = _soeImpl.interface_;
        _soeImplementsInternalNames.add(_soeIface.compiledBinaryName.replace('.', '/'));
      }
      var _soeInterfacesArr =
          _soeImplementsInternalNames.isEmpty()
              ? null
              : _soeImplementsInternalNames.toArray(new String[0]);
      var _soeCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
        @Override
        protected String getCommonSuperClass(String type1, String type2) {
          return JvmConstants.INTNAME_OBJECT.value();
        }
      };
      _soeCw.visit(
          Opcodes.V11,
          Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
          _soeInternalNameStr,
          null,
          JvmConstants.INTNAME_OBJECT.value(),
          _soeInterfacesArr);
      JavaDataDescriptor _soeDataFieldDesc;
      var _soeUnderlyingTypeMaybe = underlyingType.optUnwrapOr(null);
      if (_soeUnderlyingTypeMaybe instanceof AspectConsumeDynamic _soeUnderlyingConsume) {
        _soeDataFieldDesc = _soeUnderlyingConsume.aspectConsumeDynamicJvmClass(xx);
      } else {
        _soeDataFieldDesc = JvmConstants.DESC_OBJECT;
      }
      _soeCw.visitField(Opcodes.ACC_PUBLIC, SourceObjectEmitter.DATA_INSTANCE_FIELD, _soeDataFieldDesc.value(), null, null)
          .visitEnd();
      var _soeCtor = _soeCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", JavaMethodDescriptor.VOID_NOARGS.value(), null, null);
      _soeCtor.visitCode();
      var _soeCtorSeq = new JavaBytecodeSequence();
      _soeCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
      _soeCtorSeq.add(JavaBytecodeUtils.invokeSpecial(CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS));
      _soeCtorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
      JavaBytecodeUtils.renderMethod(_soeCtorSeq, _soeCtor, TList.of());
      var _soeRealCtorDesc =
          JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(_soeDataFieldDesc));
      var _soeRealCtor =
          _soeCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", _soeRealCtorDesc.value(), null, null);
      _soeRealCtor.visitCode();
      var _soeRealCtorSeq = new JavaBytecodeSequence();
      _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
      _soeRealCtorSeq.add(JavaBytecodeUtils.invokeSpecial(CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS));
      _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
      {
        var _soeC = _soeDataFieldDesc.value().charAt(0);
        int _soeLoadOp = switch (_soeC) {
          case 'J' -> Opcodes.LLOAD;
          case 'D' -> Opcodes.DLOAD;
          case 'F' -> Opcodes.FLOAD;
          case 'I', 'B', 'C', 'S', 'Z' -> Opcodes.ILOAD;
          default -> Opcodes.ALOAD;
        };
        _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, _soeLoadOp, 1));
      }
      _soeRealCtorSeq.add(JavaBytecodeUtils.setField(CodeLoc.Infallible.INSTANCE, _soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, _soeDataFieldDesc));
      _soeRealCtorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
      JavaBytecodeUtils.renderMethod(_soeRealCtorSeq, _soeRealCtor, TList.of());
      var _soeAllFieldNames = new TOrderedSet<String>();
      var _soeStaticFieldValues = new TOrderedMap<String, Object>();
      var _soeFieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
      for (int _soeImplIdx = 0; _soeImplIdx < interfaceImpls.size(); _soeImplIdx++) {
        var _soeImpl = interfaceImpls.get(_soeImplIdx);
        for (int _soeMIdx = 0; _soeMIdx < _soeImpl.methodImpls.size(); _soeMIdx++) {
          var _soeM = _soeImpl.methodImpls.get(_soeMIdx);
          if (_soeM.compiledBodyCode.optIsNone()) {
            continue;
          }
          var _soePrefix = SourceObjectEmitter.interfaceMethodFieldPrefix(_soeImplIdx, _soeMIdx);
          for (int _cmfI = 0; _cmfI < _soeM.capturedExternals.size(); _cmfI++) {
            var _cmfEntry = _soeM.capturedExternals.get(_cmfI);
            var _cmfName = _soePrefix + Prefixes.EXTERNAL_PREFIX + _cmfI;
            if (_soeAllFieldNames.add(_cmfName)) {
              _soeFieldDescriptors.put(_cmfName, JavaDataDescriptor.fromClass(_cmfEntry.external().externalHostClass()));
              _soeStaticFieldValues.put(_cmfName, _cmfEntry.external().externalHostValue());
            }
          }
        }
      }
      for (long _soeMethodIdx = 0; _soeMethodIdx < methods.size(); _soeMethodIdx++) {
        var _soeMethod = methods.get((int) _soeMethodIdx);
        var _soeBodyCode = _soeMethod.compiledBodyCode;
        if (_soeBodyCode == null) {
          continue;
        }
        var _soePrefix = SourceObjectEmitter.methodFieldPrefix(_soeMethodIdx);
        for (int _soeI = 0; _soeI < _soeMethod.capturedExternals.size(); _soeI++) {
          var _soeEntry = _soeMethod.capturedExternals.get(_soeI);
          var _soeName = _soePrefix + Prefixes.EXTERNAL_PREFIX + _soeI;
          if (_soeAllFieldNames.add(_soeName)) {
            _soeFieldDescriptors.put(
                _soeName, JavaDataDescriptor.fromClass(_soeEntry.external().externalHostClass()));
            _soeStaticFieldValues.put(_soeName, _soeEntry.external().externalHostValue());
          }
        }
      }
      for (var _soeName : _soeAllFieldNames) {
        var _soeDesc = _soeFieldDescriptors.getOrDefault(_soeName, JvmConstants.DESC_OBJECT);
        _soeCw.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                _soeName,
                _soeDesc.value(),
                null,
                null)
            .visitEnd();
      }
      if (_soeAllFieldNames.add(SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD)) {
        _soeCw.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD,
                JvmConstants.DESC_OBJECT.value(),
                null,
                null)
            .visitEnd();
      }
      _soeStaticFieldValues.put(SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD, this);
      _soeCw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              EvalRootEmitter.EXPR_CONTEXT_FIELD,
              EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
              null,
              null)
          .visitEnd();
      for (long _soeMethodIdx = 0; _soeMethodIdx < methods.size(); _soeMethodIdx++) {
        var _soeMethod = methods.get((int) _soeMethodIdx);
        if (_soeMethod.compiledBodyCode.optIsNone()) {
          continue;
        }
        {
          var _emmBodyCode = _soeMethod.compiledBodyCode.optUnwrap();
          var _emmUnderlyingType = underlyingType.optUnwrapOr(null);
          var _emmMethodName = SourceObjectEmitter.methodMethodName(_soeMethodIdx);
          var _emmPrefix = SourceObjectEmitter.methodFieldPrefix(_soeMethodIdx);
          JavaDataDescriptor _emmRetDesc;
          if (_soeMethod.retType.optIsSome()) {
            if (!(_soeMethod.retType.optUnwrap() instanceof AspectConsumeDynamic _emmRetConsume)) {
              throw new IllegalStateException(
                  "SourceObjectEmitter.emitMethodMethod: method '" + _soeMethod.name + "' retType does not implement AspectConsumeDynamic");
            }
            _emmRetDesc = _emmRetConsume.aspectConsumeDynamicJvmClass(xx);
          } else {
            _emmRetDesc = JavaDataDescriptor.DESC_VOID;
          }
          if (_soeMethod.compiledArgSlots.optIsNone() || _soeMethod.compiledArgSlotIds.optIsNone()) {
            throw new IllegalStateException(
                "SourceObjectEmitter.emitMethodMethod: method '" + _soeMethod.name + "' has no compiledArgSlots (finish ordering violation)");
          }
          var _emmArgSlots = _soeMethod.compiledArgSlots.optUnwrap();
          var _emmArgSlotIds = _soeMethod.compiledArgSlotIds.optUnwrap();
          var _emmArgDescs = new TList<JavaDataDescriptor>(_emmArgSlots.size());
          for (var _emmS : _emmArgSlots) _emmArgDescs.add(_emmS.jvmDesc());
          var _emmDescriptor = JavaMethodDescriptor.fromParts(_emmRetDesc, _emmArgDescs);
          var _emmMv = _soeCw.visitMethod(Opcodes.ACC_PUBLIC, _emmMethodName, _emmDescriptor.value(), null, null);
          var _emmTarget = new TargetDynamic(new Counter());
          int _emmTotalArgSlots = 0;
          for (var _emmS : _emmArgSlots) _emmTotalArgSlots += SourceObjectEmitter.slotWidth(_emmS.jvmDesc());
          var _emmCtx = new EmitCtx(_soeInternalName.value(), 1, 1 + _emmTotalArgSlots, _emmPrefix);
          if (_soeMethod.innerBindingId.optIsNone()) {
            throw new IllegalStateException(
                "SourceObjectEmitter.emit: method '" + _soeMethod.name + "' has no innerBindingId (finish ordering violation)");
          }
          var _emmSelfId = _soeMethod.innerBindingId.optUnwrap();
          var _emmSeq = new JavaBytecodeSequence();
          var _emmLoc = CodeLoc.Infallible.INSTANCE;
          var _emmSelfLocal = compilerxyz.util.MustCast.mustCast(compilerxyz.eval.BindingId.Local.class, _emmSelfId);
          JavaDataDescriptor _emmDataFieldDesc = switch (GenAspectConsumeDynamic.getMaybe(xx, _emmUnderlyingType)) {
            case compilerxyz.util.Opt.Some<AspectConsumeDynamic> s -> s.value().aspectConsumeDynamicJvmClass(xx);
            case compilerxyz.util.Opt.None<AspectConsumeDynamic> n ->
                throw new IllegalStateException(
                    "SourceObjectEmitter.emit: underlyingType " + _emmUnderlyingType.typeDesc() + " does not implement AspectConsumeDynamic; cannot emit data field access");
          };
          _emmSeq.add(JavaBytecodeUtils.varInsn(_emmLoc, Opcodes.ALOAD, 0));
          _emmSeq.add(JavaBytecodeUtils.accessField(_emmLoc, _soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, _emmDataFieldDesc));
          _emmSeq.add(new CodeDynamic(_emmLoc, new JavaBytecodeStoreLoad(SourceObjectEmitter.storeOpcodeFor(_emmDataFieldDesc), _emmSelfLocal.key())));
          var _emmInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
          _emmInitialSlots.add(new JavaBytecodeSequence.InitialSlot(new JavaBytecodeBindingKey()));
          for (int _emmI = 0; _emmI < _emmArgSlots.size(); _emmI++) {
            _emmInitialSlots.add(new JavaBytecodeSequence.InitialSlot(
                _emmArgSlotIds.get(_emmI).key(), SourceObjectEmitter.slotWidth(_emmArgSlots.get(_emmI).jvmDesc()) == 2));
          }
          for (int _emmI = 0; _emmI < _soeMethod.capturedExternals.size(); _emmI++) {
            var _emmEntry = _soeMethod.capturedExternals.get(_emmI);
            var _emmFieldName = _emmPrefix + Prefixes.EXTERNAL_PREFIX + _emmI;
            var _emmFieldDesc = JavaDataDescriptor.fromClass(_emmEntry.external().externalHostClass());
            _emmSeq.add(JavaBytecodeUtils.accessStaticField(_emmLoc, _soeInternalName, _emmFieldName, _emmFieldDesc));
            _emmSeq.add(new CodeDynamic(_emmLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, _emmEntry.key())));
          }
          _emmTarget.emit(_emmSeq, _emmCtx, _emmBodyCode);
          _emmSeq.add(JavaBytecodeUtils.inst(_emmLoc, SourceObjectEmitter.returnOpcodeFor(_emmRetDesc.value().charAt(0))));
          JavaBytecodeUtils.renderMethodTyped(_emmSeq, _emmMv, _emmInitialSlots);
        }
      }
      for (int _soeImplIdx = 0; _soeImplIdx < interfaceImpls.size(); _soeImplIdx++) {
        var _soeImpl = interfaceImpls.get(_soeImplIdx);
        for (int _soeMIdx = 0; _soeMIdx < _soeImpl.methodImpls.size(); _soeMIdx++) {
          var _soeM = _soeImpl.methodImpls.get(_soeMIdx);
          if (_soeM.compiledBodyCode.optIsNone()) {
            continue;
          }
          {
            var _eimMethodName = SourceObjectEmitter.interfaceMethodMethodName(_soeImplIdx, _soeMIdx);
            var _eimPrefix = SourceObjectEmitter.interfaceMethodFieldPrefix(_soeImplIdx, _soeMIdx);
            if (_soeM.innerBindingId.optIsNone()) {
              throw new IllegalStateException(
                  "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no innerBindingId");
            }
            var _eimSelfId = compilerxyz.util.MustCast.mustCast(compilerxyz.eval.BindingId.Local.class, _soeM.innerBindingId.optUnwrap());
            if (_soeM.compiledArgParamIds.optIsNone()) {
              throw new IllegalStateException(
                  "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
            }
            var _eimArgParamIds = _soeM.compiledArgParamIds.optUnwrap();
            var _eimIfaceMethod = _soeImpl.interface_.methods.get(_soeM.interfaceMethodIndex);
            var _eimDescriptor = SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, _soeM, _eimIfaceMethod);
            var _eimJvmParamKeys = new TList<JavaBytecodeBindingKey>();
            var _eimJvmParamWides = new TList<Boolean>();
            if (_soeM.compiledArgParamTypes.optIsNone() || _soeM.compiledArgParamTypes.optUnwrap().size() != _eimArgParamIds.size()) {
              throw new IllegalStateException(
                  "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
            }
            var _eimArgParamTypes = _soeM.compiledArgParamTypes.optUnwrap();
            for (int _eimI = 0; _eimI < _eimArgParamIds.size(); _eimI++) {
              var _eimArgType = _eimArgParamTypes.get(_eimI);
              var _eimArgConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, _eimArgType);
              if (_eimArgConsumeOpt.optIsNone()) {
                throw new IllegalStateException(
                    "SourceObjectEmitter.emitInterfaceMethodMethod: arg type " + _eimArgType.typeDesc()
                        + " does not implement AspectConsumeDynamic");
              }
              var _eimArgDesc = _eimArgConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
              _eimJvmParamKeys.add(_eimArgParamIds.get(_eimI).key());
              _eimJvmParamWides.add(SourceObjectEmitter.slotWidth(_eimArgDesc) == 2);
            }
            var _eimMv = _soeCw.visitMethod(Opcodes.ACC_PUBLIC, _eimMethodName, _eimDescriptor.value(), null, null);
            var _eimTarget = new TargetDynamic(new Counter());
            int _eimTotalParamSlots = 0;
            for (var _eimWide : _eimJvmParamWides) _eimTotalParamSlots += (_eimWide ? 2 : 1);
            var _eimCtx = new EmitCtx(_soeInternalName.value(), 1, 1 + _eimTotalParamSlots, _eimPrefix);
            var _eimSeq = new JavaBytecodeSequence();
            var _eimLoc = CodeLoc.Infallible.INSTANCE;
            if (_soeM.outerBindingId.optIsSome()) {
              switch (_soeM.outerBindingId.optUnwrap()) {
                case BindingId.Local outerLocal -> {
                  _eimSeq.add(JavaBytecodeUtils.varInsn(_eimLoc, Opcodes.ALOAD, 0));
                  _eimSeq.add(new CodeDynamic(_eimLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, outerLocal.key())));
                }
              }
            }
            JavaDataDescriptor _eimInnerDesc =
                underlyingType.optUnwrapOr(null) instanceof AspectConsumeDynamic underlyingConsume
                    ? underlyingConsume.aspectConsumeDynamicJvmClass(xx)
                    : JvmConstants.DESC_OBJECT;
            _eimSeq.add(JavaBytecodeUtils.varInsn(_eimLoc, Opcodes.ALOAD, 0));
            _eimSeq.add(JavaBytecodeUtils.accessField(_eimLoc, _soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, _eimInnerDesc));
            _eimSeq.add(new CodeDynamic(_eimLoc, new JavaBytecodeStoreLoad(SourceObjectEmitter.storeOpcodeFor(_eimInnerDesc), _eimSelfId.key())));
            var _eimInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
            _eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(new JavaBytecodeBindingKey()));
            for (int _eimI = 0; _eimI < _eimJvmParamKeys.size(); _eimI++) {
              _eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(
                  _eimJvmParamKeys.get(_eimI), _eimJvmParamWides.get(_eimI)));
            }
            for (int _eimI = 0; _eimI < _soeM.capturedExternals.size(); _eimI++) {
              var _eimEntry = _soeM.capturedExternals.get(_eimI);
              var _eimFieldName = _eimPrefix + Prefixes.EXTERNAL_PREFIX + _eimI;
              var _eimFieldDesc = JavaDataDescriptor.fromClass(_eimEntry.external().externalHostClass());
              _eimSeq.add(JavaBytecodeUtils.accessStaticField(_eimLoc, _soeInternalName, _eimFieldName, _eimFieldDesc));
              _eimSeq.add(new CodeDynamic(_eimLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, _eimEntry.key())));
            }
            _eimTarget.emit(_eimSeq, _eimCtx, _soeM.compiledBodyCode.optUnwrap());
            {
              var _ercDescStr = _eimDescriptor.value();
              var _ercRetDesc = _ercDescStr.substring(_ercDescStr.indexOf(')') + 1);
              var _ercRetChar = _ercRetDesc.charAt(0);
              if (_ercRetChar == 'L') {
                var _ercRetInternal = JavaInternalName.of(_ercRetDesc.substring(1, _ercRetDesc.length() - 1));
                _eimSeq.add(JavaBytecodeUtils.cast(_eimLoc, JavaDataDescriptor.fromInternalName(_ercRetInternal)));
                _eimSeq.add(JavaBytecodeUtils.inst(_eimLoc, Opcodes.ARETURN));
              } else {
                _eimSeq.add(JavaBytecodeUtils.inst(_eimLoc, SourceObjectEmitter.returnOpcodeFor(_ercRetChar)));
              }
            }
            JavaBytecodeUtils.renderMethodTyped(_eimSeq, _eimMv, _eimInitialSlots);
          }
        }
      }
      {
        var _ehiEmitted = new TOrderedSet<String>();
        for (int _ehiImplIdx = 0; _ehiImplIdx < interfaceImpls.size(); _ehiImplIdx++) {
          var _ehiImpl = interfaceImpls.get(_ehiImplIdx);
          var _ehiIface = _ehiImpl.interface_;
          for (int _ehiMIdx = 0; _ehiMIdx < _ehiImpl.methodImpls.size(); _ehiMIdx++) {
            var _ehiM = _ehiImpl.methodImpls.get(_ehiMIdx);
            if (_ehiM.compiledBodyCode.optIsNone()) {
              continue;
            }
            var _ehiIfaceMethod = _ehiIface.methods.get(_ehiM.interfaceMethodIndex);
            String _ehiHostName;
            JavaMethodDescriptor _ehiHostDescriptor;
            if (_ehiIfaceMethod.hostMethodName.optIsSome() && _ehiIfaceMethod.hostMethodDescriptor.optIsSome()) {
              _ehiHostName = _ehiIfaceMethod.hostMethodName.optUnwrap();
              _ehiHostDescriptor = _ehiIfaceMethod.hostMethodDescriptor.optUnwrap();
            } else {
              _ehiHostName = _ehiIfaceMethod.name;
              _ehiHostDescriptor = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
            }
            var _ehiDedupeKey = _ehiHostName + _ehiHostDescriptor.value();
            if (!_ehiEmitted.add(_ehiDedupeKey)) {
              continue;
            }
            {
              var _ehaMv = _soeCw.visitMethod(Opcodes.ACC_PUBLIC, _ehiHostName, _ehiHostDescriptor.value(), null, null);
              var _ehaHostDescStr = _ehiHostDescriptor.value();
              var _ehaLoc = CodeLoc.Infallible.INSTANCE;
              var _ehaSeq = new JavaBytecodeSequence();
              _ehaSeq.add(JavaBytecodeUtils.varInsn(_ehaLoc, Opcodes.ALOAD, 0));
              var _ehaCalleeDescriptor = SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, _ehiM, _ehiIfaceMethod);
              int _ehaHostSlot = 1;
              if (_ehiM.compiledArgParamTypes.optIsSome()) {
                var _ehaArgParamTypes = _ehiM.compiledArgParamTypes.optUnwrap();
                var _ehaHostParams = new TList<String>();
                { int _pdI = 1;
                  while (_pdI < _ehaHostDescStr.length() && _ehaHostDescStr.charAt(_pdI) != ')') {
                    int _pdStart = _pdI;
                    while (_ehaHostDescStr.charAt(_pdI) == '[') _pdI++;
                    if (_ehaHostDescStr.charAt(_pdI) == 'L') {
                      _pdI = _ehaHostDescStr.indexOf(';', _pdI) + 1;
                    } else {
                      _pdI++;
                    }
                    _ehaHostParams.add(_ehaHostDescStr.substring(_pdStart, _pdI));
                  }
                }
                int _ehaArgIdx = 0;
                for (var _ehaArgType : _ehaArgParamTypes) {
                  _ehaSeq.add(JavaBytecodeUtils.varInsn(_ehaLoc, Opcodes.ALOAD, _ehaHostSlot));
                  var _ehaArgConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, _ehaArgType);
                  if (_ehaArgConsumeOpt.optIsNone()) {
                    throw new IllegalStateException(
                        "SourceObjectEmitter.emitHostAdapter: arg type " + _ehaArgType.typeDesc()
                            + " does not implement AspectConsumeDynamic");
                  }
                  var _ehaArgDesc = _ehaArgConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
                  var _ehaHostParam = _ehaHostParams.get(_ehaArgIdx);
                  if (!_ehaHostParam.equals(_ehaArgDesc.value())
                      && (_ehaArgDesc.value().startsWith("L") || _ehaArgDesc.value().startsWith("["))
                      && !_ehaArgDesc.value().equals("Ljava/lang/Object;")) {
                    _ehaSeq.add(JavaBytecodeUtils.cast(_ehaLoc, _ehaArgDesc));
                  }
                  _ehaHostSlot += SourceObjectEmitter.slotWidth(_ehaArgDesc);
                  _ehaArgIdx++;
                }
              }
              _ehaSeq.add(JavaBytecodeUtils.callMethod(_ehaLoc,
                  _soeInternalName, SourceObjectEmitter.interfaceMethodMethodName(_ehiImplIdx, _ehiMIdx), _ehaCalleeDescriptor));
              var _ehaRetChar = _ehaHostDescStr.charAt(_ehaHostDescStr.indexOf(')') + 1);
              _ehaSeq.add(JavaBytecodeUtils.inst(_ehaLoc, SourceObjectEmitter.returnOpcodeFor(_ehaRetChar)));
              JavaBytecodeUtils.renderMethod(_ehaSeq, _ehaMv, TList.of());
            }
          }
        }
      }
      _soeCw.visitEnd();
      compiledBinaryName = Opt.some(_soeBinaryName);
      compiledBytecode = Opt.some(_soeCw.toByteArray());
      compiledStaticFieldValues = Opt.some(_soeStaticFieldValues);
    }
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
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    if (setId.optIsNone() || set == null) {
      return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }
    return JavaDataDescriptor.fromInternalName(
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objectIndex)));
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
      outer:
      for (int implIdx = 0; implIdx < interfaceImpls.size(); implIdx++) {
        var impl = interfaceImpls.get(implIdx);
        for (int mIdx = 0; mIdx < impl.interface_.methods.size(); mIdx++) {
          var method = impl.interface_.methods.get(mIdx);
          if (method.name.equals(keyName)) {
            argType = method.argType;
            retType = method.retType;
            fieldName =
                SourceObjectEmitter.interfaceMethodMethodName(
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
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      throw new AssertionError(
          "TypeSourceObjectDynamic::field: this not in its owning set");
    }
    var objectInternalName =
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objectIndex));
    var fieldType =
        new TypeMethodDynamic(
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
              JavaBytecodeUtils.callStaticInterfaceMethod(
                  new CodeLoc.Some(loc),
                  JavaInternalName.fromClass(Code.class),
                  "target",
                  JavaMethodDescriptor.fromParts(
                      JavaDataDescriptor.fromClass(Code.class),
                      TList.of(
                          JavaDataDescriptor.fromClass(TargetCode.class)))));
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
    var objectIndex = set.objects.indexOf(this);
    if (objectIndex < 0) {
      throw new AssertionError("TypeSourceObjectDynamic::typeCastFrom: this not in its owning set");
    }
    var objectInternalName =
        JavaInternalName.of(
            SourceObjectEmitter.internalNameFor(setId.optUnwrap(), objectIndex));
    var unwrapSeq = new JavaBytecodeSequence();
    unwrapSeq.add(codeSource);
    unwrapSeq.add(
        JavaBytecodeUtils.callStaticInterfaceMethod(
            new CodeLoc.Some(loc),
            JavaInternalName.fromClass(Code.class),
            "targetImpl",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(TargetCode.class),
                TList.of(
                    JavaDataDescriptor.fromClass(Code.class)))));
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
