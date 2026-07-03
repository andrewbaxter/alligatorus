package compilerxyz.eval.dynamic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class TupleClassEmitter {
  private TupleClassEmitter() {}

  public static final class StructuralKey {
    private final Type[] slotTypes;
    private final int hash;

    public StructuralKey(TList<Type> slotTypes) {
      this.slotTypes = slotTypes.toArray(new Type[0]);
      int h = 1;
      for (var t : this.slotTypes) {
        h = 31 * h + System.identityHashCode(t);
      }
      this.hash = h;
    }

    public int arity() {
      return slotTypes.length;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof StructuralKey other)) return false;
      if (other.slotTypes.length != slotTypes.length) return false;
      for (int i = 0; i < slotTypes.length; i++) {
        if (other.slotTypes[i] != slotTypes[i]) return false;
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

  public static @org.jspecify.annotations.Nullable Class<?> findLoadedByName(String binaryName) {
    return CACHE_BY_NAME.getIfPresent(binaryName);
  }

  public static final String BINARY_NAME_PREFIX = "compilerxyz.eval.gen.Tuple$";

  public static String slotFieldName(int slotIndex) {
    return "_" + slotIndex;
  }

  static String binaryNameFor(StructuralKey key) {
    return BINARY_NAME_PREFIX + key.arity() + "$" + Integer.toHexString(key.hashCode());
  }

  public static Class<?> getOrEmit(TList<Type> slotTypes) {
    var key = new StructuralKey(slotTypes);
    var existing = CACHE.getIfPresent(key);
    if (existing != null) {
      return existing;
    }

    var binaryName = binaryNameFor(key);
    var bytecode = emitBytecode(binaryName, slotTypes.size());
    var loader = new TupleClassLoader(TupleClassEmitter.class.getClassLoader(), binaryName, bytecode);
    Class<?> defined;
    try {
      defined = loader.loadClass(binaryName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError("TupleClassEmitter: freshly-defined class not loadable: " + binaryName, e);
    }

    var winner = CACHE.get(key, k -> defined);
    CACHE_BY_NAME.put(winner.getName(), winner);
    return winner;
  }

  public static String internalNameFor(TList<Type> slotTypes) {
    return getOrEmit(slotTypes).getName().replace('.', '/');
  }

  private static final JavaMethodDescriptor MDESC_OBJECT_ARRAY_CTOR =
      JavaMethodDescriptor.fromConstructorParts(JvmConstants.DESC_OBJECT.array());

  static byte[] emitBytecode(String binaryName, int arity) {
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

    for (int i = 0; i < arity; i++) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
              slotFieldName(i),
              JvmConstants.DESC_OBJECT.value(),
              null,
              null)
          .visitEnd();
    }

    var ctor =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_VARARGS,
            "<init>",
            MDESC_OBJECT_ARRAY_CTOR.value(),
            null,
            null);
    ctor.visitCode();

    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;
    var seq = JavaBytecodeUtils.seq();
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(
        JavaBytecodeUtils.invokeSpecial(
            loc, JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS));

    for (int i = 0; i < arity; i++) {
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 1));
      seq.add(JavaBytecodeUtils.literalIntShortByte(loc, i));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AALOAD));
      seq.add(
          JavaBytecodeUtils.setField(
              loc, ownName, slotFieldName(i), JvmConstants.DESC_OBJECT));
    }
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.RETURN));
    JavaBytecodeUtils.renderMethod(seq, ctor, TList.of());

    cw.visitEnd();
    return cw.toByteArray();
  }

  private static final class TupleClassLoader extends ClassLoader {
    private final String binaryName;
    private final byte[] bytecode;

    TupleClassLoader(ClassLoader parent, String binaryName, byte[] bytecode) {
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
