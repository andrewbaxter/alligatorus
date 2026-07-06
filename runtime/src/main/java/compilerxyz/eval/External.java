package compilerxyz.eval;

import compilerxyz.eval.dynamic.StageLiftCell;
import compilerxyz.eval.expr.CodeValueExternCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_interface.SourceInterfaceRef;

public sealed interface External {

  Object externalHostValue();

  Class<?> externalHostClass();

  record TypeExternal(Type type) implements External {
    @Override
    public Object externalHostValue() {
      return type;
    }

    @Override
    public Class<?> externalHostClass() {
      return Type.class;
    }
  }

  record UniqueExternal(Unique unique) implements External {
    @Override
    public Object externalHostValue() {
      return unique;
    }

    @Override
    public Class<?> externalHostClass() {
      return Unique.class;
    }
  }

  record StageLiftCellExternal(StageLiftCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }

    @Override
    public Class<?> externalHostClass() {
      return StageLiftCell.class;
    }
  }

  record StagedExprCellExternal(StagedExprCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }

    @Override
    public Class<?> externalHostClass() {
      return StagedExprCell.class;
    }
  }

  record CodeValueExternCellExternal(CodeValueExternCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }

    @Override
    public Class<?> externalHostClass() {
      return CodeValueExternCell.class;
    }
  }

  record SourceInterfaceRefExternal(SourceInterfaceRef ref) implements External {
    @Override
    public Object externalHostValue() {
      return ref;
    }

    @Override
    public Class<?> externalHostClass() {
      return SourceInterfaceRef.class;
    }
  }
}
