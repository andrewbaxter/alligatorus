package compilerxyz.eval.value;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.util.Opt;
import org.objectweb.asm.Opcodes;

@Builtin(uniqueId = 1043L)
@BuiltinPath("eval.value_source")
public sealed interface ValueDynamicSource
    permits ValueDynamicSource.Binding, ValueDynamicSource.Expr, ValueDynamicSource.Field {

  CodeDynamic realize(ExprContext xx, QExprId loc);

  Code drop(QExprId loc);

  record Binding(BindingId key) implements ValueDynamicSource {
    @Override
    public CodeDynamic realize(ExprContext xx, QExprId loc) {
      return BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, key);
    }

    @Override
    public Code drop(QExprId loc) {
      return Code.none(loc);
    }
  }

  record Expr(int stackSize) implements ValueDynamicSource {
    public Expr {
      if (stackSize != 1 && stackSize != 2) {
        throw new IllegalArgumentException(
            "ValueDynamicSource.Expr stackSize must be 1 or 2, got " + stackSize);
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
      var acdOpt = compilerxyz.gen.GenAspectConsumeDynamic.getMaybe(xx, t);
      if (!(acdOpt instanceof Opt.Some<compilerxyz.gen.AspectConsumeDynamic> some)) {
        throw new IllegalStateException(
            "stackFor: type has no AspectConsumeDynamic: " + t);
      }
      var d = some.value().aspectConsumeDynamicJvmClass(xx).value();
      var c = d.charAt(0);
      return new Expr(c == 'D' || c == 'J' ? 2 : 1);
    }
  }

  record Field(
      ValueDynamicSource base,
      JavaInternalName klass,
      String slotName,
      JavaDataDescriptor slotDesc)
      implements ValueDynamicSource {
    @Override
    public CodeDynamic realize(ExprContext xx, QExprId loc) {
      var seq = new JavaBytecodeSequence();
      seq.add(base.realize(xx, loc));
      seq.add(JavaBytecodeUtils.accessField(loc, klass, slotName, slotDesc));
      return new CodeDynamic(loc, seq);
    }

    @Override
    public Code drop(QExprId loc) {
      return base.drop(loc);
    }
  }

}
