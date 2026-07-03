package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import compilerxyz.eval.value.source_object.SourceObjectData;
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
  private static final JavaMethodDescriptor MDESC_DELEGATED_ASPECT =
      JavaMethodDescriptor.fromParts(DESC_OPT, JvmConstants.DESC_OBJECT, DESC_UNIQUE);

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
      SourceObjectData data,
      TOrderedMap<String, Object> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    var binaryName = binaryNameFor(defSet, objectIndex);
    var internalNameStr = binaryName.replace('.', '/');
    var internalName = JavaInternalName.of(internalNameStr);

    var implementsInternalNames = new TOrderedSet<String>();

    implementsInternalNames.add(INTNAME_SOURCE_OBJECT_INSTANCE.value());
    implementsInternalNames.add(INTNAME_ASPECT_BIND.value());
    implementsInternalNames.add(INTNAME_ASPECT_BIND_DYNAMIC.value());
    implementsInternalNames.add(INTNAME_ASPECT_CONSUME_DYNAMIC.value());
    implementsInternalNames.add(INTNAME_ASPECT_FIELD.value());
    implementsInternalNames.add(INTNAME_ASPECT_INITIAL_BIND.value());
    implementsInternalNames.add(INTNAME_ASPECT_LIFT.value());
    for (var impl : data.interfaceImpls) {
      var iface = impl.interface_;
      if (iface.builtinKind != null) {
        var hostInternal = builtinHostInterfaceInternalName(iface.builtinKind);
        if (hostInternal != null) {
          implementsInternalNames.add(hostInternal.value());
        }
      } else {
        var ifaceBinary = iface.compiledBinaryName;
        if (ifaceBinary != null) {
          implementsInternalNames.add(ifaceBinary.replace('.', '/'));
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
    ctorSeq.varInsn(Opcodes.ALOAD, 0);
    ctorSeq.invokeSpecial(JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT);
    ctorSeq.insn(Opcodes.RETURN);
    ctorSeq.render(ctor, TList.of());
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();

    var realCtor =
        cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", MDESC_CTOR_OBJECT.value(), null, null);
    realCtor.visitCode();
    var realCtorSeq = new JavaBytecodeSequence();
    realCtorSeq.varInsn(Opcodes.ALOAD, 0);
    realCtorSeq.invokeSpecial(JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT);
    realCtorSeq.varInsn(Opcodes.ALOAD, 0);
    realCtorSeq.varInsn(Opcodes.ALOAD, 1);
    realCtorSeq.putField(internalName, DATA_INSTANCE_FIELD, JvmConstants.DESC_OBJECT);
    realCtorSeq.insn(Opcodes.RETURN);
    realCtorSeq.render(realCtor, TList.of());
    realCtor.visitMaxs(0, 0);
    realCtor.visitEnd();

    var allFieldNames = new TOrderedSet<String>();
    var staticFieldValues = new TOrderedMap<String, Object>();

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
            bodyCode, m.capturedExternals, prefix, allFieldNames, staticFieldValues,
            builtinDynamics, staticFunctionDefs);
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
        var name = prefix + Prefixes.BUILTIN_PREFIX + builtinId;
        if (allFieldNames.add(name)) {
          var v = builtinDynamics.get(Prefixes.BUILTIN_PREFIX + builtinId);
          if (v != null) {
            staticFieldValues.put(name, v);
          }
        }
      }
      for (var entry : method.capturedExternals) {
        var id = entry.id();
        if (!(id instanceof BindingId.Local)) {
          throw new IllegalStateException("External captured under non-Local BindingId: " + id);
        }
        var name = prefix + ImportInstall.bindingIdDynamicKey(id);
        if (allFieldNames.add(name)) {
          staticFieldValues.put(name, entry.external().externalHostValue());
        }
      }
      for (var defId : EvalRootEmitter.referencedDefIds(bodyCode)) {
        if (staticFunctionDefs.contains(defId)) {
          continue;
        }
        var name = prefix + ImportInstall.bindingIdDynamicKey(defId);
        allFieldNames.add(name);

      }
    }
    for (var name : allFieldNames) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              name,
              JvmConstants.DESC_OBJECT.value(),
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
            cw, internalName, implIdx, mIdx, m, data.dataField, bodyCode, staticFunctionDefs);
      }
    }

    emitUserInterfaceStubs(cw, data.interfaceImpls);

    var emittedHostSlots = emitHostInterfaceAdapters(cw, internalName, data);
    emitDelegateAspectForwarders(cw, internalName, emittedHostSlots);

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
    var dataFieldCanonical = new ConstKey.Str(dataField).toCanonical();
    var selfSlot = ctx.allocSlot(selfId);
    seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.literalString(compilerxyz.eval.QExprId.NONE, dataFieldCanonical));
    seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "fieldGet", MDESC_FIELD_GET));
    seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, selfSlot));

    var argId = method.compiledArgBindingId;
    if (argId != null) {
      var argSlot = ctx.allocSlot(argId);
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 2));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, argSlot));
    }

    for (var entry : method.capturedExternals) {
      if (!(entry.id() instanceof BindingId.Local localId)) {
        throw new IllegalStateException(
            "External captured under non-Local BindingId: " + entry.id());
      }
      var fieldName = prefix + ImportInstall.bindingIdDynamicKey(localId);
      seq.add(JavaBytecodeUtils.accessStaticField(compilerxyz.eval.QExprId.NONE, internalName, fieldName, JvmConstants.DESC_OBJECT));
      var slot = ctx.allocSlot(localId);
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, slot));
    }
    target.emit(seq, ctx, bodyCode);
    seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
  }

  private static void collectMethodFields(
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> capturedExternals,
      String prefix,
      TOrderedSet<String> allFieldNames,
      TOrderedMap<String, Object> staticFieldValues,
      TOrderedMap<String, Object> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    for (var builtinId : EvalRootEmitter.referencedBuiltinIds(bodyCode)) {
      var name = prefix + Prefixes.BUILTIN_PREFIX + builtinId;
      if (allFieldNames.add(name)) {
        var v = builtinDynamics.get(Prefixes.BUILTIN_PREFIX + builtinId);
        if (v != null) {
          staticFieldValues.put(name, v);
        }
      }
    }
    for (var entry : capturedExternals) {
      var id = entry.id();
      if (!(id instanceof BindingId.Local)) {
        throw new IllegalStateException("External captured under non-Local BindingId: " + id);
      }
      var name = prefix + ImportInstall.bindingIdDynamicKey(id);
      if (allFieldNames.add(name)) {
        staticFieldValues.put(name, entry.external().externalHostValue());
      }
    }
    for (var defId : EvalRootEmitter.referencedDefIds(bodyCode)) {
      if (staticFunctionDefs.contains(defId)) {
        continue;
      }
      var name = prefix + ImportInstall.bindingIdDynamicKey(defId);
      allFieldNames.add(name);
    }
  }

  private static void emitInterfaceMethodMethod(
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
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
    var selfId = m.innerBindingId;
    if (selfId == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no innerBindingId");
    }
    var argParamIds = m.compiledArgParamIds;
    if (argParamIds == null) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
    }
    var seq = new JavaBytecodeSequence();
    var isArgMethod = m.compiledIsArgMethod;
    if (isArgMethod) {

      var selfSlot = ctx.allocSlot(selfId);
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 2));
      seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.QExprId.NONE, JavaDataDescriptor.fromInternalName(INTNAME_OBJECT_ARRAY_ELEM)));
      seq.add(JavaBytecodeUtils.literalIntShortByte(compilerxyz.eval.QExprId.NONE, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AALOAD));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, selfSlot));
    } else {

      var dataFieldCanonical = new ConstKey.Str(dataField).toCanonical();
      var selfSlot = ctx.allocSlot(selfId);
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 2));
      seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.QExprId.NONE, JavaDataDescriptor.fromInternalName(INTNAME_OBJECT_ARRAY_ELEM)));
      if (!argParamIds.isEmpty()) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      }
      seq.add(JavaBytecodeUtils.literalIntShortByte(compilerxyz.eval.QExprId.NONE, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AALOAD));
      seq.add(JavaBytecodeUtils.literalString(compilerxyz.eval.QExprId.NONE, dataFieldCanonical));
      seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "fieldGet", MDESC_FIELD_GET));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, selfSlot));
      for (int i = 0; i < argParamIds.size(); i++) {
        var slot = ctx.allocSlot(argParamIds.get(i));
        if (i < argParamIds.size() - 1) {
          seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        }
        seq.add(JavaBytecodeUtils.literalIntShortByte(compilerxyz.eval.QExprId.NONE, i + 1));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AALOAD));
        seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, slot));
      }
    }

    for (var entry : m.capturedExternals) {
      if (!(entry.id() instanceof BindingId.Local localId)) {
        throw new IllegalStateException(
            "External captured under non-Local BindingId: " + entry.id());
      }
      var fieldName = prefix + ImportInstall.bindingIdDynamicKey(localId);
      seq.add(JavaBytecodeUtils.accessStaticField(compilerxyz.eval.QExprId.NONE, internalName, fieldName, JvmConstants.DESC_OBJECT));
      var slot = ctx.allocSlot(localId);
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ASTORE, slot));
    }
    target.emit(seq, ctx, bodyCode);
    seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
  }

  private record HostMethodSpec(String hostName, JavaMethodDescriptor hostDescriptor, ReturnKind returnKind) {}

  private enum ReturnKind {

    OBJECT_CAST,

    BOOLEAN_UNBOX,

    ERESULT_FROM_CV_CELL,

    ERESULT_CODE_FROM_CV_CELL,

    ERESULT_FROM_CV_CELL_RETAGGED,
    OPT_ASPECT_WITH_DELEGATE_FALLTHROUGH
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
            ReturnKind.OPT_ASPECT_WITH_DELEGATE_FALLTHROUGH);
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
      ClassWriter cw, JavaInternalName internalName, SourceObjectData data) {
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
      boolean passData) {}

  private static final TList<InlineHelperSpec> INLINE_HELPER_METHODS = TList.of(
      new InlineHelperSpec(
          "bind",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectBind",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true),
      new InlineHelperSpec(
          "bindDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectBindDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true),
      new InlineHelperSpec(
          "toDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectToDynamic",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          true),
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
          true),
      new InlineHelperSpec(
          "fieldKeys",
          JavaMethodDescriptor.fromParts(DESC_TLIST, DESC_EXPR_CONTEXT),
          "sourceObjectFieldKeys",
          JavaMethodDescriptor.fromParts(DESC_TLIST, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT),
          true),
      new InlineHelperSpec(
          "fieldTypeOpt",
          JavaMethodDescriptor.fromParts(DESC_OPT, DESC_EXPR_CONTEXT, DESC_CONST_KEY),
          "sourceObjectFieldTypeOpt",
          JavaMethodDescriptor.fromParts(
              DESC_OPT, JvmConstants.DESC_OBJECT, DESC_EXPR_CONTEXT, DESC_CONST_KEY),
          true),
      new InlineHelperSpec(
          "initialBind",
          JavaMethodDescriptor.fromParts(DESC_BINDING, DESC_BINDING_ID),
          "sourceObjectInitialBind",
          JavaMethodDescriptor.fromParts(DESC_BINDING, JvmConstants.DESC_OBJECT, DESC_BINDING_ID),
          true),
      new InlineHelperSpec(
          "lift",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          "sourceObjectLift",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE),
          false),
      new InlineHelperSpec(
          "liftRuntime",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, JvmConstants.DESC_OBJECT),
          "sourceObjectLiftRuntime",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT,
              JvmConstants.DESC_OBJECT,
              DESC_EXPR_CONTEXT,
              DESC_QEXPR_ID,
              JvmConstants.DESC_OBJECT),
          true),
      new InlineHelperSpec(
          "typeCanCastTo",
          JavaMethodDescriptor.fromParts(
              JvmConstants.DESC_BOOL, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TYPE),
          "sourceObjectCanCastTo",
          JavaMethodDescriptor.fromParts(
              JvmConstants.DESC_BOOL, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_TYPE),
          false),
      new InlineHelperSpec(
          "typeCastTo",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_TYPE),
          "sourceObjectCastTo",
          JavaMethodDescriptor.fromParts(
              DESC_ERESULT, DESC_EXPR_CONTEXT, DESC_QEXPR_ID, DESC_CODE_VALUE, DESC_TYPE),
          false));

  private static void emitDelegateAspectForwarders(
      ClassWriter cw, JavaInternalName internalName, TOrderedSet<String> emittedHostSlots) {
    for (var spec : INLINE_HELPER_METHODS) {
      var dedupeKey = spec.hostName() + spec.hostDescriptor().value();
      if (emittedHostSlots.contains(dedupeKey)) {
        continue;
      }
      var mv =
          cw.visitMethod(
              Opcodes.ACC_PUBLIC, spec.hostName(), spec.hostDescriptor().value(), null, null);
      var seq = new JavaBytecodeSequence();
      if (spec.passData()) {
        seq.add(JavaBytecodeUtils.accessStaticField(compilerxyz.eval.QExprId.NONE, internalName, SOURCE_OBJECT_DATA_FIELD, JvmConstants.DESC_OBJECT));
      }
      int slot = 1;
      var descriptor = spec.hostDescriptor().value();
      int i = 1;
      while (i < descriptor.length() && descriptor.charAt(i) != ')') {
        var c = descriptor.charAt(i);
        if (c == 'L') {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, slot));
          slot++;
          i = descriptor.indexOf(';', i) + 1;
        } else if (c == '[') {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, slot));
          slot++;
          i++;
          while (i < descriptor.length() && descriptor.charAt(i) == '[') i++;
          if (i < descriptor.length() && descriptor.charAt(i) == 'L') {
            i = descriptor.indexOf(';', i) + 1;
          } else {
            i++;
          }
        } else {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, slot));
          slot++;
          i++;
        }
      }
      seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, spec.helperName(), spec.helperDescriptor()));
      var closeParen = descriptor.indexOf(')');
      var retChar = descriptor.charAt(closeParen + 1);
      if (retChar == 'Z' || retChar == 'B' || retChar == 'C' || retChar == 'S' || retChar == 'I') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.IRETURN));
      } else if (retChar == 'J') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.LRETURN));
      } else if (retChar == 'F') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.FRETURN));
      } else if (retChar == 'D') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DRETURN));
      } else if (retChar == 'V') {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.RETURN));
      } else {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
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
    seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));

    if (firstParamIsExprContext) {
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 1));
    } else {
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
    }

    emitArgPacking(seq, kind, m, ifaceMethod, firstParamIsExprContext, paramSlotCount);

    seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.QExprId.NONE, 
        internalName, interfaceMethodMethodName(implIdx, mIdx), MDESC_METHOD_METHOD));

    switch (spec.returnKind()) {
      case BOOLEAN_UNBOX -> {
        seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.QExprId.NONE, JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_BOXED_BOOL)));
        seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.QExprId.NONE, JvmConstants.INTNAME_BOXED_BOOL, "booleanValue", MDESC_BOOLEAN_VALUE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.IRETURN));
      }
      case OBJECT_CAST -> {
        var retInternal = returnTypeInternalName(hostDescStr);
        if (retInternal != null) {
          seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.QExprId.NONE, JavaDataDescriptor.fromInternalName(retInternal)));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
      }
      case ERESULT_FROM_CV_CELL -> {
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "eresultFromCvCell", MDESC_ERESULT_FROM_ONE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
      }
      case ERESULT_CODE_FROM_CV_CELL -> {
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "eresultCodeFromCvCell", MDESC_ERESULT_FROM_ONE));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
      }
      case ERESULT_FROM_CV_CELL_RETAGGED -> {
        seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "eresultFromCvCellRetagged", MDESC_ERESULT_FROM_TWO));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
      }
      case OPT_ASPECT_WITH_DELEGATE_FALLTHROUGH -> {
        seq.add(JavaBytecodeUtils.cast(compilerxyz.eval.QExprId.NONE, JavaDataDescriptor.fromInternalName(INTNAME_OPT)));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        seq.instanceOf(INTNAME_OPT_SOME);
        var fallthrough = new org.objectweb.asm.Label();
        var fallthroughNode = new org.objectweb.asm.tree.LabelNode(fallthrough);
        seq.obj(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFEQ, fallthroughNode));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
        seq.obj(fallthroughNode);
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.POP));
        seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
        seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 2));
        seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "sourceObjectDelegatedAspect", MDESC_DELEGATED_ASPECT));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
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

      seq.newInstance(JvmConstants.INTNAME_HASH_MAP);
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.invokeSpecial(compilerxyz.eval.QExprId.NONE, JvmConstants.INTNAME_HASH_MAP, "<init>", MDESC_HASHMAP_INIT));
      var dataFields = ifaceMethod.dataFieldNames;
      var sourceName = ifaceMethod.name;
      boolean isCastFrom = "cast_from".equals(sourceName);
      boolean isCanCastFrom = "can_cast_from".equals(sourceName);
      for (int i = 0; i < dataFields.size(); i++) {
        var fieldName = dataFields.get(i);
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        seq.add(JavaBytecodeUtils.literalString(compilerxyz.eval.QExprId.NONE, fieldName));
        switch (fieldName) {
          case "xx" -> {
            if (firstParamIsExprContext) {
              seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 1));
              seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "wrapExprContextPush", MDESC_WRAP_ONE_OBJ));
            } else {
              seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
            }
          }
          case "source_type" -> {
            if (isCanCastFrom && paramSlotCount >= 3) {

              seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 3));
              seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "wrapTypeRefPush", MDESC_WRAP_ONE_OBJ));
            } else if (isCastFrom && paramSlotCount >= 3) {

              seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 1));
              seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 3));
              seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "deriveSourceTypeFromCv", MDESC_WRAP_TWO_OBJ));
            } else {
              seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
            }
          }
          case "cv" -> {
            if (isCastFrom && paramSlotCount >= 3) {
              seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 3));
              seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "wrapCodeValueExternCell", MDESC_WRAP_ONE_OBJ));
            } else {
              seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
            }
          }
          default -> seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
        }
        seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.QExprId.NONE, JvmConstants.INTNAME_HASH_MAP, "put", MDESC_HASHMAP_PUT));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.POP));
      }

      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_1));
      seq.obj(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value()));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP_X1));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.SWAP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.SWAP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
    } else if (kind == BuiltinInterfaceKind.BINDING || kind == BuiltinInterfaceKind.TARGET) {

      var dataFields = ifaceMethod.dataFieldNames;
      var arrSize = 1 + dataFields.size();
      pushIntConst(seq, arrSize);
      seq.obj(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value()));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));

      for (int i = 0; i < dataFields.size(); i++) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        pushIntConst(seq, i + 1);
        var hostSlot = 1 + i;
        if (hostSlot <= paramSlotCount) {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, hostSlot));
        } else {
          seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
      }
    } else if ((kind == BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC
            || kind == BuiltinInterfaceKind.ASPECT_FIELD
            || kind == BuiltinInterfaceKind.ASPECT_FUNCTION)
        && ifaceMethod.argRecordType != null) {

      pushIntConst(seq, 2);
      seq.obj(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value()));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_1));
      seq.newInstance(JvmConstants.INTNAME_HASH_MAP);
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.invokeSpecial(compilerxyz.eval.QExprId.NONE, JvmConstants.INTNAME_HASH_MAP, "<init>", MDESC_HASHMAP_INIT));
      int nextCvSlot = 3;
      for (var fieldName : ifaceMethod.dataFieldNames) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        seq.add(JavaBytecodeUtils.literalString(compilerxyz.eval.QExprId.NONE, fieldName));
        if ("xx".equals(fieldName)) {
          if (firstParamIsExprContext) {
            seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 1));
            seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "wrapExprContextPush", MDESC_WRAP_ONE_OBJ));
          } else {
            seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
          }
        } else {
          if (nextCvSlot <= paramSlotCount) {
            seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, nextCvSlot));
            seq.add(JavaBytecodeUtils.callStaticMethod(compilerxyz.eval.QExprId.NONE, INTNAME_DYN_RUNTIME, "wrapCodeValueExternCell", MDESC_WRAP_ONE_OBJ));
            nextCvSlot++;
          } else {
            seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
          }
        }
        seq.add(JavaBytecodeUtils.callMethod(compilerxyz.eval.QExprId.NONE, JvmConstants.INTNAME_HASH_MAP, "put", MDESC_HASHMAP_PUT));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.POP));
      }
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
    } else {

      var argParamIds = m.compiledArgParamIds;
      var hasArg = argParamIds != null && !argParamIds.isEmpty();
      var arrSize = hasArg ? 2 : 1;
      pushIntConst(seq, arrSize);
      seq.obj(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value()));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_0));
      seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
      if (hasArg) {
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.DUP));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_1));

        if (paramSlotCount >= 1) {
          seq.add(JavaBytecodeUtils.varInsn(compilerxyz.eval.QExprId.NONE, Opcodes.ALOAD, paramSlotCount));
        } else {
          seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
        }
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.AASTORE));
      }
    }
  }

  private static void pushIntConst(JavaBytecodeSequence seq, int value) {
    if (value >= 0 && value <= 5) {
      seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ICONST_0 + value));
    } else {
      seq.obj(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, value));
    }
  }

  private static void emitUserInterfaceStubs(
      ClassWriter cw,
      TList<SourceObjectInterfaceImplData> interfaceImpls) {
    var emitted = new TOrderedSet<String>();
    var stubDescriptor =
        JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
    for (var impl : interfaceImpls) {
      var iface = impl.interface_;
      if (iface.builtinKind != null) {
        continue;
      }
      for (var ifaceMethod : iface.methods) {
        var key = ifaceMethod.name + ":" + stubDescriptor.value();
        if (!emitted.add(key)) {
          continue;
        }
        var mv =
            cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                ifaceMethod.name,
                stubDescriptor.value(),
                null,
                null);
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ACONST_NULL));
        seq.add(JavaBytecodeUtils.inst(compilerxyz.eval.QExprId.NONE, Opcodes.ARETURN));
        JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
      }
    }
  }
}
