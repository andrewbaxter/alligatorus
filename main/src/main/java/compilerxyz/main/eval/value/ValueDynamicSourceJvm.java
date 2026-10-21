package compilerxyz.main.eval.value;

import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6533L)
public sealed interface ValueDynamicSourceJvm extends ValueDynamicSource
        permits ValueDynamicSource.Reg, ValueDynamicSource.Field {}
