package compilerxyz.eval.value;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import org.objectweb.asm.Opcodes;

public record ValueDynamic(Source source, Type type_) implements Value {
  @Override
  public Type valueTypeOf() {
    return type_;
  }

  /**
   * Build a ValueDynamic whose Source is a fresh stack Expr sized to match the
   * type's JVM representation (2 slots for D/J primitives, 1 otherwise). Used
   * at every site that produces a value directly onto the JVM stack.
   */
  public static ValueDynamic stack(Type type_) {
    return new ValueDynamic(Source.Expr.stackFor(type_), type_);
  }

  public sealed interface Source {

    /**
     * Emit code that reconstructs this source as a fresh stack value. Noop for
     * Expr (already on the stack in the caller's context), a load for Binding,
     * base.realize + member access for Field.
     */
    CodeDynamic realize(ExprContext xx, QExprId loc);

    /**
     * Emit code to discard the stack footprint of this source's base. Binding
     * bases have no footprint (noop); Expr bases are dropped with POP/POP2 per
     * their declared stack size; Field forwards to base.drop.
     */
    Code drop(QExprId loc);

    record Binding(BindingId key) implements Source {
      @Override
      public CodeDynamic realize(ExprContext xx, QExprId loc) {
        return BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, key);
      }

      @Override
      public Code drop(QExprId loc) {
        return Code.none(loc);
      }
    }

    record Expr(int stackSize) implements Source {
      public Expr {
        if (stackSize != 1 && stackSize != 2) {
          throw new IllegalArgumentException(
              "ValueDynamic.Source.Expr stackSize must be 1 or 2, got " + stackSize);
        }
      }

      @Override
      public CodeDynamic realize(ExprContext xx, QExprId loc) {
        return new CodeDynamic(loc, new JavaBytecodeSequence());
      }

      @Override
      public Code drop(QExprId loc) {
        int popOp = stackSize == 2 ? Opcodes.POP2 : Opcodes.POP;
        return Code.of(loc, JavaBytecodeUtils.inst(loc, popOp));
      }

      public static Expr stackFor(Type t) {
        if (!(t instanceof compilerxyz.gen.AspectConsumeDynamic acd)) {
          throw new IllegalArgumentException(
              "ValueDynamic.Source.Expr.stackFor: type "
                  + t
                  + " is not AspectConsumeDynamic; cannot determine JVM stack size");
        }
        var d = acd.consumeDynamicJvmClass().value();
        var c = d.charAt(0);
        return new Expr(c == 'D' || c == 'J' ? 2 : 1);
      }
    }

    record Field(Source base, String key) implements Source {
      @Override
      public CodeDynamic realize(ExprContext xx, QExprId loc) {
        return DynUtil.buildFieldGetSequence(loc, base.realize(xx, loc), key);
      }

      @Override
      public Code drop(QExprId loc) {
        return base.drop(loc);
      }
    }

    record Dup(Source base) implements Source {
      @Override
      public CodeDynamic realize(ExprContext xx, QExprId loc) {
        return JavaBytecodeUtils.inst(loc, Opcodes.DUP);
      }

      @Override
      public Code drop(QExprId loc) {
        return Code.none(loc);
      }
    }
  }
}
