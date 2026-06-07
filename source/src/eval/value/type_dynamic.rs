pub(crate) use super::value_dynamic::{
    ValueDynamic,
    ValueDynamicDataAccess,
    ValueDynamicDataScope,
    ValueDynamicSource,
};
use {
    crate::{
        builtin::{
            aspect_bind_dynamic_sub_get_maybe,
            aspect_consume_dynamic_get,
        },
        eval::aspect::ScopeEntryDynamicSub,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBind,
            CodeDynamicBindingSet,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicFieldSet,
            CodeDynamicType,
        },
        eval::Erb,
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
            ScopeEntryId,
            Type,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

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

pub(crate) fn bind_dynamic_sub_default(xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(CodeDynamic, ScopeEntryDynamicSub)> {
    let vtype = cv.value.type_(xx, loc);
    mortar_consume_to_dynamic(xx, loc, cv).map(|c| (c, ScopeEntryDynamicSub { type_: vtype }))
}

pub(crate) fn bind_dynamic(
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EResult<(Code, Box<dyn ScopeEntry>)> {
    let vtype = cv.value.type_(xx, loc);
    let Some(bind_sub) = aspect_bind_dynamic_sub_get_maybe(xx, vtype.as_ref()) else {
        return EResult::err(loc, format!("Type ({}) is missing aspect AspectBindDynamicSub", vtype.desc()));
    };
    bind_sub.bind_dynamic_sub(xx, loc, cv).map(|(c, sub)| {
        let js_key = ScopeEntryId::Local(xx.vx.next_local_id.next());
        let code = Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(
                    CodeDynamicBlock {
                        entries: vec![
                            CodeDynamicBlockEntry::Bind(
                                CodeDynamicBind { base: js_key, value: Box::new(c) },
                            ),
                        ],
                    },
                ),
            })),
        };
        (code, Box::new(crate::eval::value::type_scope_entry::dynamic::ScopeEntryDynamicBinding {
            js_key: js_key,
            type_: sub.type_,
        }) as Box<dyn ScopeEntry>)
    })
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
            let v = (self_.value.as_ref() as &dyn Any).downcast_ref::<ValueDynamic>().unwrap();
            v.type_.clone()
        };
        let value = erb.must_er(crate::eval::typecheck_convert(xx, loc, target_type.as_ref(), value))?;
        let value_type = value.value.type_(xx, loc);
        er_all!(erb, {
            value_to_dynamic = aspect_consume_dynamic_get(xx, loc, value_type.as_ref()),
            self_code = self_.code.cast::<CodeDynamic>(),
        });
        let value_code = erb.must_er(value_to_dynamic.to_dynamic(xx, loc, value))?;
        let v = (self_.value.as_ref() as &dyn Any).downcast_ref::<ValueDynamic>().unwrap();
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
                let null_stack =
                    erb.must_er(
                        crate::builtin::aspect_stack_value_dynamic_get(
                            xx,
                            loc,
                            &super::type_null::dynamic::TypeNullDynamic as &dyn Type
                        )
                    )?;
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
                let null_stack =
                    erb.must_er(
                        crate::builtin::aspect_stack_value_dynamic_get(
                            xx,
                            loc,
                            &super::type_null::dynamic::TypeNullDynamic as &dyn Type
                        )
                    )?;
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
