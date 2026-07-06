package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import org.objectweb.asm.Opcodes;

public final class BindingCode {
  private BindingCode() {}

  public static CodeDynamic bindingUse(CodeLoc loc, EvalContext vx, BindingId id) {
    return switch (id) {
      case BindingId.Local local ->
          new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, local.key()));
      case BindingId.Def def -> staticFieldForBinding(loc, vx, def);
      case BindingId.Builtin builtin -> staticFieldForBinding(loc, vx, builtin);
    };
  }

  public static CodeDynamic bindingUse(QExprId loc, EvalContext vx, BindingId id) {
    return bindingUse(new CodeLoc.Some(loc), vx, id);
  }

  public static CodeDynamic bindingSet(
      CodeLoc loc, EvalContext vx, BindingId id, CodeDynamic value) {
    var seq = new JavaBytecodeSequence();
    seq.add(value);
    switch (id) {
      case BindingId.Local local ->
          seq.add(
              new CodeDynamic(
                  loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, local.key())));
      case BindingId.Def def -> seq.add(staticFieldSetForBinding(loc, vx, def));
      case BindingId.Builtin builtin -> seq.add(staticFieldSetForBinding(loc, vx, builtin));
    }
    return new CodeDynamic(loc, seq);
  }

  public static CodeDynamic bindingSet(
      QExprId loc, EvalContext vx, BindingId id, CodeDynamic value) {
    return bindingSet(new CodeLoc.Some(loc), vx, id, value);
  }

  private static JavaDataDescriptor descForBinding(EvalContext vx, BindingId id) {
    return switch (id) {
      case BindingId.Builtin builtin -> {
        var key = ImportInstall.bindingIdDynamicKey(builtin);
        var external = vx.ix.builtinDynamics.get(key);
        yield external == null
            ? JvmConstants.DESC_OBJECT
            : JavaDataDescriptor.fromClass(external.externalHostClass());
      }
      case BindingId.Def ignored -> JvmConstants.DESC_OBJECT;
      case BindingId.Local ignored ->
          throw new AssertionError(
              "BindingCode::descForBinding: local bindings do not have static field descriptors");
    };
  }

  private static CodeDynamic staticFieldForBinding(CodeLoc loc, EvalContext vx, BindingId id) {
    return JavaBytecodeUtils.accessStaticField(
        loc,
        JavaInternalName.of(vx.evalRootInternalName),
        ImportInstall.bindingIdDynamicKey(id),
        descForBinding(vx, id));
  }

  private static CodeDynamic staticFieldSetForBinding(
      CodeLoc loc, EvalContext vx, BindingId id) {
    return JavaBytecodeUtils.setStaticField(
        loc,
        JavaInternalName.of(vx.evalRootInternalName),
        ImportInstall.bindingIdDynamicKey(id),
        descForBinding(vx, id));
  }
}
