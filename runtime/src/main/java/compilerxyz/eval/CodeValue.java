package compilerxyz.eval;

import compilerxyz.eval.value.code_value_extern.TypeCodeValueExternDynamic;
import compilerxyz.util.Opt;

public record CodeValue(Code code, Opt<Value> value) {

  public static Type dynamicType() {
    return new TypeCodeValueExternDynamic();
  }
}
