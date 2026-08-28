package compilerxyz.eval.value.source_interface;

import compilerxyz.artifact.Artifact;
import compilerxyz.util.TList;

public record ArtifactDataDefSetInterfaceMethodData(
        String name, Artifact argType, Artifact retType, TList<String> dataFieldNames) {}
