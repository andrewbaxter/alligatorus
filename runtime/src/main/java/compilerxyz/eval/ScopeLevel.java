package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;

/**
 * One level of the scope stack. Levels are the units of the drop/exit chain:
 * an exceptional unwind or a labeled return passes level by level outward,
 * and each cleanup-bearing level contributes one chain node per (state,
 * continuation) it is passed through under.
 *
 * <p>Chain bookkeeping ({@link Emission}, arrivals, pending finally nodes) is
 * emission-path state, not branch state: forks share it by identity, so
 * registrations from inside a branch arm surface at the owning level's close
 * regardless of which fork registered them. Dedup across forks is sound
 * because state ids are minted from the per-eval counter — two paths only
 * share a node key when they share the state lineage that produced it.
 */
public sealed interface ScopeLevel {

  Emission scopeLevelEmission();

  /** A named (or synthetic unnamed) binding pushed by a block. */
  final class Entry implements ScopeLevel {
    public final Opt<ConstKey> name;
    public Binding binding;
    /**
     * True for synthetic operand-stack cleanup entries (e.g. a foreach's
     * iterator slot, dropped with a POP): they participate in exit
     * trampolines — a jump leaves the value dangling — but not in
     * exceptional chains, where the target's unwind already discards it.
     */
    public final boolean stackOnly;
    /**
     * General state id: minted on every mutation. Equal ids across branches
     * mean nothing changed and the merge is the identity.
     */
    public long state;
    /**
     * Drop-state id: minted only for drop-affecting mutations; keys chain
     * nodes. A mutation may bump {@code state} without bumping this — it does
     * not matter that such changes are invisible where drop code is emitted.
     */
    public long dropState;
    private final Emission emission;

    public Entry(Opt<ConstKey> name, Binding binding, boolean stackOnly, long state, long dropState) {
      this(name, binding, stackOnly, state, dropState, new Emission());
    }

    private Entry(
        Opt<ConstKey> name,
        Binding binding,
        boolean stackOnly,
        long state,
        long dropState,
        Emission emission) {
      this.name = name;
      this.binding = binding;
      this.stackOnly = stackOnly;
      this.state = state;
      this.dropState = dropState;
      this.emission = emission;
    }

    public Entry forkCopy() {
      return new Entry(name, binding.bindingFork(), stackOnly, state, dropState, emission);
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }
  }

  /**
   * An in-flight droppable temporary: produced on the operand stack with a
   * shadow copy stored to a local at production, popped at consumption. The
   * binding reads the shadow local, so chain nodes can drop the value after
   * a handler entry has discarded the operand stack.
   */
  final class Temp implements ScopeLevel {
    public Binding binding;
    public long state;
    public long dropState;
    private final Emission emission;

    public Temp(Binding binding, long state, long dropState) {
      this(binding, state, dropState, new Emission());
    }

    private Temp(Binding binding, long state, long dropState, Emission emission) {
      this.binding = binding;
      this.state = state;
      this.dropState = dropState;
      this.emission = emission;
    }

    public Temp forkCopy() {
      return new Temp(binding.bindingFork(), state, dropState, emission);
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }
  }

  /**
   * A control construct that consumes returns: named when pushed by a labeled
   * expression, anonymous for seq/if/match/foreach. An unlabeled return
   * targets the innermost Labeled of either kind; a labeled return targets
   * the innermost with the matching name.
   */
  final class Labeled implements ScopeLevel {
    public final Opt<String> label;
    /** Convergence point; matched returns land here after their trampoline chain. */
    public final JumpKey end;
    /** Arrivals recorded by matched returns; shared across forks. */
    public final TList<Arrival> arrivals;
    private final Emission emission;

    public Labeled(Opt<String> label, JumpKey end) {
      this(label, end, new TList<>(), new Emission());
    }

    private Labeled(Opt<String> label, JumpKey end, TList<Arrival> arrivals, Emission emission) {
      this.label = label;
      this.end = end;
      this.arrivals = arrivals;
      this.emission = emission;
    }

    public Labeled forkCopy() {
      return new Labeled(label, end, arrivals, emission);
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }
  }

  /**
   * A try with a catch: terminal of the exceptional chain for its extent —
   * chain nodes inside jump to {@code handler}, where the exception is the
   * single stack element.
   */
  final class Catch implements ScopeLevel {
    public final JumpKey handler;
    private final Emission emission;

