package compilerxyz.eval;

import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.code_value_extern.TypeCodeValueExternDynamic;
import compilerxyz.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.eval.value.function.TypeMethodDynamic;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.option.TypeOptionDynamic;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.eval.value.unique.TypeUnique;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

@compilerxyz.annotations.BuiltinTrait(
    uniqueId = 2002L,
    scopeId = 3002)
@compilerxyz.annotations.BuiltinPath("eval.type")
@compilerxyz.annotations.BuiltinTrivialIdentityAspect(6006L)
@compilerxyz.annotations.BuiltinTypeId(7012L)
public interface Type extends HasTypeId, compilerxyz.artifact.Artifactable {

  /**
   * Default: a Type exports itself as an artifact by asking its own AspectArtifact for its
   * exporter and delegating. Requires {@code this} to implement both AspectArtifact and
   * ExporterAny; Types that don't need to be exportable can omit those interfaces and this
   * default will fail at call time.
   */
  @Override
  default EResult<compilerxyz.artifact.Artifact> exportArtifact(ExprContext xx, QExprId loc) {
    if (!(this instanceof compilerxyz.gen.AspectArtifact aa)) {
      return EResult.err(loc, typeDesc() + " does not implement AspectArtifact");
    }
    if (!(this instanceof compilerxyz.artifact.ExporterAny ea)) {
      return EResult.err(loc, typeDesc() + " does not implement ExporterAny");
    }
    return aa.aspectArtifactExporter().artifactExporterExport(xx, loc, ea);
  }

  static Type dynamicType() {
    return TypeDynamicHolder.INSTANCE;
  }

  final class TypeDynamicHolder {
    static final TypeObjectDynamic INSTANCE = build();

    private TypeDynamicHolder() {}

