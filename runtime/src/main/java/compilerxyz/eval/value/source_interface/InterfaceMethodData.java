package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class InterfaceMethodData {
  public static Type dynamicType() {
    return Type.dynamicType();
  }

  public final String name;
  public final TList<String> dataFieldNames;
  public @Nullable Type argType;
  public @Nullable Type retType;

  public @Nullable String hostMethodName;
  public @Nullable JavaMethodDescriptor hostMethodDescriptor;

  public InterfaceMethodData(String name) {
    this.name = name;
    this.dataFieldNames = new TList<>();
  }
}
