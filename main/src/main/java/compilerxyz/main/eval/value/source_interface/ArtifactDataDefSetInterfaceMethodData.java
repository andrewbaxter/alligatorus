package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetInterfaceMethodData(
        String name, Artifact argType, Artifact retType, TList<String> dataFieldNames) {}
