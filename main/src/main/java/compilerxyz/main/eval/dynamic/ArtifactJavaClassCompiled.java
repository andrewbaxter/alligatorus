package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ArtifactExpr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public record ArtifactJavaClassCompiled(byte[] bytecode, TList<ArtifactExpr.ArtifactQExprId> locs) {
    public static ArtifactJavaClassCompiled fromCompiled(JavaClassCompiled compiled) {
        var locs = new TList<ArtifactExpr.ArtifactQExprId>(compiled.locs().size());
        for (var loc : compiled.locs()) {
            locs.add(new ArtifactExpr.ArtifactQExprId(loc.source().value(), loc.expr()));
        }
        return new ArtifactJavaClassCompiled(compiled.bytecode(), locs);
    }

    public JavaClassCompiled toCompiled() {
        var out = new TList<QExprId>(locs.size());
        for (var loc : locs) {
            out.add(new QExprId(new SourceId(loc.source()), loc.expr()));
        }
        return new JavaClassCompiled(bytecode, IList.from(out));
    }
}
