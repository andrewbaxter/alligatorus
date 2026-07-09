package compilerxyz.eval;

import compilerxyz.eval.dynamic.StageLiftCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.source_interface.InterfaceData;

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

  record CodeValueExternExternal(CodeValue codeValue) implements External {
    @Override
    public Object externalHostValue() {
      return codeValue;
    }

    @Override
    public Class<?> externalHostClass() {
      return CodeValue.class;
    }
  }

  record ExprContextExternal(ExprContext xx) implements External {
    @Override
    public Object externalHostValue() {
      return xx;
    }

    @Override
    public Class<?> externalHostClass() {
      return ExprContext.class;
    }
  }

  record QExprIdExternal(QExprId loc) implements External {
    @Override
    public Object externalHostValue() {
      return loc;
    }

    @Override
    public Class<?> externalHostClass() {
      return QExprId.class;
    }
  }

  record InterfaceRefExternal(InterfaceData data) implements External {
    @Override
    public Object externalHostValue() {
      return data;
    }

    @Override
    public Class<?> externalHostClass() {
      return InterfaceData.class;
    }
  }
}
