package compilerxyz.eval;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.eval.dynamic.CompositeClassEmitter;

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
    if (name.startsWith(CompositeClassEmitter.BINARY_NAME_PREFIX)) {
      var _dbnRest =
          name.substring(CompositeClassEmitter.BINARY_NAME_PREFIX.length());
      int _dbnI = 0;
      while (_dbnI < _dbnRest.length() && Character.isDigit(_dbnRest.charAt(_dbnI))) _dbnI++;
      if (_dbnI == 0) {
        throw new IllegalArgumentException(
            "CompositeClassEmitter::decodeBinaryName: missing arity: " + name);
      }
      var _dbnArity = Integer.parseInt(_dbnRest.substring(0, _dbnI));
      var _dbnSlotDescs =
          new TList<JavaDataDescriptor>(_dbnArity);
      StringBuilder _dbnCurrent = null;
      while (_dbnI < _dbnRest.length()) {
        char _dbnC = _dbnRest.charAt(_dbnI);
        if (_dbnC == '#') {
          _dbnI++;
          if (_dbnI >= _dbnRest.length()) {
            throw new IllegalArgumentException(
                "CompositeClassEmitter::decodeBinaryName: dangling escape: " + name);
          }
          char _dbnE = _dbnRest.charAt(_dbnI);
          switch (_dbnE) {
            case 'x' -> {
              if (_dbnCurrent != null)
                _dbnSlotDescs.add(
                    JavaDataDescriptor.create(_dbnCurrent.toString()));
              _dbnCurrent = new StringBuilder();
            }
            case '#' -> _dbnCurrent.append('#');
            case 's' -> _dbnCurrent.append('/');
            case 'e' -> _dbnCurrent.append(';');
            case 'a' -> _dbnCurrent.append('[');
            default ->
                throw new IllegalArgumentException(
                    "CompositeClassEmitter::decodeBinaryName: unknown escape #"
                        + _dbnE
                        + " in "
                        + name);
          }
        } else {
          if (_dbnCurrent == null) {
            throw new IllegalArgumentException(
                "CompositeClassEmitter::decodeBinaryName: descriptor before first slot marker: "
                    + name);
          }
          _dbnCurrent.append(_dbnC);
        }
        _dbnI++;
      }
      if (_dbnCurrent != null)
        _dbnSlotDescs.add(
            JavaDataDescriptor.create(_dbnCurrent.toString()));
      if (_dbnSlotDescs.size() != _dbnArity) {
        throw new IllegalArgumentException(
            "CompositeClassEmitter::decodeBinaryName: arity "
                + _dbnArity
                + " does not match "
                + _dbnSlotDescs.size()
                + " slots: "
                + name);
      }
      var composite =
          CompositeClassEmitter.emitBytecode(name, _dbnSlotDescs);
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
