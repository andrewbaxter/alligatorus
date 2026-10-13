package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6478L)
public record Imported(Opt<Type> type, Object data) {

    public static Imported of(Object data) {
        return new Imported(Opt.none(), data);
    }

    public static EResult<Imported> ok(Object data) {
        return EResult.ok(of(data));
    }

    public static EResult<Imported> okValue(Type type, Object data) {
        return EResult.ok(new Imported(Opt.some(type), data));
    }

    public Value asValue() {
        if (data instanceof Value value) return value;
        if (type.optIsNone()) {
            throw new IllegalStateException("imported " + data.getClass().getName() + " is not a value");
        }
        return new ValueObjectConst(type.optUnwrap(), data);
    }

    public Object host() {
        if (data instanceof Value && !(data instanceof ValueObjectConst)) {
            throw new IllegalStateException(
                    "imported " + data.getClass().getName() + " has no host form to hold in a constant collection");
        }
        return data;
    }

    public Type asType() {
        return MustCast.mustCast(Type.class, data);
    }

    public ArtifactExporter asExporter() {
        return MustCast.mustCast(ArtifactExporter.class, data);
    }
}
