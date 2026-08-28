package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectPostDeserialize;

public final class PostDeserializeIdentity implements AspectPostDeserialize {
    public static final PostDeserializeIdentity INSTANCE = new PostDeserializeIdentity();

    private PostDeserializeIdentity() {}

    @Override
    public EResult<CodeValue> aspectPostDeserializePostDeserialize(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }
}
