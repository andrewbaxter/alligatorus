package compilerxyz.main.gen;

import compilerxyz.main.eval.TargetCode;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 2007L, scopeId = 3007)
@BuiltinPath("eval.code_dyn")
public interface CodeDyn extends TargetCode {}
