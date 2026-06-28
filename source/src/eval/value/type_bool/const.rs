use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
        },
        eval::value::{
            type_root::r#const::{
                ExporterRootDelegate,
                TypeRoot,
            },
            value_bool::r#const::ValueBoolConst,
            value_type_bool::r#const::ValueTypeBoolConst,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBoolConst,
            aspect_bool_const_aspect_id,
            aspect_bool_const_get,
            aspect_bool_const_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeBoolConst;

impl Aspect for TypeBoolConst { }

impl AspectBoolConst for TypeBoolConst {
    fn get(&self, value: &dyn Value) -> bool {
        return must_downcast_ref::<ValueBoolConst>(value).0;
    }
}

impl ExporterAny for TypeBoolConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeBoolConst));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(ValueTypeBoolConst));
    }
}

impl ExporterRootDelegate for TypeBoolConst {
    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        return EResult::ok(Artifact::Body(make_body(serde_json::Value::Null)));
    }

    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        return EResult::ok(Rc::new(ValueTypeBoolConst) as Rc<dyn crate::artifact::ExporterAny>);
    }
}

impl Type for TypeBoolConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueTypeBoolConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bool_const_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolConst) as Box<dyn AspectBoolConst>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_bool_const_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_bool_const_get_maybe(xx, type_).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let value = value.unwrap();
            let type_ = erb.must_er(value.type_(xx, loc))?;
            let aspect_bool = erb.must_er(aspect_bool_const_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Some(Rc::new(ValueBoolConst(aspect_bool.get(value.as_ref()))))
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let value = value.unwrap();
            let aspect_bool = erb.must_er(aspect_bool_const_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Some(Rc::new(ValueBoolConst(aspect_bool.get(value.as_ref()))))
            });
        });
    }

    fn desc(&self) -> String {
        return format!("bool");
    }
}

impl Value for TypeBoolConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(ValueTypeBoolConst);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::BoolConst))
    }
}
