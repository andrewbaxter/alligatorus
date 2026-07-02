package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class SourceInterfaceData {
  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final @Nullable BuiltinInterfaceKind builtinKind;
  public @Nullable String description;
  public boolean isAspect;
  public final TList<SourceInterfaceMethodData> methods;
  public @Nullable ArtifactIdSource artifactId;

  public @Nullable String compiledBinaryName;

  public byte @Nullable [] compiledBytecode;

  public SourceInterfaceData(
      DefSetId setId,
      Unique identity,
      @Nullable BuiltinInterfaceKind builtinKind,
      DefinitionSet set) {
    this.setId = setId;
    this.set = set;
    this.identity = identity;
    this.builtinKind = builtinKind;
    this.methods = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public SourceInterfaceData(
      @Nullable DefSetId setId, Unique identity, @Nullable BuiltinInterfaceKind builtinKind) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.identity = identity;
    this.builtinKind = builtinKind;
    this.methods = new TList<>();
  }

  public EResult<Void> finish(
      QExprId loc, long interfaceIndex, DefinitionSetClassLoader loader) {
    var name = description != null ? description : "(unnamed)";
    for (var method : methods) {
      if (method.argType == null) {
        return EResult.err(
            loc,
            "Interface '" + name + "' method '" + method.name + "' is missing an arg type");
      }
      if (method.retType == null) {
        return EResult.err(
            loc,
            "Interface '" + name + "' method '" + method.name + "' is missing a return type");
      }
    }
    if (builtinKind != null) {
      return EResult.ok(null);
    }
    if (compiledBinaryName != null) {
      return EResult.ok(null);
    }
    if (setId == null) {
      return EResult.err(
          loc, "SourceInterfaceData.finish: setId is null for user-declared interface '" + name + "'");
    }
    var emitted = SourceInterfaceEmitter.emit(setId, interfaceIndex, this);
    compiledBinaryName = emitted.binaryName();
    compiledBytecode = emitted.bytecode();
    loader.registerClass(emitted.binaryName(), emitted.bytecode());
    return EResult.ok(null);
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = SourceInterfaceData.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "SourceInterfaceData.initSetReflectively: failed to write final 'set' field", e);
    }
  }
}
