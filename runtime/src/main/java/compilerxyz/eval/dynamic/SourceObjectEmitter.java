package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.value.source_interface.SourceInterfaceMethodData;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import compilerxyz.util.TSet;

public final class SourceObjectEmitter {
  private SourceObjectEmitter() {}

    private static final JavaInternalName INTNAME_DYN_RUNTIME =
            JavaInternalName.of("compilerxyz/eval/dynamic/DynRuntime");
  private static final JavaInternalName INTNAME_EXPR_CONTEXT =
      JavaInternalName.of("compilerxyz/eval/ExprContext");
  private static final JavaInternalName INTNAME_QEXPR_ID =
      JavaInternalName.of("compilerxyz/eval/QExprId");
  private static final JavaInternalName INTNAME_TYPE =
      JavaInternalName.of("compilerxyz/eval/Type");
  private static final JavaInternalName INTNAME_CODE_VALUE =
      JavaInternalName.of("compilerxyz/eval/CodeValue");
  private static final JavaInternalName INTNAME_CODE =
      JavaInternalName.of("compilerxyz/eval/Code");
  private static final JavaInternalName INTNAME_EXPR =
      JavaInternalName.of("compilerxyz/eval/Expr");
  private static final JavaInternalName INTNAME_ERESULT =
      JavaInternalName.of("compilerxyz/eval/EResult");
  private static final JavaInternalName INTNAME_CONST_KEY =
      JavaInternalName.of("compilerxyz/eval/ConstKey");
  private static final JavaInternalName INTNAME_UNIQUE =
      JavaInternalName.of("compilerxyz/eval/Unique");
  private static final JavaInternalName INTNAME_BINDING_ID =
      JavaInternalName.of("compilerxyz/eval/BindingId");
  private static final JavaInternalName INTNAME_BINDING =
      JavaInternalName.of("compilerxyz/eval/Binding");
  private static final JavaInternalName INTNAME_PAIR =
      JavaInternalName.of("compilerxyz/util/Pair");
  private static final JavaInternalName INTNAME_OPT =
      JavaInternalName.of("compilerxyz/util/Opt");
  private static final JavaInternalName INTNAME_OPT_SOME =
      JavaInternalName.of("compilerxyz/util/Opt$Some");
  private static final JavaInternalName INTNAME_TLIST =
      JavaInternalName.of("compilerxyz/util/TList");
  private static final JavaInternalName INTNAME_TARGET_BIND_SUB =
      JavaInternalName.of("compilerxyz/eval/dynamic/target/TargetBindSub");
  private static final JavaInternalName INTNAME_SOURCE_OBJECT_INSTANCE =
      JavaInternalName.of("compilerxyz/eval/value/source_object/SourceObjectInstance");
  private static final JavaInternalName INTNAME_ASPECT_BIND =
      JavaInternalName.of("compilerxyz/gen/AspectBind");
  private static final JavaInternalName INTNAME_ASPECT_BIND_DYNAMIC =
      JavaInternalName.of("compilerxyz/gen/AspectBindDynamic");
  private static final JavaInternalName INTNAME_ASPECT_CONSUME_DYNAMIC =
      JavaInternalName.of("compilerxyz/gen/AspectConsumeDynamic");
  private static final JavaInternalName INTNAME_ASPECT_FIELD =
      JavaInternalName.of("compilerxyz/gen/AspectField");
  private static final JavaInternalName INTNAME_ASPECT_INITIAL_BIND =
      JavaInternalName.of("compilerxyz/gen/AspectInitialBind");
  private static final JavaInternalName INTNAME_ASPECT_LIFT =
      JavaInternalName.of("compilerxyz/gen/AspectLift");
  private static final JavaInternalName INTNAME_OBJECT_ARRAY_ELEM =
      JavaInternalName.of("[Ljava/lang/Object;");

