package compilerxyz.eval;

import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import compilerxyz.langhelp.GlobalContext;

public final class DefinitionSetClassLoader extends ClassLoader {

  private final GlobalContext gx;
  private final TList<DefinitionSetClassLoader> dependencyLoaders;
  private final TMap<String, byte[]> pendingBytecode = new TMap<>();
  private final TMap<String, TOrderedMap<String, Object>> pendingExternals = new TMap<>();

  public DefinitionSetClassLoader(
      GlobalContext gx,
      ClassLoader parent,
      TList<DefinitionSetClassLoader> dependencyLoaders) {
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

  public void registerExternals(String binaryName, TOrderedMap<String, Object> staticFieldValues) {
    pendingExternals.put(binaryName, staticFieldValues);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    var bytes = pendingBytecode.getOrNull(name);
    if (bytes != null) {
      var defined = defineClass(name, bytes, 0, bytes.length);
      var externals = pendingExternals.getOrNull(name);
      if (externals != null) {
        for (var entry : externals.entrySet()) {
          Field field;
          try {
            field = defined.getDeclaredField(entry.getKey());
          } catch (NoSuchFieldException e) {
            throw new AssertionError(
                "External field "
                    + entry.getKey()
                    + " not declared on generated class "
                    + name,
                e);
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
    if (name.startsWith(compilerxyz.eval.dynamic.CompositeClassEmitter.BINARY_NAME_PREFIX)) {
      var slotDescs = compilerxyz.eval.dynamic.CompositeClassEmitter.decodeBinaryName(name);
      var composite =
          compilerxyz.eval.dynamic.CompositeClassEmitter.emitBytecode(name, slotDescs);
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

}
