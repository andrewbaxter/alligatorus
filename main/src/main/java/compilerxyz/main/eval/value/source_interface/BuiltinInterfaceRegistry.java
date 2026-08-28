package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.function.TypeMethodDynamic;
import compilerxyz.main.eval.value.object_.ObjectAspectSource;
import compilerxyz.main.eval.value.object_.TypeObjectDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectOperatorAdd;
import compilerxyz.main.gen.GenAspectOperatorMod;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenBinding;
import compilerxyz.main.gen.GenCodeDyn;
import compilerxyz.main.gen.GenTarget;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.GenValue;
import compilerxyz.prelude.util.Opt;

public final class BuiltinInterfaceRegistry {
    private BuiltinInterfaceRegistry() {}

    public static InterfaceData resolve(BuiltinInterfaceKind kind) {
        Type t =
                switch (kind) {
                    case VALUE -> GenValue.dynamicType();
                    case TYPE -> GenType.dynamicType();
                    case ASPECT_FUNCTION -> GenAspectFunction.dynamicType();
                    case ASPECT_FIELD -> GenAspectField.dynamicType();
                    case ASPECT_FIELD_USE_DYNAMIC -> GenAspectFieldUseDynamic.dynamicType();
                    case ASPECT_ELEMENT_USE_DYNAMIC -> GenAspectElementUseDynamic.dynamicType();
                    case ASPECT_DROP_DYNAMIC -> GenAspectDropDynamic.dynamicType();
                    case ASPECT_CONSUME_DYNAMIC -> GenAspectConsumeDynamic.dynamicType();
                    case ASPECT_BIND_DYNAMIC -> GenAspectBindDynamic.dynamicType();
                    case ASPECT_BIND -> GenAspectBind.dynamicType();
                    case ASPECT_DROP_DYNAMIC_LIGHT -> GenAspectDropDynamicLight.dynamicType();
                    case ASPECT_INITIAL_BIND -> GenAspectInitialBind.dynamicType();
                    case ASPECT_LIFT -> GenAspectLift.dynamicType();
                    case ASPECT_SET -> GenAspectSet.dynamicType();
                    case ASPECT_OPERATOR_ADD -> GenAspectOperatorAdd.dynamicType();
                    case ASPECT_OPERATOR_MOD -> GenAspectOperatorMod.dynamicType();
                    case BINDING -> GenBinding.dynamicType();
                    case TARGET -> GenTarget.dynamicType();
                    case CODE_DYN -> GenCodeDyn.dynamicType();
                };
        var _tod = (TypeObjectDynamic) t;
        if (_tod.interfaceRepresented().optIsNone()) {
            throw new IllegalStateException(_tod.typeDesc() + "::getInterfaceData: does not represent an interface");
        }
        var irValue = _tod.interfaceRepresented().optUnwrap();
        var aspSrcOpt2 = _tod.aspectSource();
        if (aspSrcOpt2.optIsNone()) {
            throw new IllegalStateException(_tod.typeDesc() + "::getInterfaceData: missing static aspect source");
        }
        ObjectAspectSource.Static staticSrc2;
        switch (aspSrcOpt2.optUnwrap()) {
            case ObjectAspectSource.Static s -> staticSrc2 = s;
            case ObjectAspectSource.FromInterfaceData ignored ->
                throw new IllegalStateException(_tod.typeDesc() + "::getInterfaceData: missing static aspect source");
        }
        var kind2 =
                switch (staticSrc2.id()) {
                    case Unique.Source s -> Opt.<BuiltinInterfaceKind>none();
                    case Unique.Builtin b -> {
                        long _v = b.id();
                        if (_v == 1015L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FIELD);
                        if (_v == 1037L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FIELD_USE_DYNAMIC);
                        if (_v == 1039L) yield Opt.some(BuiltinInterfaceKind.ASPECT_ELEMENT_USE_DYNAMIC);
                        if (_v == 1017L) yield Opt.some(BuiltinInterfaceKind.ASPECT_SET);
                        if (_v == 1018L) yield Opt.some(BuiltinInterfaceKind.ASPECT_OPERATOR_ADD);
                        if (_v == 1042L) yield Opt.some(BuiltinInterfaceKind.ASPECT_OPERATOR_MOD);
                        if (_v == 1022L) yield Opt.some(BuiltinInterfaceKind.ASPECT_BIND);
                        if (_v == 1023L) yield Opt.some(BuiltinInterfaceKind.ASPECT_BIND_DYNAMIC);
                        if (_v == 1024L) yield Opt.some(BuiltinInterfaceKind.ASPECT_CONSUME_DYNAMIC);
                        if (_v == 1032L) yield Opt.some(BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC);
                        if (_v == 1035L) yield Opt.some(BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC_LIGHT);
                        if (_v == 1021L) yield Opt.some(BuiltinInterfaceKind.ASPECT_INITIAL_BIND);
                        if (_v == 1026L) yield Opt.some(BuiltinInterfaceKind.ASPECT_LIFT);
                        if (_v == 1116L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FUNCTION);
                        if (_v == 2001L) yield Opt.some(BuiltinInterfaceKind.VALUE);
                        if (_v == 2002L || _v == 6006L) yield Opt.some(BuiltinInterfaceKind.TYPE);
                        if (_v == 2005L) yield Opt.some(BuiltinInterfaceKind.BINDING);
                        if (_v == 2006L) yield Opt.some(BuiltinInterfaceKind.TARGET);
                        if (_v == 2007L) yield Opt.some(BuiltinInterfaceKind.CODE_DYN);
                        yield Opt.<BuiltinInterfaceKind>none();
                    }
                };
        var data = new InterfaceData(Opt.<DefSetId>none(), staticSrc2.identity(), kind2);
        data.aspectId = Opt.some(staticSrc2.id());
        data.isAspect = !staticSrc2.identity().equals(staticSrc2.id());
        data.description = Opt.some(_tod.typeDesc());
        data.compiledBinaryName = irValue.value().replace('/', '.');
        for (var m : _tod.methods()) {
            if (!(m.funcType() instanceof TypeMethodDynamic mt)) {
                throw new IllegalStateException(_tod.typeDesc() + "::getInterfaceData: method " + m.langName()
                        + " funcType is not TypeMethodDynamic");
            }
            var md = new InterfaceMethodData(m.langName());
            md.argType = mt.arg();
            md.retType = mt.ret();
            md.hostMethodName = Opt.some(mt.methodName());
            md.hostMethodDescriptor = Opt.some(mt.descriptor());
            if (mt.arg() instanceof Record rec) {
                for (var key : rec.fields().keySet()) {
                    switch (key) {
                        case ConstKey.Str s -> md.dataFieldNames.add(s.value());
                        case ConstKey.Uint u -> md.dataFieldNames.add(key.constKeyToCanonical());
                    }
                }
            }
            data.methods.add(md);
        }
        return data;
    }
}
