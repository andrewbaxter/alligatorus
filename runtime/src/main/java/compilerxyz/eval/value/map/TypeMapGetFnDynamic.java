package compilerxyz.eval.value.map;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.TList;

public record TypeMapGetFnDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectFunction,
        ExporterAny {

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_MAP_GET_FN_DYNAMIC_TYPE_ID, TList.of(keyType.typeId(), valueType.typeId())); }


  @Override
  public String typeDesc() {
    return "map get fn";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.MapGetFnDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    return EResult.err(
        loc,
        "TypeMapGetFnDynamic::call: needs typecheck_convert + aspect_consume_dynamic_get +"
            + " AspectStackValueDynamic + Map.get lowering (deferred)");
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
