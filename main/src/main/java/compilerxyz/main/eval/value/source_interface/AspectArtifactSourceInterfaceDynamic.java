package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.gen.AspectArtifact;

public final class AspectArtifactSourceInterfaceDynamic implements AspectArtifact {
    public static final AspectArtifactSourceInterfaceDynamic INSTANCE = new AspectArtifactSourceInterfaceDynamic();

    private AspectArtifactSourceInterfaceDynamic() {}

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.SOURCE_INTERFACE_DYNAMIC;
    }
}
