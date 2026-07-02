package compilerxyz.eval.dynamic;

import compilerxyz.eval.QExprId;
import compilerxyz.eval.TargetCode;

public record CodeDynamic(QExprId loc, JavaBytecode code) implements TargetCode {
}
