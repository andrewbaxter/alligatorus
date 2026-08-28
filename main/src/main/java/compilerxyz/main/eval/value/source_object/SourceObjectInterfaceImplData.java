package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.value.source_interface.InterfaceData;
import compilerxyz.prelude.util.TList;

public final class SourceObjectInterfaceImplData {
    public final InterfaceData interface_;
    public int implIdx;
    public final TList<SourceObjectInterfaceMethodImplData> methodImpls;

    public SourceObjectInterfaceImplData(InterfaceData interface_) {
        this.interface_ = interface_;
        this.methodImpls = new TList<>();
    }
}
