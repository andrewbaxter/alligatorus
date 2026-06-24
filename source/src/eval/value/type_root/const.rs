use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactBodyExporter,
            ArtifactExporter,
            ArtifactIdSource,
            ExporterAny,
        },
        eval::value::{
            record::Record,
            tuple::Tuple,
            type_bool::r#const::TypeBoolConst,
            type_null::r#const::TypeNullConst,
            type_number::r#const::TypeNumberConst,
            type_source_data_type::r#const::TypeSourceDataTypeConst,
            type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
            type_source_exports::r#const::TypeSourceExportsLiftedConst,
            type_source_function::r#const::TypeSourceFunctionConst,
            type_source_interface::r#const::TypeSourceInterfaceConst,
            type_source_tagged_union::r#const::TypeSourceTaggedUnionConst,
            type_source_tagged_union_constructor::r#const::TypeSourceTaggedUnionConstructorConst,
            type_string::r#const::TypeStringConst,
            value_unique::TypeUnique,
        },
        langhelp::{
            EResult,
            ExprContext,
            ExprErr,
            ExprSubErr,
            GlobalContext,
            QExprId,
            Type,
            Value,
        },
        r#gen::{
            AspectArtifact,
            aspect_artifact_aspect_id,
        },
    },
    serde::{
        Deserialize,
        Serialize,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) trait ExporterRootDelegate {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>>;
    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact>;
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub enum ExporterRoot {
    BoolConst,
    DefinitionSet,
    NullConst,
    NumberConst,
    Pull,
    Record,
    Root,
    SourceDataTypeConst,
    SourceDataTypeConstructorConst,
    SourceExportsLiftedConst,
    SourceFunctionConst,
    SourceInterfaceConst,
    SourceTaggedUnionConst,
    SourceTaggedUnionConstructorConst,
    StringConst,
    Tuple,
    Unique,
}

impl ExporterRoot {
    fn self_to_serial_exporter(self) -> ArtifactBodyExporter {
        match self {
            ExporterRoot::Root => ArtifactBodyExporter::Root,
            _ => ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                data: serde_json::to_value(self).unwrap(),
                exporter: ExporterRoot::Root.self_to_serial_exporter(),
            })),
        }
    }
}

impl ArtifactExporter for ExporterRoot {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let id_ref = id.as_ref();
        let res: EResult<Rc<dyn crate::artifact::ExporterAny>> = match self {
            ExporterRoot::BoolConst => TypeBoolConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::DefinitionSet => crate::eval::definition_set::definition_set_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::NullConst => TypeNullConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NumberConst => TypeNumberConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Pull => crate::eval::value::type_pull::r#const::TypePull::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceDataTypeConst => TypeSourceDataTypeConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceTaggedUnionConst => TypeSourceTaggedUnionConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceTaggedUnionConstructorConst => TypeSourceTaggedUnionConstructorConst
            ::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Record => Record::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Tuple => Tuple::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Unique => TypeUnique::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Root => EResult::from(
                loc,
                serde_json::from_value::<TypeRoot>(
                    data,
                ).map(|r| Rc::new(r) as Rc<dyn crate::artifact::ExporterAny>),
            ),
        };
        let mut errors: Option<Vec<ExprErr>> = None;
        if let Some(errors1) = res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(data) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors: None,
            v: Some(data),
        };
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
    ) -> EResult<Artifact> {
        let me = *self;
        let make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody> = Box::new(move |data| ArtifactBody {
            data,
            exporter: me.self_to_serial_exporter(),
        });
        return match self {
            ExporterRoot::BoolConst => TypeBoolConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::DefinitionSet => crate::eval::definition_set::definition_set_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::NullConst => TypeNullConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::NumberConst => TypeNumberConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::Pull => crate::eval::value::type_pull::r#const::TypePull::self_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::SourceDataTypeConst => TypeSourceDataTypeConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::SourceTaggedUnionConst => TypeSourceTaggedUnionConst::self_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::SourceTaggedUnionConstructorConst => TypeSourceTaggedUnionConstructorConst::self_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_to_serial(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::Record => Record::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::Tuple => Tuple::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::Unique => TypeUnique::self_to_serial(xx, loc, self_, make_body),
            ExporterRoot::Root => EResult::ok(Artifact::Body(make_body(serde_json::to_value(me).unwrap()))),
        };
    }
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub enum TypeRoot {
    AnyScopeEntryDynamic,
    AnyTargetDynamic,
    AspectDynamic,
    AspectLookupFnType,
    BoolConst,
    BoolDynamic,
    CodeDynCastFn,
    CodeDynHandleDynamic,
    CodeEnumDynamic,
    CodeStructDynamic,
    CodeValueExternDynamic,
    ConstkeyDynamic,
    Expr,
    ExprContextPushDynamic,
    ExprStagedPlaceholder,
    FunctionAspectDynamic,
    FunctionCastValueDynamic,
    FunctionDynamic,
    ListConstructorConst,
    ListDynamic,
    ListNewDynamic,
    MapConstructorConst,
    MapDynamic,
    MapGetFnDynamic,
    MapNewDynamic,
    NullConst,
    NullDynamic,
    NumberConst,
    NumberDynamic,
    OptionConstructorConst,
    OptionDynamic,
    OptionVariantConstructorDynamic,
    Pull,
    PullAspect,
    Root,
    SourceDataTypeConstructorConst,
    SourceDataTypeConstructorDynamic,
    SourceDataTypeDynamic,
    SourceDataTypeRefDynamic,
    SourceExportsLiftedConst,
    SourceFunctionConst,
    SourceFunctionDynamic,
    SourceInterfaceConst,
    SourceInterfaceDynamic,
    SourceTaggedUnionConst,
    SourceTaggedUnionConstructorConst,
    SourceTaggedUnionConstructorDynamic,
    SourceTaggedUnionDynamic,
    SourceTaggedUnionRefDynamic,
    StringConst,
    StringDynamic,
    TypeRef,
    TypeSourceDataTypeConstructorDynamic,
    TypeSourceDataTypeDynamic,
    TypeSourceFunctionDynamic,
    Unique,
    ValueDynamic,
}

