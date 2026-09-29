package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.function.FunctionParam;
import compilerxyz.main.eval.value.function.TypeSourceMethodDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldSourceObjectDynamic(TypeSourceObjectDynamic type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        ConstKey.Str keyStr;
        switch (ck) {
            case ConstKey.Str s -> keyStr = s;
            case ConstKey.Uint u -> {
                return EResult.err(
                        loc,
                        "Field "
                                + ck.constKeyToCanonical()
                                + " not found on object (underlying-type Record fallback deferred)");
            }
        }
        String keyName = keyStr.value();
        Opt<SourceDataMethodData> foundOpt = Opt.none();
        for (var m : type.methods) {
            if (m.name.equals(keyName)) {
                foundOpt = Opt.some(m);
                break;
            }
        }
        if (foundOpt.optIsNone()) {
            var names = new TList<String>();
            for (var m : type.methods) names.add(m.name);
            return EResult.err(
                    loc,
                    "TypeSourceObjectDynamic::field: field '"
                            + keyName
                            + "' not found on object "
                            + type.typeDesc()
                            + " (methods: "
                            + names
                            + "; underlying-type Record fallback deferred)");
        }
        var found = foundOpt.optUnwrap();
        var retType = found.retType;
        var argType = found.argType.optUnwrapOr(new Tuple(IList.of()));
        var fieldName = found.jvmMethodName;
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: receiver is not a ValueDynamic");
        }

        if (type.set != null) {
            xx.vx.definitionSets.putIfAbsent(type.set.defSetId, type.set);
        }
        JavaDataDescriptor retJvmDesc;
        if (retType.optIsSome()) {
            var retConsume = GenAspectConsumeDynamic.getMaybe(xx, retType.optUnwrap());
            if (retConsume.optIsNone()) {
                return EResult.err(
                        loc, "TypeSourceObjectDynamic::field: retType does not implement AspectConsumeDynamic");
            }
            retJvmDesc = retConsume.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        } else {
            retJvmDesc = JavaDataDescriptor.DESC_VOID;
        }
        var slotsRes = DynUtil.argSignatureSlots(xx, loc, argType);
        if (slotsRes.hasErrors()) return slotsRes.reerr();
        var slots = slotsRes.requireValue("TypeSourceObjectDynamic::field argSignatureSlots");
        var argDescs = new TList<JavaDataDescriptor>(slots.size());
        for (var s : slots) argDescs.add(s.jvmDesc());
        var descObjObj = JavaMethodDescriptor.fromParts(retJvmDesc, argDescs);
        var objectInternalName = JavaInternalName.of(type.internalName());
        var fieldType = new TypeSourceMethodDynamic(
                type,
                TList.of(new FunctionParam("arg", argType)),
                retType,
                objectInternalName,
                fieldName,
                descObjObj,
                false,
                false);
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfMethodVd)) {
            return EResult.err(loc, "TypeSourceObjectDynamic::field: method receiver has no dynamic source");
        }
        return EResult.ok(
                new CodeValue(self.code(), EvalOutcome.ofValue(new ValueDynamic(selfMethodVd.source(), fieldType))));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }
}
