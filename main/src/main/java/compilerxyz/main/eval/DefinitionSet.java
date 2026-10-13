package compilerxyz.main.eval;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.definition_set.ArtifactDataDefSet;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeLocTable;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeRenderRefs;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaClassCompiled;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.eval.value.source_function.SourceFunctionData;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.eval.value.source_interface.InterfaceData;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataDefSetCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import compilerxyz.prelude.util.TOrderedSet;
import java.util.function.Function;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class DefinitionSet implements Artifactable {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();
    public final DefSetId defSetId;
    public final TOrderedMap<DefSetId, DefinitionSet> dependencies;
    public boolean finished;
    public final TList<SourceFunctionData> functions;
    public final TList<InterfaceCell> interfaces;
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

    public EResult<DefinitionSetClassLoader> install(ExprContext xx, QExprId loc) {
        var depLoaders = new TList<DefinitionSetClassLoader>();
        for (var dep : dependencies.values()) {
            var depResult = dep.install(xx, loc);
            if (depResult.hasErrors()) {
                return depResult.reerr();
            }
            var depLoader = depResult.requireValue("DefinitionSet::install dependency loader");
            depLoaders.add(depLoader);
        }
        if (xx.vx.ix.classLoader.optIsNone()) {
            xx.vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                    xx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), depLoaders));
        }
        var loader = xx.vx.ix.classLoader.optUnwrap();
        if (!xx.vx.ix.installedDefSets.add(defSetId)) {
            return EResult.ok(loader);
        }
        if (!finished) {
            return EResult.err(
                    loc, "DefinitionSet not finished — call d.finish() in source before using this definition set");
        }
        for (var iface : interfaces) {
            if (iface.compiledBytecode.optIsSome()) {
                loader.registerClass(iface.binaryName, iface.compiledBytecode.optUnwrap());
            }
        }
        for (var obj : objects) {
            if (obj.underlyingType.optIsNone()) {
                return EResult.err(
                        loc,
                        "Object '" + obj.name.optUnwrapOr("(unnamed)")
                                + "' underlying type not finalised before install");
            }
            if (obj.compiledClass.optIsSome()) {
                var binaryName = obj.binaryName;
                var compiled = obj.compiledClass.optUnwrap();
                loader.registerClass(binaryName, compiled.bytecode());
                loader.registerLocs(binaryName, compiled.locs());
                if (obj.compiledStaticFieldValues.optIsSome()) {
                    var fields = new TOrderedMap<String, Object>();
                    fields.putAll(obj.compiledStaticFieldValues.optUnwrap());
                    fields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
                    loader.registerExternals(binaryName, fields);
                }
            }
        }
        for (var union : unions) {
            if (union.compiled.baseBytecode.optIsSome()) {
                loader.registerClass(union.baseBinaryName, union.compiled.baseBytecode.optUnwrap());
                if (union.compiled.variantBytecode.optIsSome()) {
                    var variantBytes = union.compiled.variantBytecode.optUnwrap();
                    for (int i = 0; i < variantBytes.size(); i++) {
                        loader.registerClass(
                                SourceUnionEmitter.variantBinaryNameFor(union.baseBinaryName, i), variantBytes.get(i));
                    }
                }
            }
        }
        var emittedFuncsOpt = finishFunctions();
        if (emittedFuncsOpt.optIsSome()) {
            var emittedFuncs = emittedFuncsOpt.optUnwrap();
            loader.registerClass(
                    emittedFuncs.binaryName(), emittedFuncs.compiled().bytecode());
            loader.registerLocs(
                    emittedFuncs.binaryName(), emittedFuncs.compiled().locs());
            var funcStaticFields = new TOrderedMap<String, Object>();
            funcStaticFields.putAll(emittedFuncs.staticFieldValues());
            funcStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
            loader.registerExternals(emittedFuncs.binaryName(), funcStaticFields);
            try {
                Class.forName(emittedFuncs.binaryName(), true, loader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError("Functions class " + emittedFuncs.binaryName() + " failed to load", e);
            }
        } else if (!functions.isEmpty()) {
            Opt<String> icaBinaryName = Opt.none();
            Opt<JavaClassCompiled> icaCompiled = Opt.none();
            var mergedStaticFields = new TOrderedMap<String, Object>();
            for (var func : functions) {
                if (func.compiledClassName.optIsNone() || func.compiledClass.optIsNone()) {
                    continue;
                }
                if (icaBinaryName.optIsNone()) {
                    icaBinaryName = func.compiledClassName;
                    icaCompiled = func.compiledClass;
                }
                if (func.compiledStaticFieldValues.optIsSome()) {
                    mergedStaticFields.putAll(func.compiledStaticFieldValues.optUnwrap());
                }
            }
            if (icaBinaryName.optIsSome()) {
                var binaryName = icaBinaryName.optUnwrap();
                var compiled = icaCompiled.optUnwrap();
                loader.registerClass(binaryName, compiled.bytecode());
                loader.registerLocs(binaryName, compiled.locs());
                mergedStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
                loader.registerExternals(binaryName, mergedStaticFields);
                try {
                    Class.forName(binaryName, true, loader);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("Imported Functions class " + binaryName + " failed to load", e);
                }
            }
        }
        for (var func : functions) {
            var compiledClassName = func.compiledClassName;
            var compiledClass = func.compiledClass;
            if (compiledClassName.optIsNone() || compiledClass.optIsNone()) {
                if (compiledClassName.optIsSome() || compiledClass.optIsSome()) {
                    throw new AssertionError("function has partially-populated compiled artifact (binaryName="
                            + compiledClassName
                            + ", bytecodeLen="
                            + compiledClass.optMap(c -> c.bytecode().length)
                            + ")");
                }
                continue;
            }
        }
        return EResult.ok(loader);
    }

    public void addDependency(DefinitionSet other) {
        if (other.defSetId.equals(defSetId)) {
            return;
        }
        dependencies.putIfAbsent(other.defSetId, other);
    }

    private void absorbRender(
            JavaBytecodeRenderRefs refs,
            ClassWriter cw,
            TOrderedMap<String, Object> staticFieldValues,
            TList<EvalContext.ExternalEntry> capturedExternals) {
        refs.declareFields(cw, staticFieldValues);
        capturedExternals.clear();
        capturedExternals.addAll(refs.externals());
        for (var dep : refs.defSets().values()) addDependency(dep);
    }

    private boolean objectImplements(TypeSourceObjectDynamic obj, Class<?> iface) {
        var internalName = JavaInternalName.fromClass(iface).value();
        for (var impl : obj.interfaceImpls) {
            if (impl.interface_.jvmInternalNameForSet(this).value().equals(internalName)) return true;
        }
        return false;
    }

    public Opt<String> incomplete() {
        if (finished) {
            return Opt.none();
        }
        for (var func : functions) {
            var r = func.incomplete();
            if (r.optIsSome()) return r;
        }
        for (var obj : objects) {
            var objName = obj.name.optUnwrapOr("(unnamed)");
            if (obj.underlyingType.optIsNone()) {
                return Opt.some("Object '" + objName + "' has no inner type (impl was not called)");
            }
            for (var method : obj.methods) {
                var r = method.incomplete(objName);
                if (r.optIsSome()) return r;
            }
            if (objectImplements(obj, Value.class) && objectImplements(obj, Type.class)) {
                return Opt.some("Object '" + objName
                        + "' implements both eval.value and eval.type: a value's type must be distinct from the value");
            }
        }
        for (var iface : interfaces) {
            if (!iface.building().implProvided) {
                return Opt.some("Interface '" + iface.description().optUnwrapOr("(unnamed)")
                        + "' has no impl (impl was not called)");
            }
        }
        for (var dep : dependencies.values()) {
            var r = dep.incomplete();
            if (r.optIsSome()) return r;
        }
        return Opt.none();
    }

    public Opt<TList<ExprErr>> finish(ExprContext xx) {
        if (finished) {
            return Opt.none();
        }
        for (var iface : interfaces) {
            var building = iface.building();
            var sieMethods = new TList<InterfaceMethodData>();
            for (var sieMethod : building.methods) {
                sieMethods.add(new InterfaceMethodData(
                        sieMethod.name, sieMethod.argType, sieMethod.retType, sieMethod.name, sieMethod.jvmDescriptor));
            }
            var internalName = iface.binaryName.replace('.', '/');
            var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(
                    Opcodes.V11,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                    internalName,
                    null,
                    JvmConstants.INTNAME_OBJECT.value(),
                    null);
            for (var method : sieMethods) {
                cw.visitMethod(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                                method.jvmName,
                                method.jvmDescriptor.value(),
                                null,
                                null)
                        .visitEnd();
            }
            cw.visitEnd();
            iface.compiledBytecode = Opt.some(cw.toByteArray());
            iface.freeze(new InterfaceData(
                    building.description, building.isAspect, building.aspectId, IList.from(sieMethods), Opt.none()));
        }
        for (var func : functions) {
            var r = func.finish(xx);
            if (r.optIsSome()) return r;
        }
        for (var obj : objects) {
            if (obj.underlyingType.optIsNone()) {
                throw new AssertionError(
                        "Object '" + obj.name.optUnwrapOr("(unnamed)") + "' finished while incomplete");
            }
            var underlyingType = obj.underlyingType.optUnwrap();
            var objInternalName = obj.internalName();
            for (var method : obj.methods) {
                var nameStr = obj.name.optUnwrapOr("(unnamed)");
                var r = method.finish(xx, nameStr, obj, underlyingType, objInternalName);
                if (r.optIsSome()) return r;
            }
            for (int idx = 0; idx < obj.interfaceImpls.size(); idx++) {
                var impl = obj.interfaceImpls.get(idx);
                impl.materialize("_iface" + idx);
                for (var m : impl.methodImpls) {
                    var r = m.finish(xx, obj, underlyingType);
                    if (r.optIsSome()) return r;
                }
            }
            if (obj.compiledClass.optIsNone()) {
                var internalNameStr = obj.internalName();
                var soeInternalName = JavaInternalName.of(internalNameStr);
                var implementsInternalNames = new TOrderedSet<String>();
                implementsInternalNames.add(SourceObjectEmitter.INTNAME_SOURCE_OBJECT_INSTANCE.value());
                for (var soeImpl : obj.interfaceImpls) {
                    var soeIface = soeImpl.interface_;
                    implementsInternalNames.add(
                            soeIface.jvmInternalNameForSet(this).value());
                }
                var interfacesArr =
                        implementsInternalNames.isEmpty() ? null : implementsInternalNames.toArray(String[]::new);
                var soeLocTable = new JavaBytecodeLocTable();
                var soeCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                    @Override
                    protected String getCommonSuperClass(String type1, String type2) {
                        return JvmConstants.INTNAME_OBJECT.value();
                    }
                };
                soeCw.visit(
                        Opcodes.V11,
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                        internalNameStr,
                        null,
                        JvmConstants.INTNAME_OBJECT.value(),
                        interfacesArr);
                var dataFieldDesc = JvmSort.descriptor(xx, obj.underlyingType.optUnwrap());
                soeCw.visitField(
                                Opcodes.ACC_PUBLIC,
                                SourceObjectEmitter.DATA_INSTANCE_FIELD,
                                dataFieldDesc.value(),
                                null,
                                null)
                        .visitEnd();
                var ctor = soeCw.visitMethod(
                        Opcodes.ACC_PUBLIC, "<init>", JavaMethodDescriptor.VOID_NOARGS.value(), null, null);
                ctor.visitCode();
                var ctorSeq = new JavaBytecodeSequence();
                {
                    var cb = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                    var self = cb.loadIndexed(0, JavaDataDescriptor.fromInternalName(soeInternalName));
                    cb.invokeSpecialVoid(JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS, self);
                    cb.retVoid();
                    ctorSeq.add(cb.code());
                }
                JavaBytecodeUtils.renderMethod(ctorSeq, ctor, TList.of(), soeInternalName, soeLocTable);
                var realCtorDesc =
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(dataFieldDesc));
                var realCtor = soeCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", realCtorDesc.value(), null, null);
                realCtor.visitCode();
                var realCtorSeq = new JavaBytecodeSequence();
                {
                    var cb = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                    var self = cb.loadIndexed(0, JavaDataDescriptor.fromInternalName(soeInternalName));
                    cb.invokeSpecialVoid(JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS, self);
                    var selfAgain = cb.loadIndexed(0, JavaDataDescriptor.fromInternalName(soeInternalName));
                    var data = cb.loadIndexed(1, dataFieldDesc);
                    cb.putField(
                            soeInternalName, SourceObjectEmitter.DATA_INSTANCE_FIELD, dataFieldDesc, selfAgain, data);
                    cb.retVoid();
                    realCtorSeq.add(cb.code());
                }
                JavaBytecodeUtils.renderMethod(realCtorSeq, realCtor, TList.of(), soeInternalName, soeLocTable);
                var staticFieldValues = new TOrderedMap<String, Object>();
                soeCw.visitField(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD,
                                JvmConstants.DESC_OBJECT.value(),
                                null,
                                null)
                        .visitEnd();
                staticFieldValues.put(SourceObjectEmitter.SOURCE_OBJECT_DATA_FIELD, obj);
                soeCw.visitField(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                EvalRootEmitter.EXPR_CONTEXT_FIELD,
                                EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
                                null,
                                null)
                        .visitEnd();
                for (long soeMethodIdx = 0; soeMethodIdx < obj.methods.size(); soeMethodIdx++) {
                    var method = obj.methods.get((int) soeMethodIdx);
                    if (method.compiledBodyCode.optIsNone()) {
                        continue;
                    }
                    {
                        var emmBodyCode = method.compiledBodyCode.optUnwrap();
                        var methodName = method.jvmMethodName;
                        var emmPrefix = methodName + "_";
                        JavaDataDescriptor retDesc;
                        if (method.retType.optIsSome()) {
                            var retConsume = GenAspectConsumeDynamic.getMaybe(xx, method.retType.optUnwrap());
                            if (retConsume.optIsNone()) {
                                throw new IllegalStateException("SourceObjectEmitter.emitMethodMethod: method '"
                                        + method.name + "' retType does not implement AspectConsumeDynamic");
                            }
                            retDesc = retConsume.optUnwrap().aspectConsumeDynamicJvmClass(xx);
                        } else {
                            retDesc = JavaDataDescriptor.DESC_VOID;
                        }
                        if (method.compiledArgSlots.optIsNone() || method.compiledArgSlotIds.optIsNone()) {
                            throw new IllegalStateException("SourceObjectEmitter.emitMethodMethod: method '"
                                    + method.name + "' has no compiledArgSlots (finish ordering violation)");
                        }
                        var argSlots = method.compiledArgSlots.optUnwrap();
                        var argSlotIds = method.compiledArgSlotIds.optUnwrap();
                        var argDescs = new TList<JavaDataDescriptor>(argSlots.size());
                        for (var s : argSlots) argDescs.add(s.jvmDesc());
                        var descriptor = JavaMethodDescriptor.fromParts(retDesc, argDescs);
                        var mv = soeCw.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor.value(), null, null);
                        if (method.innerBindingId.optIsNone()) {
                            throw new IllegalStateException("SourceObjectEmitter.emit: method '" + method.name
                                    + "' has no innerBindingId (finish ordering violation)");
                        }
                        var selfId = method.innerBindingId.optUnwrap();
                        var seq = new JavaBytecodeSequence();
                        var emmLoc = CodeLoc.Infallible.INSTANCE;
                        var selfLocal = mustCast(BindingId.class, selfId);
                        var emmDataFieldDesc = JvmSort.descriptor(xx, obj.underlyingType.optUnwrap());
                        var soeDesc = JavaDataDescriptor.fromInternalName(soeInternalName);
                        {
                            var pb = new JavaBytecodeBuilder(emmLoc);
                            if (method.outerBindingId.optIsSome()) {
                                pb.store(method.outerBindingId.optUnwrap(), pb.loadIndexed(0, soeDesc));
                            }
                            var data = pb.getField(
                                    soeInternalName,
                                    SourceObjectEmitter.DATA_INSTANCE_FIELD,
                                    emmDataFieldDesc,
                                    pb.loadIndexed(0, soeDesc));
                            pb.store(selfLocal, data);
                            seq.add(pb.code());
                        }
                        var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
                        initialSlots.add(new JavaBytecodeSequence.InitialSlot(new BindingId()));
                        for (int emmI = 0; emmI < argSlots.size(); emmI++) {
                            initialSlots.add(new JavaBytecodeSequence.InitialSlot(
                                    argSlotIds.get(emmI),
                                    argSlots.get(emmI).jvmDesc().isWide()));
                        }
                        seq.add(emmBodyCode);
                        {
                            var rb = new JavaBytecodeBuilder(emmLoc);
                            if (retDesc.value().equals(JavaDataDescriptor.DESC_VOID.value())
                                    || method.compiledBodyResult.optIsNone()) {
                                rb.retVoid();
                            } else {
                                rb.ret(method.compiledBodyResult.optUnwrap());
                            }
                            seq.add(rb.code());
                        }
                        var emmRefs = new JavaBytecodeRenderRefs(soeInternalName, emmPrefix);
                        JavaBytecodeUtils.renderMethodTyped(
                                seq,
                                mv,
                                initialSlots,
                                obj.typeDesc() + "." + methodName,
                                Opcodes.ACC_PUBLIC,
                                descriptor,
                                emmRefs,
                                soeLocTable);
                        absorbRender(emmRefs, soeCw, staticFieldValues, method.capturedExternals);
                    }
                }
                for (int soeImplIdx = 0; soeImplIdx < obj.interfaceImpls.size(); soeImplIdx++) {
                    var impl = obj.interfaceImpls.get(soeImplIdx);
                    for (int soeMIdx = 0; soeMIdx < impl.methodImpls.size(); soeMIdx++) {
                        var m = impl.methodImpls.get(soeMIdx);
                        if (m.compiledBodyCode.optIsNone()) {
                            continue;
                        }
                        {
                            var eimMethodName = m.jvmMethodName;
                            var eimPrefix = eimMethodName + "_";
                            if (m.innerBindingId.optIsNone()) {
                                throw new IllegalStateException(
                                        "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no innerBindingId");
                            }
                            var eimSelfId = mustCast(BindingId.class, m.innerBindingId.optUnwrap());
                            if (m.compiledArgParamIds.optIsNone()) {
                                throw new IllegalStateException(
                                        "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
                            }
                            var argParamIds = m.compiledArgParamIds.optUnwrap();
                            var ifaceMethod = m.interfaceMethod;
                            var eimDescriptor = SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, m, ifaceMethod);
                            var jvmParamKeys = new TList<BindingId>();
                            var jvmParamWides = new TList<Boolean>();
                            if (m.compiledArgParamTypes.optIsNone()
                                    || m.compiledArgParamTypes.optUnwrap().size() != argParamIds.size()) {
                                throw new IllegalStateException(
                                        "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
                            }
                            var argParamTypes = m.compiledArgParamTypes.optUnwrap();
                            for (int eimI = 0; eimI < argParamIds.size(); eimI++) {
                                var eimArgType = argParamTypes.get(eimI);
                                var argConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, eimArgType);
                                if (argConsumeOpt.optIsNone()) {
                                    throw new IllegalStateException(
                                            "SourceObjectEmitter.emitInterfaceMethodMethod: arg type "
                                                    + eimArgType.typeDesc()
                                                    + " does not implement AspectConsumeDynamic");
                                }
                                var argDesc = argConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
                                jvmParamKeys.add(argParamIds.get(eimI));
                                jvmParamWides.add(argDesc.isWide());
                            }
                            var eimMv = soeCw.visitMethod(
                                    Opcodes.ACC_PUBLIC, eimMethodName, eimDescriptor.value(), null, null);
                            var eimSeq = new JavaBytecodeSequence();
                            var eimLoc = CodeLoc.Infallible.INSTANCE;
                            var innerDesc = JvmSort.descriptor(xx, obj.underlyingType.optUnwrap());
                            var eimSoeDesc = JavaDataDescriptor.fromInternalName(soeInternalName);
                            {
                                var pb = new JavaBytecodeBuilder(eimLoc);
                                if (m.outerBindingId.optIsSome()) {
                                    pb.store(m.outerBindingId.optUnwrap(), pb.loadIndexed(0, eimSoeDesc));
                                }
                                var data = pb.getField(
                                        soeInternalName,
                                        SourceObjectEmitter.DATA_INSTANCE_FIELD,
                                        innerDesc,
                                        pb.loadIndexed(0, eimSoeDesc));
                                pb.store(eimSelfId, data);
                                eimSeq.add(pb.code());
                            }
                            var eimInitialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
                            eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(new BindingId()));
                            for (int i = 0; i < jvmParamKeys.size(); i++) {
                                eimInitialSlots.add(new JavaBytecodeSequence.InitialSlot(
                                        jvmParamKeys.get(i), jvmParamWides.get(i)));
                            }
                            eimSeq.add(m.compiledBodyCode.optUnwrap());
                            {
                                var rb = new JavaBytecodeBuilder(eimLoc);
                                var ercRetDesc = eimDescriptor.returnDescriptor();
                                if (ercRetDesc.value().equals(JavaDataDescriptor.DESC_VOID.value())
                                        || m.compiledBodyResult.optIsNone()) {
                                    rb.retVoid();
                                } else {
                                    var result = m.compiledBodyResult.optUnwrap();
                                    if (ercRetDesc.value().charAt(0) == 'L') {
                                        result = rb.checkcast(ercRetDesc, result);
                                    }
                                    rb.ret(result);
                                }
                                eimSeq.add(rb.code());
                            }
                            var eimRefs = new JavaBytecodeRenderRefs(soeInternalName, eimPrefix);
                            JavaBytecodeUtils.renderMethodTyped(
                                    eimSeq,
                                    eimMv,
                                    eimInitialSlots,
                                    obj.typeDesc() + "." + eimMethodName,
                                    Opcodes.ACC_PUBLIC,
                                    eimDescriptor,
                                    eimRefs,
                                    soeLocTable);
                            absorbRender(eimRefs, soeCw, staticFieldValues, m.capturedExternals);
                        }
                    }
                }
                {
                    var emitted = new TOrderedSet<String>();
                    for (int ehiImplIdx = 0; ehiImplIdx < obj.interfaceImpls.size(); ehiImplIdx++) {
                        var ehiImpl = obj.interfaceImpls.get(ehiImplIdx);
                        for (int ehiMIdx = 0; ehiMIdx < ehiImpl.methodImpls.size(); ehiMIdx++) {
                            var ehiM = ehiImpl.methodImpls.get(ehiMIdx);
                            if (ehiM.compiledBodyCode.optIsNone()) {
                                continue;
                            }
                            var ehiIfaceMethod = ehiM.interfaceMethod;
                            var hostName = ehiIfaceMethod.jvmName;
                            var hostDescriptor = ehiIfaceMethod.jvmDescriptor;
                            var dedupeKey = hostName + hostDescriptor.value();
                            if (!emitted.add(dedupeKey)) {
                                continue;
                            }
                            {
                                var ehaMv = soeCw.visitMethod(
                                        Opcodes.ACC_PUBLIC, hostName, hostDescriptor.value(), null, null);
                                var ehaLoc = CodeLoc.Infallible.INSTANCE;
                                var ehaSeq = new JavaBytecodeSequence();
                                var eb = new JavaBytecodeBuilder(ehaLoc);
                                var ehaSelf = eb.loadIndexed(0, JavaDataDescriptor.fromInternalName(soeInternalName));
                                var calleeArgs = new TList<JavaBytecodeReg>();
                                var calleeDescriptor =
                                        SourceObjectEmitter.interfaceMethodMethodDescriptor(xx, ehiM, ehiIfaceMethod);
                                int hostSlot = 1;
                                if (ehiM.compiledArgParamTypes.optIsSome()) {
                                    var ehaArgParamTypes = ehiM.compiledArgParamTypes.optUnwrap();
                                    var hostParams = hostDescriptor.argumentDescriptors();
                                    int argIdx = 0;
                                    for (var ehaArgType : ehaArgParamTypes) {
                                        var hostParamDesc = hostParams.get(argIdx);
                                        var hostArgType = hostParamDesc.isReference()
                                                ? DynUtil.substituteBoxed(xx, ehaArgType)
                                                : ehaArgType;
                                        var hostArgDesc = JvmSort.descriptor(xx, hostArgType);
                                        var raw = eb.loadIndexed(hostSlot, hostParamDesc);
                                        if (!hostParamDesc.value().equals(hostArgDesc.value())
                                                && hostArgDesc.isReference()) {
                                            raw = eb.checkcast(hostArgDesc, raw);
                                        }
                                        var hostArgCv = DynUtil.regToValue(
                                                new CodeDynamic(ehaLoc, new JavaBytecodeSequence()), raw, hostArgType);
                                        var argDyn = DynUtil.materialize(
                                                        xx, ehiM.compiledBodyLoc.optUnwrap(), ehaArgType, hostArgCv)
                                                .requireValue("SourceObjectEmitter.emitHostAdapter: arg convert");
                                        calleeArgs.add(eb.reg(argDyn));
                                        hostSlot += hostParamDesc.slotWidth();
                                        argIdx++;
                                    }
                                }
                                var callArgs = new TList<JavaBytecodeReg>();
                                callArgs.add(ehaSelf);
                                callArgs.addAll(calleeArgs);
                                var calleeRet = eb.invoke(
                                        Opcodes.INVOKEVIRTUAL,
                                        soeInternalName,
                                        ehiM.jvmMethodName,
                                        calleeDescriptor,
                                        false,
                                        callArgs);
                                var ehaRetDesc = hostDescriptor.returnDescriptor();
                                if (ehiIfaceMethod.retType.optIsNone() || calleeRet.optIsNone()) {
                                    if (ehaRetDesc.isReference()) {
                                        eb.ret(eb.constNull());
                                    } else {
                                        eb.retVoid();
                                    }
                                } else if (ehaRetDesc.isReference()) {
                                    var calleeRetType = ehiIfaceMethod.retType.optUnwrap();
                                    var hostRetType = DynUtil.substituteBoxed(xx, calleeRetType);
                                    var calleeCv = DynUtil.regToValue(
                                            new CodeDynamic(ehaLoc, new JavaBytecodeSequence()),
                                            calleeRet.optUnwrap(),
                                            calleeRetType);
                                    var hostDyn = DynUtil.materialize(
                                                    xx, ehiM.compiledBodyLoc.optUnwrap(), hostRetType, calleeCv)
                                            .requireValue("SourceObjectEmitter.emitHostAdapter: ret convert");
                                    eb.ret(eb.reg(hostDyn));
                                } else {
                                    eb.ret(calleeRet.optUnwrap());
                                }
                                ehaSeq.add(eb.code());
                                JavaBytecodeUtils.renderMethod(ehaSeq, ehaMv, TList.of(), soeInternalName, soeLocTable);
                            }
                        }
                    }
                }
                {
                    var implementsValue = objectImplements(obj, Value.class);
                    var implementsType = objectImplements(obj, Type.class);
                    if (implementsValue || implementsType) {
                        var aeDescriptor = JavaMethodDescriptor.fromParts(
                                JavaDataDescriptor.fromClass(EResult.class),
                                JavaDataDescriptor.fromClass(ExprContext.class),
                                JavaDataDescriptor.fromClass(QExprId.class));
                        var aeMv = soeCw.visitMethod(
                                Opcodes.ACC_PUBLIC, "artifactableExporter", aeDescriptor.value(), null, null);
                        var aeSeq = new JavaBytecodeSequence();
                        var ab = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                        if (implementsValue) {
                            var aeSelf = ab.loadIndexed(0, JavaDataDescriptor.fromInternalName(soeInternalName));
                            var aeXx = ab.loadIndexed(1, JavaDataDescriptor.fromClass(ExprContext.class));
                            var aeLoc = ab.loadIndexed(2, JavaDataDescriptor.fromClass(QExprId.class));
                            ab.ret(ab.invokeStatic(
                                    GenCodegenDescriptors.DYN_RUNTIME_SOURCE_OBJECT_EXPORTER, aeSelf, aeXx, aeLoc));
                        } else {
                            ab.ret(ab.invokeStatic(GenCodegenDescriptors.DYN_RUNTIME_SOURCE_OBJECT_TYPE_EXPORTER));
                        }
                        aeSeq.add(ab.code());
                        JavaBytecodeUtils.renderMethod(aeSeq, aeMv, TList.of(), soeInternalName, soeLocTable);
                    }
                }
                soeCw.visitEnd();
                obj.compiledClass = Opt.some(new JavaClassCompiled(soeCw.toByteArray(), soeLocTable.locs()));
                obj.compiledStaticFieldValues = Opt.some(staticFieldValues);
            }
        }
        for (var seu : unions) {
            if (seu.compiled.baseBytecode.optIsNone()) {
                var seuBaseBinaryName = seu.baseBinaryName;
                var baseInternalName = seuBaseBinaryName.replace('.', '/');
                var variantBinaryNames = new TList<String>(seu.variants.size());
                var variantInternalNames = new TList<String>(seu.variants.size());
                for (int seuI = 0; seuI < seu.variants.size(); seuI++) {
                    var vbn = SourceUnionEmitter.variantBinaryNameFor(seuBaseBinaryName, seuI);
                    variantBinaryNames.add(vbn);
                    variantInternalNames.add(vbn.replace('.', '/'));
                }
                {
                    var ebCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    ebCw.visit(
                            Opcodes.V17,
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SUPER,
                            baseInternalName,
                            null,
                            JvmConstants.INTNAME_OBJECT.value(),
                            null);
                    for (var vin : variantInternalNames) {
                        ebCw.visitPermittedSubclass(vin);
                    }
                    var ebCtor = ebCw.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
                    ebCtor.visitCode();
                    ebCtor.visitVarInsn(Opcodes.ALOAD, 0);
                    ebCtor.visitMethodInsn(
                            Opcodes.INVOKESPECIAL, JvmConstants.INTNAME_OBJECT.value(), "<init>", "()V", false);
                    ebCtor.visitInsn(Opcodes.RETURN);
                    ebCtor.visitMaxs(0, 0);
                    ebCtor.visitEnd();
                    ebCw.visitEnd();
                    seu.compiled.baseBytecode = Opt.some(ebCw.toByteArray());
                }
                var seuVariantBytecode = new TList<byte[]>(seu.variants.size());
                for (int i = 0; i < seu.variants.size(); i++) {
                    var variant = seu.variants.get(i);
                    var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, variant.type);
                    {
                        var variantInternalName = variantInternalNames.get(i);
                        var evCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        evCw.visit(
                                Opcodes.V17,
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                                variantInternalName,
                                null,
                                baseInternalName,
                                null);
                        evCw.visitField(
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                                        SourceUnionEmitter.PAYLOAD_FIELD,
                                        payloadDesc.value(),
                                        null,
                                        null)
                                .visitEnd();
                        var ctorDesc = "(" + payloadDesc.value() + ")V";
                        var evCtor = evCw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ctorDesc, null, null);
                        evCtor.visitCode();
                        evCtor.visitVarInsn(Opcodes.ALOAD, 0);
                        evCtor.visitMethodInsn(Opcodes.INVOKESPECIAL, baseInternalName, "<init>", "()V", false);
                        evCtor.visitVarInsn(Opcodes.ALOAD, 0);
                        evCtor.visitVarInsn(payloadDesc.loadOpcode(), 1);
                        evCtor.visitFieldInsn(
                                Opcodes.PUTFIELD,
                                variantInternalName,
                                SourceUnionEmitter.PAYLOAD_FIELD,
                                payloadDesc.value());
                        evCtor.visitInsn(Opcodes.RETURN);
                        evCtor.visitMaxs(0, 0);
                        evCtor.visitEnd();
                        evCw.visitEnd();
                        seuVariantBytecode.add(evCw.toByteArray());
                    }
                }
                seu.compiled.variantBytecode = Opt.some(seuVariantBytecode);
            }
        }
        finishFunctions();
        finished = true;
        for (var dep : dependencies.values()) {
            var incompleteDep = dep.incomplete();
            if (incompleteDep.optIsSome()) throw new IllegalStateException(incompleteDep.optUnwrap());
            var r = dep.finish(xx);
            if (r.optIsSome()) return r;
        }
        return Opt.none();
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
        var binaryName = DefSetFunctionsEmitter.functionsBinaryName(defSetId);
        var internalName = DefSetFunctionsEmitter.functionsInternalName(defSetId);
        var locTable = new JavaBytecodeLocTable();
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return JvmConstants.INTNAME_OBJECT.value();
            }
        };
        cw.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalName,
                null,
                JvmConstants.INTNAME_OBJECT.value(),
                null);
        var staticFieldValues = new TOrderedMap<String, Object>();
        cw.visitField(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        EvalRootEmitter.EXPR_CONTEXT_FIELD,
                        EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
                        null,
                        null)
                .visitEnd();
        for (int specI = 0; specI < specs.size(); specI++) {
            var spec = specs.get(specI);
            var methodName = spec.methodName();
            var efmPrefix = DefSetFunctionsEmitter.functionFieldPrefix(spec.methodName());
            var argDescs = new TList<JavaDataDescriptor>(spec.argSlots().size());
            for (var s : spec.argSlots()) argDescs.add(s.jvmDesc());
            var mv = cw.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    methodName,
                    JavaMethodDescriptor.fromParts(spec.retDesc(), argDescs).value(),
                    null,
                    null);
            var loc = CodeLoc.Infallible.INSTANCE;
            var seq = new JavaBytecodeSequence();
            var ownerInternal = JavaInternalName.of(internalName);
            seq.add(spec.bodyCode());
            {
                var rb = new JavaBytecodeBuilder(loc);
                if (spec.retDesc().value().equals(JavaDataDescriptor.DESC_VOID.value())
                        || spec.bodyResult().optIsNone()) {
                    rb.retVoid();
                } else {
                    rb.ret(spec.bodyResult().optUnwrap());
                }
                seq.add(rb.code());
            }
            var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
            for (var efmSlot : spec.argSlots()) {
                initialSlots.add(new JavaBytecodeSequence.InitialSlot(
                        efmSlot.id(), efmSlot.jvmDesc().isWide()));
            }
            var efmRefs = new JavaBytecodeRenderRefs(ownerInternal, efmPrefix);
            JavaBytecodeUtils.renderMethodTyped(
                    seq,
                    mv,
                    initialSlots,
                    internalName + "." + spec.methodName(),
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    JavaMethodDescriptor.fromParts(spec.retDesc(), argDescs),
                    efmRefs,
                    locTable);
            absorbRender(efmRefs, cw, staticFieldValues, contributors.get(specI).capturedExternals);
        }
        cw.visitEnd();
        var emitted = new DefSetFunctionsEmitter.EmittedFunctions(
                binaryName, internalName, new JavaClassCompiled(cw.toByteArray(), locTable.locs()), staticFieldValues);
        for (var func : contributors) {
            func.compiledClassName = Opt.some(emitted.binaryName());
            func.compiledClass = Opt.some(emitted.compiled());
            func.compiledStaticFieldValues = Opt.some(emitted.staticFieldValues());
            func.functionSpec = Opt.none();
        }
        return Opt.some(emitted);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.DEFINITION_SET);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var set = mustCast(DefinitionSet.class, self);
            return ArtifactSupport.implExportIndependentArtifact(xx, set.artifactIdCell, () -> {
                var erb = new Erb(loc);
                var objects = new TList<Artifact>(set.objects.size());
                for (var dt : set.objects) {
                    var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, dt));
                    if (art.optIsSome()) objects.add(art.optUnwrap());
                }
                var functions = new TList<Artifact>(set.functions.size());
                for (var f : set.functions) {
                    var art = erb.mustErMaybeUnreachable(
                            Artifactable.exportArtifact(xx, loc, new TypeSourceFunctionConst(f)));
                    if (art.optIsSome()) functions.add(art.optUnwrap());
                }
                var interfaces = new TList<Artifact>(set.interfaces.size());
                for (var i : set.interfaces) {
                    var art = erb.mustErMaybeUnreachable(
                            Artifactable.exportArtifact(xx, loc, new TypeSourceInterfaceDynamic(i)));
                    if (art.optIsSome()) interfaces.add(art.optUnwrap());
                }
                var unions = new TList<Artifact>(set.unions.size());
                for (var u : set.unions) {
                    var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, u));
                    if (art.optIsSome()) unions.add(art.optUnwrap());
                }
                var dependencies = new TList<Artifact>(set.dependencies.size());
                for (var entry : set.dependencies.entrySet()) {
                    var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, entry.getValue()));
                    if (art.optIsSome()) dependencies.add(art.optUnwrap());
                }
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactDataDefSet(
                        set.defSetId,
                        TList.from(dependencies),
                        TList.from(functions),
                        TList.from(interfaces),
                        TList.from(objects),
                        TList.from(unions));
                return EResult.ok(makeBody.apply(ArtifactDataDefSetCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
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
                                dtData = mustCast(TypeSourceObjectDynamic.class, v);
                                set.objects.add(dtData);
                            }
                            for (var fArt : body.functions()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, fArt);
                                var v = any.requireValue("DefinitionSet::finalizer function artifact");
                                SourceFunctionData fData;
                                fData = mustCast(TypeSourceFunctionConst.class, v)
                                        .data();
                                set.functions.add(fData);
                            }
                            for (var iArt : body.interfaces()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, iArt);
                                var v = any.requireValue("DefinitionSet::finalizer interface artifact");
                                set.interfaces.add(mustCast(TypeSourceInterfaceDynamic.class, v)
                                        .cell());
                            }
                            for (var uArt : body.unions()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, uArt);
                                var v = any.requireValue("DefinitionSet::finalizer union artifact");
                                TypeSourceUnionDynamic uData;
                                uData = mustCast(TypeSourceUnionDynamic.class, v);
                                set.unions.add(uData);
                            }
                            for (var dep : body.dependencies()) {
                                var any = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, dep);
                                var v = any.requireValue("DefinitionSet::finalizer dependency artifact");
                                var depSet = mustCast(DefinitionSet.class, v);
                                set.dependencies.putIfAbsent(depSet.defSetId, depSet);
                            }
                            set.finished = true;
                            return set;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.some(set), deps, fin));
                    });
        }
    };
}
