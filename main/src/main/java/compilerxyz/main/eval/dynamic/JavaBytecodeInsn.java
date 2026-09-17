package compilerxyz.main.eval.dynamic;

import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class JavaBytecodeInsn implements JavaBytecode {
    public final TList<JavaBytecodeReg> args;
    public final Opt<JavaBytecodeReg> out;
    public final JavaBytecodeInsnBody body;

    public JavaBytecodeInsn(TList<JavaBytecodeReg> args, Opt<JavaBytecodeReg> out, JavaBytecodeInsnBody body) {
        switch (body) {
            case JavaBytecodeInsnBody.New n -> {
                if (out.optIsNone()) throw new IllegalArgumentException("New instruction must define a register");
            }
            case JavaBytecodeInsnBody.ArrayNew a -> {
                if (out.optIsNone()) throw new IllegalArgumentException("ArrayNew instruction must define a register");
            }
            case JavaBytecodeInsnBody.Var v -> {
                if (JavaBytecodeUtils.isLoadOpcode(v.opcode())
                        ? (!args.isEmpty() || out.optIsNone())
                        : (args.size() != 1 || out.optIsSome())) {
                    throw new IllegalArgumentException("Var instruction shape mismatch");
                }
            }
            case JavaBytecodeInsnBody.VarIndexed v -> {
                if (JavaBytecodeUtils.isLoadOpcode(v.opcode())
                        ? (!args.isEmpty() || out.optIsNone())
                        : (args.size() != 1 || out.optIsSome())) {
                    throw new IllegalArgumentException("VarIndexed instruction shape mismatch");
                }
            }
            case JavaBytecodeInsnBody.Op o -> {}
            case JavaBytecodeInsnBody.Node n -> {}
        }
        this.args = args;
        this.out = out;
        this.body = body;
    }
}
