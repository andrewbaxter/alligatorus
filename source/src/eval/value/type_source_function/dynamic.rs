use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            from_v8_ref,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_some,
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
    super::TypeSourceFunctionConst,
};

// --- TypeSourceFunctionDynamic: eval_half type of a function ref ---
pub(crate) struct TypeSourceFunctionDynamic;

impl Value for TypeSourceFunctionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceFunctionDynamic {
    fn desc(&self) -> String {
        format!("definition_set_function")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceFunctionDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for TypeSourceFunctionDynamic { }

impl AspectConsumeDynamic for TypeSourceFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TypeSourceFunctionDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for TypeSourceFunctionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceFunctionDynamic),
            }),
        }
    }
}

impl AspectLiftDynamic for TypeSourceFunctionDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let func_ref: Rc<TypeSourceFunctionConst> = from_v8_ref::<Rc<TypeSourceFunctionConst>>(&value).clone();
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::DefinitionSet,
            type_: Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() }),
        }))
    }
}

