use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_any::r#const::TypeAnyConst,
            type_root::r#const::{
                TypeRoot,
                TypeRootExporter,
            },
            value_bool::r#const::ValueBoolConst,
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
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeBoolConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeBoolConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bool_const_aspect_id() {
            return Some(Box::new(Box::new(TypeBoolConst) as Box<dyn AspectBoolConst>));
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
            let type_ = value.type_(xx, loc);
            let aspect_bool = erb.must_er(aspect_bool_const_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueBoolConst(aspect_bool.get(value.as_ref())))
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_bool = erb.must_er(aspect_bool_const_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code,
                value: Rc::new(ValueBoolConst(aspect_bool.get(value.as_ref())))
            });
        });
    }

    fn desc(&self) -> String {
        return format!("bool");
    }
}

impl TypeRootExporter for TypeBoolConst {
    fn self_from_artifact_data(_xx: &ExprContext, loc: QExprId, data: serde_json::Value) -> EResult<Rc<dyn Any>> {
        return EResult::from(loc, serde_json::from_value::<()>(data).map(|_| Rc::new(Self) as Rc<dyn Any>));
    }

    fn self_to_artifact_data(_xx: &ExprContext, _loc: QExprId, _self_: Rc<dyn Any>) -> EResult<serde_json::Value> {
        return EResult::ok(serde_json::to_value(()).unwrap());
    }
}

impl Value for TypeBoolConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::BoolConst))
    }
}
