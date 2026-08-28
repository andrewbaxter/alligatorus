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
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.gen.Value;
import compilerxyz.syntax.HashValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.artifact.Artifact;

@BuiltinPath("source.remote")
@BuiltinLight(uniqueId = 6158L)
public record TypeSourceRemote() implements Type, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public String typeDesc() {
    return "source.remote";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_REMOTE_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var argRes = SourceSpecs.argValue(loc, argument, "source.remote");
    if (argRes.hasErrors()) return argRes.reerr();
    var _ctVal = argRes.requireValue("source.remote arg");
    if (!(_ctVal instanceof Tuple _ctTuple) || _ctTuple.base.optIsSome()) {
      return EResult.err(loc, "source.remote: expected a const tuple");
    }
    var elements = new TList<Value>(_ctTuple.elements().size());
    for (var _ctElem : _ctTuple.elements()) {
      TupleElement.EvalRes _ctEr;
      switch (_ctElem) {
        case TupleElement.EvalRes e -> _ctEr = e;
        case TupleElement.Dynamic d -> { return EResult.err(loc, "source.remote: tuple element is not const"); }
      }
      var _ctRes = _ctEr.result();
      if (_ctRes.hasErrors()) return _ctRes.reerr();
      var _ctCv = _ctRes.requireValue("source.remote tuple element");
      Opt.Some<Value> _ctSome;
      switch (_ctCv.value()) {
        case Opt.Some<Value> s -> _ctSome = s;
        case Opt.None<Value> n -> { return EResult.err(loc, "source.remote: tuple element has no value"); }
      }
      elements.add(_ctSome.value());
    }
    if (elements.size() != 2) {
      return EResult.err(loc, "source.remote: expected (url, hash), got " + elements.size() + " arguments");
    }
    var urlRes = SourceSpecs.constString(xx, loc, elements.get(0), "source.remote url");
    if (urlRes.hasErrors()) return urlRes.reerr();
    var hashRes = (elements.get(1) instanceof ValueObjectConst voc_
        && voc_.type() instanceof TypeHash
        && voc_.data() instanceof HashValue hv_)
        ? EResult.ok(hv_)
        : EResult.<HashValue>err(loc, "source.remote hash: expected a hash value");
    if (hashRes.hasErrors()) return hashRes.reerr();
    var remote =
        new SourceSpecValue.SourceSpecRemote(
            urlRes.requireValue("source.remote url"), hashRes.requireValue("source.remote hash"));
    return EResult.ok(
        new CodeValue(
            Code.concat(loc, self.code(), argument.code()),
            EvalOutcome.ofValue(SourceSpecs.spec(new SourceSpecValue.Remote(remote)))));
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
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SOURCE_REMOTE_IDENTITY).optIsSome();
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

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
