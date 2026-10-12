package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class InterfaceMethodData {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;
    public final Type argType;
    public final Opt<Type> retType;
    public final String jvmName;
    public final JavaMethodDescriptor jvmDescriptor;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public InterfaceMethodData(
            String name, Type argType, Opt<Type> retType, String jvmName, JavaMethodDescriptor jvmDescriptor) {
        this.name = name;
        this.argType = argType;
        this.retType = retType;
        this.jvmName = jvmName;
        this.jvmDescriptor = jvmDescriptor;
    }

    public static JavaMethodDescriptor deriveDescriptor(ExprContext xx, Type arg, Opt<Type> ret, String what) {
        var argDescs = new TList<JavaDataDescriptor>();
        var tupleOpt = GenAspectTuple.getMaybe(xx, arg);
        var recordOpt = GenAspectRecord.getMaybe(xx, arg);
        if (tupleOpt.optIsSome()) {
            for (var et : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                argDescs.add(jvmDesc(xx, et, what));
            }
        } else if (recordOpt.optIsSome()) {
            for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                argDescs.add(jvmDesc(xx, entry.getValue(), what));
            }
        } else {
            argDescs.add(jvmDesc(xx, arg, what));
        }
        var retDesc = ret.optIsSome() ? jvmDesc(xx, ret.optUnwrap(), what) : JavaDataDescriptor.DESC_VOID;
        return JavaMethodDescriptor.fromParts(retDesc, argDescs);
    }

    private static JavaDataDescriptor jvmDesc(ExprContext xx, Type t, String what) {
        var consume = GenAspectConsumeDynamic.getMaybe(xx, t);
        if (consume.optIsNone()) {
            throw new IllegalStateException(what + ": type " + t.typeDesc() + " has no dynamic representation");
        }
        return consume.optUnwrap().aspectConsumeDynamicJvmClass(xx);
    }
}
