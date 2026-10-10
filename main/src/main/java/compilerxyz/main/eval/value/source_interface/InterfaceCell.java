package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.object.ObjectAspectSource;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6005L)
public final class InterfaceCell {
    public sealed interface State {
        record Building(InterfaceBuilding building) implements State {}

        record Frozen(InterfaceData data) implements State {}
    }

    public final Opt<DefinitionSet> set;

    public final Unique identity;

    public final String binaryName;

    private State state;

    public Opt<byte[]> compiledBytecode = Opt.none();

    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public InterfaceCell(Opt<DefinitionSet> set, Unique identity, String binaryName, State state) {
        this.set = set;
        this.identity = identity;
        this.binaryName = binaryName;
        this.state = state;
    }

    public static InterfaceCell ofBuiltin(InterfaceData data) {
        if (data.builtinType.optIsNone()) {
            throw new IllegalStateException("InterfaceCell::ofBuiltin: interface data has no builtin type");
        }
        var type = data.builtinType.optUnwrap();
        var aspSrcOpt = type.aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(type.typeDesc() + "::ofBuiltin: missing aspect source");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static st -> staticSrc = st;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(type.typeDesc() + "::ofBuiltin: missing static aspect source");
        }
        if (type.interfaceRepresented().optIsNone()) {
            throw new IllegalStateException(type.typeDesc() + "::ofBuiltin: does not represent an interface");
        }
        return new InterfaceCell(
                Opt.none(),
                staticSrc.identity(),
                type.interfaceRepresented().optUnwrap().value().replace('/', '.'),
                new State.Frozen(data));
    }

    public InterfaceBuilding building() {
        return switch (state) {
            case State.Building b -> b.building();
            case State.Frozen f ->
                throw new IllegalStateException("InterfaceCell::building: interface is already finished");
        };
    }

    public InterfaceData frozen() {
        return switch (state) {
            case State.Building b ->
                throw new IllegalStateException("InterfaceCell::frozen: interface definition set is not finished");
            case State.Frozen f -> f.data();
        };
    }

    public void markImplemented() {
        switch (state) {
            case State.Building b -> b.building().implemented = true;
            case State.Frozen f -> {}
        }
    }

    public void freeze(InterfaceData data) {
        switch (state) {
            case State.Building b -> state = new State.Frozen(data);
            case State.Frozen f -> throw new IllegalStateException("InterfaceCell::freeze: already frozen");
        }
    }

    public boolean isAspect() {
        return switch (state) {
            case State.Building b -> b.building().isAspect;
            case State.Frozen f -> f.data().isAspect;
        };
    }

    public Opt<Unique> aspectId() {
        return switch (state) {
            case State.Building b -> b.building().aspectId;
            case State.Frozen f -> f.data().aspectId;
        };
    }

    public TList<String> methodNames() {
        var out = new TList<String>();
        switch (state) {
            case State.Building b -> {
                for (var m : b.building().methods) out.add(m.name);
            }
            case State.Frozen f -> {
                for (var m : f.data().methods) out.add(m.name);
            }
        }
        return out;
    }

    public Opt<InterfaceData> frozenOpt() {
        return switch (state) {
            case State.Building b -> Opt.none();
            case State.Frozen f -> Opt.some(f.data());
        };
    }

    public Opt<String> description() {
        return switch (state) {
            case State.Building b -> b.building().description;
            case State.Frozen f -> f.data().description;
        };
    }

    public JavaInternalName jvmInternalNameForSet(DefinitionSet dependent) {
        if (set.optIsSome()) {
            dependent.addDependency(set.optUnwrap());
        }
        return internalName();
    }

    public JavaInternalName internalName() {
        return JavaInternalName.of(binaryName.replace('.', '/'));
    }

    public void initSetReflectively(DefinitionSet set) {
        try {
            var f = InterfaceCell.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(this, Opt.some(set));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("InterfaceCell.initSetReflectively: failed to write final 'set' field", e);
        }
    }
}
