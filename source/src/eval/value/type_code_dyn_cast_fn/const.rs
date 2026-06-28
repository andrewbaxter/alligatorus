use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_any::r#const::ValueAnyConst,
            type_code_dyn_handle::dynamic::TypeCodeDynHandleDynamic,
            type_root::r#const::TypeRoot,
            value_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
            },
        },
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
            AspectFunction,
            aspect_function_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeCodeDynCastFn;

impl Aspect for TypeCodeDynCastFn { }

impl AspectFunction for TypeCodeDynCastFn {
    fn call(&self, _xx: &ExprContext, _loc: QExprId, function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_any: &dyn Any = argument.value.as_deref().unwrap();
        let target_type: Rc<dyn Type> = if let Some(v) = arg_any.downcast_ref::<ValueAnyConst>() {
            v.inner.clone()
        } else if let Some(dt) =
            arg_any.downcast_ref::<crate::eval::value::type_source_object::r#const::TypeSourceObjectConst>() {
            Rc::new(
                crate::eval::value::type_source_object::r#const::TypeSourceObjectConst { data: dt.data.clone() },
            )
        } else {
            Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type>
        };
        EvalRes::ok(CodeValue {
            code: function.code,
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: target_type,
            })),
        })
    }

    fn exception_type(&self) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
        None
    }
}

impl ExporterAny for TypeCodeDynCastFn {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeCodeDynCastFn));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeCodeDynCastFn));
    }
}

impl Type for TypeCodeDynCastFn {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeDynCastFn) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynCastFn) as Box<dyn AspectFunction>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to code_dyn cast fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from code_dyn cast fn".to_string())
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeDynCastFn))
    }

    fn desc(&self) -> String {
        "code_dyn.cast".to_string()
    }
}

impl Value for TypeCodeDynCastFn {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeCodeDynCastFn);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeDynCastFn))
    }
}
