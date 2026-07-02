package compilerxyz.eval.value.source_object;

import compilerxyz.eval.value.source_interface.SourceInterfaceData;
import compilerxyz.util.TList;

public final class SourceObjectInterfaceImplData {
  public final SourceInterfaceData interface_;
  public final TList<SourceObjectInterfaceMethodImplData> methodImpls;

  public SourceObjectInterfaceImplData(SourceInterfaceData interface_) {
    this.interface_ = interface_;
    this.methodImpls = new TList<>();
  }
}
