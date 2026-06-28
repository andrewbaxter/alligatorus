#[macro_use]
pub mod push;
#[macro_use]
pub mod util;
pub mod aspect;
pub mod builtin_tree;
pub mod code_value_extern;
pub mod definition_set;
pub mod dynamic;
pub mod expr;
pub mod expr_artifact;
pub mod import_install;
pub mod pull_scope_dispatch;
pub mod pull_type_dispatch;
pub mod value;

use {
    core::mem,
    crate::{
        artifact::import_artifact,
        eval::dynamic::{
            code::{
                BINDING_PREFIX,
                BUILTIN_PREFIX,
                CodeDynamic,
                CodeDynamicBlock,
                CodeDynamicBlockEntry,
                CodeDynamicType,
                DEF_PREFIX,
                EXPR_PREFIX,
                TAG_VAR,
            },
            target::TargetDynamic,
        },
        invariant::must_downcast_ref,
        langhelp::{
            BindingId,
            Code,
            CodeDyn,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
            CompileRes,
            CompileResResult,
            ConstKey,
            Counter,
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
            Type,
            Value,
            downcast_value,
        },
        r#gen::{
            Binding,
            Target,
            aspect_artifact_get,
            aspect_bind_get,
            aspect_bool_const_get_maybe,
            aspect_consume_dynamic_get,
            aspect_field_get,
            aspect_function_get,
            aspect_initial_bind_get,
            aspect_iterable_get,
            aspect_lift_get,
            aspect_operator_add_get,
            aspect_operator_eq_get,
            aspect_operator_lt_get,
            aspect_post_deserialize_get,
            aspect_set_get,
            aspect_string_const_get_maybe,
            aspect_type_get,
            aspect_union_get,
            gen_builtin_tree,
        },
        source_module_to_module,
        syntax::SourceSpecAbsolute,
    },
    expr::{
        Expr,
        ExprStage,
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
            RwLock,
        },
    },
    v8::tc_scope,
    value::{
        type_record::{
            Record,
            RecordField,
        },
        type_tuple::{
            Tuple,
            TupleElement,
        },
        value_null::r#const::ValueNullConst,
    },
};

const KEY_BUILTINS: &str = "builtins";
pub(crate) const PANIC_MARKER_KEY: &str = "__panic__";

pub(crate) fn binding_id_dynamic_key(e: BindingId) -> String {
    match e {
        BindingId::Local(id) => format!("{}{}", BINDING_PREFIX, id),
        BindingId::Builtin(id) => format!("{}{}", BUILTIN_PREFIX, id),
        BindingId::Def(def_set_id, entry_id) => {
            format!("{}{}_{}_{}_{}", DEF_PREFIX, (def_set_id.0).0.0, (def_set_id.0).1.0, def_set_id.1, entry_id,)
        },
    }
}

pub(crate) fn body_builtin_scope(xx: &ExprContext) -> HashMap<ConstKey, Box<dyn Binding>> {
    let root_binding =
        xx
            .vx
            .ix
            .builtin_scope_root_binding
            .borrow()
            .clone()
            .expect("body_builtin_scope called before init_builtin_scope");
    let mut scope_entries = HashMap::<ConstKey, Box<dyn Binding>>::new();
    scope_entries.insert(
        ConstKey::String(KEY_BUILTINS.to_string()),
        Box::new(crate::eval::RcBindingWrap(root_binding)) as Box<dyn Binding>,
    );
    scope_entries
}

pub(crate) fn code_uses_binding(code: &CodeDynamic, target_id: BindingId) -> bool {
    match &code.type_ {
        CodeDynamicType::BindingUse(b) => b.id == target_id,
        CodeDynamicType::BindingSet(b) => code_uses_binding(b.value.as_ref(), target_id),
        CodeDynamicType::Block(b) => b.entries.iter().any(|e| match e {
            CodeDynamicBlockEntry::Code(c) => code_uses_binding(c, target_id),
            CodeDynamicBlockEntry::Bind(b) => code_uses_binding(&b.value, target_id),
        }),
        CodeDynamicType::Call(c) => code_uses_binding(&c.func, target_id) ||
            c.args.iter().any(|a| code_uses_binding(a, target_id)),
        CodeDynamicType::FieldSet(f) => code_uses_binding(&f.base, target_id) ||
            code_uses_binding(&f.value, target_id),
        CodeDynamicType::FieldUse(f) => code_uses_binding(&f.base, target_id),
        CodeDynamicType::Foreach(f) => code_uses_binding(&f.base, target_id) ||
            code_uses_binding(&f.body, target_id),
        CodeDynamicType::ForeachMap(f) => code_uses_binding(&f.base, target_id) ||
            code_uses_binding(&f.body, target_id),
        CodeDynamicType::If(i) => i
            .conditions
            .iter()
            .any(|(c, b)| code_uses_binding(c, target_id) || code_uses_binding(b, target_id)) ||
            i.else_.as_ref().map(|e| code_uses_binding(e, target_id)).unwrap_or(false),
        CodeDynamicType::Literal(_) => false,
        CodeDynamicType::Match(m) => code_uses_binding(&m.base, target_id) ||
            m.cases.iter().any(|c| code_uses_binding(&c.body, target_id)) ||
            m.fallback.as_ref().map(|f| code_uses_binding(f, target_id)).unwrap_or(false),
        CodeDynamicType::NewArray(n) => n.elements.iter().any(|e| code_uses_binding(e, target_id)),
        CodeDynamicType::NewMap(n) => n
            .fields
            .iter()
            .any(|(k, v)| code_uses_binding(k, target_id) || code_uses_binding(v, target_id)),
        CodeDynamicType::NewObject(n) => n.fields.iter().any(|(_, v)| code_uses_binding(v, target_id)),
        CodeDynamicType::OperatorAdd(o) => code_uses_binding(&o.base, target_id) ||
            code_uses_binding(&o.reference, target_id),
        CodeDynamicType::OperatorEq(o) => code_uses_binding(&o.base, target_id) ||
            code_uses_binding(&o.reference, target_id),
        CodeDynamicType::OperatorLt(o) => code_uses_binding(&o.base, target_id) ||
            code_uses_binding(&o.reference, target_id),
        CodeDynamicType::SubscriptUse(s) => code_uses_binding(&s.base, target_id) ||
            code_uses_binding(&s.key, target_id),
        CodeDynamicType::Throw(t) => code_uses_binding(&t.value, target_id),
        CodeDynamicType::Try(t) => code_uses_binding(&t.body, target_id) ||
            t.catch.as_ref().map(|c| code_uses_binding(&c.body, target_id)).unwrap_or(false) ||
            t.finally.as_ref().map(|f| code_uses_binding(f, target_id)).unwrap_or(false),
    }
}

pub(crate) struct CodePull {
    pub instance: v8::Global<v8::Object>,
}

impl CodeDyn for CodePull { }

