package compilerxyz.eval.value.source_exports;

import compilerxyz.eval.DefSetId;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.eval.value.source_object.SourceObjectData;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class SourceExportsData {
  public @Nullable DefSetId setId;
  public final TList<SourceFunctionData> functions;
  public final TList<SourceObjectData> objects;

  public SourceExportsData(@Nullable DefSetId setId) {
    this.setId = setId;
    this.functions = new TList<>();
    this.objects = new TList<>();
  }

  public SourceExportsData(
      @Nullable DefSetId setId,
      TList<SourceFunctionData> functions,
      TList<SourceObjectData> objects) {
    this.setId = setId;
    this.functions = functions;
    this.objects = objects;
  }
}
