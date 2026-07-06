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

  public static final JavaDataDescriptor DESC_EXPR_CONTEXT =
      JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/eval/ExprContext"));

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
      if (v instanceof JavaBytecodeInstructionObj obj
          && obj.node instanceof org.objectweb.asm.tree.FieldInsnNode fi
          && (fi.getOpcode() == org.objectweb.asm.Opcodes.GETSTATIC
              || fi.getOpcode() == org.objectweb.asm.Opcodes.PUTSTATIC)
          && fi.name.startsWith(Prefixes.DEF_PREFIX)) {
          var def = parseDefFieldName(fi.name);
          if (def != null) out.add(def);
          return;
      }
      if (v instanceof JavaBytecodeSequence s) {
          for (var child : s.children) collectReferencedDefs(child, out);
          return;
      }
      if (v instanceof CodeDynamic nested) {
          collectReferencedDefs(nested, out);
      }
  }

  private static BindingId.Def parseDefFieldName(String name) {
      var body = name.substring(Prefixes.DEF_PREFIX.length());
      var parts = body.split("_");
      if (parts.length != 4) return null;
      try {
          var importState = new compilerxyz.eval.ImportStateId(Long.parseLong(parts[0]));
          var source = new compilerxyz.eval.SourceId(Long.parseLong(parts[1]));
          var defSet = new compilerxyz.eval.DefSetId(
              new compilerxyz.eval.CompileId(importState, source), Long.parseLong(parts[2]));
          return new BindingId.Def(defSet, Long.parseLong(parts[3]));
      } catch (NumberFormatException e) {
          return null;
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
      TOrderedMap<String, compilerxyz.eval.External> builtinDynamics) {
    return emit(target, binaryName, rootCode, externals, builtinDynamics, new TSet<>());
  }

  public static EmittedRoot emit(
      TargetDynamic target,
      String binaryName,
      CodeDynamic rootCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, compilerxyz.eval.External> builtinDynamics,
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

    var fieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
    for (var entry : builtinDynamics.entrySet()) {
      fieldDescriptors.put(
          entry.getKey(),
          JavaDataDescriptor.fromClass(entry.getValue().externalHostClass()));
    }
    fieldDescriptors.put(EXPR_CONTEXT_FIELD, DESC_EXPR_CONTEXT);
    var externalFieldNames = new TList<String>(externals.size());
    var externalFieldDescriptors = new TList<JavaDataDescriptor>(externals.size());
    for (int i = 0; i < externals.size(); i++) {
      var entry = externals.get(i);
      var fieldName = Prefixes.EXTERNAL_PREFIX + i;
      externalFieldNames.add(fieldName);
      var desc = JavaDataDescriptor.fromClass(entry.external().externalHostClass());
      externalFieldDescriptors.add(desc);
      fieldDescriptors.put(fieldName, desc);
    }

    for (var def : referencedDefIds(rootCode)) {
      if (staticFunctionDefs.contains(def)) {
        continue;
      }
      fieldDescriptors.put(ImportInstall.bindingIdDynamicKey(def), JvmConstants.DESC_OBJECT);
    }
    for (var entry : fieldDescriptors.entrySet()) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              entry.getKey(),
              entry.getValue().value(),
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
    for (int i = 0; i < externals.size(); i++) {
      var entry = externals.get(i);
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc, ownerInternal, externalFieldNames.get(i), externalFieldDescriptors.get(i)));
      seq.add(
          new CodeDynamic(
              loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }
    target.emit(seq, ctx, rootCode);
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
    cw.visitEnd();
    var bytecode = cw.toByteArray();

    var staticFields = new TOrderedMap<String, Object>();
    for (var entry : builtinDynamics.entrySet()) {
      staticFields.put(entry.getKey(), entry.getValue().externalHostValue());
    }
    for (int i = 0; i < externals.size(); i++) {
      staticFields.put(externalFieldNames.get(i), externals.get(i).external().externalHostValue());
    }
    return new EmittedRoot(binaryName, internalName, bytecode, staticFields);
  }
}