    private static TypeObjectDynamic build() {
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(GenTrivialIdentityAspects.TYPE_IDENTITY);
      var methods = new TList<ObjectMethod>();
      var instance = new TypeObjectDynamic(
          GenTypeIds.TYPE_TYPE_ID,
          new compilerxyz.eval.value.object_.TypeObjectMeta("type", trivialIdentityAspects, new TList<TypeObjectCastRule>(), new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none()),
          JavaInternalName.fromClass(Type.class),
          JavaDataDescriptor.fromClass(Type.class),
          Type.class,
          new compilerxyz.eval.TypeId.Composite(compilerxyz.gen.GenTypeIds.OBJECT_CONST_OF_TYPE_TYPE_ID, compilerxyz.util.TList.of(GenTypeIds.TYPE_TYPE_ID)),
          new TList<ObjectDataField>(),
          methods,
          ObjectDropKind.DYNAMIC_LIGHT,
          Opt.<JavaInternalName>none());

      var typeIn = JavaInternalName.fromClass(Type.class);
      var exprCtxDesc = JavaDataDescriptor.fromClass(ExprContext.class);
      var qexprIdDesc = JavaDataDescriptor.fromClass(QExprId.class);
      var typeDesc_ = JavaDataDescriptor.fromClass(Type.class);
      var cvDesc = JavaDataDescriptor.fromClass(CodeValue.class);
      var eresultDesc = JavaDataDescriptor.fromClass(EResult.class);
      var boolDesc = JavaDataDescriptor.DESC_BOOL;
      var stringDesc = JavaDataDescriptor.fromClass(String.class);
      var uniqueDesc = JavaDataDescriptor.fromClass(Unique.class);
      var optDesc = JavaDataDescriptor.fromClass(Opt.class);

      // typeCanCastFrom(ExprContext, QExprId, Type) -> boolean
      {
        var fields = new TOrderedMap<ConstKey, RecordField>();
        fields.put(new ConstKey.Str("xx"), new RecordField.Dynamic(ExprContext.dynamicType()));
        fields.put(new ConstKey.Str("loc"), new RecordField.Dynamic(QExprId.dynamicType()));
        fields.put(new ConstKey.Str("source_type"), new RecordField.Dynamic(instance));
        var argRecord = new Record(Opt.none(), fields);
        methods.add(new ObjectMethod("can_cast_from", new TypeMethodDynamic(
            instance,
            argRecord,
            new TypeBoolDynamic(),
            typeIn,
            "typeCanCastFrom",
            JavaMethodDescriptor.fromParts(boolDesc, exprCtxDesc, qexprIdDesc, typeDesc_),
            true,
            false)));
      }

      // typeCastFrom(ExprContext, QExprId, CodeValue) -> EResult<CodeValue>
      {
        var fields = new TOrderedMap<ConstKey, RecordField>();
        fields.put(new ConstKey.Str("xx"), new RecordField.Dynamic(ExprContext.dynamicType()));
        fields.put(new ConstKey.Str("loc"), new RecordField.Dynamic(QExprId.dynamicType()));
        fields.put(new ConstKey.Str("cv"), new RecordField.Dynamic(new TypeCodeValueExternDynamic()));
        var argRecord = new Record(Opt.none(), fields);
        methods.add(new ObjectMethod("cast_from", new TypeMethodDynamic(
            instance,
            argRecord,
            new TypeEResultDynamic(new TypeCodeValueExternDynamic()),
            typeIn,
            "typeCastFrom",
            JavaMethodDescriptor.fromParts(eresultDesc, exprCtxDesc, qexprIdDesc, cvDesc),
            true,
            false)));
      }

      // typeCanCastTo(ExprContext, QExprId, Type) -> boolean
      {
        var fields = new TOrderedMap<ConstKey, RecordField>();
        fields.put(new ConstKey.Str("xx"), new RecordField.Dynamic(ExprContext.dynamicType()));
        fields.put(new ConstKey.Str("loc"), new RecordField.Dynamic(QExprId.dynamicType()));
        fields.put(new ConstKey.Str("dest_type"), new RecordField.Dynamic(instance));
        var argRecord = new Record(Opt.none(), fields);
        methods.add(new ObjectMethod("can_cast_to", new TypeMethodDynamic(
            instance,
            argRecord,
            new TypeBoolDynamic(),
            typeIn,
            "typeCanCastTo",
            JavaMethodDescriptor.fromParts(boolDesc, exprCtxDesc, qexprIdDesc, typeDesc_),
            true,
            false)));
      }

      // typeCastTo(ExprContext, QExprId, CodeValue, Type) -> EResult<CodeValue>
      {
        var fields = new TOrderedMap<ConstKey, RecordField>();
        fields.put(new ConstKey.Str("xx"), new RecordField.Dynamic(ExprContext.dynamicType()));
        fields.put(new ConstKey.Str("loc"), new RecordField.Dynamic(QExprId.dynamicType()));
        fields.put(new ConstKey.Str("cv"), new RecordField.Dynamic(new TypeCodeValueExternDynamic()));
        fields.put(new ConstKey.Str("dest_type"), new RecordField.Dynamic(instance));
        var argRecord = new Record(Opt.none(), fields);
        methods.add(new ObjectMethod("cast_to", new TypeMethodDynamic(
            instance,
            argRecord,
            new TypeEResultDynamic(new TypeCodeValueExternDynamic()),
            typeIn,
            "typeCastTo",
            JavaMethodDescriptor.fromParts(eresultDesc, exprCtxDesc, qexprIdDesc, cvDesc, typeDesc_),
            true,
            false)));
      }

      // typeDesc() -> String
      {
        var argTuple = new Tuple(Opt.none(), TList.<TupleElement>of());
        methods.add(new ObjectMethod("desc", new TypeMethodDynamic(
            instance,
            argTuple,
            new TypeStringDynamic(),
            typeIn,
            "typeDesc",
            JavaMethodDescriptor.fromParts(stringDesc),
            true,
            false)));
      }

      // typeAspect(Unique) -> Opt<Aspect>; xx from ExprContext static field
      methods.add(new ObjectMethod("aspect", new TypeMethodDynamic(
          instance,
          new TypeUnique(),
          new TypeOptionDynamic(Aspect.dynamicType()),
          typeIn,
          "typeAspect",
          JavaMethodDescriptor.fromParts(optDesc, exprCtxDesc, uniqueDesc),
          true,
          true)));

      return instance;
    }
  }

  @compilerxyz.annotations.SourceExposed
  boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

  @compilerxyz.annotations.SourceExposed
  EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);

  @compilerxyz.annotations.SourceExposed
  boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

  @compilerxyz.annotations.SourceExposed
  EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

  @compilerxyz.annotations.SourceExposed
  String typeDesc();

  @compilerxyz.annotations.SourceExposed
  Opt<Aspect> typeAspect(ExprContext xx, Unique key);
}
