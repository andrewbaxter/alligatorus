package compilerxyz.main.artifact;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.builtin.App;
import compilerxyz.main.eval.builtin.Dir;
import compilerxyz.main.eval.value.array.TypeArrayConst;
import compilerxyz.main.eval.value.array.TypeArrayDynamic;
import compilerxyz.main.eval.value.bool.TypeBoolConst;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.byte_.TypeByteConst;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.eval.value.byte_.TypeByteDynamicBox;
import compilerxyz.main.eval.value.code.TypeCodeDynamic;
import compilerxyz.main.eval.value.double_.TypeDoubleConst;
import compilerxyz.main.eval.value.double_.TypeDoubleDynamic;
import compilerxyz.main.eval.value.double_.TypeDoubleDynamicBox;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.eresult.TypeEResultConst;
import compilerxyz.main.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.main.eval.value.eresult.TypeTypeEResult;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.float_.TypeFloatConst;
import compilerxyz.main.eval.value.float_.TypeFloatDynamic;
import compilerxyz.main.eval.value.float_.TypeFloatDynamicBox;
import compilerxyz.main.eval.value.int_.TypeIntConst;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.eval.value.int_.TypeIntDynamicBox;
import compilerxyz.main.eval.value.jvm_object.TypeJvmObjectConst;
import compilerxyz.main.eval.value.jvm_object.TypeJvmObjectDynamic;
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic;
import compilerxyz.main.eval.value.list.TypeListConst;
import compilerxyz.main.eval.value.list.TypeTypeList;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.long_.TypeLongDynamic;
import compilerxyz.main.eval.value.long_.TypeLongDynamicBox;
import compilerxyz.main.eval.value.map.TypeMapConst;
import compilerxyz.main.eval.value.map.TypeMapDynamic;
import compilerxyz.main.eval.value.map.TypeTypeMap;
import compilerxyz.main.eval.value.object.TypeObjectConst;
import compilerxyz.main.eval.value.object.TypeObjectDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.option.TypeOptionConst;
import compilerxyz.main.eval.value.option.TypeOptionDynamic;
import compilerxyz.main.eval.value.option.TypeOptionVariantConstructorDynamic;
import compilerxyz.main.eval.value.option.TypeTypeOption;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.short_.TypeShortConst;
import compilerxyz.main.eval.value.short_.TypeShortDynamic;
import compilerxyz.main.eval.value.short_.TypeShortDynamicBox;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_interface.TypeInterfaceMethodDynamic;
import compilerxyz.main.eval.value.source_interface.TypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_interface.TypeTypeSourceInterfaceDynamic;
import compilerxyz.main.eval.value.source_macro.TypeSourceMacroConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.main.eval.value.source_union_new.TypeSourceUnionNewDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.ubyte.TypeUbyteConst;
import compilerxyz.main.eval.value.ubyte.TypeUbyteDynamic;
import compilerxyz.main.eval.value.ubyte.TypeUbyteDynamicBox;
import compilerxyz.main.eval.value.uint.TypeUintConst;
import compilerxyz.main.eval.value.uint.TypeUintDynamic;
import compilerxyz.main.eval.value.uint.TypeUintDynamicBox;
import compilerxyz.main.eval.value.ulong.TypeUlongConst;
import compilerxyz.main.eval.value.ulong.TypeUlongDynamic;
import compilerxyz.main.eval.value.ulong.TypeUlongDynamicBox;
import compilerxyz.main.eval.value.unique.TypeUniqueConst;
import compilerxyz.main.eval.value.unique.TypeUniqueDynamic;
import compilerxyz.main.eval.value.ushort.TypeUshortConst;
import compilerxyz.main.eval.value.ushort.TypeUshortDynamic;
import compilerxyz.main.eval.value.ushort.TypeUshortDynamicBox;
import compilerxyz.main.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TMap;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ExporterRoot implements ArtifactExporter {
    BOOL_CONST("bool_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeBoolConst.DELEGATE;
        }
    },
    LIST_CONST("list_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeListConst.DELEGATE;
        }
    },
    LIST_CONST_TYPE("list_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeListConst.TYPE_DELEGATE;
        }
    },
    LIST_DYNAMIC("list_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeListBoxedDynamic.DELEGATE;
        }
    },
    MAP_CONST("map_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeMapConst.DELEGATE;
        }
    },
    MAP_CONST_TYPE("map_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeMapConst.TYPE_DELEGATE;
        }
    },
    MAP_DYNAMIC("map_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeMapDynamic.DELEGATE;
        }
    },
    BYTE_CONST("byte_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeByteConst.DELEGATE;
        }
    },
    INT_CONST("int_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeIntConst.DELEGATE;
        }
    },
    SHORT_CONST("short_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeShortConst.DELEGATE;
        }
    },
    LONG_CONST("long_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeLongConst.DELEGATE;
        }
    },
    FLOAT_CONST("float_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeFloatConst.DELEGATE;
        }
    },
    DOUBLE_CONST("double_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeDoubleConst.DELEGATE;
        }
    },
    UBYTE_CONST("ubyte_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUbyteConst.DELEGATE;
        }
    },
    USHORT_CONST("ushort_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUshortConst.DELEGATE;
        }
    },
    UINT_CONST("uint_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUintConst.DELEGATE;
        }
    },
    ULONG_CONST("ulong_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUlongConst.DELEGATE;
        }
    },
    OBJECT_DYNAMIC("object_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeObjectDynamic.DELEGATE;
        }
    },
    TYPE_TYPE_OPTION("type_type_option") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeOption.TYPE_DELEGATE;
        }
    },
    TYPE_TYPE_ERESULT("type_type_eresult") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeEResult.TYPE_DELEGATE;
        }
    },
    TYPE_TYPE_LIST("type_type_list") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeList.TYPE_DELEGATE;
        }
    },
    TYPE_TYPE_MAP("type_type_map") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeTypeMap.TYPE_DELEGATE;
        }
    },
    OPTION_DYNAMIC("option_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeOptionDynamic.DELEGATE;
        }
    },
    ARRAY_DYNAMIC("array_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeArrayDynamic.DELEGATE;
        }
    },
    ARRAY_CONST("array_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeArrayConst.DELEGATE;
        }
    },
    ARRAY_CONST_TYPE("array_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeArrayConst.TYPE_DELEGATE;
        }
    },
    OPTION_CONST("option_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeOptionConst.DELEGATE;
        }
    },
    OPTION_CONST_TYPE("option_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeOptionConst.TYPE_DELEGATE;
        }
    },
    ERESULT_DYNAMIC("eresult_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeEResultDynamic.DELEGATE;
        }
    },
    ERESULT_CONST("eresult_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeEResultConst.DELEGATE;
        }
    },
    ERESULT_CONST_TYPE("eresult_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeEResultConst.TYPE_DELEGATE;
        }
    },
    APP("app") {
        @Override
        ExporterRootDelegate delegate() {
            return App.DELEGATE;
        }
    },
    DIR("dir") {
        @Override
        ExporterRootDelegate delegate() {
            return Dir.DELEGATE;
        }
    },
    EXPR_ERR("expr_err") {
        @Override
        ExporterRootDelegate delegate() {
            return ExprErr.DELEGATE;
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
    RECORD_DATA("record_data") {
        @Override
        ExporterRootDelegate delegate() {
            return Record.DATA_DELEGATE;
        }
    },
    STRING_CONST("string_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeStringConst.DELEGATE;
        }
    },
    TUPLE("tuple") {
        @Override
        ExporterRootDelegate delegate() {
            return Tuple.DELEGATE;
        }
    },
    TUPLE_DATA("tuple_data") {
        @Override
        ExporterRootDelegate delegate() {
            return Tuple.DATA_DELEGATE;
        }
    },
    UNIQUE("unique") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUniqueDynamic.DELEGATE;
        }
    },
    UNIQUE_CONST("unique_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeUniqueConst.DELEGATE;
        }
    },
    SOURCE_FUNCTION_CONST("source_function_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceFunctionConst.DELEGATE;
        }
    },
    SOURCE_MACRO_CONST("source_macro_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceMacroConst.DELEGATE;
        }
    },
    SOURCE_INTERFACE_DYNAMIC("source_interface_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceInterfaceDynamic.DELEGATE;
        }
    },
    SOURCE_INTERFACE_METHOD("source_interface_method") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeInterfaceMethodDynamic.DELEGATE;
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
    SOURCE_OBJECT_CONST_TYPE("source_object_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceObjectConst.TYPE_DELEGATE;
        }
    },
    SOURCE_OBJECT_NEW("source_object_new") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceObjectNewDynamic.DELEGATE;
        }
    },
    SOURCE_UNION_CONST("source_union_const") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceUnionDynamic.DELEGATE;
        }
    },
    SOURCE_UNION_NEW("source_union_new") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeSourceUnionNewDynamic.DELEGATE;
        }
    },
    VALUE_EMPTY("value_empty") {
        @Override
        ExporterRootDelegate delegate() {
            return ValueEmpty.DELEGATE;
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
    BOOL_CONST_TYPE("bool_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeBoolConst::new);
        }
    },
    BOOL_DYNAMIC("bool_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeBoolDynamic::new);
        }
    },
    BYTE_CONST_TYPE("byte_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeByteConst::new);
        }
    },
    BYTE_DYNAMIC_BOX("byte_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeByteDynamicBox::new);
        }
    },
    BYTE_DYNAMIC("byte_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeByteDynamic::new);
        }
    },
    CODE_DYNAMIC("code_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeCodeDynamic::new);
        }
    },
    DOUBLE_CONST_TYPE("double_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeDoubleConst::new);
        }
    },
    DOUBLE_DYNAMIC_BOX("double_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeDoubleDynamicBox::new);
        }
    },
    DOUBLE_DYNAMIC("double_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeDoubleDynamic::new);
        }
    },
    EXPR("expr") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeExpr::new);
        }
    },
    EXPR_VALUE("expr_value") {
        @Override
        ExporterRootDelegate delegate() {
            return TypeExpr.DELEGATE;
        }
    },
    FLOAT_CONST_TYPE("float_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeFloatConst::new);
        }
    },
    FLOAT_DYNAMIC_BOX("float_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeFloatDynamicBox::new);
        }
    },
    FLOAT_DYNAMIC("float_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeFloatDynamic::new);
        }
    },
    INT_CONST_TYPE("int_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeIntConst::new);
        }
    },
    INT_DYNAMIC_BOX("int_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeIntDynamicBox::new);
        }
    },
    INT_DYNAMIC("int_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeIntDynamic::new);
        }
    },
    LONG_CONST_TYPE("long_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeLongConst::new);
        }
    },
    LONG_DYNAMIC_BOX("long_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeLongDynamicBox::new);
        }
    },
    LONG_DYNAMIC("long_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeLongDynamic::new);
        }
    },
    JVM_OBJECT_CONST_TYPE("jvm_object_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeJvmObjectConst::new);
        }
    },
    JVM_OBJECT_DYNAMIC("jvm_object_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeJvmObjectDynamic::new);
        }
    },
    SHORT_CONST_TYPE("short_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeShortConst::new);
        }
    },
    SHORT_DYNAMIC_BOX("short_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeShortDynamicBox::new);
        }
    },
    SHORT_DYNAMIC("short_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeShortDynamic::new);
        }
    },
    STRING_CONST_TYPE("string_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeStringConst::new);
        }
    },
    STRING_DYNAMIC("string_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeStringDynamic::new);
        }
    },
    UBYTE_CONST_TYPE("ubyte_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUbyteConst::new);
        }
    },
    UBYTE_DYNAMIC_BOX("ubyte_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUbyteDynamicBox::new);
        }
    },
    UBYTE_DYNAMIC("ubyte_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUbyteDynamic::new);
        }
    },
    UINT_CONST_TYPE("uint_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUintConst::new);
        }
    },
    UINT_DYNAMIC_BOX("uint_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUintDynamicBox::new);
        }
    },
    UINT_DYNAMIC("uint_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUintDynamic::new);
        }
    },
    ULONG_CONST_TYPE("ulong_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUlongConst::new);
        }
    },
    ULONG_DYNAMIC_BOX("ulong_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUlongDynamicBox::new);
        }
    },
    ULONG_DYNAMIC("ulong_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUlongDynamic::new);
        }
    },
    UNIQUE_TYPE("unique_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUniqueDynamic::new);
        }
    },
    UNIQUE_CONST_TYPE("unique_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUniqueConst::new);
        }
    },
    USHORT_CONST_TYPE("ushort_const_type") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUshortConst::new);
        }
    },
    USHORT_DYNAMIC_BOX("ushort_dynamic_box") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUshortDynamicBox::new);
        }
    },
    USHORT_DYNAMIC("ushort_dynamic") {
        @Override
        ExporterRootDelegate delegate() {
            return nullaryType(TypeUshortDynamic::new);
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

    public String exporterRootTag() {
        return tag;
    }

    private static final IMap<String, ExporterRoot> BY_TAG;

    static {
        var byTag = new TMap<String, ExporterRoot>();
        for (var v : values()) {
            if (byTag.getOpt(v.tag).optIsSome()) {
                throw new IllegalStateException("ExporterRoot: duplicate tag " + v.tag);
            }
            byTag.put(v.tag, v);
        }
        BY_TAG = IMap.from(byTag);
    }

    public static Opt<ExporterRoot> fromTag(String tag) {
        return BY_TAG.getOpt(tag);
    }

    public ArtifactBody body(JsonValue data) {
        ArtifactBodyExporter exporter;
        if (this == ROOT) {
            exporter = new ArtifactBodyExporter.Root();
        } else {
            exporter = new ArtifactBodyExporter.Body(
                    new ArtifactBody(new JsonValueString(tag), new ArtifactBodyExporter.Root()));
        }
        return new ArtifactBody(data, exporter);
    }

    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        return delegate().exporterRootDelegateSelfExport(xx, loc, self, this::body);
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return delegate().exporterRootDelegateSelfFromArtifactData(xx, loc, id, data);
    }

    private static ExporterRootDelegate nullaryType(Supplier<Type> make) {
        return new ExporterRootDelegate() {
            @Override
            public EResult<Artifact> exporterRootDelegateSelfExport(
                    ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
                return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
            }

            @Override
            public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                    ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
                return switch (data) {
                    case JsonValueNull ignored -> EResult.ok(make.get());
                    case JsonValueBoolean ignored -> EResult.err(loc, "nullary type: expected null, got " + data);
                    case JsonValueNumber ignored -> EResult.err(loc, "nullary type: expected null, got " + data);
                    case JsonValueString ignored -> EResult.err(loc, "nullary type: expected null, got " + data);
                    case JsonValueArray ignored -> EResult.err(loc, "nullary type: expected null, got " + data);
                    case JsonValueObject ignored -> EResult.err(loc, "nullary type: expected null, got " + data);
                };
            }
        };
    }

    private static final ExporterRootDelegate ROOT_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var er = mustCast(ExporterRoot.class, self);
            return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueString(er.tag))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
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
