package compilerxyz.eval.value.source_interface;

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
              GenIdentities.TYPE_TYPE_IDENTITY,
              new TypeObjectCastRule.DynRuntimeHelper(
                  compilerxyz.eval.dynamic.JavaInternalName.fromClass(TypeSourceInterfaceConst.class),
                  "fromInterfaceData",
                  JavaMethodDescriptor.fromParts(
                      JavaDataDescriptor.fromClass(compilerxyz.eval.Type.class),
                      TList.of(
                          JavaDataDescriptor.fromClass(compilerxyz.eval.ExprContext.class),
                          JavaDataDescriptor.fromClass(InterfaceData.class))),
                  true)));
      castRules.add(
          new TypeObjectCastRule.CallToInterfaceSpec(
              new TypeObjectCastRule.DynRuntimeHelper(
                  compilerxyz.eval.dynamic.JavaInternalName.fromClass(
                      compilerxyz.eval.dynamic.DynRuntime.class),
                  "interfaceRefToInterfaceSpec",
                  JavaMethodDescriptor.fromParts(
                      JavaDataDescriptor.fromClass(
                          compilerxyz.eval.value.interface_spec.TypeInterfaceSpec.class),
                      TList.of(JavaDataDescriptor.fromClass(InterfaceData.class))),
                  false)));
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
