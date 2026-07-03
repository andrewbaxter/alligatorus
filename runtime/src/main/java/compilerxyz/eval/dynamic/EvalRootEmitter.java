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
      if (v instanceof JavaBytecode.BindingUse bu) {
          if (bu.id() instanceof BindingId.Builtin b) {
              out.add(b.index());
          }
          return;
      }
      if (v instanceof JavaBytecode.BindingSet bs) {
          collectReferencedBuiltins(bs.value(), out);
          return;
      }
      if (v instanceof JavaBytecode.Block b) {
          for (var entry : b.entries()) {
              switch (entry) {
                  case JavaBytecode.BlockEntry.Code c -> collectReferencedBuiltins(c.code(), out);
                  case JavaBytecode.BlockEntry.Bind bb -> collectReferencedBuiltins(bb.value(), out);
              }
          }
          return;
      }
      if (v instanceof JavaBytecode.Foreach f) {
          collectReferencedBuiltins(f.base(), out);
          collectReferencedBuiltins(f.body(), out);
          return;
      }
      if (v instanceof JavaBytecode.ForeachMap f) {
          collectReferencedBuiltins(f.base(), out);
          collectReferencedBuiltins(f.body(), out);
          return;
      }
      if (v instanceof JavaBytecode.If i) {
          for (var branch : i.conditions()) {
              collectReferencedBuiltins(branch.condition(), out);
              collectReferencedBuiltins(branch.body(), out);
          }
          if (i.elseBody() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedBuiltins(some.value(), out);
          }
          return;
      }
      if (v instanceof JavaBytecode.Match m) {
          collectReferencedBuiltins(m.base(), out);
          for (var c : m.cases()) collectReferencedBuiltins(c.body(), out);
          if (m.fallback() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedBuiltins(some.value(), out);
          }
          return;
      }
      if (v instanceof JavaBytecode.LoweredSequence s) {
          for (var e : s.elements()) {
              if (e instanceof JavaBytecode.LoweredSequence.Code c) {
                  collectReferencedBuiltins(c.code(), out);
              }
          }
          return;
      }
      if (v instanceof JavaBytecode.Try t) {
          collectReferencedBuiltins(t.body(), out);
          if (t.catchClause() instanceof Opt.Some<JavaBytecode.Catch> some) {
              collectReferencedBuiltins(some.value().body(), out);
          }
          if (t.finallyBlock() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedBuiltins(some.value(), out);
          }
          return;
      }
      if (v instanceof CodeDynamic nested) {
          collectReferencedBuiltins(nested, out);
          return;
      }
  }

  private static void collectReferencedDefs(CodeDynamic code, TOrderedSet<BindingId.Def> out) {
      var v = code.code();
      if (v instanceof JavaBytecode.BindingUse bu) {
          if (bu.id() instanceof BindingId.Def d) {
              out.add(d);
          }
          return;
      }
      if (v instanceof JavaBytecode.BindingSet bs) {
          if (bs.base() instanceof BindingId.Def d) {
              out.add(d);
          }
          collectReferencedDefs(bs.value(), out);
          return;
      }
      if (v instanceof JavaBytecode.Block b) {
          for (var entry : b.entries()) {
              switch (entry) {
                  case JavaBytecode.BlockEntry.Code c -> collectReferencedDefs(c.code(), out);
                  case JavaBytecode.BlockEntry.Bind bb -> collectReferencedDefs(bb.value(), out);
              }
          }
          return;
      }
      if (v instanceof JavaBytecode.Foreach f) {
          collectReferencedDefs(f.base(), out);
          collectReferencedDefs(f.body(), out);
          return;
      }
      if (v instanceof JavaBytecode.ForeachMap f) {
          collectReferencedDefs(f.base(), out);
          collectReferencedDefs(f.body(), out);
          return;
      }
      if (v instanceof JavaBytecode.If i) {
          for (var branch : i.conditions()) {
              collectReferencedDefs(branch.condition(), out);
              collectReferencedDefs(branch.body(), out);
          }
          if (i.elseBody() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedDefs(some.value(), out);
          }
          return;
      }
      if (v instanceof JavaBytecode.Match m) {
          collectReferencedDefs(m.base(), out);
          for (var c : m.cases()) collectReferencedDefs(c.body(), out);
          if (m.fallback() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedDefs(some.value(), out);
          }
          return;
      }
      if (v instanceof JavaBytecode.LoweredSequence s) {
          for (var e : s.elements()) {
              if (e instanceof JavaBytecode.LoweredSequence.Code c) {
                  collectReferencedDefs(c.code(), out);
              }
          }
          return;
      }
      if (v instanceof JavaBytecode.Try t) {
          collectReferencedDefs(t.body(), out);
          if (t.catchClause() instanceof Opt.Some<JavaBytecode.Catch> some) {
              collectReferencedDefs(some.value().body(), out);
          }
          if (t.finallyBlock() instanceof Opt.Some<CodeDynamic> some) {
              collectReferencedDefs(some.value(), out);
          }
          return;
      }
      if (v instanceof CodeDynamic nested) {
          collectReferencedDefs(nested, out);
          return;
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
    var loc = compilerxyz.eval.QExprId.NONE;

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
