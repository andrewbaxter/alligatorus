package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class JavaBytecodeRenderRefs {
    private final JavaInternalName owner;
    private final String prefix;
    private final TOrderedMap<BindingId, JavaStaticFieldRef> fields = new TOrderedMap<>();
    private final TList<EvalContext.ExternalEntry> externals = new TList<>();
    private final TList<JavaStaticFieldRef> fieldRefs = new TList<>();
    private final TOrderedMap<DefSetId, DefinitionSet> defSets = new TOrderedMap<>();

    public JavaBytecodeRenderRefs(JavaInternalName owner, String prefix) {
        this.owner = owner;
        this.prefix = prefix;
    }

    JavaStaticFieldRef external(EvalContext.ExternalEntry entry) {
        var existing = fields.getOpt(entry.key());
        if (existing.optIsSome()) return existing.optUnwrap();
        var ref = new JavaStaticFieldRef(
                owner,
                prefix + Prefixes.EXTERNAL_PREFIX + externals.size(),
                JavaDataDescriptor.fromClass(entry.external().externalHostClass()));
        fields.put(entry.key(), ref);
        fieldRefs.add(ref);
        externals.add(entry);
        return ref;
    }

    void defSet(DefinitionSet set) {
        defSets.putIfAbsent(set.defSetId, set);
    }

    public boolean isEmpty() {
        return externals.isEmpty() && defSets.isEmpty();
    }

    public TList<EvalContext.ExternalEntry> externals() {
        return externals;
    }

    public TOrderedMap<DefSetId, DefinitionSet> defSets() {
        return defSets;
    }

    public void declareFields(ClassWriter cw, TOrderedMap<String, Object> staticFieldValues) {
        for (int i = 0; i < externals.size(); i++) {
            var ref = fieldRefs.get(i);
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, ref.name, ref.descriptor.value(), null, null)
                    .visitEnd();
            staticFieldValues.put(ref.name, externals.get(i).external().externalHostValue());
        }
    }
}
