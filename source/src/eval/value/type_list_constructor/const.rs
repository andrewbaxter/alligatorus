use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_any::r#const::{
                TypeAnyConst,
                ValueAnyConst,
            },
            type_root::r#const::TypeRoot,
            type_binding::dynamic::BindingDynamicBinding,
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
            BindingId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectFunction,
            AspectInitialBind,
            Binding,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_type_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeListConstructorConst;

impl Aspect for TypeListConstructorConst { }

impl AspectFunction for TypeListConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_meta = match argument.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let aspect_type = match aspect_type_get(xx, loc, arg_meta.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let element_type = aspect_type.type_(argument.value.as_ref());
        let list_type: Rc<dyn Type> = Rc::new(super::super::type_list::dynamic::TypeListDynamic { element_type });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueAnyConst { inner: list_type }),
        })
    }
}

impl AspectInitialBind for TypeListConstructorConst {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeListConstructorConst),
        })
    }
}

impl ExporterAny for TypeListConstructorConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeListConstructorConst));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeListConstructorConst));
    }
}

impl Type for TypeListConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeListConstructorConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeListConstructorConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeListConstructorConst) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to list constructor".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list constructor".to_string())
    }

    fn desc(&self) -> String {
        "list constructor".to_string()
    }
}

impl Value for TypeListConstructorConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeListConstructorConst);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::ListConstructorConst))
    }
}
