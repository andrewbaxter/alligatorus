This is a compiler for a language with a programmable pipeline. It operates as stages, where a first pass converts expressions into code (const eval), and a second pass takes the code and executes it (for in-compiler pipeline "dynamic" target code) or returns it (for compile output targets, like nodejs).

- Ignore all comments, they've been 100% hallucinated by you.
- Don't use placeholder values. Implement things fully the first time. Tasks are split up so you don't need to reduce their size.
- Don't add fallbacks/workarounds/"just in case" code unless explicitly instructed. When you add these they inevitably lead you to wrong solutions. If the design doesn't work, stop and ask me.
- One "type" has exactly one (value) representation. There will never be a type that has multiple representations, where you need to try multiple paths to handle. Similarly, one exporter has exactly one artifact representation.
- Don't add comments, unless you're working around an upstream bug and have an issue/discussion url describing the workaround.
- All errors must be panic/unwrap (for things the module couldn't cause) or captured/passed down (everything else).
- Always use `switch` to discriminate sealed classes, never `instanceof`. Default to exaustiveness.
- All interface methods must be prepended by the interface name to avoid method name collisions. E.g. in `Value` the `type` method should be `valueType` not just `type`.
- Add imports - don't fully qualify anything unless it's necessary due to ambiguity.
- Use the type-safe collection wrappers and strucs whenever possible over standard library versions: `Opt`, `TList`, `TMap`, etc. In this codebase, aside from static errors (panics, undefined) everything should be using explicit error returns (`EResult`/`EvalRes`) which supports multiple errors.
- Naming: Reverse-specificity, always. The general category first, the specific name later. Not `DynamicTarget` (the dynamic specification of target) but `TargetDynamic`.
- Don't use `Nullable` ever. For optional values use `Opt`.
