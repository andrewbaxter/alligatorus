package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import compilerxyz.util.TSet;
import java.util.concurrent.atomic.AtomicLong;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class EvalRootEmitter {
  private static final AtomicLong ROOT_COUNTER = new AtomicLong();

  public static final String EXPR_CONTEXT_FIELD = "__exprContext";

  private static final JavaMethodDescriptor MDESC_RUN =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, compilerxyz.util.TList.of());

  public static String allocateBinaryName() {
    var index = ROOT_COUNTER.getAndIncrement();
    return "compilerxyz.eval.gen.EvalRoot$" + index;
  }

  public static String toInternalName(String binaryName) {
    return binaryName.replace('.', '/');
  }

  private EvalRootEmitter() {}

  public record EmittedRoot(
      String binaryName,
      String internalName,
      byte[] bytecode,
      TOrderedMap<String, Object> staticFieldValues) {}

  public static TOrderedSet<Long> referencedBuiltinIds(CodeDynamic code) {
    var out = new TOrderedSet<Long>();
    collectReferencedBuiltins(code, out);
    return out;
  }

  public static TOrderedSet<BindingId.Def> referencedDefIds(CodeDynamic code) {
    var out = new TOrderedSet<BindingId.Def>();
    collectReferencedDefs(code, out);
    return out;
  }

  private static void collectReferencedBuiltins(CodeDynamic code, TOrderedSet<Long> out) {
      var v = code.code();
      if (v instanceof JavaBytecodeInstructionObj obj
          && obj.node instanceof org.objectweb.asm.tree.FieldInsnNode fi
          && fi.getOpcode() == org.objectweb.asm.Opcodes.GETSTATIC
          && fi.name.startsWith(Prefixes.BUILTIN_PREFIX)) {
          try {
              out.add(Long.parseLong(fi.name.substring(Prefixes.BUILTIN_PREFIX.length())));
          } catch (NumberFormatException e) {
              // not a builtin id we understand
          }
          return;
      }
      if (v instanceof JavaBytecodeSequence s) {
          for (var child : s.children) collectReferencedBuiltins(child, out);
          return;
      }
      if (v instanceof CodeDynamic nested) {
          collectReferencedBuiltins(nested, out);
      }
  }

  private static void collectReferencedDefs(CodeDynamic code, TOrderedSet<BindingId.Def> out) {
      var v = code.code();
      if (v instanceof JavaBytecodeSequence s) {
          for (var child : s.children) collectReferencedDefs(child, out);
          return;
      }
      if (v instanceof CodeDynamic nested) {
          collectReferencedDefs(nested, out);
      }
  }

  public static String builtinFieldName(long scopeId) {
    return Prefixes.BUILTIN_PREFIX + scopeId;
  }

  public static EmittedRoot emit(
      TargetDynamic target,
      String binaryName,
      CodeDynamic rootCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, Object> builtinDynamics) {
    return emit(target, binaryName, rootCode, externals, builtinDynamics, new TSet<>());
  }

  public static EmittedRoot emit(
      TargetDynamic target,
      String binaryName,
      CodeDynamic rootCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, Object> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    var internalName = toInternalName(binaryName);

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        internalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);

    var fieldNames = new TOrderedSet<String>();
    fieldNames.addAll(builtinDynamics.keySet());
    fieldNames.add(EXPR_CONTEXT_FIELD);
    var externalFieldNames = new TOrderedMap<BindingId, String>();
    for (var entry : externals) {
      var fieldName = ImportInstall.bindingIdDynamicKey(entry.id());
      externalFieldNames.put(entry.id(), fieldName);
      fieldNames.add(fieldName);
    }

    for (var def : referencedDefIds(rootCode)) {
      if (staticFunctionDefs.contains(def)) {
        continue;
      }
      fieldNames.add(ImportInstall.bindingIdDynamicKey(def));
    }
    for (var name : fieldNames) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              name,
              JvmConstants.DESC_OBJECT.value(),
              null,
              null)
          .visitEnd();
    }

    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "run",
            MDESC_RUN.value(),
            null,
            null);
    var ctx = new EmitCtx(internalName, 0);
    var seq = new JavaBytecodeSequence();
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;

    var ownerInternal = JavaInternalName.of(internalName);
    for (var entry : externals) {
      if (!(entry.id() instanceof BindingId.Local localId)) {
        throw new IllegalStateException(
            "External captured under non-Local BindingId: " + entry.id());
      }
      var fieldName = externalFieldNames.get(localId);
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc, ownerInternal, fieldName, JvmConstants.DESC_OBJECT));
      var slot = ctx.allocSlot(localId);
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ASTORE, slot));
    }
    target.emit(seq, ctx, rootCode);
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
    cw.visitEnd();
    var bytecode = cw.toByteArray();

    var staticFields = new TOrderedMap<String, Object>();
    staticFields.putAll(builtinDynamics);
    for (var entry : externals) {
      var fieldName = externalFieldNames.get(entry.id());
      staticFields.put(fieldName, entry.external().externalHostValue());
    }
    return new EmittedRoot(binaryName, internalName, bytecode, staticFields);
  }
}
