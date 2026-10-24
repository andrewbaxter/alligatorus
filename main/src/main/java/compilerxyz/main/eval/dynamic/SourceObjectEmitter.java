package compilerxyz.main.eval.dynamic;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.main.eval.value.source_object.SourceObjectInstance;
import compilerxyz.main.eval.value.source_object.SourceObjectInterfaceMethodImplData;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import compilerxyz.prelude.util.TOrderedSet;

public final class SourceObjectEmitter {
    private SourceObjectEmitter() {}

    public static final JavaInternalName INTNAME_SOURCE_OBJECT_INSTANCE =
            JavaInternalName.fromClass(SourceObjectInstance.class);

    public static final String DATA_INSTANCE_FIELD = "_data";
    public static final String ARTIFACT_ID_INSTANCE_FIELD = "_artifact_id";
    public static final String ARTIFACT_ID_METHOD = "sourceObjectInstanceArtifactId";
    public static final JavaDataDescriptor DESC_ARTIFACT_ID =
            JavaDataDescriptor.fromClass(ArtifactSupport.ArtifactIdHolder.class);

    public static TOrderedMap<String, Object> rebuildStaticFieldValues(TypeSourceObjectDynamic data) {
        var staticFieldValues = new TOrderedMap<String, Object>();
        var seen = new TOrderedSet<String>();
        for (var impl : data.interfaceImpls) {
            for (var m : impl.methodImpls) {
                var prefix = m.jvmMethodName + "_";
                for (int i = 0; i < m.capturedExternals.size(); i++) {
                    var entry = m.capturedExternals.get(i);
                    var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
                    if (seen.add(name)) {
                        staticFieldValues.put(name, entry.external().externalHostValue());
                    }
                }
            }
        }
        for (var method : data.methods) {
            var prefix = method.jvmMethodName + "_";
            for (int i = 0; i < method.capturedExternals.size(); i++) {
                var entry = method.capturedExternals.get(i);
                var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
                if (seen.add(name)) {
                    staticFieldValues.put(name, entry.external().externalHostValue());
                }
            }
        }
        return staticFieldValues;
    }

    public static JavaMethodDescriptor interfaceMethodMethodDescriptor(
            ExprContext xx, SourceObjectInterfaceMethodImplData m, InterfaceMethodData ifaceMethod) {
        var jvmParamDescs = new TList<JavaDataDescriptor>();
        if (m.compiledArgParamTypes.optIsNone()) {
            throw new IllegalStateException(
                    "SourceObjectEmitter.interfaceMethodMethodDescriptor: missing compiledArgParamTypes");
        }
        for (var argType : m.compiledArgParamTypes.optUnwrap()) {
            var argConsume =
                    switch (GenAspectConsumeDynamic.getMaybe(xx, argType)) {
                        case Opt.Some<AspectConsumeDynamic> s -> s.value();
                        case Opt.None<AspectConsumeDynamic> n ->
                            throw new IllegalStateException(
                                    "SourceObjectEmitter.interfaceMethodMethodDescriptor: argParamType "
                                            + argType.typeDesc() + " does not implement AspectConsumeDynamic");
                    };
            jvmParamDescs.add(argConsume.aspectConsumeDynamicJvmClass(xx));
        }
        var retDesc = ifaceMethod.jvmDescriptor.returnDescriptor();
        return JavaMethodDescriptor.fromParts(retDesc, jvmParamDescs);
    }
}
