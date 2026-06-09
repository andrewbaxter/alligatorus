//! The `eval_const` builtin: a const-only function that takes a record
//! `{xx, expr}` and `const_eval`s the staged expression in the ambient
//! context. Used by user-defined `cast_from` impls to evaluate the body
//! they want grafted into the cast result.

use {
    crate::{
        builtin::{
            AspectBind,
            AspectFunction,
            aspect_bind_aspect_id,
            aspect_function_aspect_id,
        },
        eval::{
            expr::Expr,
            value::record::Record,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
            Type,
            Unique,
            Value,
            downcast_value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeEvalConst;

impl Aspect for TypeEvalConst { }

impl Value for TypeEvalConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl AspectFunction for TypeEvalConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, argument: CodeValue) -> EvalRes {
        // `argument` is the record `{xx, expr}`. We don't need the `xx` field
        // (we use the ambient one); we just need `expr`.
        let arg_value = argument.value;
        let record = match Rc::try_unwrap(downcast_value::<Record>(arg_value)) {
            Ok(r) => r,
            Err(_) => return EvalRes::err(loc, "eval_const arg record not unique".to_string()),
        };
        let mut fields = record.fields;
        let expr_field = fields
            .remove(&ConstKey::String("expr".to_string()))
            .expect("eval_const arg missing `expr` field");
        let expr_cv = match expr_field {
            crate::eval::value::record::RecordField::EvalRes(er) => match er.v {
                Some(cv) => cv,
                None => return EvalRes::err(loc, "eval_const arg.expr is error".to_string()),
            },
            _ => return EvalRes::err(loc, "eval_const arg.expr is not an EvalRes".to_string()),
        };
        // `expr_cv.value` is an `Expr` (the result of `const_eval` on a
        // `stage(...)` returning the inner expression as a value).
        let expr_ast = must_downcast_ref::<Expr>(expr_cv.value.as_ref());
        crate::eval::const_eval(xx, expr_ast)
    }
}

impl AspectBind for TypeEvalConst {
    fn bind(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((
            cv.code,
            Box::new(crate::eval::value::tuple::scope_entry::ScopeEntryValue { value: cv.value }) as
                Box<dyn ScopeEntry>,
        ))
    }
}

impl Type for TypeEvalConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeEvalConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeEvalConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeEvalConst) as Box<dyn AspectBind>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to eval_const".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from eval_const".to_string())
    }

    fn desc(&self) -> String {
        "eval_const".to_string()
    }
}
