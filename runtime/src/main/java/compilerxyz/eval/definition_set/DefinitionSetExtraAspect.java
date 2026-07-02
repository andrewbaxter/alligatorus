package compilerxyz.eval.definition_set;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.source_exports.SourceExportsData;
import compilerxyz.eval.value.source_exports.TypeSourceExportsLiftedConst;
import compilerxyz.util.Opt;
import org.jspecify.annotations.Nullable;
import compilerxyz.gen.DefinitionSetBuilder;

public final class DefinitionSetExtraAspect {
  private DefinitionSetExtraAspect() {}

  public static Opt<Aspect> extraAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectLift.ID)) {
      return Opt.some(TypeDefinitionSetLiftDynamic.INSTANCE);
    }
    return Opt.none();
  }

  static final class TypeDefinitionSetLiftDynamic implements AspectLift {
    static final TypeDefinitionSetLiftDynamic INSTANCE = new TypeDefinitionSetLiftDynamic();

    private TypeDefinitionSetLiftDynamic() {}

    @Override
    public EResult<CodeValue> lift(ExprContext xx, QExprId loc, CodeValue cv) {
      return EResult.err(
          loc,
          "TypeDefinitionSetLiftDynamic::lift(CodeValue): staging-side const-lift wrapper not"
              + " used on the JVM target (host runtime value path uses liftRuntime)");
    }

    @Override
    public EResult<Value> liftRuntime(
        ExprContext xx, QExprId loc, @Nullable Object externalHostValue) {
      if (!(externalHostValue instanceof DefinitionSetBuilder dsb)) {
        return EResult.err(
            loc,
            "TypeDefinitionSetLiftDynamic::liftRuntime: expected DefinitionSetBuilder, got "
                + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
      }
      DefinitionSet ds = dsb.inner;
      if (!xx.vx.definitionSets.containsKey(ds.defSetId)) {
        xx.vx.definitionSets.put(ds.defSetId, ds);
      }
      var data = new SourceExportsData(ds.defSetId, ds.functions, ds.objects);
      return EResult.ok(new ValueAnyConst(new TypeSourceExportsLiftedConst(data)));
    }
  }
}
