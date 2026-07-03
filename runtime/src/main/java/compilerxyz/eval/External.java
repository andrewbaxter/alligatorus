package compilerxyz.eval;

import compilerxyz.eval.dynamic.StageLiftCell;
import compilerxyz.eval.expr.CodeValueExternCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_interface.SourceInterfaceRef;

public sealed interface External {

  Object externalHostValue();

  record TypeExternal(Type type) implements External {
    @Override
    public Object externalHostValue() {
      return type;
    }
  }

  record UniqueExternal(Unique unique) implements External {
    @Override
    public Object externalHostValue() {
      return unique;
    }
  }

  record StageLiftCellExternal(StageLiftCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }
  }

  record StagedExprCellExternal(StagedExprCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }
  }

  record CodeValueExternCellExternal(CodeValueExternCell cell) implements External {
    @Override
    public Object externalHostValue() {
      return cell;
    }
  }

  record SourceInterfaceRefExternal(SourceInterfaceRef ref) implements External {
    @Override
    public Object externalHostValue() {
      return ref;
    }
  }
}
