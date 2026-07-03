package compilerxyz.eval.dynamic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import compilerxyz.eval.Type;
import compilerxyz.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class RecordClassEmitter {
  private RecordClassEmitter() {}

  public record FieldKey(String name, Type type) {}

  public static final class StructuralKey {
    private final FieldKey[] fields;
    private final int hash;

    public StructuralKey(TList<FieldKey> fields) {
      this.fields = fields.toArray(new FieldKey[0]);
      int h = 1;
      for (var f : this.fields) {
        h = 31 * h + f.name().hashCode();
        h = 31 * h + System.identityHashCode(f.type());
      }
      this.hash = h;
    }

    public int arity() {
      return fields.length;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof StructuralKey other)) return false;
      if (other.fields.length != fields.length) return false;
      for (int i = 0; i < fields.length; i++) {
        if (!other.fields[i].name().equals(fields[i].name())) return false;
        if (other.fields[i].type() != fields[i].type()) return false;
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

  public static final String BINARY_NAME_PREFIX = "compilerxyz.eval.gen.Record$";

  public static String sanitizeFieldName(String sourceName) {
    if (sourceName.isEmpty()) return "_";
    var sb = new StringBuilder(sourceName.length());
    var first = sourceName.charAt(0);
    if (!Character.isJavaIdentifierStart(first)) {
      sb.append('_');
      if (Character.isJavaIdentifierPart(first)) sb.append(first);
      else sb.append('_');
    } else {
      sb.append(first);
    }
    for (int i = 1; i < sourceName.length(); i++) {
      var c = sourceName.charAt(i);
      sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
    }
    return sb.toString();
  }

  public static String disambiguatedFieldName(TList<FieldKey> fields, int index) {
    var base = sanitizeFieldName(fields.get(index).name());
    for (int j = 0; j < index; j++) {
      if (sanitizeFieldName(fields.get(j).name()).equals(base)) {
        return base + "$" + index;
      }
    }
    return base;
  }

  static String binaryNameFor(StructuralKey key) {
    return BINARY_NAME_PREFIX + key.arity() + "$" + Integer.toHexString(key.hashCode());
  }

  public static Class<?> getOrEmit(TList<FieldKey> fields) {
    var key = new StructuralKey(fields);
    var existing = CACHE.getIfPresent(key);
    if (existing != null) {
      return existing;
    }

    var binaryName = binaryNameFor(key);
    var bytecode = emitBytecode(binaryName, fields);
    var loader =
        new RecordClassLoader(RecordClassEmitter.class.getClassLoader(), binaryName, bytecode);
    Class<?> defined;
    try {
      defined = loader.loadClass(binaryName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError(
          "RecordClassEmitter: freshly-defined class not loadable: " + binaryName, e);
    }

    var winner = CACHE.get(key, k -> defined);
    CACHE_BY_NAME.put(winner.getName(), winner);
    return winner;
  }

  public static String internalNameFor(TList<FieldKey> fields) {
    return getOrEmit(fields).getName().replace('.', '/');
  }

  private static final JavaMethodDescriptor MDESC_OBJECT_ARRAY_CTOR =
      JavaMethodDescriptor.fromConstructorParts(JvmConstants.DESC_OBJECT.array());

  static byte[] emitBytecode(String binaryName, TList<FieldKey> fields) {
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

    for (int i = 0; i < fields.size(); i++) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
              disambiguatedFieldName(fields, i),
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

    for (int i = 0; i < fields.size(); i++) {
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 1));
      seq.add(JavaBytecodeUtils.literalIntShortByte(loc, i));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.AALOAD));
      seq.add(
          JavaBytecodeUtils.setField(
              loc, ownName, disambiguatedFieldName(fields, i), JvmConstants.DESC_OBJECT));
    }
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.RETURN));
    JavaBytecodeUtils.renderMethod(seq, ctor, TList.of());

    cw.visitEnd();
    return cw.toByteArray();
  }

  private static final class RecordClassLoader extends ClassLoader {
    private final String binaryName;
    private final byte[] bytecode;

    RecordClassLoader(ClassLoader parent, String binaryName, byte[] bytecode) {
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
