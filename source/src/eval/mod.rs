#[macro_use]
pub mod dynamic_wrap;
#[macro_use]
pub mod util;
pub mod aspect;
pub mod definition_set;
pub mod dynamic_util;
pub mod expr;
pub mod value;

use {
    core::mem,
    crate::{
        builtin::{
            AspectLiftDynamic,
            aspect_artifact_get,
            aspect_bind_dynamic_get_maybe,
            aspect_bind_get,
            aspect_bool_const_get_maybe,
            aspect_consume_dynamic_get,
            aspect_field_get,
            aspect_function_get,
            aspect_initial_bind_get,
            aspect_iterable_get,
            aspect_lift_dynamic_get,
            aspect_operator_add_get,
            aspect_operator_lt_get,
            aspect_set_get,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get_maybe,
            aspect_tagged_union_get,
        },
        code_dynamic::{
            BINDING_PREFIX,
            BUILTIN_PREFIX,
            CodeDynamic,
            CodeDynamicBind,
            CodeDynamicBindingUse,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicCall,
            CodeDynamicIf,
            CodeDynamicMatch,
            CodeDynamicMatchBranch,
            CodeDynamicNewArray,
            CodeDynamicNewObject,
            CodeDynamicType,
            DEF_PREFIX,
            EXPR_PREFIX,
            TAG_VAR,
        },
        eval::{
            dynamic_util::wrap_with_carry,
            value::type_source_define_result::dynamic::SourceDefineResult,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
            CompileRes,
            CompileResResult,
            CompileResultEntry,
            ConstKey,
            Counter,
            DefSetId,
            EResult,
            EvalContext,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            External,
            GlobalContext,
            ImportStateId,
            LogEntry,
            QExprId,
            Scope,
            ScopeEntry,
            ScopeEntryId,
            SourceId,
            Type,
            Value,
            downcast_value,
            make_builtin_types,
        },
        syntax::{
            ExprId,
            SourceSpecAbsolute,
        },
    },
    definition_set::DefinitionSet,
    dumpster::unsync::Gc,
    dynamic_wrap::DynamicWrap,
    expr::{
        Expr,
        ExprAs,
        ExprBind,
        ExprBlock,
        ExprCall,
        ExprField,
        ExprForeach,
        ExprLower,
        ExprMatch,
        ExprMatchBranch,
        ExprSet,
        ExprStage,
        ExprTuple,
        Expr_,
        Module,
    },
    flowcontrol::shed,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        rc::Rc,
        sync::{
            Arc,
            OnceLock,
        },
    },
    v8::tc_scope,
    value::{
        record::{
            Record,
            RecordField,
        },
        tuple::{
            Tuple,
            TupleElement,
        },
        type_function::dynamic::TypeFunctionDynamic,
        type_null::dynamic::TypeNullDynamic,
        type_number::dynamic::TypeNumberDynamic,
        type_string::dynamic::TypeStringDynamic,
        value_null::r#const::ValueNullConst,
    },
};

/// Well-known scope entry ID for __record_to_type mortar function.
pub(crate) const RECORD_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for the __stage function (not a real scope entry).
const STAGE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for __tuple_to_type mortar function.
pub(crate) const TUPLE_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

pub(crate) trait Target {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, ScopeEntryId);
    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub;
    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub;
    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code>;
    fn code_to_bind_sub(&self, loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub>;
    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes;
    fn match_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ConstKey,
        cases: Vec<(String, Rc<dyn Type>, &Expr)>,
        fallback: Option<&Expr>,
    ) -> EvalRes;
    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>>;
    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes;
}

