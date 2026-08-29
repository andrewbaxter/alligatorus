package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.annotations.CodegenDescriptor;

@BuiltinFull(uniqueId = 6016L)
@BuiltinPath("types.code")
public sealed interface Code permits Code.None, Code.Target {

    record None(QExprId loc) implements Code {
        @Override
        public CodeDynamic codeDynamic() {
            return null;
        }

        @Override
        public boolean codeIsNone() {
            return true;
        }
    }

    record Target(TargetCode impl) implements Code {
        @Override
        public CodeDynamic codeDynamic() {
            return impl instanceof CodeDynamic cd ? cd : null;
        }

        @Override
        public boolean codeIsNone() {
            return false;
        }
    }

    CodeDynamic codeDynamic();

    boolean codeIsNone();

    static Code none(QExprId loc) {
        return new None(loc);
    }

    static Code of(CodeDynamic dynamic) {
        return new Target(dynamic);
    }

    @CodegenDescriptor
    static Code target(TargetCode impl) {
        return new Target(impl);
    }

    static Code concat(QExprId loc, Code first, Code second) {
        var firstDyn = first.codeDynamic();
        var secondDyn = second.codeDynamic();
        if (firstDyn == null) return secondDyn == null ? first : second;
        if (secondDyn == null) return first;
        var seq = new JavaBytecodeSequence();
        seq.add(firstDyn);
        seq.add(secondDyn);
        return of(new CodeDynamic(loc, seq));
    }

    @CodegenDescriptor
    static TargetCode targetImpl(Code code) {
        return switch (code) {
            case Code.Target t -> t.impl();
            case Code.None n -> throw new IllegalStateException("Code::targetImpl: code is None, not Target");
        };
    }
}
