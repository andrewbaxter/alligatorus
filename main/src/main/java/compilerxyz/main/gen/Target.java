package compilerxyz.main.gen;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 2006L, scopeId = 3006)
@BuiltinPath("eval.target")
public interface Target {
    EResult<Opt<Code>> targetCastException(
            ExprContext xx, QExprId loc, JumpKey self, Type calleeExc, Type callerExc, Opt<JumpKey> next);

    EResult<Binding> targetInitialBind(ExprContext xx, QExprId loc, Type type);

    EResult<Opt<TargetOperandParked>> targetOperandPark(ExprContext xx, QExprId loc, CodeValue operand);

    EResult<Code> targetLabeledStart(QExprId loc, JumpKey end);

    EResult<Code> targetExit(ExprContext xx, QExprId loc, JumpKey dest, JumpKey labeled, CodeValue value);

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
