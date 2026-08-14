package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.AspectConsumeDynamic;

public final class CompositeClassEmitter {
  private CompositeClassEmitter() {}

  public static final class StructuralKey {
    private final String[] descriptors;
    private final int hash;

    public StructuralKey(TList<JavaDataDescriptor> slotDescs) {
      this.descriptors = new String[slotDescs.size()];
      int h = 1;
      for (int i = 0; i < slotDescs.size(); i++) {
        this.descriptors[i] = slotDescs.get(i).value();
        h = 31 * h + this.descriptors[i].hashCode();
      }
      this.hash = h;
    }

    public int arity() {
      return descriptors.length;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof StructuralKey other)) return false;
      if (other.descriptors.length != descriptors.length) return false;
      for (int i = 0; i < descriptors.length; i++) {
        if (!other.descriptors[i].equals(descriptors[i])) return false;
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


  public static JavaMethodDescriptor constructorDesc(ExprContext xx, TList<Type> sortedTypes) {
    var args = new TList<JavaDataDescriptor>(sortedTypes.size());
    for (var t : sortedTypes) {
      args.add(slotDescriptor(xx, t));
    }
    return JavaMethodDescriptor.fromConstructorParts(args);
  }

  public static JavaDataDescriptor slotDescriptor(ExprContext xx, Type t) {
    return switch (compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, t)) {
      case Opt.Some<AspectConsumeDynamic> some ->
          some.value().aspectConsumeDynamicJvmClass(xx);
      case Opt.None<AspectConsumeDynamic> n ->
          throw new IllegalStateException(
              "CompositeClassEmitter: type (" + t.typeDesc() + ") does not implement AspectConsumeDynamic");
    };
  }

  public static Class<?> getOrEmit(ExprContext xx, TList<Type> sortedTypes) {
    var slotDescs = new TList<JavaDataDescriptor>(sortedTypes.size());
    for (var t : sortedTypes) slotDescs.add(slotDescriptor(xx, t));
    var key = new StructuralKey(slotDescs);
    var cache = xx.vx.ix.compositeCache;
    var existing = cache.get(key);
    if (existing != null) {
      return existing;
    }

    if (xx.vx.ix.classLoader.optIsNone()) {
      xx.vx.ix.classLoader =
          Opt.some(new DefinitionSetClassLoader(
              xx.vx.ix.gx,
              DefinitionSetClassLoader.class.getClassLoader(),
              new compilerxyz.util.TList<>()));
    }
    var loader = xx.vx.ix.classLoader.optUnwrap();
    var _bnfSb = new StringBuilder(BINARY_NAME_PREFIX);
    _bnfSb.append(key.arity());
    for (int _bnfI = 0; _bnfI < key.arity(); _bnfI++) {
      _bnfSb.append("#x");
      var _bnfD = key.descriptors[_bnfI];
      for (int _bnfJ = 0; _bnfJ < _bnfD.length(); _bnfJ++) {
        char _bnfC = _bnfD.charAt(_bnfJ);
        switch (_bnfC) {
          case '#' -> _bnfSb.append("##");
          case '/' -> _bnfSb.append("#s");
          case ';' -> _bnfSb.append("#e");
          case '[' -> _bnfSb.append("#a");
          default -> _bnfSb.append(_bnfC);
        }
      }
    }
    var binaryName = _bnfSb.toString();
    var bytecode = emitBytecode(binaryName, slotDescs);
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

  public static byte[] emitBytecode(String binaryName, TList<JavaDataDescriptor> slotDescs) {
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

    for (int i = 0; i < slotDescs.size(); i++) {
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
    for (int i = 0; i < slotDescs.size(); i++) {
      var desc = slotDescs.get(i);
      int loadOp;
      {
        var _lofV = desc.value();
        loadOp =
            (_lofV.isEmpty())
                ? Opcodes.ALOAD
                : switch (_lofV.charAt(0)) {
                  case 'D' -> Opcodes.DLOAD;
                  case 'J' -> Opcodes.LLOAD;
                  case 'F' -> Opcodes.FLOAD;
                  case 'I', 'Z', 'B', 'S', 'C' -> Opcodes.ILOAD;
                  default -> Opcodes.ALOAD;
                };
      }
      seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
      seq.add(JavaBytecodeUtils.varInsn(loc, loadOp, localSlot));
      seq.add(JavaBytecodeUtils.setField(loc, ownName, slotFieldName(i), desc));
      localSlot += desc.value().isEmpty() ? 1 : switch (desc.value().charAt(0)) { case 'D', 'J' -> 2; default -> 1; };
    }
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.RETURN));
    JavaBytecodeUtils.renderMethod(seq, ctor, TList.of());

    cw.visitEnd();
    return cw.toByteArray();
  }

}

