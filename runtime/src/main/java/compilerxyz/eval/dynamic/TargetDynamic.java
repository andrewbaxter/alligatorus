package compilerxyz.eval.dynamic;

import compilerxyz.gen.BindingSetter;

import compilerxyz.eval.JumpKey;

import compilerxyz.eval.EvalOutcome;
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
import compilerxyz.gen.BindResult;
import compilerxyz.gen.IfBranch;
import compilerxyz.gen.RecordBindField;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.TryCatchClause;
import compilerxyz.gen.TupleBindSlot;
import compilerxyz.gen.TargetCase;
import compilerxyz.gen.TargetMatchFallback;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
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
import compilerxyz.gen.Target;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenCodegenDescriptors;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.value.expr.TypeExpr;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleBaseDynamic;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.util.TOrderedMap;
import java.util.Comparator;

public final class TargetDynamic implements Target {
  private static final JavaInternalName INTNAME_DYN_EXCEPTION =
      JavaInternalName.fromClass(DynException.class);
  private static final JavaInternalName INTNAME_EXPR_CONTEXT =
      JavaInternalName.fromClass(ExprContext.class);
  private static final JavaInternalName INTNAME_OBJECT_ARRAY_ELEM =
      JavaInternalName.fromClass(Object[].class);

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
    var dropAspect = GenAspectDropDynamic.getMaybe(xx, vtype);
    if (dropAspect.optIsSome()) {
      return dropAspect.optUnwrap().aspectDropDynamicDrop(xx, loc, cv);
    }
    var lightAspect = GenAspectDropDynamicLight.getMaybe(xx, vtype);
    if (lightAspect.optIsSome()) {
      return lightAspect.optUnwrap().aspectDropDynamicLightDropDynamicLight(xx, loc, cv);
    }
    return EResult.ok(Code.none(loc));
  }

  @Override
  public CodeValue targetReturn(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      return cv;
    }
    var consumeOpt = GenAspectConsumeDynamic.getMaybe(xx, cv.value().optUnwrap().valueType());
    if (consumeOpt.optIsNone()) {
      return cv;
    }
    var _acrConsume = consumeOpt.optUnwrap();
    var _acrValueOpt = cv.value();
    if (_acrValueOpt.optIsNone() || !(_acrValueOpt.optUnwrap() instanceof ValueDynamic _acrVd)) {
      return cv;
    }
    var _acrDesc = _acrConsume.aspectConsumeDynamicJvmClass(xx).value();
    var _acrWidth = _acrDesc.equals("J") || _acrDesc.equals("D") ? 2 : 1;
    return switch (_acrVd.source()) {
      case ValueDynamicSource.Expr ignored -> cv;
      case ValueDynamicSource.Binding ignored ->
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(_acrVd.type_(), _acrWidth)));
      case ValueDynamicSource.Field ignored ->
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(_acrVd.type_(), _acrWidth)));
    };
  }

  @Override
  public BindResult targetBind(ExprContext xx, QExprId loc, TargetBindSub sub) {
    var key = new JavaBytecodeBindingKey();
    var local = new BindingId.Local(key);
    var seq = new JavaBytecodeSequence();
    seq.add(sub.data());
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeStoreLoad(
                JvmSort.storeOpcode(xx, sub.type()), key)));
    return new BindResult(new CodeDynamic(loc, seq), local);
  }

  @Override
  public EResult<Code> targetTryCatchFinally(
      ExprContext xx,
      QExprId loc,
      CodeValue body,
      Opt<TryCatchClause> catch_,
      Opt<JumpKey> catchHandler,
      Opt<Code> finallyNormal) {
    var erb = new Erb(loc);
    var seq = new JavaBytecodeSequence();
    boolean bodyHasValue = body.value().optIsSome();
    if (bodyHasValue) {
      var bodyDynOpt = cvToDynamic(erb, xx, loc, body);
      if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
      seq.add(bodyDynOpt.optUnwrap());
    } else {
      var bodyDyn = body.code().codeDynamic();
      if (bodyDyn != null) seq.add(bodyDyn);
    }
    if (catch_.optIsSome()) {
      var clause = catch_.optUnwrap();
      var handler = catchHandler.optUnwrap();
      var joinKey = new JumpKey();
      if (bodyHasValue) {
        seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(joinKey)));
      }
      // Chain nodes route every exception here (entries are catch-any); only
      // the declared user exception type enters the catch body — panics and
      // anything foreign continue unwinding after the drop chain has run.
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(handler)));
      var isExc = new LabelNode(new Label());
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(new CodeDynamic(
          new CodeLoc.Some(loc),
          new JavaBytecodeInstructionObj(
              new TypeInsnNode(
                  Opcodes.INSTANCEOF, INTNAME_DYN_EXCEPTION.value()))));
      seq.add(new CodeDynamic(
          new CodeLoc.Some(loc),
          new JavaBytecodeInstructionObj(
              new JumpInsnNode(Opcodes.IFNE, isExc))));
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionObj(isExc)));
      seq.add(JavaBytecodeUtils.cast(loc, new JavaDataDescriptor("L" + INTNAME_DYN_EXCEPTION.value() + ";")));
      seq.add(
          JavaBytecodeUtils.accessField(
              loc, INTNAME_DYN_EXCEPTION, "payload", JvmConstants.DESC_OBJECT));
      var payloadConsumeRes = GenAspectConsumeDynamic.get(xx, loc, clause.excType());
      if (payloadConsumeRes.hasErrors()) return payloadConsumeRes.reerr();
      var payloadDesc =
          payloadConsumeRes
              .requireValue("try/catch payload AspectConsumeDynamic")
              .aspectConsumeDynamicJvmClass(xx);
      if (payloadDesc.value().startsWith("L")
          && !payloadDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
        seq.add(JavaBytecodeUtils.cast(loc, payloadDesc));
      }
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, clause.binding().key())));
      if (clause.body().value().optIsSome()) {
        var catchDynOpt = cvToDynamic(erb, xx, loc, clause.body());
        if (catchDynOpt.optIsNone()) return EResult.errs(erb.errors());
        seq.add(catchDynOpt.optUnwrap());
      } else {
        // The catch body never falls through: its exits and throws carry
        // their own cleanup paths.
        var catchDyn = clause.body().code().codeDynamic();
        if (catchDyn != null) seq.add(catchDyn);
      }
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(joinKey)));
    }
    if (finallyNormal.optIsSome()) {
      var finDyn = finallyNormal.optUnwrap().codeDynamic();
      if (finDyn != null) {
        seq.add(JavaBytecodeUtils.freshen(finDyn));
      }
    }
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<Code> targetSeq(QExprId loc, TList<Code> entries) {
    var seq = new JavaBytecodeSequence();
    boolean any = false;
    for (var entry : entries) {
      var dyn = entry.codeDynamic();
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
  public EResult<Code> targetJump(QExprId loc, JumpKey dest) {
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, new JavaBytecodeJump(dest))));
  }

  @Override
  public EResult<Code> targetLand(QExprId loc, JumpKey dest) {
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, new JavaBytecodeLand(dest))));
  }

  @Override
  public EResult<Code> targetGuarded(QExprId loc, JumpKey dest, Code inner) {
    var innerDyn = inner.codeDynamic();
    if (innerDyn == null) {
      return EResult.ok(inner);
    }
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, new JavaBytecodeGuarded(dest, innerDyn))));
  }

  @Override
  public EResult<Code> targetExcCleanup(
      ExprContext xx, QExprId loc, JumpKey self, Code cleanup, Opt<JumpKey> parent) {
    var seq = new JavaBytecodeSequence();
    seq.add(new CodeDynamic(loc, new JavaBytecodeLand(self)));
    var excKey = new JavaBytecodeBindingKey();
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, excKey)));
    var cleanupDyn = cleanup.codeDynamic();
    if (cleanupDyn != null) {
      // The cleanup may be a shared template (a finally body embedded in one
      // node per state); labels are single-emission, so every embedding is a
      // fresh instance.
      seq.add(JavaBytecodeUtils.freshen(cleanupDyn));
    }
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, excKey)));
    if (parent.optIsSome()) {
      seq.add(new CodeDynamic(loc, new JavaBytecodeJump(parent.optUnwrap())));
    } else {
      seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
    }
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<Code> targetExitTrampoline(
      ExprContext xx,
      QExprId loc,
      JumpKey self,
      Opt<Type> carryType,
      Code cleanup,
      JumpKey next) {
    var seq = new JavaBytecodeSequence();
    seq.add(new CodeDynamic(loc, new JavaBytecodeLand(self)));
    var cleanupDyn = cleanup.codeDynamic();
    JavaBytecodeBindingKey parkKey = null;
    Type parkType = null;
    if (carryType.optIsSome() && cleanupDyn != null) {
      parkType = carryType.optUnwrap();
      parkKey = new JavaBytecodeBindingKey();
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(JvmSort.storeOpcode(xx, parkType), parkKey)));
    }
    if (cleanupDyn != null) {
      seq.add(cleanupDyn);
    }
    if (parkKey != null) {
      seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(JvmSort.loadOpcode(xx, parkType), parkKey)));
    }
    seq.add(new CodeDynamic(loc, new JavaBytecodeJump(next)));
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeValue> targetIf(
      ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<CodeValue> else_) {
    var erb = new Erb(loc);
    var seq = new JavaBytecodeSequence();
    var endLabel = new LabelNode();
    Type resultType = null;
    boolean allControlEnded = true;
    for (var branch : conditions) {
      var condDynOpt = cvToDynamic(erb, xx, loc, branch.condition());
      if (condDynOpt.optIsNone()) return EResult.errs(erb.errors());
      var bodyCv = branch.body();
      boolean bodyControlEnded = bodyCv.value().optIsNone();
      if (!bodyControlEnded) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = bodyCv.value().optUnwrap().valueType();
        }
      }
      CodeDynamic bodyDyn;
      if (bodyControlEnded) {
        bodyDyn = switch (bodyCv.code()) {
          case Code.Target ct when ct.impl() instanceof CodeDynamic cd -> cd;
          case Code.None n -> new CodeDynamic(loc, new JavaBytecodeSequence());
          case Code.Target ct -> new CodeDynamic(loc, new JavaBytecodeSequence());
        };
      } else {
        var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
        if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
        bodyDyn = bodyDynOpt.optUnwrap();
      }
      var nextLabel = new LabelNode();
      seq.add(condDynOpt.optUnwrap());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextLabel)));
      seq.add(bodyDyn);
      if (!bodyControlEnded) {
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      }
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextLabel));
    }
    if (else_.optIsSome()) {
      var elseCv = else_.optUnwrap();
      boolean elseControlEnded = elseCv.value().optIsNone();
      if (!elseControlEnded) allControlEnded = false;
      if (!elseControlEnded) {
        if (resultType == null) {
          resultType = elseCv.value().optUnwrap().valueType();
        }
        var elseDynOpt = cvToDynamic(erb, xx, loc, elseCv);
        if (elseDynOpt.optIsNone()) return EResult.errs(erb.errors());
        seq.add(elseDynOpt.optUnwrap());
      } else {
        switch (elseCv.code()) {
          case Code.Target ct when ct.impl() instanceof CodeDynamic ecd -> seq.add(ecd);
          case Code.None n -> {}
          case Code.Target ct -> {}
        }
      }
    } else {
      // No else: any condition may fall through — the "no branch matched"
      // path reaches endLabel with an implicit null value.
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
      allControlEnded = false;
    }
    if (allControlEnded) {
      return EResult.ok(new CodeValue(
          Code.of(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    var finalType =
        resultType == null ? (Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.optIsNone()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.optUnwrap().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeValue> targetMatch(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      TList<TargetCase> cases,
      Opt<TargetMatchFallback> fallback) {
    var erb = new Erb(loc);
    var baseDynOpt = cvToDynamic(erb, xx, loc, base);
    if (baseDynOpt.optIsNone()) return EResult.errs(erb.errors());
    var baseKey = new JavaBytecodeBindingKey();

    var seq = new JavaBytecodeSequence();
    seq.add(baseDynOpt.optUnwrap());
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, baseKey)));

    var endLabel = new LabelNode();
    Type resultType = null;
    boolean allControlEnded = true;
    var baseType = ((Opt.Some<Value>) base.value()).value().valueType();
    MatchDynamic md;
    if (baseType instanceof MatchDynamic direct) {
      md = direct;
    } else if (baseType
            instanceof TypeTypeSourceUnionConst wrapped
        && wrapped.type_() instanceof MatchDynamic wrappedMd) {
      md = wrappedMd;
    } else {
      return EResult.err(loc, "Match: base type is not matchable: " + baseType.typeDesc());
    }
    var prepRes = md.matchDynamicPrepare(xx, loc);
    if (prepRes.hasErrors()) return prepRes.reerr();
    for (var branch : cases) {
      var bodyCv = branch.body();
      boolean bodyControlEnded = bodyCv.value().optIsNone();
      if (!bodyControlEnded) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = bodyCv.value().optUnwrap().valueType();
        }
      }
      CodeDynamic bodyDyn;
      if (bodyControlEnded) {
        bodyDyn = switch (bodyCv.code()) {
          case Code.Target ct when ct.impl() instanceof CodeDynamic cd -> cd;
          case Code.None n -> new CodeDynamic(loc, new JavaBytecodeSequence());
          case Code.Target ct -> new CodeDynamic(loc, new JavaBytecodeSequence());
        };
      } else {
        var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
        if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
        bodyDyn = bodyDynOpt.optUnwrap();
      }
      var nextCaseLabel = new LabelNode();
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
      var testRes = md.matchDynamicTest(xx, loc, branch.variantIndex());
      if (testRes.hasErrors()) return testRes.reerr();
      seq.add(testRes.requireValue("targetMatch variant test"));
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextCaseLabel)));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
      var extractRes = md.matchDynamicExtract(xx, loc, branch.variantIndex(), branch.variantType());
      if (extractRes.hasErrors()) return extractRes.reerr();
      seq.add(extractRes.requireValue("targetMatch variant extract"));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, branch.payloadBinding().key())));
      seq.add(bodyDyn);
      if (!bodyControlEnded) {
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      }
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextCaseLabel));
    }
    if (fallback.optIsSome()) {
      var fallbackCv = fallback.optUnwrap().body();
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
      seq.add(
          new CodeDynamic(
              new CodeLoc.Some(loc),
              new JavaBytecodeStoreLoad(
                  Opcodes.ASTORE, fallback.optUnwrap().payloadBinding().key())));
      boolean fallbackControlEnded = fallbackCv.value().optIsNone();
      if (!fallbackControlEnded) allControlEnded = false;
      if (!fallbackControlEnded) {
        if (resultType == null) {
          resultType = fallbackCv.value().optUnwrap().valueType();
        }
        var fallbackDynOpt = cvToDynamic(erb, xx, loc, fallbackCv);
        if (fallbackDynOpt.optIsNone()) return EResult.errs(erb.errors());
        seq.add(fallbackDynOpt.optUnwrap());
      } else {
        switch (fallbackCv.code()) {
          case Code.Target ct when ct.impl() instanceof CodeDynamic fcd -> seq.add(fcd);
          case Code.None n -> {}
          case Code.Target ct -> {}
        }
      }
    } else if (allControlEnded) {
      // Every case body is control-ended; no fallback is needed since the
      // match is exhaustive. Emit an unreachable ATHROW so verifier does not
      // require a fall-through value here.
      seq.add(JavaBytecodeUtils.instantiate(loc,
          JavaInternalName.of("java/lang/AssertionError"),
          JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID),
          new CodeDynamic(loc, new JavaBytecodeSequence())));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    if (allControlEnded) {
      return EResult.ok(new CodeValue(
          Code.of(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    var finalType =
        resultType == null ? (Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.optIsNone()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.optUnwrap().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  private static Opt<CodeDynamic> cvToDynamic(
      Erb erb, ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      erb.errPush("TargetDynamic::if_: CodeValue has no value");
      return Opt.none();
    }
    var vtype = cv.value().optUnwrap().valueType();
    var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
    if (consumeOpt.optIsNone()) return Opt.none();
    return erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
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
    var treeKey = new JavaBytecodeBindingKey();
    var treeId = new BindingId.Local(treeKey);
    var treeExprType = new TypeExpr();
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(
            treeKey, treeExprType, new External(StagedExprCell.class, treeCell)));
    var restElements = new TList<CodeDynamic>();
    for (var cv : lowerValues) {
      if (cv.code().codeIsNone()) {
        if (cv.value().optIsNone()) {
          return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
        }
        var cell = new StagedExprCell(cv.value().optUnwrap());
        var key = new JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                key,
                new TypeExpr(),
                new External(StagedExprCell.class, cell)));
        restElements.add(JavaBytecodeUtils.literalNull(loc));
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
      } else {
        if (cv.value().optIsNone()) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic lower CodeValue has no value");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var liftCell =
            new StageLiftCell(
                loc, xx.vx, xx.importState, xx.scope, vtype, xx.currentExceptionType, this);
        var liftKey = new JavaBytecodeBindingKey();
        var liftId = new BindingId.Local(liftKey);
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                liftKey, vtype, new External(StageLiftCell.class, liftCell)));
        var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
        if (innerJsOpt.optIsNone()) return EResult.errs(erb.errors());
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, liftId));
        restElements.add(innerJsOpt.optUnwrap());
      }
    }
    for (var cv : graftValues) {
      if (cv.code().codeIsNone()) {
        var graftCv = new CodeValue(Code.none(loc), cv.outcome());
        var key = new JavaBytecodeBindingKey();
        var id = new BindingId.Local(key);
        var graftType = cv.value().optIsSome() ? cv.value().optUnwrap().valueType() : new TypeNullDynamic();
        xx.vx.externals.add(
            new EvalContext.ExternalEntry(
                key, graftType, new External(CodeValue.class, graftCv)));
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
      } else {
        if (cv.value().optIsNone()) {
          return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
        if (innerJsOpt.optIsNone()) return EResult.errs(erb.errors());
        restElements.add(innerJsOpt.optUnwrap());
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
    stageSeq.add(JavaBytecodeUtils.callStaticMethod(loc, GenCodegenDescriptors.DYN_RUNTIME_STAGE));
    var stageCall = new CodeDynamic(loc, stageSeq);
    var resultValue =
        ValueDynamic.stack(new TypeExprStagedPlaceholder(), 1);
    return EResult.ok(new CodeValue(Code.of(loc, stageCall), EvalOutcome.ofValue(resultValue)));
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

  @Override
  public EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      throw new IllegalStateException(
          "Target::maybeBindSub: CodeValue has no value; Rust unwraps unconditionally");
    }
    var vtype = cv.value().optUnwrap().valueType();
    var bindDynOpt = GenAspectBindDynamic.getMaybe(xx, vtype);
    if (bindDynOpt.optIsNone()) {
      return EResult.ok(Opt.none());
    }
    return bindDynOpt.optUnwrap().aspectBindDynamicBindDynamic(xx, loc, cv);
  }

  @Override
  public EResult<TargetBindSub> targetCodeToBindSub(
      QExprId loc, Code code, Type type_) {
    var dyn = code.codeDynamic();
    if (dyn == null) {
      return EResult.err(loc, "Target::codeToBindSub: code is not dynamic");
    }
    return EResult.ok(new TargetBindSub(dyn, type_));
  }

  @Override
  public TargetBindSub targetBindSubRecord(
      ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields) {
    var entries = new TOrderedMap<ConstKey, TargetBindSub>();
    for (var f : fields) entries.put(f.key(), f.sub());
    var sortedSubs = sortBindSubs(xx, entries, ConstKey.CANONICAL_ORDER);
    var dynamicFields =
        new TOrderedMap<ConstKey, RecordField>();
    for (var f : fields) {
      dynamicFields.put(
          f.key(), new RecordField.Dynamic(f.sub().type()));
    }
    Type recordType =
        new Record(Opt.none(), dynamicFields);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, recordType);
  }

  @Override
  public TargetBindSub targetBindSubTuple(
      ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots) {
    var entries = new TOrderedMap<Integer, TargetBindSub>();
    for (int i = 0; i < slots.size(); i++) entries.put(i, slots.get(i).sub());
    var sortedSubs = sortBindSubs(xx, entries, Integer::compare);
    var dynamicElements =
        new TList<TupleElement>(slots.size());
    for (var slot : slots) {
      dynamicElements.add(
          new TupleElement.Dynamic(slot.sub().type()));
    }
    Type tupleType =
        new Tuple(
            Opt.<TupleBaseDynamic>none(), dynamicElements);
    var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
    return new TargetBindSub(dynamicCode, tupleType);
  }

  @Override
  public EResult<Opt<Code>> targetCastException(
      ExprContext xx,
      QExprId loc,
      JumpKey self,
      Type calleeExc,
      Type callerExc,
      Opt<JumpKey> next) {
    var erb = new Erb(loc);
    // Materialize the in-flight exception through a fresh local — pure
    // target machinery: the handler frame's single stack element lands in a
    // slot and reads back as a value of the callee's exception type.
    var catchBinding = BindingId.localFresh();
    var initialBindOpt = erb.mustEr(GenAspectInitialBind.get(xx, loc, calleeExc));
    if (initialBindOpt.optIsNone()) return EResult.errs(erb.errors());
    var binding = initialBindOpt.optUnwrap().aspectInitialBindInitialBind(catchBinding);
    var boundCv =
        binding.bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc));
    var castCvOpt = erb.mustEr(calleeExc.typeCastTo(xx, loc, boundCv, callerExc));
    if (castCvOpt.optIsNone()) return EResult.errs(erb.errors());
    var castCv = castCvOpt.optUnwrap();
    if (castCv.code().codeIsNone()) {
      return EResult.ok(Opt.<Code>none());
    }
    var seq = new JavaBytecodeSequence();
    seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(self)));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeStoreLoad(Opcodes.ASTORE, catchBinding.key())));
    var castDynOpt = cvToDynamic(erb, xx, loc, castCv);
    if (castDynOpt.optIsNone()) return EResult.errs(erb.errors());
    seq.add(castDynOpt.optUnwrap());
    if (next.optIsSome()) {
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(next.optUnwrap())));
    } else {
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
    }
    return EResult.ok(Opt.some(Code.of(loc, new CodeDynamic(loc, seq))));
  }

  @Override
  public EResult<Code> targetFresh(QExprId loc, Code code) {
    var dyn = code.codeDynamic();
    if (dyn == null) return EResult.ok(code);
    return EResult.ok(Code.of(loc, JavaBytecodeUtils.freshen(dyn)));
  }

  private static <K> TList<TargetBindSub> sortBindSubs(
      ExprContext xx,
      TOrderedMap<K, TargetBindSub> entries,
      Comparator<K> tiebreaker) {
    var typeEntries = new TOrderedMap<K, Type>();
    for (var e : entries.entrySet()) typeEntries.put(e.getKey(), e.getValue().type());
    var sort = CompositeSlotSort.sort(xx, typeEntries, tiebreaker);
    var out = new TList<TargetBindSub>(entries.size());
    for (var e : entries.entrySet()) out.add(null);
    for (var e : entries.entrySet()) {
      out.set(sort.slotOfKey().get(e.getKey()), e.getValue());
    }
    return out;
  }

  private static CodeDynamic emitCompositeInstantiation(
      ExprContext xx, QExprId loc, Code carry, TList<TargetBindSub> sortedSubs) {
    var sortedTypes = new TList<Type>(sortedSubs.size());
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
