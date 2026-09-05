package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc.Some;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.eresult.TypeEResultDynamic.FieldSpec;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeEResultDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var ckStr =
                switch (ck) {
                    case ConstKey.Str s -> Opt.some(s);
                    case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
                };
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("has_errors")) {
            var selfDynRes = CodeDynamic.castRequired(
                    loc, self.code(), "TypeEResultDynamic::aspectFieldField(has_errors): self has no runtime code");
            if (selfDynRes.hasErrors()) return selfDynRes.reerr();
            var selfDyn = selfDynRes.requireValue("selfDyn");
            var seq = new JavaBytecodeSequence();
            seq.add(selfDyn);
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_HAS_ERRORS));
            return EResult.ok(new CodeValue(
                    Code.target(new CodeDynamic(new Some(loc), seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeBoolDynamic()))));
        }
        if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
            var selfDynRes = CodeDynamic.castRequired(
                    loc, self.code(), "TypeEResultDynamic::aspectFieldField(value): self has no runtime code");
            if (selfDynRes.hasErrors()) return selfDynRes.reerr();
            var selfDyn = selfDynRes.requireValue("selfDyn");
            var seq = new JavaBytecodeSequence();
            seq.add(selfDyn);
            seq.add(JavaBytecodeUtils.literalString(new Some(loc), "eresult.value"));
            seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE));
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("TypeEResultDynamic::field value AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                seq.add(JavaBytecodeUtils.cast(loc, elemDesc));
            }
            return EResult.ok(new CodeValue(
                    Code.target(new CodeDynamic(new Some(loc), seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, type.elementType()))));
        }
        var spec = type.fieldSpecs().getOrNull(ck);
        if (spec == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on eresult");
        }
        return switch (spec) {
            case FieldSpec.Factory f ->
                EResult.ok(new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(type.fieldSpecs().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var spec = type.fieldSpecs().getOrNull(key);
        return spec == null ? Opt.none() : Opt.some(spec.fieldSpecFieldType());
    }
}
