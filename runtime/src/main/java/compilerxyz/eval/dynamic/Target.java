package compilerxyz.eval.dynamic;

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
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.IfBranch;
import compilerxyz.eval.dynamic.target.RecordBindField;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.target.TargetCase;
import compilerxyz.eval.dynamic.target.TryCatchClause;
import compilerxyz.eval.dynamic.target.TupleBindSlot;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface Target {

  static Type dynamicType() {
    return TargetDynamicType.INSTANCE;
  }

  final class TargetDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private TargetDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JvmConstants.INTNAME_OBJECT;
      var identityAspects = new TList<Unique>();
      identityAspects.add(GenBuiltinInterfaces.builtinTargetIfaceIdentity());
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_ANY_TARGET_DYNAMIC_TYPE_ID,
          "target",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          identityAspects,
          new TList<compilerxyz.eval.value.object_.TypeObjectCastRule>(),
          new TList<compilerxyz.eval.value.object_.TypeObjectCastFromRule>(),
          Opt.<compilerxyz.eval.value.object_.TypeObjectLift>none(),
          Opt.<compilerxyz.eval.value.object_.ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
  }

  BindResult targetBind(QExprId loc, TargetBindSub sub);

  EResult<Opt<TargetBindSub>> targetMaybeBindSub(ExprContext xx, QExprId loc, CodeValue cv);

  EResult<TargetBindSub> targetCodeToBindSub(QExprId loc, Code code, Type type_);

  TargetBindSub targetBindSubRecord(
      ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields);

  TargetBindSub targetBindSubTuple(
      ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots);

  EResult<Code> targetCastException(
      ExprContext xx, QExprId loc, Code callCode, BindingId catchBinding, CodeValue castCv);

  EResult<Code> targetSeq(QExprId loc, TList<Code> entries);

  EResult<Code> targetTryFinally(QExprId loc, Code body, Code finally_);

  EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv);

  EResult<CodeValue> targetStage(
      ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues);

  EResult<CodeValue> targetIf_(
      ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<Expr> else_);

  EResult<CodeDynamic> targetTryCatchFinally(
      QExprId loc,
      CodeDynamic body,
      Opt<TryCatchClause> catch_,
      Opt<CodeDynamic> finally_);

  EResult<CodeValue> targetMatch_(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      ConstKey elementKey,
      TList<TargetCase> cases,
      Opt<Expr> fallback);
}
