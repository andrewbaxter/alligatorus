package compilerxyz.main.eval;

import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public sealed interface ScopeLevel {

    Emission scopeLevelEmission();

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

    final class Temp implements ScopeLevel {
        public Binding binding;
        public long state;
        public long dropState;
        final Emission emission;

        public Temp(Binding binding, long state, long dropState, Emission emission) {
            this.binding = binding;
            this.state = state;
            this.dropState = dropState;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

    final class Labeled implements ScopeLevel {
        public final Opt<String> label;
        public final JumpKey end;
        public final TList<Arrival> arrivals;
        final Emission emission;

        public Labeled(Opt<String> label, JumpKey end, TList<Arrival> arrivals, Emission emission) {
            this.label = label;
            this.end = end;
            this.arrivals = arrivals;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }
    }

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

    final class Finally implements ScopeLevel {
        public Code body;
        public final TList<Pending> pending;
        final Emission emission;

        public Finally(TList<Pending> pending, Emission emission) {
            this.pending = pending;
            this.emission = emission;
        }

        @Override
        public Emission scopeLevelEmission() {
            return emission;
        }

        public record Pending(JumpKey self, DestKey key) {}
    }

    record Arrival(QExprId loc, Value value, boolean stackCarried, Scope scope) {}

    final class DestKey {
        public final long dropState;
        public final Opt<String> label;
        public final Opt<JumpKey> next;
        public final int carryTypeIdx;

        public DestKey(long dropState, Opt<String> label, Opt<JumpKey> next, int carryTypeIdx) {
            this.dropState = dropState;
            this.label = label;
            this.next = next;
            this.carryTypeIdx = carryTypeIdx;
        }

        public Opt<String> label() {
            return label;
        }

        public Opt<JumpKey> next() {
            return next;
        }

        public Opt<Type> carryType(Emission emission) {
            if (carryTypeIdx < 0) return Opt.none();
            return Opt.some(emission.carryTypes.get(carryTypeIdx));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof DestKey d
                    && d.dropState == dropState
                    && d.label.equals(label)
                    && switch (d.next) {
                        case Opt.Some<JumpKey> as ->
                            switch (next) {
                                case Opt.Some<JumpKey> bs -> as.value() == bs.value();
                                case Opt.None<JumpKey> ignored -> false;
                            };
                        case Opt.None<JumpKey> ignored -> next.optIsNone();
                    }
                    && d.carryTypeIdx == carryTypeIdx;
        }

        @Override
        public int hashCode() {
            int h = Long.hashCode(dropState);
            h = h * 31 + label.hashCode();
            h = h * 31
                    + switch (next) {
                        case Opt.Some<JumpKey> some -> System.identityHashCode(some.value());
                        case Opt.None<JumpKey> ignored -> 0;
                    };
            h = h * 31 + carryTypeIdx;
            return h;
        }
    }

    final class Emission {
        public final long id;

        public Emission(long id) {
            this.id = id;
        }

        public final TMap<DestKey, Opt<JumpKey>> dests = new TMap<>();
        public final TList<Code> blocks = new TList<>();
        public final TList<Type> carryTypes = new TList<>();

        public int carryTypeIntern(ExprContext xx, Opt<Type> carryType) {
            return switch (carryType) {
                case Opt.None<Type> ignored -> -1;
                case Opt.Some<Type> some -> {
                    for (int i = 0; i < carryTypes.size(); i++) {
                        if (carryTypes.get(i).typeIsEqual(xx, some.value())) yield i;
                    }
                    carryTypes.add(some.value());
                    yield carryTypes.size() - 1;
                }
            };
        }
    }
}
