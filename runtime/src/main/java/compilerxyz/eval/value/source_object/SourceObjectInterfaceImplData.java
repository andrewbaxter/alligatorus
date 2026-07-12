package compilerxyz.eval.value.source_object;

import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.util.TList;

public final class SourceObjectInterfaceImplData {
  public final InterfaceData interface_;
  public int implIdx;
  public final TList<SourceObjectInterfaceMethodImplData> methodImpls;

  public SourceObjectInterfaceImplData(InterfaceData interface_) {
    this.interface_ = interface_;
    this.methodImpls = new TList<>();
  }
}
