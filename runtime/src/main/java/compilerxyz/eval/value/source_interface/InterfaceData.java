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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6005L)
public final class InterfaceData {
  public Opt<DefSetId> setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final Opt<BuiltinInterfaceKind> builtinKind;
  public Opt<String> description;
  public boolean isAspect;
  public Opt<Unique> aspectId;
  public final TList<InterfaceMethodData> methods;
  public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

  @SuppressWarnings("NullAway.Init")
  public String compiledBinaryName;

  public Opt<byte[]> compiledBytecode;

  public InterfaceData(
      DefSetId setId,
      Unique identity,
      Opt<BuiltinInterfaceKind> builtinKind,
      DefinitionSet set) {
    this.setId = Opt.some(setId);
    this.set = set;
    this.identity = identity;
    this.builtinKind = builtinKind;
    this.description = Opt.none();
    this.aspectId = Opt.none();
    this.compiledBytecode = Opt.none();
    this.methods = new TList<>();
  }

  @SuppressWarnings("NullAway")
  public InterfaceData(
      Opt<DefSetId> setId, Unique identity, Opt<BuiltinInterfaceKind> builtinKind) {
    this.setId = setId;
    this.set = (DefinitionSet) null;
    this.identity = identity;
    this.builtinKind = builtinKind;
    this.description = Opt.none();
    this.aspectId = Opt.none();
    this.compiledBytecode = Opt.none();
    this.methods = new TList<>();
  }

  public EResult<Void> finish(QExprId loc, long interfaceIndex) {
    var name = description.optUnwrapOr("(unnamed)");
    if (builtinKind.optIsSome()) {
      return EResult.ok(null);
    }
    if (compiledBytecode.optIsSome()) {
      return EResult.ok(null);
    }
    if (setId.optIsNone()) {
      return EResult.err(
          loc, "InterfaceData.finish: setId is null for user-declared interface '" + name + "'");
    }
    var _sieBinaryName = SourceInterfaceEmitter.binaryNameFor(setId.optUnwrap(), interfaceIndex);
    var _sieInternalName = _sieBinaryName.replace('.', '/');
    var _sieCw =
        new ClassWriter(
            ClassWriter.COMPUTE_FRAMES
                | ClassWriter.COMPUTE_MAXS);
    _sieCw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC
            | Opcodes.ACC_INTERFACE
            | Opcodes.ACC_ABSTRACT,
        _sieInternalName,
        null,
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT.value(),
        null);
    for (var _sieMethod : methods) {
      if (_sieMethod.argType == null) {
        throw new IllegalStateException(
            "SourceInterfaceEmitter.emit: method '"
                + _sieMethod.name
                + "' has no arg type (finish ordering violation)");
      }
      _sieCw
          .visitMethod(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
              _sieMethod.name,
              JavaMethodDescriptor.fromParts(
                      compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT,
                      compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT)
                  .value(),
              null,
              null)
          .visitEnd();
    }
    _sieCw.visitEnd();
    compiledBinaryName = _sieBinaryName;
    compiledBytecode = Opt.some(_sieCw.toByteArray());
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