pub(crate) fn compile_importid(
    fx: &Arc<GlobalContext>,
    import_state: ImportStateId,
    source: Module,
    compile_path: Vec<CompileId>,
) -> CompileRes {
    let mut root_expr = match source {
        Module::V1(e) => e,
    };
    let root_loc = root_expr.loc;
    let source_id = root_loc.0;
    let mut compile_path = compile_path;
    compile_path.push(CompileId(import_state, source_id));
    let cix = Rc::new(CompileImportContext {
        gx: fx.clone(),
        compile_path: compile_path,
        deserialized_artifacts: RefCell::new(HashMap::new()),
        isolate: Rc::new(RefCell::new(v8::Isolate::new(Default::default()))),
        import_state: import_state,
        installed_def_sets: RefCell::new(std::collections::HashSet::new()),
        def_set_counter: Counter::default(),
        dynamic_context: RefCell::new(None),
        builtins_registered: Cell::new(false),
        source_id: source_id,
        builtin_scope_root_binding: RefCell::new(None),
    });

    fn max_lower_excess(expr: &Expr, sd: usize, ld: usize) -> usize {
        match &expr.r#type {
            Expr_::Lower(e) => {
                let new_ld = ld + 1;
                let excess = new_ld.saturating_sub(sd);
                excess.max(max_lower_excess(&e.expr, sd, new_ld))
            },
            Expr_::Stage(e) => max_lower_excess(&e.expr, sd + 1, ld),
            Expr_::Graft(e) => match e {
                expr::ExprGraft::Pending(inner) => max_lower_excess(inner, sd, ld),
                expr::ExprGraft::Evaluated(_) => 0,
            },
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
            Expr_::Seq(e) => {
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
            Expr_::OperatorEq(e) => {
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
            Expr_::Try(e) => {
                let mut m = max_lower_excess(&e.body, sd, ld);
                if let Some(c) = &e.catch {
                    m =
                        m
                            .max(max_lower_excess(&c.binding_name, sd, ld))
                            .max(max_lower_excess(&c.exception_type, sd, ld))
                            .max(max_lower_excess(&c.body, sd, ld));
                }
                if let Some(f) = &e.finally {
                    m = m.max(max_lower_excess(f, sd, ld));
                }
                m
            },
        }
    }

    let excess = max_lower_excess(&root_expr, 0, 0);
    for _ in 0 .. excess {
        root_expr = Expr {
            loc: root_loc,
            r#type: Expr_::Stage(ExprStage { expr: Rc::new(root_expr) }),
        };
    }
    let mut all_logs = Vec::new();
    loop {
        let is_stage = matches!(&root_expr.r#type, Expr_::Stage(_));
        let eval_result = match eval(&cix, import_state, &root_expr) {
            Ok(v) => v,
            Err(errors) => return CompileRes {
                logs: all_logs,
                generated_dynamic_scripts: vec![],
                result: CompileResResult::Err(errors),
            },
        };
        let eval_value = eval_result.value;
        all_logs.extend(eval_result.logs);
        if !is_stage {
            let logs = all_logs;
            let ex = Rc::new(EvalContext {
                ix: cix.clone(),
                next_local_id: Default::default(),
                externals: RefCell::new(Vec::new()),
                dynamic_scope: std::cell::Cell::new(None),
                definition_sets: RefCell::new(std::collections::HashMap::new()),
            });
            let xx = Rc::new(ExprContext {
                vx: ex,
                import_state: Rc::new(Cell::new(import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                target: Rc::new(TargetDynamic),
                current_exception_type: Some(Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic)),
            });
            let value_type = match eval_value.type_(&xx, root_loc).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_dynamic_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let value_serial = match aspect_artifact_get(&xx, root_loc, value_type.as_ref()).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_dynamic_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            let value = match value_serial.exporter().export(&xx, root_loc, eval_value.as_exporter_any()).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    generated_dynamic_scripts: vec![],
                    result: CompileResResult::Err(e),
                },
            };
            return CompileRes {
                logs,
                generated_dynamic_scripts: mem::take(&mut *fx.generated_dynamic_scripts.lock().unwrap()),
                result: CompileResResult::Ok(value),
            };
        }
        let next_expr =
            Rc::try_unwrap(downcast_value::<Expr>(eval_value)).ok().expect("Stage eval Expr: Rc not unique");
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
                    value: Some(p.consume()),
                }),
                errors: None,
            };
        },
        Expr_::ScopeUse(p) => return er_seq_cv!(expr.loc, erb, {
            let key = erb.must_er(const_eval(xx, p.key.as_ref()))?;
            let constkey = erb.must_res(extract_const_key(xx, expr.loc, key.value.as_deref().unwrap()))?;
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
            return Ok(binding.use_(xx, expr.loc, key.code));
        }),
        Expr_::Call(p) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                func = const_eval(xx, p.func.as_ref()),
                arg = const_eval(xx, p.arg.as_ref()),
            });
            let func_type = erb.must_er(func.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_func = erb.must_er(aspect_function_get(xx, expr.loc, func_type.as_ref()))?;
            let callee_exc = aspect_func.exception_type();
            let v = erb.must_er(aspect_func.call(xx, expr.loc, func, arg))?;
            let v = erb.must_er(maybe_wrap_call_exception(xx, expr.loc, callee_exc, v))?;
            return Ok(v);
        }),
        Expr_::Set(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                value = const_eval(xx, e.value.as_ref()),
            });
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let base_set = erb.must_er(aspect_set_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(base_set.set(xx, expr.loc, base, value))?;
            return Ok(v);
        }),
        Expr_::Field(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                field = const_eval(xx, e.key.as_ref()),
            });
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_field = erb.must_er(aspect_field_get(xx, expr.loc, base_type.as_ref()))?;
            let extracted_key = erb.must_res(extract_const_key(xx, expr.loc, field.value.as_deref().unwrap()))?;
            let mut sibling_drops: Vec<Code> = Vec::new();
            let field_keys = aspect_field.field_keys(xx);
            for key in field_keys {
                if key == extracted_key {
                    continue;
                }
                let key_value: Rc<dyn Value> = match &key {
                    ConstKey::String(s) => Rc::new(
                        crate::eval::value::value_string::r#const::ValueStringConst(s.clone()),
                    ),
                    ConstKey::Usize(n) => Rc::new(
                        crate::eval::value::value_number::r#const::ValueNumberConst(*n as f64),
                    ),
                };
                let key_cv = CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: Some(key_value),
                };
                let base_clone = base.clone();
                let field_cv = erb.must_er(aspect_field.field(xx, expr.loc, base_clone, key_cv))?;
                let field_v = field_cv.value.as_ref().ok_or_else(|| {
                    erb.errors.push(ExprErr {
                        loc: expr.loc,
                        errors: vec![
                            ExprSubErr::Opaque(
                                format!(
                                    "Sibling field '{:?}' listed in field_keys() but field() returned no value",
                                    key,
                                ),
                            ),
                        ],
                    });
                })?;
                let field_type = erb.must_er(field_v.type_(xx, expr.loc))?;
                if let Some(drop_aspect) = crate::r#gen::aspect_drop_dynamic_get_maybe(xx, field_type.as_ref()) {
                    let dc =
                        erb.must_er(
                            crate::r#gen::AspectDropDynamic::drop(drop_aspect.as_ref(), xx, expr.loc, field_cv,),
                        )?;
                    sibling_drops.push(dc);
                }
            }
            let v = erb.must_er(aspect_field.field(xx, expr.loc, base, field))?;
            if sibling_drops.is_empty() {
                return Ok(v);
            }
            sibling_drops.push(v.code.clone());
            let combined = erb.must_er(xx.target.seq(expr.loc, sibling_drops))?;
            return Ok(CodeValue {
                code: combined,
                value: v.value,
            });
        }),
        Expr_::Bind(_) => {
            return EvalRes::err(expr.loc, format!("Bind expression must be directly inside a block"));
        },
        Expr_::Seq(e) => {
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
                target: xx.target.clone(),
                current_exception_type: xx.current_exception_type.clone(),
            };
            return er_seq_cv!(expr.loc, erb, {
                struct Frame {
                    codes: Vec<Code>,
                    drop_code: Code,
                }

                let mut frames: Vec<Frame> = vec![Frame {
                    codes: vec![],
                    drop_code: Code {
                        loc: expr.loc,
                        code: Code_::None
                    }
                }];
                let mut last_val: Option<Rc<dyn Value>> = Some(Rc::new(ValueNullConst) as Rc<dyn Value>);
                let mut pending_discard: Option<CodeValue> = None;
                let mut control_ended = false;
                let total = e.exprs.len();
                for (idx, sub) in e.exprs.iter().enumerate() {
                    if control_ended {
                        erb.errors.push(ExprErr {
                            loc: sub.loc,
                            errors: vec![
                                ExprSubErr::Opaque("Unreachable: control flow ended at prior expression".to_string())
                            ],
                        });
                        continue;
                    }
                    if let Some(prev) = pending_discard.take() {
                        let drop_code =
                            erb.must_er(
                                crate::r#gen::Target::drop(child_xx.target.as_ref(), &child_xx, sub.loc, prev)
                            )?;
                        frames.last_mut().unwrap().codes.push(drop_code);
                    }
                    if let Expr_::Bind(bind) = &sub.r#type {
                        er_all!(erb, {
                            base = const_eval(&child_xx, bind.base.as_ref()),
                            value = const_eval(&child_xx, bind.value.as_ref()),
                        });
                        let base_constkey =
                            erb.must_res(extract_const_key(&child_xx, sub.loc, base.value.as_deref().unwrap()))?;
                        frames.last_mut().unwrap().codes.push(base.code);
                        if value.value.is_none() {
                            erb.errors.push(ExprErr {
                                loc: sub.loc,
                                errors: vec![
                                    ExprSubErr::Opaque(
                                        "Cannot bind result of expression that ends control flow".to_string(),
                                    ),
                                ],
                            });
                            return Err(());
                        }
                        let value_type = erb.must_er(value.value.as_ref().unwrap().type_(&child_xx, sub.loc))?;
                        let bind_aspect = erb.must_er(aspect_bind_get(&child_xx, sub.loc, value_type.as_ref()))?;
                        let binding = erb.must_er(bind_aspect.bind(&child_xx, sub.loc, value))?;
                        let binding_use_cv = binding.1.use_(&child_xx, sub.loc, Code {
                            loc: sub.loc,
                            code: Code_::None
                        });
                        let drop_code =
                            erb.must_er(
                                crate::r#gen::Target::drop(child_xx.target.as_ref(), &child_xx, sub.loc, binding_use_cv)
                            )?;
                        frames.last_mut().unwrap().codes.push(binding.0);
                        frames.push(Frame {
                            codes: vec![],
                            drop_code
                        });
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Rc::new(ValueNullConst) as Rc<dyn Value>);
                    } else {
                        let r = erb.must_er(const_eval(&child_xx, sub))?;
                        frames.last_mut().unwrap().codes.push(r.code.clone());
                        if r.value.is_none() {
                            control_ended = true;
                            last_val = None;
                            continue;
                        }
                        if idx + 1 < total {
                            pending_discard = Some(r);
                        } else {
                            last_val = r.value;
                        }
                    }
                }
                while frames.len() > 1 {
                    let inner = frames.pop().unwrap();
                    match &inner.drop_code.code {
                        Code_::None => {
                            frames.last_mut().unwrap().codes.extend(inner.codes);
                        },
                        Code_::Dyn(_) => {
                            let body = erb.must_er(child_xx.target.seq(expr.loc, inner.codes))?;
                            let wrapped = erb.must_er(child_xx.target.try_finally(expr.loc, body, inner.drop_code))?;
                            frames.last_mut().unwrap().codes.push(wrapped);
                        },
                    }
                }
                let outer = frames.pop().unwrap();
                return Ok(CodeValue {
                    code: erb.must_er(child_xx.target.seq(expr.loc, outer.codes))?,
                    value: last_val,
                });
            });
        },
        Expr_::Stage(e) => {
            fn find_matched_lowers_inner<'a>(expr: &'a Expr, sd: usize, ld: usize, out: &mut Vec<&'a Expr>) {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            out.push(&e.expr);
                        } else {
                            find_matched_lowers_inner(&e.expr, sd, new_ld, out);
                        }
                    },
                    Expr_::Stage(e) => find_matched_lowers_inner(&e.expr, sd + 1, ld, out),
                    Expr_::Graft(e) => match e {
                        expr::ExprGraft::Pending(inner) => find_matched_lowers_inner(inner, sd, ld, out),
                        expr::ExprGraft::Evaluated(_) => { },
                    },
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
                    Expr_::Seq(e) => {
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
                    Expr_::OperatorEq(e) => {
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
                    Expr_::Try(e) => {
                        find_matched_lowers_inner(&e.body, sd, ld, out);
                        if let Some(c) = &e.catch {
                            find_matched_lowers_inner(&c.binding_name, sd, ld, out);
                            find_matched_lowers_inner(&c.exception_type, sd, ld, out);
                            find_matched_lowers_inner(&c.body, sd, ld, out);
                        }
                        if let Some(f) = &e.finally {
                            find_matched_lowers_inner(f, sd, ld, out);
                        }
                    },
                }
            }

            fn find_matched_grafts_inner<'a>(expr: &'a Expr, sd: usize, out: &mut Vec<&'a Expr>) {
                match &expr.r#type {
                    Expr_::Graft(g) => match g {
                        expr::ExprGraft::Pending(inner) if sd == 0 => out.push(inner),
                        expr::ExprGraft::Pending(inner) => find_matched_grafts_inner(inner, sd, out),
                        expr::ExprGraft::Evaluated(_) => { },
                    },
                    Expr_::Stage(e) => find_matched_grafts_inner(&e.expr, sd + 1, out),
                    Expr_::Lower(e) => find_matched_grafts_inner(&e.expr, sd, out),
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => { },
                    Expr_::As(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.target, sd, out);
                    },
                    Expr_::Call(c) => {
                        find_matched_grafts_inner(&c.func, sd, out);
                        find_matched_grafts_inner(&c.arg, sd, out);
                    },
                    Expr_::Bind(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.value, sd, out);
                    },
                    Expr_::Set(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.value, sd, out);
                    },
                    Expr_::Field(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.key, sd, out);
                    },
                    Expr_::Seq(e) => {
                        for sub in &e.exprs {
                            find_matched_grafts_inner(sub, sd, out);
                        }
                    },
                    Expr_::Tuple(t) => {
                        for e in &t.elements {
                            find_matched_grafts_inner(e, sd, out);
                        }
                    },
                    Expr_::Record(r) => {
                        for (k, v) in &r.entries {
                            find_matched_grafts_inner(k, sd, out);
                            find_matched_grafts_inner(v, sd, out);
                        }
                    },
                    Expr_::If(e) => {
                        for b in &e.conditions {
                            find_matched_grafts_inner(&b.condition, sd, out);
                            find_matched_grafts_inner(&b.body, sd, out);
                        }
                        if let Some(el) = &e.else_ {
                            find_matched_grafts_inner(el, sd, out);
                        }
                    },
                    Expr_::OperatorLt(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.reference, sd, out);
                    },
                    Expr_::OperatorEq(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.reference, sd, out);
                    },
                    Expr_::OperatorAdd(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.reference, sd, out);
                    },
                    Expr_::Foreach(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.element_key, sd, out);
                        find_matched_grafts_inner(&e.body, sd, out);
                    },
                    Expr_::Match(e) => {
                        find_matched_grafts_inner(&e.base, sd, out);
                        find_matched_grafts_inner(&e.element_key, sd, out);
                        for c in &e.cases {
                            find_matched_grafts_inner(&c.condition, sd, out);
                            find_matched_grafts_inner(&c.body, sd, out);
                        }
                        if let Some(f) = &e.fallback {
                            find_matched_grafts_inner(f, sd, out);
                        }
                    },
                    Expr_::Try(e) => {
                        find_matched_grafts_inner(&e.body, sd, out);
                        if let Some(c) = &e.catch {
                            find_matched_grafts_inner(&c.binding_name, sd, out);
                            find_matched_grafts_inner(&c.exception_type, sd, out);
                            find_matched_grafts_inner(&c.body, sd, out);
                        }
                        if let Some(f) = &e.finally {
                            find_matched_grafts_inner(f, sd, out);
                        }
                    },
                }
            }

            let mut matched_lower_exprs = Vec::new();
            find_matched_lowers_inner(&e.expr, 0, 0, &mut matched_lower_exprs);
            let mut matched_graft_exprs = Vec::new();
            find_matched_grafts_inner(&e.expr, 0, &mut matched_graft_exprs);
            if matched_lower_exprs.is_empty() && matched_graft_exprs.is_empty() {
                return EvalRes::ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None,
                    },
                    value: Some(Rc::new(e.expr.as_ref().clone())),
                });
            }
            return er_seq_cv!(expr.loc, erb, {
                let mut lower_values = Vec::new();
                for lower_inner in &matched_lower_exprs {
                    let cv = erb.must_er(const_eval(xx, lower_inner))?;
                    lower_values.push(cv);
                }
                let mut graft_values = Vec::new();
                for graft_inner in &matched_graft_exprs {
                    let cv = erb.must_er(const_eval(xx, graft_inner))?;
                    graft_values.push(cv);
                }
                let v =
                    erb.must_er(xx.target.stage(xx, expr.loc, e.expr.as_ref().clone(), lower_values, graft_values))?;
                return Ok(v);
            });
        },
        Expr_::Lower(e) => {
            return const_eval(xx, &e.expr);
        },
        Expr_::Graft(e) => {
            return match e {
                expr::ExprGraft::Evaluated(cv) => EvalRes::ok(cv.clone()),
                expr::ExprGraft::Pending(_) => EvalRes::err(
                    expr.loc,
                    "Graft outside of stage processing — its inner was never evaluated".to_string(),
                ),
            };
        },
        Expr_::Tuple(t) => {
            let elements: Vec<TupleElement> =
                t.elements.iter().map(|e| TupleElement::EvalRes(const_eval(xx, e))).collect();
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Some(Rc::new(Tuple { elements: elements })),
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
                if let Some(dynamic_code) = k_cv.code.cast::<CodeDynamic>().v.flatten() {
                    key_codes.push(dynamic_code.clone());
                }
                match extract_const_key(xx, expr.loc, k_cv.value.as_deref().unwrap()) {
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
                value: Some(Rc::new(Record {
                    base: None,
                    fields,
                })),
            });
        },
        Expr_::OperatorLt(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_lt = erb.must_er(aspect_operator_lt_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_lt.lt(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorEq(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_eq = erb.must_er(aspect_operator_eq_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_eq.eq(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorAdd(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                reference = const_eval(xx, e.reference.as_ref()),
            });
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_add = erb.must_er(aspect_operator_add_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_add.add(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::Foreach(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = const_eval(xx, e.base.as_ref()),
                element_key_cv = const_eval(xx, e.element_key.as_ref()),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_deref().unwrap()))?;
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let iterable = erb.must_er(aspect_iterable_get(xx, expr.loc, base_type.as_ref()))?;
            let element_scope_id = BindingId::Local(xx.vx.next_local_id.next());
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
                current_exception_type: xx.current_exception_type.clone(),
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
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_deref().unwrap()))?;
            let base_type = erb.must_er(base.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let union = erb.must_er(aspect_union_get(xx, expr.loc, base_type.as_ref()))?;
            let variants = union.variants();
            let mut case_tags: Vec<ConstKey> = Vec::new();
            for case in &e.cases {
                let cond_cv = erb.must_er(const_eval(xx, &case.condition))?;
                let tag = erb.must_res(extract_const_key(xx, expr.loc, cond_cv.value.as_deref().unwrap()))?;
                if !variants.iter().any(|(t, _, _)| t == &tag) {
                    return erb.err_new(format!("Match case tag '{:?}' not found in union", tag));
                }
                case_tags.push(tag);
            }
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
            let spec_str = erb.must_res(extract_const_key(xx, expr.loc, spec_cv.value.as_deref().unwrap()))?;
            let spec_name = match spec_str {
                ConstKey::String(s) => s,
                _ => return erb.err_new("Import spec must be a string"),
            };
            let source_spec = SourceSpecAbsolute::Virtual(spec_name);
            let (source_id, source_module) = erb.must_res(xx.vx.ix.gx.sources.get(source_spec.clone()))?;
            let compile_id = CompileId(xx.import_state.get(), source_id);
            if xx.vx.ix.compile_path.contains(&compile_id) {
                return erb.err_new("Import cycle detected");
            }
            let new_compile_path = xx.vx.ix.compile_path.clone();
            let entry = {
                let mut results = xx.vx.ix.gx.compile_results.lock().unwrap();
                if let Some(entry) = results.get(&compile_id) {
                    entry.clone()
                } else {
                    let entry: Arc<RwLock<Option<CompileRes>>> = Arc::new(RwLock::new(None));
                    results.insert(compile_id, entry.clone());
                    let mut write_guard = entry.write().unwrap();
                    drop(results);
                    let gx = xx.vx.ix.gx.clone();
                    let import_state = xx.import_state.get();
                    let res = std::thread::Builder::new().stack_size(512 * 1024 * 1024).spawn(move || {
                        let module = source_module_to_module(source_id, &source_module);
                        compile_importid(&gx, import_state, module, new_compile_path)
                    }).expect("compile_importid thread spawn").join().expect("compile_importid thread panicked");
                    *write_guard = Some(res);
                    drop(write_guard);
                    entry
                }
            };
            let guard = entry.read().unwrap();
            let compile_res = guard.as_ref().unwrap();
            let artifact = match &compile_res.result {
                CompileResResult::Ok(a) => a.clone(),
                CompileResResult::Err(e) => {
                    for err in e {
                        erb.errors.push(err.clone());
                    }
                    return erb.err_new("Import failed");
                },
            };
            drop(guard);
            let exporter = erb.must_er(import_artifact(xx, expr.loc, artifact))?;
            let value = erb.must_er(exporter.as_value())?;
            let cv = CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Some(value),
            };
            let value_type = erb.must_er(cv.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let post_deserialize = erb.must_er(aspect_post_deserialize_get(xx, expr.loc, value_type.as_ref()))?;
            let cv = erb.must_er(post_deserialize.post_deserialize(xx, expr.loc, cv))?;
            return Ok(cv);
        }),
        Expr_::If(e) => return er_seq_cv!(expr.loc, erb, {
            let mut conditions_evaluated = Vec::new();
            let mut all_const = true;
            for branch in &e.conditions {
                let cond = erb.must_er(const_eval(xx, &branch.condition))?;
                let cond_type = erb.must_er(cond.value.as_ref().unwrap().type_(xx, expr.loc))?;
                if let Some(aspect_bool) = aspect_bool_const_get_maybe(xx, cond_type.as_ref()) {
                    let val = aspect_bool.get(cond.value.as_deref().unwrap());
                    conditions_evaluated.push((val, cond, &branch.body));
                } else {
                    all_const = false;
                    conditions_evaluated.push((false, cond, &branch.body));
                }
            }
            if all_const {
                for (val, _, body) in &conditions_evaluated {
                    if *val {
                        let body_cv = erb.must_er(const_eval(xx, body))?;
                        return Ok(body_cv);
                    }
                }
                if let Some(else_expr) = &e.else_ {
                    let else_cv = erb.must_er(const_eval(xx, else_expr))?;
                    return Ok(else_cv);
                }
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None
                    },
                    value: Some(Rc::new(ValueNullConst)),
                });
            } else {
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
            let target_meta_type = erb.must_er(target.value.as_ref().unwrap().type_(xx, expr.loc))?;
            let aspect_type = erb.must_er(aspect_type_get(xx, expr.loc, target_meta_type.as_ref()))?;
            let target_type = aspect_type.type_(target.value.as_deref().unwrap());
            let v = erb.must_er(typecheck_convert(xx, expr.loc, target_type.as_ref(), base))?;
            return Ok(v);
        }),
        Expr_::Try(e) => {
            if e.catch.is_none() && e.finally.is_none() {
                return EvalRes::err(expr.loc, "Try requires at least one of catch/finally".to_string());
            }
            return er_seq_cv!(expr.loc, erb, {
                let body_exc_type: Option<Rc<dyn Type>> = if let Some(catch) = &e.catch {
                    let exc_type_cv = erb.must_er(const_eval(xx, catch.exception_type.as_ref()))?;
                    let exc_meta = erb.must_er(exc_type_cv.value.as_ref().unwrap().type_(xx, expr.loc))?;
                    let aspect_type = erb.must_er(aspect_type_get(xx, expr.loc, exc_meta.as_ref()))?;
                    Some(aspect_type.type_(exc_type_cv.value.as_deref().unwrap()))
                } else {
                    xx.current_exception_type.clone()
                };
                let body_xx = ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: xx.scope.clone(),
                    target: xx.target.clone(),
                    current_exception_type: body_exc_type.clone(),
                };
                let body_cv = erb.must_er(const_eval(&body_xx, e.body.as_ref()))?;
                let body_type: Option<Rc<dyn Type>> = if let Some(v) = body_cv.value.as_ref() {
                    Some(erb.must_er(v.type_(xx, expr.loc))?)
                } else {
                    None
                };
                let body_dyn = match &body_cv.code.code {
                    Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                        Some(d) => d.clone(),
                        None => return erb.err_new("Try body must produce CodeDynamic".to_string()),
                    },
                    Code_::None => return erb.err_new(
                        "Try body must produce CodeDynamic (got Code::None)".to_string()
                    ),
                };
                let mut result_value: Option<Rc<dyn Value>> = body_cv.value.clone();
                let catch_dyn = if let Some(catch) = &e.catch {
                    let binding_key_cv = erb.must_er(const_eval(xx, catch.binding_name.as_ref()))?;
                    let binding_constkey =
                        erb.must_res(
                            extract_const_key(xx, catch.binding_name.loc, binding_key_cv.value.as_deref().unwrap())
                        )?;
                    let binding_id = BindingId::Local(xx.vx.next_local_id.next());
                    let catch_scope = Rc::new(Scope {
                        values: RefCell::new(HashMap::new()),
                        parent: Some(xx.scope.clone()),
                    });
                    let catch_exc_type =
                        body_exc_type.as_ref().expect("Try catch present but body_exc_type is None").clone();
                    if let Some(ib) = crate::r#gen::aspect_initial_bind_get_maybe(xx, catch_exc_type.as_ref()) {
                        catch_scope.values.borrow_mut().insert(binding_constkey, ib.initial_bind(binding_id));
                    }
                    let catch_xx = ExprContext {
                        vx: xx.vx.clone(),
                        import_state: xx.import_state.clone(),
                        scope: catch_scope,
                        target: xx.target.clone(),
                        current_exception_type: xx.current_exception_type.clone(),
                    };
                    let catch_cv = erb.must_er(const_eval(&catch_xx, catch.body.as_ref()))?;
                    let catch_cv = if let (Some(bt), Some(_)) = (body_type.as_ref(), catch_cv.value.as_ref()) {
                        erb.must_er(typecheck_convert(&catch_xx, expr.loc, bt.as_ref(), catch_cv))?
                    } else {
                        catch_cv
                    };
                    if result_value.is_none() {
                        result_value = catch_cv.value.clone();
                    }
                    let catch_code_dyn = match &catch_cv.code.code {
                        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                            Some(d) => d.clone(),
                            None => return erb.err_new("Try catch body must produce CodeDynamic".to_string()),
                        },
                        Code_::None => return erb.err_new(
                            "Try catch body must produce CodeDynamic (got Code::None)".to_string()
                        ),
                    };
                    Some(crate::eval::dynamic::code::CodeDynamicCatch {
                        binding: binding_id,
                        body: Box::new(catch_code_dyn),
                    })
                } else {
                    None
                };
                let finally_dyn = if let Some(finally) = &e.finally {
                    let finally_cv = erb.must_er(const_eval(xx, finally.as_ref()))?;
                    let finally_code_dyn = match &finally_cv.code.code {
                        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                            Some(d) => d.clone(),
                            None => return erb.err_new("Try finally body must produce CodeDynamic".to_string()),
                        },
                        Code_::None => return erb.err_new(
                            "Try finally body must produce CodeDynamic (got Code::None)".to_string()
                        ),
                    };
                    let finally_drop_code: Option<Code> = if let Some(fv) = finally_cv.value.as_ref() {
                        let finally_type = erb.must_er(fv.type_(xx, expr.loc))?;
                        if let Some(drop_aspect) =
                            crate::r#gen::aspect_drop_dynamic_get_maybe(xx, finally_type.as_ref()) {
                            Some(
                                erb.must_er(
                                    crate::r#gen::AspectDropDynamic::drop(
                                        drop_aspect.as_ref(),
                                        xx,
                                        expr.loc,
                                        finally_cv.clone()
                                    )
                                )?
                            )
                        } else {
                            None
                        }
                    } else {
                        None
                    };
                    let combined_finally_dyn = if let Some(drop_code) = finally_drop_code {
                        let combined = erb.must_er(xx.target.seq(expr.loc, vec![Code {
                            loc: expr.loc,
                            code: Code_::Dyn(Rc::new(finally_code_dyn))
                        }, drop_code,]))?;
                        match &combined.code {
                            Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                                Some(d) => d.clone(),
                                None => return erb.err_new(
                                    "Try finally combined must produce CodeDynamic".to_string()
                                ),
                            },
                            Code_::None => return erb.err_new(
                                "Try finally combined must produce CodeDynamic (got Code::None)".to_string()
                            ),
                        }
                    } else {
                        finally_code_dyn
                    };
                    Some(Box::new(combined_finally_dyn))
                } else {
                    None
                };
                let try_code = CodeDynamic {
                    loc: expr.loc,
                    type_: CodeDynamicType::Try(crate::eval::dynamic::code::CodeDynamicTry {
                        body: Box::new(body_dyn),
                        catch: catch_dyn,
                        finally: finally_dyn,
                    }),
                };
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::Dyn(Rc::new(try_code)),
                    },
                    value: result_value,
                });
            });
        },
    }
}

