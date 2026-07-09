package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.DefinitionSetClassLoader;
import compilerxyz.eval.EResult;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.SourceInterfaceEmitter;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class InterfaceData {
  public @Nullable DefSetId setId;

  @SuppressWarnings("NullAway.Init")
  public final DefinitionSet set;
  public final Unique identity;
  public final @Nullable BuiltinInterfaceKind builtinKind;
  public @Nullable String description;
  public boolean isAspect;
  public final TList<InterfaceMethodData> methods;
  public @Nullable ArtifactIdSource artifactId;

  public @Nullable String compiledBinaryName;

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
          loc, "InterfaceData.finish: setId is null for user-declared interface '" + name + "'");
    }
    var emitted = SourceInterfaceEmitter.emit(setId, interfaceIndex, this);
    compiledBinaryName = emitted.binaryName();
    compiledBytecode = emitted.bytecode();
    loader.registerClass(emitted.binaryName(), emitted.bytecode());
    return EResult.ok(null);
  }

  public static Type dynamicType() {
    return InterfaceDataDynamicType.INSTANCE;
  }

  static final class InterfaceDataDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private InterfaceDataDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/eval/value/source_interface/InterfaceData");
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenIdentities.INTERFACE_REF_TYPE_IDENTITY);
      var castRules = new TList<TypeObjectCastRule>();
      castRules.add(new TypeObjectCastRule.RestampToIdentity(GenIdentities.INTERFACE_REF_TYPE_IDENTITY));
      castRules.add(new TypeObjectCastRule.RestampToIdentity(GenIdentities.INTERFACE_CONST_IDENTITY));
      castRules.add(
          new TypeObjectCastRule.CallByDestIdentity(
              GenIdentities.TYPE_TYPE_IDENTITY, DynRuntime.INTERFACE_REF_TO_TYPE_TYPE));
      castRules.add(
          new TypeObjectCastRule.CallToInterfaceSpec(DynRuntime.INTERFACE_REF_TO_INTERFACE_SPEC));
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_SOURCE_INTERFACE_DYNAMIC_TYPE_ID,
          "definition_set_interface_ref",
          jvmClass,
          JavaDataDescriptor.fromClass(InterfaceData.class),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          castRules,
          new TList<compilerxyz.eval.value.object_.TypeObjectCastFromRule>(),
          Opt.<compilerxyz.eval.value.object_.TypeObjectLift>none(),
          Opt.<ObjectAspectSource>some(new ObjectAspectSource.FromInterfaceData()),
          Opt.<JavaInternalName>none());
    }
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
