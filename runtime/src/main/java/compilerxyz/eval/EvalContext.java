package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
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
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;

@BuiltinFull(uniqueId = 6025L)
public final class EvalContext {

  public final TMap<DefSetId, DefinitionSet> definitionSets = new TMap<>();
  public final TList<ExternalEntry> externals = new TList<>();
  public final CompileImportContext ix;
  public final Counter nextLocalId = new Counter();

  public final Counter uniqueCounter = new Counter();

  /**
   * Mints drop-state ids for scope levels. Ids are unique across the whole
   * eval so states minted on divergent branch paths never collide in the
   * shared per-level chain lookups.
   */
  public final Counter dropStateCounter = new Counter();

  public final TList<LogEntry> logs = new TList<>();

  public String evalRootInternalName;

  public EvalContext(CompileImportContext ix) {
    this.ix = ix;
  }

  public record ExternalEntry(
      JavaBytecodeBindingKey key, Type type, External external) {}
}
