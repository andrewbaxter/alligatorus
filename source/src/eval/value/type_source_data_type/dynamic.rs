use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            from_v8_ref,
            value::dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
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
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::TypeSourceDataTypeConst,
};

pub(crate) struct TypeSourceDataTypeDynamic;

impl Aspect for TypeSourceDataTypeDynamic { }

impl AspectBind for TypeSourceDataTypeDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let dt =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TypeSourceDataTypeConst>()
                .expect("TypeSourceDataTypeDynamic::lift: expected TypeSourceDataTypeConst");
        dt.data.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TypeSourceDataTypeConst { data: dt.data.clone() }),
        }))
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceDataTypeDynamic),
            }),
        }
    }
}

impl Type for TypeSourceDataTypeDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeDynamic) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_ref"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_ref"))
    }

    fn desc(&self) -> String {
        format!("definition_set_data_type_ref")
    }
}

impl Value for TypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
