package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class InterfaceMethodData {
  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }

  public final String name;
  public final TList<String> dataFieldNames;
  public Type argType;
  // Optional return: some(type) if the method yields a value, none if it returns nothing.
  public Opt<Type> retType = Opt.none();

  public Opt<String> hostMethodName = Opt.none();
  public Opt<JavaMethodDescriptor> hostMethodDescriptor = Opt.none();

  public InterfaceMethodData(String name) {
    this.name = name;
    this.dataFieldNames = new TList<>();
  }
}
