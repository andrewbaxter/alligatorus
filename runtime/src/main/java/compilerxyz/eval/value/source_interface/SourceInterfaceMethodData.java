package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class SourceInterfaceMethodData {
  public final String name;
  public final String fieldName;
  public final TList<String> dataFieldNames;
  public @Nullable Type argType;
  public @Nullable Type argRecordType;
  public @Nullable Type retType;

  public SourceInterfaceMethodData(String name, String fieldName) {
    this.name = name;
    this.fieldName = fieldName;
    this.dataFieldNames = new TList<>();
  }
}
