package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectConsumeDynamic;

public final class CompositeClassEmitter {
  private CompositeClassEmitter() {}

  public static final class StructuralKey {
    private final TypeId[] typeIds;
    private final int hash;

    public StructuralKey(TList<Type> sortedTypes) {
      this.typeIds = new TypeId[sortedTypes.size()];
      int h = 1;
      for (int i = 0; i < sortedTypes.size(); i++) {
        this.typeIds[i] = sortedTypes.get(i).hasTypeIdTypeId();
        h = 31 * h + this.typeIds[i].hashCode();
      }
      this.hash = h;
    }

    public int arity() {
      return typeIds.length;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof StructuralKey other)) return false;
      if (other.typeIds.length != typeIds.length) return false;
      for (int i = 0; i < typeIds.length; i++) {
        if (!other.typeIds[i].equals(typeIds[i])) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  public static final String BINARY_NAME_PREFIX = "compilerxyz.eval.gen.Composite$";

  public static String slotFieldName(int slotIndex) {
    return "_" + slotIndex;
  }

  public static String binaryNameFor(StructuralKey key) {
    return BINARY_NAME_PREFIX + key.arity() + "$" + Integer.toHexString(key.hashCode());
  }

  public static JavaMethodDescriptor constructorDesc(ExprContext xx, TList<Type> sortedTypes) {
    var args = new TList<JavaDataDescriptor>(sortedTypes.size());
    for (var t : sortedTypes) {
      args.add(slotDescriptor(xx, t));
    }
    return JavaMethodDescriptor.fromConstructorParts(args);
  }

  public static JavaDataDescriptor slotDescriptor(ExprContext xx, Type t) {
    return switch (compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, t)) {
      case Opt.Some<compilerxyz.gen.AspectConsumeDynamic> some ->
          some.value().aspectConsumeDynamicJvmClass(xx);
      case Opt.None<compilerxyz.gen.AspectConsumeDynamic> n ->
          throw new IllegalStateException(
              "CompositeClassEmitter: type (" + t.typeDesc() + ") does not implement AspectConsumeDynamic");
    };
  }

  public static Class<?> getOrEmit(ExprContext xx, TList<Type> sortedTypes) {
    var key = new StructuralKey(sortedTypes);
    var cache = xx.vx.ix.compositeCache;
    var existing = cache.get(key);
    if (existing != null) {
      return existing;
    }

    if (xx.vx.ix.classLoader == null) {
      xx.vx.ix.classLoader =
          new DefinitionSetClassLoader(
              DefinitionSetClassLoader.class.getClassLoader(),
              new compilerxyz.util.TList<>());
    }
    var loader = xx.vx.ix.classLoader;
    var binaryName = binaryNameFor(key);
    var bytecode = emitBytecode(xx, binaryName, sortedTypes);
    loader.registerClass(binaryName, bytecode);
    Class<?> defined;
    try {
      defined = Class.forName(binaryName, true, loader);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "CompositeClassEmitter: freshly-registered class not loadable: " + binaryName, e);
    }
    cache.put(key, defined);
    return defined;
  }

  public static String internalNameFor(ExprContext xx, TList<Type> sortedTypes) {
    return getOrEmit(xx, sortedTypes).getName().replace('.', '/');
  }

  static byte[] emitBytecode(ExprContext xx, String binaryName, TList<Type> sortedTypes) {
    var internalName = binaryName.replace('.', '/');
    var ownName = JavaInternalName.of(internalName);
    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        internalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);

    var slotDescs = new TList<JavaDataDescriptor>(sortedTypes.size());
    for (var t : sortedTypes) slotDescs.add(slotDescriptor(xx, t));

    for (int i = 0; i < sortedTypes.size(); i++) {
      cw.visitField(
              Opcodes.ACC_PUBLIC,
              slotFieldName(i),
              slotDescs.get(i).value(),
              null,
              null)
          .visitEnd();
    }

    var ctor =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            JavaMethodDescriptor.fromConstructorParts(slotDescs).value(),
            null,
            null);
    ctor.visitCode();

    var loc = CodeLoc.Infallible.INSTANCE;
    var seq = JavaBytecodeUtils.seq();
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(
        JavaBytecodeUtils.invokeSpecial(
            loc, JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS));

    int localSlot = 1;
    for (int i = 0; i < sortedTypes.size(); i++) {
      var desc = slotDescs.get(i);
      int loadOp = loadOpcodeFor(desc);
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.varInsn(loc, loadOp, localSlot));
      seq.add(JavaBytecodeUtils.setField(loc, ownName, slotFieldName(i), desc));
      localSlot += slotSizeFor(desc);
    }
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.RETURN));
    JavaBytecodeUtils.renderMethod(seq, ctor, TList.of());

    cw.visitEnd();
    return cw.toByteArray();
  }

  private static int loadOpcodeFor(JavaDataDescriptor desc) {
    var v = desc.value();
    if (v.isEmpty()) return Opcodes.ALOAD;
    return switch (v.charAt(0)) {
      case 'D' -> Opcodes.DLOAD;
      case 'J' -> Opcodes.LLOAD;
      case 'F' -> Opcodes.FLOAD;
      case 'I', 'Z', 'B', 'S', 'C' -> Opcodes.ILOAD;
      default -> Opcodes.ALOAD;
    };
  }

  private static int slotSizeFor(JavaDataDescriptor desc) {
    var v = desc.value();
    if (v.isEmpty()) return 1;
    return switch (v.charAt(0)) {
      case 'D', 'J' -> 2;
      default -> 1;
    };
  }

}
