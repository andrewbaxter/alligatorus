package compilerxyz.main.eval.builtin;

import compilerxyz.main.eval.EvaluationLocal;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.prelude.annotations.BuiltinFull;
import java.util.Arrays;

@BuiltinFull(uniqueId = 6467L)
public final class FileHandle implements EvaluationLocal {
    public final int len;
    private final byte[] bytes;

    public FileHandle(byte[] bytes) {
        this.bytes = bytes;
        this.len = bytes.length;
    }

    public byte[] read(int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > this.bytes.length) {
            throw new DynPanic("file.read: range [" + offset + ", " + (offset + len) + ") outside file of length "
                    + this.bytes.length);
        }
        return Arrays.copyOfRange(this.bytes, offset, offset + len);
    }
}
