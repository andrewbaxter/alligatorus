package compilerxyz.eval.value.source;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.Binding;
import compilerxyz.eval.NumericKind;
import compilerxyz.gen.AspectListConst;
import compilerxyz.gen.AspectNumericConst;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectListConst;
import compilerxyz.gen.GenAspectNumericConst;
import compilerxyz.gen.Value;
import compilerxyz.syntax.HashValue;
import compilerxyz.util.Opt;

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
      case Opt.None<AspectListConst> n -> { return EResult.err(loc, "source.sha256: expected a const list of bytes"); }
    }
    var _cbElements = _cbListSome.value().aspectListConstElements(_cbVal);
    var bytes = new byte[_cbElements.size()];
    for (var _cbI = 0; _cbI < _cbElements.size(); _cbI++) {
      var _cbElem = _cbElements.get(_cbI);
      Opt.Some<AspectNumericConst> _cbByteSome;
      switch (GenAspectNumericConst.getMaybe(xx, _cbElem.valueType())) {
        case Opt.Some<AspectNumericConst> s -> _cbByteSome = s;
        case Opt.None<AspectNumericConst> n -> { return EResult.err(loc, "source.sha256: list element is not a byte"); }
      }
      if (_cbByteSome.value().aspectNumericConstKind() != NumericKind.BYTE) {
        return EResult.err(loc, "source.sha256: list element is not a byte");
      }
      bytes[_cbI] = _cbByteSome.value().aspectNumericConstGet(_cbElem).byteValue();
    }
    if (bytes.length != 32) {
      return EResult.err(loc, "source.sha256: expected 32 bytes, got " + bytes.length);
    }
    return EResult.ok(
        new CodeValue(
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
    return EResult.ok(new ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SOURCE_SHA256_IDENTITY).optIsSome();
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
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
