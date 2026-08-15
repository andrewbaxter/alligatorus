package compilerxyz.eval;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.dynamic.DynCompileErrors;
import compilerxyz.eval.dynamic.DynException;
import compilerxyz.eval.dynamic.DynPanic;
import compilerxyz.eval.dynamic.EmitCtx;
import compilerxyz.eval.dynamic.EvalRootEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.Prefixes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenBuiltinTree;
import compilerxyz.gen.Value;
import compilerxyz.langhelp.GlobalContext;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TSet;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.safejson.JsonValueNull;
import java.util.ArrayDeque;
import static compilerxyz.util.MustCast.mustCast;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;

public final class CompileImport {
  private CompileImport() {}

  public record CompileImportResult(CompileRes res, TList<CompileId> localDeps) {}

  public static CompileImportResult compileImportId(
      GlobalContext gx,
      ImportStateId importState,
      Expr.Module source,
      TList<CompileId> compilePath,
      SourceSpecAbsolute currentSpec) {
    Expr rootExpr =
        switch (source) {
          case Expr.Module.V1 v1 -> v1.expr();
        };
    var rootLoc = rootExpr.loc();
    var sourceId = rootLoc.source();
    var extendedPath = new TList<CompileId>(compilePath.size() + 1);
    extendedPath.addAll(compilePath);
    extendedPath.add(new CompileId(importState, sourceId));
    var cix = new CompileImportContext(gx, importState, sourceId, extendedPath, currentSpec);

    var excess = maxLowerExcess(rootExpr, 0, 0);
    for (var i = 0; i < excess; i++) {
      rootExpr = new Expr(rootLoc, new Expr.Stage(rootExpr));
    }

    var allLogs = new TList<LogEntry>();
    while (true) {
      boolean isStage = switch (rootExpr.variant()) {
        case Expr.Stage s -> true;
        case Expr.As a -> false;
        case Expr.Bind b -> false;
        case Expr.Call c -> false;
        case Expr.Field f -> false;
        case Expr.Foreach f -> false;
        case Expr.Graft.Pending p -> false;
        case Expr.Graft.Evaluated g -> false;
        case Expr.If i -> false;
        case Expr.Labeled l -> false;
        case Expr.Literal l -> false;
        case Expr.Lower l -> false;
        case Expr.Match m -> false;
        case Expr.OperatorBinary o -> false;
        case Expr.OperatorUnary o -> false;
        case Expr.Record r -> false;
        case Expr.Return r -> false;
        case Expr.ScopeUse s -> false;
        case Expr.Seq s -> false;
        case Expr.Set s -> false;
        case Expr.Try t -> false;
        case Expr.Tuple t -> false;
      };
      EvalOutcome evalResult;
      evalBlock: {
        var evalRootLoc = rootExpr.loc();
        var evalVx = new EvalContext(cix);
        var evalRootScope = new Scope(evalVx);
        var evalTarget = new TargetDynamic(evalVx.nextLocalId);
        var evalXx = new ExprContext(evalRootScope, evalTarget, evalVx, importState, Opt.some(new TypeStringConst()));
        var _bindingRes = GenBuiltinTree.installRoot(evalXx, evalRootLoc, importState);
        if (_bindingRes.hasErrors()) {
          evalResult = new EvalOutcome.Err(_bindingRes.errorsOrNull().optUnwrap());
          break evalBlock;
        }
        var _rootBinding = _bindingRes.requireValue("Eval::eval installRoot");
        evalXx.vx.ix.builtinScopeRootBinding = Opt.some(_rootBinding);
        var scopeEntries = new TMap<ConstKey, Binding>();
        scopeEntries.put(new ConstKey.Str(Eval.KEY_BUILTINS), _rootBinding);
        for (var entry : scopeEntries.entrySet()) {
          evalRootScope.pushNamed(entry.getKey(), entry.getValue());
        }
        var evalBinaryName = "compilerxyz.eval.gen.EvalRoot$" + evalXx.vx.ix.evalRootCounter.next();
        evalVx.evalRootInternalName = EvalRootEmitter.toInternalName(evalBinaryName);
        var cvRes = ConstEval.constEval(evalXx, rootExpr);
        if (cvRes.hasErrors()) {
          evalResult = new EvalOutcome.Err(cvRes.errorsOrNull().optUnwrap());
          break evalBlock;
        }
        var evalRes = cvRes.requireValue("Eval::eval constEval");
        var cv = evalRes.cv();
        Opt<CodeDynamic> stage1Code;
        Opt<Value> fallbackValue;
        Type valueType;
        if (cv.value().optIsNone()) {
          var codeDyn = cv.code().codeDynamic();
          if (codeDyn == null) {
            evalResult = new EvalOutcome.Err(
                TList.of(
                    new ExprErr(
                        evalRootLoc,
                        TList.of(
                            new ExprSubErr.Opaque(
                                "eval: const_eval produced no compile-time value and no dynamic code")))));
            break evalBlock;
          }
          var nullType = new TypeNullDynamic();
          var seq = new JavaBytecodeSequence();
          seq.add(codeDyn);
          seq.add(JavaBytecodeUtils.literalNull(evalRootLoc));
          stage1Code = Opt.some(new CodeDynamic(new Some(evalRootLoc), seq));
          fallbackValue = Opt.none();
          valueType = nullType;
        } else {
          var cvValue = cv.value().optUnwrap();
          valueType = cvValue.valueType();
          if (cv.code().codeIsNone()) {
            stage1Code = Opt.none();
            fallbackValue = Opt.some(cvValue);
          } else {
            var consumeRes = GenAspectConsumeDynamic.get(evalXx, evalRootLoc, valueType);
            if (consumeRes.hasErrors()) {
              evalResult = new EvalOutcome.Err(consumeRes.errorsOrNull().optUnwrap());
              break evalBlock;
            }
            var consume = consumeRes.requireValue("Eval::eval consume aspect");
            var dynamicRes = consume.aspectConsumeDynamicToDynamic(evalXx, evalRootLoc, cv);
            if (dynamicRes.hasErrors()) {
              evalResult = new EvalOutcome.Err(dynamicRes.errorsOrNull().optUnwrap());
              break evalBlock;
            }
            var dynamicCode = dynamicRes.requireValue("Eval::eval consume.toDynamic");
            stage1Code = Opt.some(dynamicCode);
            fallbackValue = Opt.none();
          }
        }
        if (stage1Code.optIsNone() && evalVx.definitionSets.isEmpty()) {
          if (fallbackValue.optIsNone()) {
            throw new IllegalStateException("eval: no dynamic code but also no fallback value");
          }
          evalResult = new EvalOutcome.Ok(fallbackValue.optUnwrap(), new TList<>(), false);
          break evalBlock;
        }
        var dependencyLoaders = new TList<DefinitionSetClassLoader>();
        var seenSets = new TSet<DefSetId>();
        var dss = new TList<DefinitionSet>();
        var toVisit = new ArrayDeque<DefinitionSet>();
        for (var ds : evalVx.definitionSets.values()) {
          toVisit.push(ds);
        }
        defSetLoop: while (true) {
          while (!toVisit.isEmpty()) {
            var ds = toVisit.pop();
            if (!seenSets.add(ds.defSetId)) {
              continue;
            }
            var installRes = ImportInstall.installDefSetToDynamic(evalXx, evalRootLoc, ds);
            if (installRes.hasErrors()) {
              evalResult = new EvalOutcome.Err(installRes.errorsOrNull().optUnwrap());
              break evalBlock;
            }
            var installedLoader = installRes.requireValue("Eval::eval installDefSetToDynamic");
            dependencyLoaders.add(installedLoader);
            for (var dep : ds.dependencies.values()) {
              if (!seenSets.contains(dep.defSetId)) {
                toVisit.push(dep);
              }
            }
            dss.add(ds);
          }
          boolean foundNew = false;
          for (var ds : evalVx.definitionSets.values()) {
            if (!seenSets.contains(ds.defSetId)) {
              toVisit.push(ds);
              foundNew = true;
            }
          }
          if (!foundNew) {
            break defSetLoop;
          }
        }
        Object rawResult;
        try {
          if (stage1Code.optIsNone()) {
            if (fallbackValue.optIsNone()) {
              throw new IllegalStateException("eval: no stage1 code and no fallback value");
            }
            evalResult = new EvalOutcome.Ok(fallbackValue.optUnwrap(), evalVx.logs, false);
            break evalBlock;
          }
          // inlined EvalRootEmitter.emit
          var _ereInternalName = EvalRootEmitter.toInternalName(evalBinaryName);
          var _ereCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
              return JvmConstants.INTNAME_OBJECT.value();
            }
          };
          _ereCw.visit(Opcodes.V11,
              Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
              _ereInternalName, null, JvmConstants.INTNAME_OBJECT.value(), null);
          var _ereFieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
          _ereFieldDescriptors.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, EvalRootEmitter.DESC_EXPR_CONTEXT);
          var _ereExternalFieldNames = new TList<String>(evalVx.externals.size());
          var _ereExternalFieldDescs = new TList<JavaDataDescriptor>(evalVx.externals.size());
          for (int _ereI = 0; _ereI < evalVx.externals.size(); _ereI++) {
            var _ereEntry = evalVx.externals.get(_ereI);
            var _ereFieldName = Prefixes.EXTERNAL_PREFIX + _ereI;
            _ereExternalFieldNames.add(_ereFieldName);
            var _ereDesc = JavaDataDescriptor.fromClass(_ereEntry.external().externalHostClass());
            _ereExternalFieldDescs.add(_ereDesc);
            _ereFieldDescriptors.put(_ereFieldName, _ereDesc);
          }
          for (var _ereFieldEntry : _ereFieldDescriptors.entrySet()) {
            _ereCw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                _ereFieldEntry.getKey(), _ereFieldEntry.getValue().value(), null, null).visitEnd();
          }
          var _ereMdescRun = JavaMethodDescriptor.fromParts(
              JvmConstants.DESC_OBJECT, TList.of());
          var _ereMv = _ereCw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              "run", _ereMdescRun.value(), null, null);
          var _ereCtx = new EmitCtx(_ereInternalName, 0);
          var _ereSeq = new JavaBytecodeSequence();
          var _ereOwnerInternal = JavaInternalName.of(_ereInternalName);
          for (int _ereI = 0; _ereI < evalVx.externals.size(); _ereI++) {
            var _ereEntry = evalVx.externals.get(_ereI);
            _ereSeq.add(JavaBytecodeUtils.accessStaticField(
                CodeLoc.Infallible.INSTANCE,
                _ereOwnerInternal, _ereExternalFieldNames.get(_ereI), _ereExternalFieldDescs.get(_ereI)));
            _ereSeq.add(new CodeDynamic(
                CodeLoc.Infallible.INSTANCE,
                new JavaBytecodeStoreLoad(Opcodes.ASTORE, _ereEntry.key())));
          }
          evalTarget.emit(_ereSeq, _ereCtx, stage1Code.optUnwrap());
          _ereSeq.add(JavaBytecodeUtils.inst(
              CodeLoc.Infallible.INSTANCE, Opcodes.ARETURN));
          JavaBytecodeUtils.renderMethod(_ereSeq, _ereMv, TList.of());
          _ereCw.visitEnd();
          var _ereBytecode = _ereCw.toByteArray();
          var _ereStaticFields = new TOrderedMap<String, Object>();
          for (int _ereI = 0; _ereI < evalVx.externals.size(); _ereI++) {
            _ereStaticFields.put(_ereExternalFieldNames.get(_ereI),
                evalVx.externals.get(_ereI).external().externalHostValue());
          }
          if (evalXx.vx.ix.classLoader.optIsNone()) {
            evalXx.vx.ix.classLoader =
                Opt.some(new DefinitionSetClassLoader(
                    evalXx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
          }
          var loader = evalXx.vx.ix.classLoader.optUnwrap();
          loader.registerClass(evalBinaryName, _ereBytecode);
          var rootStaticFields = new TOrderedMap<String, Object>();
          rootStaticFields.putAll(_ereStaticFields);
          rootStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, evalXx);
          loader.registerExternals(evalBinaryName, rootStaticFields);
          Class<?> rootClass;
          try {
            rootClass = Class.forName(evalBinaryName, true, loader);
          } catch (ClassNotFoundException e) {
            throw new AssertionError(
                "Generated root class " + evalBinaryName + " failed to load", e);
          }
          try {
            var runMethod = rootClass.getDeclaredMethod("run");
            rawResult = runMethod.invoke(null);
          } catch (ReflectiveOperationException e) {
            var cause = e.getCause();
            if (cause instanceof DynPanic panic) {
              evalResult = new EvalOutcome.Err(
                  TList.of(new ExprErr(evalRootLoc, TList.of(new ExprSubErr.Opaque("panic: " + panic.getMessage())))));
              break evalBlock;
            }
            if (cause instanceof DynCompileErrors compileErrs) {
              evalResult = new EvalOutcome.Err(compileErrs.errors);
              break evalBlock;
            }
            if (cause instanceof DynException dynEx) {
              var rendered = dynEx.payload == null ? "null" : dynEx.payload.toString();
              evalResult = new EvalOutcome.Err(
                  TList.of(new ExprErr(evalRootLoc, TList.of(new ExprSubErr.Opaque(rendered)))));
              break evalBlock;
            }
            if (cause instanceof RuntimeException re) {
              throw re;
            }
            var causeMsg = cause == null ? e.toString() : (cause.getClass().getName() + ": " + cause.getMessage());
            throw new AssertionError(
                "Generated root class " + evalBinaryName + " invoke failed: " + causeMsg, e);
          }
        } finally {
        }
        Value finalValue;
        if (fallbackValue.optIsSome()) {
          finalValue = fallbackValue.optUnwrap();
        } else {
          var liftRes = GenAspectLift.get(evalXx, evalRootLoc, valueType);
          if (liftRes.hasErrors()) {
            evalResult = new EvalOutcome.Err(liftRes.errorsOrNull().optUnwrap());
            break evalBlock;
          }
          var liftAspect = liftRes.requireValue("Eval::eval lift aspect");
          var liftedRes = liftAspect.aspectLiftLift(evalXx, evalRootLoc, Opt.ofNullable(rawResult));
          if (liftedRes.hasErrors()) {
            evalResult = new EvalOutcome.Err(liftedRes.errorsOrNull().optUnwrap());
            break evalBlock;
          }
          finalValue = liftedRes.requireValue("Eval::eval lift result");
        }
        evalResult = new EvalOutcome.Ok(finalValue, evalVx.logs, true);
      }
      switch (evalResult) {
        case EvalOutcome.Err err -> {
          return new CompileImportResult(
              new CompileRes(allLogs, new CompileResResult.Err(err.errors())),
              cix.localDependencies);
        }
        case EvalOutcome.Ok ok -> {
          allLogs.addAll(ok.logs());
          if (!isStage) {
            if (compilePath.isEmpty() && ok.producedDynamicRoot()) {
              return new CompileImportResult(
                  new CompileRes(
                      allLogs,
                      new CompileResResult.Ok(
                          new Artifact.Body(
                              new ArtifactBody(
                                  new JsonValueNull(),
                                  new ArtifactBodyExporter.Root())))),
                  cix.localDependencies);
            }
            var vx = new EvalContext(cix);
            var rootScope = new Scope(vx);
            var target = new TargetDynamic(vx.nextLocalId);
            var xx =
                new ExprContext(
                    rootScope, target, vx, importState, Opt.some(new TypeStringDynamic()));
            var exportErrors = new TList<ExprErr>();
            var exportAsp = GenAspectArtifact.get(xx, rootLoc, ok.value().valueType());
            if (exportAsp.hasErrors()) {
              exportAsp.drainInto(exportErrors);
              return new CompileImportResult(
                  new CompileRes(allLogs, new CompileResResult.Err(exportErrors)),
                  cix.localDependencies);
            }
            var exportAspVal = exportAsp.requireValue("ArtifactSupport::exportArtifact aspect");
            var exportArt = exportAspVal.aspectArtifactExporter().artifactExporterExport(
                xx, rootLoc, ArtifactSupport.valueAsExporterAny(ok.value()));
            if (exportArt.hasErrors()) {
              exportArt.drainInto(exportErrors);
              return new CompileImportResult(
                  new CompileRes(allLogs, new CompileResResult.Err(exportErrors)),
                  cix.localDependencies);
            }
            var artifact = exportArt.requireValue("ArtifactSupport::exportArtifact artifact");
            return new CompileImportResult(
                new CompileRes(allLogs, new CompileResResult.Ok(artifact)), cix.localDependencies);
          }
          rootExpr = mustCast(Expr.class, ok.value());
        }
      }
    }
  }

  private static int maxLowerExcess(Expr expr, int sd, int ld) {
    return switch (expr.variant()) {
      case Expr.Lower l -> {
        var newLd = ld + 1;
        var excess = Math.max(0, newLd - sd);
        yield Math.max(excess, maxLowerExcess(l.expr(), sd, newLd));
      }
      case Expr.Stage s -> maxLowerExcess(s.expr(), sd + 1, ld);
      case Expr.Graft g ->
          switch (g) {
            case Expr.Graft.Pending p -> maxLowerExcess(p.expr(), sd, ld);
            case Expr.Graft.Evaluated e -> 0;
          };
      case Expr.Literal lit -> 0;
      case Expr.ScopeUse u -> 0;
      case Expr.Labeled lb -> maxLowerExcess(lb.inner(), sd, ld);
      case Expr.Return rt ->
          rt.value().optIsSome() ? maxLowerExcess(rt.value().optUnwrap(), sd, ld) : 0;
      case Expr.As a -> Math.max(maxLowerExcess(a.base(), sd, ld), maxLowerExcess(a.target(), sd, ld));
      case Expr.Call c -> Math.max(maxLowerExcess(c.func(), sd, ld), maxLowerExcess(c.arg(), sd, ld));
      case Expr.Bind b -> Math.max(maxLowerExcess(b.base(), sd, ld), maxLowerExcess(b.value(), sd, ld));
      case Expr.Set s -> Math.max(maxLowerExcess(s.base(), sd, ld), maxLowerExcess(s.value(), sd, ld));
      case Expr.Field f -> Math.max(maxLowerExcess(f.base(), sd, ld), maxLowerExcess(f.key(), sd, ld));
      case Expr.Seq s -> {
        var m = 0;
        for (var sub : s.exprs()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
        yield m;
      }
      case Expr.Tuple t -> {
        var m = 0;
        for (var sub : t.elements()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
        yield m;
      }
      case Expr.Record r -> {
        var m = 0;
        for (var entry : r.entries()) {
          m = Math.max(m, maxLowerExcess(entry.key(), sd, ld));
          m = Math.max(m, maxLowerExcess(entry.value(), sd, ld));
        }
        yield m;
      }
      case Expr.If i -> {
        var m = 0;
        for (var branch : i.conditions()) {
          m = Math.max(m, maxLowerExcess(branch.condition(), sd, ld));
          m = Math.max(m, maxLowerExcess(branch.body(), sd, ld));
        }
        var el = i.else_();
        if (el.optIsSome()) {
          m = Math.max(m, maxLowerExcess(el.optUnwrap(), sd, ld));
        }
        yield m;
      }
      case Expr.OperatorBinary op ->
          Math.max(maxLowerExcess(op.base(), sd, ld), maxLowerExcess(op.reference(), sd, ld));
      case Expr.OperatorUnary op -> maxLowerExcess(op.base(), sd, ld);
      case Expr.Foreach f -> {
        var m = maxLowerExcess(f.base(), sd, ld);
        m = Math.max(m, maxLowerExcess(f.elementKey(), sd, ld));
        m = Math.max(m, maxLowerExcess(f.body(), sd, ld));
        yield m;
      }
      case Expr.Match m -> {
        var x = maxLowerExcess(m.base(), sd, ld);
        x = Math.max(x, maxLowerExcess(m.elementKey(), sd, ld));
        for (var c : m.cases()) {
          x = Math.max(x, maxLowerExcess(c.condition(), sd, ld));
          x = Math.max(x, maxLowerExcess(c.body(), sd, ld));
        }
        var fb = m.fallback();
        if (fb.optIsSome()) {
          x = Math.max(x, maxLowerExcess(fb.optUnwrap(), sd, ld));
        }
        yield x;
      }
      case Expr.Try t -> {
        var m = maxLowerExcess(t.body(), sd, ld);
        var cc = t.catchClause();
        if (cc.optIsSome()) {
          var c = cc.optUnwrap();
          m = Math.max(m, maxLowerExcess(c.bindingName(), sd, ld));
          m = Math.max(m, maxLowerExcess(c.exceptionType(), sd, ld));
          m = Math.max(m, maxLowerExcess(c.body(), sd, ld));
        }
        var fb = t.finallyBlock();
        if (fb.optIsSome()) {
          m = Math.max(m, maxLowerExcess(fb.optUnwrap(), sd, ld));
        }
        yield m;
      }
    };
  }

  public sealed interface EvalOutcome {

    record Ok(Value value, TList<LogEntry> logs, boolean producedDynamicRoot)
        implements EvalOutcome {}

    record Err(TList<ExprErr> errors) implements EvalOutcome {}
  }
}
