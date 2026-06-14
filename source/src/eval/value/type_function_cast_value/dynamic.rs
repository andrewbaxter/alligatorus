//! `TypeFunctionCastValueDynamic` — the source-level `cast_value(value, type)`
//! builtin. Takes an opaque `Rc<dyn Value>` (e.g. what `cve.value` /
//! `__cve_get_value` returns) and a `TypeSourceDataTypeConst` template, and
//! asserts at runtime that the value is a `ValueUserDefined` whose data type
//! matches the template — returning the data-type instance form so source
//! code can drive field/method access on it normally.
//!
//! This is the bridge between the opaque-value world (`Rc<dyn Value>`) and a
//! concrete user-defined data type at a statically-known site. Eventually we
//! want this kind of unwrapping inferred statically end-to-end; for now it's
//! an explicit construct.

use {
    crate::{
        builtin::{
            AspectFunction,
            AspectInitialBind,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_tuple_get_maybe,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
            },
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_data_type::{
                    TypeSourceDataTypeConst,
                    dynamic::TypeSourceDataTypeDynamic,
                },
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
            ScopeEntry,
            ScopeEntryId,
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
};

pub(crate) struct TypeFunctionCastValueDynamic;

impl Aspect for TypeFunctionCastValueDynamic { }

impl AspectFunction for TypeFunctionCastValueDynamic {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        _function: CodeValue,
        argument: CodeValue,
    ) -> EvalRes {
        // Unpack `(value, type)` from the tuple argument.
        let arg_type = argument.value.type_(xx, loc);
        let arg_tuple = match aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(
                loc,
                format!(
                    "cast_value: argument type ({}) is not a tuple — expected `(value, type)`",
                    arg_type.desc(),
                ),
            ),
        };
        let CodeValue { code: _, value: arg_value } = argument;
        let elements = arg_tuple.take_elements(arg_value);
        if elements.len() != 2 {
            return EvalRes::err(
                loc,
                format!("cast_value: expected 2-tuple `(value, type)`, got {} elements", elements.len()),
            );
        }
        let mut iter = elements.into_iter();
        let value = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let type_ = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };

        // The type argument's value should be a `TypeSourceDataTypeConst`
        // (the const wrapper for a user-defined data type). Extract `.data`
        // to type the result as the corresponding data-type instance type.
        let type_value_any: &dyn Any = type_.value.as_ref();
        let data = match type_value_any.downcast_ref::<TypeSourceDataTypeConst>() {
            Some(t) => t.data.clone(),
            None => return EvalRes::err(
                loc,
                format!(
                    "cast_value: type argument's value is not `TypeSourceDataTypeConst` (got {})",
                    type_.value.type_(xx, loc).desc(),
                ),
            ),
        };

        return er_seq_cv!(loc, erb, {
            // Emit `__cast_value(value_code, type_template)` — the v8 callback
            // extracts the value's `ValueUserDefined.bundle.instance` and
            // panics if the value isn't a `ValueUserDefined` matching the
            // type template.
            let value_code = erb.must_er(value.code.cast::<CodeDynamic>())?.unwrap().clone();
            // The type argument is compile-time only (a `TypeSourceDataTypeConst`
            // singleton with no Dyn code). Materialize it for v8 by pushing an
            // `External::Type` slot and emitting a `BindingUse` to it.
            let template: Rc<dyn Type> = Rc::new(TypeSourceDataTypeConst { data: data.clone() });
            let template_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            xx.vx.externals.borrow_mut().push((
                template_id,
                crate::langhelp::External::Type(template),
            ));
            let type_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: template_id }),
            };
            let cast_fn = CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse {
                    id: crate::eval::CAST_VALUE_FN_SCOPE_ID,
                }),
            };
            let call_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(cast_fn),
                    args: vec![value_code, type_code],
                }),
            };
            let result_type: Rc<dyn Type> = Rc::new(TypeSourceDataTypeDynamic { data });
            return Ok(CodeValue {
                code: Code { loc, code: Code_::Dyn(Rc::new(call_code)) },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: result_type,
                }),
            });
        });
    }
}

impl AspectInitialBind for TypeFunctionCastValueDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeFunctionCastValueDynamic),
        })
    }
}

impl Type for TypeFunctionCastValueDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeFunctionCastValueDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(
                Box::new(TypeFunctionCastValueDynamic) as Box<dyn AspectFunction>,
            ));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(
                Box::new(TypeFunctionCastValueDynamic) as Box<dyn AspectInitialBind>,
            ));
        }
        None
    }

    fn desc(&self) -> String {
        "fn-cast-value".to_string()
    }
}

impl Value for TypeFunctionCastValueDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeFunctionCastValueDynamic)
    }
}
