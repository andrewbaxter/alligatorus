package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.function.TypeMethodDynamic;
import compilerxyz.main.eval.value.object.ObjectAspectSource;
import compilerxyz.main.eval.value.object.TypeObjectDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class InterfaceData {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final Opt<String> description;
    public final boolean isAspect;
    public final Opt<Unique> aspectId;
    public final IList<InterfaceMethodData> methods;

    public final Opt<TypeObjectDynamic> builtinType;

    public InterfaceData(
            Opt<String> description,
            boolean isAspect,
            Opt<Unique> aspectId,
            IList<InterfaceMethodData> methods,
            Opt<TypeObjectDynamic> builtinType) {
        this.description = description;
        this.isAspect = isAspect;
        this.aspectId = aspectId;
        this.methods = methods;
        this.builtinType = builtinType;
    }

    public static InterfaceData ofBuiltinType(TypeObjectDynamic type) {
        var aspSrcOpt = type.aspectSource();
        if (aspSrcOpt.optIsNone()) {
            throw new IllegalStateException(type.typeDesc() + "::ofBuiltinType: missing aspect source");
        }
        ObjectAspectSource.Static staticSrc;
        switch (aspSrcOpt.optUnwrap()) {
            case ObjectAspectSource.Static st -> staticSrc = st;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(type.typeDesc() + "::ofBuiltinType: missing static aspect source");
        }
        var methods = new TList<InterfaceMethodData>();
        for (var m : type.methods()) {
            if (!(m.funcType() instanceof TypeMethodDynamic mt)) {
                throw new IllegalStateException(type.typeDesc() + "::ofBuiltinType: method " + m.langName()
                        + " funcType is not TypeMethodDynamic");
            }
            var dataFieldNames = new TList<String>();
            if (mt.arg() instanceof Record rec) {
                for (var key : rec.fields().keys()) {
                    switch (key) {
                        case ConstKey.Str str -> dataFieldNames.add(str.value());
                        case ConstKey.Uint u -> dataFieldNames.add(key.constKeyToCanonical());
                    }
                }
            }
            methods.add(new InterfaceMethodData(
                    m.langName(),
                    mt.arg(),
                    mt.ret(),
                    IList.from(dataFieldNames),
                    Opt.some(mt.methodName()),
                    Opt.some(mt.descriptor())));
        }
        return new InterfaceData(
                Opt.some(type.typeDesc()),
                !staticSrc.identity().equals(staticSrc.id()),
                Opt.some(staticSrc.id()),
                IList.from(methods),
                Opt.some(type));
    }
}
