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
        eval::{
            code_value_extern::{
                TypeCodeValueExternDynamic,
                TypeExprContextPushDynamic,
            },
            definition_set::DefinitionSet,
            pull_type_dispatch::{
                TypeAspectLookupFnType,
                TypeTypeRef,
            },
            value::{
                type_any_scope_entry::dynamic::TypeAnyScopeEntryDynamic,
                type_any_target::dynamic::TypeAnyTargetDynamic,
                type_aspect::dynamic::TypeAspectDynamic,
                type_bool::{
                    dynamic::TypeBoolDynamic,
                    r#const::TypeBoolConst,
                },
                type_code_source::{
                    TypeCodeDynCastFn,
                    TypeCodeDynHandleDynamic,
                    TypeCodeEnumDynamic,
                    TypeCodeStructDynamic,
                },
                type_constkey::dynamic::TypeConstkeyDynamic,
                type_expr::{
                    TypeExpr,
                    TypeExprStagedPlaceholder,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_function_aspect::dynamic::TypeFunctionAspectDynamic,
                type_function_cast_value::dynamic::TypeFunctionCastValueDynamic,
                type_list::dynamic::{
                    TypeListDynamic,
                    TypeListNewDynamic,
                },
                type_list_constructor::r#const::TypeListConstructorConst,
                type_map::dynamic::{
                    TypeMapDynamic,
                    TypeMapGetFnDynamic,
                    TypeMapNewDynamic,
                },
                type_map_constructor::r#const::TypeMapConstructorConst,
                type_null::{
                    dynamic::TypeNullDynamic,
                    r#const::TypeNullConst,
                },
                type_number::{
                    dynamic::TypeNumberDynamic,
                    r#const::TypeNumberConst,
                },
                type_option::dynamic::{
                    TypeOptionDynamic,
                    TypeOptionVariantConstructorDynamic,
                },
                type_option_constructor::r#const::TypeOptionConstructorConst,
                type_pull::TypePull,
                type_source_data_type::dynamic::{
                    TypeSourceDataTypeDynamic,
                    TypeSourceDataTypeRefDynamic,
                },
                type_source_data_type_constructor::{
                    TypeSourceDataTypeConstructorConst,
                    dynamic::TypeSourceDataTypeConstructorDynamic,
                },
                type_source_exports::r#const::TypeSourceExportsLiftedConst,
                type_source_function::{
                    dynamic::{
                        TypeSourceFunctionDynamic,
                        TypeTypeSourceFunctionDynamic,
                    },
                    r#const::TypeSourceFunctionConst,
                },
                type_source_interface::{
                    dynamic::TypeSourceInterfaceDynamic,
                    r#const::TypeSourceInterfaceConst,
                },
                type_source_tagged_union::{
                    dynamic::{
                        TypeSourceTaggedUnionDynamic,
                        TypeSourceTaggedUnionRefDynamic,
                    },
                    r#const::TypeSourceTaggedUnionConst,
                },
                type_source_tagged_union_constructor::{
                    dynamic::TypeSourceTaggedUnionConstructorDynamic,
                    r#const::TypeSourceTaggedUnionConstructorConst,
                },
                type_string::{
                    dynamic::TypeStringDynamic,
                    r#const::TypeStringConst,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
                type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic,
                type_value::dynamic::TypeValueDynamic,
                value_pull::aspect::TypePullAspect,
                value_unique::TypeUnique,
            },
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
            ExporterRoot::AnyScopeEntryDynamic => TypeAnyScopeEntryDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::AnyTargetDynamic => TypeAnyTargetDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::AspectDynamic => TypeAspectDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::AspectLookupFnType => TypeAspectLookupFnType::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::BoolConst => TypeBoolConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::BoolDynamic => TypeBoolDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::CodeDynCastFn => TypeCodeDynCastFn::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::CodeDynHandleDynamic => TypeCodeDynHandleDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::CodeEnumDynamic => TypeCodeEnumDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::CodeStructDynamic => TypeCodeStructDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::CodeValueExternDynamic => TypeCodeValueExternDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::ConstkeyDynamic => TypeConstkeyDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::DefinitionSet => DefinitionSet::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::Expr => TypeExpr::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::ExprContextPushDynamic => TypeExprContextPushDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::ExprStagedPlaceholder => TypeExprStagedPlaceholder::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::FunctionAspectDynamic => TypeFunctionAspectDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::FunctionCastValueDynamic => TypeFunctionCastValueDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::FunctionDynamic => TypeFunctionDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::ListConstructorConst => TypeListConstructorConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::ListDynamic => TypeListDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::ListNewDynamic => TypeListNewDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::MapConstructorConst => TypeMapConstructorConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::MapDynamic => TypeMapDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::MapGetFnDynamic => TypeMapGetFnDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::MapNewDynamic => TypeMapNewDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NullConst => TypeNullConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NullDynamic => TypeNullDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NumberConst => TypeNumberConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::NumberDynamic => TypeNumberDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::OptionConstructorConst => TypeOptionConstructorConst::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::OptionDynamic => TypeOptionDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::OptionVariantConstructorDynamic => TypeOptionVariantConstructorDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::Pull => TypePull::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::PullAspect => TypePullAspect::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceDataTypeConstructorDynamic => TypeSourceDataTypeConstructorDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceDataTypeDynamic => TypeSourceDataTypeDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceDataTypeRefDynamic => TypeSourceDataTypeRefDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceFunctionDynamic => TypeSourceFunctionDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::SourceInterfaceDynamic => TypeSourceInterfaceDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
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
            ExporterRoot::SourceTaggedUnionConstructorDynamic => TypeSourceTaggedUnionConstructorDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceTaggedUnionDynamic => TypeSourceTaggedUnionDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::SourceTaggedUnionRefDynamic => TypeSourceTaggedUnionRefDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::StringDynamic => TypeStringDynamic::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::TypeRef => TypeTypeRef::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::TypeSourceDataTypeConstructorDynamic => TypeTypeSourceDataTypeConstructorDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::TypeSourceDataTypeDynamic => TypeTypeSourceDataTypeDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::TypeSourceFunctionDynamic => TypeTypeSourceFunctionDynamic::self_from_artifact_data(
                &xx,
                loc,
                data,
            ),
            ExporterRoot::Unique => TypeUnique::self_from_artifact_data(&xx, loc, data),
            ExporterRoot::ValueDynamic => TypeValueDynamic::self_from_artifact_data(&xx, loc, data),
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
            ExporterRoot::AnyScopeEntryDynamic => TypeAnyScopeEntryDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::AnyTargetDynamic => TypeAnyTargetDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::AspectDynamic => TypeAspectDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::AspectLookupFnType => TypeAspectLookupFnType::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::BoolConst => TypeBoolConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::BoolDynamic => TypeBoolDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::CodeDynCastFn => TypeCodeDynCastFn::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::CodeDynHandleDynamic => TypeCodeDynHandleDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::CodeEnumDynamic => TypeCodeEnumDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::CodeStructDynamic => TypeCodeStructDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::CodeValueExternDynamic => TypeCodeValueExternDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ConstkeyDynamic => TypeConstkeyDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::DefinitionSet => DefinitionSet::self_to_artifact_data(&xx, loc, self_),
            ExporterRoot::Expr => TypeExpr::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ExprContextPushDynamic => TypeExprContextPushDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ExprStagedPlaceholder => TypeExprStagedPlaceholder::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::FunctionAspectDynamic => TypeFunctionAspectDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::FunctionCastValueDynamic => TypeFunctionCastValueDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::FunctionDynamic => TypeFunctionDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ListConstructorConst => TypeListConstructorConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ListDynamic => TypeListDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ListNewDynamic => TypeListNewDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::MapConstructorConst => TypeMapConstructorConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::MapDynamic => TypeMapDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::MapGetFnDynamic => TypeMapGetFnDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::MapNewDynamic => TypeMapNewDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NullConst => TypeNullConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NullDynamic => TypeNullDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NumberConst => TypeNumberConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::NumberDynamic => TypeNumberDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::OptionConstructorConst => TypeOptionConstructorConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::OptionDynamic => TypeOptionDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::OptionVariantConstructorDynamic => TypeOptionVariantConstructorDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::Pull => TypePull::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::PullAspect => TypePullAspect::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceDataTypeConstructorConst => TypeSourceDataTypeConstructorConst::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceDataTypeConstructorDynamic => TypeSourceDataTypeConstructorDynamic
            ::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceDataTypeDynamic => TypeSourceDataTypeDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceDataTypeRefDynamic => TypeSourceDataTypeRefDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceFunctionDynamic => TypeSourceFunctionDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceInterfaceDynamic => TypeSourceInterfaceDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceTaggedUnionConst => TypeSourceTaggedUnionConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::SourceTaggedUnionConstructorConst => TypeSourceTaggedUnionConstructorConst
            ::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceTaggedUnionConstructorDynamic => TypeSourceTaggedUnionConstructorDynamic
            ::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceTaggedUnionDynamic => TypeSourceTaggedUnionDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::SourceTaggedUnionRefDynamic => TypeSourceTaggedUnionRefDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::StringDynamic => TypeStringDynamic::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::TypeRef => TypeTypeRef::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::TypeSourceDataTypeConstructorDynamic => TypeTypeSourceDataTypeConstructorDynamic
            ::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::TypeSourceDataTypeDynamic => TypeTypeSourceDataTypeDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::TypeSourceFunctionDynamic => TypeTypeSourceFunctionDynamic::self_to_artifact_data(
                xx,
                loc,
                self_,
            ),
            ExporterRoot::Unique => TypeUnique::self_to_artifact_data(xx, loc, self_),
            ExporterRoot::ValueDynamic => TypeValueDynamic::self_to_artifact_data(xx, loc, self_),
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
            TypeRoot::AnyScopeEntryDynamic => Some(ExporterRoot::AnyScopeEntryDynamic),
            TypeRoot::AnyTargetDynamic => Some(ExporterRoot::AnyTargetDynamic),
            TypeRoot::AspectDynamic => Some(ExporterRoot::AspectDynamic),
            TypeRoot::AspectLookupFnType => Some(ExporterRoot::AspectLookupFnType),
            TypeRoot::BoolConst => Some(ExporterRoot::BoolConst),
            TypeRoot::BoolDynamic => Some(ExporterRoot::BoolDynamic),
            TypeRoot::CodeDynCastFn => Some(ExporterRoot::CodeDynCastFn),
            TypeRoot::CodeDynHandleDynamic => Some(ExporterRoot::CodeDynHandleDynamic),
            TypeRoot::CodeEnumDynamic => Some(ExporterRoot::CodeEnumDynamic),
            TypeRoot::CodeStructDynamic => Some(ExporterRoot::CodeStructDynamic),
            TypeRoot::CodeValueExternDynamic => Some(ExporterRoot::CodeValueExternDynamic),
            TypeRoot::ConstkeyDynamic => Some(ExporterRoot::ConstkeyDynamic),
            TypeRoot::Expr => Some(ExporterRoot::Expr),
            TypeRoot::ExprContextPushDynamic => Some(ExporterRoot::ExprContextPushDynamic),
            TypeRoot::ExprStagedPlaceholder => Some(ExporterRoot::ExprStagedPlaceholder),
            TypeRoot::FunctionAspectDynamic => Some(ExporterRoot::FunctionAspectDynamic),
            TypeRoot::FunctionCastValueDynamic => Some(ExporterRoot::FunctionCastValueDynamic),
            TypeRoot::FunctionDynamic => Some(ExporterRoot::FunctionDynamic),
            TypeRoot::ListConstructorConst => Some(ExporterRoot::ListConstructorConst),
            TypeRoot::ListDynamic => Some(ExporterRoot::ListDynamic),
            TypeRoot::ListNewDynamic => Some(ExporterRoot::ListNewDynamic),
            TypeRoot::MapConstructorConst => Some(ExporterRoot::MapConstructorConst),
            TypeRoot::MapDynamic => Some(ExporterRoot::MapDynamic),
            TypeRoot::MapGetFnDynamic => Some(ExporterRoot::MapGetFnDynamic),
            TypeRoot::MapNewDynamic => Some(ExporterRoot::MapNewDynamic),
            TypeRoot::NullConst => Some(ExporterRoot::NullConst),
            TypeRoot::NullDynamic => Some(ExporterRoot::NullDynamic),
            TypeRoot::NumberConst => Some(ExporterRoot::NumberConst),
            TypeRoot::NumberDynamic => Some(ExporterRoot::NumberDynamic),
            TypeRoot::OptionConstructorConst => Some(ExporterRoot::OptionConstructorConst),
            TypeRoot::OptionDynamic => Some(ExporterRoot::OptionDynamic),
            TypeRoot::OptionVariantConstructorDynamic => Some(ExporterRoot::OptionVariantConstructorDynamic),
            TypeRoot::Pull => Some(ExporterRoot::Pull),
            TypeRoot::PullAspect => Some(ExporterRoot::PullAspect),
            TypeRoot::Root => Some(ExporterRoot::Root),
            TypeRoot::SourceDataTypeConstructorConst => Some(ExporterRoot::SourceDataTypeConstructorConst),
            TypeRoot::SourceDataTypeConstructorDynamic => Some(ExporterRoot::SourceDataTypeConstructorDynamic),
            TypeRoot::SourceDataTypeDynamic => Some(ExporterRoot::SourceDataTypeDynamic),
            TypeRoot::SourceDataTypeRefDynamic => Some(ExporterRoot::SourceDataTypeRefDynamic),
            TypeRoot::SourceExportsLiftedConst => Some(ExporterRoot::SourceExportsLiftedConst),
            TypeRoot::SourceFunctionConst => Some(ExporterRoot::SourceFunctionConst),
            TypeRoot::SourceFunctionDynamic => Some(ExporterRoot::SourceFunctionDynamic),
            TypeRoot::SourceInterfaceConst => Some(ExporterRoot::SourceInterfaceConst),
            TypeRoot::SourceInterfaceDynamic => Some(ExporterRoot::SourceInterfaceDynamic),
            TypeRoot::SourceTaggedUnionConst => Some(ExporterRoot::SourceTaggedUnionConst),
            TypeRoot::SourceTaggedUnionConstructorConst => Some(ExporterRoot::SourceTaggedUnionConstructorConst),
            TypeRoot::SourceTaggedUnionConstructorDynamic => Some(ExporterRoot::SourceTaggedUnionConstructorDynamic),
            TypeRoot::SourceTaggedUnionDynamic => Some(ExporterRoot::SourceTaggedUnionDynamic),
            TypeRoot::SourceTaggedUnionRefDynamic => Some(ExporterRoot::SourceTaggedUnionRefDynamic),
            TypeRoot::StringConst => Some(ExporterRoot::StringConst),
            TypeRoot::StringDynamic => Some(ExporterRoot::StringDynamic),
            TypeRoot::TypeRef => Some(ExporterRoot::TypeRef),
            TypeRoot::TypeSourceDataTypeConstructorDynamic => Some(
                ExporterRoot::TypeSourceDataTypeConstructorDynamic,
            ),
            TypeRoot::TypeSourceDataTypeDynamic => Some(ExporterRoot::TypeSourceDataTypeDynamic),
            TypeRoot::TypeSourceFunctionDynamic => Some(ExporterRoot::TypeSourceFunctionDynamic),
            TypeRoot::Unique => Some(ExporterRoot::Unique),
            TypeRoot::ValueDynamic => Some(ExporterRoot::ValueDynamic),
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
