# Static analysis

All static analysis lives in the `analysis/` module and runs on **Spoon** (a whole-program,
source-level model). There is no Error Prone / per-file compiler plugin — one Spoon model is built
and both the convention checks and the reachability analysis run against it.

## Running it

It runs automatically in the `verify` phase (so `mvn verify` / `mvn install` include it), and can be
run on demand:

```sh
mvn -q -pl analysis exec:java
```

It builds the model over all sources (`util`, `codegen`, `runtime` main + test + generated) and writes:

- `analysis/target/conventions-report.txt` — per-location convention findings (summary printed to stdout).
- `analysis/target/reachability-report.txt` — dead code (#1) and single-use (#2).

`mvn compile` / `mvn test` do **not** run it (it's bound to `verify`), so the inner dev loop is fast;
the whole-program pass runs on full builds.

## 1. Convention checks (`ConventionChecks`)

The CLAUDE.md conventions, as Spoon checks over the source model (generated `compilerxyz.gen.*` code
is excluded via source-root origin):

| Check | Convention |
|-------|-----------|
| `InterfaceMethodNamePrefix` | Interface methods are prefixed with the lowerCamelCase interface name (`Value.valueType()`). |
| `NoInstanceofOnSealed` | Discriminate a sealed type with an exhaustive `switch`, not `instanceof`. |
| `NoNullableType` | No Nullable annotation in any form (checked by source text — Spoon drops type-use annotations on array types from the model); model optional values with `Opt<T>`. |
| `AvoidJdkOptional` | Use `Opt` instead of an explicitly-written `java.util.Optional`. |
| `UnnecessaryFullyQualifiedName` | Import types and use simple names. Covers declared types (including generic/array forms) and fully-qualified static-call/constant receivers (detected via targeted-expression positions — noClasspath Spoon gives the receiver's type references no positions). A simple name bound to more than one qualified name in a file is ambiguous and not flagged. |

To add a check: add a method to `ConventionChecks.run(CtModel)` that emits `Finding`s.

## 1b. Immutability + equality checks (`ImmutabilityChecks`)

Backed by `ImmutabilityCheck`, a recursive, memoized deep-immutability judgment over the Spoon
model: a type is immutable when every instance field is final and of an immutable type — a
primitive, a whitelisted leaf (`String`, boxed primitives, `Pattern`, javapoet value types, …), an
immutable collection (`compilerxyz.util.IList`/`IMap`/`ISet`, which copy on construction and expose
no mutation methods) of immutable types, or another immutable type. Field-typed references must be
closed (final, record, enum, or sealed with all permitted types immutable); the extends chain is
exempt from closedness since the concrete type is known. Cycles are treated as immutable (results
computed under a still-unresolved cycle assumption are not memoized, so a later mutable outcome is
not masked). Everything unresolvable — external types off the whitelist, unbound type parameters,
raw generics, arrays — is mutable, fail-closed.

| Check | Rule |
|-------|------|
| `ImmutableAnnotationValid` | Every `@compilerxyz.annotations.Immutable` assertion is verified structurally; the annotation is never trusted. |
| `ImmutableTypeValid` | Every implementer of `compilerxyz.eval.TypeImmutable` is verified — structurally, or (for the two-pass reflective-init protocol) by value analysis: every constructor and reflective-init call site's argument expression graph is evaluated (`checkValue` — constructions by record components / generic-class type-param substitution, I-collection and `TList.of` elements, `Opt.some/none`, single-assignment locals, `TList` accumulators bounded to add/addAll, final-field and trivial-factory look-through with parameter substitution). References to implementers are then immutable by verified contract. |
| `StaticFieldImmutable` | Every global (static field, interface constant, enum value) is final and deeply immutable. For a final field whose declared type fails (e.g. an interface), the initializer's exact type is checked instead when it is a direct `new` or anonymous class — sound because the field is final, so the runtime type is exactly the initializer's. |
| `MapKeyImmutable` | The key type argument of every map-typed reference (`TMap`/`TOrderedMap`/`IMap`/`java.util` maps) is immutable. Type parameters in scope at the reference are exempt — their concrete instantiations are checked at their own use sites. `IdentityHashMap`/`WeakHashMap` are exempt: they hash by reference identity, so key mutation cannot corrupt them. |
| `NoReferenceEquality` | No `==`/`!=` on references (null checks and enums exempt); use the type's defined equality — identity aspects for types. |
| `NoReflectiveEquals` | No equality that falls through to Object's reflective identity equals: `.equals` on model classes with no equals override, and `java.util.Objects.equals`/`deepEquals`. |

## 2. Reachability + single-use (`ReachabilityMain`)

Class-hierarchy reachability over the same Spoon model. The codegen module is excluded from the
index (its external JavaPoet/JavaParser types break resolution of its internal call graph, and its
members are findings-exempt), but every codegen body, field initializer, and static initializer
block is traversed as an always-live edge *source*, so `util`/`runtime` members used only from
codegen are correctly reachable. Roots are `main` methods, JUnit tests,
`@CodegenDescriptor` methods, and everything generated code references. `@Builtin*` annotations are
**not** roots themselves: they drive codegen, and the generated registrations
(`TypeMethodDynamic`/`TypeFunctionDynamic`/`callStaticMethod` name literals in generated code) and
direct `Gen*` calls root the actually-used members — an annotated-but-unregistered member surfaces
as dead. `@SuppressWarnings` is never consulted. The analysis is fail-closed: Spoon model errors
propagate instead of silently rooting.

Reports unreachable classes/methods (#1) and single-use private/static methods and private fields
(#2). Methods referenced from generated code are exempt from single-use — the generated reference
is the annotation-driven root, and nothing can be inlined into code that is regenerated every
build.

The two dynamic-dispatch patterns that used to cause false positives are now static and traceable:

- **Emitted-into-target calls** — helpers the compiler emits (e.g. `NumericFormat.formatInt`,
  `TypeObjectDynamic.getInterfaceData`) are marked `@CodegenDescriptor`; codegen generates
  `compilerxyz.gen.GenCodegenDescriptors` constants (`JavaStaticMethodRef` for static methods,
  `JavaVirtualMethodRef` for instance methods) that emit sites reference by name. The annotation
  roots the method; an unused generated constant is flagged as dead.
- **JSON dispatch** — no runtime `Class → Codec` registry or reflection; `CodecsGen` emits a static
  `compilerxyz.gen.codec.Codecs.lookup` (class-literal dispatch) that `Json` calls directly.

**Caveat:** `Class.forName(binaryName)` on *computed* names (the compiler loading its own emitted
target classes) is opaque to the analysis, but those load the compiled program's output, not project
code, so they don't produce false positives. Findings are candidates to review.
