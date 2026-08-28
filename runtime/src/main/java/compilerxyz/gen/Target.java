package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.JumpKey;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinFull(uniqueId = 2006L, scopeId = 3006)
@BuiltinPath("eval.target")
public interface Target {
    BindResult targetBind(ExprContext xx, QExprId loc, TargetBindSub sub);

    EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv);

    EResult<TargetBindSub> targetCodeToBindSub(QExprId loc, Code code, Type type_);

    TargetBindSub targetBindSubRecord(ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields);

    TargetBindSub targetBindSubTuple(ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots);

    EResult<Opt<Code>> targetCastException(
            ExprContext xx, QExprId loc, JumpKey self, Type calleeExc, Type callerExc, Opt<JumpKey> next);

    EResult<Code> targetSeq(QExprId loc, TList<Code> entries);

    EResult<Code> targetJump(QExprId loc, JumpKey dest);

    EResult<Code> targetLand(QExprId loc, JumpKey dest);

    EResult<Code> targetGuarded(QExprId loc, JumpKey dest, Code inner);

    EResult<Code> targetExcCleanup(ExprContext xx, QExprId loc, JumpKey self, Code cleanup, Opt<JumpKey> parent);

    EResult<Code> targetExitTrampoline(
            ExprContext xx, QExprId loc, JumpKey self, Opt<Type> carryType, Code cleanup, JumpKey next);

    EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv);

    CodeValue targetReturn(ExprContext xx, QExprId loc, CodeValue cv);

    EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues);

    EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<CodeValue> else_);

    EResult<Code> targetTryCatchFinally(
            ExprContext xx,
            QExprId loc,
            CodeValue body,
            Opt<TryCatchClause> catch_,
            Opt<JumpKey> catchHandler,
            Opt<Code> finallyNormal);

    EResult<Code> targetFresh(QExprId loc, Code code);

    EResult<CodeValue> targetMatch(
            ExprContext xx, QExprId loc, CodeValue base, TList<TargetCase> cases, Opt<TargetMatchFallback> fallback);
}