pub(crate) fn compile_importid(
    fx: &Arc<GlobalContext>,
    import_state: ImportStateId,
    source: Module,
    compile_path: Vec<(CompileId, ExprId)>,
) -> CompileRes {
    let cix = Rc::new(CompileImportContext {
        gx: fx.clone(),
        compile_path: compile_path,
        builtin_types: make_builtin_types(),
    });
    let mut root_expr = match source {
        Module::V1(e) => e,
    };
    let root_loc = root_expr.loc;

    // Step 1: Count lower excess and wrap root in stage layers Root is treated as
    // implicit stage (stage_depth=1)
    fn max_lower_excess(expr: &Expr, stage_depth: usize, lower_depth: usize) -> usize {
        //! Walk the expression tree counting stage/lower nesting depths. Returns the max
        //! (lower_depth - stage_depth) where lower_depth > stage_depth. This tells us how
        //! many extra `stage` wrappers are needed at the root.
        fn recurse_children(expr: &Expr, sd: usize, ld: usize) -> usize {
            match &expr.r#type {
                Expr_::Lower(e) => {
                    let new_ld = ld + 1;
                    let excess = new_ld.saturating_sub(sd);
                    excess.max(max_lower_excess(&e.expr, sd, new_ld))
                },
                Expr_::Stage(e) => max_lower_excess(&e.expr, sd + 1, ld),
                Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => 0,
                Expr_::As(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.target, sd, ld))
                },
                Expr_::Call(c) => {
                    max_lower_excess(&c.func, sd, ld).max(max_lower_excess(&c.arg, sd, ld))
                },
                Expr_::Bind(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.value, sd, ld))
                },
                Expr_::Set(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.value, sd, ld))
                },
                Expr_::Field(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.key, sd, ld))
                },
                Expr_::Block(e) => {
                    e.exprs.iter().map(|sub| max_lower_excess(sub, sd, ld)).max().unwrap_or(0)
                },
                Expr_::Tuple(t) => {
                    t.elements.iter().map(|e| max_lower_excess(e, sd, ld)).max().unwrap_or(0)
                },
                Expr_::Record(r) => {
                    r.entries.iter().map(|(k, v)| {
                        max_lower_excess(k, sd, ld).max(max_lower_excess(v, sd, ld))
                    }).max().unwrap_or(0)
                },
                Expr_::If(e) => {
                    let mut m = 0;
                    for b in &e.conditions {
                        m = m.max(max_lower_excess(&b.condition, sd, ld));
                        m = m.max(max_lower_excess(&b.body, sd, ld));
                    }
                    if let Some(el) = &e.else_ {
                        m = m.max(max_lower_excess(el, sd, ld));
                    }
                    m
                },
                Expr_::OperatorLt(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.reference, sd, ld))
                },
                Expr_::OperatorAdd(e) => {
                    max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.reference, sd, ld))
                },
                Expr_::Foreach(e) => {
                    max_lower_excess(&e.base, sd, ld)
                        .max(max_lower_excess(&e.element_key, sd, ld))
                        .max(max_lower_excess(&e.body, sd, ld))
                },
                Expr_::Match(e) => {
                    let mut m = max_lower_excess(&e.base, sd, ld).max(max_lower_excess(&e.element_key, sd, ld));
                    for c in &e.cases {
                        m = m.max(max_lower_excess(&c.condition, sd, ld));
                        m = m.max(max_lower_excess(&c.body, sd, ld));
                    }
                    if let Some(f) = &e.fallback {
                        m = m.max(max_lower_excess(f, sd, ld));
                    }
                    m
                },
            }
        }

        recurse_children(expr, stage_depth, lower_depth)
    }

    let excess = max_lower_excess(&root_expr, 0, 0);
    for _ in 0 .. excess {
        root_expr = Expr {
            loc: root_loc,
            r#type: Expr_::Stage(ExprStage { expr: Rc::new(root_expr) }),
        };
    }

    // Step 2: Repeatedly eval until root is no longer a Stage, then once more
    let mut all_logs = Vec::new();
    loop {
        let is_stage = matches!(&root_expr.r#type, Expr_::Stage(_));
        let eval_result = match eval(&cix, import_state, &root_expr) {
            Ok(v) => v,
            Err(errors) => return CompileRes {
                logs: all_logs,
                generated_mortar_scripts: vec![],
                result: CompileResResult::Err(errors),
            },
        };
        let eval_value = eval_result.value;
        all_logs.extend(eval_result.logs);
        if !is_stage {
            // This was the final eval (root was not a stage) Serialize result
            let logs = all_logs;
            let ex = Rc::new(EvalContext {
                ix: cix.clone(),
                next_local_id: Default::default(),
                definition_sets: RefCell::new(Vec::new()),
                externals: RefCell::new(Vec::new()),
            });
            let xx = Rc::new(ExprContext {
                vx: ex,
                import_state: Rc::new(Cell::new(import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target: Rc::new(TargetDynamic),
            });
            let value_type = eval_value.type_(&xx, root_loc);
            let value_serial = match aspect_artifact_get(&xx, root_loc, value_type.as_ref()).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_mortar_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let value = match value_serial.to_serial(&xx, &fx, root_loc, eval_value).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_mortar_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let generated_mortar_scripts = mem::take(&mut *fx.generated_mortar_scripts.lock().unwrap());
            return CompileRes {
                logs,
                generated_mortar_scripts,
                result: CompileResResult::Ok(value),
            };
        }

        // Result is an Expr (from stage eval) - use it as next expression
        let next_expr = Rc::try_unwrap(downcast_value::<Expr>(eval_value)).ok().expect("Stage eval Expr: Rc not unique");
        root_expr = next_expr;
    }
}

pub(crate) fn const_eval(xx: &ExprContext, expr: &Expr) -> EvalRes {
    match &expr.r#type {
        Expr_::Literal(p) => {
            return EvalRes {
                v: Some(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: p(),
                }),
                errors: None,
            };
        },
        Expr_::ScopeUse(p) => return er_seq_cv!(expr.loc, erb, {
            let key = erb.must_er(const_eval(xx, p.key.as_ref()))?;
            let constkey = erb.must_res(extract_const_key(xx, expr.loc, key.value.as_ref()))?;
            let scope = shed!{
                'found _;
                let mut at = &xx.scope;
                loop {
                    if at.values.borrow().contains_key(&constkey) {
                        break 'found at;
                    }
                    if let Some(parent) = &at.parent {
                        at = parent;
                    } else {
                        break;
                    }
                }
                return erb.err_new(format!("No bound value with id [{:?}]", constkey));
            };
            let scope = scope.values.borrow();
            let binding = scope.get(&constkey).unwrap();
            return Ok(binding.use_(expr.loc, key.code));
        }),
        Expr_::Call(p) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                func = const_eval(xx, p.func.as_ref()),
                arg = const_eval(xx, p.arg.as_ref()),
            });
            let func_type = func.value.type_(xx, expr.loc);
            let aspect_func = erb.must_er(aspect_function_get(xx, expr.loc, func_type.as_ref()))?;
            let v = erb.must_er(aspect_func.call(xx, expr.loc, func, arg))?;
            return Ok(v);
        }),
        Expr_::Set(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                value = const_eval(xx, e.value.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let base_set = erb.must_er(aspect_set_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(base_set.set(xx, expr.loc, base, value))?;
            return Ok(v);
        }),
        Expr_::Field(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                field = const_eval(xx, e.key.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_field = erb.must_er(aspect_field_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_field.field(xx, expr.loc, base, field))?;
            return Ok(v);
        }),
        Expr_::Bind(_) => {
            return EvalRes::err(expr.loc, format!("Bind expression must be directly inside a block"));
        },
        Expr_::Block(e) => {
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
                target: xx.target.clone(),
            };
            return er_seq_cv!(expr.loc, erb, {
                let mut entries = vec![];
                let mut last_val = None;
                for sub in &e.exprs {
                    if let Expr_::Bind(bind) = &sub.r#type {
                        er_all!(erb, {
                            base = const_eval(&child_xx, bind.base.as_ref()),
                            value = const_eval(&child_xx, bind.value.as_ref()),
                        });
                        let base_constkey = erb.must_res(extract_const_key(&child_xx, sub.loc, base.value.as_ref()))?;
                        entries.push(base.code);
                        let value_type = value.value.type_(&child_xx, sub.loc);
                        let bind_aspect = erb.must_er(aspect_bind_get(&child_xx, sub.loc, value_type.as_ref()))?;
                        let binding = erb.must_er(bind_aspect.bind(&child_xx, sub.loc, value))?;
                        entries.push(binding.0);
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Rc::new(ValueNullConst) as Rc<dyn Value>);
                    } else {
                        let r = erb.must_er(const_eval(&child_xx, sub))?;
                        entries.push(r.code);
                        last_val = Some(r.value);
                    }
                }
                return Ok(CodeValue {
                    code: erb.must_er(xx.target.block(expr.loc, entries))?,
                    value: last_val.unwrap_or_else(|| Rc::new(ValueNullConst) as Rc<dyn Value>)
                });
            });
        },
        Expr_::Stage(e) => {
            fn find_matched_lowers_inner<'a>(expr: &'a Expr, sd: usize, ld: usize, out: &mut Vec<&'a Expr>) {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            // Matched! Collect the inner expression. Don't descend further.
                            out.push(&e.expr);
                        } else {
                            find_matched_lowers_inner(&e.expr, sd, new_ld, out);
                        }
                    },
                    Expr_::Stage(e) => find_matched_lowers_inner(&e.expr, sd + 1, ld, out),
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => { },
                    Expr_::As(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.target, sd, ld, out);
                    },
                    Expr_::Call(c) => {
                        find_matched_lowers_inner(&c.func, sd, ld, out);
                        find_matched_lowers_inner(&c.arg, sd, ld, out);
                    },
                    Expr_::Bind(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.value, sd, ld, out);
                    },
                    Expr_::Set(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.value, sd, ld, out);
                    },
                    Expr_::Field(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.key, sd, ld, out);
                    },
                    Expr_::Block(e) => {
                        for sub in &e.exprs {
                            find_matched_lowers_inner(sub, sd, ld, out);
                        }
                    },
                    Expr_::Tuple(t) => {
                        for e in &t.elements {
                            find_matched_lowers_inner(e, sd, ld, out);
                        }
                    },
                    Expr_::Record(r) => {
                        for (k, v) in &r.entries {
                            find_matched_lowers_inner(k, sd, ld, out);
                            find_matched_lowers_inner(v, sd, ld, out);
                        }
                    },
                    Expr_::If(e) => {
                        for b in &e.conditions {
                            find_matched_lowers_inner(&b.condition, sd, ld, out);
                            find_matched_lowers_inner(&b.body, sd, ld, out);
                        }
                        if let Some(el) = &e.else_ {
                            find_matched_lowers_inner(el, sd, ld, out);
                        }
                    },
                    Expr_::OperatorLt(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.reference, sd, ld, out);
                    },
                    Expr_::OperatorAdd(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.reference, sd, ld, out);
                    },
                    Expr_::Foreach(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.element_key, sd, ld, out);
                        find_matched_lowers_inner(&e.body, sd, ld, out);
                    },
                    Expr_::Match(e) => {
                        find_matched_lowers_inner(&e.base, sd, ld, out);
                        find_matched_lowers_inner(&e.element_key, sd, ld, out);
                        for c in &e.cases {
                            find_matched_lowers_inner(&c.condition, sd, ld, out);
                            find_matched_lowers_inner(&c.body, sd, ld, out);
                        }
                        if let Some(f) = &e.fallback {
                            find_matched_lowers_inner(f, sd, ld, out);
                        }
                    },
                }
            }

            let mut matched_lower_exprs = Vec::new();
            find_matched_lowers_inner(&e.expr, 0, 0, &mut matched_lower_exprs);
            if matched_lower_exprs.is_empty() {
                // No matched lowers - return the inner tree as-is
                return EvalRes::ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: Rc::new(e.expr.as_ref().clone()),
                });
            }

            // half_eval each matched lower's inner expression
            return er_seq_cv!(expr.loc, erb, {
                let mut lower_values = Vec::new();
                for lower_inner in &matched_lower_exprs {
                    let cv = erb.must_er(const_eval(xx, lower_inner))?;
                    lower_values.push(cv);
                }
                let v = erb.must_er(xx.target.stage(xx, expr.loc, e.expr.as_ref().clone(), lower_values))?;
                return Ok(v);
            });
        },
        Expr_::Lower(e) => {
            // Transparently evaluate inner expression
            return const_eval(xx, &e.expr);
        },
        Expr_::Tuple(t) => {
            let elements: Vec<TupleElement> =
                t.elements.iter().map(|e| TupleElement::EvalRes(const_eval(xx, e))).collect();
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Rc::new(Tuple {
                    base: None,
                    elements,
                }),
            });
        },
        Expr_::Record(r) => {
            let mut entries = std::collections::BTreeMap::new();
            let mut key_codes = vec![];
            let mut errors = vec![];
            for (k_expr, v_expr) in &r.entries {
                let k_res = const_eval(xx, k_expr);
                if let Some(errs) = k_res.errors {
                    errors.extend(errs);
                }
                let Some(k_cv) = k_res.v else {
                    continue;
                };
                if let Some(js) = k_cv.code.cast::<CodeDynamic>().v.flatten() {
                    key_codes.push(js.clone());
                }
                match extract_const_key(xx, expr.loc, k_cv.value.as_ref()) {
                    Ok(key) => {
                        entries.insert(key, const_eval(xx, v_expr));
                    },
                    Err(e) => errors.push(ExprErr {
                        loc: expr.loc,
                        errors: vec![ExprSubErr::Opaque(e)],
                    }),
                }
            }
            if !errors.is_empty() {
                return EvalRes {
                    v: None,
                    errors: Some(errors),
                };
            }
            let code = if key_codes.is_empty() {
                Code {
                    loc: expr.loc,
                    code: Code_::None,
                }
            } else {
                let block_entries = key_codes.into_iter().map(CodeDynamicBlockEntry::Code).collect();
                Code {
                    loc: expr.loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc: expr.loc,
                        type_: CodeDynamicType::Block(CodeDynamicBlock { entries: block_entries }),
                    })),
                }
            };
            let fields = entries.into_iter().map(|(k, v)| (k, RecordField::EvalRes(v))).collect();
            return EvalRes::ok(CodeValue {
                code,
                value: Rc::new(Record {
                    base: None,
                    fields,
                }),
            });
        },
        Expr_::OperatorLt(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_lt = erb.must_er(aspect_operator_lt_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_lt.lt(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorAdd(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_add = erb.must_er(aspect_operator_add_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_add.add(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::Foreach(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                element_key_cv = const_eval(xx, e.element_key.as_ref()),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_ref()))?;
            let base_type = base.value.type_(xx, expr.loc);
            let iterable = erb.must_er(aspect_iterable_get(xx, expr.loc, base_type.as_ref()))?;

            // Create child scope with element bound, then eval body
            let element_scope_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            let element_type = iterable.element_type();
            let element_ib = erb.must_er(aspect_initial_bind_get(xx, expr.loc, element_type.as_ref()))?;
            child_scope.values.borrow_mut().insert(element_key, element_ib.initial_bind(element_scope_id));
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
                target: xx.target.clone(),
            };
            let body = erb.must_er(const_eval(&child_xx, e.body.as_ref()))?;
            let v = erb.must_er(iterable.foreach(xx, expr.loc, base, element_scope_id, body))?;
            return Ok(v);
        }),
        Expr_::Match(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                element_key_cv = const_eval(xx, e.element_key.as_ref()),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_ref()))?;
            let base_type = base.value.type_(xx, expr.loc);
            let tagged_union = erb.must_er(aspect_tagged_union_get(xx, expr.loc, base_type.as_ref()))?;
            let variants = tagged_union.variants();

            // Validate case tags against variant tags
            let mut case_tags: Vec<ConstKey> = Vec::new();
            for case in &e.cases {
                let cond_cv = erb.must_er(const_eval(xx, &case.condition))?;
                let tag = erb.must_res(extract_const_key(xx, expr.loc, cond_cv.value.as_ref()))?;
                if !variants.iter().any(|(t, _, _)| t == &tag) {
                    return erb.err_new(format!("Match case tag '{:?}' not found in tagged union", tag));
                }
                case_tags.push(tag);
            }

            // Check exhaustiveness
            let is_exhaustive = variants.iter().all(|(vt, _, _)| case_tags.contains(vt));
            if !is_exhaustive && e.fallback.is_none() {
                for (variant_tag, _, _) in &variants {
                    if !case_tags.contains(variant_tag) {
                        return erb.err_new(format!("Non-exhaustive match: missing case for tag '{:?}'", variant_tag));
                    }
                }
            }
            if is_exhaustive && e.fallback.is_some() {
                return erb.err_new("Exhaustive match must not have a fallback");
            }

            // Delegate code generation to target
            let mut cases = Vec::new();
            for (i, case) in e.cases.iter().enumerate() {
                let tag = &case_tags[i];
                let (_, dynamic_tag, variant_type) = variants.iter().find(|(t, _, _)| t == tag).unwrap();
                cases.push((dynamic_tag.clone(), variant_type.clone(), &case.body));
            }
            let v = erb.must_er(xx.target.match_(xx, expr.loc, base, element_key, cases, e.fallback.as_deref()))?;
            return Ok(v);
        }),
        Expr_::Import(e) => return er_seq_cv!(expr.loc, erb, {
            let spec_cv = erb.must_er(const_eval(xx, &e.spec))?;
            let spec_str = erb.must_res(extract_const_key(xx, expr.loc, spec_cv.value.as_ref()))?;
            let spec_name = match spec_str {
                ConstKey::String(s) => s,
                _ => return erb.err_new("Import spec must be a string"),
            };
            let source_spec = SourceSpecAbsolute::Virtual(spec_name);
            let (source_id, source_module) = erb.must_res(xx.vx.ix.gx.sources.get(source_spec.clone()))?;
            let compile_id = CompileId(xx.import_state.get(), source_id);

            // Check if already compiling/compiled; spawn thread if new
            let entry = {
                let mut results = xx.vx.ix.gx.compile_results.lock().unwrap();
                if let Some(entry) = results.get(&compile_id) {
                    entry.clone()
                } else {
                    let entry = Arc::new(CompileResultEntry { result: std::sync::RwLock::new(None), });
                    let started = Arc::new((std::sync::Mutex::new(false), std::sync::Condvar::new()));
                    let started_clone = started.clone();
                    let gx = xx.vx.ix.gx.clone();
                    let import_state = xx.import_state.get();
                    let entry_clone = entry.clone();
                    std::thread::spawn(move || {
                        let mut write_guard = entry_clone.result.write().unwrap();
                        {
                            let (lock, cvar) = &*started_clone;
                            *lock.lock().unwrap() = true;
                            cvar.notify_one();
                        }
                        let module = crate::source_module_to_module(source_id, &source_module);
                        let result = compile_importid(&gx, import_state, module, vec![]);
                        *write_guard = Some(Ok(result));
                    });
                    let (lock, cvar) = &*started;
                    let mut started_flag = lock.lock().unwrap();
                    while !*started_flag {
                        started_flag = cvar.wait(started_flag).unwrap();
                    }
                    results.insert(compile_id, entry.clone());
                    entry
                }
            };

            // Return a lazy proxy value — resolution deferred until use
            let state = Rc::new(value::value_pending_import::PendingImportState {
                entry,
                gx: xx.vx.ix.gx.clone(),
            });
            return Ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None
                },
                value: Rc::new(value::value_pending_import::ValuePendingImport {
                    state,
                    resolved: RefCell::new(None)
                }),
            });
        }),
        Expr_::If(e) => return er_seq_cv!(expr.loc, erb, {
            // Try constant folding first: check if all conditions resolve to constant bools
            let mut conditions_evaluated = Vec::new();
            let mut all_const = true;
            for branch in &e.conditions {
                let cond = erb.must_er(const_eval(xx, &branch.condition))?;
                let cond_type = cond.value.type_(xx, expr.loc);
                if let Some(aspect_bool) = aspect_bool_const_get_maybe(xx, cond_type.as_ref()) {
                    let val = aspect_bool.get(cond.value.as_ref());
                    conditions_evaluated.push((val, cond, &branch.body));
                } else {
                    all_const = false;
                    conditions_evaluated.push((false, cond, &branch.body));
                }
            }
            if all_const {
                // Constant folding: find the first true branch
                for (val, _cond, body) in &conditions_evaluated {
                    if *val {
                        let body_cv = erb.must_er(const_eval(xx, body))?;
                        return Ok(body_cv);
                    }
                }

                // No true branch; use else or null
                if let Some(else_expr) = &e.else_ {
                    let else_cv = erb.must_er(const_eval(xx, else_expr))?;
                    return Ok(else_cv);
                }
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None
                    },
                    value: Rc::new(ValueNullConst),
                });
            } else {
                // Delegate code generation to target
                let conditions: Vec<(CodeValue, &Expr)> =
                    conditions_evaluated.into_iter().map(|(_, cond, body)| (cond, body as &Expr)).collect();
                let v = erb.must_er(xx.target.if_(xx, expr.loc, conditions, e.else_.as_deref(),))?;
                return Ok(v);
            }
        }),
        Expr_::As(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                target = const_eval(xx, e.target.as_ref()),
            });
            let target_type = target.value.type_(xx, expr.loc);
            let v = erb.must_er(typecheck_convert(xx, expr.loc, target_type.as_ref(), base))?;
            return Ok(v);
        }),
    }
}

