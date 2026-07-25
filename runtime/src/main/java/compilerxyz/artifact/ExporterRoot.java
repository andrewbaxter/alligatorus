package compilerxyz.artifact;

import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.value.bool_.TypeBoolConst;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.expr.TypeExpr;
import compilerxyz.eval.value.list.TypeListDynamic;
import compilerxyz.eval.value.map.TypeMapDynamic;
import compilerxyz.eval.value.null_.TypeNullConst;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.int_.TypeIntConst;
import compilerxyz.eval.value.int_.TypeIntDynamic;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.option.TypeOptionDynamic;
import compilerxyz.eval.value.option.TypeOptionVariantConstructorDynamic;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.eval.value.source_object_new.TypeTypeSourceObjectNewConst;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.eval.value.source_union_new.TypeTypeSourceUnionNewConst;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.unique.TypeUniqueDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import java.util.function.Function;
import static compilerxyz.util.MustCast.mustCast;

public enum ExporterRoot implements ArtifactExporter, ExporterAny {
  BOOL_CONST("bool_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeBoolConst.DELEGATE;
    }
  },
  BOOL_DYNAMIC("bool_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeBoolDynamic.DELEGATE;
    }
  },
  EXPR("expr") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeExpr.DELEGATE;
    }
  },
  LIST_DYNAMIC("list_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeListDynamic.DELEGATE;
    }
  },
  MAP_DYNAMIC("map_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeMapDynamic.DELEGATE;
    }
  },
  NULL_CONST("null_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeNullConst.DELEGATE;
    }
  },
  NULL_DYNAMIC("null_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeNullDynamic.DELEGATE;
    }
  },
  INT_CONST("int_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeIntConst.DELEGATE;
    }
  },
  INT_DYNAMIC("int_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeIntDynamic.DELEGATE;
    }
  },
  OBJECT_DYNAMIC("object_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeObjectDynamic.DELEGATE;
    }
  },
  OPTION_DYNAMIC("option_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeOptionDynamic.DELEGATE;
    }
  },
  OPTION_VARIANT_CONSTRUCTOR_DYNAMIC("option_variant_constructor_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeOptionVariantConstructorDynamic.DELEGATE;
    }
  },
  RECORD("record") {
    @Override
    ExporterRootDelegate delegate() {
      return Record.DELEGATE;
    }
  },
  STRING_CONST("string_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeStringConst.DELEGATE;
    }
  },
  STRING_DYNAMIC("string_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeStringDynamic.DELEGATE;
    }
  },
  TUPLE("tuple") {
    @Override
    ExporterRootDelegate delegate() {
      return Tuple.DELEGATE;
    }
  },
  UNIQUE("unique") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeUniqueDynamic.DELEGATE;
    }
  },
  SOURCE_FUNCTION_CONST("source_function_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeSourceFunctionConst.DELEGATE;
    }
  },
  SOURCE_INTERFACE_DYNAMIC("source_interface_dynamic") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeSourceInterfaceDynamic.DELEGATE;
    }
  },
  SOURCE_OBJECT_CONST("source_object_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeTypeSourceObjectConst.DELEGATE;
    }
  },
  SOURCE_OBJECT_VALUE_CONST("source_object_value_const") {
    @Override
    ExporterRootDelegate delegate() {
      return ValueSourceObjectConst.DELEGATE;
    }
  },
  SOURCE_OBJECT_NEW_CONST("source_object_new_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeTypeSourceObjectNewConst.DELEGATE;
    }
  },
  SOURCE_UNION_CONST("source_union_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeTypeSourceUnionConst.DELEGATE;
    }
  },
  SOURCE_UNION_NEW_CONST("source_union_new_const") {
    @Override
    ExporterRootDelegate delegate() {
      return TypeTypeSourceUnionNewConst.DELEGATE;
    }
  },
  DEFINITION_SET("definition_set") {
    @Override
    ExporterRootDelegate delegate() {
      return DefinitionSet.DELEGATE;
    }
  },
  ROOT("root") {
    @Override
    ExporterRootDelegate delegate() {
      return ROOT_DELEGATE;
    }
  };

  private final String tag;

  ExporterRoot(String tag) {
    this.tag = tag;
  }

  public String tag() {
    return tag;
  }

  abstract ExporterRootDelegate delegate();

  public static Opt<ExporterRoot> fromTag(String tag) {
    for (var v : values()) {
      if (v.tag.equals(tag)) return Opt.some(v);
    }
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ExporterRoot is not a Type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    throw new UnsupportedOperationException("ExporterRoot is not a Value");
  }

  @Override
  public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self) {
    var me = this;
    Function<JsonValue, ArtifactBody> makeBody =
        (data) -> {
          ArtifactBodyExporter exporter;
          if (me == ROOT) {
            exporter = new ArtifactBodyExporter.Root();
          } else {
            exporter =
                new ArtifactBodyExporter.Body(
                    new ArtifactBody(new JsonValueString(me.tag), new ArtifactBodyExporter.Root()));
          }
          return new ArtifactBody(data, exporter);
        };
    return delegate().exporterRootDelegateSelfExport(xx, loc, self, makeBody);
  }

  @Override
  public EResult<ExporterAny> artifactExporterImportFromData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
    return delegate().exporterRootDelegateSelfFromArtifactData(xx, loc, id, data);
  }

  private static final ExporterRootDelegate ROOT_DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          var er = mustCast(ExporterRoot.class, self);
          return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueString(er.tag))));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(data instanceof JsonValueString s)) {
            return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
          }
          return switch (fromTag(s.value())) {
            case Opt.Some<ExporterRoot> some -> EResult.ok(some.value());
            case Opt.None<ExporterRoot> n ->
                EResult.err(loc, "ExporterRoot::ROOT::import: unknown tag " + s.value());
          };
        }
      };
}
