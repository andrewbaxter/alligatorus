package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Counter;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.expr.CodeValueExternCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.expr.TypeExprStagedPlaceholder;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.Optional;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class TargetDynamic implements Target {
  private static final JavaInternalName INTNAME_DYN_EXCEPTION =
      JavaInternalName.fromClass(DynRuntime.DynException.class);
  private static final JavaInternalName INTNAME_EXPR_CONTEXT =
      JavaInternalName.of("compilerxyz/eval/ExprContext");
  private static final JavaInternalName INTNAME_OBJECT_ARRAY_ELEM =
      JavaInternalName.of("[Ljava/lang/Object;");

  private static final JavaDataDescriptor DESC_EXPR_CONTEXT =
      JavaDataDescriptor.fromInternalName(INTNAME_EXPR_CONTEXT);
  private static final JavaDataDescriptor DESC_ITERATOR =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_ITERATOR);
  private static final JavaDataDescriptor DESC_LIST =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_LIST);
  private static final JavaDataDescriptor DESC_MAP =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP);
  private static final JavaDataDescriptor DESC_MAP_ENTRY =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP_ENTRY);
  private static final JavaDataDescriptor DESC_RUNTIME_EXCEPTION =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_RUNTIME_EXCEPTION);

  private static final JavaDataDescriptor DESC_SET =
      JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_SET);
  private static final JavaMethodDescriptor MDESC_ITERATOR =
      JavaMethodDescriptor.fromParts(DESC_ITERATOR);
  private static final JavaMethodDescriptor MDESC_HAS_NEXT =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL);
  private static final JavaMethodDescriptor MDESC_NEXT =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_BOOL_VALUE =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL);
  private static final JavaMethodDescriptor MDESC_MAP_ENTRY_GET =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_MAP_ENTRY_SET =
      JavaMethodDescriptor.fromParts(DESC_SET);
  private static final JavaMethodDescriptor MDESC_MAP_PUT =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_MAP_CONTAINS_KEY =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_STRING_EQUALS =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL, JvmConstants.DESC_OBJECT);

  private final Counter localIds;

  private compilerxyz.util.TSet<BindingId.Def> staticFunctionDefs = new compilerxyz.util.TSet<>();

  public TargetDynamic(Counter localIds) {
    this.localIds = localIds;
  }

  public void registerStaticFunctionDefs(compilerxyz.util.TSet<BindingId.Def> defs) {
    this.staticFunctionDefs = defs;
  }

  public boolean isStaticFunctionDef(BindingId.Def def) {
    return staticFunctionDefs.contains(def);
  }

  @Override
  public EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      return EResult.ok(Code.none(loc));
    }
    var value = cv.value().optUnwrap();
    var vtype = value.valueTypeOf();
    var dropAspect = AspectDropDynamic.getMaybe(xx, vtype);
    if (dropAspect.optIsNone()) {
      return EResult.ok(Code.none(loc));
    }
    return dropAspect.optUnwrap().drop(xx, loc, cv);
  }

  @Override
  public BindResult targetBind(QExprId loc, TargetBindSub sub) {
    var local = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var bind = new JavaBytecode.BlockEntry.Bind(local, sub.data());
    var block = new CodeDynamic(loc, new JavaBytecode.Block(TList.of(bind)));
    return new BindResult(block, local);
  }

  @Override
  public EResult<Code> targetTryFinally(QExprId loc, Code body, Code finally_) {
    var bodyDyn = body.dynamic();
    var finallyDyn = finally_.dynamic();
    if (bodyDyn == null && finallyDyn == null) {
      return EResult.ok(Code.none(loc));
    }
    if (finallyDyn == null) {
      return EResult.ok(Code.of(loc, bodyDyn));
    }
    if (bodyDyn == null) {
      return EResult.ok(Code.of(loc, finallyDyn));
    }
    var tryCode =
        new CodeDynamic(loc, new JavaBytecode.Try(
                bodyDyn,
                Opt.none(),
                Opt.some(finallyDyn)));
    return EResult.ok(Code.of(loc, tryCode));
  }

  @Override
  public EResult<Code> targetSeq(QExprId loc, TList<Code> entries) {
    var dynamicEntries = new TList<JavaBytecode.BlockEntry>();
    for (var entry : entries) {
      var dyn = entry.dynamic();
      if (dyn == null) {
        continue;
      }
      if (dyn.code() instanceof JavaBytecode.Block block
          && !block.entries().isEmpty()
          && allBinds(block.entries())) {
        for (var be : block.entries()) {
          dynamicEntries.add(be);
        }
      } else {
        dynamicEntries.add(new JavaBytecode.BlockEntry.Code(dyn));
      }
    }
    if (dynamicEntries.isEmpty()) {
      return EResult.ok(Code.none(loc));
    }
    return EResult.ok(
        Code.of(loc, new CodeDynamic(loc, new JavaBytecode.Block(dynamicEntries))));
  }

  private static boolean allBinds(TList<JavaBytecode.BlockEntry> entries) {
    for (var e : entries) {
      if (!(e instanceof JavaBytecode.BlockEntry.Bind)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public EResult<CodeValue> targetIf_(
      ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_) {
    var erb = new Erb(loc);
    var dynamicConditions = new TList<JavaBytecode.IfBranch>();
    compilerxyz.eval.Type resultType = null;
    for (var branch : conditions) {
      var condJsOpt = cvToDynamic(erb, xx, loc, branch.condition());
      if (condJsOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCvOpt = erb.mustEr(ConstEval.constEval(xx, branch.body()));
      if (bodyCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = bodyCvOpt.get();
      if (resultType == null) {
        if (!(bodyCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::if_: body CodeValue has no value");
        }
        resultType = some.value().valueTypeOf();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, bodyCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyCv = convertedOpt.get();
      }
      var bodyJsOpt = cvToDynamic(erb, xx, loc, bodyCv);
      if (bodyJsOpt.isEmpty()) return EResult.errs(erb.errors());
      dynamicConditions.add(new JavaBytecode.IfBranch(condJsOpt.get(), bodyJsOpt.get()));
    }
    Opt<CodeDynamic> dynamicElse;
    if (else_ instanceof Opt.Some<Expr> someElse) {
      var elseCvOpt = erb.mustEr(ConstEval.constEval(xx, someElse.value()));
      if (elseCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var elseCv = elseCvOpt.get();
      if (resultType == null) {
        if (!(elseCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::if_: else CodeValue has no value");
        }
        resultType = some.value().valueTypeOf();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, elseCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        elseCv = convertedOpt.get();
      }
      var elseJsOpt = cvToDynamic(erb, xx, loc, elseCv);
      if (elseJsOpt.isEmpty()) return EResult.errs(erb.errors());
      dynamicElse = Opt.some(elseJsOpt.get());
    } else {
      dynamicElse = Opt.none();
    }
    var ifJs =
        new CodeDynamic(loc, new JavaBytecode.If(dynamicConditions, dynamicElse));
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(AspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().toValue(loc, ifJs));
  }

  @Override
  public EResult<CodeValue> targetMatch_(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      ConstKey elementKey,
      TList<TargetCase> cases,
      Opt<Expr> fallback) {
    var erb = new Erb(loc);
    if (!(base.value() instanceof Opt.Some<Value> baseSome)) {
      return EResult.err(loc, "TargetDynamic::match_: base CodeValue has no value");
    }
    var baseType = baseSome.value().valueTypeOf();
    var baseConsumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, baseType));
    if (baseConsumeOpt.isEmpty()) return EResult.errs(erb.errors());
    var baseJsOpt = erb.mustEr(baseConsumeOpt.get().toDynamic(xx, loc, base));
    if (baseJsOpt.isEmpty()) return EResult.errs(erb.errors());
    var elementScopeId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var dynamicCases = new TList<JavaBytecode.MatchBranch>(cases.size());
    compilerxyz.eval.Type resultType = null;
    for (var tc : cases) {
      var childScope = new Scope(xx.scope);
      var ibOpt = erb.mustEr(AspectInitialBind.get(xx, loc, tc.variantType()));
      if (ibOpt.isEmpty()) return EResult.errs(erb.errors());
      childScope.values.put(elementKey, ibOpt.get().initialBind(elementScopeId));
      var childXx = xx.child(childScope);
      var bodyCvOpt = erb.mustEr(ConstEval.constEval(childXx, tc.body()));
      if (bodyCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = bodyCvOpt.get();
      if (resultType == null) {
        if (!(bodyCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::match_: body CodeValue has no value");
        }
        resultType = some.value().valueTypeOf();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(childXx, loc, resultType, bodyCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyCv = convertedOpt.get();
      }
      var bodyJsOpt = cvToDynamic(erb, childXx, loc, bodyCv);
      if (bodyJsOpt.isEmpty()) return EResult.errs(erb.errors());
      dynamicCases.add(
          new JavaBytecode.MatchBranch(
              new ConstKey.Str(tc.dynamicTag()).toCanonical(), bodyJsOpt.get()));
    }
    Opt<CodeDynamic> dynamicFallback;
    if (fallback instanceof Opt.Some<Expr> someFb) {
      var fbCvOpt = erb.mustEr(ConstEval.constEval(xx, someFb.value()));
      if (fbCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var fbCv = fbCvOpt.get();
      if (resultType == null) {
        if (!(fbCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::match_: fallback CodeValue has no value");
        }
        resultType = some.value().valueTypeOf();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, fbCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        fbCv = convertedOpt.get();
      }
      var fbJsOpt = cvToDynamic(erb, xx, loc, fbCv);
      if (fbJsOpt.isEmpty()) return EResult.errs(erb.errors());
      dynamicFallback = Opt.some(fbJsOpt.get());
    } else {
      dynamicFallback = Opt.none();
    }
    var matchJs =
        new CodeDynamic(loc, new JavaBytecode.Match(baseJsOpt.get(), elementScopeId, dynamicCases, dynamicFallback));
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(AspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().toValue(loc, matchJs));
  }

  private static Optional<CodeDynamic> cvToDynamic(
      Erb erb, ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      erb.errPush("TargetDynamic::if_: CodeValue has no value");
      return Optional.empty();
    }
    var vtype = some.value().valueTypeOf();
    var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
    if (consumeOpt.isEmpty()) return Optional.empty();
    return erb.mustEr(consumeOpt.get().toDynamic(xx, loc, cv));
  }

  @Override
  public EResult<CodeValue> targetStage(
      ExprContext xx,
      QExprId loc,
      Expr tree,
      TList<CodeValue> lowerValues,
      TList<CodeValue> graftValues) {
    var erb = new Erb(loc);
    var treeCell = new StagedExprCell(tree);
    var treeId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(treeId, new External.StagedExprCellExternal(treeCell)));
    var stageArgs = new TList<CodeDynamic>();
    stageArgs.add(new CodeDynamic(loc, new JavaBytecode.BindingUse(treeId)));
    stageArgs.add(
        new CodeDynamic(loc, new JavaBytecode.LoweredSequence(
                JavaBytecodeUtils.jsonLiteral(new JsonValueNumber(lowerValues.size())))));
    for (var cv : lowerValues) {
      if (cv.code().isNone()) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
        }
        var cell = new StagedExprCell(some.value());
        var id = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(id, new External.StagedExprCellExternal(cell)));
        stageArgs.add(
            new CodeDynamic(loc, new JavaBytecode.LoweredSequence(JavaBytecodeUtils.jsonLiteral(new JsonValueNull()))));
        stageArgs.add(new CodeDynamic(loc, new JavaBytecode.BindingUse(id)));
      } else {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic lower CodeValue has no value");
        }
        var vtype = some.value().valueTypeOf();
        var liftCell =
            new StageLiftCell(
                loc, xx.vx, xx.importState, xx.scope, vtype, xx.currentExceptionType, this);
        var liftId = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(liftId, new External.StageLiftCellExternal(liftCell)));
        var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().toDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        stageArgs.add(new CodeDynamic(loc, new JavaBytecode.BindingUse(liftId)));
        stageArgs.add(innerJsOpt.get());
      }
    }
    for (var cv : graftValues) {
      if (cv.code().isNone()) {
        var cell =
            new CodeValueExternCell(
                new CodeValue(Code.none(loc), cv.value()));
        var id = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(id, new External.CodeValueExternCellExternal(cell)));
        stageArgs.add(new CodeDynamic(loc, new JavaBytecode.BindingUse(id)));
      } else {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
        }
        var vtype = some.value().valueTypeOf();
        var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().toDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        stageArgs.add(innerJsOpt.get());
      }
    }
    var stageCall =
        compilerxyz.eval.dynamic.DynRuntime.STAGE_TYPE.emitInvocation(
            loc, xx.vx, stageArgs);
    var resultValue =
        new ValueDynamic(new ValueDynamic.Source.Expr(), new TypeExprStagedPlaceholder());
    return EResult.ok(new CodeValue(Code.of(loc, stageCall), Opt.some(resultValue)));
  }

  public void emit(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
    emitInner(seq, ctx, code);
  }

  private void emitInner(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
    switch (code.code()) {
      case JavaBytecode.BindingSet v -> emitBindingSet(seq, ctx, v);
      case JavaBytecode.BindingUse v -> emitBindingUse(seq, ctx, v);
      case JavaBytecode.Block v -> emitBlock(seq, ctx, v);
      case JavaBytecode.Foreach v -> emitForeach(seq, ctx, v);
      case JavaBytecode.ForeachMap v -> emitForeachMap(seq, ctx, v);
      case JavaBytecode.If v -> emitIf(seq, ctx, v);
      case JavaBytecode.Match v -> emitMatch(seq, ctx, v);
      case JavaBytecode.LoweredSequence v -> emitSequence(seq, ctx, v);
      case JavaBytecode.Try v -> emitTry(seq, ctx, v);
      case CodeDynamic nested -> emitInner(seq, ctx, nested);
      case JavaBytecode jbc -> seq.add(jbc);
    }
  }

  private void emitBindingSet(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.BindingSet v) {
    emitInner(seq, ctx, v.value());
    switch (v.base()) {
      case BindingId.Local local -> {
        var slot = slotForBinding(ctx, local);
        seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
        seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, slot));
      }
      case BindingId.Def def -> {
        if (isStaticFunctionDef(def)) {
          throw new AssertionError(
              "BindingSet(Def) for statically-dispatched function " + def
                  + " is not supported (function-as-value path is deferred)");
        }
        seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
        seq.add(
            JavaBytecodeUtils.setStaticField(
                -1,
                JavaInternalName.of(ctx.ownerInternalName),
                ctx.fieldPrefix + ImportInstall.bindingIdDynamicKey(def),
                JvmConstants.DESC_OBJECT));
      }
      case BindingId.Builtin builtin -> {
        seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
        seq.add(
            JavaBytecodeUtils.setStaticField(
                -1,
                JavaInternalName.of(ctx.ownerInternalName),
                ctx.fieldPrefix + Prefixes.BUILTIN_PREFIX + builtin.index(),
                JvmConstants.DESC_OBJECT));
      }
    }
  }

  private void emitBindingUse(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.BindingUse v) {
    switch (v.id()) {
      case BindingId.Local local ->
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, ctx.slotOf(local)));
      case BindingId.Def def -> {
        if (isStaticFunctionDef(def)) {
            throw new IllegalStateException(
                    "Static-def function-as-value not yet implemented; def="
                            + def
                            + ". Needs LambdaMetafactory INVOKEDYNAMIC or a wrapper class emitter.");
        } else {
          seq.add(
              JavaBytecodeUtils.accessStaticField(
                  -1,
                  JavaInternalName.of(ctx.ownerInternalName),
                  ctx.fieldPrefix + ImportInstall.bindingIdDynamicKey(def),
                  JvmConstants.DESC_OBJECT));
        }
      }
      case BindingId.Builtin builtin ->
          seq.add(
              JavaBytecodeUtils.accessStaticField(
                  -1,
                  JavaInternalName.of(ctx.ownerInternalName),
                  ctx.fieldPrefix + Prefixes.BUILTIN_PREFIX + builtin.index(),
                  JvmConstants.DESC_OBJECT));
    }
  }

  private void emitBlock(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.Block v) {
    var entries = v.entries();
    if (entries.isEmpty()) {
      seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
      return;
    }
    for (int i = 0; i < entries.size(); i++) {
      var entry = entries.get(i);
      var isLast = i == entries.size() - 1;
      switch (entry) {
        case JavaBytecode.BlockEntry.Bind bind -> {
          emitInner(seq, ctx, bind.value());
          var slot = slotForBinding(ctx, bind.base());
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, slot));
          if (isLast) {
            seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
          }
        }
        case JavaBytecode.BlockEntry.Code code -> {
          emitInner(seq, ctx, code.code());
          if (!isLast) {
            seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
          }
        }
      }
    }
  }

  private void emitArgs(JavaBytecodeSequence seq, EmitCtx ctx, TList<CodeDynamic> args) {
    if (args.isEmpty()) {
      seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
    } else if (args.size() == 1) {
      emitInner(seq, ctx, args.get(0));
    } else {
      seq.add(JavaBytecodeUtils.literalIntShortByte(args.size()));
      seq.add(
          new JavaBytecodeInstruction.Obj(
              new org.objectweb.asm.tree.TypeInsnNode(
                  Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
      for (int i = 0; i < args.size(); i++) {
        seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
        seq.add(JavaBytecodeUtils.literalIntShortByte(i));
        emitInner(seq, ctx, args.get(i));
        seq.add(JavaBytecodeUtils.inst(Opcodes.AASTORE));
      }
    }
  }

  private void emitForeach(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.Foreach v) {
      emitInner(seq, ctx, v.base());
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_ITERABLE)));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_ITERABLE, "iterator", MDESC_ITERATOR));
      emitIterLoop(seq, ctx, v.elementKey(), v.body(), false);
  }

  private void emitForeachMap(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.ForeachMap v) {
      emitInner(seq, ctx, v.base());
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP)));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_MAP, "entrySet", MDESC_MAP_ENTRY_SET));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_SET, "iterator", MDESC_ITERATOR));
      emitIterLoop(seq, ctx, v.elementKey(), v.body(), true);
  }

  private void emitIterLoop(
      JavaBytecodeSequence seq,
      EmitCtx ctx,
      BindingId elementKey,
      CodeDynamic body,
      boolean wrapEntryAsKvMap) {
      var iterSlot = ctx.nextSlot++;
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, iterSlot));
      var elementSlot = slotForBinding(ctx, elementKey);

      var loopStartNode = new LabelNode();
      var loopEndNode = new LabelNode();
      seq.add(new JavaBytecodeInstruction.Obj(loopStartNode));
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, iterSlot));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_ITERATOR, "hasNext", MDESC_HAS_NEXT));
      seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.IFEQ, loopEndNode)));
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, iterSlot));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_ITERATOR, "next", MDESC_NEXT));
      if (wrapEntryAsKvMap) {
          seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP_ENTRY)));
          var entrySlot = ctx.nextSlot++;
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, entrySlot));
          seq.add(new JavaBytecodeInstruction.Obj(new TypeInsnNode(Opcodes.NEW, JvmConstants.INTNAME_LINKED_HASH_MAP.value())));
          seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
          seq.add(JavaBytecodeUtils.callConstructor(-1, JvmConstants.INTNAME_LINKED_HASH_MAP, JavaMethodDescriptor.VOID_NOARGS));
          seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
          seq.add(JavaBytecodeUtils.literalString("key"));
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, entrySlot));
          seq.add(
                  JavaBytecodeUtils.callInterfaceMethod(
                          -1, JvmConstants.INTNAME_MAP_ENTRY, "getKey", MDESC_MAP_ENTRY_GET));
          seq.add(
                  JavaBytecodeUtils.callInterfaceMethod(
                          -1, JvmConstants.INTNAME_MAP, "put", MDESC_MAP_PUT));
          seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
          seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
          seq.add(JavaBytecodeUtils.literalString("value"));
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, entrySlot));
          seq.add(
                  JavaBytecodeUtils.callInterfaceMethod(
                          -1, JvmConstants.INTNAME_MAP_ENTRY, "getValue", MDESC_MAP_ENTRY_GET));
          seq.add(
                  JavaBytecodeUtils.callInterfaceMethod(
                          -1, JvmConstants.INTNAME_MAP, "put", MDESC_MAP_PUT));
          seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
      }
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, elementSlot));
      emitInner(seq, ctx, body);
      seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
      seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, loopStartNode)));
      seq.add(new JavaBytecodeInstruction.Obj(loopEndNode));
      seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
  }

  private void emitIf(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.If v) {
    var endNode = new LabelNode();
    var conditions = v.conditions();
    for (var branch : conditions) {
      var nextCondNode = new LabelNode();
      emitInner(seq, ctx, branch.condition());
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_BOXED_BOOL)));
      seq.add(
          JavaBytecodeUtils.callMethod(
              -1, JvmConstants.INTNAME_BOXED_BOOL, "booleanValue", MDESC_BOOL_VALUE));
      seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.IFEQ, nextCondNode)));
      emitInner(seq, ctx, branch.body());
      seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, endNode)));
      seq.add(new JavaBytecodeInstruction.Obj(nextCondNode));
    }
    if (v.elseBody() instanceof Opt.Some<CodeDynamic> some) {
      emitInner(seq, ctx, some.value());
    } else {
      seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
    }
    seq.add(new JavaBytecodeInstruction.Obj(endNode));
  }

  private void emitMatch(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.Match v) {
      emitInner(seq, ctx, v.base());
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP)));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_MAP, "entrySet", MDESC_MAP_ENTRY_SET));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_SET, "iterator", MDESC_ITERATOR));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_ITERATOR, "next", MDESC_NEXT));
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP_ENTRY)));
      var entrySlot = ctx.nextSlot++;
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, entrySlot));

      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, entrySlot));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_MAP_ENTRY, "getKey", MDESC_MAP_ENTRY_GET));
      seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_STRING)));
      var tagSlot = ctx.nextSlot++;
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, tagSlot));

      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, entrySlot));
      seq.add(
              JavaBytecodeUtils.callInterfaceMethod(
                      -1, JvmConstants.INTNAME_MAP_ENTRY, "getValue", MDESC_MAP_ENTRY_GET));
      var elementSlot = slotForBinding(ctx, v.elementKey());
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, elementSlot));

      var endNode = new LabelNode();
      for (var branch : v.cases()) {
          var nextCaseNode = new LabelNode();
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, tagSlot));
          seq.add(JavaBytecodeUtils.literalString(branch.dynamicTag()));
          seq.add(
                  JavaBytecodeUtils.callMethod(
                          -1, JvmConstants.INTNAME_STRING, "equals", MDESC_STRING_EQUALS));
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.IFEQ, nextCaseNode)));
          emitInner(seq, ctx, branch.body());
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, endNode)));
          seq.add(new JavaBytecodeInstruction.Obj(nextCaseNode));
      }
      if (v.fallback() instanceof Opt.Some<CodeDynamic> some) {
          emitInner(seq, ctx, some.value());
      } else {
          seq.add(JavaBytecodeUtils.inst(Opcodes.ACONST_NULL));
      }
      seq.add(new JavaBytecodeInstruction.Obj(endNode));
  }

  private void emitSequence(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.LoweredSequence v) {
    for (var e : v.elements()) {
      switch (e) {
        case JavaBytecode.LoweredSequence.Bytecode b -> seq.add(b.bytecode());
        case JavaBytecode.LoweredSequence.Code c -> emitInner(seq, ctx, c.code());
        case JavaBytecode.LoweredSequence.LoadExprContext lxc ->
            seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, ctx.xxSlot));
      }
    }
  }

  private void emitTry(JavaBytecodeSequence seq, EmitCtx ctx, JavaBytecode.Try v) {
      var bodyStart = new Label();
      var bodyEnd = new Label();
      var handlerStart = new Label();
      var afterBody = new Label();
      var end = new Label();

      var bodyStartNode = new LabelNode(bodyStart);
      var bodyEndNode = new LabelNode(bodyEnd);
      var handlerStartNode = new LabelNode(handlerStart);
      var afterBodyNode = new LabelNode(afterBody);
      var endNode = new LabelNode(end);

      var hasCatch = v.catchClause() instanceof Opt.Some<JavaBytecode.Catch>;
      var hasFinally = v.finallyBlock() instanceof Opt.Some<CodeDynamic>;
      var finallyHandler = hasFinally ? new Label() : null;
      var finallyHandlerNode = finallyHandler == null ? null : new LabelNode(finallyHandler);

      seq.add(new JavaBytecodeInstruction.Obj(bodyStartNode));
      emitInner(seq, ctx, v.body());
      var resultSlot = ctx.nextSlot++;
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, resultSlot));
      seq.add(new JavaBytecodeInstruction.Obj(bodyEndNode));
      seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, afterBodyNode)));

      if (hasCatch) {
          seq.add(
                  new JavaBytecodeInstruction.TryCatch(
                          bodyStart, bodyEnd, handlerStart, INTNAME_DYN_EXCEPTION.value()));
      }
      if (hasFinally) {
          seq.add(new JavaBytecodeInstruction.TryCatch(bodyStart, bodyEnd, finallyHandler, null));
      }

      if (hasCatch) {
          var catchClause = ((Opt.Some<JavaBytecode.Catch>) v.catchClause()).value();
          seq.add(new JavaBytecodeInstruction.Obj(handlerStartNode));
          seq.add(JavaBytecodeUtils.inst(Opcodes.DUP));
          seq.add(
                  JavaBytecodeUtils.accessField(
                          -1, INTNAME_DYN_EXCEPTION, "payload", JvmConstants.DESC_OBJECT));
          var bindingSlot = slotForBinding(ctx, catchClause.binding());
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, bindingSlot));
          var noPanicNode = new LabelNode();
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, bindingSlot));
          seq.add(new JavaBytecodeInstruction.Obj(
                  new org.objectweb.asm.tree.TypeInsnNode(Opcodes.INSTANCEOF, JvmConstants.INTNAME_MAP.value())));
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.IFEQ, noPanicNode)));
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, bindingSlot));
          seq.add(JavaBytecodeUtils.cast(JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP)));
          seq.add(JavaBytecodeUtils.literalString(Eval.PANIC_MARKER_KEY));
          seq.add(
                  JavaBytecodeUtils.callInterfaceMethod(
                          -1, JvmConstants.INTNAME_MAP, "containsKey", MDESC_MAP_CONTAINS_KEY));
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.IFEQ, noPanicNode)));
          seq.add(JavaBytecodeUtils.inst(Opcodes.ATHROW));
          seq.add(new JavaBytecodeInstruction.Obj(noPanicNode));
          seq.add(JavaBytecodeUtils.inst(Opcodes.POP));

          emitInner(seq, ctx, catchClause.body());
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, resultSlot));
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, afterBodyNode)));
      }

      seq.add(new JavaBytecodeInstruction.Obj(afterBodyNode));
      if (hasFinally) {
          var finallyBody = ((Opt.Some<CodeDynamic>) v.finallyBlock()).value();
          emitInner(seq, ctx, finallyBody);
          seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
          seq.add(new JavaBytecodeInstruction.Obj(new JumpInsnNode(Opcodes.GOTO, endNode)));

          seq.add(new JavaBytecodeInstruction.Obj(finallyHandlerNode));
          var excSlot = ctx.nextSlot++;
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ASTORE, excSlot));
          emitInner(seq, ctx, finallyBody);
          seq.add(JavaBytecodeUtils.inst(Opcodes.POP));
          seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, excSlot));
          seq.add(JavaBytecodeUtils.inst(Opcodes.ATHROW));
      }

      seq.add(new JavaBytecodeInstruction.Obj(endNode));
      seq.add(JavaBytecodeUtils.varInsn(Opcodes.ALOAD, resultSlot));
  }

  private int slotForBinding(EmitCtx ctx, BindingId id) {
    var existing = ctx.slots.getOrNull(id);
    if (existing != null) {
      return existing;
    }
    return ctx.allocSlot(id);
  }
}
