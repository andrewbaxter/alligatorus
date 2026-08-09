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
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;

public final class TargetDynamic implements compilerxyz.gen.Target {
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
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return cv;
    }
    var consumeOpt = GenAspectConsumeDynamic.getMaybe(xx, some.value().valueType());
    if (!(consumeOpt instanceof Opt.Some<AspectConsumeDynamic> consumeSome)) {
      return cv;
    }
    return consumeSome.value().aspectConsumeDynamicReturn(xx, loc, cv);
  }

  @Override
  public BindResult targetBind(ExprContext xx, QExprId loc, TargetBindSub sub) {
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
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

  /** Discards a value of the given descriptor from the operand stack. */
  private static CodeDynamic popValue(QExprId loc, JavaDataDescriptor desc) {
    return switch (desc.value().charAt(0)) {
      case 'J', 'D' -> JavaBytecodeUtils.inst(loc, Opcodes.POP2);
      default -> JavaBytecodeUtils.inst(loc, Opcodes.POP);
    };
  }

  /** Zero/null of the given descriptor, used to definitely-assign a join local on every path. */
  private static CodeDynamic defaultValue(QExprId loc, JavaDataDescriptor desc) {
    return switch (desc.value().charAt(0)) {
      case 'J' -> JavaBytecodeUtils.inst(loc, Opcodes.LCONST_0);
      case 'D' -> JavaBytecodeUtils.inst(loc, Opcodes.DCONST_0);
      case 'F' -> JavaBytecodeUtils.inst(loc, Opcodes.FCONST_0);
      case 'I', 'S', 'B', 'Z', 'C' -> JavaBytecodeUtils.inst(loc, Opcodes.ICONST_0);
      default -> JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL);
    };
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
    boolean bodyHasValue = body.value() instanceof Opt.Some<Value>;
    if (bodyHasValue) {
      var bodyDynOpt = cvToDynamic(erb, xx, loc, body);
      if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
      seq.add(bodyDynOpt.get());
    } else {
      var bodyDyn = body.code().dynamic();
      if (bodyDyn != null) seq.add(bodyDyn);
    }
    if (catch_ instanceof Opt.Some<TryCatchClause> someCatch) {
      var clause = someCatch.value();
      var handler = ((Opt.Some<JumpKey>) catchHandler).value();
      var joinKey = new JumpKey();
      if (bodyHasValue) {
        seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(joinKey)));
      }
      // Chain nodes route every exception here (entries are catch-any); only
      // the declared user exception type enters the catch body — panics and
      // anything foreign continue unwinding after the drop chain has run.
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(handler)));
      var isExc = new org.objectweb.asm.tree.LabelNode(new org.objectweb.asm.Label());
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(new CodeDynamic(
          new CodeLoc.Some(loc),
          new JavaBytecodeInstructionObj(
              new org.objectweb.asm.tree.TypeInsnNode(
                  Opcodes.INSTANCEOF, INTNAME_DYN_EXCEPTION.value()))));
      seq.add(new CodeDynamic(
          new CodeLoc.Some(loc),
          new JavaBytecodeInstructionObj(
              new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, isExc))));
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
      if (clause.body().value() instanceof Opt.Some<Value>) {
        var catchDynOpt = cvToDynamic(erb, xx, loc, clause.body());
        if (catchDynOpt.isEmpty()) return EResult.errs(erb.errors());
        seq.add(catchDynOpt.get());
      } else {
        // The catch body never falls through: its exits and throws carry
        // their own cleanup paths.
        var catchDyn = clause.body().code().dynamic();
        if (catchDyn != null) seq.add(catchDyn);
      }
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(joinKey)));
    }
    if (finallyNormal instanceof Opt.Some<Code> someFinally) {
      var finDyn = someFinally.value().dynamic();
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
  public EResult<Code> targetJump(QExprId loc, JumpKey dest) {
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, new JavaBytecodeJump(dest))));
  }

  @Override
  public EResult<Code> targetLand(QExprId loc, JumpKey dest) {
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, new JavaBytecodeLand(dest))));
  }

  @Override
  public EResult<Code> targetGuarded(QExprId loc, JumpKey dest, Code inner) {
    var innerDyn = inner.dynamic();
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
    var cleanupDyn = cleanup.dynamic();
    if (cleanupDyn != null) {
      // The cleanup may be a shared template (a finally body embedded in one
      // node per state); labels are single-emission, so every embedding is a
      // fresh instance.
      seq.add(JavaBytecodeUtils.freshen(cleanupDyn));
    }
    seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, excKey)));
    if (parent instanceof Opt.Some<JumpKey> someParent) {
      seq.add(new CodeDynamic(loc, new JavaBytecodeJump(someParent.value())));
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
      Opt<compilerxyz.eval.Type> carryType,
      Code cleanup,
      JumpKey next) {
    var seq = new JavaBytecodeSequence();
    seq.add(new CodeDynamic(loc, new JavaBytecodeLand(self)));
    var cleanupDyn = cleanup.dynamic();
    JavaBytecodeBindingKey parkKey = null;
    compilerxyz.eval.Type parkType = null;
    if (carryType instanceof Opt.Some<compilerxyz.eval.Type> someCarry && cleanupDyn != null) {
      // The carried value only leaves the stack when cleanup actually runs
      // between the jump and the land — cleanup code may contain handler
      // entries, which discard the operand stack.
      parkType = someCarry.value();
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
    compilerxyz.eval.Type resultType = null;
    boolean allControlEnded = true;
    for (var branch : conditions) {
      var condDynOpt = cvToDynamic(erb, xx, loc, branch.condition());
      if (condDynOpt.isEmpty()) return EResult.errs(erb.errors());
      var bodyCv = branch.body();
      boolean bodyControlEnded = !(bodyCv.value() instanceof Opt.Some<Value>);
      if (!bodyControlEnded) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = ((Opt.Some<Value>) bodyCv.value()).value().valueType();
        }
      }
      CodeDynamic bodyDyn;
      if (bodyControlEnded) {
        bodyDyn = bodyCv.code() instanceof compilerxyz.eval.Code.Target ct
            && ct.impl() instanceof CodeDynamic cd
            ? cd
            : new CodeDynamic(loc, new compilerxyz.eval.dynamic.JavaBytecodeSequence());
      } else {
        var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
        if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyDyn = bodyDynOpt.get();
      }
      var nextLabel = new LabelNode();
      seq.add(condDynOpt.get());
      seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextLabel)));
      seq.add(bodyDyn);
      if (!bodyControlEnded) {
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      }
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextLabel));
    }
    if (else_ instanceof Opt.Some<CodeValue> someElse) {
      var elseCv = someElse.value();
      boolean elseControlEnded = !(elseCv.value() instanceof Opt.Some<Value>);
      if (!elseControlEnded) allControlEnded = false;
      if (!elseControlEnded) {
        if (resultType == null) {
          resultType = ((Opt.Some<Value>) elseCv.value()).value().valueType();
        }
        var elseDynOpt = cvToDynamic(erb, xx, loc, elseCv);
        if (elseDynOpt.isEmpty()) return EResult.errs(erb.errors());
        seq.add(elseDynOpt.get());
      } else {
        if (elseCv.code() instanceof compilerxyz.eval.Code.Target ct
            && ct.impl() instanceof CodeDynamic ecd) {
          seq.add(ecd);
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
          compilerxyz.eval.Code.of(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public EResult<CodeValue> targetMatch(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      TList<TargetCase> cases,
      Opt<CodeValue> fallback) {
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
    boolean allControlEnded = true;
    var baseType = ((Opt.Some<Value>) base.value()).value().valueType();
    compilerxyz.eval.value.source_union.TypeSourceUnionDynamic sud;
    if (baseType instanceof compilerxyz.eval.value.source_union.TypeSourceUnionDynamic direct) {
      sud = direct;
    } else if (baseType
        instanceof compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst wrapped) {
      sud = wrapped.type_();
    } else {
      return EResult.err(loc, "Match: base type is not a source union: " + baseType.typeDesc());
    }
    var unionIndex = sud.set.unions.indexOf(sud);
    if (unionIndex < 0) {
      return EResult.err(loc, "Match: source union not in owning DefinitionSet");
    }
    xx.vx.definitionSets.putIfAbsent(sud.set.defSetId, sud.set);
    for (var branch : cases) {
      var variantInternal =
          JavaInternalName.of(
              SourceUnionEmitter.variantBinaryNameFor(
                      sud.set.defSetId, unionIndex, branch.variantIndex())
                  .replace('.', '/'));
      var payloadDesc =
          compilerxyz.eval.dynamic.SourceUnionEmitter.payloadDescriptor(xx, branch.variantType());
      var bodyCv = branch.body();
      boolean bodyControlEnded = !(bodyCv.value() instanceof Opt.Some<Value>);
      if (!bodyControlEnded) {
        allControlEnded = false;
        if (resultType == null) {
          resultType = ((Opt.Some<Value>) bodyCv.value()).value().valueType();
        }
      }
      CodeDynamic bodyDyn;
      if (bodyControlEnded) {
        var bodyCd = bodyCv.code() instanceof compilerxyz.eval.Code.Target ct
            && ct.impl() instanceof CodeDynamic cd
            ? cd
            : new CodeDynamic(loc, new compilerxyz.eval.dynamic.JavaBytecodeSequence());
        bodyDyn = bodyCd;
      } else {
        var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
        if (bodyDynOpt.isEmpty()) return EResult.errs(erb.errors());
        bodyDyn = bodyDynOpt.get();
      }
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
              new JavaBytecodeStoreLoad(Opcodes.ASTORE, branch.payloadBinding().key())));
      seq.add(bodyDyn);
      if (!bodyControlEnded) {
        seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
      }
      seq.add(JavaBytecodeUtils.wrapObj(loc, nextCaseLabel));
    }
    if (fallback instanceof Opt.Some<CodeValue> someFallback) {
      var fallbackCv = someFallback.value();
      boolean fallbackControlEnded = !(fallbackCv.value() instanceof Opt.Some<Value>);
      if (!fallbackControlEnded) allControlEnded = false;
      if (!fallbackControlEnded) {
        if (resultType == null) {
          resultType = ((Opt.Some<Value>) fallbackCv.value()).value().valueType();
        }
        var fallbackDynOpt = cvToDynamic(erb, xx, loc, fallbackCv);
        if (fallbackDynOpt.isEmpty()) return EResult.errs(erb.errors());
        seq.add(fallbackDynOpt.get());
      } else {
        if (fallbackCv.code() instanceof compilerxyz.eval.Code.Target ct
            && ct.impl() instanceof CodeDynamic fcd) {
          seq.add(fcd);
        }
      }
    } else if (allControlEnded) {
      // Every case body is control-ended; no fallback is needed since the
      // match is exhaustive. Emit an unreachable ATHROW so verifier does not
      // require a fall-through value here.
      seq.add(JavaBytecodeUtils.instantiate(loc,
          JavaInternalName.of("java/lang/AssertionError"),
          JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID),
          new CodeDynamic(loc, new compilerxyz.eval.dynamic.JavaBytecodeSequence())));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
    }
    seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
    if (allControlEnded) {
      return EResult.ok(new CodeValue(
          compilerxyz.eval.Code.of(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }
    var finalType =
        resultType == null ? (compilerxyz.eval.Type) new TypeNullDynamic() : resultType;
    var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
    if (stackOpt.isEmpty()) return EResult.errs(erb.errors());
    return EResult.ok(stackOpt.get().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  private static Optional<CodeDynamic> cvToDynamic(
      Erb erb, ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      erb.errPush("TargetDynamic::if_: CodeValue has no value");
      return Optional.empty();
    }
    var vtype = some.value().valueType();
    var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
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
        var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.isEmpty()) return EResult.errs(erb.errors());
        var innerJsOpt = erb.mustEr(consumeOpt.get().aspectConsumeDynamicToDynamic(xx, loc, cv));
        if (innerJsOpt.isEmpty()) return EResult.errs(erb.errors());
        restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, liftId));
        restElements.add(innerJsOpt.get());
      }
    }
    for (var cv : graftValues) {
      if (cv.code().isNone()) {
        var graftCv = new CodeValue(Code.none(loc), cv.outcome());
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
        var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
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
    var bindDynOpt = compilerxyz.gen.GenAspectBindDynamic.getMaybe(xx, vtype);
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
    var sortedSubs = sortBindSubs(xx, entries, ConstKey.CANONICAL_ORDER);
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
    var sortedSubs = sortBindSubs(xx, entries, Integer::compare);
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
  public EResult<Opt<Code>> targetCastException(
      ExprContext xx,
      QExprId loc,
      JumpKey self,
      compilerxyz.eval.Type calleeExc,
      compilerxyz.eval.Type callerExc,
      Opt<JumpKey> next) {
    var erb = new Erb(loc);
    // Materialize the in-flight exception through a fresh local — pure
    // target machinery: the handler frame's single stack element lands in a
    // slot and reads back as a value of the callee's exception type.
    var catchBinding = BindingId.localFresh();
    var initialBindOpt = erb.mustEr(GenAspectInitialBind.get(xx, loc, calleeExc));
    if (initialBindOpt.isEmpty()) return EResult.errs(erb.errors());
    var binding = initialBindOpt.get().aspectInitialBindInitialBind(catchBinding);
    var boundCv =
        binding.bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc));
    var castCvOpt = erb.mustEr(calleeExc.typeCastTo(xx, loc, boundCv, callerExc));
    if (castCvOpt.isEmpty()) return EResult.errs(erb.errors());
    var castCv = castCvOpt.get();
    if (castCv.code().isNone()) {
      return EResult.ok(Opt.<Code>none());
    }
    var seq = new JavaBytecodeSequence();
    seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(self)));
    seq.add(
        new CodeDynamic(
            new CodeLoc.Some(loc),
            new JavaBytecodeStoreLoad(Opcodes.ASTORE, catchBinding.key())));
    var castDynOpt = cvToDynamic(erb, xx, loc, castCv);
    if (castDynOpt.isEmpty()) return EResult.errs(erb.errors());
    seq.add(castDynOpt.get());
    if (next instanceof Opt.Some<JumpKey> someNext) {
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(someNext.value())));
    } else {
      seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
    }
    return EResult.ok(Opt.some(Code.of(loc, new CodeDynamic(loc, seq))));
  }

  @Override
  public EResult<Code> targetFresh(QExprId loc, Code code) {
    var dyn = code.dynamic();
    if (dyn == null) return EResult.ok(code);
    return EResult.ok(Code.of(loc, JavaBytecodeUtils.freshen(dyn)));
  }

  private static <K> TList<TargetBindSub> sortBindSubs(
      ExprContext xx,
      compilerxyz.util.TOrderedMap<K, TargetBindSub> entries,
      java.util.Comparator<K> tiebreaker) {
    var typeEntries = new compilerxyz.util.TOrderedMap<K, compilerxyz.eval.Type>();
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
