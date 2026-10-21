package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;

public record ObjectUnionVariant(ConstKey key, JavaInternalName klass, JavaDataDescriptor klassDesc, Type type) {}
