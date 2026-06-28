use {
    crate::{
        eval::{
            Erb,
            External,
            const_eval,
            dynamic::{
                code::{
                    CodeDynamic,
                    CodeDynamicBind,
                    CodeDynamicBindingUse,
                    CodeDynamicBlock,
                    CodeDynamicBlockEntry,
                    CodeDynamicCall,
                    CodeDynamicCatch,
                    CodeDynamicIf,
                    CodeDynamicMatch,
                    CodeDynamicMatchBranch,
                    CodeDynamicNewArray,
                    CodeDynamicNewObject,
                    CodeDynamicThrow,
                    CodeDynamicTry,
                    CodeDynamicType,
                },
                util::wrap_with_carry,
            },
            expr::{
                Expr,
                self,
            },
            typecheck_convert,
            value::{
                self,
                type_null::dynamic::TypeNullDynamic,
                type_record::Record,
                type_tuple::{
                    Tuple,
                    TupleElement,
                },
            },
        },
        langhelp::{
            BindingId,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalContext,
            EvalRes,
            ExprContext,
            ImportStateId,
            QExprId,
            Scope,
            Type,
            Value,
        },
        r#gen::{
            AspectDropDynamic,
            STAGE_FN_SCOPE_ID,
            Target,
            aspect_bind_dynamic_get_maybe,
            aspect_consume_dynamic_get,
            aspect_drop_dynamic_get_maybe,
            aspect_initial_bind_get,
            aspect_stack_value_dynamic_get,
        },
    },
    std::{
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        rc::Rc,
    },
};

pub(crate) struct StageLiftCell {
    pub current_exception_type: Option<Rc<dyn Type>>,
    pub import_state: Rc<Cell<ImportStateId>>,
    pub loc: QExprId,
    pub scope: Rc<Scope>,
    pub source_type: Rc<dyn Type>,
    pub target: Rc<dyn Target>,
    pub vx: Rc<EvalContext>,
}

pub(crate) struct TargetBindSub {
    pub data: Code,
    pub type_: Rc<dyn Type>,
}
pub(crate) struct TargetDynamic;

