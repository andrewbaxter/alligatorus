package compilerxyz.eval.dynamic;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;

/**
 * A dynamic-target matchable type: it knows how to discriminate and destructure
 * its own variants at runtime. {@code targetMatch} routes through this instead
 * of hardcoding a single union representation, so any type (a source union, or
 * a builtin like {@code eresult}) can be matched by providing the two code
 * fragments below.
 *
 * <p>Both fragments run with the base value's reference on top of the operand
 * stack (the caller has just reloaded it from a local).
 */
public interface MatchDynamic {

  /** Called once before the branch chain; e.g. register the owning definition set. */
  EResult<Void> matchDynamicPrepare(ExprContext xx, QExprId loc);

  /** Stack: {@code [base] -> [boolean]} — is the base the variant at {@code variantIndex}? */
  EResult<CodeDynamic> matchDynamicTest(ExprContext xx, QExprId loc, int variantIndex);

  /**
   * Stack: {@code [base] -> [payload]} — the payload of the variant at
   * {@code variantIndex}, whose type is {@code variantType}.
   */
  EResult<CodeDynamic> matchDynamicExtract(
      ExprContext xx, QExprId loc, int variantIndex, Type variantType);
}
