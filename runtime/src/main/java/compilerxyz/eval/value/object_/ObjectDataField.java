package compilerxyz.eval.value.object_;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaDataDescriptor;

public record ObjectDataField(
    String langName, String jvmFieldName, JavaDataDescriptor jvmFieldDesc, Type fieldType) {}