/// EvalResBuilder
pub(crate) struct Erb {
    errors: Vec<ExprErr>,
    loc: QExprId,
}

impl Erb {
    pub(crate) fn err_new(&mut self, message: impl ToString) -> Result<CodeValue, ()> {
        self.errors.push(ExprErr {
            loc: self.loc,
            errors: vec![ExprSubErr::Opaque(message.to_string())],
        });
        return Err(());
    }

    pub(crate) fn err_push(&mut self, message: impl ToString) {
        self.errors.push(ExprErr {
            loc: self.loc,
            errors: vec![ExprSubErr::Opaque(message.to_string())],
        });
    }

    pub(crate) fn must_er<T>(&mut self, r: EResult<T>) -> Result<T, ()> {
        if let Some(errs) = r.errors {
            self.errors.extend(errs);
        }
        let Some(cv) = r.v else {
            return Err(());
        };
        return Ok(cv);
    }

    pub(crate) fn must_res<T>(&mut self, r: Result<T, String>) -> Result<T, ()> {
        match r {
            Ok(r) => {
                return Ok(r);
            },
            Err(e) => {
                self.err_push(e);
                return Err(());
            },
        }
    }

    pub(crate) fn new(loc: QExprId) -> Self {
        return Self {
            loc: loc,
            errors: Default::default(),
        };
    }
}

