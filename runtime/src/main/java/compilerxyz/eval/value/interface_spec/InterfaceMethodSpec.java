package compilerxyz.eval.value.interface_spec;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record InterfaceMethodSpec(
    String name,
    String hostMethodName,
    String fieldName,
    Opt<Type> argType,
    Opt<Type> argRecordType,
    Type retType,
    JavaMethodDescriptor jvmDescriptor,
    TList<String> dataFieldNames) {}
