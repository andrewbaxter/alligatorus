package compilerxyz.eval.dynamic;

/*
 * Container for {@link DynException}. That's it.
 *
 * The Phase-1 refactor eliminated every runtime dispatch helper this class
 * used to hold (fieldGet, fieldSet, throwValue, newList, newMap, foreach*,
 * matchEntry, add/eq/lt, subscript, rethrowIfPanic, aspect-lowering
 * helpers). Every emitter site now writes primitive JVM bytecode directly
 * using the type information the construction site already has.
 *
 * If a future refactor is tempted to reintroduce a static helper here to
 * "just do runtime dispatch," resist. The design goal is: every dispatch
 * decision happens at construction time, in the type-aware caller. The
 * emitter emits primitives; the runtime interprets them.
 *
 * {@link DynException} stays because a user-level `throw` needs some
 * concrete Java exception class to wrap the thrown value in when it
 * escapes into host code (it's referenced by ATHROW-emitting code in
 * TypeThrowDynamic / TypePanicDynamic, by the try/catch handler type in
 * TargetDynamic, and by unwrapping code in Eval).
 */
public final class DynRuntime {
  private DynRuntime() {}

  public static final class DynException extends RuntimeException {
    public final Object payload;

    public DynException(Object payload) {
      super(payload == null ? "null" : payload.toString());
      this.payload = payload;
    }
  }
}
