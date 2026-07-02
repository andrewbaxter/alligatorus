package compilerxyz.eval;

import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;

public final class ExprArtifactCodec {
  private ExprArtifactCodec() {}

  public static JsonValue toJson(ArtifactExpr expr) {
    return JsonValues.toValue(expr);
  }

  public static ArtifactExpr fromJson(JsonValue value) {
    return JsonValues.fromValue(ArtifactExpr.class, value);
  }
}
