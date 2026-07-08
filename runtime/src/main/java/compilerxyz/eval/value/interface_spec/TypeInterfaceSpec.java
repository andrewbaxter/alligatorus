package compilerxyz.eval.value.interface_spec;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;

public record TypeInterfaceSpec()
    implements Type, AspectConsumeDynamic, ExporterAny {

  private static final JavaInternalName INTERNAL_NAME =
      JavaInternalName.of("compilerxyz/eval/value/interface_spec/InterfaceSpec");

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_INTERFACE_SPEC_TYPE_ID;
  }

  @Override
  public String typeDesc() {
    return "interface_spec";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.InterfaceSpec();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public JavaDataDescriptor consumeDynamicJvmClass() {
    return JavaDataDescriptor.fromInternalName(INTERNAL_NAME);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
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
