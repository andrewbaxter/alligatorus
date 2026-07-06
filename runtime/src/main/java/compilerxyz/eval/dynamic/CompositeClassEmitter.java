package compilerxyz.eval.dynamic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class CompositeClassEmitter {
  private CompositeClassEmitter() {}

  public static final class StructuralKey {
    private final TypeId[] typeIds;
    private final int hash;

    public StructuralKey(TList<Type> sortedTypes) {
      this.typeIds = new TypeId[sortedTypes.size()];
      int h = 1;
      for (int i = 0; i < sortedTypes.size(); i++) {
        this.typeIds[i] = sortedTypes.get(i).typeId();
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

  private static final Cache<StructuralKey, Class<?>> CACHE =
      Caffeine.newBuilder().weakValues().build();

  private static final Cache<String, Class<?>> CACHE_BY_NAME =
      Caffeine.newBuilder().weakValues().build();

  public static @Nullable Class<?> findLoadedByName(String binaryName) {
    return CACHE_BY_NAME.getIfPresent(binaryName);
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
    return slotDescriptor(t);
  }

  public static JavaDataDescriptor slotDescriptor(Type t) {
    if (!(t instanceof compilerxyz.gen.AspectConsumeDynamic consume)) {
      throw new IllegalStateException(
          "CompositeClassEmitter: type (" + t.typeDesc() + ") does not directly implement AspectConsumeDynamic");
    }
    return consume.consumeDynamicJvmClass();
  }

  public static Class<?> getOrEmit(ExprContext xx, TList<Type> sortedTypes) {
    return getOrEmit(sortedTypes);
  }

  public static Class<?> getOrEmit(TList<Type> sortedTypes) {
    var key = new StructuralKey(sortedTypes);
    var existing = CACHE.getIfPresent(key);
    if (existing != null) {
      return existing;
    }

    var binaryName = binaryNameFor(key);
    var bytecode = emitBytecode(binaryName, sortedTypes);
    var loader =
        new CompositeClassLoader(CompositeClassEmitter.class.getClassLoader(), binaryName, bytecode);
    Class<?> defined;
    try {
      defined = loader.loadClass(binaryName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "CompositeClassEmitter: freshly-defined class not loadable: " + binaryName, e);
    }

    var winner = CACHE.get(key, k -> defined);
    CACHE_BY_NAME.put(winner.getName(), winner);
    return winner;
  }

  public static String internalNameFor(ExprContext xx, TList<Type> sortedTypes) {
    return getOrEmit(xx, sortedTypes).getName().replace('.', '/');
  }

  static byte[] emitBytecode(String binaryName, TList<Type> sortedTypes) {
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
    for (var t : sortedTypes) slotDescs.add(slotDescriptor(t));

    for (int i = 0; i < sortedTypes.size(); i++) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
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

  private static final class CompositeClassLoader extends ClassLoader {
    private final String binaryName;
    private final byte[] bytecode;

    CompositeClassLoader(ClassLoader parent, String binaryName, byte[] bytecode) {
      super(parent);
      this.binaryName = binaryName;
      this.bytecode = bytecode;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      if (name.equals(binaryName)) {
        return defineClass(name, bytecode, 0, bytecode.length);
      }
      throw new ClassNotFoundException(name);
    }
  }
}
