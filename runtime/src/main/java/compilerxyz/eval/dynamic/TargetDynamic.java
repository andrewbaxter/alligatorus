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
import compilerxyz.gen.Value;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.IfBranch;
import compilerxyz.eval.dynamic.target.RecordBindField;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TryCatchClause;
import compilerxyz.eval.dynamic.target.TupleBindSlot;
import compilerxyz.eval.dynamic.target.TargetCase;
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

public final class TargetDynamic implements compilerxyz.gen.Target {
  private static final JavaInternalName INTNAME_DYN_EXCEPTION =
      JavaInternalName.fromClass(DynException.class);
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
  private static final JavaMethodDescriptor MDESC_STRING_EQUALS =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL, JvmConstants.DESC_OBJECT);

  private final Counter localIds;

  public TargetDynamic(Counter localIds) {
    this.localIds = localIds;
  }

  @Override
  public EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      return EResult.ok(Code.none(loc));
    }
    var value = cv.value().optUnwrap();
    var vtype = value.valueType();
    var dropAspect = AspectDropDynamic.getMaybe(xx, vtype);
    if (dropAspect.optIsSome()) {
      return dropAspect.optUnwrap().aspectDropDynamicDrop(xx, loc, cv);
    }
    var lightAspect = AspectDropDynamicLight.getMaybe(xx, vtype);
    if (lightAspect.optIsSome()) {
      return lightAspect.optUnwrap().aspectDropDynamicLightDropDynamicLight(xx, loc, cv);
    }
    return EResult.ok(Code.none(loc));
  }

  @Override
  public BindResult targetBind(QExprId loc, TargetBindSub sub) {
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var local = new BindingId.Local(key);
    var seq = new JavaBytecodeSequence();
    seq.add(sub.data());
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeStoreLoad(
                JvmSort.storeOpcode(sub.type()), key)));
    return new BindResult(new CodeDynamic(loc, seq), local);
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
    // Stack-neutral: body and finally do not produce values here (used by the
    // scoped-drop wrap path). Any exception thrown by body runs finally too and
    // is re-thrown.
    var bodyStart = new Label();
    var bodyEnd = new Label();
    var handler = new Label();
    var end = new Label();
    var seq = new JavaBytecodeSequence();
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeInstructionTryCatch(bodyStart, bodyEnd, handler, null)));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyStart)));
    seq.add(bodyDyn);
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyEnd)));
    seq.add(finallyDyn);
    seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(end))));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(handler)));
    var excKey = new JavaBytecodeBindingKey();
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, excKey)));
    seq.add(finallyDyn);
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, excKey)));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(end)));
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeDynamic> targetTryCatchFinally(
      QExprId loc,
      CodeDynamic body,
      Opt<TryCatchClause> catch_,
      Opt<CodeDynamic> finally_) {
    var bodyStart = new Label();
    var bodyEnd = new Label();
    var handler = new Label();
    var afterBody = new Label();
    var end = new Label();
    var finallyHandler = finally_ instanceof Opt.Some<?> ? new Label() : null;

    var resultKey = new JavaBytecodeBindingKey();
    var seq = new JavaBytecodeSequence();

    // Reserve the JVM slot for resultKey (and for catch payload / finally exc if
    // present) up front, before any jumps. The frame-index tracker requires all
    // control-flow paths that reach a label to agree on the variable set —
    // reserving early avoids the case where the try body's GOTO to afterBody has
    // fewer slots than the handler's GOTO to the same label.
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, resultKey)));
    JavaBytecodeBindingKey finallyExcKey = null;
    if (finallyHandler != null) {
      finallyExcKey = new JavaBytecodeBindingKey();
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, finallyExcKey)));
    }
    if (catch_ instanceof Opt.Some<TryCatchClause> someCatchInit) {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(
                  Opcodes.ASTORE, someCatchInit.value().binding().key())));
    }

    // Emit try-catch table entries BEFORE the body. The frame-index tracker
    // snapshots at try-catch declaration time to be able to restore the frame
    // at the handler label — if these come after the body's terminating GOTO,
    // currentIndexes is null and the snapshot is missed.
    if (catch_ instanceof Opt.Some<TryCatchClause> someCatch) {
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeInstructionTryCatch(
                  bodyStart, bodyEnd, handler, INTNAME_DYN_EXCEPTION.value())));
    }
    if (finallyHandler != null) {
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeInstructionTryCatch(bodyStart, bodyEnd, finallyHandler, null)));
    }

    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyStart)));
    seq.add(body);
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, resultKey)));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyEnd)));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(afterBody))));

    if (catch_ instanceof Opt.Some<TryCatchClause> someCatch2) {
      var clause = someCatch2.value();
      seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(handler)));
      // Stack: DynException (JVM auto-pushed on catch). Panics use DynPanic,
      // which is not a DynException, so the JVM catch table excludes them and
      // they propagate past this handler untouched.
      seq.add(
          JavaBytecodeUtils.accessField(
              loc, INTNAME_DYN_EXCEPTION, "payload", JvmConstants.DESC_OBJECT));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, clause.binding().key())));
      seq.add(clause.body());
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, resultKey)));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(afterBody))));
    }

    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(afterBody)));
    if (finally_ instanceof Opt.Some<CodeDynamic> someFinally) {
      // Caller passes finally_ already drop-wrapped (stack-neutral) — the drop
      // aspect emits its own POP.
      seq.add(someFinally.value());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(end))));

      seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(finallyHandler)));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, finallyExcKey)));
      seq.add(someFinally.value());
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, finallyExcKey)));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    }

    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(end)));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, resultKey)));
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  @Override
  public EResult<Code> targetSeq(QExprId loc, TList<Code> entries) {
    var seq = new JavaBytecodeSequence();
    boolean any = false;
    for (var entry : entries) {
      var dyn = entry.dynamic();
      if (dyn == null) continue;
      seq.add(dyn);
      any = true;
    }
    if (!any) {
      return EResult.ok(Code.none(loc));
    }
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeValue> targetIf(
      ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_) {
    var erb = new Erb(loc);
    var seq = new JavaBytecodeSequence();
    var endLabel = new LabelNode();
    var boolDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_BOXED_BOOL);
    compilerxyz.eval.Type resultType = null;
    for (var branch : conditions) {
      var condDynOpt = cvToDynamic(erb, xx, loc, branch.condition());
      if (condDynOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCvOpt = erb.mustEr(ConstEval.constEval(xx, branch.body()));
      if (bodyCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = bodyCvOpt.get();
      if (resultType == null) {
        if (!(bodyCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::if_: body CodeValue has no value");
        }
        resultType = some.value().valueType();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, bodyCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyCv = convertedOpt.get();
      }
      var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
      if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
      var nextLabel = new LabelNode();
      seq.add(condDynOpt.get());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextLabel)));
      seq.add(bodyDynOpt.get());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextLabel));
    }
    if (else_ instanceof Opt.Some<Expr> someElse) {
      var elseCvOpt = erb.mustEr(ConstEval.constEval(xx, someElse.value()));
      if (elseCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var elseCv = elseCvOpt.get();
      if (resultType == null) {
        if (!(elseCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::if_: else CodeValue has no value");
        }
        resultType = some.value().valueType();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, elseCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        elseCv = convertedOpt.get();
      }
      var elseDynOpt = cvToDynamic(erb, xx, loc, elseCv);
      if (elseDynOpt.isEmpty()) return EResult.errs(erb.errors());
      seq.add(elseDynOpt.get());
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(AspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().aspectStackValueDynamicToValue(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeValue> targetMatch(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      ConstKey elementKey,
      TList<TargetCase> cases,
      Opt<Expr> fallback) {
    var erb = new Erb(loc);
    var baseDynOpt = cvToDynamic(erb, xx, loc, base);
    if (baseDynOpt.isEmpty()) return EResult.errs(erb.errors());
    var baseKey = new JavaBytecodeBindingKey();

    var seq = new JavaBytecodeSequence();
    seq.add(baseDynOpt.get());
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, baseKey)));

    var endLabel = new LabelNode();
    compilerxyz.eval.Type resultType = null;
    for (var branch : cases) {
      var variantInternal =
          JavaInternalName.of(branch.variantBinaryName().replace('.', '/'));
      var payloadDesc =
          compilerxyz.eval.dynamic.SourceUnionEmitter.payloadDescriptor(branch.variantType());
      var payloadBinding = new BindingId.Local(new JavaBytecodeBindingKey());
      var caseScope = new compilerxyz.eval.Scope(xx.scope);
      var ibMaybe = AspectInitialBind.getMaybe(xx, branch.variantType());
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        caseScope.values.put(elementKey, ibSome.value().aspectInitialBindInitialBind(payloadBinding));
      }
      var caseXx = xx.child(caseScope);
      var bodyCvOpt = erb.mustEr(ConstEval.constEval(caseXx, branch.body()));
      if (bodyCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = bodyCvOpt.get();
      if (resultType == null) {
        if (!(bodyCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::match_: case CodeValue has no value");
        }
        resultType = some.value().valueType();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(caseXx, loc, resultType, bodyCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyCv = convertedOpt.get();
      }
      var bodyDynOpt = cvToDynamic(erb, caseXx, loc, bodyCv);
      if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
      var nextCaseLabel = new LabelNode();
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
      seq.add(JavaBytecodeUtils.instanceOf(loc, variantInternal));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextCaseLabel)));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
      seq.add(JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(variantInternal)));
      seq.add(
          JavaBytecodeUtils.accessField(
              loc,
              variantInternal,
              compilerxyz.eval.dynamic.SourceUnionEmitter.PAYLOAD_FIELD,
              payloadDesc));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, payloadBinding.key())));
      seq.add(bodyDynOpt.get());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextCaseLabel));
    }
    if (fallback instanceof Opt.Some<Expr> someFallback) {
      var fallbackCvOpt = erb.mustEr(ConstEval.constEval(xx, someFallback.value()));
      if (fallbackCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var fallbackCv = fallbackCvOpt.get();
      if (resultType == null) {
        if (!(fallbackCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::match_: fallback CodeValue has no value");
        }
        resultType = some.value().valueType();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, fallbackCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        fallbackCv = convertedOpt.get();
      }
      var fallbackDynOpt = cvToDynamic(erb, xx, loc, fallbackCv);
      if (fallbackDynOpt.isEmpty()) return EResult.errs(erb.errors());
      seq.add(fallbackDynOpt.get());
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(AspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().aspectStackValueDynamicToValue(loc, new CodeDynamic(loc, seq)));
  }

  private static Optional<CodeDynamic> cvToDynamic(
      Erb erb, ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      erb.errPush("TargetDynamic::if_: CodeValue has no value");
      return Optional.empty();
    }
    var vtype = some.value().valueType();
    var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
    if (consumeOpt.isEmpty()) return Optional.empty();
    return erb.mustEr(consumeOpt.get().aspectConsumeDynamicToDynamic(xx, loc, cv));
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
    var treeKey = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var treeId = new BindingId.Local(treeKey);
    var treeExprType = new compilerxyz.eval.value.expr.TypeExpr();
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(
            treeKey, treeExprType, new External(StagedExprCell.class, treeCell)));
    var restElements = new TList<CodeDynamic>();
    for (var cv : lowerValues) {
      if (cv.code().isNone()) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
        }
        var cell = new StagedExprCell(some.value());
        var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                key,
                new compilerxyz.eval.value.expr.TypeExpr(),
                new External(StagedExprCell.class, cell)));
        restElements.add(JavaBytecodeUtils.literalNull(loc));
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
      } else {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic lower CodeValue has no value");
        }
        var vtype = some.value().valueType();
        var liftCell =
            new StageLiftCell(
                loc, xx.vx, xx.importState, xx.scope, vtype, xx.currentExceptionType, this);
        var liftKey = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
        var liftId = new BindingId.Local(liftKey);
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                liftKey, vtype, new External(StageLiftCell.class, liftCell)));
        var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().aspectConsumeDynamicToDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, liftId));
        restElements.add(innerJsOpt.get());
      }
    }
    for (var cv : graftValues) {
      if (cv.code().isNone()) {
        var graftCv = new CodeValue(Code.none(loc), cv.value());
        var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        var graftType =
            cv.value() instanceof Opt.Some<Value> vSome
                ? vSome.value().valueType()
                : new TypeNullDynamic();
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                key, graftType, new External(CodeValue.class, graftCv)));
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
      } else {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
        }
        var vtype = some.value().valueType();
        var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().aspectConsumeDynamicToDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        restElements.add(innerJsOpt.get());
      }
    }
    var stageSeq = new JavaBytecodeSequence();
    stageSeq.add(
        JavaBytecodeUtils.accessStaticField(
            loc,
            JavaInternalName.of(xx.vx.evalRootInternalName),
            EvalRootEmitter.EXPR_CONTEXT_FIELD,
            EvalRootEmitter.DESC_EXPR_CONTEXT));
    stageSeq.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, treeId));
    stageSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, lowerValues.size()));
    stageSeq.add(JavaBytecodeUtils.buildObjectArray(loc, restElements));
    stageSeq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            JavaInternalName.fromClass(DynRuntime.class),
            "stage",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(StagedExprCell.class),
                TList.of(
                    JavaDataDescriptor.fromClass(ExprContext.class),
                    JavaDataDescriptor.fromClass(StagedExprCell.class),
                    JavaDataDescriptor.DESC_INT,
                    JavaDataDescriptor.fromClass(Object[].class)))));
    var stageCall = new CodeDynamic(loc, stageSeq);
    var resultValue =
        ValueDynamic.stack(new TypeExprStagedPlaceholder());
    return EResult.ok(new CodeValue(Code.of(loc, stageCall), Opt.some(resultValue)));
  }

  public void emit(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
    emitInner(seq, ctx, code);
  }

  private void emitInner(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
    switch (code.code()) {
      case JavaBytecodeSequence v -> {
        for (var child : v.children) emitInner(seq, ctx, child);
      }
      case CodeDynamic nested -> emitInner(seq, ctx, nested);
      case JavaBytecode jbc -> seq.add(new CodeDynamic(code.loc(), jbc));
    }
  }

  private int slotForBinding(EmitCtx ctx, BindingId id) {
    var existing = ctx.slots.getOrNull(id);
    if (existing != null) {
      return existing;
    }
    return ctx.allocSlot(id);
  }

  @Override
  public EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException(
          "Target::maybeBindSub: CodeValue has no value; Rust unwraps unconditionally");
    }
    var vtype = some.value().valueType();
    var bindDynOpt = compilerxyz.gen.AspectBindDynamic.getMaybe(xx, vtype);
    if (!(bindDynOpt instanceof Opt.Some<compilerxyz.gen.AspectBindDynamic> bindDynSome)) {
      return EResult.ok(Opt.none());
    }
    return bindDynSome.value().aspectBindDynamicBindDynamic(xx, loc, cv);
  }

  @Override
  public EResult<TargetBindSub> targetCodeToBindSub(
      QExprId loc, Code code, compilerxyz.eval.Type type_) {
    var dyn = code.dynamic();
    if (dyn == null) {
      return EResult.err(loc, "Target::codeToBindSub: code is not dynamic");
    }
    return EResult.ok(new TargetBindSub(dyn, type_));
  }

  @Override
  public TargetBindSub targetBindSubRecord(
      ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields) {
    var entries = new compilerxyz.util.TOrderedMap<ConstKey, TargetBindSub>();
    for (var f : fields) entries.put(f.key(), f.sub());
    var sortedSubs = sortBindSubs(entries, ConstKey.CANONICAL_ORDER);
    var dynamicFields =
        new compilerxyz.util.TOrderedMap<ConstKey, compilerxyz.eval.value.record.RecordField>();
    for (var f : fields) {
      dynamicFields.put(
          f.key(), new compilerxyz.eval.value.record.RecordField.Dynamic(f.sub().type()));
    }
    compilerxyz.eval.Type recordType =
        new compilerxyz.eval.value.record.Record(Opt.none(), dynamicFields);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, recordType);
  }

  @Override
  public TargetBindSub targetBindSubTuple(
      ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots) {
    var entries = new compilerxyz.util.TOrderedMap<Integer, TargetBindSub>();
    for (int i = 0; i < slots.size(); i++) entries.put(i, slots.get(i).sub());
    var sortedSubs = sortBindSubs(entries, Integer::compare);
    var dynamicElements =
        new TList<compilerxyz.eval.value.tuple.TupleElement>(slots.size());
    for (var slot : slots) {
      dynamicElements.add(
          new compilerxyz.eval.value.tuple.TupleElement.Dynamic(slot.sub().type()));
    }
    compilerxyz.eval.Type tupleType =
        new compilerxyz.eval.value.tuple.Tuple(
            Opt.<compilerxyz.eval.value.tuple.TupleBaseDynamic>none(), dynamicElements);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, tupleType);
  }

  @Override
  public EResult<Code> targetCastException(
      ExprContext xx,
      QExprId loc,
      Code callCode,
      BindingId catchBinding,
      CodeValue castCv) {
    throw new UnsupportedOperationException(
        "TargetDynamic::targetCastException: not implemented");
  }

  private static <K> TList<TargetBindSub> sortBindSubs(
      compilerxyz.util.TOrderedMap<K, TargetBindSub> entries,
      java.util.Comparator<K> tiebreaker) {
    var typeEntries = new compilerxyz.util.TOrderedMap<K, compilerxyz.eval.Type>();
    for (var e : entries.entrySet()) typeEntries.put(e.getKey(), e.getValue().type());
    var sort = CompositeSlotSort.sort(typeEntries, tiebreaker);
    var out = new TList<TargetBindSub>(entries.size());
    for (var e : entries.entrySet()) out.add(null);
    for (var e : entries.entrySet()) {
      out.set(sort.slotOfKey().get(e.getKey()), e.getValue());
    }
    return out;
  }

  private static CodeDynamic emitCompositeInstantiation(
      ExprContext xx, QExprId loc, Code carry, TList<TargetBindSub> sortedSubs) {
    var sortedTypes = new TList<compilerxyz.eval.Type>(sortedSubs.size());
    var argSeq = new JavaBytecodeSequence();
    for (var sub : sortedSubs) {
      sortedTypes.add(sub.type());
      argSeq.add(sub.data());
    }
    var klass =
        JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
    var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
    return DynUtil.wrapWithCarry(
        loc,
        carry,
        JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq)));
  }
}
