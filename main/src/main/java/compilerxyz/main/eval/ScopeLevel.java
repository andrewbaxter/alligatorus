package compilerxyz.main.eval;

import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

@BuiltinFull(uniqueId = 6482L)
public sealed interface ScopeLevel {

    Emission scopeLevelEmission();

    @BuiltinFull(uniqueId = 6404L)
    final class Entry implements ScopeLevel {
        public final Opt<ConstKey> name;
        public Binding binding;
        public final boolean stackOnly;
        public long state;
        public long dropState;
        final Emission emission;

        public Entry(
                Opt<ConstKey> name, Binding binding, boolean stackOnly, long state, long dropState, Emission emission) {
            this.name = name;
            this.binding = binding;
            this.stackOnly = stackOnly;
            this.state = state;
            this.dropState = dropState;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    @BuiltinFull(uniqueId = 6421L)
    final class Temp implements ScopeLevel {
        public final Value value;
        public long state;
        public long dropState;
        final Emission emission;

        public Temp(Value value, long state, long dropState, Emission emission) {
            this.value = value;
            this.state = state;
            this.dropState = dropState;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    @BuiltinFull(uniqueId = 6403L)
    final class Labeled implements ScopeLevel {
        public final Opt<String> label;
        public final boolean isBlock;
        public final JumpKey end;
        public final Landing landing;
        final Emission emission;

        public Labeled(Opt<String> label, boolean isBlock, JumpKey end, Landing landing, Emission emission) {
            this.label = label;
            this.isBlock = isBlock;
            this.end = end;
            this.landing = landing;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    @BuiltinFull(uniqueId = 6503L)
    final class Catch implements ScopeLevel {
        public final JumpKey handler;
        final Emission emission;

        public Catch(JumpKey handler, Emission emission) {
            this.handler = handler;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    @BuiltinFull(uniqueId = 6504L)
    final class Finally implements ScopeLevel {
        public final TList<FinallyPending> pending;

        final Emission emission;

        public Finally(TList<FinallyPending> pending, Emission emission) {
            this.pending = pending;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    @BuiltinFull(uniqueId = 6531L)
    record FinallyPending(JumpKey self, DestKey key) {}

    @BuiltinFull(uniqueId = 6489L)
    record Arrival(QExprId loc, Opt<Value> value, Scope scope) {}

    @BuiltinFull(uniqueId = 6490L)
    final class Landing {
        public final TList<Arrival> arrivals = new TList<>();
        public Opt<Type> resultType = Opt.none();

        public boolean carries = false;
    }

    @BuiltinFull(uniqueId = 6491L)
    record DestKey(long dropState, Opt<String> label, Opt<JumpKey> next, int argumentIdx) {
        public Opt<Type> argument(Emission emission) {
            if (argumentIdx < 0) return Opt.none();
            return Opt.some(emission.argumentTypes.get(argumentIdx));
        }
    }

    @BuiltinFull(uniqueId = 6415L)
    final class Emission {
        public final long id;

        public Emission(long id) {
            this.id = id;
        }

        public final TMap<DestKey, Opt<JumpKey>> dests = new TMap<>();
        public final TList<Code> blocks = new TList<>();
        public final TList<Type> argumentTypes = new TList<>();

        public int argumentIntern(ExprContext xx, Opt<Type> argument) {
            return switch (argument) {
                case Opt.None<Type> ignored -> -1;
                case Opt.Some<Type> some -> {
                    for (int i = 0; i < argumentTypes.size(); i++) {
                        if (argumentTypes.get(i).typeIsEqual(xx, some.value())) yield i;
                    }
                    argumentTypes.add(some.value());
                    yield argumentTypes.size() - 1;
                }
            };
        }
    }
}
