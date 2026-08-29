package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefSetId;
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

    public static String binaryNameFor(DefSetId defSet, long objectIndex) {
        var compileId = defSet.compileId();
        return "SourceObject$"
                + compileId.importState().value()
                + "_"
                + compileId.source().value()
                + "_"
                + defSet.index()
                + "_"
                + objectIndex;
    }

    public static String internalNameFor(DefSetId defSet, long objectIndex) {
        return binaryNameFor(defSet, objectIndex).replace('.', '/');
    }

    public static String methodMethodName(long methodIdx) {
        return "_" + methodIdx;
    }

    public static String methodFieldPrefix(long methodIdx) {
        return "_" + methodIdx + "_";
    }

    public static String interfaceMethodMethodName(int implIdx, int methodImplIdx) {
        return "_iface" + implIdx + "_method" + methodImplIdx;
    }

    public static String interfaceMethodFieldPrefix(int implIdx, int methodImplIdx) {
        return "_iface" + implIdx + "_method" + methodImplIdx + "_";
    }

    public static final String DATA_INSTANCE_FIELD = "_data";

    public static final String DATA_CTOR_DESCRIPTOR = JvmConstants.DESC_OBJECT.value();

    public static final String SOURCE_KEY_MAP_FIELD = "_sourceKeyMap";

    public static final String SOURCE_OBJECT_DATA_FIELD = "_sourceObjectData";

    public static TOrderedMap<String, Object> rebuildStaticFieldValues(TypeSourceObjectDynamic data) {
        var staticFieldValues = new TOrderedMap<String, Object>();
        var seen = new TOrderedSet<String>();
        for (int implIdx = 0; implIdx < data.interfaceImpls.size(); implIdx++) {
            var impl = data.interfaceImpls.get(implIdx);
            for (int mIdx = 0; mIdx < impl.methodImpls.size(); mIdx++) {
                var m = impl.methodImpls.get(mIdx);
                var prefix = interfaceMethodFieldPrefix(implIdx, mIdx);
                for (int i = 0; i < m.capturedExternals.size(); i++) {
                    var entry = m.capturedExternals.get(i);
                    var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
                    if (seen.add(name)) {
                        staticFieldValues.put(name, entry.external().externalHostValue());
                    }
                }
            }
        }
        for (long methodIdx = 0; methodIdx < data.methods.size(); methodIdx++) {
            var method = data.methods.get((int) methodIdx);
            var prefix = methodFieldPrefix(methodIdx);
            for (int i = 0; i < method.capturedExternals.size(); i++) {
                var entry = method.capturedExternals.get(i);
                var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
                if (seen.add(name)) {
                    staticFieldValues.put(name, entry.external().externalHostValue());
                }
            }
        }
        staticFieldValues.put(SOURCE_OBJECT_DATA_FIELD, data);
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
        JavaDataDescriptor retDesc;
        if (ifaceMethod.hostMethodDescriptor.optIsSome()) {
            var hmd = ifaceMethod.hostMethodDescriptor.optUnwrap();
            retDesc =
                    JavaDataDescriptor.create(hmd.value().substring(hmd.value().indexOf(')') + 1));
        } else if (ifaceMethod.retType.optIsSome()) {
            if (!(ifaceMethod.retType.optUnwrap() instanceof AspectConsumeDynamic retConsume)) {
                throw new IllegalStateException(
                        "SourceObjectEmitter.interfaceMethodMethodDescriptor: retType does not implement AspectConsumeDynamic");
            }
            retDesc = retConsume.aspectConsumeDynamicJvmClass(xx);
        } else {
            retDesc = JavaDataDescriptor.DESC_VOID;
        }
        return JavaMethodDescriptor.fromParts(retDesc, jvmParamDescs);
    }
}
