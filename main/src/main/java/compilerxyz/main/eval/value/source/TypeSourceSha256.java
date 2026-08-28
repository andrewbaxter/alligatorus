package compilerxyz.main.eval.value.source;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectListConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectListConst;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinPath("source.sha256")
@BuiltinLight(uniqueId = 6159L)
public record TypeSourceSha256() implements Type, AspectFunction, AspectInitialBind, ExporterAny {

    @Override
    public String typeDesc() {
        return "source.sha256";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_SHA256_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.sha256");
        if (argRes.hasErrors()) return argRes.reerr();
        var _cbVal = argRes.requireValue("source.sha256 arg");
        Opt.Some<AspectListConst> _cbListSome;
        switch (GenAspectListConst.getMaybe(xx, _cbVal.valueType())) {
            case Opt.Some<AspectListConst> s -> _cbListSome = s;
            case Opt.None<AspectListConst> n -> {
                return EResult.err(loc, "source.sha256: expected a const list of bytes");
            }
        }
        var _cbElements = _cbListSome.value().aspectListConstElements(_cbVal);
        var bytes = new byte[_cbElements.size()];
        for (var _cbI = 0; _cbI < _cbElements.size(); _cbI++) {
            var _cbElem = _cbElements.get(_cbI);
            Opt.Some<AspectNumericConst> _cbByteSome;
            switch (GenAspectNumericConst.getMaybe(xx, _cbElem.valueType())) {
                case Opt.Some<AspectNumericConst> s -> _cbByteSome = s;
                case Opt.None<AspectNumericConst> n -> {
                    return EResult.err(loc, "source.sha256: list element is not a byte");
                }
            }
            if (_cbByteSome.value().aspectNumericConstKind() != NumericKind.BYTE) {
                return EResult.err(loc, "source.sha256: list element is not a byte");
            }
            bytes[_cbI] = _cbByteSome.value().aspectNumericConstGet(_cbElem).byteValue();
        }
        if (bytes.length != 32) {
            return EResult.err(loc, "source.sha256: expected 32 bytes, got " + bytes.length);
        }
        return EResult.ok(new CodeValue(
                Code.concat(loc, self.code(), argument.code()),
                EvalOutcome.ofValue(new ValueObjectConst(new TypeHash(), new HashValue.Sha256(bytes)))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SOURCE_SHA256_IDENTITY)
                .optIsSome();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
