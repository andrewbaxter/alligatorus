package compilerxyz.main.eval.value.source;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportRun;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6161L)
public record TypeBundle(SourceSpecAbsolute.Remote base)
        implements Type, AspectBind, AspectDropDynamicLight, AspectField {

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
            case Opt.None<Value> n -> {
                return EResult.err(loc, "bundle: field key has no compile-time value");
            }
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, keySome.value()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        ConstKey.Str s;
        switch (ckOpt.optUnwrap()) {
            case ConstKey.Str str -> s = str;
            case ConstKey.Uint u -> {
                return EResult.err(loc, "bundle: field key must be a string");
            }
        }
        var subSpec = new SourceSpecAbsolute.Remote(base.baseUrl(), base.hash(), s.value() + ".at");
        var _outcomeRes = ImportRun.compileAbsolute(xx, loc, subSpec);
        if (_outcomeRes.hasErrors()) return _outcomeRes.reerr();
        var _outcome = _outcomeRes.requireValue("importAbsolute");
        return switch (_outcome) {
            case ImportRun.SpecOutcome.BundleOut b ->
                EResult.ok(new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(
                                new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeBundle(b.base())))));
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
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BUNDLE_IDENTITY)
                .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
