package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefinitionSetClassLoader;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class CompositeClassEmitter {
    private CompositeClassEmitter() {}

    public static final String BINARY_NAME_PREFIX = "compilerxyz.main.eval.gen.Composite$";

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
        return JvmSort.descriptor(xx, t);
    }

    public static Class<?> getOrEmit(ExprContext xx, TList<Type> sortedTypes) {
        var slotDescs = new TList<JavaDataDescriptor>(sortedTypes.size());
        for (var t : sortedTypes) slotDescs.add(slotDescriptor(xx, t));
        var _bnfSb = new StringBuilder(BINARY_NAME_PREFIX);
        _bnfSb.append(slotDescs.size());
        for (var _bnfDesc : slotDescs) {
            _bnfSb.append("#x");
            var _bnfD = _bnfDesc.value();
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
        var cache = xx.vx.ix.compositeCache;
        var existing = cache.get(binaryName);
        if (existing != null) {
            return existing;
        }

        if (xx.vx.ix.classLoader.optIsNone()) {
            xx.vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                    xx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), new TList<>()));
        }
        var loader = xx.vx.ix.classLoader.optUnwrap();
        var bytecode = emitBytecode(binaryName, slotDescs);
        loader.registerClass(binaryName, bytecode);
        Class<?> defined;
        try {
            defined = Class.forName(binaryName, true, loader);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("CompositeClassEmitter: freshly-registered class not loadable: " + binaryName, e);
        }
        cache.put(binaryName, defined);
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
            cw.visitField(Opcodes.ACC_PUBLIC, slotFieldName(i), slotDescs.get(i).value(), null, null)
                    .visitEnd();
        }

        var ctor = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                JavaMethodDescriptor.fromConstructorParts(slotDescs).value(),
                null,
                null);
        ctor.visitCode();

        var loc = CodeLoc.Infallible.INSTANCE;
        var seq = JavaBytecodeUtils.seq();
        seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
        seq.add(JavaBytecodeUtils.invokeSpecial(
                loc, JvmConstants.INTNAME_OBJECT, "<init>", JavaMethodDescriptor.VOID_NOARGS));

        int localSlot = 1;
        for (int i = 0; i < slotDescs.size(); i++) {
            var desc = slotDescs.get(i);
            seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
            seq.add(JavaBytecodeUtils.varInsn(loc, desc.loadOpcode(), localSlot));
            seq.add(JavaBytecodeUtils.setField(loc, ownName, slotFieldName(i), desc));
            localSlot += desc.slotWidth();
        }
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.RETURN));
        JavaBytecodeUtils.renderMethod(seq, ctor, TList.of());

        cw.visitEnd();
        return cw.toByteArray();
    }
}