pub(crate) struct DynamicExprContextSlot {
    pub(crate) xx: Rc<ExprContext>,
}

pub(crate) struct DynamicLogStore {
    pub(crate) logs: RefCell<Vec<LogEntry>>,
}

pub(crate) struct DynamicRcStore {
    weaks: RefCell<Vec<v8::Weak<v8::External>>>,
}

#[derive(Default)]
pub(crate) struct DynamicSourceBuilder {
    pub(crate) lines: Vec<String>,
    pub(crate) mapping: HashMap<usize, QExprId>,
    pub(crate) next_expr_id: usize,
}

impl DynamicSourceBuilder {
    pub(crate) fn next_expr_id(&mut self) -> usize {
        let id = self.next_expr_id;
        self.next_expr_id += 1;
        id
    }

    pub(crate) fn push(&mut self, id: QExprId, line: String) {
        let dynamic_num = self.lines.len() + 1;
        self.mapping.insert(dynamic_num, id);
        self.lines.push(line);
    }
}

pub(crate) struct DynamicUniqueCounter {
    pub(crate) compile_id: CompileId,
    pub(crate) counter: Counter,
}

pub(crate) struct Erb {
    pub(crate) errors: Vec<ExprErr>,
    pub(crate) loc: QExprId,
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
    let mut isolate_refmut = cix.isolate.borrow_mut();
    let dynamic_scope_isolate: &mut v8::OwnedIsolate = &mut *isolate_refmut;
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_local_id: Default::default(),
        externals: RefCell::new(Vec::new()),
        dynamic_scope: std::cell::Cell::new(None),
        definition_sets: RefCell::new(std::collections::HashMap::new()),
    });
    dynamic_scope_isolate.set_slot(DynamicRcStore { weaks: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(DynamicLogStore { logs: RefCell::new(Vec::new()) });
    dynamic_scope_isolate.set_slot(DynamicUniqueCounter {
        compile_id: CompileId(cix.import_state, cix.source_id),
        counter: Counter::default(),
    });
    let dynamic_scope_handle = std::pin::pin!(v8::HandleScope::new(dynamic_scope_isolate));
    let mut dynamic_scope_handle2 = dynamic_scope_handle.init();
    let dynamic_scope_context = {
        let mut ctx_slot = cix.dynamic_context.borrow_mut();
        match ctx_slot.as_ref() {
            Some(global) => v8::Local::new(&mut dynamic_scope_handle2, global),
            None => {
                let ctx = v8::Context::new(&mut dynamic_scope_handle2, Default::default());
                *ctx_slot = Some(v8::Global::new(&mut dynamic_scope_handle2, ctx));
                ctx
            },
        }
    };
    let dynamic_scope_context2 = &mut v8::ContextScope::new(&mut dynamic_scope_handle2, dynamic_scope_context);
    tc_scope!(let dynamic_scope, dynamic_scope_context2);
    {
        let scope_ref: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = &mut **dynamic_scope;
        let scope_ptr = std::ptr::NonNull::from(scope_ref);
        let scope_ptr_static: std::ptr::NonNull<v8::PinnedRef<'static, v8::HandleScope<'static, v8::Context>>> =
            unsafe {
                std::mem::transmute(scope_ptr)
            };
        ex.dynamic_scope.set(Some(scope_ptr_static));
    }

    struct V8RefsGuard<'a>(&'a EvalContext);

    impl<'a> Drop for V8RefsGuard<'a> {
        fn drop(&mut self) {
            self.0.dynamic_scope.set(None);
        }
    }

    let _v8_refs_guard = V8RefsGuard(&ex);
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
        current_exception_type: Some(Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic)),
    });
    dynamic_scope.set_slot(DynamicExprContextSlot { xx: xx.clone() });
    let register_dynamic = !cix.builtins_registered.get();
    {
        let scope_entries = init_builtin_scope(&xx, root_loc, import_state, dynamic_scope, register_dynamic)?;
        scope.values.borrow_mut().extend(scope_entries);
    }
    if register_dynamic {
        crate::r#gen::install_invisible_builtins(dynamic_scope);
        cix.builtins_registered.set(true);
    }
    let result = const_eval(&xx, expr);
    if let Some(errs) = result.errors {
        return Err(errs);
    }
    let cv = result.v.unwrap();
    let value_type = cv.value.as_ref().unwrap().type_(&xx, root_loc).into_res()?;
    let (stage1_code, fallback_value) = match &cv.code.code {
        Code_::None => {
            (None, Some(cv.value))
        },
        _ => {
            let to_dynamic = aspect_consume_dynamic_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let dynamic_code = to_dynamic.to_dynamic(&xx, root_loc, cv).into_res()?;
            (Some(dynamic_code), None)
        },
    };
    let mut code1 = DynamicSourceBuilder::default();
    let mut has_member_js = false;
    let mut dss: Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>> = Vec::new();
    {
        let mut seen: std::collections::HashSet<crate::langhelp::DefSetId> = std::collections::HashSet::new();
        let mut to_visit: Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>> =
            ex.definition_sets.borrow().values().cloned().collect();
        while let Some(ds) = to_visit.pop() {
            if !seen.insert(ds.def_set_id) {
                continue;
            }
            if crate::eval::definition_set::DefinitionSet::install(&ds, &xx, root_loc, &mut code1)? {
                has_member_js = true;
            }
            for dep in ds.dependencies.borrow().values() {
                if !seen.contains(&dep.def_set_id) {
                    to_visit.push(dep.clone());
                }
            }
            dss.push(ds);
        }
    }
    let has_js = stage1_code.is_some() || has_member_js;
    if !has_js {
        let logs = dynamic_scope.get_slot::<DynamicLogStore>().unwrap().logs.take();
        return Ok(EvalResult {
            value: fallback_value.expect("No dynamic code and no fallback value").expect("Fallback value is None"),
            logs,
        });
    }
    code1.push(root_loc, "{/*main*/".to_string());
    if let Some(code) = stage1_code {
        let root_var = render_code(&mut code1, &code);
        code1.push(root_loc, format!("{};", root_var));
    } else {
        code1.push(root_loc, "null;".to_string());
    }
    code1.push(root_loc, "}/*main*/".to_string());
    let mut all_externals: Vec<(String, External)> =
        ex.externals.borrow().iter().map(|(id, v)| (binding_id_dynamic_key(*id), v.clone())).collect();
    for ds in &dss {
        for func_data in ds.functions.borrow().iter() {
            let scope_id = func_data.binding_id.get().expect("Definition set function not registered");
            let func_name = binding_id_dynamic_key(scope_id);
            for (id, value) in func_data.captured_externals.borrow().iter() {
                let local_key = binding_id_dynamic_key(*id);
                all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
            }
        }
        for dt_data in ds.objects.borrow().iter() {
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.binding_id.get().expect("Method binding ID not assigned");
                let func_name = binding_id_dynamic_key(method_id);
                for (id, value) in method.captured_externals.borrow().iter() {
                    let local_key = binding_id_dynamic_key(*id);
                    all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
                }
            }
            for impl_data in dt_data.interface_impls.borrow().iter() {
                for method_impl in &impl_data.method_impls {
                    let method_id =
                        method_impl.binding_id.get().expect("Interface method impl binding ID not assigned");
                    let func_name = binding_id_dynamic_key(method_id);
                    for (id, value) in method_impl.captured_externals.borrow().iter() {
                        let local_key = binding_id_dynamic_key(*id);
                        all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
                    }
                }
            }
        }
    }
    for (key_str, value) in &all_externals {
        let key = v8::String::new(dynamic_scope, key_str).unwrap().cast();
        match value {
            External::StagedExprCell(cell) => {
                let ext = to_dynamic_ext(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::StageLiftCell(cell) => {
                let ext = to_dynamic_ext(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::CodeValueExternCell(cell) => {
                let ext = to_dynamic_ext(dynamic_scope, cell.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::Type(type_) => {
                let ext = rc_type_to_dynamic_ext(dynamic_scope, type_.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
            External::Unique(u) => {
                let ext = to_dynamic_ext(dynamic_scope, u.clone());
                dynamic_scope.get_current_context().global(dynamic_scope).set(dynamic_scope, key, ext.into());
            },
        }
    }
    let dynamic_source = code1.lines.join("\n");
    cix.gx.generated_dynamic_scripts.lock().unwrap().push(dynamic_source.clone());
    let script =
        v8::Script::compile(dynamic_scope, v8::String::new(dynamic_scope, &dynamic_source).unwrap(), None).unwrap();
    match script.run(dynamic_scope) {
        Some(stage2) => {
            let value: Rc<dyn Value> = match fallback_value {
                Some(v) => v.expect("Fallback value is None"),
                None => {
                    let value_from_js = aspect_lift_get(&xx, root_loc, value_type.as_ref()).into_res()?;
                    value_from_js.lift(&xx, &*dynamic_scope, stage2).map_err(|e| vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque(e)],
                    }])?
                },
            };
            let logs = dynamic_scope.get_slot::<DynamicLogStore>().unwrap().logs.take();
            Ok(EvalResult {
                value,
                logs,
            })
        },
        None => {
            let text;
            match dynamic_scope.exception() {
                Some(e) => {
                    let mut rendered = e.to_rust_string_lossy(&dynamic_scope);
                    if let Ok(obj) = v8::Local::<v8::Object>::try_from(e) {
                        if let Some(panic_key) = v8::String::new(&mut *dynamic_scope, PANIC_MARKER_KEY) {
                            let panic_val = obj.get(&mut *dynamic_scope, panic_key.into());
                            if panic_val.map_or(false, |v| v.boolean_value(&mut *dynamic_scope)) {
                                if let Some(message_key) = v8::String::new(&mut *dynamic_scope, "message") {
                                    if let Some(message_val) = obj.get(&mut *dynamic_scope, message_key.into()) {
                                        if !message_val.is_undefined() && !message_val.is_null() {
                                            let message_text = message_val.to_rust_string_lossy(&mut *dynamic_scope);
                                            rendered = format!("panic: {}", message_text);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    text = rendered;
                },
                None => {
                    text = format!("Script failed to return, but no exception captured (compiler bug)");
                },
            }
            match dynamic_scope.message().and_then(|e| e.get_line_number(&dynamic_scope)) {
                Some(dynamic_line) => {
                    let dl = dynamic_line as usize;
                    let start = dl.saturating_sub(6);
                    let end = (dl + 4).min(code1.lines.len());
                    let mut context = String::new();
                    for (i, line) in code1.lines[start .. end].iter().enumerate() {
                        let n = start + i + 1;
                        let marker = if n == dl {
                            ">>"
                        } else {
                            "  "
                        };
                        context.push_str(&format!("\n{} {}: {}", marker, n, line));
                    }
                    Err(vec![ExprErr {
                        loc: *code1.mapping.get(&(dynamic_line as usize)).unwrap_or(&root_loc),
                        errors: vec![
                            ExprSubErr::Opaque(format!("{} (at dynamic line {}):{}", text, dynamic_line, context)),
                        ],
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
    let vtype = v.type_(xx, loc).into_res().map_err(|e| format!("{:?}", e))?;
    if let Some(a) = aspect_string_const_get_maybe(xx, vtype.as_ref()) {
        return Ok(ConstKey::String(a.get(v)));
    }
    return Err(format!("TODO value type [X] must expose const key aspect [LIST] but doesn't"));
}

fn from_dynamic_any(value: &v8::Value) -> &dyn std::any::Any {
    assert!(value.is_external(), "from_dynamic_any: expected v8::External");
    let ext = unsafe {
        &*(value as *const v8::Value as *const v8::External)
    };
    let ptr = ext.value() as *const Box<dyn std::any::Any>;
    unsafe {
        &**ptr
    }
}

pub(crate) fn from_dynamic_ref<T: 'static>(value: &v8::Value) -> &T {
    must_downcast_ref::<T>(from_dynamic_any(value))
}

pub(crate) fn init_builtin_scope<
    's,
>(
    xx: &ExprContext,
    loc: QExprId,
    import_state: ImportStateId,
    dynamic_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    register_dynamic: bool,
) -> Result<HashMap<ConstKey, Box<dyn Binding>>, Vec<ExprErr>> {
    let tree = gen_builtin_tree(dynamic_scope, import_state);
    if register_dynamic {
        walk_register_dynamic(&tree, dynamic_scope);
    }
    let root_binding = walk_build_binding(&tree, xx, loc)?;
    *xx.vx.ix.builtin_scope_root_binding.borrow_mut() = Some(root_binding.clone());
    let mut scope_entries = HashMap::<ConstKey, Box<dyn Binding>>::new();
    scope_entries.insert(
        ConstKey::String(KEY_BUILTINS.to_string()),
        Box::new(crate::eval::RcBindingWrap(root_binding)) as Box<dyn Binding>,
    );
    Ok(scope_entries)
}

fn maybe_wrap_call_exception(
    xx: &ExprContext,
    loc: QExprId,
    callee_exc: Option<Rc<dyn Type>>,
    call_cv: CodeValue,
) -> EResult<CodeValue> {
    return er_seq_cv!(loc, erb, {
        let callee_exc = match callee_exc {
            Some(t) => t,
            None => return Ok(call_cv),
        };
        let caller_exc = match xx.current_exception_type.as_ref() {
            Some(t) => t.clone(),
            None => return erb.err_new(
                "Cannot call a function that may throw from a function that did not declare an exception type".to_string(),
            ),
        };
        let catch_binding = BindingId::Local(xx.vx.next_local_id.next());
        let initial_bind = erb.must_er(aspect_initial_bind_get(xx, loc, callee_exc.as_ref()))?;
        let binding = initial_bind.initial_bind(catch_binding);
        let bound_cv = binding.use_(xx, loc, Code {
            loc,
            code: Code_::None
        });
        let cast_cv = erb.must_er(callee_exc.cast_to(xx, loc, bound_cv, caller_exc.as_ref()))?;
        if matches!(cast_cv.code.code, Code_::None) {
            return Ok(call_cv);
        }
        let wrapped_code = erb.must_er(xx.target.cast_exception(xx, loc, call_cv.code, catch_binding, cast_cv))?;
        return Ok(CodeValue {
            code: wrapped_code,
            value: call_cv.value,
        });
    });
}

pub(crate) fn rc_binding_from_v8(value: &v8::Value) -> Rc<dyn Binding> {
    from_dynamic_ref::<Rc<dyn Binding>>(value).clone()
}

pub(crate) fn rc_binding_to_dynamic_ext<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Binding>) -> v8::Local<'s, v8::External> {
    to_dynamic_ext(scope, value)
}

pub(crate) fn rc_target_from_v8(value: &v8::Value) -> Rc<dyn Target> {
    from_dynamic_ref::<Rc<dyn Target>>(value).clone()
}

pub(crate) fn rc_target_to_dynamic_ext<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Target>) -> v8::Local<'s, v8::External> {
    to_dynamic_ext(scope, value)
}

pub(crate) fn rc_type_from_v8(value: &v8::Value) -> Rc<dyn Type> {
    from_dynamic_ref::<Rc<dyn Type>>(value).clone()
}

pub(crate) fn rc_type_to_dynamic_ext<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Type>) -> v8::Local<'s, v8::External> {
    to_dynamic_ext(scope, value)
}

pub(crate) fn rc_value_from_v8(value: &v8::Value) -> Rc<dyn Value> {
    from_dynamic_ref::<Rc<dyn Value>>(value).clone()
}

pub(crate) fn rc_value_to_dynamic_ext<
    's,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<dyn Value>) -> v8::Local<'s, v8::External> {
    to_dynamic_ext(scope, value)
}

pub(crate) struct RcBindingWrap(pub Rc<dyn Binding>);

impl Binding for RcBindingWrap {
    fn use_(&self, xx: &ExprContext, loc: QExprId, code: Code) -> CodeValue {
        self.0.use_(xx, loc, code)
    }
}

pub(crate) fn render_code(lines: &mut DynamicSourceBuilder, code: &CodeDynamic) -> String {
    let out = format!("{}{}", EXPR_PREFIX, lines.next_expr_id());
    lines.push(code.loc, format!("let {};", out));
    match &code.type_ {
        CodeDynamicType::Block(c) => {
            for entry in &c.entries {
                match entry {
                    CodeDynamicBlockEntry::Bind(b) => {
                        let bind_key = binding_id_dynamic_key(b.base);
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
            lines.push(code.loc, format!("{} = {};", out, binding_id_dynamic_key(c.id)));
        },
        CodeDynamicType::Call(c) => {
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
        CodeDynamicType::SubscriptUse(s) => {
            let base = render_code(lines, &s.base);
            let key = render_code(lines, &s.key);
            lines.push(code.loc, format!("{} = {}[{}];", out, base, key));
        },
        CodeDynamicType::BindingSet(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{} = {};", binding_id_dynamic_key(c.base), val_var));
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
        CodeDynamicType::OperatorEq(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} === {});", out, base_var, ref_var));
        },
        CodeDynamicType::OperatorAdd(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} + {});", out, base_var, ref_var));
        },
        CodeDynamicType::Foreach(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = binding_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("for (let {} of {}) {{", elem_key, base_var));
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        CodeDynamicType::ForeachMap(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = binding_id_dynamic_key(c.element_key);
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
            let elem_key = binding_id_dynamic_key(c.element_key);
            lines.push(code.loc, format!("{{ let [{}, {}] = Object.entries({})[0];", TAG_VAR, elem_key, base_var));
            lines.push(code.loc, format!("switch ({}) {{", TAG_VAR));
            for case in &c.cases {
                lines.push(code.loc, format!("case {}: {{", serde_json::to_string(&case.dynamic_tag).unwrap()));
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
        CodeDynamicType::Throw(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("throw {};", val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        CodeDynamicType::Try(c) => {
            lines.push(code.loc, "try {".to_string());
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            if let Some(catch) = &c.catch {
                let bind_key = binding_id_dynamic_key(catch.binding);
                lines.push(code.loc, format!("}} catch ({}) {{", bind_key));
                lines.push(
                    code.loc,
                    format!(
                        "if ({} && {}[{}]) {{ throw {}; }}",
                        bind_key,
                        bind_key,
                        serde_json::to_string(PANIC_MARKER_KEY).unwrap(),
                        bind_key,
                    ),
                );
                let catch_var = render_code(lines, &catch.body);
                lines.push(code.loc, format!("{} = {};", out, catch_var));
            }
            if let Some(finally) = &c.finally {
                lines.push(code.loc, "} finally {".to_string());
                let fin_var = render_code(lines, finally);
                lines.push(code.loc, format!("{};", fin_var));
            }
            lines.push(code.loc, "}".to_string());
        },
    }
    return out;
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

pub(crate) fn to_dynamic_ext<
    's,
    T: 'static,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: T) -> v8::Local<'s, v8::External> {
    let any: Box<dyn std::any::Any> = Box::new(value);
    let ptr = Box::into_raw(Box::new(any)) as *mut std::ffi::c_void;
    let ext = v8::External::new(&*scope, ptr);
    let weak = v8::Weak::with_guaranteed_finalizer(&mut *scope, ext, Box::new(move || unsafe {
        drop(Box::from_raw(ptr as *mut Box<dyn std::any::Any>));
    }));
    scope.get_slot::<DynamicRcStore>().unwrap().weaks.borrow_mut().push(weak);
    ext
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        if source.value.is_none() {
            return Ok(source);
        }
        let source_type = erb.must_er(source.value.as_ref().unwrap().type_(xx, loc))?;
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

fn walk_build_binding(
    node: &builtin_tree::BuiltinNode,
    xx: &ExprContext,
    loc: QExprId,
) -> Result<Rc<dyn Binding>, Vec<ExprErr>> {
    use {
        builtin_tree::{
            BuiltinLeaf,
            BuiltinNode,
        },
        crate::eval::value::type_argument_record::r#const::{
            ArgumentRecord,
            ArgumentRecordBinding,
        },
    };

    match node {
        BuiltinNode::Branch(children) => {
            let mut fields = std::collections::BTreeMap::new();
            for (key, child) in children {
                fields.insert(ConstKey::String((*key).to_string()), walk_build_binding(child, xx, loc)?);
            }
            Ok(Rc::new(ArgumentRecordBinding { record: Rc::new(ArgumentRecord { fields }) }))
        },
        BuiltinNode::Leaf(BuiltinLeaf::Function { dynamic_key, type_, .. }) |
        BuiltinNode::Leaf(BuiltinLeaf::Interface { dynamic_key, type_, .. }) |
        BuiltinNode::Leaf(BuiltinLeaf::Constructor { dynamic_key, type_, .. }) |
        BuiltinNode::Leaf(BuiltinLeaf::InitialBind { dynamic_key, type_ }) => {
            let ib = aspect_initial_bind_get(xx, loc, type_.as_ref()).into_res()?;
            Ok(Rc::from(ib.initial_bind(*dynamic_key)))
        },
        BuiltinNode::Leaf(BuiltinLeaf::AspectId { dynamic_key, unique, .. }) => {
            Ok(Rc::new(value::type_unique::r#const::BindingUniqueDualMode {
                dynamic_key: *dynamic_key,
                unique: *unique,
            }) as Rc<dyn Binding>)
        },
        BuiltinNode::Leaf(BuiltinLeaf::AnyConst { type_ }) => {
            let cv = CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Some(Rc::new(value::type_any::r#const::ValueAnyConst { inner: type_.clone() })),
            };
            let any_const_type: Rc<dyn Type> = Rc::new(value::type_any::r#const::TypeAnyConst);
            let bind = aspect_bind_get(xx, loc, any_const_type.as_ref()).into_res()?;
            let (_, binding) = bind.bind(xx, loc, cv).into_res()?;
            Ok(Rc::from(binding))
        },
    }
}

fn walk_register_dynamic<
    's,
>(node: &builtin_tree::BuiltinNode<'s>, dynamic_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) {
    use builtin_tree::{
        BuiltinLeaf,
        BuiltinNode,
    };

    match node {
        BuiltinNode::Branch(children) => {
            for (_, child) in children {
                walk_register_dynamic(child, dynamic_scope);
            }
        },
        BuiltinNode::Leaf(leaf) => {
            let (dynamic_key, dynamic_value) = match leaf {
                BuiltinLeaf::Function { dynamic_key, dynamic_value, .. } => (*dynamic_key, *dynamic_value),
                BuiltinLeaf::Interface { dynamic_key, dynamic_value, .. } => (*dynamic_key, *dynamic_value),
                BuiltinLeaf::AspectId { dynamic_key, dynamic_value, .. } => (*dynamic_key, *dynamic_value),
                BuiltinLeaf::Constructor { dynamic_key, dynamic_value, .. } => (*dynamic_key, *dynamic_value),
                BuiltinLeaf::AnyConst { .. } | BuiltinLeaf::InitialBind { .. } => {
                    return;
                },
            };
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(dynamic_key)).unwrap().cast(),
                    dynamic_value,
                );
        },
    }
}
