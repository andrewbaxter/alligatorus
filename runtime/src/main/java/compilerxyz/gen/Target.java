package compilerxyz.gen;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.IfBranch;
import compilerxyz.eval.dynamic.target.RecordBindField;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.dynamic.target.TryCatchClause;
import compilerxyz.eval.dynamic.target.TupleBindSlot;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

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

    EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues);

    EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_);

    EResult<CodeDynamic> targetTryCatchFinally(
            QExprId loc, CodeDynamic body, Opt<TryCatchClause> catch_, Opt<CodeDynamic> finally_);

    EResult<CodeValue> targetMatch(
            ExprContext xx,
            QExprId loc,
            CodeValue base,
            ConstKey elementKey,
            TList<TargetCase> cases,
            Opt<Expr> fallback);

    static Type dynamicType() {
        return DynamicType.INSTANCE;
    }

    final class DynamicType {
        static final TypeObjectDynamic INSTANCE = build();

        private DynamicType() {}

        private static TypeObjectDynamic build() {
            var identityAspects = new TList<Unique>();
            identityAspects.add(GenBuiltinInterfaces.builtinTargetIfaceIdentity());
            return new TypeObjectDynamic(
                    GenTypeIds.TYPE_ANY_TARGET_DYNAMIC_TYPE_ID,
                    "target",
                    JavaInternalName.of("compilerxyz/gen/Target"),
                    JavaDataDescriptor.fromClass(Target.class),
                    new TList<ObjectDataField>(),
                    new TList<ObjectMethod>(),
                    ObjectDropKind.DYNAMIC_LIGHT,
                    identityAspects,
                    new TList<TypeObjectCastRule>(),
                    new TList<TypeObjectCastFromRule>(),
                    Opt.<TypeObjectLift>none(),
                    Opt.<ObjectAspectSource>none(),
                    Opt.<JavaInternalName>none());
        }
    }
}
