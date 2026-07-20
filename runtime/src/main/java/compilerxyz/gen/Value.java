package compilerxyz.gen;

import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@compilerxyz.annotations.BuiltinTrait(
    uniqueId = 2001L,
    scopeId = 3001)
@compilerxyz.annotations.BuiltinPath("eval.value")
@compilerxyz.annotations.BuiltinTypeId(7109L)
public interface Value extends compilerxyz.artifact.Artifactable {
    @compilerxyz.annotations.SourceExposed
    Type valueType();

    @Override
    default compilerxyz.eval.EResult<compilerxyz.artifact.Artifact> exportArtifact(
        compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc) {
      var asp = compilerxyz.gen.GenAspectArtifact.get(xx, loc, valueType());
      if (asp.hasErrors()) return asp.reerr();
      var exporter = asp.requireValue("Value::exportArtifact aspect").aspectArtifactExporter();
      return exporter.artifactExporterExport(xx, loc,
          compilerxyz.artifact.ArtifactSupport.valueAsExporterAny(this));
    }

    static Type dynamicType() {
        return DynamicType.INSTANCE;
    }

    final class DynamicType {
        static final TypeObjectDynamic INSTANCE = build();

        private DynamicType() {}

        private static TypeObjectDynamic build() {
            var trivialIdentityAspects = new TList<Unique>();
            trivialIdentityAspects.add(GenBuiltinInterfaces.builtinValueIfaceIdentity());
            return new TypeObjectDynamic(
                    GenTypeIds.VALUE_TYPE_ID,
                    new TypeObjectMeta("value", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
                    JavaInternalName.fromClass(Value.class),
                    JavaDataDescriptor.fromClass(Value.class),
                    Value.class,
                    new TypeId.Composite(GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID, TList.of(GenTypeIds.VALUE_TYPE_ID)),
                    new TList<ObjectDataField>(),
                    new TList<ObjectMethod>(),
                    ObjectDropKind.DYNAMIC_LIGHT,
                    Opt.<JavaInternalName>none());
        }
    }
}
