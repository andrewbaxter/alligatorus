package compilerxyz.eval.value.source_object;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import org.jspecify.annotations.Nullable;

public final class SourceObjectData {
  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final @Nullable String name;
  public @Nullable String description;
  public @Nullable Type underlyingType;
  public final Unique identity;
  public final String dataField;
  public final TList<SourceDataMethodData> methods;
  public final TList<SourceObjectInterfaceImplData> interfaceImpls;
  public @Nullable ArtifactIdSource artifactId;
  public @Nullable ArtifactIdSource newArtifactId;

  public @Nullable String compiledBinaryName;

  public byte @Nullable [] compiledBytecode;

  public @Nullable TOrderedMap<String, Object> compiledStaticFieldValues;

  public SourceObjectData(
      DefSetId setId, @Nullable String name, Unique identity, String dataField, DefinitionSet set) {
    this.setId = setId;
    this.set = set;
    this.name = name;
    this.identity = identity;
    this.dataField = dataField;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public SourceObjectData(
      @Nullable DefSetId setId, @Nullable String name, Unique identity, String dataField) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.name = name;
    this.identity = identity;
    this.dataField = dataField;
    this.methods = new TList<>();
    this.interfaceImpls = new TList<>();
  }

  public EResult<Void> finish(
      QExprId loc,
      long objectIndex,
      DefinitionSetClassLoader loader,
      TOrderedMap<String, Object> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    if (compiledBinaryName != null) {
      return EResult.ok(null);
    }
    if (setId == null) {
      return EResult.err(
          loc,
          "SourceObjectData.finish: setId is null for object '"
              + (name == null ? "(unnamed)" : name)
              + "'");
    }
    var emitted =
        SourceObjectEmitter.emit(setId, objectIndex, this, builtinDynamics, staticFunctionDefs);
    compiledBinaryName = emitted.binaryName();
    compiledBytecode = emitted.bytecode();
    compiledStaticFieldValues = emitted.staticFieldValues();
    loader.registerClass(emitted.binaryName(), emitted.bytecode());
    if (!emitted.staticFieldValues().isEmpty()) {
      loader.registerExternals(emitted.binaryName(), emitted.staticFieldValues());
    }
    return EResult.ok(null);
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = SourceObjectData.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "SourceObjectData.initSetReflectively: failed to write final 'set' field", e);
    }
  }
}
