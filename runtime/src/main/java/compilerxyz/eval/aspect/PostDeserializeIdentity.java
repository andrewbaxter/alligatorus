package compilerxyz.eval.aspect;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.gen.AspectPostDeserialize;

public final class PostDeserializeIdentity implements AspectPostDeserialize {
  public static final PostDeserializeIdentity INSTANCE = new PostDeserializeIdentity();

  private PostDeserializeIdentity() {}

  @Override
  public EResult<CodeValue> aspectPostDeserializePostDeserialize(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }
}
