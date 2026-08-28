package compilerxyz.eval.value.source;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ImportRun;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.Value;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.artifact.Artifact;

@BuiltinLight(uniqueId = 6161L)
public record TypeBundle(SourceSpecAbsolute.Remote base)
    implements Type, AspectBind, AspectDropDynamicLight, AspectField, ExporterAny {

  @Override
  public String typeDesc() {
    return "bundle(" + base.baseUrl() + ")";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_BUNDLE_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    Opt.Some<Value> keySome;
    switch (key.value()) {
      case Opt.Some<Value> s -> keySome = s;
      case Opt.None<Value> n -> { return EResult.err(loc, "bundle: field key has no compile-time value"); }
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    ConstKey.Str s;
    switch (ckOpt.optUnwrap()) {
      case ConstKey.Str str -> s = str;
      case ConstKey.Uint u -> { return EResult.err(loc, "bundle: field key must be a string"); }
    }
    var subSpec = new SourceSpecAbsolute.Remote(base.baseUrl(), base.hash(), s.value() + ".at");
    var _outcomeRes = ImportRun.compileAbsolute(xx, loc, subSpec);
    if (_outcomeRes.hasErrors()) return _outcomeRes.reerr();
    var _outcome = _outcomeRes.requireValue("importAbsolute");
    return switch (_outcome) {
      case ImportRun.SpecOutcome.BundleOut b ->
          EResult.ok(
              new CodeValue(
                  Code.none(loc),
                  EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeBundle(b.base())))));
      case ImportRun.SpecOutcome.ModuleOut m -> {
        switch (m.abs()) {
          case SourceSpecAbsolute.Local ignored -> xx.vx.ix.localDependencies.add(m.compileId());
          case SourceSpecAbsolute.Virtual v -> {}
          case SourceSpecAbsolute.Remote r -> {}
        }
        yield ImportRun.materialize(xx, loc, m.res());
      }
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return switch (cv.value()) {
      case Opt.Some<Value> some -> EResult.ok(new BindResult(cv.code(), new BindingValue(some.value())));
      case Opt.None<Value> none -> EResult.err(loc, "TypeBundle::bind: CodeValue has no compile-time value");
    };
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
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
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BUNDLE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
