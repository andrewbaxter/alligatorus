package compilerxyz.main.meta;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public abstract class MetasBase implements Metas {
    private final TMap<String, MetaId> structural = new TMap<>();
    private final Set<Meta> inProgress = Collections.newSetFromMap(new IdentityHashMap<>());

    protected abstract MetaId metasBaseAllocate();

    protected abstract void metasBaseStore(MetaId id, String kind, String structuralKey, JsonValue body);

    protected Opt<MetaId> metasBaseFindStructural(String structuralKey) {
        return Opt.none();
    }

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
            var key = new StringBuilder();
            key.append(meta.kind)
                    .append('\n')
                    .append(JsonValues.serialize(meta.data))
                    .append('\n');
            for (var entry : meta.fields.entrySet()) {
                var field = new TOrderedMap<String, JsonValue>();
                field.put("data", entry.getValue().data());
                var dataJson = JsonValues.serialize(entry.getValue().data());
                key.append(entry.getKey()).append('=').append(dataJson);
                var fieldMeta = entry.getValue().meta();
                if (fieldMeta.optIsSome()) {
                    var fieldId = metasIntern(xx, fieldMeta.optUnwrap().apply(xx));
                    field.put("meta", new JsonValueNumberInt(fieldId.value()));
                    key.append('#').append(fieldId.value());
                }
                key.append('\n');
                fields.put(entry.getKey(), new JsonValueObject(field));
            }
            var structuralKey = key.toString();
            var body = new TOrderedMap<String, JsonValue>();
            body.put("kind", new JsonValueString(meta.kind));
            body.put("data", meta.data);
            body.put("fields", new JsonValueObject(fields));
            var assignedInCycle = meta.metaAssignedId(this);
            if (assignedInCycle.optIsSome()) {
                var id = assignedInCycle.optUnwrap();
                metasBaseStore(id, meta.kind, structuralKey, new JsonValueObject(body));
                return id;
            }
            var known = structural.getOpt(structuralKey);
            if (known.optIsNone()) known = metasBaseFindStructural(structuralKey);
            if (known.optIsSome()) {
                structural.put(structuralKey, known.optUnwrap());
                meta.metaAssign(this, known.optUnwrap());
                return known.optUnwrap();
            }
            var id = metasBaseAllocate();
            meta.metaAssign(this, id);
            structural.put(structuralKey, id);
            metasBaseStore(id, meta.kind, structuralKey, new JsonValueObject(body));
            return id;
        } finally {
            inProgress.remove(meta);
        }
    }
}
