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
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var local = new BindingId.Local(key);
    var seq = new JavaBytecodeSequence();
    seq.add(sub.data());
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, key)));
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
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyStart)));
    seq.add(bodyDyn);
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyEnd)));
    seq.add(finallyDyn);
    seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(end))));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeInstructionTryCatch(bodyStart, bodyEnd, handler, null)));
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

    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyStart)));
    seq.add(body);
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, resultKey)));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(bodyEnd)));
    seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(afterBody))));

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
      seq.add(someFinally.value());
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, new LabelNode(end))));

      seq.add(JavaBytecodeUtils.wrapObj(loc, new LabelNode(finallyHandler)));
      var excKey = new JavaBytecodeBindingKey();
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, excKey)));
      seq.add(someFinally.value());
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, excKey)));
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
  public EResult<CodeValue> targetIf_(
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
        resultType = some.value().valueTypeOf();
      } else {
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, resultType, bodyCv));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyCv = convertedOpt.get();
      }
      var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
      if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
      var nextLabel = new LabelNode();
      seq.add(condDynOpt.get());
      seq.add(JavaBytecodeUtils.cast(loc, boolDesc));
      seq.add(
          JavaBytecodeUtils.callMethod(
              loc, JvmConstants.INTNAME_BOXED_BOOL, "booleanValue", MDESC_BOOL_VALUE));
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
        resultType = some.value().valueTypeOf();
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
    return EResult.ok(stackOpt.get().toValue(loc, new CodeDynamic(loc, seq)));
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
    var baseDynOpt = cvToDynamic(erb, xx, loc, base);
    if (baseDynOpt.isEmpty()) return EResult.errs(erb.errors());
    var stringDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_STRING);
    var mapDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP);
    var mapEntryDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_MAP_ENTRY);
    var payloadBinding = new BindingId.Local(new JavaBytecodeBindingKey());
    var tagKey = new JavaBytecodeBindingKey();

    var seq = new JavaBytecodeSequence();
    // Base is a single-tag payload — a Map with exactly one entry {tag: payload}.
    // Extract the sole Map.Entry inline: entrySet().iterator().next().
    seq.add(baseDynOpt.get());
    seq.add(JavaBytecodeUtils.cast(loc, mapDesc));
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc, JvmConstants.INTNAME_MAP, "entrySet", MDESC_MAP_ENTRY_SET));
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc, JvmConstants.INTNAME_SET, "iterator", MDESC_ITERATOR));
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc, JvmConstants.INTNAME_ITERATOR, "next", MDESC_NEXT));
    seq.add(JavaBytecodeUtils.cast(loc, mapEntryDesc));
    // Duplicate the entry so we can read both key and value.
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc, JvmConstants.INTNAME_MAP_ENTRY, "getKey", MDESC_MAP_ENTRY_GET));
    seq.add(JavaBytecodeUtils.cast(loc, stringDesc));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, tagKey)));
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc, JvmConstants.INTNAME_MAP_ENTRY, "getValue", MDESC_MAP_ENTRY_GET));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeStoreLoad(Opcodes.ASTORE, payloadBinding.key())));

    var endLabel = new LabelNode();
    compilerxyz.eval.Type resultType = null;
    for (var branch : cases) {
      var caseScope = new compilerxyz.eval.Scope(xx.scope);
      var ibMaybe = AspectInitialBind.getMaybe(xx, branch.variantType());
      if (ibMaybe instanceof Opt.Some<AspectInitialBind> ibSome) {
        caseScope.values.put(elementKey, ibSome.value().initialBind(payloadBinding));
      }
      var caseXx = xx.child(caseScope);
      var bodyCvOpt = erb.mustEr(ConstEval.constEval(caseXx, branch.body()));
      if (bodyCvOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = bodyCvOpt.get();
      if (resultType == null) {
        if (!(bodyCv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::match_: case CodeValue has no value");
        }
        resultType = some.value().valueTypeOf();
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
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, tagKey)));
      seq.add(JavaBytecodeUtils.literalString(loc, branch.dynamicTag()));
      seq.add(
          JavaBytecodeUtils.callMethod(
              loc, JvmConstants.INTNAME_STRING, "equals", MDESC_STRING_EQUALS));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextCaseLabel)));
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
        resultType = some.value().valueTypeOf();
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
    return EResult.ok(stackOpt.get().toValue(loc, new CodeDynamic(loc, seq)));
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
    var restElements = new TList<CodeDynamic>();
    for (var cv : lowerValues) {
      if (cv.code().isNone()) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
        }
        var cell = new StagedExprCell(some.value());
        var id = new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(id, new External.StagedExprCellExternal(cell)));
        restElements.add(JavaBytecodeUtils.literalNull(loc));
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
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
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, liftId));
        restElements.add(innerJsOpt.get());
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
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
      } else {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
        }
        var vtype = some.value().valueTypeOf();
        var consumeOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().toDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        restElements.add(innerJsOpt.get());
      }
    }
    var stageArgs = new TList<CodeDynamic>();
    stageArgs.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, treeId));
    stageArgs.add(JavaBytecodeUtils.literalIntShortByte(loc, lowerValues.size()));
    stageArgs.add(JavaBytecodeUtils.buildObjectArray(loc, restElements));
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
}
