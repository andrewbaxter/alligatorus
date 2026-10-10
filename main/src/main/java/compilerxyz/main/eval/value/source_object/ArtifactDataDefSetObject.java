package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.ArtifactJavaClassCompiled;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record ArtifactDataDefSetObject(
        Artifact defSet,
        DefSetId defSetId,
        Opt<String> name,
        Unique identity,
        Artifact underlyingType,
        TList<ArtifactDataDefSetMethod> methods,
        TList<ArtifactDataDefSetInterfaceImpl> interfaceImpls,
        String binaryName,
        Opt<ArtifactJavaClassCompiled> compiledClass,
        Opt<Long> metaId)
        implements SafeJsonSerializable {}
