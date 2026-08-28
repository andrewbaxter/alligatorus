package compilerxyz.eval;

import static compilerxyz.util.MustCast.mustCast;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.definition_set.ArtifactDataDefSet;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.dynamic.EmitCtx;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.dynamic.SourceUnionEmitter;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenType;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactDataDefSetCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import java.util.HashSet;
import java.util.function.Function;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class DefinitionSet implements ExporterAny {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();
    public final DefSetId defSetId;
    public final TOrderedMap<DefSetId, DefinitionSet> dependencies;
    public boolean finished;
    public final TList<SourceFunctionData> functions;
    public final TList<InterfaceData> interfaces;
    public final TList<TypeSourceObjectDynamic> objects;
    public final TList<TypeSourceUnionDynamic> unions;

    public DefinitionSet(DefSetId defSetId) {
        this.defSetId = defSetId;
        this.dependencies = new TOrderedMap<>();
        this.finished = false;
        this.functions = new TList<>();
        this.interfaces = new TList<>();
        this.objects = new TList<>();
        this.unions = new TList<>();
    }

    public EResult<Void> finish(ExprContext xx, QExprId loc) {
        if (finished) {
            return EResult.ok(null);
        }
        for (var func : functions) {
            var r = func.finish(xx, loc);
            if (r.hasErrors()) return r.reerr();
        }
        for (long objIdx = 0; objIdx < objects.size(); objIdx++) {
            var obj = objects.get((int) objIdx);
            if (obj.underlyingType.optIsNone()) {
                return EResult.err(
                        loc,
                        "Object '" + obj.name.optUnwrapOr("(unnamed)")
                                + "' underlying type not finalised before finish");
            }
            var underlyingType = obj.underlyingType.optUnwrap();
            var objInternalName = SourceObjectEmitter.internalNameFor(obj.setId, objIdx);
            for (var method : obj.methods) {
                var nameStr = obj.name.optUnwrapOr("(unnamed)");
                var r = method.finish(xx, loc, nameStr, obj, underlyingType, objInternalName);
                if (r.hasErrors()) return r.reerr();
            }
            for (var impl : obj.interfaceImpls) {
                for (var m : impl.methodImpls) {
                    var r = m.finish(xx, loc, impl.interface_, obj, underlyingType);
                    if (r.hasErrors()) return r.reerr();
                }
            }
            EResult<Void> r;
            if (obj.compiledBinaryName.optIsSome()) {
                r = EResult.ok(null);
            } else {
                var _soeBinaryName = SourceObjectEmitter.binaryNameFor(obj.setId, objIdx);
                var _soeInternalNameStr = _soeBinaryName.replace('.', '/');
                var _soeInternalName = JavaInternalName.of(_soeInternalNameStr);
                var _soeClaimedKinds = new HashSet<BuiltinInterfaceKind>();
                for (var _soeImpl : obj.interfaceImpls) {
                    if (_soeImpl.interface_.builtinKind.optIsSome()) {
                        _soeClaimedKinds.add(_soeImpl.interface_.builtinKind.optUnwrap());
                    }
                }
                var _soeImplementsInternalNames = new TOrderedSet<String>();
                _soeImplementsInternalNames.add(SourceObjectEmitter.INTNAME_SOURCE_OBJECT_INSTANCE.value());
                for (var _soeImpl : obj.interfaceImpls) {
                    var _soeIface = _soeImpl.interface_;
                    _soeImplementsInternalNames.add(_soeIface.compiledBinaryName.replace('.', '/'));
                }
                var _soeInterfacesArr = _soeImplementsInternalNames.isEmpty()
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
                var _soeUnderlyingTypeMaybe = obj.underlyingType.optUnwrapOr(null);
                if (_soeUnderlyingTypeMaybe instanceof AspectConsumeDynamic _soeUnderlyingConsume) {
                    _soeDataFieldDesc = _soeUnderlyingConsume.aspectConsumeDynamicJvmClass(xx);
                } else {
                    _soeDataFieldDesc = JvmConstants.DESC_OBJECT;
                }
                _soeCw.visitField(
                                Opcodes.ACC_PUBLIC,
                                SourceObjectEmitter.DATA_INSTANCE_FIELD,
                                _soeDataFieldDesc.value(),
                                null,
                                null)
                        .visitEnd();
                var _soeCtor = _soeCw.visitMethod(
                        Opcodes.ACC_PUBLIC, "<init>", JavaMethodDescriptor.VOID_NOARGS.value(), null, null);
                _soeCtor.visitCode();
                var _soeCtorSeq = new JavaBytecodeSequence();
                _soeCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
                _soeCtorSeq.add(JavaBytecodeUtils.invokeSpecial(
                        CodeLoc.Infallible.INSTANCE,
                        JvmConstants.INTNAME_OBJECT,
                        "<init>",
                        JavaMethodDescriptor.VOID_NOARGS));
                _soeCtorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
                JavaBytecodeUtils.renderMethod(_soeCtorSeq, _soeCtor, TList.of());
                var _soeRealCtorDesc =
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(_soeDataFieldDesc));
                var _soeRealCtor =
                        _soeCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", _soeRealCtorDesc.value(), null, null);
                _soeRealCtor.visitCode();
                var _soeRealCtorSeq = new JavaBytecodeSequence();
                _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
                _soeRealCtorSeq.add(JavaBytecodeUtils.invokeSpecial(
                        CodeLoc.Infallible.INSTANCE,
                        JvmConstants.INTNAME_OBJECT,
                        "<init>",
                        JavaMethodDescriptor.VOID_NOARGS));
                _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
                {
                    var _soeC = _soeDataFieldDesc.value().charAt(0);
                    int _soeLoadOp =
                            switch (_soeC) {
                                case 'J' -> Opcodes.LLOAD;
                                case 'D' -> Opcodes.DLOAD;
                                case 'F' -> Opcodes.FLOAD;
                                case 'I', 'B', 'C', 'S', 'Z' -> Opcodes.ILOAD;
                                default -> Opcodes.ALOAD;
                            };
                    _soeRealCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, _soeLoadOp, 1));
                }
                _soeRealCtorSeq.add(JavaBytecodeUtils.setField(
                        CodeLoc.Infallible.INSTANCE,
                        _soeInternalName,
                        SourceObjectEmitter.DATA_INSTANCE_FIELD,
                        _soeDataFieldDesc));
                _soeRealCtorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
                JavaBytecodeUtils.renderMethod(_soeRealCtorSeq, _soeRealCtor, TList.of());
                var _soeAllFieldNames = new TOrderedSet<String>();
                var _soeStaticFieldValues = new TOrderedMap<String, Object>();
                var _soeFieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
                for (int _soeImplIdx = 0; _soeImplIdx < obj.interfaceImpls.size(); _soeImplIdx++) {
                    var _soeImpl = obj.interfaceImpls.get(_soeImplIdx);
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
                                _soeFieldDescriptors.put(
                                        _cmfName,
                                        JavaDataDescriptor.fromClass(
                                                _cmfEntry.external().externalHostClass()));
                                _soeStaticFieldValues.put(
                                        _cmfName, _cmfEntry.external().externalHostValue());
                            }
                        }
                    }
                }
                for (long _soeMethodIdx = 0; _soeMethodIdx < obj.methods.size(); _soeMethodIdx++) {
                    var _soeMethod = obj.methods.get((int) _soeMethodIdx);
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
                                    _soeName,
                                    JavaDataDescriptor.fromClass(
                                            _soeEntry.external().externalHostClass()));
                            _soeStaticFieldValues.put(
                                    _soeName, _soeEntry.external().externalHostValue());
                        }
                    }
                }
                for (var _soeName : _soeAllFieldNames) {
                    var _soeDesc = _soeFieldDescriptors.getOrDefault(_soeName, JvmConstants.DESC_OBJECT);
                    _soeCw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, _soeName, _soeDesc.value(), null, null)
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
                _soeStaticFieldValues.put(SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD, obj);
                _soeCw.visitField(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                EvalRootEmitter.EXPR_CONTEXT_FIELD,
                                EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
                                null,
                                null)
                        .visitEnd();
                for (long _soeMethodIdx = 0; _soeMethodIdx < obj.methods.size(); _soeMethodIdx++) {
                    var _soeMethod = obj.methods.get((int) _soeMethodIdx);
                    if (_soeMethod.compiledBodyCode.optIsNone()) {
                        continue;
                    }
                    {
                        var _emmBodyCode = _soeMethod.compiledBodyCode.optUnwrap();
                        var _emmUnderlyingType = obj.underlyingType.optUnwrapOr(null);
                        var _emmMethodName = SourceObjectEmitter.methodMethodName(_soeMethodIdx);
                        var _emmPrefix = SourceObjectEmitter.methodFieldPrefix(_soeMethodIdx);
                        JavaDataDescriptor _emmRetDesc;
                        if (_soeMethod.retType.optIsSome()) {
                            if (!(_soeMethod.retType.optUnwrap() instanceof AspectConsumeDynamic _emmRetConsume)) {
                                throw new IllegalStateException("SourceObjectEmitter.emitMethodMethod: method '"
                                        + _soeMethod.name + "' retType does not implement AspectConsumeDynamic");
                            }
                            _emmRetDesc = _emmRetConsume.aspectConsumeDynamicJvmClass(xx);
                        } else {
                            _emmRetDesc = JavaDataDescriptor.DESC_VOID;
                        }
                        if (_soeMethod.compiledArgSlots.optIsNone() || _soeMethod.compiledArgSlotIds.optIsNone()) {
                            throw new IllegalStateException("SourceObjectEmitter.emitMethodMethod: method '"
                                    + _soeMethod.name + "' has no compiledArgSlots (finish ordering violation)");
                        }
                        var _emmArgSlots = _soeMethod.compiledArgSlots.optUnwrap();
                        var _emmArgSlotIds = _soeMethod.compiledArgSlotIds.optUnwrap();
                        var _emmArgDescs = new TList<JavaDataDescriptor>(_emmArgSlots.size());
                        for (var _emmS : _emmArgSlots) _emmArgDescs.add(_emmS.jvmDesc());
                        var _emmDescriptor = JavaMethodDescriptor.fromParts(_emmRetDesc, _emmArgDescs);
                        var _emmMv = _soeCw.visitMethod(
                                Opcodes.ACC_PUBLIC, _emmMethodName, _emmDescriptor.value(), null, null);
                        var _emmTarget = new TargetDynamic(new Counter());
                        int _emmTotalArgSlots = 0;
                        for (var _emmS : _emmArgSlots)
                            _emmTotalArgSlots += SourceObjectEmitter.slotWidth(_emmS.jvmDesc());
                        var _emmCtx = new EmitCtx(_soeInternalName.value(), 1, 1 + _emmTotalArgSlots, _emmPrefix);
                        if (_soeMethod.innerBindingId.optIsNone()) {
                            throw new IllegalStateException("SourceObjectEmitter.emit: method '" + _soeMethod.name
                                    + "' has no innerBindingId (finish ordering violation)");
                        }
                        var _emmSelfId = _soeMethod.innerBindingId.optUnwrap();
                        var _emmSeq = new JavaBytecodeSequence();
                        var _emmLoc = CodeLoc.Infallible.INSTANCE;
                        var _emmSelfLocal = mustCast(BindingId.Local.class, _emmSelfId);
                        JavaDataDescriptor _emmDataFieldDesc =
                                switch (GenAspectConsumeDynamic.getMaybe(xx, _emmUnderlyingType)) {
                                    case Opt.Some<AspectConsumeDynamic> s ->
                                        s.value().aspectConsumeDynamicJvmClass(xx);
                                    case Opt.None<AspectConsumeDynamic> n ->
                                        throw new IllegalStateException(
                                                "SourceObjectEmitter.emit: underlyingType "
                                                        + _emmUnderlyingType.typeDesc()
                                                        + " does not implement AspectConsumeDynamic; cannot emit data field access");
                                };
                        _emmSeq.add(JavaBytecodeUtils.varInsn(_emmLoc, Opcodes.ALOAD, 0));
                        _emmSeq.add(JavaBytecodeUtils.accessField(
                                _emmLoc, _soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, _emmDataFieldDesc));
                        _emmSeq.add(new CodeDynamic(
                                _emmLoc,
                                new JavaBytecodeStoreLoad(
                                        SourceObjectEmitter.storeOpcodeFor(_emmDataFieldDesc), _emmSelfLocal.key())));
                        var _emmInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
                        _emmInitialSlots.add(new JavaBytecodeSequence.InitialSlot(new JavaBytecodeBindingKey()));
                        for (int _emmI = 0; _emmI < _emmArgSlots.size(); _emmI++) {
                            _emmInitialSlots.add(new JavaBytecodeSequence.InitialSlot(
                                    _emmArgSlotIds.get(_emmI).key(),
                                    SourceObjectEmitter.slotWidth(
                                                    _emmArgSlots.get(_emmI).jvmDesc())
                                            == 2));
                        }
                        for (int _emmI = 0; _emmI < _soeMethod.capturedExternals.size(); _emmI++) {
                            var _emmEntry = _soeMethod.capturedExternals.get(_emmI);
                            var _emmFieldName = _emmPrefix + Prefixes.EXTERNAL_PREFIX + _emmI;
                            var _emmFieldDesc = JavaDataDescriptor.fromClass(
                                    _emmEntry.external().externalHostClass());
                            _emmSeq.add(JavaBytecodeUtils.accessStaticField(
                                    _emmLoc, _soeInternalName, _emmFieldName, _emmFieldDesc));
                            _emmSeq.add(new CodeDynamic(
                                    _emmLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, _emmEntry.key())));
                        }
                        _emmTarget.emit(_emmSeq, _emmCtx, _emmBodyCode);
                        _emmSeq.add(JavaBytecodeUtils.inst(
                                _emmLoc,
                                SourceObjectEmitter.returnOpcodeFor(
                                        _emmRetDesc.value().charAt(0))));
                        JavaBytecodeUtils.renderMethodTyped(_emmSeq, _emmMv, _emmInitialSlots);
                    }
                }
                for (int _soeImplIdx = 0; _soeImplIdx < obj.interfaceImpls.size(); _soeImplIdx++) {
                    var _soeImpl = obj.interfaceImpls.get(_soeImplIdx);
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
                            var _eimSelfId = mustCast(BindingId.Local.class, _soeM.innerBindingId.optUnwrap());
                            if (_soeM.compiledArgParamIds.optIsNone()) {
                                throw new IllegalStateException(
                                        "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
                            }
                            var _eimArgParamIds = _soeM.compiledArgParamIds.optUnwrap();
                            var _eimIfaceMethod = _soeImpl.interface_.methods.get(_soeM.interfaceMethodIndex);
                            var _eimDescriptor =
                                    SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, _soeM, _eimIfaceMethod);
                            var _eimJvmParamKeys = new TList<JavaBytecodeBindingKey>();
                            var _eimJvmParamWides = new TList<Boolean>();
                            if (_soeM.compiledArgParamTypes.optIsNone()
                                    || _soeM.compiledArgParamTypes.optUnwrap().size() != _eimArgParamIds.size()) {
                                throw new IllegalStateException(
                                        "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
                            }
                            var _eimArgParamTypes = _soeM.compiledArgParamTypes.optUnwrap();
                            for (int _eimI = 0; _eimI < _eimArgParamIds.size(); _eimI++) {
                                var _eimArgType = _eimArgParamTypes.get(_eimI);
                                var _eimArgConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, _eimArgType);
                                if (_eimArgConsumeOpt.optIsNone()) {
                                    throw new IllegalStateException(
                                            "SourceObjectEmitter.emitInterfaceMethodMethod: arg type "
                                                    + _eimArgType.typeDesc()
                                                    + " does not implement AspectConsumeDynamic");
                                }
                                var _eimArgDesc = _eimArgConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
                                _eimJvmParamKeys.add(_eimArgParamIds.get(_eimI).key());
                                _eimJvmParamWides.add(SourceObjectEmitter.slotWidth(_eimArgDesc) == 2);
                            }
                            var _eimMv = _soeCw.visitMethod(
                                    Opcodes.ACC_PUBLIC, _eimMethodName, _eimDescriptor.value(), null, null);
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
                                        _eimSeq.add(new CodeDynamic(
                                                _eimLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, outerLocal.key())));
                                    }
                                }
                            }
                            JavaDataDescriptor _eimInnerDesc = obj.underlyingType.optUnwrapOr(null)
                                            instanceof AspectConsumeDynamic underlyingConsume
                                    ? underlyingConsume.aspectConsumeDynamicJvmClass(xx)
                                    : JvmConstants.DESC_OBJECT;
                            _eimSeq.add(JavaBytecodeUtils.varInsn(_eimLoc, Opcodes.ALOAD, 0));
                            _eimSeq.add(JavaBytecodeUtils.accessField(
                                    _eimLoc, _soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, _eimInnerDesc));
                            _eimSeq.add(new CodeDynamic(
                                    _eimLoc,
                                    new JavaBytecodeStoreLoad(
                                            SourceObjectEmitter.storeOpcodeFor(_eimInnerDesc), _eimSelfId.key())));
                            var _eimInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
                            _eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(new JavaBytecodeBindingKey()));
                            for (int _eimI = 0; _eimI < _eimJvmParamKeys.size(); _eimI++) {
                                _eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(
                                        _eimJvmParamKeys.get(_eimI), _eimJvmParamWides.get(_eimI)));
                            }
                            for (int _eimI = 0; _eimI < _soeM.capturedExternals.size(); _eimI++) {
                                var _eimEntry = _soeM.capturedExternals.get(_eimI);
                                var _eimFieldName = _eimPrefix + Prefixes.EXTERNAL_PREFIX + _eimI;
                                var _eimFieldDesc = JavaDataDescriptor.fromClass(
                                        _eimEntry.external().externalHostClass());
                                _eimSeq.add(JavaBytecodeUtils.accessStaticField(
                                        _eimLoc, _soeInternalName, _eimFieldName, _eimFieldDesc));
                                _eimSeq.add(new CodeDynamic(
                                        _eimLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, _eimEntry.key())));
                            }
                            _eimTarget.emit(_eimSeq, _eimCtx, _soeM.compiledBodyCode.optUnwrap());
                            {
                                var _ercDescStr = _eimDescriptor.value();
                                var _ercRetDesc = _ercDescStr.substring(_ercDescStr.indexOf(')') + 1);
                                var _ercRetChar = _ercRetDesc.charAt(0);
                                if (_ercRetChar == 'L') {
                                    var _ercRetInternal =
                                            JavaInternalName.of(_ercRetDesc.substring(1, _ercRetDesc.length() - 1));
                                    _eimSeq.add(JavaBytecodeUtils.cast(
                                            _eimLoc, JavaDataDescriptor.fromInternalName(_ercRetInternal)));
                                    _eimSeq.add(JavaBytecodeUtils.inst(_eimLoc, Opcodes.ARETURN));
                                } else {
                                    _eimSeq.add(JavaBytecodeUtils.inst(
                                            _eimLoc, SourceObjectEmitter.returnOpcodeFor(_ercRetChar)));
                                }
                            }
                            JavaBytecodeUtils.renderMethodTyped(_eimSeq, _eimMv, _eimInitialSlots);
                        }
                    }
                }
                {
                    var _ehiEmitted = new TOrderedSet<String>();
                    for (int _ehiImplIdx = 0; _ehiImplIdx < obj.interfaceImpls.size(); _ehiImplIdx++) {
                        var _ehiImpl = obj.interfaceImpls.get(_ehiImplIdx);
                        var _ehiIface = _ehiImpl.interface_;
                        for (int _ehiMIdx = 0; _ehiMIdx < _ehiImpl.methodImpls.size(); _ehiMIdx++) {
                            var _ehiM = _ehiImpl.methodImpls.get(_ehiMIdx);
                            if (_ehiM.compiledBodyCode.optIsNone()) {
                                continue;
                            }
                            var _ehiIfaceMethod = _ehiIface.methods.get(_ehiM.interfaceMethodIndex);
                            String _ehiHostName;
                            JavaMethodDescriptor _ehiHostDescriptor;
                            if (_ehiIfaceMethod.hostMethodName.optIsSome()
                                    && _ehiIfaceMethod.hostMethodDescriptor.optIsSome()) {
                                _ehiHostName = _ehiIfaceMethod.hostMethodName.optUnwrap();
                                _ehiHostDescriptor = _ehiIfaceMethod.hostMethodDescriptor.optUnwrap();
                            } else {
                                _ehiHostName = _ehiIfaceMethod.name;
                                _ehiHostDescriptor = JavaMethodDescriptor.fromParts(
                                        JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
                            }
                            var _ehiDedupeKey = _ehiHostName + _ehiHostDescriptor.value();
                            if (!_ehiEmitted.add(_ehiDedupeKey)) {
                                continue;
                            }
                            {
                                var _ehaMv = _soeCw.visitMethod(
                                        Opcodes.ACC_PUBLIC, _ehiHostName, _ehiHostDescriptor.value(), null, null);
                                var _ehaHostDescStr = _ehiHostDescriptor.value();
                                var _ehaLoc = CodeLoc.Infallible.INSTANCE;
                                var _ehaSeq = new JavaBytecodeSequence();
                                _ehaSeq.add(JavaBytecodeUtils.varInsn(_ehaLoc, Opcodes.ALOAD, 0));
                                var _ehaCalleeDescriptor =
                                        SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, _ehiM, _ehiIfaceMethod);
                                int _ehaHostSlot = 1;
                                if (_ehiM.compiledArgParamTypes.optIsSome()) {
                                    var _ehaArgParamTypes = _ehiM.compiledArgParamTypes.optUnwrap();
                                    var _ehaHostParams = new TList<String>();
                                    {
                                        int _pdI = 1;
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
                                                    "SourceObjectEmitter.emitHostAdapter: arg type "
                                                            + _ehaArgType.typeDesc()
                                                            + " does not implement AspectConsumeDynamic");
                                        }
                                        var _ehaArgDesc =
                                                _ehaArgConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
                                        var _ehaHostParam = _ehaHostParams.get(_ehaArgIdx);
                                        if (!_ehaHostParam.equals(_ehaArgDesc.value())
                                                && (_ehaArgDesc.value().startsWith("L")
                                                        || _ehaArgDesc.value().startsWith("["))
                                                && !_ehaArgDesc.value().equals("Ljava/lang/Object;")) {
                                            _ehaSeq.add(JavaBytecodeUtils.cast(_ehaLoc, _ehaArgDesc));
                                        }
                                        _ehaHostSlot += SourceObjectEmitter.slotWidth(_ehaArgDesc);
                                        _ehaArgIdx++;
                                    }
                                }
                                _ehaSeq.add(JavaBytecodeUtils.callMethod(
                                        _ehaLoc,
                                        _soeInternalName,
                                        SourceObjectEmitter.interfaceMethodMethodName(_ehiImplIdx, _ehiMIdx),
                                        _ehaCalleeDescriptor));
                                var _ehaRetChar = _ehaHostDescStr.charAt(_ehaHostDescStr.indexOf(')') + 1);
                                _ehaSeq.add(JavaBytecodeUtils.inst(
                                        _ehaLoc, SourceObjectEmitter.returnOpcodeFor(_ehaRetChar)));
                                JavaBytecodeUtils.renderMethod(_ehaSeq, _ehaMv, TList.of());
                            }
                        }
                    }
                }
                _soeCw.visitEnd();
                obj.compiledBinaryName = Opt.some(_soeBinaryName);
                obj.compiledBytecode = Opt.some(_soeCw.toByteArray());
                obj.compiledStaticFieldValues = Opt.some(_soeStaticFieldValues);
                r = EResult.ok(null);
            }
            if (r.hasErrors()) return r.reerr();
        }
        for (var _seu : unions) {
            EResult<Void> r;
            if (_seu.compiled.baseBinaryName.optIsSome()) {
                r = EResult.ok(null);
            } else {
                var _seuBaseBinaryName = SourceUnionEmitter.baseBinaryNameFor(defSetId, _seu.unionIdx);
                var _seuBaseInternalName = _seuBaseBinaryName.replace('.', '/');
                var _seuVariantBinaryNames = new TList<String>(_seu.variants.size());
                var _seuVariantInternalNames = new TList<String>(_seu.variants.size());
                for (int _seuI = 0; _seuI < _seu.variants.size(); _seuI++) {
                    var _seuVbn = SourceUnionEmitter.variantBinaryNameFor(defSetId, _seu.unionIdx, _seuI);
                    _seuVariantBinaryNames.add(_seuVbn);
                    _seuVariantInternalNames.add(_seuVbn.replace('.', '/'));
                }
                {
                    var _ebCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    _ebCw.visit(
                            Opcodes.V17,
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SUPER,
                            _seuBaseInternalName,
                            null,
                            JvmConstants.INTNAME_OBJECT.value(),
                            null);
                    for (var _ebVin : _seuVariantInternalNames) {
                        _ebCw.visitPermittedSubclass(_ebVin);
                    }
                    var _ebCtor = _ebCw.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
                    _ebCtor.visitCode();
                    _ebCtor.visitVarInsn(Opcodes.ALOAD, 0);
                    _ebCtor.visitMethodInsn(
                            Opcodes.INVOKESPECIAL, JvmConstants.INTNAME_OBJECT.value(), "<init>", "()V", false);
                    _ebCtor.visitInsn(Opcodes.RETURN);
                    _ebCtor.visitMaxs(0, 0);
                    _ebCtor.visitEnd();
                    _ebCw.visitEnd();
                    _seu.compiled.baseBytecode = Opt.some(_ebCw.toByteArray());
                }
                var _seuVariantBytecode = new TList<byte[]>(_seu.variants.size());
                for (int _seuI = 0; _seuI < _seu.variants.size(); _seuI++) {
                    var _seuVariant = _seu.variants.get(_seuI);
                    var _seuPayloadDesc = SourceUnionEmitter.payloadDescriptor(xx, _seuVariant.type);
                    {
                        var _evVariantInternalName = _seuVariantInternalNames.get(_seuI);
                        var _evCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        _evCw.visit(
                                Opcodes.V17,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                                _evVariantInternalName,
                                null,
                                _seuBaseInternalName,
                                null);
                        _evCw.visitField(
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                                        SourceUnionEmitter.PAYLOAD_FIELD,
                                        _seuPayloadDesc.value(),
                                        null,
                                        null)
                                .visitEnd();
                        var _evCtorDesc = "(" + _seuPayloadDesc.value() + ")V";
                        var _evCtor = _evCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", _evCtorDesc, null, null);
                        _evCtor.visitCode();
                        _evCtor.visitVarInsn(Opcodes.ALOAD, 0);
                        _evCtor.visitMethodInsn(Opcodes.INVOKESPECIAL, _seuBaseInternalName, "<init>", "()V", false);
                        _evCtor.visitVarInsn(Opcodes.ALOAD, 0);
                        int _evLoadOp =
                                switch (_seuPayloadDesc.value().charAt(0)) {
                                    case 'Z', 'B', 'C', 'S', 'I' -> Opcodes.ILOAD;
                                    case 'J' -> Opcodes.LLOAD;
                                    case 'F' -> Opcodes.FLOAD;
                                    case 'D' -> Opcodes.DLOAD;
                                    default -> Opcodes.ALOAD;
                                };
                        _evCtor.visitVarInsn(_evLoadOp, 1);
                        _evCtor.visitFieldInsn(
                                Opcodes.PUTFIELD,
                                _evVariantInternalName,
                                SourceUnionEmitter.PAYLOAD_FIELD,
                                _seuPayloadDesc.value());
                        _evCtor.visitInsn(Opcodes.RETURN);
                        _evCtor.visitMaxs(0, 0);
                        _evCtor.visitEnd();
                        _evCw.visitEnd();
                        _seuVariantBytecode.add(_evCw.toByteArray());
                    }
                }
                _seu.compiled.baseBinaryName = Opt.some(_seuBaseBinaryName);
                _seu.compiled.variantBinaryNames = Opt.some(_seuVariantBinaryNames);
                _seu.compiled.variantBytecode = Opt.some(_seuVariantBytecode);
                r = EResult.ok(null);
            }
            if (r.hasErrors()) return r.reerr();
        }
        finishFunctions();
        for (long i = 0; i < interfaces.size(); i++) {
            var iface = interfaces.get((int) i);
            EResult<Void> r;
            var _ifaceName = iface.description.optUnwrapOr("(unnamed)");
            if (iface.builtinKind.optIsSome() || iface.compiledBytecode.optIsSome()) {
                r = EResult.ok(null);
            } else if (iface.setId.optIsNone()) {
                r = EResult.err(
                        loc, "InterfaceData.finish: setId is null for user-declared interface '" + _ifaceName + "'");
            } else {
                var _sieBinaryName = SourceInterfaceEmitter.binaryNameFor(iface.setId.optUnwrap(), i);
                var _sieInternalName = _sieBinaryName.replace('.', '/');
                var _sieCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                _sieCw.visit(
                        Opcodes.V11,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                        _sieInternalName,
                        null,
                        JvmConstants.INTNAME_OBJECT.value(),
                        null);
                for (var _sieMethod : iface.methods) {
                    if (_sieMethod.argType == null) {
                        throw new IllegalStateException("SourceInterfaceEmitter.emit: method '" + _sieMethod.name
                                + "' has no arg type (finish ordering violation)");
                    }
                    _sieCw.visitMethod(
                                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                                    _sieMethod.name,
                                    JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT)
                                            .value(),
                                    null,
                                    null)
                            .visitEnd();
                }
                _sieCw.visitEnd();
                iface.compiledBinaryName = _sieBinaryName;
                iface.compiledBytecode = Opt.some(_sieCw.toByteArray());
                r = EResult.ok(null);
            }
            if (r.hasErrors()) return r.reerr();
        }
        finished = true;
        for (var dep : dependencies.values()) {
            var r = dep.finish(xx, loc);
            if (r.hasErrors()) return r.reerr();
        }
        return EResult.ok(null);
    }

    public Opt<DefSetFunctionsEmitter.EmittedFunctions> finishFunctions() {
        var specs = new TList<DefSetFunctionsEmitter.FunctionSpec>();
        var contributors = new TList<SourceFunctionData>();
        for (var func : functions) {
            var spec = func.functionSpec;
            if (spec.optIsNone()) {
                continue;
            }
            if (func.compiledClassName.optIsSome()) {
                continue;
            }
            specs.add(spec.optUnwrap());
            contributors.add(func);
        }
        if (specs.isEmpty()) {
            return Opt.none();
        }
        var _dsfBinaryName = DefSetFunctionsEmitter.functionsBinaryName(defSetId);
        var _dsfInternalName = DefSetFunctionsEmitter.functionsInternalName(defSetId);
        var _dsfCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return JvmConstants.INTNAME_OBJECT.value();
            }
        };
        _dsfCw.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                _dsfInternalName,
                null,
                JvmConstants.INTNAME_OBJECT.value(),
                null);
        var _dsfFieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
        var _dsfStaticFieldValues = new TOrderedMap<String, Object>();
        for (var _dsfSpec : specs) {
            var _dsfPrefix = DefSetFunctionsEmitter.functionFieldPrefix(_dsfSpec.funcIdx());
            for (int _dsfI = 0; _dsfI < _dsfSpec.externals().size(); _dsfI++) {
                var _dsfEntry = _dsfSpec.externals().get(_dsfI);
                var _dsfName = _dsfPrefix + Prefixes.EXTERNAL_PREFIX + _dsfI;
                var _dsfExternal = _dsfEntry.external();
                var _dsfDesc = JavaDataDescriptor.fromClass(_dsfExternal.externalHostClass());
                var _dsfPrev = _dsfFieldDescriptors.put(_dsfName, _dsfDesc);
                if (_dsfPrev != null && !_dsfPrev.value().equals(_dsfDesc.value())) {
                    throw new AssertionError("DefSetFunctionsEmitter::emit: descriptor conflict for " + _dsfName);
                }
                _dsfStaticFieldValues.put(_dsfName, _dsfExternal.externalHostValue());
            }
        }
        for (var _dsfFdEntry : _dsfFieldDescriptors.entrySet()) {
            _dsfCw.visitField(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                            _dsfFdEntry.getKey(),
                            _dsfFdEntry.getValue().value(),
                            null,
                            null)
                    .visitEnd();
        }
        _dsfCw.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        EvalRootEmitter.EXPR_CONTEXT_FIELD,
                        EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
                        null,
                        null)
                .visitEnd();
        for (var _dsfSpec : specs) {
            var _efmMethodName = DefSetFunctionsEmitter.functionMethodName(_dsfSpec.funcIdx());
            var _efmPrefix = DefSetFunctionsEmitter.functionFieldPrefix(_dsfSpec.funcIdx());
            var _efmArgDescs = new TList<JavaDataDescriptor>(_dsfSpec.argSlots().size());
            for (var _efmS : _dsfSpec.argSlots()) _efmArgDescs.add(_efmS.jvmDesc());
            var _efmMv = _dsfCw.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    _efmMethodName,
                    JavaMethodDescriptor.fromParts(_dsfSpec.retDesc(), _efmArgDescs)
                            .value(),
                    null,
                    null);
            var _efmTarget = new TargetDynamic(new Counter());
            int _efmReservedLocals = 0;
            for (var _efmSlot : _dsfSpec.argSlots()) {
                var _efmSlotDesc = _efmSlot.jvmDesc().value();
                _efmReservedLocals += (_efmSlotDesc.equals("D") || _efmSlotDesc.equals("J")) ? 2 : 1;
            }
            var _efmCtx = new EmitCtx(_dsfInternalName, Math.max(1, _efmReservedLocals), _efmPrefix);
            var _efmLoc = CodeLoc.Infallible.INSTANCE;
            var _efmSeq = JavaBytecodeUtils.seq();
            var _efmOwnerInternal = JavaInternalName.of(_dsfInternalName);
            for (int _efmI = 0; _efmI < _dsfSpec.externals().size(); _efmI++) {
                var _efmEntry = _dsfSpec.externals().get(_efmI);
                var _efmFieldName = _efmPrefix + Prefixes.EXTERNAL_PREFIX + _efmI;
                var _efmFieldDesc =
                        JavaDataDescriptor.fromClass(_efmEntry.external().externalHostClass());
                _efmSeq.add(
                        JavaBytecodeUtils.accessStaticField(_efmLoc, _efmOwnerInternal, _efmFieldName, _efmFieldDesc));
                _efmSeq.add(new CodeDynamic(_efmLoc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, _efmEntry.key())));
            }
            _efmTarget.emit(_efmSeq, _efmCtx, _dsfSpec.bodyCode());
            int _efmRetOpcode =
                    switch (_dsfSpec.retDesc().value()) {
                        case "V" -> Opcodes.RETURN;
                        case "I", "Z", "B", "S", "C" -> Opcodes.IRETURN;
                        case "J" -> Opcodes.LRETURN;
                        case "F" -> Opcodes.FRETURN;
                        case "D" -> Opcodes.DRETURN;
                        default -> Opcodes.ARETURN;
                    };
            _efmSeq.add(JavaBytecodeUtils.inst(_efmLoc, _efmRetOpcode));
            var _efmInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
            for (var _efmSlot : _dsfSpec.argSlots()) {
                var _efmSlotDesc = _efmSlot.jvmDesc().value();
                boolean _efmIsWide = _efmSlotDesc.equals("D") || _efmSlotDesc.equals("J");
                _efmInitialSlots.add(
                        new JavaBytecodeSequence.InitialSlot(_efmSlot.id().key(), _efmIsWide));
            }
            JavaBytecodeUtils.renderMethodTyped(_efmSeq, _efmMv, _efmInitialSlots);
        }
        _dsfCw.visitEnd();
        var _dsfEmitted = new DefSetFunctionsEmitter.EmittedFunctions(
                _dsfBinaryName, _dsfInternalName, _dsfCw.toByteArray(), _dsfStaticFieldValues);
        for (var func : contributors) {
            func.compiledClassName = Opt.some(_dsfEmitted.binaryName());
            func.compiledBytecode = Opt.some(_dsfEmitted.bytecode());
            func.compiledStaticFieldValues = Opt.some(_dsfEmitted.staticFieldValues());
            func.functionSpec = Opt.none();
        }
        return Opt.some(_dsfEmitted);
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        throw new UnsupportedOperationException("DefinitionSet is neither a Type nor a Value");
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        throw new UnsupportedOperationException("DefinitionSet is neither a Type nor a Value");
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            var set = mustCast(DefinitionSet.class, self);
            var errors = new TList<ExprErr>();
            var artifact =
                    ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, set.artifactIdCell, () -> {
                        var objects = new TList<Artifact>(set.objects.size());
                        for (var dt : set.objects) {
                            var dtConst = new TypeTypeSourceObjectConst(dt);
                            var artRes = dtConst.artifactableExportArtifact(xx, loc);
                            artRes.drainInto(errors);
                            if (artRes.valueOrNull() != null) {
                                objects.add(artRes.requireValue("DefinitionSet::exporterExport object"));
                            }
                        }
                        var functions = new TList<Artifact>(set.functions.size());
                        for (var f : set.functions) {
                            var fConst = new TypeSourceFunctionConst(f);
                            var artRes = fConst.artifactableExportArtifact(xx, loc);
                            artRes.drainInto(errors);
                            if (artRes.valueOrNull() != null) {
                                functions.add(artRes.requireValue("DefinitionSet::exporterExport function"));
                            }
                        }
                        var interfaces = new TList<Artifact>(set.interfaces.size());
                        for (var i : set.interfaces) {
                            var iConst = new TypeSourceInterfaceDynamic(i);
                            var artRes = iConst.artifactableExportArtifact(xx, loc);
                            artRes.drainInto(errors);
                            if (artRes.valueOrNull() != null) {
                                interfaces.add(artRes.requireValue("DefinitionSet::exporterExport interface"));
                            }
                        }
                        var unions = new TList<Artifact>(set.unions.size());
                        for (var u : set.unions) {
                            var uConst = new TypeTypeSourceUnionConst(u);
                            var artRes = uConst.artifactableExportArtifact(xx, loc);
                            artRes.drainInto(errors);
                            if (artRes.valueOrNull() != null) {
                                unions.add(artRes.requireValue("DefinitionSet::exporterExport union"));
                            }
                        }
                        var dependencies = new TList<Artifact>(set.dependencies.size());
                        for (var entry : set.dependencies.entrySet()) {
                            var depRes = ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, entry.getValue());
                            depRes.drainInto(errors);
                            if (depRes.valueOrNull() != null) {
                                dependencies.add(depRes.requireValue("DefinitionSet::exporterExport dependency"));
                            }
                        }
                        var body = new ArtifactDataDefSet(
                                set.defSetId,
                                TList.from(dependencies),
                                TList.from(functions),
                                TList.from(interfaces),
                                TList.from(objects),
                                TList.from(unions));
                        return makeBody.apply(ArtifactDataDefSetCodec.toValue(body));
                    });
            if (!errors.isEmpty()) {
                return EResult.mixed(artifact, errors);
            }
            return EResult.ok(artifact);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "DefinitionSet requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataDefSet body;
                        try {
                            body = ArtifactDataDefSetCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "DefinitionSet data: failed to parse ArtifactDataDefSet: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var set = new DefinitionSet(body.defSetId());
                        set.artifactIdCell.set(idValue);
                        var deps = new TList<Artifact>();
                        deps.addAll(body.objects());
                        deps.addAll(body.functions());
                        deps.addAll(body.interfaces());
                        deps.addAll(body.unions());
                        deps.addAll(body.dependencies());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            for (var dtArt : body.objects()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, dtArt);
                                var v = any.requireValue("DefinitionSet::finalizer object artifact");
                                TypeSourceObjectDynamic dtData;
                                if (v instanceof TypeTypeSourceObjectConst dtConst) {
                                    dtData = dtConst.type_();
                                } else if (v instanceof TypeTypeSourceObjectConst.ImportedSourceObjectConst impDt) {
                                    dtData = impDt.type_();
                                } else {
                                    throw new IllegalStateException(
                                            "def-set object artifact had wrong type: " + v.getClass());
                                }
                                dtData.objIdx = set.objects.size();
                                set.objects.add(dtData);
                            }
                            for (var fArt : body.functions()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, fArt);
                                var v = any.requireValue("DefinitionSet::finalizer function artifact");
                                SourceFunctionData fData;
                                if (v instanceof TypeSourceFunctionConst fConst) {
                                    fData = fConst.data();
                                } else if (v instanceof TypeSourceFunctionConst.ImportedSourceFunctionConst impFn) {
                                    fData = impFn.data();
                                } else {
                                    throw new IllegalStateException(
                                            "def-set function artifact had wrong type: " + v.getClass());
                                }
                                fData.funcIdx = set.functions.size();
                                set.functions.add(fData);
                            }
                            for (var iArt : body.interfaces()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, iArt);
                                var v = any.requireValue("DefinitionSet::finalizer interface artifact");
                                InterfaceData iData;
                                if (v instanceof TypeSourceInterfaceDynamic iConst) {
                                    iData = iConst.data();
                                } else if (v instanceof TypeSourceInterfaceDynamic.ImportedInterfaceConst impIc) {
                                    iData = impIc.data();
                                } else {
                                    throw new IllegalStateException(
                                            "def-set interface artifact had wrong type: " + v.getClass());
                                }
                                set.interfaces.add(iData);
                            }
                            for (var uArt : body.unions()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, uArt);
                                var v = any.requireValue("DefinitionSet::finalizer union artifact");
                                TypeSourceUnionDynamic uData;
                                if (v instanceof TypeTypeSourceUnionConst uConst) {
                                    uData = uConst.type_();
                                } else if (v instanceof TypeTypeSourceUnionConst.ImportedSourceUnionConst impU) {
                                    uData = impU.type_();
                                } else {
                                    throw new IllegalStateException(
                                            "def-set union artifact had wrong type: " + v.getClass());
                                }
                                uData.unionIdx = set.unions.size();
                                set.unions.add(uData);
                            }
                            for (var dep : body.dependencies()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, dep);
                                var v = any.requireValue("DefinitionSet::finalizer dependency artifact");
                                var depSet = mustCast(DefinitionSet.class, v);
                                set.dependencies.putIfAbsent(depSet.defSetId, depSet);
                            }
                            set.finished = true;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(set, deps, fin));
                    });
        }
    };
}
