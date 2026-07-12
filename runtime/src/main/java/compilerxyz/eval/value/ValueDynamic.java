package compilerxyz.eval.value;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectLift;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;

public record ValueDynamic(Source source, Type type_) implements Value {
  @Override
  public Type valueType() {
    return type_;
  }

  /**
   * Build a ValueDynamic whose Source is a fresh stack Expr sized to match the
   * type's JVM representation (2 slots for D/J primitives, 1 otherwise). Used
   * at every site that produces a value directly onto the JVM stack.
   */
  public static ValueDynamic stack(ExprContext xx, Type type_) {
    return new ValueDynamic(Source.Expr.stackFor(xx, type_), type_);
  }

  public static Type sourceDynamicType() {
    return SourceDynamicType.INSTANCE;
  }

  final class SourceDynamicType {
    static final TypeObjectDynamic INSTANCE = build();

    private SourceDynamicType() {}

    private static TypeObjectDynamic build() {
      var jvmClass = JavaInternalName.of("compilerxyz/eval/value/ValueDynamic$Source");
      return new TypeObjectDynamic(
          GenTypeIds.TYPE_SOURCE_DYNAMIC_TYPE_ID,
          "source",
          jvmClass,
          JavaDataDescriptor.fromInternalName(jvmClass),
          new TList<ObjectDataField>(),
          new TList<ObjectMethod>(),
          ObjectDropKind.DYNAMIC_LIGHT,
          new TList<Unique>(),
          new TList<TypeObjectCastRule>(),
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none(),
          Opt.<JavaInternalName>none());
    }
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

      public static Expr stackFor(ExprContext xx, Type t) {
        if (!(t instanceof compilerxyz.gen.AspectConsumeDynamic acd)) {
          throw new IllegalArgumentException(
              "ValueDynamic.Source.Expr.stackFor: type "
                  + t
                  + " is not AspectConsumeDynamic; cannot determine JVM stack size");
        }
        var d = acd.aspectConsumeDynamicConsumeDynamicJvmClass(xx).value();
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

    record Dup(Source base, Type type_) implements Source {
      @Override
      public CodeDynamic realize(ExprContext xx, QExprId loc) {
        if (!(type_ instanceof compilerxyz.gen.AspectConsumeDynamic acd)) {
          throw new IllegalStateException(
              "ValueDynamic.Source.Dup.realize: type "
                  + type_
                  + " is not AspectConsumeDynamic");
        }
        return acd.aspectConsumeDynamicEmitDup(xx, loc);
      }

      @Override
      public Code drop(QExprId loc) {
        return Code.none(loc);
      }
    }
  }
}