    private static final JavaDataDescriptor DESC_EXPR_CONTEXT =
            JavaDataDescriptor.fromInternalName(INTNAME_EXPR_CONTEXT);
  private static final JavaDataDescriptor DESC_QEXPR_ID =
      JavaDataDescriptor.fromInternalName(INTNAME_QEXPR_ID);
  private static final JavaDataDescriptor DESC_TYPE =
      JavaDataDescriptor.fromInternalName(INTNAME_TYPE);
  private static final JavaDataDescriptor DESC_CODE_VALUE =
      JavaDataDescriptor.fromInternalName(INTNAME_CODE_VALUE);
  private static final JavaDataDescriptor DESC_CODE =
      JavaDataDescriptor.fromInternalName(INTNAME_CODE);
  private static final JavaDataDescriptor DESC_EXPR =
      JavaDataDescriptor.fromInternalName(INTNAME_EXPR);
  private static final JavaDataDescriptor DESC_ERESULT =
      JavaDataDescriptor.fromInternalName(INTNAME_ERESULT);
  private static final JavaDataDescriptor DESC_CONST_KEY =
      JavaDataDescriptor.fromInternalName(INTNAME_CONST_KEY);
  private static final JavaDataDescriptor DESC_UNIQUE =
      JavaDataDescriptor.fromInternalName(INTNAME_UNIQUE);
  private static final JavaDataDescriptor DESC_BINDING_ID =
      JavaDataDescriptor.fromInternalName(INTNAME_BINDING_ID);
  private static final JavaDataDescriptor DESC_BINDING =
      JavaDataDescriptor.fromInternalName(INTNAME_BINDING);
  private static final JavaDataDescriptor DESC_PAIR =
      JavaDataDescriptor.fromInternalName(INTNAME_PAIR);
  private static final JavaDataDescriptor DESC_OPT =
      JavaDataDescriptor.fromInternalName(INTNAME_OPT);
  private static final JavaDataDescriptor DESC_TLIST =
      JavaDataDescriptor.fromInternalName(INTNAME_TLIST);
  private static final JavaDataDescriptor DESC_TARGET_BIND_SUB =
      JavaDataDescriptor.fromInternalName(INTNAME_TARGET_BIND_SUB);
  private static final JavaDataDescriptor DESC_LIST =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_LIST);

    private static final JavaMethodDescriptor MDESC_METHOD_METHOD =
            JavaMethodDescriptor.fromParts(
                    JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_FIELD_GET =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_STRING);
  private static final JavaMethodDescriptor MDESC_OBJECT_INIT =
      JavaMethodDescriptor.VOID_NOARGS;
  private static final JavaMethodDescriptor MDESC_CTOR_OBJECT =
      JavaMethodDescriptor.fromConstructorParts(JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_HASHMAP_INIT =
      JavaMethodDescriptor.VOID_NOARGS;
  private static final JavaMethodDescriptor MDESC_HASHMAP_PUT =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_BOOLEAN_VALUE =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL);
  private static final JavaMethodDescriptor MDESC_WRAP_ONE_OBJ =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_WRAP_TWO_OBJ =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_ERESULT_FROM_ONE =
      JavaMethodDescriptor.fromParts(DESC_ERESULT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_ERESULT_FROM_TWO =
      JavaMethodDescriptor.fromParts(DESC_ERESULT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);

  public record EmittedSourceObject(
      String binaryName,
      String internalName,
      byte[] bytecode,
      TOrderedMap<String, Object> staticFieldValues) {}

  public static String binaryNameFor(DefSetId defSet, long objectIndex) {
    var compileId = defSet.compileId();
    return "SourceObject$"
        + compileId.importState().value()
        + "_"
        + compileId.source().value()
        + "_"
        + defSet.index()
        + "_"
        + objectIndex;
  }

  public static String internalNameFor(DefSetId defSet, long objectIndex) {
    return binaryNameFor(defSet, objectIndex).replace('.', '/');
  }

  public static String methodMethodName(long methodIdx) {
    return "_" + methodIdx;
  }

  public static String methodFieldPrefix(long methodIdx) {
    return "_" + methodIdx + "_";
  }

  public static String interfaceMethodMethodName(int implIdx, int methodImplIdx) {
    return "_iface" + implIdx + "_method" + methodImplIdx;
  }

  public static String interfaceMethodFieldPrefix(int implIdx, int methodImplIdx) {
    return "_iface" + implIdx + "_method" + methodImplIdx + "_";
  }

  public static final String DATA_INSTANCE_FIELD = "_data";

  public static final String DATA_CTOR_DESCRIPTOR = JvmConstants.DESC_OBJECT.value();

  public static final String SOURCE_KEY_MAP_FIELD = "_sourceKeyMap";

  public static final String SOURCE_OBJECT_DATA_FIELD = "_sourceObjectData";

  public static @org.jspecify.annotations.Nullable JavaInternalName builtinHostInterfaceInternalName(
      BuiltinInterfaceKind kind) {
    return switch (kind) {
      case TYPE -> INTNAME_TYPE;
      case VALUE -> JavaInternalName.of("compilerxyz/eval/Value");
      case ASPECT_FIELD -> INTNAME_ASPECT_FIELD;
      case ASPECT_FUNCTION -> JavaInternalName.of("compilerxyz/gen/AspectFunction");
      case ASPECT_DROP_DYNAMIC -> JavaInternalName.of("compilerxyz/gen/AspectDropDynamic");
      case BINDING -> JavaInternalName.of("compilerxyz/gen/Binding");
      case TARGET -> JavaInternalName.of("compilerxyz/gen/Target");
      case CODE_DYN -> null;
    };
  }

  public static EmittedSourceObject emit(
      DefSetId defSet,
      long objectIndex,
      TypeSourceObjectDynamic data,
      TOrderedMap<String, compilerxyz.eval.External> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    var binaryName = binaryNameFor(defSet, objectIndex);
    var internalNameStr = binaryName.replace('.', '/');
    var internalName = JavaInternalName.of(internalNameStr);

    var claimedKinds = new java.util.HashSet<BuiltinInterfaceKind>();
    for (var impl : data.interfaceImpls) {
      if (impl.interface_.builtinKind != null) {
        claimedKinds.add(impl.interface_.builtinKind);
      }
    }
    var implementsInternalNames = new TOrderedSet<String>();

    implementsInternalNames.add(INTNAME_SOURCE_OBJECT_INSTANCE.value());
    for (var spec : INLINE_HELPER_METHODS) {
      if (spec.jvmInterface() == null) continue;
      if (spec.requiredKind() != null && !claimedKinds.contains(spec.requiredKind())) continue;
      implementsInternalNames.add(spec.jvmInterface().value());
    }
    for (var impl : data.interfaceImpls) {
      var iface = impl.interface_;
      if (iface.builtinKind != null) {
        var hostInternal = builtinHostInterfaceInternalName(iface.builtinKind);
        if (hostInternal != null) {
          implementsInternalNames.add(hostInternal.value());
        }
      }
    }
    var interfacesArr =
        implementsInternalNames.isEmpty()
            ? null
            : implementsInternalNames.toArray(new String[0]);

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
        internalNameStr,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        interfacesArr);

    cw.visitField(Opcodes.ACC_PUBLIC, DATA_INSTANCE_FIELD, JvmConstants.DESC_OBJECT.value(), null, null)
        .visitEnd();

    cw.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            SOURCE_KEY_MAP_FIELD,
            JvmConstants.DESC_OBJECT.value(),
            null,
            null)
        .visitEnd();

    var ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", MDESC_OBJECT_INIT.value(), null, null);
    ctor.visitCode();
    var ctorSeq = new JavaBytecodeSequence();
    ctorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    ctorSeq.add(JavaBytecodeUtils.invokeSpecial(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT));
    ctorSeq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
    ctorSeq.render(ctor, TList.of());
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();

    var realCtor =
        cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", MDESC_CTOR_OBJECT.value(), null, null);
    realCtor.visitCode();
    var realCtorSeq = new JavaBytecodeSequence();
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    realCtorSeq.add(JavaBytecodeUtils.invokeSpecial(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT));
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 1));
    realCtorSeq.add(JavaBytecodeUtils.setField(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, internalName, DATA_INSTANCE_FIELD, JvmConstants.DESC_OBJECT));
    realCtorSeq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
    realCtorSeq.render(realCtor, TList.of());
    realCtor.visitMaxs(0, 0);
    realCtor.visitEnd();

    var allFieldNames = new TOrderedSet<String>();
    var staticFieldValues = new TOrderedMap<String, Object>();

    var fieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        var bodyCode = m.compiledBodyCode;
        if (bodyCode == null) {
          continue;
        }
        var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
        collectMethodFields(
            bodyCode, m.capturedExternals, prefix, allFieldNames, fieldDescriptors,
            staticFieldValues, builtinDynamics, staticFunctionDefs);
      }
    }

    for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
      var method = data.methods.get((int) methodIdx);
      var bodyCode = method.compiledBodyCode;
      if (bodyCode == null) {

        continue;
      }
      var prefix = methodFieldPrefix(methodIdx);
      for (var builtinId : EvalRootEmitter.referencedBuiltinIds(bodyCode)) {
        var name = Prefixes.BUILTIN_PREFIX + builtinId;
        if (allFieldNames.add(name)) {
          var v = builtinDynamics.get(name);
          if (v == null) {
            throw new AssertionError(
                "SourceObjectEmitter: no builtin registered for " + name);
          }
          fieldDescriptors.put(name, JavaDataDescriptor.fromClass(v.externalHostClass()));
          staticFieldValues.put(name, v.externalHostValue());
        }
      }
      for (int i = 0; i < method.capturedExternals.size(); i++) {
        var entry = method.capturedExternals.get(i);
        var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
        if (allFieldNames.add(name)) {
          fieldDescriptors.put(
              name, JavaDataDescriptor.fromClass(entry.external().externalHostClass()));
          staticFieldValues.put(name, entry.external().externalHostValue());
        }
      }
      for (var defId : EvalRootEmitter.referencedDefIds(bodyCode)) {
        if (staticFunctionDefs.contains(defId)) {
          continue;
        }
        var name = ImportInstall.bindingIdDynamicKey(defId);
        if (allFieldNames.add(name)) {
          fieldDescriptors.put(name, JvmConstants.DESC_OBJECT);
        }
      }
    }
    for (var name : allFieldNames) {
      var desc = fieldDescriptors.getOrDefault(name, JvmConstants.DESC_OBJECT);
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              name,
              desc.value(),
              null,
              null)
          .visitEnd();
    }

    if (allFieldNames.add(SOURCE_OBJECT_DATA_FIELD)) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              SOURCE_OBJECT_DATA_FIELD,
              JvmConstants.DESC_OBJECT.value(),
              null,
              null)
          .visitEnd();
    }
    staticFieldValues.put(SOURCE_OBJECT_DATA_FIELD, data);

    cw.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            EvalRootEmitter.EXPR_CONTEXT_FIELD,
            EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
            null,
            null)
        .visitEnd();

    for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
      var method = data.methods.get((int) methodIdx);
      var bodyCode = method.compiledBodyCode;
      if (bodyCode == null) {
        continue;
      }
      emitMethodMethod(cw, internalName, methodIdx, method, data.dataField, bodyCode, staticFunctionDefs);
    }

    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        var bodyCode = m.compiledBodyCode;
        if (bodyCode == null) {
          continue;
        }
        emitInterfaceMethodMethod(
            cw, internalName, implIdx, mIdx, impl.interface_, m, data.dataField, bodyCode, staticFunctionDefs);
      }
    }

    var emittedHostSlots = emitHostInterfaceAdapters(cw, internalName, data);
    emitDelegateAspectForwarders(cw, internalName, emittedHostSlots, claimedKinds);

    var sourceKeyMap = new TOrderedMap<String, String>();
    sourceKeyMap.put(
        new ConstKey.Str(data.dataField).toCanonical(),
        "F:" + DATA_INSTANCE_FIELD);
    for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
      var method = data.methods.get((int) methodIdx);
      if (method.compiledBodyCode == null) {
        continue;
      }
      sourceKeyMap.put(
          new ConstKey.Str(method.fieldName).toCanonical(),
          "M:" + methodMethodName(methodIdx));
    }
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        if (m.compiledBodyCode == null) {
          continue;
        }
        var ifaceMethod = impl.interface_.methods.get(m.interfaceMethodIndex);
        sourceKeyMap.put(
            new ConstKey.Str(ifaceMethod.fieldName).toCanonical(),
            "M:" + interfaceMethodMethodName(implIdx, mIdx));
      }
    }
    staticFieldValues.put(SOURCE_KEY_MAP_FIELD, sourceKeyMap);

    cw.visitEnd();
    return new EmittedSourceObject(binaryName, internalNameStr, cw.toByteArray(), staticFieldValues);
  }

  private static void emitMethodMethod(
      ClassWriter cw,
      JavaInternalName internalName,
      long methodIdx,
      SourceDataMethodData method,
      String dataField,
      CodeDynamic bodyCode,
      TSet<BindingId.Def> staticFunctionDefs) {
    var methodName = methodMethodName(methodIdx);
    var prefix = methodFieldPrefix(methodIdx);
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            methodName,
            MDESC_METHOD_METHOD.value(),
            null,
            null);

    var target = new TargetDynamic(new Counter());
    target.registerStaticFunctionDefs(staticFunctionDefs);
    var ctx = new EmitCtx(internalName.value(), 1, 3, prefix);
    var selfId = method.innerBindingId;
    if (selfId == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emit: method '" + method.name + "' has no innerBindingId (finish ordering violation)");
    }

    var seq = new JavaBytecodeSequence();
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
    var dataFieldCanonical = new ConstKey.Str(dataField).toCanonical();
    if (!(selfId instanceof BindingId.Local selfLocal)) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emit: selfId must be Local, got " + selfId);
    }
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.literalString(loc, dataFieldCanonical));
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "fieldGet", MDESC_FIELD_GET));
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, selfLocal.key())));

    var argId = method.compiledArgBindingId;
    if (argId != null) {
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 2));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, argId.key())));
    }

    for (int i = 0; i < method.capturedExternals.size(); i++) {
      var entry = method.capturedExternals.get(i);
      var fieldName = prefix + Prefixes.EXTERNAL_PREFIX + i;
      var fieldDesc = JavaDataDescriptor.fromClass(entry.external().externalHostClass());
      seq.add(JavaBytecodeUtils.accessStaticField(loc, internalName, fieldName, fieldDesc));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }
    target.emit(seq, ctx, bodyCode);
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    var reservedSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    for (int i = 0; i < 3; i++) {
      reservedSlots.add(new JavaBytecodeSequence.InitialSlot(
          new compilerxyz.eval.dynamic.JavaBytecodeBindingKey()));
    }
    JavaBytecodeUtils.renderMethodTyped(seq, mv, reservedSlots);
  }

  private static void collectMethodFields(
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> capturedExternals,
      String prefix,
      TOrderedSet<String> allFieldNames,
      TOrderedMap<String, JavaDataDescriptor> fieldDescriptors,
      TOrderedMap<String, Object> staticFieldValues,
      TOrderedMap<String, compilerxyz.eval.External> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    for (var builtinId : EvalRootEmitter.referencedBuiltinIds(bodyCode)) {
      var name = Prefixes.BUILTIN_PREFIX + builtinId;
      if (allFieldNames.add(name)) {
        var v = builtinDynamics.get(name);
        if (v == null) {
          throw new AssertionError(
              "SourceObjectEmitter: no builtin registered for " + name);
        }
        fieldDescriptors.put(name, JavaDataDescriptor.fromClass(v.externalHostClass()));
        staticFieldValues.put(name, v.externalHostValue());
      }
    }
    for (int i = 0; i < capturedExternals.size(); i++) {
      var entry = capturedExternals.get(i);
      var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
      if (allFieldNames.add(name)) {
        fieldDescriptors.put(
            name, JavaDataDescriptor.fromClass(entry.external().externalHostClass()));
        staticFieldValues.put(name, entry.external().externalHostValue());
      }
    }
    for (var defId : EvalRootEmitter.referencedDefIds(bodyCode)) {
      if (staticFunctionDefs.contains(defId)) {
        continue;
      }
      var name = ImportInstall.bindingIdDynamicKey(defId);
      if (allFieldNames.add(name)) {
        fieldDescriptors.put(name, JvmConstants.DESC_OBJECT);
      }
    }
  }

  private static void emitInterfaceMethodMethod(
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
      compilerxyz.eval.value.source_interface.SourceInterfaceData iface,
      SourceObjectInterfaceMethodImplData m,
      String dataField,
      CodeDynamic bodyCode,
      TSet<BindingId.Def> staticFunctionDefs) {
    var methodName = interfaceMethodMethodName(implIdx, mIdx);
    var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            methodName,
            MDESC_METHOD_METHOD.value(),
            null,
            null);
    var target = new TargetDynamic(new Counter());
    target.registerStaticFunctionDefs(staticFunctionDefs);
    var ctx = new EmitCtx(internalName.value(), 1, 3, prefix);
    var selfIdRaw = m.innerBindingId;
    if (selfIdRaw == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no innerBindingId");
    }
    if (!(selfIdRaw instanceof BindingId.Local selfId)) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: selfId must be Local, got " + selfIdRaw);
    }
    var argParamIds = m.compiledArgParamIds;
    if (argParamIds == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
    }
    var seq = new JavaBytecodeSequence();
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
    var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);
    var isArgMethod = m.compiledIsArgMethod;
    if (isArgMethod) {

      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 2));
      seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(INTNAME_OBJECT_ARRAY_ELEM)));
      seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AALOAD));
      var argRecordType = ifaceMethod.argRecordType;
      if (argRecordType == null) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitInterfaceMethodMethod: TYPE-kind method missing argRecordType");
      }
      if (!(argRecordType instanceof compilerxyz.gen.AspectConsumeDynamic argRecordConsume)) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitInterfaceMethodMethod: argRecordType does not implement AspectConsumeDynamic");
      }
      seq.add(JavaBytecodeUtils.cast(loc, argRecordConsume.consumeDynamicJvmClass()));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, selfId.key())));
    } else {

      var dataFieldCanonical = new ConstKey.Str(dataField).toCanonical();
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 2));
      seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(INTNAME_OBJECT_ARRAY_ELEM)));
      if (!argParamIds.isEmpty()) {
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      }
      seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AALOAD));
      seq.add(JavaBytecodeUtils.literalString(loc, dataFieldCanonical));
      seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "fieldGet", MDESC_FIELD_GET));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, selfId.key())));
      var argParamTypes = m.compiledArgParamTypes;
      if (argParamTypes == null || argParamTypes.size() != argParamIds.size()) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
      }
      for (int i = 0; i < argParamIds.size(); i++) {
        var argId = argParamIds.get(i);
        var argType = argParamTypes.get(i);
        if (i < argParamIds.size() - 1) {
          seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        }
        seq.add(JavaBytecodeUtils.literalIntShortByte(loc, i + 1));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AALOAD));
        if (!(argType instanceof compilerxyz.gen.AspectConsumeDynamic argConsume)) {
          throw new IllegalStateException(
              "SourceObjectEmitter.emitInterfaceMethodMethod: argParamType "
                  + argType.typeDesc()
                  + " does not implement AspectConsumeDynamic");
        }
        var argDesc = argConsume.consumeDynamicJvmClass();
        if (argDesc.value().startsWith("L")
            && !argDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
          seq.add(JavaBytecodeUtils.cast(loc, argDesc));
        }
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, argId.key())));
      }
    }

    for (int i = 0; i < m.capturedExternals.size(); i++) {
      var entry = m.capturedExternals.get(i);
      var fieldName = prefix + Prefixes.EXTERNAL_PREFIX + i;
      var fieldDesc = JavaDataDescriptor.fromClass(entry.external().externalHostClass());
      seq.add(JavaBytecodeUtils.accessStaticField(loc, internalName, fieldName, fieldDesc));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }
    target.emit(seq, ctx, bodyCode);
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    var reservedSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    for (int i = 0; i < 3; i++) {
      reservedSlots.add(new JavaBytecodeSequence.InitialSlot(
          new compilerxyz.eval.dynamic.JavaBytecodeBindingKey()));
    }
    JavaBytecodeUtils.renderMethodTyped(seq, mv, reservedSlots);
  }

  private record HostMethodSpec(String hostName, JavaMethodDescriptor hostDescriptor, ReturnKind returnKind) {}

  private enum ReturnKind {

    OBJECT_CAST,

    BOOLEAN_UNBOX,

    ERESULT_FROM_CV_CELL,

    ERESULT_CODE_FROM_CV_CELL,

    ERESULT_FROM_CV_CELL_RETAGGED
  }

  private static @org.jspecify.annotations.Nullable HostMethodSpec hostMethodSpec(
      BuiltinInterfaceKind kind, String sourceName) {
    return switch (kind) {
      case VALUE -> switch (sourceName) {
        case "type" -> new HostMethodSpec(
            "valueTypeOf",
            JavaMethodDescriptor.fromParts(DESC_TYPE),
            ReturnKind.OBJECT_CAST);
        default -> null;
      };
      case TYPE -> switch (sourceName) {
        case "can_cast_from" -> new HostMethodSpec(
            "typeCanCastFrom",
            JavaMethodDescriptor.fromParts(
                JvmConstants.DESC_BOOL, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TYPE),
            ReturnKind.BOOLEAN_UNBOX);
        case "cast_from" -> new HostMethodSpec(
            "typeCastFrom",
            JavaMethodDescriptor.fromParts(
                DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
            ReturnKind.ERESULT_FROM_CV_CELL_RETAGGED);
        case "desc" -> new HostMethodSpec(
            "typeDesc",
            JavaMethodDescriptor.fromParts(JvmConstants.DESC_STRING),
            ReturnKind.OBJECT_CAST);
        case "aspect" -> new HostMethodSpec(
            "typeAspect",
            JavaMethodDescriptor.fromParts(DESC_OPT, DESC_EXPR_CONTEXT, DESC_UNIQUE),
            ReturnKind.OBJECT_CAST);
        default -> null;
      };
      case ASPECT_FUNCTION -> switch (sourceName) {
        case "call" -> new HostMethodSpec(
            "call",
            JavaMethodDescriptor.fromParts(
                DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_CODE_VALUE),
            ReturnKind.OBJECT_CAST);
        default -> null;
      };
      case ASPECT_FIELD -> switch (sourceName) {
        case "field" -> new HostMethodSpec(
            "field",
            JavaMethodDescriptor.fromParts(
                DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_CODE_VALUE),
            ReturnKind.OBJECT_CAST);
        case "field_type_opt" -> new HostMethodSpec(
            "fieldTypeOpt",
            JavaMethodDescriptor.fromParts(DESC_OPT, DESC_EXPR_CONTEXT, DESC_CONST_KEY),
            ReturnKind.OBJECT_CAST);
        default -> null;
      };
      case ASPECT_DROP_DYNAMIC -> switch (sourceName) {
        case "drop" -> new HostMethodSpec(
            "drop",
            JavaMethodDescriptor.fromParts(
                DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
            ReturnKind.ERESULT_CODE_FROM_CV_CELL);
        default -> null;
      };
      case BINDING -> switch (sourceName) {
        case "bindingUse_" -> new HostMethodSpec(
            "use",
            JavaMethodDescriptor.fromParts(
                DESC_CODE_VALUE, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE),
            ReturnKind.OBJECT_CAST);
        default -> null;
      };
      case TARGET -> targetHostSpec(sourceName);
      case CODE_DYN -> null;
    };
  }

  private static @org.jspecify.annotations.Nullable HostMethodSpec targetHostSpec(String sourceName) {
    return switch (sourceName) {
      case "bind" -> new HostMethodSpec(
          "bind",
          JavaMethodDescriptor.fromParts(
              DESC_PAIR, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TARGET_BIND_SUB),
          ReturnKind.OBJECT_CAST);
      case "bind_sub_record" -> new HostMethodSpec(
          "bind_sub_record",
          JavaMethodDescriptor.fromParts(
              DESC_TARGET_BIND_SUB, DESC_QEXPR_ID, DESC_CODE, DESC_LIST),
          ReturnKind.OBJECT_CAST);
      case "bind_sub_tuple" -> new HostMethodSpec(
          "bind_sub_tuple",
          JavaMethodDescriptor.fromParts(
              DESC_TARGET_BIND_SUB, DESC_QEXPR_ID, DESC_CODE, DESC_LIST),
          ReturnKind.OBJECT_CAST);
      case "seq" -> new HostMethodSpec(
          "seq",
          JavaMethodDescriptor.fromParts(DESC_ERESULT, DESC_QEXPR_ID, DESC_LIST),
          ReturnKind.OBJECT_CAST);
      case "code_to_bind_sub" -> new HostMethodSpec(
          "code_to_bind_sub",
          JavaMethodDescriptor.fromParts(DESC_ERESULT, DESC_QEXPR_ID, DESC_CODE, DESC_TYPE),
          ReturnKind.OBJECT_CAST);
      case "if_" -> new HostMethodSpec(
          "if_",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_LIST, DESC_OPT),
          ReturnKind.OBJECT_CAST);
      case "match_" -> new HostMethodSpec(
          "match",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT,
              DESC_EXPR_CONTEXT,
              DESC_QEXPR_ID,
              DESC_CODE_VALUE,
              DESC_CONST_KEY,
              DESC_LIST,
              DESC_OPT),
          ReturnKind.OBJECT_CAST);
      case "maybe_bind_sub" -> new HostMethodSpec(
          "maybe_bind_sub",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          ReturnKind.OBJECT_CAST);
      case "stage" -> new HostMethodSpec(
          "stage",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_EXPR, DESC_LIST, DESC_LIST),
          ReturnKind.OBJECT_CAST);
      case "cast_exception" -> new HostMethodSpec(
          "cast_exception",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT,
              DESC_EXPR_CONTEXT,
              DESC_QEXPR_ID,
              DESC_CODE,
              DESC_BINDING_ID,
              DESC_CODE_VALUE),
          ReturnKind.OBJECT_CAST);
      case "drop" -> new HostMethodSpec(
          "drop",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          ReturnKind.OBJECT_CAST);
      case "try_finally" -> new HostMethodSpec(
          "try_finally",
          JavaMethodDescriptor.fromParts(DESC_ERESULT, DESC_QEXPR_ID, DESC_CODE, DESC_CODE),
          ReturnKind.OBJECT_CAST);
      default -> null;
    };
  }

  private static int parameterSlotCount(String descriptor) {
    var n = 0;
    var i = 1;
    while (i < descriptor.length() && descriptor.charAt(i) != ')') {
      var c = descriptor.charAt(i);
      if (c == 'L') {
        n++;
        i = descriptor.indexOf(';', i) + 1;
      } else if (c == '[') {
        i++;
      } else {
        n++;
        i++;
      }
    }
    return n;
  }

  private static TOrderedSet<String> emitHostInterfaceAdapters(
      ClassWriter cw, JavaInternalName internalName, TypeSourceObjectDynamic data) {
    var emitted = new TOrderedSet<String>();
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      var iface = impl.interface_;
      var kind = iface.builtinKind;
      if (kind == null) {
        continue;
      }
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        if (m.compiledBodyCode == null) {
          continue;
        }
        var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);
        var spec = hostMethodSpec(kind, ifaceMethod.name);
        if (spec == null) {
          continue;
        }
        var dedupeKey = spec.hostName() + spec.hostDescriptor().value();
        if (!emitted.add(dedupeKey)) {
          continue;
        }
        emitHostAdapter(cw, internalName, implIdx, mIdx, m, ifaceMethod, kind, spec);
      }
    }
    return emitted;
  }

  private record InlineHelperSpec(
      String hostName,
      JavaMethodDescriptor hostDescriptor,
      String helperName,
      JavaMethodDescriptor helperDescriptor,
      boolean passData,
      @org.jspecify.annotations.Nullable BuiltinInterfaceKind requiredKind,
      @org.jspecify.annotations.Nullable JavaInternalName jvmInterface) {}

  private static final TList<InlineHelperSpec> INLINE_HELPER_METHODS = TList.of(
      new InlineHelperSpec(
          "bind",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectBind",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true,
          BuiltinInterfaceKind.BINDING,
          INTNAME_ASPECT_BIND),
      new InlineHelperSpec(
          "bindDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectBindDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true,
          BuiltinInterfaceKind.BINDING,
          INTNAME_ASPECT_BIND_DYNAMIC),
      new InlineHelperSpec(
          "toDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectToDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true,
          BuiltinInterfaceKind.BINDING,
          INTNAME_ASPECT_CONSUME_DYNAMIC),
      new InlineHelperSpec(
          "field",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_CODE_VALUE),
          "sourceObjectField",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT,
              JvmConstants.DESC_OBJECT,
              DESC_EXPR_CONTEXT,
              DESC_QEXPR_ID,
              DESC_CODE_VALUE,
              DESC_CODE_VALUE),
          true,
          BuiltinInterfaceKind.ASPECT_FIELD,
          INTNAME_ASPECT_FIELD),
      new InlineHelperSpec(
          "fieldKeys",
          JavaMethodDescriptor.fromParts(DESC_TLIST, DESC_EXPR_CONTEXT),
          "sourceObjectFieldKeys",
          JavaMethodDescriptor.fromParts(DESC_TLIST, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT),
          true,
          BuiltinInterfaceKind.ASPECT_FIELD,
          INTNAME_ASPECT_FIELD),
      new InlineHelperSpec(
          "fieldTypeOpt",
          JavaMethodDescriptor.fromParts(DESC_OPT, DESC_EXPR_CONTEXT, DESC_CONST_KEY),
          "sourceObjectFieldTypeOpt",
          JavaMethodDescriptor.fromParts(
              DESC_OPT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_CONST_KEY),
          true,
          BuiltinInterfaceKind.ASPECT_FIELD,
          INTNAME_ASPECT_FIELD),
      new InlineHelperSpec(
          "initialBind",
          JavaMethodDescriptor.fromParts(DESC_BINDING, DESC_BINDING_ID),
          "sourceObjectInitialBind",
          JavaMethodDescriptor.fromParts(DESC_BINDING, JvmConstants.DESC_OBJECT, DESC_BINDING_ID),
          true,
          BuiltinInterfaceKind.BINDING,
          INTNAME_ASPECT_INITIAL_BIND),
      new InlineHelperSpec(
          "lift",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectLift",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          false,
          BuiltinInterfaceKind.TYPE,
          INTNAME_ASPECT_LIFT),
      new InlineHelperSpec(
          "lift",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, JvmConstants.DESC_OBJECT),
          "sourceObjectLiftRuntime",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT,
              JvmConstants.DESC_OBJECT,
              DESC_EXPR_CONTEXT,
              DESC_QEXPR_ID,
              JvmConstants.DESC_OBJECT),
          true,
          BuiltinInterfaceKind.TYPE,
          INTNAME_ASPECT_LIFT),
      new InlineHelperSpec(
          "typeCanCastTo",
          JavaMethodDescriptor.fromParts(
              JvmConstants.DESC_BOOL, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TYPE),
          "sourceObjectCanCastTo",
          JavaMethodDescriptor.fromParts(
              JvmConstants.DESC_BOOL, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TYPE),
          false,
          BuiltinInterfaceKind.TYPE,
          null),
      new InlineHelperSpec(
          "typeCastTo",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_TYPE),
          "sourceObjectCastTo",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_TYPE),
          false,
          BuiltinInterfaceKind.TYPE,
          null),
      new InlineHelperSpec(
          "consumeDynamicJvmClass",
          JavaMethodDescriptor.fromParts(
              JavaDataDescriptor.fromClass(JavaDataDescriptor.class)),
          "sourceObjectConsumeDynamicJvmClass",
          JavaMethodDescriptor.fromParts(
              JavaDataDescriptor.fromClass(JavaDataDescriptor.class),
              JvmConstants.DESC_OBJECT),
          true,
          BuiltinInterfaceKind.VALUE,
          INTNAME_ASPECT_CONSUME_DYNAMIC));

  private static void emitDelegateAspectForwarders(
      ClassWriter cw,
      JavaInternalName internalName,
      TOrderedSet<String> emittedHostSlots,
      java.util.Set<BuiltinInterfaceKind> claimedKinds) {
    for (var spec : INLINE_HELPER_METHODS) {
      if (spec.requiredKind() != null && !claimedKinds.contains(spec.requiredKind())) {
        continue;
      }
      var dedupeKey = spec.hostName() + spec.hostDescriptor().value();
      if (emittedHostSlots.contains(dedupeKey)) {
        continue;
      }
      var mv =
          cw.visitMethod(
              Opcodes.ACC_PUBLIC, spec.hostName(), spec.hostDescriptor().value(), null, null);
      var seq = new JavaBytecodeSequence();
      if (spec.passData()) {
        seq.add(JavaBytecodeUtils.accessStaticField(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, internalName, SOURCE_OBJECT_DATA_FIELD, JvmConstants.DESC_OBJECT));
      }
      int slot = 1;
      var descriptor = spec.hostDescriptor().value();
      int i = 1;
      while (i < descriptor.length() && descriptor.charAt(i) != ')') {
        var c = descriptor.charAt(i);
        if (c == 'L') {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, slot));
          slot++;
          i = descriptor.indexOf(';', i) + 1;
        } else if (c == '[') {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, slot));
          slot++;
          i++;
          while (i < descriptor.length() && descriptor.charAt(i) == '[') i++;
          if (i < descriptor.length() && descriptor.charAt(i) == 'L') {
            i = descriptor.indexOf(';', i) + 1;
          } else {
            i++;
          }
        } else {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, slot));
          slot++;
          i++;
        }
      }
      seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, INTNAME_DYN_RUNTIME, spec.helperName(), spec.helperDescriptor()));
      var closeParen = descriptor.indexOf(')');
      var retChar = descriptor.charAt(closeParen + 1);
      if (retChar == 'Z' || retChar == 'B' || retChar == 'C' || retChar == 'S' || retChar == 'I') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.IRETURN));
      } else if (retChar == 'J') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.LRETURN));
      } else if (retChar == 'F') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.FRETURN));
      } else if (retChar == 'D') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.DRETURN));
      } else if (retChar == 'V') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
      } else {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
      }
      JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
    }
  }

  private static void emitHostAdapter(
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
      SourceObjectInterfaceMethodImplData m,
      SourceInterfaceMethodData ifaceMethod,
      BuiltinInterfaceKind kind,
      HostMethodSpec spec) {
    var mv = cw.visitMethod(
        Opcodes.ACC_PUBLIC, spec.hostName(), spec.hostDescriptor().value(), null, null);
    var hostDescStr = spec.hostDescriptor().value();
    var paramSlotCount = parameterSlotCount(hostDescStr);

    boolean firstParamIsExprContext =
        hostDescStr.startsWith("(" + DESC_EXPR_CONTEXT.value());

    var seq = new JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));

    if (firstParamIsExprContext) {
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 1));
    } else {
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ACONST_NULL));
    }

    emitArgPacking(seq, kind, m, ifaceMethod, firstParamIsExprContext, paramSlotCount);

    seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, 
        internalName, interfaceMethodMethodName(implIdx, mIdx), MDESC_METHOD_METHOD));

    switch (spec.returnKind()) {
      case BOOLEAN_UNBOX -> {
        seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_BOXED_BOOL)));
        seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_BOXED_BOOL, "booleanValue", MDESC_BOOLEAN_VALUE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.IRETURN));
      }
      case OBJECT_CAST -> {
        var retInternal = returnTypeInternalName(hostDescStr);
        if (retInternal != null) {
          seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JavaDataDescriptor.fromInternalName(retInternal)));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
      }
      case ERESULT_FROM_CV_CELL -> {
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, INTNAME_DYN_RUNTIME, "eresultFromCvCell", MDESC_ERESULT_FROM_ONE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
      }
      case ERESULT_CODE_FROM_CV_CELL -> {
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, INTNAME_DYN_RUNTIME, "eresultCodeFromCvCell", MDESC_ERESULT_FROM_ONE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
      }
      case ERESULT_FROM_CV_CELL_RETAGGED -> {
        seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, INTNAME_DYN_RUNTIME, "eresultFromCvCellRetagged", MDESC_ERESULT_FROM_TWO));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
      }
    }
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
  }

  private static @org.jspecify.annotations.Nullable JavaInternalName returnTypeInternalName(
      String descriptor) {
    var closeParen = descriptor.indexOf(')');
    if (closeParen < 0 || closeParen + 1 >= descriptor.length()) {
      return null;
    }
    var ret = descriptor.charAt(closeParen + 1);
    if (ret != 'L') {
      return null;
    }
    return JavaInternalName.of(descriptor.substring(closeParen + 2, descriptor.length() - 1));
  }

  private static void emitArgPacking(
      JavaBytecodeSequence seq,
      BuiltinInterfaceKind kind,
      SourceObjectInterfaceMethodImplData m,
      SourceInterfaceMethodData ifaceMethod,
      boolean firstParamIsExprContext,
      int paramSlotCount) {

    if (m.compiledIsArgMethod) {
      var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
      var argRecordType = ifaceMethod.argRecordType;
      if (!(argRecordType instanceof compilerxyz.eval.value.record.Record argRecord)) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitArgPacking: isArgMethod true but argRecordType is not a Record: "
                + (argRecordType == null ? "null" : argRecordType.typeDesc()));
      }
      var recordFieldTypes = argRecord.recordFieldTypes();
      var slotSort = compilerxyz.eval.dynamic.CompositeSlotSort.sort(
          recordFieldTypes, compilerxyz.eval.ConstKey.CANONICAL_ORDER);
      var sortedTypes = slotSort.sortedTypes();
      var compositeClass = compilerxyz.eval.dynamic.CompositeClassEmitter.getOrEmit(sortedTypes);
      var compositeInternal =
          compilerxyz.eval.dynamic.JavaInternalName.of(compositeClass.getName().replace('.', '/'));
      var ctorArgDescs = new compilerxyz.util.TList<JavaDataDescriptor>(sortedTypes.size());
      for (var t : sortedTypes) {
        ctorArgDescs.add(compilerxyz.eval.dynamic.CompositeClassEmitter.slotDescriptor(t));
      }
      var ctorDesc =
          compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(ctorArgDescs);

      var dataFields = ifaceMethod.dataFieldNames;
      var sourceName = ifaceMethod.name;
      boolean isCastFrom = "cast_from".equals(sourceName);
      boolean isCanCastFrom = "can_cast_from".equals(sourceName);
      var slotOfKey = slotSort.slotOfKey();
      var slotValueSources = new java.util.HashMap<Integer, Runnable>();
      for (int i = 0; i < dataFields.size(); i++) {
        var fieldName = dataFields.get(i);
        var ck = new compilerxyz.eval.ConstKey.Str(fieldName);
        var slotIdxBoxed = slotOfKey.get(ck);
        if (slotIdxBoxed == null) {
          throw new IllegalStateException(
              "SourceObjectEmitter.emitArgPacking: dataField '" + fieldName
                  + "' not in argRecord fields");
        }
        int slotIdx = slotIdxBoxed;
        switch (fieldName) {
          case "xx" -> {
            if (firstParamIsExprContext) {
              slotValueSources.put(slotIdx, () -> {
                seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 1));
                seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "wrapExprContextPush", MDESC_WRAP_ONE_OBJ));
              });
            } else {
              slotValueSources.put(slotIdx, () ->
                  seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
            }
          }
          case "source_type" -> {
            if (isCanCastFrom && paramSlotCount >= 3) {
              slotValueSources.put(slotIdx, () -> {
                seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 3));
                seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "wrapTypeRefPush", MDESC_WRAP_ONE_OBJ));
              });
            } else if (isCastFrom && paramSlotCount >= 3) {
              slotValueSources.put(slotIdx, () -> {
                seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 1));
                seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 3));
                seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "deriveSourceTypeFromCv", MDESC_WRAP_TWO_OBJ));
              });
            } else {
              slotValueSources.put(slotIdx, () ->
                  seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
            }
          }
          case "cv" -> {
            if (isCastFrom && paramSlotCount >= 3) {
              slotValueSources.put(slotIdx, () -> {
                seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 3));
                seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "wrapCodeValueExternCell", MDESC_WRAP_ONE_OBJ));
              });
            } else {
              slotValueSources.put(slotIdx, () ->
                  seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
            }
          }
          default -> slotValueSources.put(slotIdx, () ->
              seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
        }
      }

      seq.add(JavaBytecodeUtils.newInstance(loc, compositeInternal));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      for (int slotIdx = 0; slotIdx < sortedTypes.size(); slotIdx++) {
        var source = slotValueSources.get(slotIdx);
        if (source == null) {
          throw new IllegalStateException(
              "SourceObjectEmitter.emitArgPacking: no value source for slot " + slotIdx);
        }
        source.run();
        var slotDesc = ctorArgDescs.get(slotIdx);
        if (slotDesc.value().startsWith("L")
            && !slotDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
          seq.add(JavaBytecodeUtils.cast(loc, slotDesc));
        }
      }
      seq.add(JavaBytecodeUtils.invokeSpecial(loc, compositeInternal, "<init>", ctorDesc));

      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_1));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP_X1));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.SWAP));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.SWAP));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AASTORE));
    } else if (kind == BuiltinInterfaceKind.BINDING || kind == BuiltinInterfaceKind.TARGET) {

      var dataFields = ifaceMethod.dataFieldNames;
      var arrSize = 1 + dataFields.size();
      pushIntConst(seq, arrSize);
      seq.add(JavaBytecodeUtils.wrapObj(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.AASTORE));

      for (int i = 0; i < dataFields.size(); i++) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.DUP));
        pushIntConst(seq, i + 1);
        var hostSlot = 1 + i;
        if (hostSlot <= paramSlotCount) {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, hostSlot));
        } else {
          seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ACONST_NULL));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.AASTORE));
      }
    } else if ((kind == BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC
            || kind == BuiltinInterfaceKind.ASPECT_FIELD
            || kind == BuiltinInterfaceKind.ASPECT_FUNCTION)
        && ifaceMethod.argRecordType instanceof compilerxyz.eval.value.record.Record argRecord) {
      var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
      pushIntConst(seq, 2);
      seq.add(JavaBytecodeUtils.wrapObj(loc, new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AASTORE));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_1));

      var recordFieldTypes = argRecord.recordFieldTypes();
      var slotSort = compilerxyz.eval.dynamic.CompositeSlotSort.sort(
          recordFieldTypes, compilerxyz.eval.ConstKey.CANONICAL_ORDER);
      var sortedTypes = slotSort.sortedTypes();
      var compositeClass = compilerxyz.eval.dynamic.CompositeClassEmitter.getOrEmit(sortedTypes);
      var compositeInternal =
          compilerxyz.eval.dynamic.JavaInternalName.of(compositeClass.getName().replace('.', '/'));
      var ctorArgDescs = new compilerxyz.util.TList<JavaDataDescriptor>(sortedTypes.size());
      for (var t : sortedTypes) {
        ctorArgDescs.add(compilerxyz.eval.dynamic.CompositeClassEmitter.slotDescriptor(t));
      }
      var ctorDesc =
          compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(ctorArgDescs);

      seq.add(JavaBytecodeUtils.newInstance(loc, compositeInternal));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));

      var slotOfKey = slotSort.slotOfKey();
      var slotValueSources = new java.util.HashMap<Integer, Runnable>();
      int nextCvSlot = 3;
      for (var fieldName : ifaceMethod.dataFieldNames) {
        int slotIdx = slotOfKey.get(new compilerxyz.eval.ConstKey.Str(fieldName));
        int finalNextCvSlot = nextCvSlot;
        if ("xx".equals(fieldName)) {
          if (firstParamIsExprContext) {
            slotValueSources.put(slotIdx, () -> {
              seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 1));
              seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "wrapExprContextPush", MDESC_WRAP_ONE_OBJ));
            });
          } else {
            slotValueSources.put(slotIdx, () ->
                seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
          }
        } else {
          if (nextCvSlot <= paramSlotCount) {
            slotValueSources.put(slotIdx, () -> {
              seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, finalNextCvSlot));
              seq.add(JavaBytecodeUtils.callStaticMethod(loc, INTNAME_DYN_RUNTIME, "wrapCodeValueExternCell", MDESC_WRAP_ONE_OBJ));
            });
            nextCvSlot++;
          } else {
            slotValueSources.put(slotIdx, () ->
                seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL)));
          }
        }
      }
      for (int slotIdx = 0; slotIdx < sortedTypes.size(); slotIdx++) {
        var source = slotValueSources.get(slotIdx);
        if (source == null) {
          throw new IllegalStateException(
              "SourceObjectEmitter.emitUserInterfaceStubs: no value source for slot " + slotIdx
                  + " of composite type " + argRecord.typeDesc());
        }
        source.run();
      }
      seq.add(JavaBytecodeUtils.invokeSpecial(loc, compositeInternal, "<init>", ctorDesc));

      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AASTORE));
    } else {

      var argParamIds = m.compiledArgParamIds;
      var hasArg = argParamIds != null && !argParamIds.isEmpty();
      var arrSize = hasArg ? 2 : 1;
      pushIntConst(seq, arrSize);
      seq.add(JavaBytecodeUtils.wrapObj(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.AASTORE));
      if (hasArg) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.DUP));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_1));

        if (paramSlotCount >= 1) {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, paramSlotCount));
        } else {
          seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ACONST_NULL));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.AASTORE));
      }
    }
  }

  private static void pushIntConst(JavaBytecodeSequence seq, int value) {
    if (value >= 0 && value <= 5) {
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_0 + value));
    } else {
      seq.add(JavaBytecodeUtils.wrapObj(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, value)));
    }
  }

}
