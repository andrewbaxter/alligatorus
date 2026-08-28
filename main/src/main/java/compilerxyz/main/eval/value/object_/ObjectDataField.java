package compilerxyz.main.eval.value.object_;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;

public record ObjectDataField(String langName, ObjectDataField.Access access, Type fieldType) {

    public sealed interface Access {
        record FieldGet(String jvmFieldName, JavaDataDescriptor jvmFieldDesc) implements Access {}

        record MethodCall(String jvmMethodName, JavaMethodDescriptor jvmMethodDesc) implements Access {}
    }

    public static ObjectDataField ofField(
            String langName, String jvmFieldName, JavaDataDescriptor jvmFieldDesc, Type fieldType) {
        return new ObjectDataField(langName, new Access.FieldGet(jvmFieldName, jvmFieldDesc), fieldType);
    }
}
