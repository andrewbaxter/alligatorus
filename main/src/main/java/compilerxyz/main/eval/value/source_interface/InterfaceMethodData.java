package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;

public final class InterfaceMethodData {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;
    public final Type argType;
    public final Opt<Type> retType;
    public final IList<String> dataFieldNames;
    public final Opt<String> hostMethodName;
    public final Opt<JavaMethodDescriptor> hostMethodDescriptor;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public InterfaceMethodData(
            String name,
            Type argType,
            Opt<Type> retType,
            IList<String> dataFieldNames,
            Opt<String> hostMethodName,
            Opt<JavaMethodDescriptor> hostMethodDescriptor) {
        this.name = name;
        this.argType = argType;
        this.retType = retType;
        this.dataFieldNames = dataFieldNames;
        this.hostMethodName = hostMethodName;
        this.hostMethodDescriptor = hostMethodDescriptor;
    }
}
