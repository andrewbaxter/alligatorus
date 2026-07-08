package compilerxyz.eval.value.interface_spec;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;

public record InterfaceMethodSpec(
    String name,
    String hostMethodName,
    Type argType,
    Type retType,
    JavaMethodDescriptor jvmDescriptor) {}
