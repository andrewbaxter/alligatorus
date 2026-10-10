package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public abstract class MetasBase implements Metas {
    private final Set<Meta> inProgress = Collections.newSetFromMap(new IdentityHashMap<>());

    protected abstract MetaId metasBaseAllocate();

    protected abstract void metasBaseStore(MetaId id, String kind, JsonValue body);

    @Override
    public final synchronized MetaId metasIntern(ExprContext xx, Meta meta) {
        var existing = meta.metaAssignedId(this);
        if (existing.optIsSome()) return existing.optUnwrap();
        if (inProgress.contains(meta)) {
            var id = metasBaseAllocate();
            meta.metaAssign(this, id);
            return id;
        }
        inProgress.add(meta);
        try {
            var fields = new TOrderedMap<String, JsonValue>();
            for (var entry : meta.fields.entrySet()) {
                var field = new TOrderedMap<String, JsonValue>();
                field.put("data", entry.getValue().data());
                var fieldMeta = entry.getValue().meta();
                if (fieldMeta.optIsSome()) {
                    var fieldId = metasIntern(xx, fieldMeta.optUnwrap().apply(xx));
                    field.put("meta", new JsonValueNumberInt(fieldId.value()));
                }
                fields.put(entry.getKey(), new JsonValueObject(field));
            }
            var body = new TOrderedMap<String, JsonValue>();
            body.put("kind", new JsonValueString(meta.kind));
            body.put("data", meta.data);
            body.put("fields", new JsonValueObject(fields));
            var assignedInCycle = meta.metaAssignedId(this);
            var id = assignedInCycle.optIsSome() ? assignedInCycle.optUnwrap() : metasBaseAllocate();
            meta.metaAssign(this, id);
            metasBaseStore(id, meta.kind, new JsonValueObject(body));
            return id;
        } finally {
            inProgress.remove(meta);
        }
    }
}
