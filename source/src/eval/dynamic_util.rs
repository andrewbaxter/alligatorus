use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingSet,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicFieldSet,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            typecheck_convert,
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_null::dynamic::TypeNullDynamic,
                type_tuple::binding::BindingValue,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
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
            Type,
        },
        r#gen::{
            Binding,
            aspect_bind_dynamic_get_maybe,
            aspect_consume_dynamic_get,
            aspect_stack_value_dynamic_get,
        },
    },
    std::rc::Rc,
};

pub(crate) fn bind_dynamic_default(xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
    if matches!(cv.code.code, Code_::None) {
        return EResult::ok(None);
    }
    let vtype = match cv.value.type_(xx, loc).into_res() {
        Ok(v) => v,
        Err(e) => return EResult {
            v: None,
            errors: Some(e),
        },
    };
    dynamic_consume_to_dynamic(xx, loc, cv).map(|c| Some(TargetBindSub {
        data: Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(c)),
        },
        type_: vtype,
    }))
}

pub(crate) fn bind_via_target(xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
    let vtype = match cv.value.type_(xx, loc).into_res() {
        Ok(v) => v,
        Err(e) => return EResult {
            v: None,
            errors: Some(e),
        },
    };
    if matches!(cv.code.code, Code_::None) {
        let mut value_res = crate::langhelp::EResult::ok(cv.value.clone());
        let mut errors = None;
        value_res.push_errors(&mut errors);
        let Some(value) = value_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some((cv.code, Box::new(BindingValue { value }) as Box<dyn Binding>)),
        };
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
            EResult::ok((code, Box::new(BindingDynamicBinding {
                dynamic_key: js_key,
                type_: vtype,
            }) as Box<dyn Binding>))
        },
        Ok(None) => {
            EResult::err(loc, format!("Type ({}) returned None from AspectBindDynamic", vtype.desc()))
        },
    }
}

pub(crate) fn dynamic_consume_to_dynamic(_xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
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

pub(crate) fn dynamic_set(xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let target_type = {
            let v = must_downcast_ref::<ValueDynamic>(self_.value.as_ref());
            v.type_.clone()
        };
        let value = erb.must_er(typecheck_convert(xx, loc, target_type.as_ref(), value))?;
        let value_type = erb.must_er(value.value.type_(xx, loc))?;
        er_all!(erb, {
            value_to_dynamic = aspect_consume_dynamic_get(xx, loc, value_type.as_ref()),
            self_code = self_.code.cast::<CodeDynamic>(),
        });
        let value_code = erb.must_er(value_to_dynamic.to_dynamic(xx, loc, value))?;
        let v = must_downcast_ref::<ValueDynamic>(self_.value.as_ref());
        match &v.source {
            ValueDynamicSource::Field(b) => {
                let Some(self_code) = self_code else {
                    return erb.err_new(
                        format!("TODO assertion set left hand doesn't evaluate to dynamic code (empty)")
                    );
                };
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
        }
    });
}

pub(crate) fn dynamic_stack_to_value(loc: QExprId, value: CodeDynamic, type_: Rc<dyn Type>) -> CodeValue {
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

pub(crate) fn throw_error(scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>, msg: &str) {
    let msg = v8::String::new(scope, msg).unwrap();
    let exception = v8::Exception::error(scope, msg);
    scope.throw_exception(exception);
}

pub(crate) fn wrap_with_carry(loc: QExprId, carry: Code, dynamic_code: CodeDynamic) -> CodeDynamic {
    if let Some(Some(carry_js)) = carry.cast::<CodeDynamic>().v {
        let mut entries = match &carry_js.type_ {
            CodeDynamicType::Block(block) => block.entries.clone(),
            _ => vec![CodeDynamicBlockEntry::Code(carry_js.clone())],
        };
        entries.push(CodeDynamicBlockEntry::Code(dynamic_code));
        return CodeDynamic {
            loc,
            type_: CodeDynamicType::Block(CodeDynamicBlock { entries }),
        };
    }
    dynamic_code
}
