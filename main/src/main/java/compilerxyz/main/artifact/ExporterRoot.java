package compilerxyz.main.artifact;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.bool_.TypeBoolConst;
import compilerxyz.main.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.main.eval.value.byte_.TypeByteConst;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.eval.value.code.TypeCodeDynamic;
import compilerxyz.main.eval.value.double_.TypeDoubleConst;
import compilerxyz.main.eval.value.double_.TypeDoubleDynamic;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.float_.TypeFloatConst;
import compilerxyz.main.eval.value.float_.TypeFloatDynamic;
import compilerxyz.main.eval.value.int_.TypeIntConst;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.eval.value.list.TypeListConst;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.long_.TypeLongDynamic;
import compilerxyz.main.eval.value.map.TypeMapConst;
import compilerxyz.main.eval.value.null_.TypeNullConst;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.object_.TypeObjectConst;
import compilerxyz.main.eval.value.object_.TypeObjectDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.option.TypeOptionDynamic;
import compilerxyz.main.eval.value.option.TypeOptionVariantConstructorDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.short_.TypeShortConst;
import compilerxyz.main.eval.value.short_.TypeShortDynamic;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_interface.TypeTypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.main.eval.value.source_object_new.TypeTypeSourceObjectNewConst;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.main.eval.value.source_union_new.TypeTypeSourceUnionNewConst;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.ubyte_.TypeUbyteConst;
import compilerxyz.main.eval.value.ubyte_.TypeUbyteDynamic;
import compilerxyz.main.eval.value.uint_.TypeUintConst;
import compilerxyz.main.eval.value.uint_.TypeUintDynamic;
import compilerxyz.main.eval.value.ulong_.TypeUlongConst;
import compilerxyz.main.eval.value.ulong_.TypeUlongDynamic;
import compilerxyz.main.eval.value.unique.TypeUniqueDynamic;
import compilerxyz.main.eval.value.ushort_.TypeUshortConst;
import compilerxyz.main.eval.value.ushort_.TypeUshortDynamic;
import compilerxyz.main.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public enum ExporterRoot implements ArtifactExporter, Artifactable {
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
    LIST_CONST("list_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeListConst.DELEGATE;
        }
    },
    MAP_CONST("map_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeMapConst.DELEGATE;
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
    BYTE_CONST("byte_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeByteConst.DELEGATE;
        }
    },
    BYTE_DYNAMIC("byte_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeByteDynamic.DELEGATE;
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
    SHORT_CONST("short_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeShortConst.DELEGATE;
        }
    },
    SHORT_DYNAMIC("short_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeShortDynamic.DELEGATE;
        }
    },
    LONG_CONST("long_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeLongConst.DELEGATE;
        }
    },
    LONG_DYNAMIC("long_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeLongDynamic.DELEGATE;
        }
    },
    FLOAT_CONST("float_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeFloatConst.DELEGATE;
        }
    },
    FLOAT_DYNAMIC("float_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeFloatDynamic.DELEGATE;
        }
    },
    DOUBLE_CONST("double_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeDoubleConst.DELEGATE;
        }
    },
    DOUBLE_DYNAMIC("double_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeDoubleDynamic.DELEGATE;
        }
    },
    UBYTE_CONST("ubyte_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUbyteConst.DELEGATE;
        }
    },
    UBYTE_DYNAMIC("ubyte_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUbyteDynamic.DELEGATE;
        }
    },
    USHORT_CONST("ushort_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUshortConst.DELEGATE;
        }
    },
    USHORT_DYNAMIC("ushort_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUshortDynamic.DELEGATE;
        }
    },
    UINT_CONST("uint_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUintConst.DELEGATE;
        }
    },
    UINT_DYNAMIC("uint_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUintDynamic.DELEGATE;
        }
    },
    ULONG_CONST("ulong_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUlongConst.DELEGATE;
        }
    },
    ULONG_DYNAMIC("ulong_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUlongDynamic.DELEGATE;
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
            return TypeSourceObjectDynamic.DELEGATE;
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
            return TypeSourceUnionDynamic.DELEGATE;
        }
    },
    SOURCE_UNION_NEW_CONST("source_union_new_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeSourceUnionNewConst.DELEGATE;
        }
    },
    CODE_DYNAMIC("code_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeCodeDynamic.DELEGATE;
        }
    },
    OBJECT_CONST("object_const") {
        @Override
        ExporterRootDelegate delegate() {
            return ValueObjectConst.DELEGATE;
        }
    },
    TYPE_OBJECT_CONST("type_object_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeObjectConst.DELEGATE;
        }
    },
    SOURCE_OBJECT_TYPE_CONST("source_object_type_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeSourceObjectConst.TYPE_DELEGATE;
        }
    },
    SOURCE_UNION_TYPE_CONST("source_union_type_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeSourceUnionConst.TYPE_DELEGATE;
        }
    },
    SOURCE_INTERFACE_TYPE_DYNAMIC("source_interface_type_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeSourceInterfaceDynamic.TYPE_DELEGATE;
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

    abstract ExporterRootDelegate delegate();

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ROOT);
    }

    public static Opt<ExporterRoot> fromTag(String tag) {
        for (var v : values()) {
            if (v.tag.equals(tag)) return Opt.some(v);
        }
        return Opt.none();
    }

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Artifactable self) {
        var me = this;
        Function<JsonValue, ArtifactBody> makeBody = (data) -> {
            ArtifactBodyExporter exporter;
            if (me == ROOT) {
                exporter = new ArtifactBodyExporter.Root();
            } else {
                exporter = new ArtifactBodyExporter.Body(
                        new ArtifactBody(new JsonValueString(me.tag), new ArtifactBodyExporter.Root()));
            }
            return new ArtifactBody(data, exporter);
        };
        return delegate().exporterRootDelegateSelfExport(xx, loc, self, makeBody);
    }

    @Override
    public EResult<Artifactable> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return delegate().exporterRootDelegateSelfFromArtifactData(xx, loc, id, data);
    }

    private static final ExporterRootDelegate ROOT_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var er = mustCast(ExporterRoot.class, self);
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueString(er.tag))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            JsonValueString s;
            switch (data) {
                case JsonValueString js -> s = js;
                case JsonValueNull ignored -> {
                    return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
                }
                case JsonValueBoolean ignored -> {
                    return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
                }
                case JsonValueNumber ignored -> {
                    return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
                }
                case JsonValueArray ignored -> {
                    return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
                }
                case JsonValueObject ignored -> {
                    return EResult.err(loc, "ExporterRoot::ROOT::import: expected string tag, got " + data);
                }
            }
            return switch (fromTag(s.value())) {
                case Opt.Some<ExporterRoot> some -> EResult.ok(some.value());
                case Opt.None<ExporterRoot> n ->
                    EResult.err(loc, "ExporterRoot::ROOT::import: unknown tag " + s.value());
            };
        }
    };
}
