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
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.eval.TypeId;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrait(
    uniqueId = 2006L,
    scopeId = 3006)
@compilerxyz.annotations.BuiltinPath("eval.target")
@compilerxyz.annotations.BuiltinTypeId(7024L)
public interface Target {
    @compilerxyz.annotations.SourceExposed
    BindResult targetBind(ExprContext xx, QExprId loc, TargetBindSub sub);

    @compilerxyz.annotations.SourceExposed
    EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv);

    @compilerxyz.annotations.SourceExposed
    EResult<TargetBindSub> targetCodeToBindSub(QExprId loc, Code code, Type type_);

    @compilerxyz.annotations.SourceExposed
    TargetBindSub targetBindSubRecord(ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields);

    @compilerxyz.annotations.SourceExposed
    TargetBindSub targetBindSubTuple(ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots);

    @compilerxyz.annotations.SourceExposed
    EResult<Code> targetCastException(
            ExprContext xx, QExprId loc, Code callCode, BindingId catchBinding, CodeValue castCv);

    @compilerxyz.annotations.SourceExposed
    EResult<Code> targetSeq(QExprId loc, TList<Code> entries);

    @compilerxyz.annotations.SourceExposed
    EResult<Code> targetTryFinally(QExprId loc, Code body, Code finally_);

    @compilerxyz.annotations.SourceExposed
    EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv);

    @compilerxyz.annotations.SourceExposed
    EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues);

    @compilerxyz.annotations.SourceExposed
    EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_);

    @compilerxyz.annotations.SourceExposed
    EResult<CodeDynamic> targetTryCatchFinally(
            QExprId loc, CodeDynamic body, Opt<TryCatchClause> catch_, Opt<CodeDynamic> finally_);

    @compilerxyz.annotations.SourceExposed
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
            var trivialIdentityAspects = new TList<Unique>();
            trivialIdentityAspects.add(GenBuiltinInterfaces.builtinTargetIfaceIdentity());
            return new TypeObjectDynamic(
                    GenTypeIds.TARGET_TYPE_ID,
                    new TypeObjectMeta("target", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
                    JavaInternalName.fromClass(Target.class),
                    JavaDataDescriptor.fromClass(Target.class),
                    Target.class,
                    new TypeId.Composite(GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID, TList.of(GenTypeIds.TARGET_TYPE_ID)),
                    new TList<ObjectDataField>(),
                    new TList<ObjectMethod>(),
                    ObjectDropKind.DYNAMIC_LIGHT,
                    Opt.<JavaInternalName>none());
        }
    }
}