fn eval(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<EvalResult, Vec<ExprErr>> {
    let mut dynamic_scope_isolate = v8::Isolate::new(Default::default());
    dynamic_scope_isolate.set_slot(V8RcStore { weaks: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(V8LogStore { logs: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(V8UniqueCounter {
        compile_id: cix
            .compile_path
            .last()
            .map(|(c, _)| c.clone())
            .unwrap_or(CompileId(ImportStateId(0), SourceId(0))),
        counter: Counter::default(),
    });
    dynamic_scope_isolate.set_slot(V8DefSetCounter {
        import_id: import_state,
        counter: Counter::default(),
    });
    let dynamic_scope_handle = std::pin::pin!(v8::HandleScope::new(&mut dynamic_scope_isolate));
    let mut dynamic_scope_handle2 = dynamic_scope_handle.init();
    let dynamic_scope_context = v8::Context::new(&mut dynamic_scope_handle2, Default::default());
    let dynamic_scope_context2 = &mut v8::ContextScope::new(&mut dynamic_scope_handle2, dynamic_scope_context);
    tc_scope!(let dynamic_scope, dynamic_scope_context2);
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_local_id: Default::default(),
        definition_sets: RefCell::new(Vec::new()),
        externals: RefCell::new(Vec::new()),
    });
    let root_loc = expr.loc;
    let target: Rc<dyn Target> = Rc::new(TargetDynamic);
    let scope = Rc::new(Scope {
        values: RefCell::new(HashMap::new()),
        parent: None,
    });
    let xx = Rc::new(ExprContext {
        vx: ex.clone(),
        import_state: Rc::new(Cell::new(import_state)),
        scope: scope.clone(),
        target: target.clone(),
    });
    let mut builtin_scope_entries: Vec<(String, ScopeEntryId, Rc<dyn Type>)> = Vec::new();
    for (key, dynamic_key, type_, dynamic_value) in [
        //. .
        dynamic_wrap!(fn log(ex, dynamic_scope, scope)(message: String) ->() {
            let timestamp_ms =
                std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
            scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                timestamp_ms,
                message
            });
        }),
        dynamic_wrap!(fn define(ex, dynamic_scope, scope)() -> SourceDefineResult {
            let dsc = scope.get_slot::<V8DefSetCounter>().unwrap();
            let def_set_id = DefSetId(dsc.counter.next());
            let import_id = dsc.import_id;
            SourceDefineResult { ds: Rc::new(Gc::new(DefinitionSet::new(import_id, def_set_id))) }
        }),
        dynamic_wrap!(fn str_to_json(ex, dynamic_scope, scope)(s: String) -> String {
            serde_json::to_string(&s).unwrap()
        }),
        dynamic_wrap!(fn constkey_to_str(ex, dynamic_scope, scope)(key: crate:: langhelp:: ConstKey) -> String {
            key.to_str()
        }),
    ] {
        dynamic_scope
            .get_current_context()
            .global(dynamic_scope)
            .set(
                dynamic_scope,
                v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key)).unwrap().cast(),
                dynamic_value,
            );
        let ib = aspect_initial_bind_get(&xx, root_loc, type_.as_ref()).into_res()?;
        builtin_scope_entries.push((key.to_string(), dynamic_key, type_));
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), ib.initial_bind(dynamic_key));
    }

    // Type references (static scope entries, no JS representation)
    for (
        key,
        type_ref,
    ) in [
        ("null", Rc::new(TypeNullDynamic) as Rc<dyn Type>),
        ("string", Rc::new(TypeStringDynamic) as Rc<dyn Type>),
        ("number", Rc::new(TypeNumberDynamic) as Rc<dyn Type>),
        ("expr", Rc::new(value::type_expr::TypeExpr) as Rc<dyn Type>),
    ] {
        let cv = CodeValue {
            code: Code {
                loc: root_loc,
                code: Code_::None,
            },
            value: Rc::new(value::value_empty::r#const::ValueEmptyConst { type_: type_ref.clone() }),
        };
        let bind = aspect_bind_get(&xx, root_loc, type_ref.as_ref()).into_res()?;
        let (_, entry) = bind.bind(&xx, root_loc, cv).into_res()?;
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), entry);
    }

    // list/map constructors: jswrap fns for v8 with constructor types for half-eval
    for (
        key,
        constructor_type,
        (_, dynamic_key, _, dynamic_value),
    ) in [
        (
            "list",
            Rc::new(value::type_list_constructor::r#const::TypeListConstructorConst) as Rc<dyn Type>,
            dynamic_wrap!(
                fn list(
                    ex,
                    dynamic_scope,
                    scope
                )(element_type: dynamic_wrap:: AnyType) -> value:: type_list:: dynamic:: TypeListDynamic {
                    value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
                }
            ),
        ),
        (
            "map",
            Rc::new(value::type_map_constructor::r#const::TypeMapConstructorConst) as Rc<dyn Type>,
            dynamic_wrap!(
                fn map(
                    ex,
                    dynamic_scope,
                    scope
                )(
                    key_type: dynamic_wrap:: AnyType,
                    value_type: dynamic_wrap:: AnyType
                ) -> value:: type_map:: dynamic:: TypeMapDynamic {
                    value::type_map::dynamic::TypeMapDynamic {
                        key_type: key_type.0,
                        value_type: value_type.0
                    }
                }
            ),
        ),
    ] {
        dynamic_scope
            .get_current_context()
            .global(dynamic_scope)
            .set(
                dynamic_scope,
                v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(dynamic_key)).unwrap().cast(),
                dynamic_value,
            );
        let ib = aspect_initial_bind_get(&xx, root_loc, constructor_type.as_ref()).into_res()?;
        builtin_scope_entries.push((key.to_string(), dynamic_key, constructor_type));
        scope.values.borrow_mut().insert(ConstKey::String(key.to_string()), ib.initial_bind(dynamic_key));
    }
    let result = const_eval(&xx, expr);
    if let Some(errs) = result.errors {
        return Err(errs);
    }
    let cv = result.v.unwrap();
    let value_type = cv.value.type_(&xx, root_loc);
    let (stage1_code, fallback_value) = match &cv.code.code {
        Code_::None => (None, Some(cv.value)),
        _ => {
            let to_dynamic = aspect_consume_dynamic_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let js = to_dynamic.to_dynamic(&xx, root_loc, cv).into_res()?;
            (Some(js), None)
        },
    };

    // Compile definition set implementations Collect function data first to release
    // the borrow on definition_sets, since eval_half may trigger function calls that
    // re-borrow it.
    struct DefFuncInfo {
        arg_type: Rc<dyn Type>,
        impl_expr_ast: Rc<dyn Value>,
        ret_type: Rc<dyn Type>,
        scope_entry_id: ScopeEntryId,
    }

    let def_func_infos: Vec<DefFuncInfo> = {
        let sets = ex.definition_sets.borrow();
        let mut infos = vec![];
        for ds in sets.iter() {
            for func_data in ds.functions.borrow().iter() {
                let impl_expr_ast =
                    func_data.impl_expr.borrow_mut().take().expect("Definition set function missing implementation");
                let arg_type = func_data.arg_type.borrow().clone().expect("Definition set function missing arg type");
                let ret_type =
                    func_data.ret_type.borrow().clone().expect("Definition set function missing return type");
                let scope_id = func_data.scope_entry_id.get().expect("Definition set function not registered");
                infos.push(DefFuncInfo {
                    impl_expr_ast,
                    arg_type,
                    ret_type,
                    scope_entry_id: scope_id,
                });
            }
        }
        infos
    };

    // Build scope for definition set body compilation: only builtins and type
    // references. Definition set members are accessed via lower + to_dynamic.
    fn build_builtin_scope(
        xx: &ExprContext,
        loc: QExprId,
        builtin_scope_entries: &[(String, ScopeEntryId, Rc<dyn Type>)],
    ) -> Result<HashMap<ConstKey, Box<dyn ScopeEntry>>, Vec<ExprErr>> {
        let mut scope_entries = HashMap::<ConstKey, Box<dyn ScopeEntry>>::new();
        for (name, dynamic_key, type_) in builtin_scope_entries {
            let ib = aspect_initial_bind_get(xx, loc, type_.as_ref()).into_res()?;
            scope_entries.insert(ConstKey::String(name.clone()), ib.initial_bind(*dynamic_key));
        }
        for (
            key,
            type_ref,
        ) in [
            ("null", Rc::new(TypeNullDynamic) as Rc<dyn Type>),
            ("string", Rc::new(TypeStringDynamic) as Rc<dyn Type>),
            ("number", Rc::new(TypeNumberDynamic) as Rc<dyn Type>),
            ("expr", Rc::new(value::type_expr::TypeExpr) as Rc<dyn Type>),
        ] {
            let cv = CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::None,
                },
                value: Rc::new(value::value_empty::r#const::ValueEmptyConst { type_: type_ref.clone() }),
            };
            let bind = aspect_bind_get(xx, loc, type_ref.as_ref()).into_res()?;
            let (_, entry) = bind.bind(xx, loc, cv).into_res()?;
            scope_entries.insert(ConstKey::String(key.to_string()), entry);
        }
        Ok(scope_entries)
    }

    let mut def_funcs: Vec<(ScopeEntryId, Option<ScopeEntryId>, CodeDynamic)> = vec![];
    for info in &def_func_infos {
        let impl_expr_ast = must_downcast_ref::<Expr>(info.impl_expr_ast.as_ref());

        // Create a separate evaluation context for the function body
        let body_ex = Rc::new(EvalContext {
            ix: ex.ix.clone(),
            next_local_id: Counter::default(),
            definition_sets: RefCell::new(Vec::new()),
            externals: RefCell::new(Vec::new()),
        });
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let func_xx = {
            let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
            let arg_ib = aspect_initial_bind_get(&xx, root_loc, info.arg_type.as_ref()).into_res()?;
            body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
            Rc::new(ExprContext {
                vx: body_ex,
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope {
                    values: RefCell::new(body_scope),
                    parent: None,
                }),
                target: target.clone(),
            })
        };
        let impl_result = const_eval(&func_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();

        // Type check return value against ret type
        let impl_cv = {
            let r = typecheck_convert(&func_xx, root_loc, info.ret_type.as_ref(), impl_cv);
            if let Some(errs) = r.errors {
                return Err(errs);
            }
            r.v.unwrap()
        };
        let impl_code =
            impl_cv
                .code
                .cast::<CodeDynamic>()
                .into_res()?
                .expect("Definition set function body must produce JS code")
                .clone();
        def_funcs.push((info.scope_entry_id, Some(arg_id), impl_code));
    }

    // Compile data type method implementations
    enum CompiledDataMethodBody {
        Fresh {
            self_scope_entry_id: ScopeEntryId,
            data_field: String,
            impl_code: CodeDynamic,
        },
        Precompiled(String),
    }

    struct CompiledDataMethod {
        body: CompiledDataMethodBody,
        scope_entry_id: ScopeEntryId,
    }

    let mut def_data_methods: Vec<CompiledDataMethod> = vec![];
    for ds in ex.definition_sets.borrow().iter() {
        for dt_data in ds.data_types.borrow().iter() {
            let underlying_type: Rc<dyn Type> = dt_data.underlying_type.get().clone();
            let data_field = dt_data.data_field.clone();
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not assigned");

                // Check for pre-compiled JS first
                if let Some(compiled_js) = method.compiled_js.borrow().clone() {
                    def_data_methods.push(CompiledDataMethod {
                        scope_entry_id: method_id,
                        body: CompiledDataMethodBody::Precompiled(compiled_js),
                    });
                    continue;
                }
                let impl_expr = method.impl_expr.borrow();
                let impl_expr_ast = must_downcast_ref::<expr::Expr>(impl_expr.as_ref().unwrap().as_ref());
                let ret_type: Rc<dyn Type> =
                    method.ret_type.borrow().clone().expect("Data type method missing return type");

                // Create a separate evaluation context for the method body
                let body_ex = Rc::new(EvalContext {
                    ix: ex.ix.clone(),
                    next_local_id: Counter::default(),
                    definition_sets: RefCell::new(Vec::new()),
                    externals: RefCell::new(Vec::new()),
                });
                let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                method.self_scope_entry_id.set(Some(self_id));
                let method_xx = {
                    let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
                    let self_ib = aspect_initial_bind_get(&xx, root_loc, underlying_type.as_ref()).into_res()?;
                    body_scope.insert(ConstKey::String("self".to_string()), self_ib.initial_bind(self_id));
                    Rc::new(ExprContext {
                        vx: body_ex,
                        import_state: xx.import_state.clone(),
                        scope: Rc::new(Scope {
                            values: RefCell::new(body_scope),
                            parent: None,
                        }),
                        target: target.clone(),
                    })
                };
                let impl_result = const_eval(&method_xx, impl_expr_ast);
                if let Some(errs) = impl_result.errors {
                    return Err(errs);
                }
                let impl_cv = impl_result.v.unwrap();

                // Type check return value
                let impl_cv = {
                    let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                    if let Some(errs) = r.errors {
                        return Err(errs);
                    }
                    r.v.unwrap()
                };
                let impl_code =
                    impl_cv
                        .code
                        .cast::<CodeDynamic>()
                        .into_res()?
                        .expect("Method body must produce JS code")
                        .clone();
                def_data_methods.push(CompiledDataMethod {
                    scope_entry_id: method_id,
                    body: CompiledDataMethodBody::Fresh {
                        self_scope_entry_id: self_id,
                        data_field: data_field.clone(),
                        impl_code,
                    },
                });
            }

            // Compile interface method implementations
            for impl_data in dt_data.interface_impls.borrow().iter() {
                for method_impl in &impl_data.method_impls {
                    let impl_expr_ast = must_downcast_ref::<expr::Expr>(method_impl.impl_expr.as_ref());
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not assigned");

                    // Resolve ret type from interface method
                    let ret_type: Rc<dyn Type> = {
                        let iface_methods = impl_data.interface.methods.borrow();
                        let iface_method = &iface_methods[method_impl.interface_method_index];
                        iface_method.ret_type.borrow().clone().expect("Interface method missing return type")
                    };

                    // Create a separate evaluation context for the method body
                    let body_ex = Rc::new(EvalContext {
                        ix: ex.ix.clone(),
                        next_local_id: Counter::default(),
                        definition_sets: RefCell::new(Vec::new()),
                        externals: RefCell::new(Vec::new()),
                    });
                    let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
                    method_impl.self_scope_entry_id.set(Some(self_id));
                    let method_xx = {
                        let mut body_scope = build_builtin_scope(&xx, root_loc, &builtin_scope_entries)?;
                        let self_ib = aspect_initial_bind_get(&xx, root_loc, underlying_type.as_ref()).into_res()?;
                        body_scope.insert(ConstKey::String("self".to_string()), self_ib.initial_bind(self_id));
                        Rc::new(ExprContext {
                            vx: body_ex,
                            import_state: xx.import_state.clone(),
                            scope: Rc::new(Scope {
                                values: RefCell::new(body_scope),
                                parent: None,
                            }),
                            target: target.clone(),
                        })
                    };
                    let impl_result = const_eval(&method_xx, impl_expr_ast);
                    if let Some(errs) = impl_result.errors {
                        return Err(errs);
                    }
                    let impl_cv = impl_result.v.unwrap();
                    let impl_cv = {
                        let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                        if let Some(errs) = r.errors {
                            return Err(errs);
                        }
                        r.v.unwrap()
                    };
                    let impl_code =
                        impl_cv
                            .code
                            .cast::<CodeDynamic>()
                            .into_res()?
                            .expect("Interface method body must produce JS code")
                            .clone();
                    def_data_methods.push(CompiledDataMethod {
                        scope_entry_id: method_id,
                        body: CompiledDataMethodBody::Fresh {
                            self_scope_entry_id: self_id,
                            data_field: data_field.clone(),
                            impl_code,
                        },
                    });
                }
            }
        }
    }
    let has_js = stage1_code.is_some() || !def_funcs.is_empty() || !def_data_methods.is_empty();
    if !has_js {
        let logs = dynamic_scope.get_slot::<V8LogStore>().unwrap().logs.take();
        return Ok(EvalResult {
            value: fallback_value.expect("No JS and no fallback value"),
            logs,
        });
    }
    let mut code1 = JsSourceBuilder::default();

    // Render definition set function implementations as JS functions
    for (scope_id, arg_scope_id, impl_code) in &def_funcs {
        let func_name = scope_entry_id_dynamic_key(*scope_id);
        if let Some(arg_id) = arg_scope_id {
            code1.push(root_loc, format!("function {}({}) {{", func_name, scope_entry_id_dynamic_key(*arg_id)));
        } else {
            code1.push(root_loc, format!("function {}() {{", func_name));
        }
        let body_var = render_code(&mut code1, impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "}".to_string());
    }

    // Render data type method implementations as JS functions Methods use `this` to
    // access the wrapper object; self = this.<data_field>
    for method in &def_data_methods {
        match &method.body {
            CompiledDataMethodBody::Fresh { self_scope_entry_id, data_field, impl_code } => {
                let start_line = code1.lines.len();
                let func_name = scope_entry_id_dynamic_key(method.scope_entry_id);
                code1.push(root_loc, format!("function {}() {{", func_name));
                code1.push(
                    root_loc,
                    format!("let {} = this.{};", scope_entry_id_dynamic_key(*self_scope_entry_id), data_field),
                );
                let body_var = render_code(&mut code1, impl_code);
                code1.push(root_loc, format!("return {};", body_var));
                code1.push(root_loc, "}".to_string());

                // Store compiled JS for later serialization
                let compiled = code1.lines[start_line..].join("\n");
                for ds in ex.definition_sets.borrow().iter() {
                    for dt_data in ds.data_types.borrow().iter() {
                        for m in dt_data.methods.borrow().iter() {
                            if m.scope_entry_id.get() == Some(method.scope_entry_id) {
                                *m.compiled_js.borrow_mut() = Some(compiled.clone());
                            }
                        }
                    }
                }
            },
            CompiledDataMethodBody::Precompiled(js) => {
                for line in js.lines() {
                    code1.push(root_loc, line.to_string());
                }
            },
        }
    }

    // Render main code
    if let Some(code) = stage1_code {
        let root_var = render_code(&mut code1, &code);
        code1.push(root_loc, format!("{};", root_var));
    } else {
        code1.push(root_loc, "null;".to_string());
    }

    // Register __stage v8 function Signature: __stage(tree, lift1, val1, lift2, val2,
    // ...) If lift is null, val is already a StagedExprCell external. If lift is a
    // StageLiftCell external, val is a raw v8 value to lift.
    {
        fn stage_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let tree_cell: Rc<expr::StagedExprCell> =
                from_v8_ref::<Rc<expr::StagedExprCell>>(&*args.get(0)).clone();
            let tree_value = tree_cell.value.borrow_mut().take().expect("stage tree already consumed");
            let tree = must_downcast_ref::<Expr>(tree_value.as_ref()).clone();

            enum StageLowerValue {
                Expr(Expr),
                Value(Rc<dyn Value>),
            }

            let mut values: Vec<StageLowerValue> = Vec::new();
            let mut i = 1;
            while i < args.length() {
                let lift_arg = args.get(i);
                let val_arg = args.get(i + 1);
                i += 2;
                if lift_arg.is_null_or_undefined() {
                    // Already a StagedExprCell external
                    let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*val_arg).clone();
                    let value = cell.value.borrow_mut().take().expect("stage lower value already consumed");
                    if (value.as_ref() as &dyn Any).is::<Expr>() {
                        let expr =
                            Rc::try_unwrap(downcast_value::<Expr>(value))
                                .ok()
                                .expect("Stage lower Expr: Rc not unique");
                        values.push(StageLowerValue::Expr(expr));
                    } else {
                        values.push(StageLowerValue::Value(value));
                    }
                } else {
                    // Lift the raw v8 value
                    let lift_cell: Rc<StageLiftCell> = from_v8_ref::<Rc<StageLiftCell>>(&*lift_arg).clone();
                    let lift_xx = ExprContext {
                        vx: lift_cell.vx.clone(),
                        import_state: lift_cell.import_state.clone(),
                        scope: lift_cell.scope.clone(),
                        target: lift_cell.target.clone(),
                    };
                    let value = lift_cell.lift.lift(&lift_xx, &*scope, val_arg).expect("__stage: lift failed");
                    values.push(StageLowerValue::Value(value));
                }
            }

            /// Clone an expression tree, replacing Lower nodes at net depth 1 (lower_depth -
            /// stage_depth == 1) with Expr values from the iterator.
            fn clone_with_lower_substitutions(
                expr: &Expr,
                sd: usize,
                ld: usize,
                values: &mut dyn Iterator<Item = StageLowerValue>,
            ) -> Expr {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            match values.next().expect("Not enough lower values for substitution") {
                                StageLowerValue::Expr(expr_val) => expr_val,
                                StageLowerValue::Value(value) => {
                                    let value_cell: Rc<RefCell<Option<Rc<dyn Value>>>> =
                                        Rc::new(RefCell::new(Some(value)));
                                    Expr {
                                        loc: expr.loc,
                                        r#type: Expr_::Literal(Rc::new(move || {
                                            value_cell.borrow_mut().take().expect("Lower literal consumed twice")
                                        })),
                                    }
                                },
                            }
                        } else {
                            Expr {
                                loc: expr.loc,
                                r#type: Expr_::Lower(
                                    ExprLower {
                                        expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd, new_ld, values)),
                                    },
                                ),
                            }
                        }
                    },
                    Expr_::Stage(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Stage(
                            ExprStage { expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd + 1, ld, values)) },
                        ),
                    },
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => expr.clone(),
                    Expr_::As(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::As(ExprAs {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            target: Rc::new(clone_with_lower_substitutions(&e.target, sd, ld, values)),
                        }),
                    },
                    Expr_::Call(c) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Call(ExprCall {
                            func: Rc::new(clone_with_lower_substitutions(&c.func, sd, ld, values)),
                            arg: Rc::new(clone_with_lower_substitutions(&c.arg, sd, ld, values)),
                        }),
                    },
                    Expr_::Bind(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Bind(ExprBind {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values)),
                        }),
                    },
                    Expr_::Set(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Set(ExprSet {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values)),
                        }),
                    },
                    Expr_::Field(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Field(ExprField {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            key: Rc::new(clone_with_lower_substitutions(&e.key, sd, ld, values)),
                        }),
                    },
                    Expr_::Block(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Block(
                            ExprBlock {
                                exprs: e
                                    .exprs
                                    .iter()
                                    .map(|sub| clone_with_lower_substitutions(sub, sd, ld, values))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Tuple(t) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Tuple(
                            ExprTuple {
                                elements: t
                                    .elements
                                    .iter()
                                    .map(|e| clone_with_lower_substitutions(e, sd, ld, values))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Record(r) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Record(expr::ExprRecord { entries: r.entries.iter().map(|(k, v)| {
                            (
                                clone_with_lower_substitutions(k, sd, ld, values),
                                clone_with_lower_substitutions(v, sd, ld, values),
                            )
                        }).collect() }),
                    },
                    Expr_::If(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::If(expr::ExprIf {
                            conditions: e.conditions.iter().map(|b| expr::ExprConditionBranch {
                                condition: clone_with_lower_substitutions(&b.condition, sd, ld, values),
                                body: clone_with_lower_substitutions(&b.body, sd, ld, values),
                            }).collect(),
                            else_: e
                                .else_
                                .as_ref()
                                .map(|el| Rc::new(clone_with_lower_substitutions(el, sd, ld, values))),
                        }),
                    },
                    Expr_::OperatorLt(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorLt(expr::ExprOperatorLt {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values)),
                        }),
                    },
                    Expr_::OperatorAdd(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorAdd(expr::ExprOperatorAdd {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            reference: Rc::new(clone_with_lower_substitutions(&e.reference, sd, ld, values)),
                        }),
                    },
                    Expr_::Foreach(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Foreach(ExprForeach {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            element_key: Rc::new(clone_with_lower_substitutions(&e.element_key, sd, ld, values)),
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values)),
                        }),
                    },
                    Expr_::Match(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Match(ExprMatch {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values)),
                            element_key: Rc::new(clone_with_lower_substitutions(&e.element_key, sd, ld, values)),
                            cases: e.cases.iter().map(|c| ExprMatchBranch {
                                condition: clone_with_lower_substitutions(&c.condition, sd, ld, values),
                                body: clone_with_lower_substitutions(&c.body, sd, ld, values),
                            }).collect(),
                            fallback: e
                                .fallback
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values))),
                        }),
                    },
                }
            }

            let new_tree = clone_with_lower_substitutions(&tree, 0, 0, &mut values.into_iter());
            let result_cell =
                Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(new_tree) as Rc<dyn Value>)) });
            let ext = to_v8(scope, result_cell);
            rv.set(ext.into());
        }

        let stage_fn = v8::Function::builder(stage_callback).build(dynamic_scope).unwrap();
        dynamic_scope
            .get_current_context()
            .global(dynamic_scope)
            .set(
                dynamic_scope,
                v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(STAGE_FN_SCOPE_ID)).unwrap().cast(),
                stage_fn.into(),
            );
    }

    // Register __record_to_type and __tuple_to_type mortar functions. These convert
    // JS objects/arrays into Rc`<dyn Type>` externals at runtime.
    {
        /// Recursively convert a v8 value (external, object, or array) to Rc`<dyn Type>`.
        fn v8_value_to_type(scope: &mut v8::PinnedRef<v8::HandleScope>, value: v8::Local<v8::Value>) -> Rc<dyn Type> {
            if value.is_external() {
                return from_v8_ref::<Rc<dyn Type>>(&*value).clone();
            }
            if value.is_array() {
                let arr = v8::Local::<v8::Array>::try_from(value).unwrap();
                let len = arr.length();
                let mut elements = Vec::with_capacity(len as usize);
                for i in 0 .. len {
                    let elem = arr.get_index(scope, i).unwrap();
                    elements.push(TupleElement::Mortar(v8_value_to_type(scope, elem)));
                }
                return Rc::new(Tuple {
                    base: None,
                    elements,
                });
            }
            if value.is_object() && !value.is_null_or_undefined() {
                let obj = v8::Local::<v8::Object>::try_from(value).unwrap();
                let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
                let mut fields = std::collections::BTreeMap::new();
                for i in 0 .. keys.length() {
                    let key = keys.get_index(scope, i).unwrap();
                    let key_str = key.to_string(scope).unwrap().to_rust_string_lossy(scope);
                    let val = obj.get(scope, key).unwrap();
                    let const_key =
                        ConstKey::from_str(
                            &key_str,
                        ).expect(&format!("Invalid ConstKey in v8 object property: {}", key_str));
                    fields.insert(const_key, RecordField::Mortar(v8_value_to_type(scope, val)));
                }
                return Rc::new(Record {
                    base: None,
                    fields,
                });
            }
            panic!("v8_value_to_type: unexpected v8 value type");
        }

        fn record_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let type_ = v8_value_to_type(scope, args.get(0));
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        fn tuple_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let type_ = v8_value_to_type(scope, args.get(0));
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        let record_to_type_fn = v8::Function::builder(record_to_type_callback).build(dynamic_scope).unwrap();
        dynamic_scope
            .get_current_context()
            .global(dynamic_scope)
            .set(
                dynamic_scope,
                v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(RECORD_TO_TYPE_FN_SCOPE_ID))
                    .unwrap()
                    .cast(),
                record_to_type_fn.into(),
            );
        let tuple_to_type_fn = v8::Function::builder(tuple_to_type_callback).build(dynamic_scope).unwrap();
        dynamic_scope
            .get_current_context()
            .global(dynamic_scope)
            .set(
                dynamic_scope,
                v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(TUPLE_TO_TYPE_FN_SCOPE_ID))
                    .unwrap()
                    .cast(),
                tuple_to_type_fn.into(),
            );
    }

    // Set externals as globals before running the script
    eprintln!("externals count: {}", ex.externals.borrow().len());
    for (id, value) in ex.externals.borrow().iter() {
        let ext_type = match value {
            External::StagedExprCell(_) => "StagedExprCell",
            External::StageLiftCell(_) => "StageLiftCell",
            External::Type(t) => {
                eprintln!("  external {:?} = Type({})", id, t.desc());
                "Type"
            },
        };
        let _ = ext_type;
        let key = v8::String::new(dynamic_scope, &scope_entry_id_dynamic_key(*id)).unwrap().cast();
        match value {
            External::StagedExprCell(cell) => {
                let ext = to_v8(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::StageLiftCell(cell) => {
                let ext = to_v8(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::Type(type_) => {
                let ext = rc_type_to_v8(dynamic_scope, type_.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
        }
    }
    let dynamic_source = code1.lines.join("\n");
    eprintln!("source: [[{}]]", dynamic_source);
    cix.gx.generated_mortar_scripts.lock().unwrap().push(dynamic_source.clone());
    let script =
        v8::Script::compile(dynamic_scope, v8::String::new(dynamic_scope, &dynamic_source).unwrap(), None).unwrap();
    match script.run(dynamic_scope) {
        Some(stage2) => {
            let value_from_js = aspect_lift_dynamic_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let value: Rc<dyn Value> = value_from_js.lift(&xx, &*dynamic_scope, stage2).map_err(|e| vec![ExprErr {
                loc: root_loc,
                errors: vec![ExprSubErr::Opaque(e)],
            }])?;
            let logs = dynamic_scope.get_slot::<V8LogStore>().unwrap().logs.take();
            Ok(EvalResult {
                value,
                logs,
            })
        },
        None => {
            let text;
            match dynamic_scope.exception() {
                Some(e) => {
                    text = e.to_rust_string_lossy(&dynamic_scope);
                },
                None => {
                    text = format!("Script failed to return, but no exception captured (compiler bug)");
                },
            }
            match dynamic_scope.message().and_then(|e| e.get_line_number(&dynamic_scope)) {
                Some(dynamic_line) => {
                    Err(vec![ExprErr {
                        loc: *code1.mapping.get(&(dynamic_line as usize)).unwrap_or(&root_loc),
                        errors: vec![ExprSubErr::Opaque(text)],
                    }])
                },
                None => {
                    Err(vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque(text)],
                    }])
                },
            }
        },
    }
}

struct EvalResult {
    logs: Vec<LogEntry>,
    value: Rc<dyn Value>,
}

fn extract_const_key(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> Result<ConstKey, String> {
    let vtype = v.type_(xx, loc);
    if let Some(a) = aspect_string_const_get_maybe(xx, vtype.as_ref()) {
        return Ok(ConstKey::String(a.get(v)));
    }
    return Err(format!("TODO value type [X] must expose const key aspect [LIST] but doesn't"));
}

fn from_v8_any(value: &v8::Value) -> &dyn std::any::Any {
    assert!(value.is_external(), "from_v8_any: expected v8::External");
    let ext = unsafe {
        &*(value as *const v8::Value as *const v8::External)
    };
    let ptr = ext.value() as *const Box<dyn std::any::Any>;
    unsafe {
        &**ptr
    }
}

/// Get a reference to the value inside a `v8::External` created by `to_v8`.
pub(crate) fn from_v8_ref<T: 'static>(value: &v8::Value) -> &T {
    must_downcast_ref::<T>(from_v8_any(value))
}

#[derive(Default)]
struct JsSourceBuilder {
    lines: Vec<String>,
    mapping: HashMap<usize, QExprId>,
    next_expr_id: usize,
}

impl JsSourceBuilder {
    fn next_expr_id(&mut self) -> usize {
        let id = self.next_expr_id;
        self.next_expr_id += 1;
        id
    }

    fn push(&mut self, id: QExprId, line: String) {
        let dynamic_num = self.lines.len() + 1;
        self.mapping.insert(dynamic_num, id);
        self.lines.push(line);
    }
}

/// Extract a clone of `Rc<dyn Type>` from a `v8::External`. Only operates on
/// `dyn Type` — for concrete `Rc<T>`, use `from_v8_ref` directly.
pub(crate) fn rc_type_from_v8(value: &v8::Value) -> Rc<dyn Type> {
    from_v8_ref::<Rc<dyn Type>>(value).clone()
}

/// Store an `Rc<dyn Type>` as a `v8::External`. Only operates on `dyn Type` — for
/// concrete `Rc<T>`, use `to_v8` directly.
pub(crate) fn rc_type_to_v8<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Type>) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

fn render_code(lines: &mut JsSourceBuilder, code: &CodeDynamic) -> String {
    let out = format!("{}{}", EXPR_PREFIX, lines.next_expr_id());
    lines.push(code.loc, format!("let {};", out));
    match &code.type_ {
        CodeDynamicType::Block(c) => {
            for entry in &c.entries {
                match entry {
                    CodeDynamicBlockEntry::Bind(b) => {
                        let bind_key = scope_entry_id_dynamic_key(b.base);
                        lines.push(code.loc, format!("let {};", bind_key));
                        lines.push(code.loc, "{".to_string());
                        let val_var = render_code(lines, &b.value);
                        lines.push(code.loc, format!("{} = {};", bind_key, val_var));
                        lines.push(code.loc, "}".to_string());
                    },
                    CodeDynamicBlockEntry::Code(sub) => {
                        lines.push(code.loc, "{".to_string());
                        let sub_var = render_code(lines, sub);
                        lines.push(code.loc, format!("{} = {};", out, sub_var));
                        lines.push(code.loc, "}".to_string());
                    },
                }
            }
        },
        CodeDynamicType::Literal(c) => {
            lines.push(code.loc, format!("{} = {};", out, serde_json::to_string(c).unwrap()));
        },
        CodeDynamicType::BindingUse(c) => {
            lines.push(code.loc, format!("{} = {};", out, scope_entry_id_dynamic_key(c.id)));
        },
        CodeDynamicType::Call(c) => {
            // Special case: method call preserves `this` binding
            if let CodeDynamicType::FieldUse(f) = &c.func.type_ {
                let base_var = render_code(lines, &f.base);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(
                    code.loc,
                    format!(
                        "{} = {}[{}]({});",
                        out,
                        base_var,
                        serde_json::to_string(&f.field).unwrap(),
                        arg_vars.join(",")
                    ),
                );
            } else {
                let func_var = render_code(lines, &c.func);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(code.loc, format!("{} = ({})({});", out, func_var, arg_vars.join(",")));
            }
        },
        CodeDynamicType::FieldUse(dynamic_code_field) => {
            let base = render_code(lines, &dynamic_code_field.base);
            lines.push(
                code.loc,
                format!("{} = {}[{}];", out, base, serde_json::to_string(&dynamic_code_field.field).unwrap()),
            );
        },
        CodeDynamicType::BindingSet(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{} = {};", scope_entry_id_dynamic_key(c.base), val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        CodeDynamicType::FieldSet(c) => {
            let base_var = render_code(lines, &c.base);
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{}[{}] = {};", base_var, serde_json::to_string(&c.key).unwrap(), val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        CodeDynamicType::NewObject(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let val_var = render_code(lines, value);
                fields_str.push(format!("{}: {}", serde_json::to_string(key).unwrap(), val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        CodeDynamicType::NewMap(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let key_var = render_code(lines, key);
                let val_var = render_code(lines, value);
                fields_str.push(format!("[{}]: {}", key_var, val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        CodeDynamicType::NewArray(c) => {
            let mut elem_vars = vec![];
            for elem in &c.elements {
                elem_vars.push(render_code(lines, elem));
            }
            lines.push(code.loc, format!("{} = [{}];", out, elem_vars.join(", ")));
        },
        CodeDynamicType::If(c) => {
            for (i, (cond, body)) in c.conditions.iter().enumerate() {
                let cond_var = render_code(lines, cond);
                if i == 0 {
                    lines.push(code.loc, format!("if ({}) {{", cond_var));
                } else {
                    lines.push(code.loc, format!("}} else if ({}) {{", cond_var));
                }
                let body_var = render_code(lines, body);
                lines.push(code.loc, format!("{} = {};", out, body_var));
            }
            if let Some(else_) = &c.else_ {
                lines.push(code.loc, "} else {".to_string());
                let else_var = render_code(lines, else_);
                lines.push(code.loc, format!("{} = {};", out, else_var));
            }
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::OperatorLt(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} < {});", out, base_var, ref_var));
        },
        CodeDynamicType::OperatorAdd(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} + {});", out, base_var, ref_var));
        },
        CodeDynamicType::Foreach(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("for (let {} of {}) {{", elem_key, base_var));
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::ForeachMap(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("for (let __entry of Object.entries({})) {{", base_var));
            lines.push(
                code.loc,
                format!(
                    "let {} = {{ {}: __entry[0], {}: __entry[1] }};",
                    elem_key,
                    serde_json::to_string(&ConstKey::String("key".to_string()).to_str()).unwrap(),
                    serde_json::to_string(&ConstKey::String("value".to_string()).to_str()).unwrap(),
                ),
            );
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::Match(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("{{ let [{}, {}] = Object.entries({})[0];", TAG_VAR, elem_key, base_var));
            lines.push(code.loc, format!("switch ({}) {{", TAG_VAR));
            for case in &c.cases {
                lines.push(code.loc, format!("case \"{}\": {{", case.dynamic_tag));
                let body_var = render_code(lines, &case.body);
                lines.push(code.loc, format!("{} = {};", out, body_var));
                lines.push(code.loc, "break; }".to_string());
            }
            if let Some(fallback) = &c.fallback {
                lines.push(code.loc, "default: {".to_string());
                let fb_var = render_code(lines, fallback);
                lines.push(code.loc, format!("{} = {};", out, fb_var));
                lines.push(code.loc, "}".to_string());
            }
            lines.push(code.loc, "} }".to_string());
        },
    }
    return out;
}

pub(crate) fn scope_entry_id_dynamic_key(e: ScopeEntryId) -> String {
    match e {
        ScopeEntryId::Local(id) => format!("{}{}", BINDING_PREFIX, id),
        ScopeEntryId::Builtin(id) => format!("{}{}", BUILTIN_PREFIX, id),
        ScopeEntryId::Def(import_id, def_set_id, entry_id) => {
            format!("{}{}_{}_{}", DEF_PREFIX, import_id.0, def_set_id.0, entry_id)
        },
    }
}

pub fn setup_v8() {
    static V: OnceLock<()> = OnceLock::new();
    V.get_or_init(|| {
        let platform = v8::new_default_platform(0, false).make_shared();
        v8::V8::initialize_platform(platform);
        v8::V8::initialize();
        return ();
    });
}

/// Holds an AspectLiftDynamic, stored as a v8::External so __stage can lift raw
/// values.
pub(crate) struct StageLiftCell {
    pub import_state: Rc<Cell<ImportStateId>>,
    pub lift: Box<dyn AspectLiftDynamic>,
    pub scope: Rc<Scope>,
    pub target: Rc<dyn Target>,
    pub vx: Rc<EvalContext>,
}

pub(crate) struct TargetBindSub {
    pub data: Code,
    pub type_: Rc<dyn Type>,
}
pub(crate) struct TargetDynamic;

impl Target for TargetDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, sub: TargetBindSub) -> (Code, ScopeEntryId) {
        let js =
            sub
                .data
                .cast::<CodeDynamic>()
                .into_res()
                .expect("TargetDynamic bind: expected CodeDynamic")
                .unwrap()
                .clone();
        let js_key = ScopeEntryId::Local(xx.vx.next_local_id.next());
        let code = Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Block(
                    CodeDynamicBlock { entries: vec![CodeDynamicBlockEntry::Bind(CodeDynamicBind {
                        base: js_key,
                        value: Box::new(js),
                    })] },
                ),
            })),
        };
        (code, js_key)
    }

    fn bind_sub_record(&self, loc: QExprId, carry: Code, fields: Vec<(ConstKey, TargetBindSub)>) -> TargetBindSub {
        let mut js_fields = Vec::new();
        let mut mortar_fields = std::collections::BTreeMap::new();
        for (key, sub) in fields {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_record: expected CodeDynamic")
                    .unwrap()
                    .clone();
            js_fields.push((key.to_str(), cd));
            mortar_fields.insert(key, value::record::RecordField::Mortar(sub.type_));
        }
        let record_type = Rc::new(Record {
            base: None,
            fields: mortar_fields,
        }) as Rc<dyn Type>;
        let js = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: js_fields }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(js)),
            },
            type_: record_type,
        }
    }

    fn bind_sub_tuple(&self, loc: QExprId, carry: Code, elements: Vec<TargetBindSub>) -> TargetBindSub {
        let mut js_elements = Vec::new();
        let mut mortar_elements = Vec::new();
        for sub in elements {
            let cd =
                sub
                    .data
                    .cast::<CodeDynamic>()
                    .into_res()
                    .expect("TargetDynamic bind_sub_tuple: expected CodeDynamic")
                    .unwrap()
                    .clone();
            js_elements.push(cd);
            mortar_elements.push(TupleElement::Mortar(sub.type_));
        }
        let tuple_type = Rc::new(Tuple {
            base: None,
            elements: mortar_elements,
        }) as Rc<dyn Type>;
        let js = wrap_with_carry(loc, carry, CodeDynamic {
            loc: loc,
            type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: js_elements }),
        });
        TargetBindSub {
            data: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(js)),
            },
            type_: tuple_type,
        }
    }

    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code> {
        let mut dynamic_entries = vec![];
        let mut errors = vec![];
        for entry in &entries {
            match entry.cast::<CodeDynamic>().into_res() {
                Ok(Some(js)) => {
                    match &js.type_ {
                        CodeDynamicType::Block(block) if
                            !block.entries.is_empty() &&
                                block.entries.iter().all(|e| matches!(e, CodeDynamicBlockEntry::Bind(_))) => {
                            dynamic_entries.extend(block.entries.clone());
                        },
                        _ => dynamic_entries.push(CodeDynamicBlockEntry::Code(js.clone())),
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

    fn code_to_bind_sub(&self, _loc: QExprId, code: Code, type_: Rc<dyn Type>) -> EResult<TargetBindSub> {
        EResult::ok(TargetBindSub {
            data: code,
            type_: type_,
        })
    }

    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes {
        fn cv_to_dynamic(erb: &mut Erb, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> Result<CodeDynamic, ()> {
            let vtype = cv.value.type_(xx, loc);
            let consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, vtype.as_ref()))?;
            erb.must_er(consume.to_dynamic(xx, loc, cv))
        }

        return er_seq_cv!(loc, erb, {
            let mut dynamic_conditions = Vec::new();
            for (cond, body) in conditions {
                let cond_js = cv_to_dynamic(&mut erb, xx, loc, cond)?;
                let body_cv = erb.must_er(const_eval(xx, body))?;
                let body_js = cv_to_dynamic(&mut erb, xx, loc, body_cv)?;
                dynamic_conditions.push((cond_js, body_js));
            }
            let dynamic_else = if let Some(else_expr) = else_ {
                let else_cv = erb.must_er(const_eval(xx, else_expr))?;
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
            let null_stack = aspect_stack_value_dynamic_get(xx, loc, &TypeNullDynamic as &dyn Type);
            let null_stack = erb.must_er(null_stack)?;
            return Ok(null_stack.to_value(loc, if_js));
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
            let base_type = base.value.type_(xx, loc);
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, base_type.as_ref()))?;
            let base_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, base))?;
            let element_scope_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            let mut dynamic_cases = Vec::new();
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
                };
                let body_cv = erb.must_er(const_eval(&child_xx, body_expr))?;
                let body_type = body_cv.value.type_(&child_xx, loc);
                let body_consume = erb.must_er(aspect_consume_dynamic_get(&child_xx, loc, body_type.as_ref()))?;
                let body_js = erb.must_er(body_consume.to_dynamic(&child_xx, loc, body_cv))?;
                dynamic_cases.push(CodeDynamicMatchBranch {
                    dynamic_tag: tag.clone(),
                    body: body_js,
                });
            }
            let dynamic_fallback = if let Some(fallback_expr) = fallback {
                let fb_cv = erb.must_er(const_eval(xx, fallback_expr))?;
                let fb_type = fb_cv.value.type_(xx, loc);
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
            let null_stack = aspect_stack_value_dynamic_get(xx, loc, &TypeNullDynamic as &dyn Type);
            let null_stack = erb.must_er(null_stack)?;
            return Ok(null_stack.to_value(loc, match_js));
        });
    }

    fn maybe_bind_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        let vtype = cv.value.type_(xx, loc);
        if let Some(bind_dyn) = aspect_bind_dynamic_get_maybe(xx, vtype.as_ref()) {
            return bind_dyn.bind_dynamic(xx, loc, cv);
        }
        EResult::ok(None)
    }

    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes {
        // Store tree as external
        let tree_cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(tree) as Rc<dyn Value>)) });
        let tree_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((tree_id, External::StagedExprCell(tree_cell)));
        let tree_js = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: tree_id }),
        };

        // Build args for __stage(tree, lift1, val1, lift2, val2, ...). For each lower
        // value, pass a (lift, value) pair:
        //
        // * Code_::None: lift=null, value=StagedExprCell external
        //
        // * Code_::Dyn:  lift=StageLiftCell external, value=raw v8 result __stage handles the
        //   lifting internally.
        let mut stage_args = vec![tree_js];
        return er_seq_cv!(loc, erb, {
            for cv in lower_values {
                let vtype = cv.value.type_(xx, loc);
                match cv.code.code {
                    Code_::None => {
                        let cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(cv.value)), });
                        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
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
                        let lift = erb.must_er(aspect_lift_dynamic_get(xx, loc, vtype.as_ref()))?;
                        let lift_cell = Rc::new(StageLiftCell {
                            lift,
                            vx: xx.vx.clone(),
                            import_state: xx.import_state.clone(),
                            scope: xx.scope.clone(),
                            target: xx.target.clone(),
                        });
                        let lift_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
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
                value: Rc::new(value::value_dynamic::ValueDynamic {
                    source: value::value_dynamic::ValueDynamicSource::Expr,
                    type_: Rc::new(value::type_expr::TypeExprStagedPlaceholder),
                }),
            });
        });
    }
}