impl TypeRoot {
    fn exporter_for(&self) -> Option<ExporterRoot> {
        return match self {
            TypeRoot::BoolConst => Some(ExporterRoot::BoolConst),
            TypeRoot::NullConst => Some(ExporterRoot::NullConst),
            TypeRoot::NumberConst => Some(ExporterRoot::NumberConst),
            TypeRoot::Pull => Some(ExporterRoot::Pull),
            TypeRoot::Root => Some(ExporterRoot::Root),
            TypeRoot::SourceDataTypeConstructorConst => Some(ExporterRoot::SourceDataTypeConstructorConst),
            TypeRoot::SourceExportsLiftedConst => Some(ExporterRoot::SourceExportsLiftedConst),
            TypeRoot::SourceFunctionConst => Some(ExporterRoot::SourceFunctionConst),
            TypeRoot::SourceInterfaceConst => Some(ExporterRoot::SourceInterfaceConst),
            TypeRoot::SourceTaggedUnionConst => Some(ExporterRoot::SourceTaggedUnionConst),
            TypeRoot::SourceTaggedUnionConstructorConst => Some(ExporterRoot::SourceTaggedUnionConstructorConst),
            TypeRoot::StringConst => Some(ExporterRoot::StringConst),
            TypeRoot::Unique => Some(ExporterRoot::Unique),
            _ => None,
        };
    }
}

impl AspectArtifact for TypeRoot {
    fn exporter(&self) -> Rc<dyn ArtifactExporter> {
        return Rc::new(self.exporter_for().unwrap());
    }
}

impl crate::langhelp::Aspect for TypeRoot { }

impl ExporterAny for TypeRoot {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeRoot {
    fn as_value(&self) -> Rc<dyn Value> {
        return Rc::new(*self);
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_artifact_aspect_id() && self.exporter_for().is_some() {
            return Some(Box::new(*self));
        }
        return None;
    }

