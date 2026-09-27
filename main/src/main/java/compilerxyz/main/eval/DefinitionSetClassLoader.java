package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class DefinitionSetClassLoader extends ClassLoader {

    private final GlobalContext gx;
    private final TList<DefinitionSetClassLoader> dependencyLoaders;
    private final TMap<String, byte[]> pendingBytecode = new TMap<>();
    private final TMap<String, TOrderedMap<String, Object>> pendingExternals = new TMap<>();
    private final TMap<String, IList<QExprId>> pendingLocs = new TMap<>();

    public DefinitionSetClassLoader(
            GlobalContext gx, ClassLoader parent, TList<DefinitionSetClassLoader> dependencyLoaders) {
        super(parent);
        this.gx = gx;
        this.dependencyLoaders = dependencyLoaders;
    }

    public void registerClass(String binaryName, byte[] bytecode) {
        if (gx.captureBytecode) {
            gx.capturedBytecode.put(binaryName, bytecode);
        }
        pendingBytecode.put(binaryName, bytecode);
    }

    public void registerLocs(String binaryName, IList<QExprId> locs) {
        pendingLocs.put(binaryName, locs);
    }

    public void registerExternals(String binaryName, TOrderedMap<String, Object> staticFieldValues) {
        pendingExternals.put(binaryName, staticFieldValues);
    }

    public Opt<QExprId> locate(StackTraceElement frame) {
        if (pendingBytecode.getOpt(frame.getClassName()).optIsNone()) {
            for (var dep : dependencyLoaders) {
                var found = dep.locate(frame);
                if (found.optIsSome()) return found;
            }
            return Opt.none();
        }
        var locsOpt = pendingLocs.getOpt(frame.getClassName());
        if (locsOpt.optIsNone()) return Opt.none();
        var locs = locsOpt.optUnwrap();
        var line = frame.getLineNumber();
        if (line < 1 || line > locs.size()) return Opt.none();
        return Opt.some(locs.get(line - 1));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        var bytesOpt = pendingBytecode.getOpt(name);
        if (bytesOpt.optIsSome()) {
            var bytes = bytesOpt.optUnwrap();
            var defined = defineClass(name, bytes, 0, bytes.length);
            var externalsOpt = pendingExternals.getOpt(name);
            if (externalsOpt.optIsSome()) {
                var externals = externalsOpt.optUnwrap();
                for (var entry : externals.entrySet()) {
                    Field field;
                    try {
                        field = defined.getDeclaredField(entry.getKey());
                    } catch (NoSuchFieldException e) {
                        throw new AssertionError(
                                "External field " + entry.getKey() + " not declared on generated class " + name, e);
                    }
                    if (!Modifier.isStatic(field.getModifiers())) {
                        throw new AssertionError("External field " + entry.getKey() + " must be static");
                    }
                    field.setAccessible(true);
                    try {
                        field.set(null, entry.getValue());
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            }
            return defined;
        }
        if (name.startsWith(CompositeClassEmitter.BINARY_NAME_PREFIX)) {
            var rest = name.substring(CompositeClassEmitter.BINARY_NAME_PREFIX.length());
            int i = 0;
            while (i < rest.length() && Character.isDigit(rest.charAt(i))) i++;
            if (i == 0) {
                throw new IllegalArgumentException("CompositeClassEmitter::decodeBinaryName: missing arity: " + name);
            }
            var arity = Integer.parseInt(rest.substring(0, i));
            var slotDescs = new TList<JavaDataDescriptor>(arity);
            Opt<StringBuilder> current = Opt.none();
            while (i < rest.length()) {
                char c = rest.charAt(i);
                if (c == '#') {
                    i++;
                    if (i >= rest.length()) {
                        throw new IllegalArgumentException(
                                "CompositeClassEmitter::decodeBinaryName: dangling escape: " + name);
                    }
                    char dbnE = rest.charAt(i);
                    switch (dbnE) {
                        case 'x' -> {
                            if (current.optIsSome()) {
                                slotDescs.add(JavaDataDescriptor.create(
                                        current.optUnwrap().toString()));
                            }
                            current = Opt.some(new StringBuilder());
                        }
                        case '#' -> slotBuilder(current, name).append('#');
                        case 's' -> slotBuilder(current, name).append('/');
                        case 'e' -> slotBuilder(current, name).append(';');
                        case 'a' -> slotBuilder(current, name).append('[');
                        default ->
                            throw new IllegalArgumentException(
                                    "CompositeClassEmitter::decodeBinaryName: unknown escape #" + dbnE + " in " + name);
                    }
                } else {
                    slotBuilder(current, name).append(c);
                }
                i++;
            }
            if (current.optIsSome())
                slotDescs.add(JavaDataDescriptor.create(current.optUnwrap().toString()));
            if (slotDescs.size() != arity) {
                throw new IllegalArgumentException("CompositeClassEmitter::decodeBinaryName: arity "
                        + arity
                        + " does not match "
                        + slotDescs.size()
                        + " slots: "
                        + name);
            }
            var composite = CompositeClassEmitter.emitBytecode(name, slotDescs);
            return defineClass(name, composite, 0, composite.length);
        }
        for (var dep : dependencyLoaders) {
            try {
                return dep.loadClass(name);
            } catch (ClassNotFoundException ignored) {

            }
        }
        throw new ClassNotFoundException(name);
    }

    private static StringBuilder slotBuilder(Opt<StringBuilder> current, String name) {
        if (current.optIsNone()) {
            throw new IllegalArgumentException(
                    "CompositeClassEmitter::decodeBinaryName: descriptor before first slot marker: " + name);
        }
        return current.optUnwrap();
    }
}
