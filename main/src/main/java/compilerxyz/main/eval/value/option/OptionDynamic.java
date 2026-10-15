package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.prelude.util.Opt;

public sealed interface OptionDynamic extends Type permits TypeOptionDynamic, TypeOptionSpecializedDynamic {
    ArtifactSupport.ArtifactIdHolder optionDynamicArtifactIdCell();

    Type optionDynamicElementType();

    CodeDynamicReg optionDynamicConstruct(ExprContext xx, QExprId loc, Opt<JavaBytecodeReg> payload);
}
