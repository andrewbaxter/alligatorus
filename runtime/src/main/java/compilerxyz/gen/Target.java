package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.IfBranch;
import compilerxyz.eval.dynamic.target.RecordBindField;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.dynamic.target.TryCatchClause;
import compilerxyz.eval.dynamic.target.TupleBindSlot;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@Builtin(uniqueId = 2006L, scopeId = 3006)
@BuiltinPath("eval.target")
public interface Target {
    BindResult targetBind(ExprContext xx, QExprId loc, TargetBindSub sub);

    EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv);

    EResult<TargetBindSub> targetCodeToBindSub(QExprId loc, Code code, Type type_);

    TargetBindSub targetBindSubRecord(ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields);

    TargetBindSub targetBindSubTuple(ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots);

    EResult<Code> targetCastException(
            ExprContext xx, QExprId loc, Code callCode, BindingId catchBinding, CodeValue castCv);

    EResult<Code> targetSeq(QExprId loc, TList<Code> entries);

    EResult<Code> targetTryFinally(QExprId loc, Code body, Code finally_);

    EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv);

    CodeValue targetReturn(ExprContext xx, QExprId loc, CodeValue cv);

    EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues);

    EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_);

    EResult<CodeDynamic> targetTryCatchFinally(
            ExprContext xx,
            QExprId loc,
            CodeValue body,
            Opt<TryCatchClause> catch_,
            Opt<CodeDynamic> finally_);

    EResult<CodeValue> targetMatch(
            ExprContext xx,
            QExprId loc,
            CodeValue base,
            ConstKey elementKey,
            TList<TargetCase> cases,
            Opt<Expr> fallback);

}