    fn desc(&self) -> String {
        return format!("{} type", match self {
            TypeRoot::AnyScopeEntryDynamic => "Any scope entry type (dynamic)",
            TypeRoot::AnyTargetDynamic => "Any target type (dynamic)",
            TypeRoot::AspectDynamic => "Aspect type (dynamic)",
            TypeRoot::AspectLookupFnType => "Aspect lookup fn type",
            TypeRoot::BoolConst => "Bool type (const)",
            TypeRoot::BoolDynamic => "Bool type (dynamic)",
            TypeRoot::CodeDynCastFn => "Code dyn cast fn type",
            TypeRoot::CodeDynHandleDynamic => "Code dyn handle type (dynamic)",
            TypeRoot::CodeEnumDynamic => "Code enum type (dynamic)",
            TypeRoot::CodeStructDynamic => "Code struct type (dynamic)",
            TypeRoot::CodeValueExternDynamic => "Code value extern type (dynamic)",
            TypeRoot::ConstkeyDynamic => "Constkey type (dynamic)",
            TypeRoot::Expr => "Expr type",
            TypeRoot::ExprContextPushDynamic => "Expr context push type (dynamic)",
            TypeRoot::ExprStagedPlaceholder => "Expr staged placeholder type",
            TypeRoot::FunctionAspectDynamic => "Function aspect type (dynamic)",
            TypeRoot::FunctionCastValueDynamic => "Function cast value type (dynamic)",
            TypeRoot::FunctionDynamic => "Function type (dynamic)",
            TypeRoot::ListConstructorConst => "List constructor type (const)",
            TypeRoot::ListDynamic => "List type (dynamic)",
            TypeRoot::ListNewDynamic => "List new type (dynamic)",
            TypeRoot::MapConstructorConst => "Map constructor type (const)",
            TypeRoot::MapDynamic => "Map type (dynamic)",
            TypeRoot::MapGetFnDynamic => "Map get fn type (dynamic)",
            TypeRoot::MapNewDynamic => "Map new type (dynamic)",
            TypeRoot::NullConst => "Null type (const)",
            TypeRoot::NullDynamic => "Null type (dynamic)",
            TypeRoot::NumberConst => "Number type (const)",
            TypeRoot::NumberDynamic => "Number type (dynamic)",
            TypeRoot::OptionConstructorConst => "Option constructor type (const)",
            TypeRoot::OptionDynamic => "Option type (dynamic)",
            TypeRoot::OptionVariantConstructorDynamic => "Option variant constructor type (dynamic)",
            TypeRoot::Pull => "Pull type",
            TypeRoot::PullAspect => "Pull aspect type",
            TypeRoot::SourceDataTypeConstructorConst => "Source data type constructor (const)",
            TypeRoot::SourceDataTypeConstructorDynamic => "Source data type constructor (dynamic)",
            TypeRoot::SourceDataTypeDynamic => "Source data type (dynamic)",
            TypeRoot::SourceDataTypeRefDynamic => "Source data type ref (dynamic)",
            TypeRoot::SourceExportsLiftedConst => "Source exports lifted type (const)",
            TypeRoot::SourceFunctionConst => "Source function type (const)",
            TypeRoot::SourceFunctionDynamic => "Source function type (dynamic)",
            TypeRoot::SourceInterfaceConst => "Source interface type (const)",
            TypeRoot::SourceInterfaceDynamic => "Source interface type (dynamic)",
            TypeRoot::SourceTaggedUnionConst => "Source tagged union type (const)",
            TypeRoot::SourceTaggedUnionConstructorConst => "Source tagged union constructor (const)",
            TypeRoot::SourceTaggedUnionConstructorDynamic => "Source tagged union constructor (dynamic)",
            TypeRoot::SourceTaggedUnionDynamic => "Source tagged union type (dynamic)",
            TypeRoot::SourceTaggedUnionRefDynamic => "Source tagged union ref (dynamic)",
            TypeRoot::StringConst => "String type (const)",
            TypeRoot::StringDynamic => "String type (dynamic)",
            TypeRoot::TypeRef => "Type ref",
            TypeRoot::TypeSourceDataTypeConstructorDynamic => "Type source data type constructor (dynamic)",
            TypeRoot::TypeSourceDataTypeDynamic => "Type source data type (dynamic)",
            TypeRoot::TypeSourceFunctionDynamic => "Type source function (dynamic)",
            TypeRoot::Unique => "Unique type",
            TypeRoot::ValueDynamic => "Value type (dynamic)",
            TypeRoot::Root => "Root type",
        });
    }
}

impl Value for TypeRoot {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(*self);
    }

    fn type_(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeRoot::Root));
    }
}
