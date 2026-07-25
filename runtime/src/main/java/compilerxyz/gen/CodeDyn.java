package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPath;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.TargetCode;

@Builtin(uniqueId = 2007L, scopeId = 3007)
@BuiltinPath("eval.code_dyn")
public interface CodeDyn extends TargetCode {}
