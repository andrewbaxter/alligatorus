package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;

@Builtin(uniqueId = 6025L)
public final class EvalContext {

  public static Type dynamicType() {
    return EvalContextDynamicType.INSTANCE;
  }

  static final class EvalContextDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private EvalContextDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.fromClass(EvalContext.class);
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.EVAL_CONTEXT_IDENTITY);
      var castFromRules = new TList<TypeObjectCastFromRule>();
      castFromRules.add(
          new TypeObjectCastFromRule.RestampFromIdentity(
              GenTrivialIdentityAspects.EVAL_CONTEXT_IDENTITY));
      return new TypeObjectDynamic(
          new TypeObjectMeta(
              "EvalContext",
              trivialIdentityAspects,
              new TList<TypeObjectCastRule>(),
              castFromRules,
              Opt.<ObjectAspectSource>none()),
          jvmClass,
          JavaDataDescriptor.fromClass(EvalContext.class),
          EvalContext.class,
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());
    }
  }
  public final TMap<DefSetId, DefinitionSet> definitionSets = new TMap<>();
  public final TList<ExternalEntry> externals = new TList<>();
  public final CompileImportContext ix;
  public final Counter nextLocalId = new Counter();

  public final Counter uniqueCounter = new Counter();

  public final TList<LogEntry> logs = new TList<>();

  public String evalRootInternalName;

  public EvalContext(CompileImportContext ix) {
    this.ix = ix;
  }

  public record ExternalEntry(
      compilerxyz.eval.dynamic.JavaBytecodeBindingKey key, Type type, External external) {}
}
