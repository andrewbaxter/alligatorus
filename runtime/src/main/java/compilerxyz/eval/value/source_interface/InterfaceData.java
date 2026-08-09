package compilerxyz.eval.value.source_interface;

import compilerxyz.annotations.BuiltinFull;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

@BuiltinFull(uniqueId = 6005L)
public final class InterfaceData {
  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final @Nullable BuiltinInterfaceKind builtinKind;
  public @Nullable String description;
  public boolean isAspect;
  public @Nullable Unique aspectId;
  public final TList<InterfaceMethodData> methods;
  public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

  @SuppressWarnings("NullAway.Init")
  public String compiledBinaryName;

  public byte @Nullable [] compiledBytecode;

  public InterfaceData(
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
  public InterfaceData(
      @Nullable DefSetId setId, Unique identity, @Nullable BuiltinInterfaceKind builtinKind) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.identity = identity;
    this.builtinKind = builtinKind;
    this.methods = new TList<>();
  }

  public EResult<Void> finish(QExprId loc, long interfaceIndex) {
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
    if (compiledBytecode != null) {
      return EResult.ok(null);
    }
    if (setId == null) {
      return EResult.err(
          loc, "InterfaceData.finish: setId is null for user-declared interface '" + name + "'");
    }
    var emitted = SourceInterfaceEmitter.emit(setId, interfaceIndex, this);
    compiledBinaryName = emitted.binaryName();
    compiledBytecode = emitted.bytecode();
    return EResult.ok(null);
  }

  public void initSetReflectively(DefinitionSet set) {
    try {
      var f = InterfaceData.class.getDeclaredField("set");
      f.setAccessible(true);
      f.set(this, set);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(
          "InterfaceData.initSetReflectively: failed to write final 'set' field", e);
    }
  }
}
