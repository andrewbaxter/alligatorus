use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactBodyExporter,
            ArtifactExporter,
            ArtifactIdSource,
            ExporterAny,
            ValueFromAny,
        },
        eval::value::{
            type_any::r#const::TypeAnyConst,
            type_null::artifact::ArtifactNull,
            type_root::r#const::{
                ExporterRootDelegate,
                TypeRoot,
            },
            value_null::r#const::ValueNullConst,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectNull,
            aspect_artifact_aspect_id,
            aspect_artifact_get,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_null_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeNullConst;

impl ArtifactExporter for TypeNullConst {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        return EResult {
            errors: None,
            v: Some(Rc::new(ValueFromAny(Rc::new(ValueNullConst)))),
        };
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn ExporterAny>) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(ArtifactBody {
            data: serde_json::Value::Null,
            exporter: crate::artifact::ArtifactBodyExporter::Root,
        }));
    }
}

impl Aspect for TypeNullConst { }

impl AspectArtifact for TypeNullConst {
    fn exporter(&self) -> Rc<dyn ArtifactExporter> {
        return Rc::new(crate::eval::value::type_root::r#const::ExporterRoot::NullConst);
    }
}

impl AspectNull for TypeNullConst { }

impl ExporterAny for TypeNullConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(Self));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(ValueNullConst));
    }
}

impl ExporterRootDelegate for TypeNullConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        return EResult::from(
            loc,
            serde_json::from_value::<ArtifactNull>(
                data,
            ).map(|_| Rc::new(Self) as Rc<dyn crate::artifact::ExporterAny>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::to_value(ArtifactNull).unwrap())));
    }
}

impl Type for TypeNullConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNullConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNullConst) as Box<dyn AspectNull>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeNullConst) as Box<dyn AspectArtifact>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_null_get_maybe(xx, type_).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = erb.must_er(cv.value.type_(xx, loc))?;
            erb.must_er(aspect_null_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueNullConst)
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            erb.must_er(aspect_null_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueNullConst)
            });
        });
    }

    fn desc(&self) -> String {
        return format!("null");
    }
}

impl Value for TypeNullConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(Self);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeRoot::NullConst));
    }
}