/// Store a value as a `v8::External`. The value is boxed as `dyn Any` so that
/// `from_v8_ref` can safely downcast to any type, including trait object wrappers
/// like `Rc<dyn Type>`. A guaranteed finalizer drops the value when the isolate is
/// disposed.
pub(crate) fn to_v8<'s, T: 'static>(scope: &mut v8::PinScope<'s, '_, ()>, value: T) -> v8::Local<'s, v8::External> {
    let any: Box<dyn std::any::Any> = Box::new(value);
    let ptr = Box::into_raw(Box::new(any)) as *mut std::ffi::c_void;
    let ext = v8::External::new(&*scope, ptr);
    let weak = v8::Weak::with_guaranteed_finalizer(&mut *scope, ext, Box::new(move || unsafe {
        drop(Box::from_raw(ptr as *mut Box<dyn std::any::Any>));
    }));
    scope.get_slot::<V8RcStore>().unwrap().weaks.borrow_mut().push(weak);
    ext
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let source_type = source.value.type_(xx, loc);
        let dest;
        if source_type.can_cast_to(xx, loc, dest_type) {
            dest = erb.must_er(source_type.cast_to(xx, loc, source, dest_type))?;
        } else if dest_type.can_cast_from(xx, loc, source_type.as_ref()) {
            dest = erb.must_er(dest_type.cast_from(xx, loc, source))?;
        } else {
            return erb.err_new(
                format!("Incompatible types: source {} cannot convert to dest {}", source_type.desc(), dest_type.desc())
            );
        }
        return Ok(dest);
    });
}

pub(crate) struct V8DefSetCounter {
    pub(crate) counter: Counter,
    pub(crate) import_id: ImportStateId,
}

pub(crate) struct V8LogStore {
    pub(crate) logs: RefCell<Vec<LogEntry>>,
}

// --- Rc`<T>` ↔ v8::External helpers ---
/// Weak handle storage, set as an isolate slot. During dispose_annex(), slots are
/// cleared first (Weak handles dropped — but isolate ptr is already null so
/// finalizers can't be cancelled), then finalizer_map is drained, running our
/// guaranteed finalizer callbacks.
pub(crate) struct V8RcStore {
    weaks: RefCell<Vec<v8::Weak<v8::External>>>,
}

pub(crate) struct V8UniqueCounter {
    pub(crate) compile_id: CompileId,
    pub(crate) counter: Counter,
}
