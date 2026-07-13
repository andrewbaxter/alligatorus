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
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import org.jspecify.annotations.Nullable;

public final class SourceObjectEmitter {
  private SourceObjectEmitter() {}

    private static final JavaInternalName INTNAME_DYN_RUNTIME =
            JavaInternalName.of("compilerxyz/eval/dynamic/DynRuntime");
    private static final JavaInternalName INTNAME_DYN_UTIL =
            JavaInternalName.of("compilerxyz/eval/dynamic/DynUtil");
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
      JavaInternalName.of("compilerxyz/gen/Binding");
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
  private static final JavaMethodDescriptor MDESC_DERIVED_SOURCE_TYPE =
      JavaMethodDescriptor.fromParts(
          DESC_TYPE,
          DESC_EXPR_CONTEXT,
          DESC_CODE_VALUE);
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

  public static EmittedSourceObject emit(
      compilerxyz.eval.ExprContext xx,
      DefSetId defSet,
      long objectIndex,
      TypeSourceObjectDynamic data) {
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
    for (var impl : data.interfaceImpls) {
      var iface = impl.interface_;
      implementsInternalNames.add(iface.compiledBinaryName.replace('.', '/'));
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

    JavaDataDescriptor dataFieldDesc;
    if (data.underlyingType instanceof compilerxyz.gen.AspectConsumeDynamic underlyingConsume) {
      dataFieldDesc = underlyingConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      dataFieldDesc = JvmConstants.DESC_OBJECT;
    }
    cw.visitField(Opcodes.ACC_PUBLIC, DATA_INSTANCE_FIELD, dataFieldDesc.value(), null, null)
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

    var realCtorDesc =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(dataFieldDesc));
    var realCtor =
        cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", realCtorDesc.value(), null, null);
    realCtor.visitCode();
    var realCtorSeq = new JavaBytecodeSequence();
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    realCtorSeq.add(JavaBytecodeUtils.invokeSpecial(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT));
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    realCtorSeq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 1));
    realCtorSeq.add(JavaBytecodeUtils.setField(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, internalName, DATA_INSTANCE_FIELD, dataFieldDesc));
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
            staticFieldValues);
      }
    }

    for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
      var method = data.methods.get((int) methodIdx);
      var bodyCode = method.compiledBodyCode;
      if (bodyCode == null) {

        continue;
      }
      var prefix = methodFieldPrefix(methodIdx);
      for (int i = 0; i < method.capturedExternals.size(); i++) {
        var entry = method.capturedExternals.get(i);
        var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
        if (allFieldNames.add(name)) {
          fieldDescriptors.put(
              name, JavaDataDescriptor.fromClass(entry.external().externalHostClass()));
          staticFieldValues.put(name, entry.external().externalHostValue());
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
      emitMethodMethod(xx, cw, internalName, methodIdx, method, data.underlyingType, bodyCode);
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
            xx, cw, internalName, implIdx, mIdx, impl.interface_, m, data.underlyingType, bodyCode);
      }
    }

    var emittedHostSlots = emitHostInterfaceAdapters(xx, cw, internalName, data);

    cw.visitEnd();
    return new EmittedSourceObject(binaryName, internalNameStr, cw.toByteArray(), staticFieldValues);
  }

  private static void emitMethodMethod(
      compilerxyz.eval.ExprContext xx,
      ClassWriter cw,
      JavaInternalName internalName,
      long methodIdx,
      SourceDataMethodData method,
      compilerxyz.eval.@org.jspecify.annotations.Nullable Type underlyingType,
      CodeDynamic bodyCode) {
    var methodName = methodMethodName(methodIdx);
    var prefix = methodFieldPrefix(methodIdx);
    if (!(method.retType instanceof compilerxyz.gen.AspectConsumeDynamic retConsume)) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitMethodMethod: method '" + method.name
              + "' retType does not implement AspectConsumeDynamic");
    }
    var argSlots = method.compiledArgSlots;
    var argSlotIds = method.compiledArgSlotIds;
    if (argSlots == null || argSlotIds == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitMethodMethod: method '" + method.name
              + "' has no compiledArgSlots (finish ordering violation)");
    }
    var retDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
    var argDescs = new TList<JavaDataDescriptor>(argSlots.size());
    for (var s : argSlots) argDescs.add(s.jvmDesc());
    var descriptor = JavaMethodDescriptor.fromParts(retDesc, argDescs);
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            methodName,
            descriptor.value(),
            null,
            null);

    var target = new TargetDynamic(new Counter());
    int totalArgSlots = 0;
    for (var s : argSlots) totalArgSlots += slotWidth(s.jvmDesc());
    var ctx = new EmitCtx(internalName.value(), 1, 1 + totalArgSlots, prefix);
    var selfId = method.innerBindingId;
    if (selfId == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emit: method '" + method.name + "' has no innerBindingId (finish ordering violation)");
    }

    var seq = new JavaBytecodeSequence();
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
    if (!(selfId instanceof BindingId.Local selfLocal)) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emit: selfId must be Local, got " + selfId);
    }
    JavaDataDescriptor dataFieldDesc =
        underlyingType instanceof compilerxyz.gen.AspectConsumeDynamic underlyingConsume
            ? underlyingConsume.aspectConsumeDynamicJvmClass(xx)
            : JvmConstants.DESC_OBJECT;
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.accessField(loc, internalName, DATA_INSTANCE_FIELD, dataFieldDesc));
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, selfLocal.key())));

    var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    initialSlots.add(new JavaBytecodeSequence.InitialSlot(
        new compilerxyz.eval.dynamic.JavaBytecodeBindingKey()));
    for (int i = 0; i < argSlots.size(); i++) {
      initialSlots.add(new JavaBytecodeSequence.InitialSlot(
          argSlotIds.get(i).key(),
          slotWidth(argSlots.get(i).jvmDesc()) == 2));
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
    JavaBytecodeUtils.renderMethodTyped(seq, mv, initialSlots);
  }

  private static int slotWidth(JavaDataDescriptor d) {
    var v = d.value();
    return (v.equals("D") || v.equals("J")) ? 2 : 1;
  }

  private static void collectMethodFields(
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> capturedExternals,
      String prefix,
      TOrderedSet<String> allFieldNames,
      TOrderedMap<String, JavaDataDescriptor> fieldDescriptors,
      TOrderedMap<String, Object> staticFieldValues) {
    for (int i = 0; i < capturedExternals.size(); i++) {
      var entry = capturedExternals.get(i);
      var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
      if (allFieldNames.add(name)) {
        fieldDescriptors.put(
            name, JavaDataDescriptor.fromClass(entry.external().externalHostClass()));
        staticFieldValues.put(name, entry.external().externalHostValue());
      }
    }
  }

  private static void emitInterfaceMethodMethod(
      compilerxyz.eval.ExprContext xx,
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
      compilerxyz.eval.value.source_interface.InterfaceData iface,
      SourceObjectInterfaceMethodImplData m,
      compilerxyz.eval.@org.jspecify.annotations.Nullable Type underlyingType,
      CodeDynamic bodyCode) {
    var methodName = interfaceMethodMethodName(implIdx, mIdx);
    var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
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
    var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);

    var descriptor = interfaceMethodMethodDescriptor(xx, m, ifaceMethod);
    var jvmParamKeys = new TList<compilerxyz.eval.dynamic.JavaBytecodeBindingKey>();
    var jvmParamWides = new TList<Boolean>();
    var argParamTypes = m.compiledArgParamTypes;
    if (argParamTypes == null || argParamTypes.size() != argParamIds.size()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
    }
    for (int i = 0; i < argParamIds.size(); i++) {
      var argConsume = (compilerxyz.gen.AspectConsumeDynamic) argParamTypes.get(i);
      var argDesc = argConsume.aspectConsumeDynamicJvmClass(xx);
      jvmParamKeys.add(argParamIds.get(i).key());
      jvmParamWides.add(slotWidth(argDesc) == 2);
    }
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            methodName,
            descriptor.value(),
            null,
            null);
    var target = new TargetDynamic(new Counter());
    int totalParamSlots = 0;
    for (var wide : jvmParamWides) totalParamSlots += (wide ? 2 : 1);
    var ctx = new EmitCtx(internalName.value(), 1, 1 + totalParamSlots, prefix);

    var seq = new JavaBytecodeSequence();
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;

    var outerBindingId = m.outerBindingId;
    if (outerBindingId instanceof BindingId.Local outerLocal) {
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, outerLocal.key())));
    }

    JavaDataDescriptor innerDesc =
        underlyingType instanceof compilerxyz.gen.AspectConsumeDynamic underlyingConsume
            ? underlyingConsume.aspectConsumeDynamicJvmClass(xx)
            : JvmConstants.DESC_OBJECT;
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.accessField(loc, internalName, DATA_INSTANCE_FIELD, innerDesc));
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, selfId.key())));

    var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    initialSlots.add(new JavaBytecodeSequence.InitialSlot(
        new compilerxyz.eval.dynamic.JavaBytecodeBindingKey()));
    for (int i = 0; i < jvmParamKeys.size(); i++) {
      initialSlots.add(new JavaBytecodeSequence.InitialSlot(
          jvmParamKeys.get(i), jvmParamWides.get(i)));
    }

    for (int i = 0; i < m.capturedExternals.size(); i++) {
      var entry = m.capturedExternals.get(i);
      var fieldName = prefix + Prefixes.EXTERNAL_PREFIX + i;
      var fieldDesc = JavaDataDescriptor.fromClass(entry.external().externalHostClass());
      seq.add(JavaBytecodeUtils.accessStaticField(loc, internalName, fieldName, fieldDesc));
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }
    target.emit(seq, ctx, bodyCode);
    emitReturnConversion(seq, loc, descriptor);
    JavaBytecodeUtils.renderMethodTyped(seq, mv, initialSlots);
  }

  private static void emitReturnConversion(
      JavaBytecodeSequence seq,
      compilerxyz.eval.dynamic.CodeLoc loc,
      JavaMethodDescriptor descriptor) {
    var descStr = descriptor.value();
    var retDesc = descStr.substring(descStr.indexOf(')') + 1);
    var retChar = retDesc.charAt(0);
    if (retChar == 'L') {
      var retInternal = JavaInternalName.of(retDesc.substring(1, retDesc.length() - 1));
      seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(retInternal)));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, returnOpcodeFor(retChar)));
    }
  }

  private static int returnOpcodeFor(char retChar) {
    return switch (retChar) {
      case 'Z', 'B', 'C', 'S', 'I' -> Opcodes.IRETURN;
      case 'J' -> Opcodes.LRETURN;
      case 'F' -> Opcodes.FRETURN;
      case 'D' -> Opcodes.DRETURN;
      case 'V' -> Opcodes.RETURN;
      default -> Opcodes.ARETURN;
    };
  }

  private record HostMethodSpec(String hostName, JavaMethodDescriptor hostDescriptor) {}

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
      compilerxyz.eval.ExprContext xx, ClassWriter cw, JavaInternalName internalName, TypeSourceObjectDynamic data) {
    var emitted = new TOrderedSet<String>();
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      var iface = impl.interface_;
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        if (m.compiledBodyCode == null) {
          continue;
        }
        var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);
        HostMethodSpec spec;
        if (ifaceMethod.hostMethodName != null && ifaceMethod.hostMethodDescriptor != null) {
          spec = new HostMethodSpec(
              ifaceMethod.hostMethodName, ifaceMethod.hostMethodDescriptor);
        } else {
          spec = new HostMethodSpec(
              ifaceMethod.name,
              JavaMethodDescriptor.fromParts(
                  JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT));
        }
        var dedupeKey = spec.hostName() + spec.hostDescriptor().value();
        if (!emitted.add(dedupeKey)) {
          continue;
        }
        emitHostAdapter(xx, cw, internalName, implIdx, mIdx, m, ifaceMethod, spec);
      }
    }
    return emitted;
  }


  private static void emitHostAdapter(
      compilerxyz.eval.ExprContext xx,
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
      SourceObjectInterfaceMethodImplData m,
      InterfaceMethodData ifaceMethod,
      HostMethodSpec spec) {
    var mv = cw.visitMethod(
        Opcodes.ACC_PUBLIC, spec.hostName(), spec.hostDescriptor().value(), null, null);
    var hostDescStr = spec.hostDescriptor().value();

    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
    var seq = new JavaBytecodeSequence();

    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));

    var calleeDescriptor = interfaceMethodMethodDescriptor(xx, m, ifaceMethod);

    int hostSlot = 1;
    var argParamTypes = m.compiledArgParamTypes;
    if (argParamTypes != null) {
      for (var argType : argParamTypes) {
        seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, hostSlot));
        var argConsume = (compilerxyz.gen.AspectConsumeDynamic) argType;
        var argDesc = argConsume.aspectConsumeDynamicJvmClass(xx);
        hostSlot += slotWidth(argDesc);
      }
    }

    seq.add(JavaBytecodeUtils.callMethod(loc,
        internalName, interfaceMethodMethodName(implIdx, mIdx), calleeDescriptor));

    var retChar = hostDescStr.charAt(hostDescStr.indexOf(')') + 1);
    seq.add(JavaBytecodeUtils.inst(loc, returnOpcodeFor(retChar)));
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

  private static JavaMethodDescriptor interfaceMethodMethodDescriptor(
      compilerxyz.eval.ExprContext xx,
      SourceObjectInterfaceMethodImplData m,
      InterfaceMethodData ifaceMethod) {
    var jvmParamDescs = new TList<JavaDataDescriptor>();
    var argParamTypes = m.compiledArgParamTypes;
    if (argParamTypes == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.interfaceMethodMethodDescriptor: missing compiledArgParamTypes");
    }
    for (var argType : argParamTypes) {
      if (!(argType instanceof compilerxyz.gen.AspectConsumeDynamic argConsume)) {
        throw new IllegalStateException(
            "SourceObjectEmitter.interfaceMethodMethodDescriptor: argParamType "
                + argType.typeDesc() + " does not implement AspectConsumeDynamic");
      }
      jvmParamDescs.add(argConsume.aspectConsumeDynamicJvmClass(xx));
    }
    JavaDataDescriptor retDesc;
    if (ifaceMethod.hostMethodDescriptor != null) {
      retDesc = returnDescOf(ifaceMethod.hostMethodDescriptor);
    } else if (ifaceMethod.retType instanceof compilerxyz.gen.AspectConsumeDynamic retConsume) {
      retDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      throw new IllegalStateException(
          "SourceObjectEmitter.interfaceMethodMethodDescriptor: retType does not implement AspectConsumeDynamic");
    }
    return JavaMethodDescriptor.fromParts(retDesc, jvmParamDescs);
  }

  private static JavaDataDescriptor returnDescOf(JavaMethodDescriptor md) {
    var s = md.value();
    var raw = s.substring(s.indexOf(')') + 1);
    return JavaDataDescriptor.create(raw);
  }

  private static void pushIntConst(JavaBytecodeSequence seq, int value) {
    if (value >= 0 && value <= 5) {
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_0 + value));
    } else {
      seq.add(JavaBytecodeUtils.wrapObj(compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE, new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, value)));
    }
  }

}
