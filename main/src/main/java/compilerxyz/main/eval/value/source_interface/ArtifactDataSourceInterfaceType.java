package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;

public record ArtifactDataSourceInterfaceType(Opt<Artifact> interface_) implements SafeJsonSerializable {}
