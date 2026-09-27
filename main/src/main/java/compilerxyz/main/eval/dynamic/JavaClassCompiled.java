package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.IList;

public record JavaClassCompiled(byte[] bytecode, IList<QExprId> locs) {}