    public Catch(JumpKey handler) {
      this(handler, new Emission());
    }

    private Catch(JumpKey handler, Emission emission) {
      this.handler = handler;
      this.emission = emission;
    }

    public Catch forkCopy() {
      return new Catch(handler, emission);
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }
  }

  /**
   * A try's finally: chain-wise a cleanup level whose code is a fresh
   * instance of the evaluated finally body rather than a binding drop. The
   * body is const-evaled after the try's body and catch (source order), so
   * node code generation is deferred: chain construction records a
   * {@link Pending} per key and the try's close generates the blocks once
   * {@code body} is set.
   */
  final class Finally implements ScopeLevel {
    public Code body;
    /** Deferred chain nodes, shared across forks; generated at the try's close. */
    public final TList<Pending> pending;
    private final Emission emission;

    public Finally() {
      this(new TList<>(), new Emission());
    }

    private Finally(TList<Pending> pending, Emission emission) {
      this.pending = pending;
      this.emission = emission;
    }

    public Finally forkCopy() {
      var out = new Finally(pending, emission);
      out.body = body;
      return out;
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }

    public record Pending(JumpKey self, DestKey key) {}
  }

  /**
   * An operand-position boundary: a return may not cross it (control exits
   * are not valid in expression-operand positions), while exceptional chains
   * pass through freely.
   */
  final class Barrier implements ScopeLevel {
    private final Emission emission;

    public Barrier() {
      this(new Emission());
    }

    private Barrier(Emission emission) {
      this.emission = emission;
    }

    public Barrier forkCopy() {
      return new Barrier(emission);
    }

    @Override
    public Emission scopeLevelEmission() {
      return emission;
    }
  }

  /** A return's arrival at a {@link Labeled} level, pre-convergence. */
  record Arrival(QExprId loc, Value value, boolean stackCarried, Scope scope) {}

  /**
   * Identity of one chain node at a level: the level's drop state when the
   * chain passed through, the destination discriminator (none = exceptional
   * chain; some = a return trampoline chain), the next node's key (none only
   * for the exceptional root, which rethrows), and the carried value's type
   * for trampolines whose value rides the stack (types compare by reference
   * identity, which is exactly the sharing we want).
   */
  final class DestKey {
    public final long dropState;
    public final Opt<String> label;
    public final Opt<JumpKey> next;
    public final Opt<Type> carryType;

    public DestKey(long dropState, Opt<String> label, Opt<JumpKey> next, Opt<Type> carryType) {
      this.dropState = dropState;
      this.label = label;
      this.next = next;
      this.carryType = carryType;
    }

    public Opt<String> label() {
      return label;
    }

    public Opt<JumpKey> next() {
      return next;
    }

    public Opt<Type> carryType() {
      return carryType;
    }

    private static <T> boolean optIdentityEquals(Opt<T> a, Opt<T> b) {
      if (a instanceof Opt.Some<T> as) {
        return b instanceof Opt.Some<T> bs && as.value() == bs.value();
      }
      return b instanceof Opt.None<T>;
    }

    private static <T> int optIdentityHash(Opt<T> o) {
      return o instanceof Opt.Some<T> some ? System.identityHashCode(some.value()) : 0;
    }

    @Override
    public boolean equals(Object other) {
      // Types and jump keys compare by reference identity — never
      // structurally; type object graphs are cyclic.
      return other instanceof DestKey d
          && d.dropState == dropState
          && d.label.equals(label)
          && optIdentityEquals(d.next, next)
          && optIdentityEquals(d.carryType, carryType);
    }

    @Override
    public int hashCode() {
      int h = Long.hashCode(dropState);
      h = h * 31 + label.hashCode();
      h = h * 31 + optIdentityHash(next);
      h = h * 31 + optIdentityHash(carryType);
      return h;
    }
  }

  /**
   * Per-level-lineage chain bookkeeping, shared across forks by identity.
   * {@code dests} dedups chain nodes; {@code blocks} holds their generated
   * code, spliced in by the construct that owns the level at its close.
   */
  final class Emission {
    /**
     * Resolved destination per chain key. A level whose cleanup is empty at
     * the keyed state resolves to its parent's dest (pass-through, no node);
     * none = the chain root rethrows out of the method.
     */
    public final TMap<DestKey, Opt<JumpKey>> dests = new TMap<>();
    public final TList<Code> blocks = new TList<>();
  }
}
