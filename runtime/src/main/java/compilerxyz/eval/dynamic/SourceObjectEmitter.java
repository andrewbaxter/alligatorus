package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.eval.value.source_object.SourceDataMethodData;
import compilerxyz.eval.value.source_object.SourceObjectInstance;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceImplData;
import compilerxyz.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.Binding;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.value.source_interface.InterfaceData;
import org.objectweb.asm.tree.IntInsnNode;

public final class SourceObjectEmitter {
  private SourceObjectEmitter() {}

    private static final JavaInternalName INTNAME_DYN_RUNTIME =
            JavaInternalName.fromClass(DynRuntime.class);
    private static final JavaInternalName INTNAME_DYN_UTIL =
            JavaInternalName.fromClass(DynUtil.class);
  private static final JavaInternalName INTNAME_EXPR_CONTEXT =
      JavaInternalName.fromClass(ExprContext.class);
  private static final JavaInternalName INTNAME_QEXPR_ID =
      JavaInternalName.fromClass(QExprId.class);
  private static final JavaInternalName INTNAME_TYPE =
      JavaInternalName.fromClass(Type.class);
  private static final JavaInternalName INTNAME_CODE_VALUE =
      JavaInternalName.fromClass(CodeValue.class);
  private static final JavaInternalName INTNAME_CODE =
      JavaInternalName.fromClass(Code.class);
  private static final JavaInternalName INTNAME_EXPR =
      JavaInternalName.fromClass(Expr.class);
  private static final JavaInternalName INTNAME_ERESULT =
      JavaInternalName.fromClass(EResult.class);
  private static final JavaInternalName INTNAME_CONST_KEY =
      JavaInternalName.fromClass(ConstKey.class);
  private static final JavaInternalName INTNAME_UNIQUE =
      JavaInternalName.fromClass(Unique.class);
  private static final JavaInternalName INTNAME_BINDING_ID =
      JavaInternalName.fromClass(BindingId.class);
  private static final JavaInternalName INTNAME_BINDING =
      JavaInternalName.fromClass(Binding.class);
  private static final JavaInternalName INTNAME_PAIR =
      JavaInternalName.fromClass(Pair.class);
  private static final JavaInternalName INTNAME_OPT =
      JavaInternalName.fromClass(Opt.class);
  private static final JavaInternalName INTNAME_OPT_SOME =
      JavaInternalName.fromClass(Opt.Some.class);
  private static final JavaInternalName INTNAME_TLIST =
      JavaInternalName.fromClass(TList.class);
  private static final JavaInternalName INTNAME_TARGET_BIND_SUB =
      JavaInternalName.fromClass(TargetBindSub.class);
  private static final JavaInternalName INTNAME_SOURCE_OBJECT_INSTANCE =
      JavaInternalName.fromClass(SourceObjectInstance.class);
  private static final JavaInternalName INTNAME_ASPECT_BIND =
      JavaInternalName.fromClass(AspectBind.class);
  private static final JavaInternalName INTNAME_ASPECT_BIND_DYNAMIC =
      JavaInternalName.fromClass(AspectBindDynamic.class);
  private static final JavaInternalName INTNAME_ASPECT_CONSUME_DYNAMIC =
      JavaInternalName.fromClass(AspectConsumeDynamic.class);
  private static final JavaInternalName INTNAME_ASPECT_INITIAL_BIND =
      JavaInternalName.fromClass(AspectInitialBind.class);
  private static final JavaInternalName INTNAME_ASPECT_LIFT =
      JavaInternalName.fromClass(AspectLift.class);
  private static final JavaInternalName INTNAME_OBJECT_ARRAY_ELEM =
      JavaInternalName.fromClass(Object[].class);

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
      ExprContext xx,
      DefSetId defSet,
      long objectIndex,
      TypeSourceObjectDynamic data) {
    var binaryName = binaryNameFor(defSet, objectIndex);
    var internalNameStr = binaryName.replace('.', '/');
    var internalName = JavaInternalName.of(internalNameStr);

    var claimedKinds = new java.util.HashSet<BuiltinInterfaceKind>();
    for (var impl : data.interfaceImpls) {
      if (impl.interface_.builtinKind.optIsSome()) {
        claimedKinds.add(impl.interface_.builtinKind.optUnwrap());
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

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
      @Override
      protected String getCommonSuperClass(String type1, String type2) {
        return JvmConstants.INTNAME_OBJECT.value();
      }
    };
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
        internalNameStr,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        interfacesArr);

    JavaDataDescriptor dataFieldDesc;
    var underlyingTypeMaybe = data.underlyingType.optUnwrapOr(null);
    if (underlyingTypeMaybe instanceof AspectConsumeDynamic underlyingConsume) {
      dataFieldDesc = underlyingConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      dataFieldDesc = JvmConstants.DESC_OBJECT;
    }
    cw.visitField(Opcodes.ACC_PUBLIC, DATA_INSTANCE_FIELD, dataFieldDesc.value(), null, null)
        .visitEnd();

    var ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", MDESC_OBJECT_INIT.value(), null, null);
    ctor.visitCode();
    var ctorSeq = new JavaBytecodeSequence();
    ctorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    ctorSeq.add(JavaBytecodeUtils.invokeSpecial(CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT));
    ctorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
    ctorSeq.render(ctor, TList.of());
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();

    var realCtorDesc =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(dataFieldDesc));
    var realCtor =
        cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", realCtorDesc.value(), null, null);
    realCtor.visitCode();
    var realCtorSeq = new JavaBytecodeSequence();
    realCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    realCtorSeq.add(JavaBytecodeUtils.invokeSpecial(CodeLoc.Infallible.INSTANCE, JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT));
    realCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, Opcodes.ALOAD, 0));
    {
      var c = dataFieldDesc.value().charAt(0);
      int loadOp = switch (c) {
        case 'J' -> Opcodes.LLOAD;
        case 'D' -> Opcodes.DLOAD;
        case 'F' -> Opcodes.FLOAD;
        case 'I', 'B', 'C', 'S', 'Z' -> Opcodes.ILOAD;
        default -> Opcodes.ALOAD;
      };
      realCtorSeq.add(JavaBytecodeUtils.varInsn(CodeLoc.Infallible.INSTANCE, loadOp, 1));
    }
    realCtorSeq.add(JavaBytecodeUtils.setField(CodeLoc.Infallible.INSTANCE, internalName, DATA_INSTANCE_FIELD, dataFieldDesc));
    realCtorSeq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.RETURN));
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
        if (m.compiledBodyCode.optIsNone()) {
          continue;
        }
        var bodyCode = m.compiledBodyCode.optUnwrap();
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
      if (method.compiledBodyCode.optIsNone()) {
        continue;
      }
      emitMethodMethod(xx, cw, internalName, methodIdx, method, data.underlyingType.optUnwrapOr(null), method.compiledBodyCode.optUnwrap());
    }

    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        if (m.compiledBodyCode.optIsNone()) {
          continue;
        }
        emitInterfaceMethodMethod(
            xx, cw, internalName, implIdx, mIdx, impl.interface_, m, data.underlyingType.optUnwrapOr(null), m.compiledBodyCode.optUnwrap());
      }
    }

    var emittedHostSlots = emitHostInterfaceAdapters(xx, cw, internalName, data);

    cw.visitEnd();
    return new EmittedSourceObject(binaryName, internalNameStr, cw.toByteArray(), staticFieldValues);
  }

  private static void emitMethodMethod(
      ExprContext xx,
      ClassWriter cw,
      JavaInternalName internalName,
      long methodIdx,
      SourceDataMethodData method,
      Type underlyingType,
      CodeDynamic bodyCode) {
    var methodName = methodMethodName(methodIdx);
    var prefix = methodFieldPrefix(methodIdx);
    JavaDataDescriptor retDesc;
    if (method.retType.optIsSome()) {
      if (!(method.retType.optUnwrap() instanceof AspectConsumeDynamic retConsume)) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitMethodMethod: method '" + method.name
                + "' retType does not implement AspectConsumeDynamic");
      }
      retDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      retDesc = JavaDataDescriptor.DESC_VOID;
    }
    if (method.compiledArgSlots.optIsNone() || method.compiledArgSlotIds.optIsNone()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitMethodMethod: method '" + method.name
              + "' has no compiledArgSlots (finish ordering violation)");
    }
    var argSlots = method.compiledArgSlots.optUnwrap();
    var argSlotIds = method.compiledArgSlotIds.optUnwrap();
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
    if (method.innerBindingId.optIsNone()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emit: method '" + method.name + "' has no innerBindingId (finish ordering violation)");
    }
    var selfId = method.innerBindingId.optUnwrap();

    var seq = new JavaBytecodeSequence();
    var loc = CodeLoc.Infallible.INSTANCE;
    var selfLocal = compilerxyz.util.MustCast.mustCast(compilerxyz.eval.BindingId.Local.class, selfId);
    JavaDataDescriptor dataFieldDesc =
        switch (compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, underlyingType)) {
          case compilerxyz.util.Opt.Some<AspectConsumeDynamic> s ->
              s.value().aspectConsumeDynamicJvmClass(xx);
          case compilerxyz.util.Opt.None<AspectConsumeDynamic> n ->
              throw new IllegalStateException(
                  "SourceObjectEmitter.emit: underlyingType " + underlyingType.typeDesc()
                      + " does not implement AspectConsumeDynamic; cannot emit data field access");
        };
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.accessField(loc, internalName, DATA_INSTANCE_FIELD, dataFieldDesc));
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(storeOpcodeFor(dataFieldDesc), selfLocal.key())));

    var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    initialSlots.add(new JavaBytecodeSequence.InitialSlot(
        new JavaBytecodeBindingKey()));
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
    seq.add(JavaBytecodeUtils.inst(loc, returnOpcodeFor(retDesc.value().charAt(0))));
    JavaBytecodeUtils.renderMethodTyped(seq, mv, initialSlots);
  }

  private static int slotWidth(JavaDataDescriptor d) {
    var v = d.value();
    return (v.equals("D") || v.equals("J")) ? 2 : 1;
  }

  private static int storeOpcodeFor(JavaDataDescriptor d) {
    return switch (d.value().charAt(0)) {
      case 'J' -> Opcodes.LSTORE;
      case 'D' -> Opcodes.DSTORE;
      case 'F' -> Opcodes.FSTORE;
      case 'I', 'B', 'C', 'S', 'Z' -> Opcodes.ISTORE;
      default -> Opcodes.ASTORE;
    };
  }

  public static TOrderedMap<String, Object> rebuildStaticFieldValues(TypeSourceObjectDynamic data) {
    var staticFieldValues = new TOrderedMap<String, Object>();
    var seen = new TOrderedSet<String>();
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
        for (int i = 0; i < m.capturedExternals.size(); i++) {
          var entry = m.capturedExternals.get(i);
          var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
          if (seen.add(name)) {
            staticFieldValues.put(name, entry.external().externalHostValue());
          }
        }
      }
    }
    for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
      var method = data.methods.get((int) methodIdx);
      var prefix = methodFieldPrefix(methodIdx);
      for (int i = 0; i < method.capturedExternals.size(); i++) {
        var entry = method.capturedExternals.get(i);
        var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
        if (seen.add(name)) {
          staticFieldValues.put(name, entry.external().externalHostValue());
        }
      }
    }
    staticFieldValues.put(SOURCE_OBJECT_DATA_FIELD, data);
    return staticFieldValues;
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
      ExprContext xx,
      ClassWriter cw,
      JavaInternalName internalName,
      int implIdx,
      int mIdx,
      InterfaceData iface,
      SourceObjectInterfaceMethodImplData m,
      Type underlyingType,
      CodeDynamic bodyCode) {
    var methodName = interfaceMethodMethodName(implIdx, mIdx);
    var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
    if (m.innerBindingId.optIsNone()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no innerBindingId");
    }
    var selfId = compilerxyz.util.MustCast.mustCast(compilerxyz.eval.BindingId.Local.class, m.innerBindingId.optUnwrap());
    if (m.compiledArgParamIds.optIsNone()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: impl has no compiledArgParamIds");
    }
    var argParamIds = m.compiledArgParamIds.optUnwrap();
    var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);

    var descriptor = interfaceMethodMethodDescriptor(xx, m, ifaceMethod);
    var jvmParamKeys = new TList<JavaBytecodeBindingKey>();
    var jvmParamWides = new TList<Boolean>();
    if (m.compiledArgParamTypes.optIsNone() || m.compiledArgParamTypes.optUnwrap().size() != argParamIds.size()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.emitInterfaceMethodMethod: compiledArgParamTypes missing or mismatched");
    }
    var argParamTypes = m.compiledArgParamTypes.optUnwrap();
    for (int i = 0; i < argParamIds.size(); i++) {
      var argType = argParamTypes.get(i);
      var argConsumeOpt = compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, argType);
      if (argConsumeOpt.optIsNone()) {
        throw new IllegalStateException(
            "SourceObjectEmitter.emitInterfaceMethodMethod: arg type " + argType.typeDesc()
                + " does not implement AspectConsumeDynamic");
      }
      var argDesc = argConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
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
    var loc = CodeLoc.Infallible.INSTANCE;

    if (m.outerBindingId.optIsSome()) {
      switch (m.outerBindingId.optUnwrap()) {
        case BindingId.Local outerLocal -> {
          seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
          seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, outerLocal.key())));
        }
      }
    }

    JavaDataDescriptor innerDesc =
        underlyingType instanceof AspectConsumeDynamic underlyingConsume
            ? underlyingConsume.aspectConsumeDynamicJvmClass(xx)
            : JvmConstants.DESC_OBJECT;
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.accessField(loc, internalName, DATA_INSTANCE_FIELD, innerDesc));
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(storeOpcodeFor(innerDesc), selfId.key())));

    var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    initialSlots.add(new JavaBytecodeSequence.InitialSlot(
        new JavaBytecodeBindingKey()));
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
      CodeLoc loc,
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

  private static TList<String> parameterDescriptors(String descriptor) {
    var out = new TList<String>();
    var i = 1;
    while (i < descriptor.length() && descriptor.charAt(i) != ')') {
      var start = i;
      while (descriptor.charAt(i) == '[') i++;
      if (descriptor.charAt(i) == 'L') {
        i = descriptor.indexOf(';', i) + 1;
      } else {
        i++;
      }
      out.add(descriptor.substring(start, i));
    }
    return out;
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
      ExprContext xx, ClassWriter cw, JavaInternalName internalName, TypeSourceObjectDynamic data) {
    var emitted = new TOrderedSet<String>();
    for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
      var impl = data.interfaceImpls.get(implIdx);
      var iface = impl.interface_;
      for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
        var m = impl.methodImpls.get(mIdx);
        if (m.compiledBodyCode.optIsNone()) {
          continue;
        }
        var ifaceMethod = iface.methods.get(m.interfaceMethodIndex);
        HostMethodSpec spec;
        if (ifaceMethod.hostMethodName.optIsSome() && ifaceMethod.hostMethodDescriptor.optIsSome()) {
          spec = new HostMethodSpec(
              ifaceMethod.hostMethodName.optUnwrap(), ifaceMethod.hostMethodDescriptor.optUnwrap());
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
      ExprContext xx,
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

    var loc = CodeLoc.Infallible.INSTANCE;
    var seq = new JavaBytecodeSequence();

    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));

    var calleeDescriptor = interfaceMethodMethodDescriptor(xx, m, ifaceMethod);

    int hostSlot = 1;
    if (m.compiledArgParamTypes.optIsSome()) {
      var argParamTypes = m.compiledArgParamTypes.optUnwrap();
      var hostParams = parameterDescriptors(hostDescStr);
      int argIdx = 0;
      for (var argType : argParamTypes) {
        seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, hostSlot));
        var argConsumeOpt = compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, argType);
        if (argConsumeOpt.optIsNone()) {
          throw new IllegalStateException(
              "SourceObjectEmitter.emitHostAdapter: arg type " + argType.typeDesc()
                  + " does not implement AspectConsumeDynamic");
        }
        var argDesc = argConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        var hostParam = hostParams.get(argIdx);
        if (!hostParam.equals(argDesc.value())
            && (argDesc.value().startsWith("L") || argDesc.value().startsWith("["))
            && !argDesc.value().equals("Ljava/lang/Object;")) {
          seq.add(JavaBytecodeUtils.cast(loc, argDesc));
        }
        hostSlot += slotWidth(argDesc);
        argIdx++;
      }
    }

    seq.add(JavaBytecodeUtils.callMethod(loc,
        internalName, interfaceMethodMethodName(implIdx, mIdx), calleeDescriptor));

    var retChar = hostDescStr.charAt(hostDescStr.indexOf(')') + 1);
    seq.add(JavaBytecodeUtils.inst(loc, returnOpcodeFor(retChar)));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
  }

  private static compilerxyz.util.Opt<JavaInternalName> returnTypeInternalName(
      String descriptor) {
    var closeParen = descriptor.indexOf(')');
    if (closeParen < 0 || closeParen + 1 >= descriptor.length()) {
      return compilerxyz.util.Opt.none();
    }
    var ret = descriptor.charAt(closeParen + 1);
    if (ret != 'L') {
      return compilerxyz.util.Opt.none();
    }
    return compilerxyz.util.Opt.some(JavaInternalName.of(descriptor.substring(closeParen + 2, descriptor.length() - 1)));
  }

  private static JavaMethodDescriptor interfaceMethodMethodDescriptor(
      ExprContext xx,
      SourceObjectInterfaceMethodImplData m,
      InterfaceMethodData ifaceMethod) {
    var jvmParamDescs = new TList<JavaDataDescriptor>();
    if (m.compiledArgParamTypes.optIsNone()) {
      throw new IllegalStateException(
          "SourceObjectEmitter.interfaceMethodMethodDescriptor: missing compiledArgParamTypes");
    }
    for (var argType : m.compiledArgParamTypes.optUnwrap()) {
      var argConsume = switch (compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, argType)) {
        case compilerxyz.util.Opt.Some<AspectConsumeDynamic> s -> s.value();
        case compilerxyz.util.Opt.None<AspectConsumeDynamic> n ->
            throw new IllegalStateException(
                "SourceObjectEmitter.interfaceMethodMethodDescriptor: argParamType "
                    + argType.typeDesc() + " does not implement AspectConsumeDynamic");
      };
      jvmParamDescs.add(argConsume.aspectConsumeDynamicJvmClass(xx));
    }
    JavaDataDescriptor retDesc;
    if (ifaceMethod.hostMethodDescriptor.optIsSome()) {
      retDesc = returnDescOf(ifaceMethod.hostMethodDescriptor.optUnwrap());
    } else if (ifaceMethod.retType.optIsSome()) {
      if (!(ifaceMethod.retType.optUnwrap() instanceof AspectConsumeDynamic retConsume)) {
        throw new IllegalStateException(
            "SourceObjectEmitter.interfaceMethodMethodDescriptor: retType does not implement AspectConsumeDynamic");
      }
      retDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
    } else {
      retDesc = JavaDataDescriptor.DESC_VOID;
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
      seq.add(JavaBytecodeUtils.inst(CodeLoc.Infallible.INSTANCE, Opcodes.ICONST_0 + value));
    } else {
      seq.add(JavaBytecodeUtils.wrapObj(CodeLoc.Infallible.INSTANCE, new IntInsnNode(Opcodes.BIPUSH, value)));
    }
  }

}
