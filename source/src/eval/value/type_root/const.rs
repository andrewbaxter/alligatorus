use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactExporter,
            ArtifactIdSource,
            ExporterAny,
            ExporterRoot,
        },
        langhelp::{
            EResult,
            ExprContext,
            ExprErr,
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
    std::rc::Rc,
};

pub(crate) trait ExporterRootDelegate {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>>;
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact>;
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub enum TypeRoot {
    AnyBindingDynamic,
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
    SourceObjectConst,
    SourceObjectNewConst,
    SourceObjectNewDynamic,
    SourceObjectDynamic,
    SourceObjectRefDynamic,
    SourceExportsLiftedConst,
    SourceFunctionConst,
    SourceFunctionDynamic,
    SourceInterfaceConst,
    SourceInterfaceDynamic,
    SourceUnionConst,
    SourceUnionNewConst,
    SourceUnionNewDynamic,
    SourceUnionDynamic,
    SourceUnionRefDynamic,
    StringConst,
    StringDynamic,
    TypeRef,
    TypeSourceObjectNewDynamic,
    TypeSourceObjectDynamic,
    TypeSourceFunctionDynamic,
    Unique,
    ValueDynamic,
}

impl TypeRoot {
    fn exporter_for(&self) -> Option<ExporterRoot> {
        return match self {
            TypeRoot::BoolConst => Some(ExporterRoot::BoolConst),
            TypeRoot::BoolDynamic => Some(ExporterRoot::BoolDynamic),
            TypeRoot::CodeValueExternDynamic => Some(ExporterRoot::CodeValueExternDynamic),
            TypeRoot::Expr => Some(ExporterRoot::Expr),
            TypeRoot::ListDynamic => Some(ExporterRoot::ListDynamic),
            TypeRoot::MapDynamic => Some(ExporterRoot::MapDynamic),
            TypeRoot::NullConst => Some(ExporterRoot::NullConst),
            TypeRoot::NullDynamic => Some(ExporterRoot::NullDynamic),
            TypeRoot::NumberConst => Some(ExporterRoot::NumberConst),
            TypeRoot::NumberDynamic => Some(ExporterRoot::NumberDynamic),
            TypeRoot::OptionDynamic => Some(ExporterRoot::OptionDynamic),
            TypeRoot::OptionVariantConstructorDynamic => Some(ExporterRoot::OptionVariantConstructorDynamic),
            TypeRoot::Pull => Some(ExporterRoot::Pull),
            TypeRoot::Root => Some(ExporterRoot::Root),
            TypeRoot::SourceObjectConst => Some(ExporterRoot::SourceObjectConst),
            TypeRoot::SourceObjectNewConst => Some(ExporterRoot::SourceObjectNewConst),
            TypeRoot::SourceExportsLiftedConst => Some(ExporterRoot::SourceExportsLiftedConst),
            TypeRoot::SourceFunctionConst => Some(ExporterRoot::SourceFunctionConst),
            TypeRoot::SourceInterfaceConst => Some(ExporterRoot::SourceInterfaceConst),
            TypeRoot::SourceUnionConst => Some(ExporterRoot::SourceUnionConst),
            TypeRoot::SourceUnionNewConst => Some(ExporterRoot::SourceUnionNewConst),
            TypeRoot::StringConst => Some(ExporterRoot::StringConst),
            TypeRoot::StringDynamic => Some(ExporterRoot::StringDynamic),
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
        return EResult::ok(Rc::new(*self));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(*self));
    }
}

impl Type for TypeRoot {
    fn as_value(&self) -> Rc<dyn Value> {
        return Rc::new(*self);
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_artifact_aspect_id() && self.exporter_for().is_some() {
            return Some(Box::new(Box::new(*self) as Box<dyn AspectArtifact>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn desc(&self) -> String {
        return format!("{} type", match self {
            TypeRoot::AnyBindingDynamic => "Any binding type (dynamic)",
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
            TypeRoot::SourceObjectConst => "Source object (const)",
            TypeRoot::SourceObjectNewConst => "Source object new (const)",
            TypeRoot::SourceObjectNewDynamic => "Source object new (dynamic)",
            TypeRoot::SourceObjectDynamic => "Source object (dynamic)",
            TypeRoot::SourceObjectRefDynamic => "Source object ref (dynamic)",
            TypeRoot::SourceExportsLiftedConst => "Source exports lifted type (const)",
            TypeRoot::SourceFunctionConst => "Source function type (const)",
            TypeRoot::SourceFunctionDynamic => "Source function type (dynamic)",
            TypeRoot::SourceInterfaceConst => "Source interface type (const)",
            TypeRoot::SourceInterfaceDynamic => "Source interface type (dynamic)",
            TypeRoot::SourceUnionConst => "Source union type (const)",
            TypeRoot::SourceUnionNewConst => "Source union new (const)",
            TypeRoot::SourceUnionNewDynamic => "Source union new (dynamic)",
            TypeRoot::SourceUnionDynamic => "Source union type (dynamic)",
            TypeRoot::SourceUnionRefDynamic => "Source union ref (dynamic)",
            TypeRoot::StringConst => "String type (const)",
            TypeRoot::StringDynamic => "String type (dynamic)",
            TypeRoot::TypeRef => "Type ref",
            TypeRoot::TypeSourceObjectNewDynamic => "Type source object new (dynamic)",
            TypeRoot::TypeSourceObjectDynamic => "Type source object (dynamic)",
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
