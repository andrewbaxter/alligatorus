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
            type_bool::r#const::TypeBoolConst,
            type_null::r#const::TypeNullConst,
            type_number::r#const::TypeNumberConst,
            type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
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
    fn self_from_artifact_data(xx: &ExprContext, loc: QExprId, data: serde_json::Value) -> EResult<Rc<dyn Any>>;
    fn self_to_artifact_data(xx: &ExprContext, loc: QExprId, self_: Rc<dyn Any>) -> EResult<serde_json::Value>;
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub enum ExporterRoot {
    BoolConst,
    NullConst,
    NumberConst,
    Root,
    SourceDataTypeConst,
    SourceDataTypeConstructorConst,
    SourceFunctionConst,
    SourceInterfaceConst,
    SourceTaggedUnionConst,
    SourceTaggedUnionConstructorConst,
    StringConst,
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
    ) -> EResult<Rc<dyn std::any::Any>> {
        let res: EResult<Rc<dyn Any>> = match self {
            ExporterRoot::BoolConst => TypeBoolConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NullConst => TypeNullConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NumberConst => TypeNumberConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceTaggedUnionConst => TypeSourceTaggedUnionConst::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceTaggedUnionConstructorConst => TypeSourceTaggedUnionConstructorConst
            ::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::Unique => TypeUnique::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::Root => EResult::from(
                loc,
                serde_json::from_value::<TypeRoot>(data).map(|r| Rc::new(r).into()),
            ),
        };
        let mut errors = None;
        if let Some(errors1) = res.errors {
            errors.get_or_default().extend(errors1);
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

    fn to_serial(self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn std::any::Any>) -> EResult<Artifact> {
        let mut errors = None;
        let mut data = match self {
            ExporterRoot::BoolConst => TypeBoolConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NullConst => TypeNullConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NumberConst => TypeNumberConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceTaggedUnionConst => TypeSourceTaggedUnionConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceTaggedUnionConstructorConst => TypeSourceTaggedUnionConstructorConst
            ::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::Unique => TypeUnique::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::Root => EResult::ok(serde_json::to_value(self).unwrap()),
        };
        data.push_errors(&mut errors);
        let Some(data) = data.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult::ok(Artifact::Body(ArtifactBody {
            data: data,
            exporter: self.self_to_serial_exporter(),
        }));
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
            TypeRoot::Root => Some(ExporterRoot::Root),
            TypeRoot::SourceDataTypeConstructorConst => Some(ExporterRoot::SourceDataTypeConstructorConst),
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
    fn exporter(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn ArtifactExporter> {
        return Rc::new(self.exporter_for().unwrap());
    }
}

impl ExporterAny for TypeRoot {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeRoot {
    fn as_value(&self) -> Rc<dyn Value> {
        return Rc::new(*self);
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if key == aspect_artifact_aspect_id() && self.exporter_for().is_some() {
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
