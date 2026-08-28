package compilerxyz.eval;

/**
 * A {@link Type} whose every implementer is deeply immutable. The contract is verified, never
 * trusted: the {@code ImmutableTypeValid} lint structurally checks every implementing type in the
 * model, and the immutability checker treats references to this interface as immutable on that
 * basis. Use this for type-valued fields and collections that must be immutable.
 */
public interface TypeImmutable extends Type {}
