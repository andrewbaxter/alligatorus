use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectFunction,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_function_aspect_id,
            aspect_js_stack_value_get,
        },
        jscode::{
            JsCode,
            JsCodeCall,
            JsCodeType,
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
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        typecheck_convert,
    },
};

#[derive(Clone)]
pub(crate) struct MortarTypeFunction {
    pub arg: Rc<dyn Type>,
    pub ret: Rc<dyn Type>,
}

impl Value for MortarTypeFunction {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}

impl Type for MortarTypeFunction {
    fn desc(&self) -> String {
        return format!("mortar func");
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeJs>));
        }
        return None;
    }

    fn cast_to(&self, _ex: &ExprContext, _loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        unimplemented!();
    }

    fn cast_from(&self, _ex: &ExprContext, _loc: QExprId, _cv: &CodeValue) -> EvalRes {
        unimplemented!();
    }
}

impl AspectConsumeJs for MortarTypeFunction {
    fn to_js(&self, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        return cv.code.cast::<JsCode>().map(|x| x.unwrap().clone());
    }
}

impl Aspect for MortarTypeFunction { }

impl AspectFunction for MortarTypeFunction {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        function: CodeValue,
        argument: CodeValue,
    ) -> crate::langhelp::EvalRes {
        return er_seq_cv!(loc, erb, {
            let func_type = function.value.type_(xx, loc);
            er_all!(erb, {
                func_to_js = aspect_consume_js_get(xx, loc, func_type.as_ref()),
                arg_to_js = aspect_consume_js_get(xx, loc, self.arg.as_ref()),
                ret_stack = aspect_js_stack_value_get(xx, loc, self.ret.as_ref()),
                argument = typecheck_convert(xx, loc, self.arg.as_ref(), argument),
            });
            er_all!(erb, {
                func = func_to_js.to_js(loc, function),
                arg = arg_to_js.to_js(loc, argument),
            });
            return Ok(ret_stack.to_value(loc, JsCode {
                loc: loc,
                type_: JsCodeType::Call(JsCodeCall {
                    func: Box::new(func),
                    args: vec![arg],
                })
            }));
        });
    }
}
