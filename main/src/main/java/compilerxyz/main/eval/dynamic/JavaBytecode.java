package compilerxyz.main.eval.dynamic;

public sealed interface JavaBytecode
        permits CodeDynamic,
                JavaBytecodeSequence,
                JavaBytecodeInsn,
                JavaBytecodeLand,
                JavaBytecodeJump,
                JavaBytecodeBranch,
                JavaBytecodeReturn,
                JavaBytecodeThrow,
                JavaBytecodeGuarded {}
