package compilerxyz.main.eval.value;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Array;

public final class ConstHost {
    private ConstHost() {}

    public static <K, V> TOrderedMap<K, V> hostMap(TList<K> keys, TList<V> values) {
        var out = new TOrderedMap<K, V>();
        for (int i = 0; i < keys.size(); i++) out.put(keys.get(i), values.get(i));
        return out;
    }

    public static EResult<Value> lift(ExprContext xx, QExprId loc, Type type, Object host) {
        var liftRes = GenAspectLift.get(xx, loc, type);
        if (liftRes.hasErrors()) return liftRes.reerr();
        return liftRes.requireValue("ConstHost::lift aspect").aspectLiftLift(xx, loc, host);
    }

    public static EResult<Object> unlift(QExprId loc, Value value) {
        if (value instanceof ValueObjectConst voc) return EResult.ok(voc.data());
        return EResult.err(
                loc, "a " + value.valueType().typeDesc() + " value has no host form to hold in a constant collection");
    }

    public static EResult<CodeValue> loadDynamic(
            QExprId loc, CodeValue cv, JavaDataDescriptor hostDesc, Type dynamicType) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, dynamicType.typeDesc() + "::toDynamic: CodeValue has no compile-time value");
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var cell = new StagedExprCell(cv.value().optUnwrap());
        var entry = new EvalContext.ExternalEntry(
                new BindingId(), new TypeExpr(), new External(StagedExprCell.class, cell));
        var host = b.invokeStatic(GenCodegenDescriptors.DYN_RUNTIME_CONST_HOST, b.external(entry));
        return EResult.ok(DynUtil.regToValue(b.code(), b.checkcast(hostDesc, host), dynamicType));
    }

    public static Class<?> hostClass(ExprContext xx, JavaDataDescriptor desc) {
        var d = desc.value;
        return switch (d.charAt(0)) {
            case 'I' -> int.class;
            case 'J' -> long.class;
            case 'S' -> short.class;
            case 'B' -> byte.class;
            case 'F' -> float.class;
            case 'D' -> double.class;
            case 'Z' -> boolean.class;
            case 'C' -> char.class;
            case '[' ->
                Array.newInstance(hostClass(xx, new JavaDataDescriptor(d.substring(1))), 0)
                        .getClass();
            default -> {
                var name = d.substring(1, d.length() - 1).replace('/', '.');
                ClassLoader loader = xx.vx.ix.classLoader.optIsSome()
                        ? xx.vx.ix.classLoader.optUnwrap()
                        : DynRuntime.class.getClassLoader();
                try {
                    yield Class.forName(name, false, loader);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("ConstHost: host class " + name + " not found", e);
                }
            }
        };
    }
}
