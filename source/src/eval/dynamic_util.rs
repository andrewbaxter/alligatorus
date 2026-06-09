pub(crate) use crate::eval::value::value_dynamic::{
    ValueDynamic,
    ValueDynamicDataAccess,
    ValueDynamicDataScope,
    ValueDynamicSource,
};
use {
    crate::{
        builtin::{
            aspect_bind_dynamic_get_maybe,
            aspect_consume_dynamic_get,
            aspect_initial_bind_get,
            aspect_stack_value_dynamic_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingSet,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicFieldSet,
            CodeDynamicType,
        },
        eval::{
            Erb,
            TargetBindSub,
            typecheck_convert,
            value::{
                tuple::scope_entry::ScopeEntryValue,
                type_null::dynamic::TypeNullDynamic,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            ScopeEntry,
            Type,
        },
    },
    std::{
        mem,
        rc::Rc,
    },
};

pub(crate) fn bind_dynamic_default(xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
    if matches!(cv.code.code, Code_::None) {
        return EResult::ok(None);
    }
    let vtype = cv.value.type_(xx, loc);
    mortar_consume_to_dynamic(xx, loc, cv).map(|c| Some(TargetBindSub {
        data: Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(c)),
        },
        type_: vtype,
    }))
}

pub(crate) fn bind_via_target(
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EResult<(Code, Box<dyn ScopeEntry>)> {
    let vtype = cv.value.type_(xx, loc);
    if matches!(cv.code.code, Code_::None) {
        return EResult::ok((cv.code, Box::new(ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>));
    }
    let Some(bind_dyn) = aspect_bind_dynamic_get_maybe(xx, vtype.as_ref()) else {
        return EResult::err(loc, format!("Type ({}) is missing aspect AspectBindDynamic", vtype.desc()));
    };
    let res = bind_dyn.bind_dynamic(xx, loc, cv);
    match res.into_res() {
        Err(errs) => EResult {
            v: None,
            errors: Some(errs),
        },
        Ok(Some(sub)) => {
            let (code, js_key) = xx.target.bind(xx, loc, sub);
            let ib = aspect_initial_bind_get(xx, loc, vtype.as_ref());
            match ib.into_res() {
                Ok(ib) => EResult::ok((code, ib.initial_bind(js_key))),
                Err(errs) => EResult {
                    v: None,
                    errors: Some(errs),
                },
            }
        },
        Ok(None) => {
            EResult::err(loc, format!("Type ({}) returned None from AspectBindDynamic", vtype.desc()))
        },
    }
}

pub(crate) fn mortar_consume_to_dynamic(_xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
    let r = cv.code.cast::<CodeDynamic>();
    if let Some(c) = r.v {
        if let Some(c) = c {
            return EResult {
                v: Some(c.clone()),
                errors: r.errors,
            };
        } else {
            let mut errors = r.errors.unwrap_or_default();
            errors.push(ExprErr {
                loc: loc,
                errors: vec![ExprSubErr::Opaque(format!("Expression resulted in empty code"))],
            });
            return EResult {
                v: None,
                errors: Some(errors),
            }
        }
    } else {
        return EResult {
            v: None,
            errors: r.errors,
        };
    }
}

pub(crate) fn mortar_dynamic_stack_to_value(loc: QExprId, value: CodeDynamic, type_: Rc<dyn Type>) -> CodeValue {
    CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(value)),
        },
        value: Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_,
        }),
    }
}

pub(crate) fn mortar_set(xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let target_type = {
            let v = must_downcast_ref::<ValueDynamic>(self_.value.as_ref());
            v.type_.clone()
        };
        let value = erb.must_er(typecheck_convert(xx, loc, target_type.as_ref(), value))?;
        let value_type = value.value.type_(xx, loc);
        er_all!(erb, {
            value_to_dynamic = aspect_consume_dynamic_get(xx, loc, value_type.as_ref()),
            self_code = self_.code.cast::<CodeDynamic>(),
        });
        let value_code = erb.must_er(value_to_dynamic.to_dynamic(xx, loc, value))?;
        let v = must_downcast_ref::<ValueDynamic>(self_.value.as_ref());
        match &v.source {
            ValueDynamicSource::Field(b) => {
                let Some(self_code) = self_code else {
                    return erb.err_new(format!("TODO assertion set left hand doesn't evaluate to js code (empty)"));
                };

                // Extract the base from FieldUse so FieldSet targets the parent object
                let base_code = match &self_code.type_ {
                    CodeDynamicType::FieldUse(fu) => *fu.base.clone(),
                    _ => self_code.clone(),
                };
                let set_js = CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::FieldSet(CodeDynamicFieldSet {
                        base: Box::new(base_code),
                        key: b.key.clone(),
                        value: Box::new(value_code)
                    })
                };
                let null_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, &TypeNullDynamic as &dyn Type))?;
                return Ok(null_stack.to_value(loc, set_js))
            },
            ValueDynamicSource::Binding(b) => {
                let mut seq = vec![];
                if let Some(self_code) = self_code {
                    seq.push(self_code.clone());
                }
                seq.push(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::BindingSet(CodeDynamicBindingSet {
                        base: b.key,
                        value: Box::new(value_code),
                    })
                });
                let set_js = CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::Block(
                        CodeDynamicBlock { entries: seq.into_iter().map(CodeDynamicBlockEntry::Code).collect() }
                    )
                };
                let null_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, &TypeNullDynamic as &dyn Type))?;
                return Ok(null_stack.to_value(loc, set_js))
            },
            ValueDynamicSource::Expr => {
                return erb.err_new(format!("Cannot modify a temporary value"));
            },
            ValueDynamicSource::DefinitionSet => {
                return erb.err_new(format!("Cannot modify a definition set function"));
            },
        }
    });
}

pub(crate) fn throw_error(scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>, msg: &str) {
    let msg = v8::String::new(scope, msg).unwrap();
    let exception = v8::Exception::error(scope, msg);
    scope.throw_exception(exception);
}

/// When a compound value (Tuple/Record) is the last expression in a block, the
/// block's code contains binding declarations but the compound value has
/// Code_::None. When consumed to JS, the generated JS references those bindings.
/// This helper wraps the consumed JS with the carry code so the bindings are in
/// scope.
pub(crate) fn wrap_with_carry(loc: QExprId, carry: Code, js: CodeDynamic) -> CodeDynamic {
    if let Some(Some(carry_js)) = carry.cast::<CodeDynamic>().v {
        let mut entries = match &carry_js.type_ {
            CodeDynamicType::Block(block) => block.entries.clone(),
            _ => vec![CodeDynamicBlockEntry::Code(carry_js.clone())],
        };
        entries.push(CodeDynamicBlockEntry::Code(js));
        return CodeDynamic {
            loc,
            type_: CodeDynamicType::Block(CodeDynamicBlock { entries }),
        };
    }
    js
}
