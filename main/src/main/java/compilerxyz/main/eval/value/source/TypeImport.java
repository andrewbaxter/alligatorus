package compilerxyz.main.eval.value.source;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportRun;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordBaseDynamic;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@BuiltinPath("import")
@BuiltinLight(uniqueId = 6160L)
public record TypeImport() implements Type, AspectFunction, AspectInitialBind, ExporterAny {

    @Override
    public String typeDesc() {
        return "import";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_IMPORT_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "import: argument must have a compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var recordAspectOpt = GenAspectRecord.getMaybe(xx, argValue.valueType());
        if (recordAspectOpt.optIsNone()) {
            return EResult.err(loc, "import: argument must be a record of source specs");
        }
        var takenFields = recordAspectOpt.optUnwrap().aspectRecordTakeFields(xx, loc, argValue);
        var erb = new Erb(loc);
        var specs = new TOrderedMap<ConstKey, SourceSpecValue>();
        for (var entry : takenFields.entrySet()) {
            var key = entry.getKey();
            var fieldRes = entry.getValue();
            if (fieldRes.hasErrors()) {
                fieldRes.drainInto(erb.errors());
                continue;
            }
            var fieldCv = fieldRes.requireValue("import field");
            if (fieldCv.value().optIsNone()
                    || !(fieldCv.value().optUnwrap() instanceof ValueObjectConst voc)
                    || !(voc.type() instanceof TypeSourceSpec)
                    || !(voc.data() instanceof SourceSpecValue specValue)) {
                erb.errPush("import: entry '" + key.constKeyToCanonical() + "' is not a source spec");
                continue;
            }
            specs.put(key, specValue);
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var keys = new TList<ConstKey>(specs.size());
        keys.addAll(specs.keySet());
        var outcomes = new TList<EResult<ImportRun.SpecOutcome>>(keys.size());
        if (keys.size() <= 1) {
            for (var key : keys) outcomes.add(ImportRun.compileOne(xx, loc, specs.getOrNull(key)));
        } else {
            var pool = Executors.newFixedThreadPool(Math.min(keys.size(), 16));
            try {
                var futures = new ArrayList<Future<EResult<ImportRun.SpecOutcome>>>(keys.size());
                for (var key : keys) {
                    var spec = specs.getOrNull(key);
                    futures.add(pool.submit(() -> ImportRun.compileOne(xx, loc, spec)));
                }
                for (var future : futures) {
                    try {
                        outcomes.add(future.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("import: interrupted", e);
                    } catch (ExecutionException e) {
                        var cause = e.getCause();
                        throw cause instanceof RuntimeException re
                                ? re
                                : new RuntimeException("import task failed", cause);
                    }
                }
            } finally {
                pool.shutdown();
            }
        }
        var ix = xx.vx.ix;
        var fields = new TOrderedMap<ConstKey, RecordField>();
        for (var i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            var outcomeRes = outcomes.get(i);
            if (outcomeRes.hasErrors()) {
                outcomeRes.drainInto(erb.errors());
                continue;
            }
            var outcome = outcomeRes.requireValue("import outcome");
            switch (outcome) {
                case ImportRun.SpecOutcome.BundleOut b -> {
                    var bundleValue = new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeBundle(b.base()));
                    fields.put(
                            key,
                            new RecordField.EvalRes(
                                    EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(bundleValue)))));
                }
                case ImportRun.SpecOutcome.ModuleOut m -> {
                    switch (m.abs()) {
                        case SourceSpecAbsolute.Local ignored -> ix.localDependencies.add(m.compileId());
                        case SourceSpecAbsolute.Virtual v -> {}
                        case SourceSpecAbsolute.Remote r -> {}
                    }
                    var valueRes = ImportRun.materialize(xx, loc, m.res());
                    if (valueRes.hasErrors()) {
                        valueRes.drainInto(erb.errors());
                        continue;
                    }
                    fields.put(key, new RecordField.EvalRes(EResult.ok(valueRes.requireValue("import value"))));
                }
            }
        }
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        Value resultRecord = new Record(Opt.<RecordBaseDynamic>none(), fields);
        return EResult.ok(new CodeValue(argument.code(), EvalOutcome.ofValue(resultRecord)));
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
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IMPORT_IDENTITY)
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
