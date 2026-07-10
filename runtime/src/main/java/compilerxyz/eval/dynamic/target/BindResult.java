package compilerxyz.eval.dynamic.target;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.dynamic.CodeDynamic;

public record BindResult(CodeDynamic code, BindingId.Local id) {}