impl Target for TargetDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, BindingId) {
        let dynamic_code =
            sub
                .data
                .cast::<CodeDynamic>()
                .into_res()
                .expect("TargetDynamic bind: expected CodeDynamic")
                .unwrap()
                .clone();
        let js_key = BindingId::Local(xx.vx.next_local_id.next());
        let code = Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(
                    CodeDynamicBlock { entries: vec![CodeDynamicBlockEntry::Bind(CodeDynamicBind {
                        base: js_key,
                        value: Box::new(dynamic_code),
                    })] },
                ),
            })),
        };
        (code, js_key)
    }

    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub {
        let mut dynamic_code_fields = Vec::new();
        let mut dynamic_fields = std::collections::BTreeMap::new();
        for (key, sub) in fields {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_record: expected CodeDynamic")
                    .unwrap()
                    .clone();
            dynamic_code_fields.push((key.to_str(), cd));
            dynamic_fields.insert(key, value::type_record::RecordField::Dynamic(sub.type_));
        }
        let record_type = Rc::new(Record {
            base: None,
            fields: dynamic_fields,
        }) as Rc<dyn Type>;
        let dynamic_code = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: dynamic_code_fields }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(dynamic_code)),
            },
            type_: record_type,
        }
    }

    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub {
        let mut dynamic_code_elements = Vec::new();
        let mut dynamic_elements = Vec::new();
        for sub in elements {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_tuple: expected CodeDynamic")
                    .unwrap()
                    .clone();
            dynamic_code_elements.push(cd);
            dynamic_elements.push(TupleElement::Dynamic(sub.type_));
        }
        let tuple_type = Rc::new(Tuple { elements: dynamic_elements }) as Rc<dyn Type>;
        let dynamic_code = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: dynamic_code_elements }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(dynamic_code)),
            },
            type_: tuple_type,
        }
    }

    fn cast_exception(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        call_code: Code,
        catch_binding: BindingId,
        cast_cv: CodeValue,
    ) -> EResult<Code> {
        let mut errs = vec![];
        let body_dyn = match call_code.cast::<CodeDynamic>().into_res() {
            Ok(Some(d)) => d.clone(),
            Ok(None) => {
                return EResult::err(
                    loc,
                    "Call result code must be CodeDynamic to wrap with exception cast".to_string(),
                );
            },
            Err(e) => {
                errs.extend(e);
                return EResult {
                    v: None,
                    errors: Some(errs),
                };
            },
        };
        let cast_value_type = match cast_cv.value.as_ref() {
            Some(v) => match v.type_(xx, loc).into_res() {
                Ok(t) => t,
                Err(e) => {
                    errs.extend(e);
                    return EResult {
                        v: None,
                        errors: Some(errs),
                    };
                },
            },
            None => {
                return EResult::err(loc, "Exception cast value missing type for to_dynamic".to_string());
            },
        };
        let to_dynamic =
            match crate::r#gen::aspect_consume_dynamic_get(xx, loc, cast_value_type.as_ref()).into_res() {
                Ok(v) => v,
                Err(e) => {
                    errs.extend(e);
                    return EResult {
                        v: None,
                        errors: Some(errs),
                    };
                },
            };
        let cast_dyn = match to_dynamic.to_dynamic(xx, loc, cast_cv).into_res() {
            Ok(v) => v,
            Err(e) => {
                errs.extend(e);
                return EResult {
                    v: None,
                    errors: Some(errs),
                };
            },
        };
        let throw_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::Throw(CodeDynamicThrow { value: Box::new(cast_dyn) }),
        };
        let try_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::Try(CodeDynamicTry {
                body: Box::new(body_dyn),
                catch: Some(CodeDynamicCatch {
                    binding: catch_binding,
                    body: Box::new(throw_code),
                }),
                finally: None,
            }),
        };
        EResult::ok(Code {
            loc,
            code: Code_::Dyn(Rc::new(try_code)),
        })
    }

    fn code_to_bind_sub(&self, _loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub> {
        EResult::ok(TargetBindSub {
            data: code,
            type_: type_,
        })
    }

    fn drop(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Code> {
        let Some(value) = cv.value.as_ref() else {
            return EResult::ok(Code {
                loc,
                code: Code_::None,
            });
        };
        let vtype = match value.type_(xx, loc).into_res() {
            Ok(t) => t,
            Err(e) => return EResult {
                v: None,
                errors: Some(e),
            },
        };
        let Some(drop_aspect) = aspect_drop_dynamic_get_maybe(xx, vtype.as_ref()) else {
            return EResult::ok(Code {
                loc,
                code: Code_::None,
            });
        };
        AspectDropDynamic::drop(drop_aspect.as_ref(), xx, loc, cv)
    }

    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes {
        fn cv_to_dynamic(erb: &mut Erb, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> Result<CodeDynamic, ()> {
            let vtype = erb.must_er(cv.value.as_ref().unwrap().type_(xx, loc))?;
            let consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
            erb.must_er(consume.to_dynamic(xx, loc, cv))
        }

        return er_seq_cv!(loc, erb, {
            let mut dynamic_conditions = Vec::new();
            let mut result_type: Option<Rc<dyn Type>> = None;
            for (cond, body) in conditions {
                let cond_js = cv_to_dynamic(&mut erb, xx, loc, cond)?;
                let body_cv = erb.must_er(const_eval(xx, body))?;
                if result_type.is_none() {
                    result_type = Some(erb.must_er(body_cv.value.as_ref().unwrap().type_(xx, loc))?);
                }
                let body_cv = if let Some(rt) = &result_type {
                    erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), body_cv))?
                } else {
                    body_cv
                };
                let body_js = cv_to_dynamic(&mut erb, xx, loc, body_cv)?;
                dynamic_conditions.push((cond_js, body_js));
            }
            let dynamic_else = if let Some(else_expr) = else_ {
                let else_cv = erb.must_er(const_eval(xx, else_expr))?;
                let else_cv = if let Some(rt) = &result_type {
                    erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), else_cv))?
                } else {
                    result_type = Some(erb.must_er(else_cv.value.as_ref().unwrap().type_(xx, loc))?);
                    else_cv
                };
                let else_js = cv_to_dynamic(&mut erb, xx, loc, else_cv)?;
                Some(Box::new(else_js))
            } else {
                None
            };
            let if_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::If(CodeDynamicIf {
                    conditions: dynamic_conditions,
                    else_: dynamic_else,
                }),
            };
            let final_type: Rc<dyn Type> = result_type.unwrap_or_else(|| Rc::new(TypeNullDynamic) as Rc<dyn Type>);
            let stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, final_type.as_ref()))?;
            return Ok(stack.to_value(loc, if_js));
        });
    }

    fn match_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ConstKey,
        cases: Vec<(String, Rc<dyn Type>, &Expr)>,
        fallback: Option<&Expr>,
    ) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, loc))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, base_type.as_ref()))?;
            let base_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, base))?;
            let element_scope_id = BindingId::Local(xx.vx.next_local_id.next());
            let mut dynamic_cases = Vec::new();
            let mut result_type: Option<Rc<dyn Type>> = None;
            for (tag, variant_type, body_expr) in &cases {
                let child_scope = Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: Some(xx.scope.clone()),
                });
                child_scope.values.borrow_mut().insert(element_key.clone(), {
                    let variant_ib = erb.must_er(aspect_initial_bind_get(xx, loc, variant_type.as_ref()))?;
                    variant_ib.initial_bind(element_scope_id)
                });
                let child_xx = ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: child_scope,
                    target: xx.target.clone(),
                    current_exception_type: xx.current_exception_type.clone(),
                };
                let body_cv = erb.must_er(const_eval(&child_xx, body_expr))?;
                let body_cv = match &result_type {
                    None => {
                        result_type = Some(erb.must_er(body_cv.value.as_ref().unwrap().type_(&child_xx, loc))?);
                        body_cv
                    },
                    Some(rt) => erb.must_er(typecheck_convert(&child_xx, loc, rt.as_ref(), body_cv))?,
                };
                let body_type = erb.must_er(body_cv.value.as_ref().unwrap().type_(&child_xx, loc))?;
                let body_consume = erb.must_er(aspect_consume_dynamic_get(&child_xx, loc, body_type.as_ref()))?;
                let body_js = erb.must_er(body_consume.to_dynamic(&child_xx, loc, body_cv))?;
                dynamic_cases.push(CodeDynamicMatchBranch {
                    dynamic_tag: ConstKey::String(tag.clone()).to_str(),
                    body: body_js,
                });
            }
            let dynamic_fallback = if let Some(fallback_expr) = fallback {
                let fb_cv = erb.must_er(const_eval(xx, fallback_expr))?;
                let fb_cv = match &result_type {
                    None => {
                        result_type = Some(erb.must_er(fb_cv.value.as_ref().unwrap().type_(xx, loc))?);
                        fb_cv
                    },
                    Some(rt) => erb.must_er(typecheck_convert(xx, loc, rt.as_ref(), fb_cv))?,
                };
                let fb_type = erb.must_er(fb_cv.value.as_ref().unwrap().type_(xx, loc))?;
                let fb_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, fb_type.as_ref()))?;
                let fb_js = erb.must_er(fb_consume.to_dynamic(xx, loc, fb_cv))?;
                Some(Box::new(fb_js))
            } else {
                None
            };
            let match_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::Match(CodeDynamicMatch {
                    base: Box::new(base_js),
                    element_key: element_scope_id,
                    cases: dynamic_cases,
                    fallback: dynamic_fallback,
                }),
            };
            let stack_type: Rc<dyn Type> = result_type.unwrap_or_else(|| Rc::new(TypeNullDynamic) as Rc<dyn Type>);
            let stack = aspect_stack_value_dynamic_get(xx, loc, stack_type.as_ref());
            let stack = erb.must_er(stack)?;
            return Ok(stack.to_value(loc, match_js));
        });
    }

    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        let vtype = match cv.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(errors) => return EResult {
                errors: Some(errors),
                v: None,
            },
        };
        if let Some(bind_dyn) = aspect_bind_dynamic_get_maybe(xx, vtype.as_ref()) {
            return bind_dyn.bind_dynamic(xx, loc, cv);
        }
        EResult::ok(None)
    }

    fn seq(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code> {
        let mut dynamic_entries = vec![];
        let mut errors = vec![];
        for entry in &entries {
            match entry.cast::<CodeDynamic>().into_res() {
                Ok(Some(dynamic_code)) => {
                    match &dynamic_code.type_ {
                        CodeDynamicType::Block(block) if
                            !block.entries.is_empty() &&
                                block.entries.iter().all(|e| matches!(e, CodeDynamicBlockEntry::Bind(_))) => {
                            dynamic_entries.extend(block.entries.clone());
                        },
                        _ => dynamic_entries.push(CodeDynamicBlockEntry::Code(dynamic_code.clone())),
                    }
                },
                Ok(None) => { },
                Err(e) => errors.extend(e),
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        if dynamic_entries.is_empty() {
            return EResult::ok(Code {
                loc: loc,
                code: Code_::None,
            });
        }
        return EResult::ok(Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(CodeDynamicBlock { entries: dynamic_entries }),
            })),
        });
    }

    fn stage(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        tree: Expr,
        lower_values: Vec<CodeValue>,
        graft_values: Vec<CodeValue>,
    ) -> EvalRes {
        let tree_cell =
            Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(tree) as Rc<dyn Value>)) });
        let tree_id = BindingId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((tree_id, External::StagedExprCell(tree_cell)));
        let tree_js = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: tree_id }),
        };
        let lower_count = lower_values.len();
        let mut stage_args = vec![tree_js, CodeDynamic {
            loc,
            type_: CodeDynamicType::Literal(serde_json::Value::Number(serde_json::Number::from(lower_count))),
        }];
        return er_seq_cv!(loc, erb, {
            for cv in lower_values {
                let vtype = erb.must_er(cv.value.as_ref().unwrap().type_(xx, loc))?;
                match cv.code.code {
                    Code_::None => {
                        let cell = Rc::new(expr::StagedExprCell { value: RefCell::new(cv.value), });
                        let id = BindingId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((id, External::StagedExprCell(cell)));
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::Literal(serde_json::Value::Null),
                        });
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                        });
                    },
                    Code_::Dyn(_) => {
                        let lift_cell = Rc::new(StageLiftCell {
                            source_type: vtype.clone(),
                            loc,
                            vx: xx.vx.clone(),
                            import_state: xx.import_state.clone(),
                            scope: xx.scope.clone(),
                            target: xx.target.clone(),
                            current_exception_type: xx.current_exception_type.clone(),
                        });
                        let lift_id = BindingId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((lift_id, External::StageLiftCell(lift_cell)));
                        let inner_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
                        let inner_js = erb.must_er(inner_consume.to_dynamic(xx, loc, cv))?;
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: lift_id }),
                        });
                        stage_args.push(inner_js);
                    },
                }
            }
            for cv in graft_values {
                match cv.code.code {
                    Code_::None => {
                        let cell: Rc<RefCell<Option<CodeValue>>> = Rc::new(RefCell::new(Some(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None,
                            },
                            value: cv.value,
                        })));
                        let id = BindingId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((id, External::CodeValueExternCell(cell)));
                        stage_args.push(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                        });
                    },
                    Code_::Dyn(_) => {
                        let vtype = erb.must_er(cv.value.as_ref().unwrap().type_(xx, loc))?;
                        let inner_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
                        let inner_js = erb.must_er(inner_consume.to_dynamic(xx, loc, cv))?;
                        stage_args.push(inner_js);
                    },
                }
            }
            let stage_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: STAGE_FN_SCOPE_ID, }),
                    }),
                    args: stage_args,
                }),
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(stage_js)),
                },
                value: Some(Rc::new(value::value_dynamic::ValueDynamic {
                    source: value::value_dynamic::ValueDynamicSource::Expr,
                    type_: Rc::new(value::type_expr::TypeExprStagedPlaceholder),
                })),
            });
        });
    }

    fn try_finally(&self, loc: QExprId, body: Code, finally: Code) -> EResult<Code> {
        let body_dyn_opt = match body.cast::<CodeDynamic>().into_res() {
            Ok(v) => v.cloned(),
            Err(e) => return EResult {
                v: None,
                errors: Some(e),
            },
        };
        let finally_dyn_opt = match finally.cast::<CodeDynamic>().into_res() {
            Ok(v) => v.cloned(),
            Err(e) => return EResult {
                v: None,
                errors: Some(e),
            },
        };
        match (body_dyn_opt, finally_dyn_opt) {
            (None, None) => EResult::ok(Code {
                loc,
                code: Code_::None,
            }),
            (Some(b), None) => EResult::ok(Code {
                loc,
                code: Code_::Dyn(Rc::new(b)),
            }),
            (None, Some(f)) => EResult::ok(Code {
                loc,
                code: Code_::Dyn(Rc::new(f)),
            }),
            (Some(b), Some(f)) => {
                let try_code = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Try(CodeDynamicTry {
                        body: Box::new(b),
                        catch: None,
                        finally: Some(Box::new(f)),
                    }),
                };
                EResult::ok(Code {
                    loc,
                    code: Code_::Dyn(Rc::new(try_code)),
                })
            },
        }
    }
}
