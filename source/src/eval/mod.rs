#[macro_use]
pub mod push;
#[macro_use]
pub mod util;
pub mod aspect;
pub mod builtin_tree;
pub mod code_value_extern;
pub mod definition_set;
pub mod dynamic_util;
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
            aspect::AspectPull,
            code_value_extern::TypeCodeValueExternDynamic,
            dynamic_util::wrap_with_carry,
            expr::make_lit_value_once,
            pull_scope_dispatch::{
                PullBinding,
                PullTarget,
            },
            value::{
                type_pull::r#const::TypePull,
                type_source_function::r#const::TypeSourceFunctionConst,
                type_source_interface::{
                    SourceInterfaceRef,
                    r#const::TypeSourceInterfaceConst,
                },
                type_source_object::{
                    SourceObjectRef,
                    dynamic::TypeSourceObjectDynamic,
                    r#const::TypeSourceObjectConst,
                },
                type_source_object_new::TypeSourceObjectNewConst,
                type_source_union::{
                    SourceUnionRef,
                    r#const::TypeSourceUnionConst,
                },
                type_source_union_new::SourceUnionNewRef,
                value_pull::dynamic::ValuePull,
            },
        },
        invariant::{
            must_downcast_box,
            must_downcast_ref,
        },
        langhelp::{
            Aspect,
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
            Unique,
            Value,
            downcast_value,
        },
        r#gen::{
            ASPECT_LOOKUP_FN_SCOPE_ID,
            AspectField,
            Binding,
            CAST_VALUE_FN_SCOPE_ID,
            CVE_EXTRACT_FN_SCOPE_ID,
            CVE_GET_CODE_ENUM_FN_SCOPE_ID,
            CVE_GET_VALUE_FN_SCOPE_ID,
            DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID,
            DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID,
            DATA_INSTANCE_TO_CODE_FN_SCOPE_ID,
            DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID,
            DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID,
            DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID,
            DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID,
            DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID,
            FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID,
            INTERFACE_REF_TO_TYPE_FN_SCOPE_ID,
            RECORD_TO_TYPE_FN_SCOPE_ID,
            STAGE_FN_SCOPE_ID,
            TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID,
            TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID,
            TUPLE_TO_TYPE_FN_SCOPE_ID,
            Target,
            TypeRefPush,
            UNIQUE_EQ_FN_SCOPE_ID,
            VALUE_GET_TYPE_FN_SCOPE_ID,
            aspect_artifact_get,
            aspect_bind_dynamic_get_maybe,
            aspect_bind_get,
            aspect_bool_const_get_maybe,
            aspect_consume_dynamic_get,
            aspect_field_get,
            aspect_function_get,
            aspect_initial_bind_get,
            aspect_initial_bind_get_maybe,
            aspect_iterable_get,
            aspect_lift_get,
            aspect_operator_add_get,
            aspect_operator_eq_get,
            aspect_operator_lt_get,
            aspect_post_deserialize_get,
            aspect_set_get,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get_maybe,
            aspect_type_get,
            aspect_union_get,
            gen_builtin_tree,
        },
        source_module_to_module,
        syntax::{
            ExprId,
            SourceSpecAbsolute,
        },
    },
    expr::{
        Expr,
        ExprAs,
        ExprBind,
        ExprCall,
        ExprField,
        ExprForeach,
        ExprLower,
        ExprMatch,
        ExprMatchBranch,
        ExprSeq,
        ExprSet,
        ExprStage,
        ExprTuple,
        Expr_,
        Module,
    },
    flowcontrol::shed,
    push::Push,
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
        type_null::dynamic::TypeNullDynamic,
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
                    m = m
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
                current_exception_type: Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic),
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
            let v = erb.must_er(maybe_wrap_call_exception(xx, expr.loc, callee_exc.as_ref(), v))?;
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
                if let Some(drop_aspect) =
                    crate::r#gen::aspect_drop_dynamic_get_maybe(xx, field_type.as_ref()) {
                    let dc =
                        erb.must_er(
                            crate::r#gen::AspectDropDynamic::drop(
                                drop_aspect.as_ref(),
                                xx,
                                expr.loc,
                                field_cv,
                            ),
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
                #[derive(Clone)]
                enum SeqSegment {
                    Pre(Vec<Code>),
                    BindingDrop {
                        before: Vec<Code>,
                        binding_code: Code,
                        drop_code: Option<Code>,
                    },
                }
                let mut segments: Vec<SeqSegment> = vec![SeqSegment::Pre(vec![])];
                let mut last_val: Option<Rc<dyn Value>> = None;
                let mut previous_non_binding: Option<CodeValue> = None;
                let mut control_ended = false;
                let total = e.exprs.len();
                fn push_code(segments: &mut Vec<SeqSegment>, code: Code) {
                    match segments.last_mut().unwrap() {
                        SeqSegment::Pre(v) => v.push(code),
                        SeqSegment::BindingDrop { before: _, .. } => {
                            segments.push(SeqSegment::Pre(vec![code]));
                        },
                    }
                }
                for (idx, sub) in e.exprs.iter().enumerate() {
                    if let Some(prev) = previous_non_binding.take() {
                        if let Some(prev_v) = prev.value.as_ref() {
                            let prev_type = erb.must_er(prev_v.type_(&child_xx, sub.loc))?;
                            if let Some(drop_aspect) =
                                crate::r#gen::aspect_drop_dynamic_get_maybe(&child_xx, prev_type.as_ref()) {
                                let drop_code = erb.must_er(crate::r#gen::AspectDropDynamic::drop(drop_aspect.as_ref(), &child_xx, sub.loc, prev))?;
                                push_code(&mut segments, drop_code);
                            }
                        }
                    }
                    if let Expr_::Bind(bind) = &sub.r#type {
                        er_all!(erb, {
                            base = const_eval(&child_xx, bind.base.as_ref()),
                            value = const_eval(&child_xx, bind.value.as_ref()),
                        });
                        let base_constkey = erb.must_res(extract_const_key(&child_xx, sub.loc, base.value.as_deref().unwrap()))?;
                        push_code(&mut segments, base.code);
                        let value_type = erb.must_er(value.value.as_ref().unwrap().type_(&child_xx, sub.loc))?;
                        let bind_aspect = erb.must_er(aspect_bind_get(&child_xx, sub.loc, value_type.as_ref()))?;
                        let binding = erb.must_er(bind_aspect.bind(&child_xx, sub.loc, value))?;
                        let drop_code = if let Some(drop_aspect) =
                            crate::r#gen::aspect_drop_dynamic_get_maybe(&child_xx, value_type.as_ref()) {
                            let binding_use_cv = binding.1.use_(&child_xx, sub.loc, Code { loc: sub.loc, code: Code_::None });
                            Some(erb.must_er(crate::r#gen::AspectDropDynamic::drop(drop_aspect.as_ref(), &child_xx, sub.loc, binding_use_cv))?)
                        } else {
                            None
                        };
                        let before = std::mem::replace(&mut segments, vec![]);
                        let prev_segs = before;
                        segments = prev_segs;
                        let last = match segments.pop().unwrap() {
                            SeqSegment::Pre(v) => v,
                            other => {
                                segments.push(other);
                                vec![]
                            },
                        };
                        segments.push(SeqSegment::BindingDrop {
                            before: last,
                            binding_code: binding.0,
                            drop_code,
                        });
                        segments.push(SeqSegment::Pre(vec![]));
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Rc::new(ValueNullConst) as Rc<dyn Value>);
                    } else {
                        let r = erb.must_er(const_eval(&child_xx, sub))?;
                        push_code(&mut segments, r.code.clone());
                        if r.value.is_none() {
                            control_ended = true;
                            last_val = None;
                            break;
                        }
                        if idx + 1 < total {
                            previous_non_binding = Some(r);
                        } else {
                            last_val = r.value;
                        }
                    }
                }
                fn build_segments(
                    segments: Vec<SeqSegment>,
                    loc: QExprId,
                    target: &Rc<dyn Target>,
                ) -> EResult<Vec<Code>> {
                    let mut out: Vec<Code> = vec![];
                    let mut i = 0;
                    while i < segments.len() {
                        match &segments[i] {
                            SeqSegment::Pre(v) => {
                                for c in v {
                                    out.push(c.clone());
                                }
                                i += 1;
                            },
                            SeqSegment::BindingDrop { before, binding_code, drop_code } => {
                                for c in before {
                                    out.push(c.clone());
                                }
                                out.push(binding_code.clone());
                                let rest_segs = segments[i+1..].to_vec();
                                let rest_eres = build_segments(rest_segs, loc, target);
                                if rest_eres.errors.is_some() || rest_eres.v.is_none() {
                                    return rest_eres;
                                }
                                let rest_codes = rest_eres.v.unwrap();
                                if let Some(drop_code) = drop_code {
                                    let body_eres = target.seq(loc, rest_codes);
                                    if body_eres.errors.is_some() || body_eres.v.is_none() {
                                        return EResult { errors: body_eres.errors, v: None };
                                    }
                                    let body_code = body_eres.v.unwrap();
                                    let finally_dyn = match &drop_code.code {
                                        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                                            Some(d) => d.clone(),
                                            None => return EResult::err(loc, "Drop code must produce CodeDynamic".to_string()),
                                        },
                                        Code_::None => return EResult::err(loc, "Drop code must produce CodeDynamic".to_string()),
                                    };
                                    let body_dyn_opt = match &body_code.code {
                                        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                                            Some(d) => Some(d.clone()),
                                            None => return EResult::err(loc, "Seq body must produce CodeDynamic for drop wrap".to_string()),
                                        },
                                        Code_::None => None,
                                    };
                                    if let Some(body_dyn) = body_dyn_opt {
                                        let try_code = CodeDynamic {
                                            loc,
                                            type_: CodeDynamicType::Try(crate::code_dynamic::CodeDynamicTry {
                                                body: Box::new(body_dyn),
                                                catch: None,
                                                finally: Some(Box::new(finally_dyn)),
                                            }),
                                        };
                                        out.push(Code {
                                            loc,
                                            code: Code_::Dyn(Rc::new(try_code)),
                                        });
                                    } else {
                                        out.push(Code {
                                            loc,
                                            code: Code_::Dyn(Rc::new(finally_dyn)),
                                        });
                                    }
                                } else {
                                    for c in rest_codes {
                                        out.push(c);
                                    }
                                }
                                return EResult::ok(out);
                            },
                        }
                    }
                    EResult::ok(out)
                }
                let final_codes = erb.must_er(build_segments(segments, expr.loc, &xx.target))?;
                let _ = control_ended;
                return Ok(CodeValue {
                    code: erb.must_er(xx.target.seq(expr.loc, final_codes))?,
                    value: last_val.or_else(|| if control_ended { None } else { Some(Rc::new(ValueNullConst) as Rc<dyn Value>) }),
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
                let body_exc_type: Rc<dyn Type> = if let Some(catch) = &e.catch {
                    let exc_type_cv = erb.must_er(const_eval(xx, catch.exception_type.as_ref()))?;
                    let exc_meta = erb.must_er(exc_type_cv.value.as_ref().unwrap().type_(xx, expr.loc))?;
                    let aspect_type = erb.must_er(aspect_type_get(xx, expr.loc, exc_meta.as_ref()))?;
                    aspect_type.type_(exc_type_cv.value.as_deref().unwrap())
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
                let body_dyn = match &body_cv.code.code {
                    Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                        Some(d) => d.clone(),
                        None => return erb.err_new("Try body must produce CodeDynamic".to_string()),
                    },
                    Code_::None => return erb.err_new("Try body must produce CodeDynamic (got Code::None)".to_string()),
                };
                let mut result_value: Option<Rc<dyn Value>> = body_cv.value.clone();
                let catch_dyn = if let Some(catch) = &e.catch {
                    let binding_key_cv = erb.must_er(const_eval(xx, catch.binding_name.as_ref()))?;
                    let binding_constkey = erb.must_res(extract_const_key(xx, catch.binding_name.loc, binding_key_cv.value.as_deref().unwrap()))?;
                    let binding_id = BindingId::Local(xx.vx.next_local_id.next());
                    let catch_scope = Rc::new(Scope {
                        values: RefCell::new(HashMap::new()),
                        parent: Some(xx.scope.clone()),
                    });
                    if let Some(ib) = crate::r#gen::aspect_initial_bind_get_maybe(xx, body_exc_type.as_ref()) {
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
                    if result_value.is_none() {
                        result_value = catch_cv.value.clone();
                    }
                    let catch_code_dyn = match &catch_cv.code.code {
                        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
                            Some(d) => d.clone(),
                            None => return erb.err_new("Try catch body must produce CodeDynamic".to_string()),
                        },
                        Code_::None => return erb.err_new("Try catch body must produce CodeDynamic (got Code::None)".to_string()),
                    };
                    Some(crate::code_dynamic::CodeDynamicCatch {
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
                        Code_::None => return erb.err_new("Try finally body must produce CodeDynamic (got Code::None)".to_string()),
                    };
                    Some(Box::new(finally_code_dyn))
                } else {
                    None
                };
                let try_code = CodeDynamic {
                    loc: expr.loc,
                    type_: CodeDynamicType::Try(crate::code_dynamic::CodeDynamicTry {
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

pub(crate) struct DynamicEvalContextSlot {
    pub(crate) ex: Rc<EvalContext>,
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
    dynamic_scope_isolate.set_slot(DynamicEvalContextSlot { ex: ex.clone() });
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
        current_exception_type: Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic),
    });
    let register_dynamic = !cix.builtins_registered.get();
    {
        let scope_entries = init_builtin_scope(&xx, root_loc, import_state, dynamic_scope, register_dynamic)?;
        scope.values.borrow_mut().extend(scope_entries);
    }
    {
        fn stage_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let tree_cell: Rc<expr::StagedExprCell> =
                from_dynamic_ref::<Rc<expr::StagedExprCell>>(&*args.get(0)).clone();
            let tree_value = tree_cell.value.borrow_mut().take().expect("stage tree already consumed");
            let tree = must_downcast_ref::<Expr>(tree_value.as_ref()).clone();

            enum StageLowerValue {
                Expr(Expr),
                Value(Rc<dyn Value>),
            }

            let lower_count =
                args.get(1).integer_value(scope).expect("__stage: lower_count not an integer") as usize;
            let mut values: Vec<StageLowerValue> = Vec::new();
            let mut i = 2;
            for _ in 0 .. lower_count {
                let lift_arg = args.get(i);
                let val_arg = args.get(i + 1);
                i += 2;
                if lift_arg.is_null_or_undefined() {
                    let cell: Rc<expr::StagedExprCell> =
                        from_dynamic_ref::<Rc<expr::StagedExprCell>>(&*val_arg).clone();
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
                    let lift_cell: Rc<StageLiftCell> = from_dynamic_ref::<Rc<StageLiftCell>>(&*lift_arg).clone();
                    let lift_xx = ExprContext {
                        vx: lift_cell.vx.clone(),
                        import_state: lift_cell.import_state.clone(),
                        scope: lift_cell.scope.clone(),
                        target: lift_cell.target.clone(),
                        current_exception_type: lift_cell.current_exception_type.clone(),
                    };
                    let lift =
                        aspect_lift_get(&lift_xx, lift_cell.loc, lift_cell.source_type.as_ref())
                            .into_res()
                            .expect("__stage: source type missing AspectLift");
                    let value = lift.lift(&lift_xx, &*scope, val_arg).expect("__stage: lift failed");
                    values.push(StageLowerValue::Value(value));
                }
            }
            let mut graft_values: Vec<CodeValue> = Vec::new();
            while i < args.length() {
                let graft_arg = args.get(i);
                i += 1;
                let cell: Rc<RefCell<Option<CodeValue>>> =
                    from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*graft_arg).clone();
                let cv = cell.borrow_mut().take().expect("__stage: graft CodeValueExtern already consumed");
                graft_values.push(cv);
            }

            fn clone_with_lower_substitutions(
                expr: &Expr,
                sd: usize,
                ld: usize,
                values: &mut dyn Iterator<Item = StageLowerValue>,
                grafts: &mut dyn Iterator<Item = CodeValue>,
            ) -> Expr {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            match values.next().expect("Not enough lower values for substitution") {
                                StageLowerValue::Expr(expr_val) => expr_val,
                                StageLowerValue::Value(value) => Expr {
                                    loc: expr.loc,
                                    r#type: Expr_::Literal(make_lit_value_once(value)),
                                },
                            }
                        } else {
                            Expr {
                                loc: expr.loc,
                                r#type: Expr_::Lower(
                                    ExprLower {
                                        expr: Rc::new(
                                            clone_with_lower_substitutions(&e.expr, sd, new_ld, values, grafts),
                                        ),
                                    },
                                ),
                            }
                        }
                    },
                    Expr_::Stage(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Stage(
                            ExprStage {
                                expr: Rc::new(clone_with_lower_substitutions(&e.expr, sd + 1, ld, values, grafts)),
                            },
                        ),
                    },
                    Expr_::Graft(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Graft(match e {
                            expr::ExprGraft::Pending(_) if sd == 0 => expr::ExprGraft::Evaluated(
                                grafts.next().expect("Not enough graft values for substitution"),
                            ),
                            expr::ExprGraft::Pending(inner) => expr::ExprGraft::Pending(
                                Rc::new(clone_with_lower_substitutions(inner, sd, ld, values, grafts)),
                            ),
                            expr::ExprGraft::Evaluated(cv) => expr::ExprGraft::Evaluated(cv.clone()),
                        }),
                    },
                    Expr_::Literal(_) | Expr_::ScopeUse(_) | Expr_::Import(_) => expr.clone(),
                    Expr_::As(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::As(ExprAs {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            target: Rc::new(clone_with_lower_substitutions(&e.target, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Call(c) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Call(ExprCall {
                            func: Rc::new(clone_with_lower_substitutions(&c.func, sd, ld, values, grafts)),
                            arg: Rc::new(clone_with_lower_substitutions(&c.arg, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Bind(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Bind(ExprBind {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Set(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Set(ExprSet {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            value: Rc::new(clone_with_lower_substitutions(&e.value, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Field(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Field(ExprField {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            key: Rc::new(clone_with_lower_substitutions(&e.key, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Seq(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Seq(
                            ExprSeq {
                                exprs: e
                                    .exprs
                                    .iter()
                                    .map(|sub| clone_with_lower_substitutions(sub, sd, ld, values, grafts))
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
                                    .map(|e| clone_with_lower_substitutions(e, sd, ld, values, grafts))
                                    .collect(),
                            },
                        ),
                    },
                    Expr_::Record(r) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Record(expr::ExprRecord { entries: r.entries.iter().map(|(k, v)| {
                            (
                                clone_with_lower_substitutions(k, sd, ld, values, grafts),
                                clone_with_lower_substitutions(v, sd, ld, values, grafts),
                            )
                        }).collect() }),
                    },
                    Expr_::If(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::If(expr::ExprIf {
                            conditions: e.conditions.iter().map(|b| expr::ExprConditionBranch {
                                condition: clone_with_lower_substitutions(&b.condition, sd, ld, values, grafts),
                                body: clone_with_lower_substitutions(&b.body, sd, ld, values, grafts),
                            }).collect(),
                            else_: e
                                .else_
                                .as_ref()
                                .map(|el| Rc::new(clone_with_lower_substitutions(el, sd, ld, values, grafts))),
                        }),
                    },
                    Expr_::OperatorLt(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorLt(expr::ExprOperatorLt {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(
                                clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),
                            ),
                        }),
                    },
                    Expr_::OperatorEq(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorEq(expr::ExprOperatorEq {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(
                                clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),
                            ),
                        }),
                    },
                    Expr_::OperatorAdd(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::OperatorAdd(expr::ExprOperatorAdd {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            reference: Rc::new(
                                clone_with_lower_substitutions(&e.reference, sd, ld, values, grafts),
                            ),
                        }),
                    },
                    Expr_::Foreach(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Foreach(ExprForeach {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            element_key: Rc::new(
                                clone_with_lower_substitutions(&e.element_key, sd, ld, values, grafts),
                            ),
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values, grafts)),
                        }),
                    },
                    Expr_::Match(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Match(ExprMatch {
                            base: Rc::new(clone_with_lower_substitutions(&e.base, sd, ld, values, grafts)),
                            element_key: Rc::new(
                                clone_with_lower_substitutions(&e.element_key, sd, ld, values, grafts),
                            ),
                            cases: e.cases.iter().map(|c| ExprMatchBranch {
                                condition: clone_with_lower_substitutions(&c.condition, sd, ld, values, grafts),
                                body: clone_with_lower_substitutions(&c.body, sd, ld, values, grafts),
                            }).collect(),
                            fallback: e
                                .fallback
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values, grafts))),
                        }),
                    },
                    Expr_::Try(e) => Expr {
                        loc: expr.loc,
                        r#type: Expr_::Try(expr::ExprTry {
                            body: Rc::new(clone_with_lower_substitutions(&e.body, sd, ld, values, grafts)),
                            catch: e.catch.as_ref().map(|c| expr::ExprTryCatch {
                                binding_name: Rc::new(
                                    clone_with_lower_substitutions(&c.binding_name, sd, ld, values, grafts),
                                ),
                                exception_type: Rc::new(
                                    clone_with_lower_substitutions(&c.exception_type, sd, ld, values, grafts),
                                ),
                                body: Rc::new(clone_with_lower_substitutions(&c.body, sd, ld, values, grafts)),
                            }),
                            finally: e
                                .finally
                                .as_ref()
                                .map(|f| Rc::new(clone_with_lower_substitutions(f, sd, ld, values, grafts))),
                        }),
                    },
                }
            }

            let new_tree =
                clone_with_lower_substitutions(&tree, 0, 0, &mut values.into_iter(), &mut graft_values.into_iter());
            let result_cell =
                Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Rc::new(new_tree) as Rc<dyn Value>)) });
            let ext = to_dynamic_ext(scope, result_cell);
            rv.set(ext.into());
        }

        if register_dynamic {
            let stage_fn = v8::Function::builder(stage_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(STAGE_FN_SCOPE_ID)).unwrap().cast(),
                    stage_fn.into(),
                );
        }
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

    struct DefFuncInfo {
        arg_type: Rc<dyn Type>,
        binding_id: BindingId,
        impl_expr_ast: Rc<dyn Value>,
        ret_type: Rc<dyn Type>,
        exception_type: Rc<dyn Type>,
    }

    {
        let mut to_visit: Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>> =
            ex.definition_sets.borrow().values().cloned().collect();
        while let Some(ds) = to_visit.pop() {
            for (dep_id, dep) in ds.dependencies.borrow().iter() {
                let inserted = ex.definition_sets.borrow_mut().insert(*dep_id, dep.clone()).is_none();
                if inserted {
                    to_visit.push(dep.clone());
                }
            }
        }
    }
    let mut precompiled_def_funcs: Vec<String> = vec![];
    let def_func_infos: Vec<DefFuncInfo> = {
        let sets = ex.definition_sets.borrow();
        let mut infos = vec![];
        for ds in sets.values() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.impl_expr.borrow().is_none() {
                    if let Some(dynamic_code) = func_data.compiled_source.borrow().clone() {
                        precompiled_def_funcs.push(dynamic_code);
                    }
                    continue;
                }
                let impl_expr_ast =
                    func_data.impl_expr.borrow_mut().take().expect("Definition set function missing implementation");
                let arg_type = func_data.arg_type.borrow().clone().expect("Definition set function missing arg type");
                let ret_type =
                    func_data.ret_type.borrow().clone().expect("Definition set function missing return type");
                let scope_id = func_data.binding_id.get().expect("Definition set function not registered");
                let exception_type = func_data
                    .exception_type
                    .borrow()
                    .clone()
                    .unwrap_or_else(|| Rc::new(crate::eval::value::type_never::r#const::TypeNever));
                infos.push(DefFuncInfo {
                    impl_expr_ast,
                    arg_type,
                    ret_type,
                    binding_id: scope_id,
                    exception_type,
                });
            }
        }
        infos
    };
    let mut def_funcs: Vec<(BindingId, Option<BindingId>, CodeDynamic, Vec<(BindingId, External)>)> = vec![];
    for info in &def_func_infos {
        let impl_expr_ast = must_downcast_ref::<Expr>(info.impl_expr_ast.as_ref());
        let body_ex = Rc::new(EvalContext {
            ix: ex.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            dynamic_scope: std::cell::Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let arg_id = BindingId::Local(body_ex.next_local_id.next());
        let func_xx = {
            let mut body_scope = body_builtin_scope(&xx);
            if let Some(arg_ib) = aspect_initial_bind_get_maybe(&xx, info.arg_type.as_ref()) {
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
            }
            Rc::new(ExprContext {
                vx: body_ex,
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope {
                    values: RefCell::new(body_scope),
                    parent: None,
                }),
                target: target.clone(),
                current_exception_type: info.exception_type.clone(),
            })
        };
        let impl_result = const_eval(&func_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();
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
                .expect("Definition set function body must produce dynamic code")
                .clone();
        let captured = func_xx.vx.externals.borrow().clone();
        for ds in ex.definition_sets.borrow().values() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.binding_id.get() == Some(info.binding_id) {
                    *func_data.captured_externals.borrow_mut() = captured.clone();
                }
            }
        }
        def_funcs.push((info.binding_id, Some(arg_id), impl_code, captured));
    }

    enum CompiledDataMethodBody {
        Fresh {
            inner_binding_id: BindingId,
            data_field: String,
            impl_code: CodeDynamic,
            is_arg_method: bool,
            outer_id: BindingId,
            arg_param_id: BindingId,
        },
        Precompiled(String),
    }

    struct CompiledDataMethod {
        binding_id: BindingId,
        body: CompiledDataMethodBody,
        captured_externals: Vec<(BindingId, External)>,
    }

    let mut def_data_methods: Vec<CompiledDataMethod> = vec![];
    let dss: Vec<_> = ex.definition_sets.borrow().values().cloned().collect();
    for ds in &dss {
        let dts: Vec<_> = ds.objects.borrow().iter().cloned().collect();
        for dt_data in &dts {
            let underlying_type: Rc<dyn Type> = dt_data.underlying_type.get().clone();
            let data_field = dt_data.data_field.clone();
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.binding_id.get().expect("Method binding ID not assigned");
                if let Some(compiled_source) = method.compiled_source.borrow().clone() {
                    def_data_methods.push(CompiledDataMethod {
                        binding_id: method_id,
                        body: CompiledDataMethodBody::Precompiled(compiled_source),
                        captured_externals: method.captured_externals.borrow().clone(),
                    });
                    continue;
                }
                let impl_expr = method.impl_expr.borrow();
                let impl_expr_ast = must_downcast_ref::<expr::Expr>(impl_expr.as_ref().unwrap().as_ref());
                let ret_type: Rc<dyn Type> =
                    method.ret_type.borrow().clone().expect("Data type method missing return type");
                let body_ex = Rc::new(EvalContext {
                    ix: ex.ix.clone(),
                    next_local_id: Counter::default(),
                    externals: RefCell::new(Vec::new()),
                    dynamic_scope: std::cell::Cell::new(None),
                    definition_sets: RefCell::new(std::collections::HashMap::new()),
                });
                let self_id = BindingId::Local(body_ex.next_local_id.next());
                method.inner_binding_id.set(Some(self_id));
                let arg_id = BindingId::Local(body_ex.next_local_id.next());
                let outer_id = BindingId::Builtin(usize::MAX);
                let method_xx = {
                    let mut body_scope = body_builtin_scope(&xx);
                    if let Some(self_ib) = aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                        body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                    }
                    if let Some(arg_type) = method.arg_type.borrow().clone() {
                        if let Some(arg_ib) = aspect_initial_bind_get_maybe(&xx, arg_type.as_ref()) {
                            body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
                        }
                    }
                    Rc::new(ExprContext {
                        vx: body_ex,
                        import_state: xx.import_state.clone(),
                        scope: Rc::new(Scope {
                            values: RefCell::new(body_scope),
                            parent: None,
                        }),
                        target: target.clone(),
                        current_exception_type: Rc::new(crate::eval::value::type_never::r#const::TypeNever),
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
                        .expect("Method body must produce dynamic code")
                        .clone();
                let captured = method_xx.vx.externals.borrow().clone();
                *method.captured_externals.borrow_mut() = captured.clone();
                def_data_methods.push(CompiledDataMethod {
                    binding_id: method_id,
                    body: CompiledDataMethodBody::Fresh {
                        inner_binding_id: self_id,
                        data_field: data_field.clone(),
                        impl_code,
                        is_arg_method: false,
                        outer_id,
                        arg_param_id: arg_id,
                    },
                    captured_externals: captured,
                });
            }
            for impl_data in dt_data.interface_impls.borrow().iter() {
                let builtin_kind = impl_data.interface.builtin_kind;
                for method_impl in &impl_data.method_impls {
                    let method_id =
                        method_impl.binding_id.get().expect("Interface method impl binding ID not assigned");
                    if let Some(compiled_source) = method_impl.compiled_source.borrow().clone() {
                        def_data_methods.push(CompiledDataMethod {
                            binding_id: method_id,
                            body: CompiledDataMethodBody::Precompiled(compiled_source),
                            captured_externals: method_impl.captured_externals.borrow().clone(),
                        });
                        continue;
                    }
                    let impl_expr_value =
                        method_impl.impl_expr.borrow().clone().expect("Interface method impl missing impl_expr");
                    let impl_expr_ast = must_downcast_ref::<expr::Expr>(impl_expr_value.as_ref());
                    let ret_type: Rc<dyn Type> = {
                        let iface_methods = impl_data.interface.methods.borrow();
                        let iface_method = &iface_methods[method_impl.interface_method_index];
                        iface_method.ret_type.borrow().clone().expect("Interface method missing return type")
                    };
                    let body_ex = Rc::new(EvalContext {
                        ix: ex.ix.clone(),
                        next_local_id: Counter::default(),
                        externals: RefCell::new(Vec::new()),
                        dynamic_scope: std::cell::Cell::new(None),
                        definition_sets: RefCell::new(std::collections::HashMap::new()),
                    });
                    let self_id = BindingId::Local(body_ex.next_local_id.next());
                    method_impl.inner_binding_id.set(Some(self_id));
                    let outer_id = BindingId::Local(body_ex.next_local_id.next());
                    let arg_id = BindingId::Local(body_ex.next_local_id.next());
                    let method_xx = {
                        let mut body_scope = body_builtin_scope(&xx);
                        let outer_type: Rc<dyn Type> =
                            Rc::new(
                                value::type_source_object::dynamic::TypeSourceObjectDynamic { data: dt_data.clone() },
                            );
                        if let Some(outer_self_ib) = aspect_initial_bind_get_maybe(&xx, outer_type.as_ref()) {
                            body_scope.insert(
                                ConstKey::String("outer".to_string()),
                                outer_self_ib.initial_bind(outer_id),
                            );
                        }
                        let (iface_method_arg_record_type, iface_method_arg_type) = {
                            let iface_methods = impl_data.interface.methods.borrow();
                            let m = &iface_methods[method_impl.interface_method_index];
                            (m.arg_record_type.borrow().clone(), m.arg_type.borrow().clone())
                        };
                        let arg_binding_id = match builtin_kind {
                            Some(value::type_source_interface::BuiltinInterfaceKind::Type) => self_id,
                            _ => arg_id,
                        };
                        match builtin_kind {
                            Some(value::type_source_interface::BuiltinInterfaceKind::Type) => {
                                let arg_type =
                                    iface_method_arg_record_type
                                        .clone()
                                        .expect("Type iface method missing arg_record_type");
                                let arg_ib = aspect_initial_bind_get(&xx, root_loc, arg_type.as_ref()).into_res()?;
                                body_scope.insert(
                                    ConstKey::String("arg".to_string()),
                                    arg_ib.initial_bind(arg_binding_id),
                                );
                            },
                            Some(value::type_source_interface::BuiltinInterfaceKind::Binding) |
                            Some(value::type_source_interface::BuiltinInterfaceKind::Target) => {
                                if let Some(self_ib) = aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("inner".to_string()),
                                        self_ib.initial_bind(self_id),
                                    );
                                }
                                let method_name = {
                                    let iface_methods = impl_data.interface.methods.borrow();
                                    iface_methods[method_impl.interface_method_index].name.clone()
                                };
                                let arg_field_names: Vec<&str> = match (builtin_kind, method_name.as_str()) {
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Binding),
                                        "use",
                                    ) => vec![
                                        "loc",
                                        "code",
                                    ],
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Target),
                                        "bind",
                                    ) => vec![
                                        "xx",
                                        "loc",
                                        "sub",
                                    ],
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Target),
                                        "bind_sub_record",
                                    ) => vec![
                                        "loc",
                                        "carry",
                                        "fields",
                                    ],
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Target),
                                        "bind_sub_tuple",
                                    ) => vec![
                                        "loc",
                                        "carry",
                                        "elements",
                                    ],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "seq") => vec![
                                        "loc",
                                        "entries",
                                    ],
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Target),
                                        "code_to_bind_sub",
                                    ) => vec![
                                        "loc",
                                        "code",
                                        "type",
                                    ],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "if") => vec![
                                        "xx",
                                        "loc",
                                        "conditions",
                                        "else",
                                    ],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "match") => {
                                        vec!["xx", "loc", "base", "element_key", "cases", "fallback"]
                                    },
                                    (
                                        Some(value::type_source_interface::BuiltinInterfaceKind::Target),
                                        "maybe_bind_sub",
                                    ) => vec![
                                        "xx",
                                        "loc",
                                        "cv",
                                    ],
                                    (Some(value::type_source_interface::BuiltinInterfaceKind::Target), "stage") => {
                                        vec!["xx", "loc", "tree", "lower_values"]
                                    },
                                    _ => vec![],
                                };
                                let mut fields = std::collections::BTreeMap::new();
                                let cve_type: Rc<dyn Type> = Rc::new(TypeCodeValueExternDynamic);
                                for name in &arg_field_names {
                                    fields.insert(
                                        ConstKey::String(name.to_string()),
                                        value::type_record::RecordField::Dynamic(cve_type.clone()),
                                    );
                                }
                                let arg_type: Rc<dyn Type> = Rc::new(value::type_record::Record {
                                    base: Some(Box::new(value::type_record::RecordBaseDynamic)),
                                    fields,
                                });
                                if let Some(arg_ib) = aspect_initial_bind_get_maybe(&xx, arg_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("arg".to_string()),
                                        arg_ib.initial_bind(arg_binding_id),
                                    );
                                }
                            },
                            _ => {
                                if let Some(self_ib) = aspect_initial_bind_get_maybe(&xx, underlying_type.as_ref()) {
                                    body_scope.insert(
                                        ConstKey::String("inner".to_string()),
                                        self_ib.initial_bind(self_id),
                                    );
                                }
                                let arg_type = iface_method_arg_record_type.clone().or(iface_method_arg_type.clone());
                                if let Some(arg_type) = arg_type {
                                    if let Some(arg_ib) = aspect_initial_bind_get_maybe(&xx, arg_type.as_ref()) {
                                        body_scope.insert(
                                            ConstKey::String("arg".to_string()),
                                            arg_ib.initial_bind(arg_binding_id),
                                        );
                                    }
                                }
                            },
                        }
                        Rc::new(ExprContext {
                            vx: body_ex,
                            import_state: xx.import_state.clone(),
                            scope: Rc::new(Scope {
                                values: RefCell::new(body_scope),
                                parent: None,
                            }),
                            target: target.clone(),
                            current_exception_type: Rc::new(crate::eval::value::type_never::r#const::TypeNever),
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
                            .expect("Interface method body must produce dynamic code")
                            .clone();
                    let captured = method_xx.vx.externals.borrow().clone();
                    *method_impl.captured_externals.borrow_mut() = captured.clone();
                    def_data_methods.push(CompiledDataMethod {
                        binding_id: method_id,
                        body: CompiledDataMethodBody::Fresh {
                            inner_binding_id: self_id,
                            data_field: data_field.clone(),
                            impl_code,
                            is_arg_method: matches!(
                                builtin_kind,
                                Some(value::type_source_interface::BuiltinInterfaceKind::Type)
                            ),
                            outer_id,
                            arg_param_id: arg_id,
                        },
                        captured_externals: captured,
                    });
                }
            }
        }
    }
    let has_js = stage1_code.is_some() || !def_funcs.is_empty() || !def_data_methods.is_empty();
    if !has_js {
        let logs = dynamic_scope.get_slot::<DynamicLogStore>().unwrap().logs.take();
        return Ok(EvalResult {
            value: fallback_value.expect("No dynamic code and no fallback value").expect("Fallback value is None"),
            logs,
        });
    }
    let mut code1 = DynamicSourceBuilder::default();
    for dynamic_code in &precompiled_def_funcs {
        for line in dynamic_code.lines() {
            code1.push(root_loc, line.to_string());
        }
    }
    for (scope_id, arg_scope_id, impl_code, captured_externals) in &def_funcs {
        let start_line = code1.lines.len();
        let func_name = binding_id_dynamic_key(*scope_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in captured_externals {
            let local_key = binding_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key,));
        }
        if let Some(arg_id) = arg_scope_id {
            code1.push(root_loc, format!("{} = function({}) {{", func_name, binding_id_dynamic_key(*arg_id)));
        } else {
            code1.push(root_loc, format!("{} = function() {{", func_name));
        }
        let body_var = render_code(&mut code1, impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines[start_line..].join("\n");
        for ds in ex.definition_sets.borrow().values() {
            for func_data in ds.functions.borrow().iter() {
                if func_data.binding_id.get() == Some(*scope_id) {
                    if let Some(errs) = ds.check_mutable(root_loc).errors {
                        return Err(errs);
                    }
                    *func_data.compiled_source.borrow_mut() = Some(compiled.clone());
                }
            }
        }
    }
    for method in &def_data_methods {
        match &method.body {
            CompiledDataMethodBody
            ::Fresh {
                inner_binding_id,
                data_field,
                impl_code,
                is_arg_method,
                outer_id,
                arg_param_id,
            } => {
                let start_line = code1.lines.len();
                let func_name = binding_id_dynamic_key(method.binding_id);
                let self_key = binding_id_dynamic_key(*inner_binding_id);
                code1.push(root_loc, format!("var {};", func_name));
                code1.push(root_loc, "{".to_string());
                for (id, _) in &method.captured_externals {
                    let local_key = binding_id_dynamic_key(*id);
                    code1.push(
                        root_loc,
                        format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key,),
                    );
                }
                if *is_arg_method {
                    code1.push(root_loc, format!("{} = function({}) {{", func_name, self_key));
                } else {
                    let arg_key = binding_id_dynamic_key(*arg_param_id);
                    code1.push(root_loc, format!("{} = function({}) {{", func_name, arg_key));
                    code1.push(
                        root_loc,
                        format!(
                            "let {} = this[{}];",
                            self_key,
                            serde_json::to_string(&ConstKey::String(data_field.clone()).to_str()).unwrap(),
                        ),
                    );
                }
                if code_uses_binding(impl_code, *outer_id) {
                    code1.push(root_loc, format!("let {} = this;", binding_id_dynamic_key(*outer_id)));
                }
                let body_var = render_code(&mut code1, impl_code);
                code1.push(root_loc, format!("return {};", body_var));
                code1.push(root_loc, "};".to_string());
                code1.push(root_loc, "}".to_string());
                let compiled = code1.lines[start_line..].join("\n");
                for ds in ex.definition_sets.borrow().values() {
                    for dt_data in ds.objects.borrow().iter() {
                        for m in dt_data.methods.borrow().iter() {
                            if m.binding_id.get() == Some(method.binding_id) {
                                if let Some(errs) = ds.check_mutable(root_loc).errors {
                                    return Err(errs);
                                }
                                *m.compiled_source.borrow_mut() = Some(compiled.clone());
                            }
                        }
                        for impl_data in dt_data.interface_impls.borrow().iter() {
                            for m in &impl_data.method_impls {
                                if m.binding_id.get() == Some(method.binding_id) {
                                    if let Some(errs) = ds.check_mutable(root_loc).errors {
                                        return Err(errs);
                                    }
                                    *m.compiled_source.borrow_mut() = Some(compiled.clone());
                                }
                            }
                        }
                    }
                }
            },
            CompiledDataMethodBody::Precompiled(dynamic_code) => {
                for line in dynamic_code.lines() {
                    code1.push(root_loc, line.to_string());
                }
            },
        }
    }
    code1.push(root_loc, "{/*main*/".to_string());
    if let Some(code) = stage1_code {
        let root_var = render_code(&mut code1, &code);
        code1.push(root_loc, format!("{};", root_var));
    } else {
        code1.push(root_loc, "null;".to_string());
    }
    code1.push(root_loc, "}/*main*/".to_string());
    {
        fn dynamic_value_to_type(
            _scope: &mut v8::PinnedRef<v8::HandleScope>,
            value: v8::Local<v8::Value>,
        ) -> Rc<dyn Type> {
            if value.is_external() {
                let any = from_dynamic_any(&*value);
                if any.is::<Rc<dyn Type>>() {
                    return must_downcast_ref::<Rc<dyn Type>>(any).clone();
                }
                if let Some(iface_ref) = any.downcast_ref::<Rc<SourceInterfaceRef>>() {
                    return Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() });
                }
                if let Some(tu_ref) = any.downcast_ref::<Rc<SourceUnionRef>>() {
                    return Rc::new(TypeSourceUnionConst { data: tu_ref.data.clone() });
                }
                if let Some(dt_ref) = any.downcast_ref::<Rc<SourceObjectRef>>() {
                    return Rc::new(TypeSourceObjectDynamic { data: dt_ref.data.clone() });
                }
                return from_dynamic_ref::<Rc<dyn Type>>(&*value).clone();
            }
            panic!("dynamic_value_to_type: expected `Rc<dyn Type>` external, got something else");
        }

        fn record_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let value = args.get(0);
            let obj = v8::Local::<v8::Object>::try_from(value).expect("__record_to_type: expected dynamic object");
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
                fields.insert(const_key, RecordField::Dynamic(dynamic_value_to_type(scope, val)));
            }
            let type_: Rc<dyn Type> = Rc::new(Record {
                base: None,
                fields,
            });
            let ext = rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        }

        fn tuple_to_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let value = args.get(0);
            let arr = v8::Local::<v8::Array>::try_from(value).expect("__tuple_to_type: expected dynamic array");
            let len = arr.length();
            let mut elements = Vec::with_capacity(len as usize);
            for i in 0 .. len {
                let elem = arr.get_index(scope, i).unwrap();
                elements.push(TupleElement::Dynamic(dynamic_value_to_type(scope, elem)));
            }
            let type_: Rc<dyn Type> = Rc::new(Tuple { elements: elements });
            let ext = rc_type_to_dynamic_ext(scope, type_);
            rv.set(ext.into());
        }

        fn cve_extract_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<CodeValue>>> =
                from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*cve_v8).clone();
            let inner_value: Rc<dyn Value> = {
                let guard = cve_cell.borrow();
                let cv = guard.as_ref().expect("__cve_extract: inner CodeValue already consumed");
                cv.value.clone().expect("__cve_extract: inner CodeValue has no value")
            };
            let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                current_exception_type: Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic),
                target: Rc::new(TargetDynamic) as Rc<dyn Target>,
            });
            let loc = (ex_slot.ex.ix.source_id, ExprId(0));
            let value_type =
                inner_value.type_(&xx, loc).into_res().expect("__cve_extract: inner CV type_() returned errors");
            if let Some(aspect_str) = aspect_string_const_get_maybe(&xx, value_type.as_ref()) {
                let s = aspect_str.get(inner_value.as_ref());
                rv.set(v8::String::new(scope, &s).unwrap().into());
                return;
            }
            panic!("__cve_extract: unsupported inner CV value type ({})", value_type.desc());
        }

        fn cve_get_code_enum_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<CodeValue>>> =
                from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*cve_v8).clone();
            let inner_code: Code = {
                let guard = cve_cell.borrow();
                let cv = guard.as_ref().expect("__cve_get_code_enum: inner CodeValue already consumed");
                cv.code.clone()
            };
            let obj = v8::Object::new(scope);
            match inner_code.code {
                Code_::None => {
                    let key = v8::String::new(scope, &ConstKey::String("none".to_string()).to_str()).unwrap();
                    let null: v8::Local<v8::Value> = v8::null(scope).into();
                    obj.set(scope, key.into(), null);
                },
                Code_::Dyn(c) => {
                    let key = v8::String::new(scope, &ConstKey::String("dyn".to_string()).to_str()).unwrap();
                    let dynamic_val: v8::Local<v8::Value> =
                        if let Some(pull) = (c.as_ref() as &dyn std::any::Any).downcast_ref::<CodePull>() {
                            v8::Local::new(scope, &pull.instance).into()
                        } else {
                            to_dynamic_ext(scope, c).into()
                        };
                    obj.set(scope, key.into(), dynamic_val);
                },
            }
            rv.set(obj.into());
        }

        fn cve_get_value_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let cve_v8 = args.get(0);
            let cve_cell: Rc<RefCell<Option<CodeValue>>> =
                from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*cve_v8).clone();
            let value: Rc<dyn Value> = {
                let borrow = cve_cell.borrow();
                borrow.as_ref().expect("__cve_get_value: inner CodeValue already consumed").value.clone().expect("__cve_get_value: no value")
            };
            let ext = rc_value_to_dynamic_ext(scope, value);
            rv.set(ext.into());
        }

        fn value_get_type_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let value: Rc<dyn Value> = rc_value_from_v8(&*args.get(0));
            let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
            let target: Rc<dyn Target> = Rc::new(TargetDynamic);
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                current_exception_type: Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic),
                target,
            });
            let loc = (ex_slot.ex.ix.source_id, ExprId(0));
            let type_ref: Rc<dyn Type> =
                value.type_(&xx, loc).into_res().expect("value_get_type: type_() returned errors");
            let type_ref_push = Push::into_dynamic(TypeRefPush { type_ref }, scope);
            rv.set(type_ref_push);
        }

        fn aspect_lookup_callback<
            's,
        >(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let arg0 = args.get(0);
            let arg1 = args.get(1);
            let arg0_obj =
                v8::Local::<v8::Object>::try_from(
                    arg0,
                ).expect("__aspect_lookup: expected dynamic object for TypeRefPush");
            let d_key = v8::String::new(scope, "_d").expect("__aspect_lookup: alloc _d key");
            let d_val = arg0_obj.get(scope, d_key.into()).expect("__aspect_lookup: TypeRefPush missing _d field");
            let type_ref: Rc<dyn Type> = from_dynamic_ref::<Rc<TypeRefPush>>(&*d_val).type_ref.clone();
            let unique: Unique = *from_dynamic_ref::<Unique>(&*arg1);
            let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
            let target: Rc<dyn Target> = Rc::new(TargetDynamic);
            let xx = Rc::new(ExprContext {
                vx: ex_slot.ex.clone(),
                import_state: Rc::new(Cell::new(ex_slot.ex.ix.import_state)),
                scope: Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: None,
                }),
                current_exception_type: Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic),
                target,
            });
            let result = v8::Object::new(scope);
            match type_ref.aspect(&xx, &unique) {
                Some(aspect_box) => {
                    let aspect_field: Box<dyn AspectField> =
                        must_downcast_box::<Box<dyn AspectField>>(aspect_box);
                    let aspect_dyn: &dyn Aspect = &*aspect_field;
                    let instance_dynamic: v8::Local<v8::Value> = match aspect_dyn.dynamic_instance() {
                        Some(g) => v8::Local::new(scope, &g).into(),
                        None => v8::null(scope).into(),
                    };
                    let some_key = v8::String::new(scope, &ConstKey::String("some".to_string()).to_str()).unwrap();
                    result.set(scope, some_key.into(), instance_dynamic);
                },
                None => {
                    let none_key = v8::String::new(scope, &ConstKey::String("none".to_string()).to_str()).unwrap();
                    let null_v8: v8::Local<v8::Value> = v8::null(scope).into();
                    result.set(scope, none_key.into(), null_v8);
                },
            }
            rv.set(result.into());
        }

        if register_dynamic {
            let record_to_type_fn = v8::Function::builder(record_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(RECORD_TO_TYPE_FN_SCOPE_ID))
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
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(TUPLE_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    tuple_to_type_fn.into(),
                );
            let cve_get_value_fn = v8::Function::builder(cve_get_value_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(CVE_GET_VALUE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    cve_get_value_fn.into(),
                );
            let cve_extract_fn = v8::Function::builder(cve_extract_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(CVE_EXTRACT_FN_SCOPE_ID)).unwrap().cast(),
                    cve_extract_fn.into(),
                );
            let cve_get_code_enum_fn =
                v8::Function::builder(cve_get_code_enum_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(CVE_GET_CODE_ENUM_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    cve_get_code_enum_fn.into(),
                );
            let value_get_type_fn = v8::Function::builder(value_get_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(VALUE_GET_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    value_get_type_fn.into(),
                );
            let aspect_lookup_fn = v8::Function::builder(aspect_lookup_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(ASPECT_LOOKUP_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    aspect_lookup_fn.into(),
                );

            fn cast_value_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let value: Rc<dyn Value> = rc_value_from_v8(&*args.get(0));
                let type_: Rc<dyn Type> = rc_type_from_v8(&*args.get(1));
                let type_const = crate::invariant::must_downcast_ref::<TypeSourceObjectConst>(type_.as_ref());
                let vud = crate::invariant::must_downcast_ref::<ValuePull>(value.as_ref());
                let _ = type_const;
                let instance = v8::Local::new(scope, &vud.bundle);
                rv.set(instance.into());
            }

            let cast_value_fn = v8::Function::builder(cast_value_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(CAST_VALUE_FN_SCOPE_ID)).unwrap().cast(),
                    cast_value_fn.into(),
                );

            fn object_ref_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let dt_ref: Rc<SourceObjectRef> = from_dynamic_ref::<Rc<SourceObjectRef>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(TypeSourceObjectConst { data: dt_ref.data.clone() });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let object_ref_to_type_fn =
                v8::Function::builder(object_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    object_ref_to_type_fn.into(),
                );

            fn union_ref_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let tu_ref: Rc<SourceUnionRef> = from_dynamic_ref::<Rc<SourceUnionRef>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(TypeSourceUnionConst { data: tu_ref.data.clone() });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let union_ref_to_type_fn =
                v8::Function::builder(union_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    union_ref_to_type_fn.into(),
                );

            fn interface_ref_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let iface_ref: Rc<SourceInterfaceRef> =
                    from_dynamic_ref::<Rc<SourceInterfaceRef>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let interface_ref_to_type_fn =
                v8::Function::builder(interface_ref_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(INTERFACE_REF_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    interface_ref_to_type_fn.into(),
                );

            fn function_const_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let func_ref: Rc<TypeSourceFunctionConst> =
                    from_dynamic_ref::<Rc<TypeSourceFunctionConst>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let function_const_to_type_fn =
                v8::Function::builder(function_const_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    function_const_to_type_fn.into(),
                );

            fn object_new_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let ctor_ref: Rc<TypeSourceObjectNewConst> =
                    from_dynamic_ref::<Rc<TypeSourceObjectNewConst>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> = Rc::new(TypeSourceObjectNewConst { data: ctor_ref.data.clone() });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let object_new_to_type_fn =
                v8::Function::builder(object_new_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &binding_id_dynamic_key(DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    object_new_to_type_fn.into(),
                );

            fn union_new_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let ctor_ref: Rc<SourceUnionNewRef> =
                    from_dynamic_ref::<Rc<SourceUnionNewRef>>(&*args.get(0)).clone();
                let type_: Rc<dyn Type> =
                    Rc::new(crate::eval::value::type_source_union_new::r#const::TypeSourceUnionNewConst {
                        data: ctor_ref.data.clone(),
                        variant_index: ctor_ref.variant_index,
                    });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let union_new_to_type_fn =
                v8::Function::builder(union_new_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(
                        dynamic_scope,
                        &binding_id_dynamic_key(TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID),
                    )
                        .unwrap()
                        .cast(),
                    union_new_to_type_fn.into(),
                );

            fn data_instance_to_type_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let template_type: Rc<dyn Type> = from_dynamic_ref::<Rc<dyn Type>>(&*args.get(0)).clone();
                let template = crate::invariant::must_downcast_ref::<TypeSourceObjectConst>(template_type.as_ref());
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(1),
                    ).expect("__data_instance_to_type: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let type_: Rc<dyn Type> = Rc::new(TypePull {
                    data: template.data.clone(),
                    bundle: std::cell::RefCell::new(Some(instance_global)),
                    artifact_id: std::cell::RefCell::new(None),
                });
                let ext = rc_type_to_dynamic_ext(scope, type_);
                rv.set(ext.into());
            }

            let data_instance_to_type_fn =
                v8::Function::builder(data_instance_to_type_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_type_fn.into(),
                );

            fn data_instance_to_target_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(1),
                    ).expect("__data_instance_to_target: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let target: Rc<dyn Target> = Rc::new(PullTarget { instance: instance_global });
                let ext = rc_target_to_dynamic_ext(scope, target);
                rv.set(ext.into());
            }

            let data_instance_to_target_fn =
                v8::Function::builder(data_instance_to_target_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_target_fn.into(),
                );

            fn data_instance_to_binding_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(1),
                    ).expect("__data_instance_to_binding: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let entry: Rc<dyn Binding> = Rc::new(PullBinding { instance: instance_global });
                let ext = rc_binding_to_dynamic_ext(scope, entry);
                rv.set(ext.into());
            }

            let data_instance_to_binding_fn =
                v8::Function::builder(data_instance_to_binding_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_binding_fn.into(),
                );

            fn data_instance_to_value_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(1),
                    ).expect("__data_instance_to_value: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let value: Rc<dyn Value> = Rc::new(ValuePull { bundle: instance_global });
                let ext = rc_value_to_dynamic_ext(scope, value);
                rv.set(ext.into());
            }

            let data_instance_to_value_fn =
                v8::Function::builder(data_instance_to_value_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_value_fn.into(),
                );

            fn data_instance_to_aspect_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(0),
                    ).expect("__data_instance_to_aspect: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let aspect: Rc<dyn Aspect> = std::rc::Rc::new(AspectPull { bundle: instance_global });
                let ext = to_dynamic_ext(scope, aspect);
                rv.set(ext.into());
            }

            let data_instance_to_aspect_fn =
                v8::Function::builder(data_instance_to_aspect_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_aspect_fn.into(),
                );

            fn data_instance_to_code_callback<
                's,
            >(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let instance =
                    v8::Local::<v8::Object>::try_from(
                        args.get(1),
                    ).expect("__data_instance_to_code: expected dynamic object for instance");
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, instance);
                let code: Rc<dyn CodeDyn> = Rc::new(CodePull { instance: instance_global });
                let ext = to_dynamic_ext(scope, code);
                rv.set(ext.into());
            }

            let data_instance_to_code_fn =
                v8::Function::builder(data_instance_to_code_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(DATA_INSTANCE_TO_CODE_FN_SCOPE_ID))
                        .unwrap()
                        .cast(),
                    data_instance_to_code_fn.into(),
                );

            fn unique_eq_callback<
                's,
            >(
                _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'s>,
                mut rv: v8::ReturnValue<'s>,
            ) {
                let a = *from_dynamic_ref::<Unique>(&*args.get(0));
                let b = *from_dynamic_ref::<Unique>(&*args.get(1));
                rv.set_bool(a == b);
            }

            let unique_eq_fn = v8::Function::builder(unique_eq_callback).build(dynamic_scope).unwrap();
            dynamic_scope
                .get_current_context()
                .global(dynamic_scope)
                .set(
                    dynamic_scope,
                    v8::String::new(dynamic_scope, &binding_id_dynamic_key(UNIQUE_EQ_FN_SCOPE_ID)).unwrap().cast(),
                    unique_eq_fn.into(),
                );
            cix.builtins_registered.set(true);
        }
    }
    let mut all_externals: Vec<(String, External)> =
        ex.externals.borrow().iter().map(|(id, v)| (binding_id_dynamic_key(*id), v.clone())).collect();
    for method in &def_data_methods {
        let func_name = binding_id_dynamic_key(method.binding_id);
        for (id, value) in &method.captured_externals {
            let local_key = binding_id_dynamic_key(*id);
            all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
        }
    }
    for (scope_id, _, _, captured) in &def_funcs {
        let func_name = binding_id_dynamic_key(*scope_id);
        for (id, value) in captured {
            let local_key = binding_id_dynamic_key(*id);
            all_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
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
                    text = e.to_rust_string_lossy(&dynamic_scope);
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

pub(crate) struct StageLiftCell {
    pub current_exception_type: Rc<dyn Type>,
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

fn is_type_never(t: &dyn Type) -> bool {
    (t as &dyn Any).is::<crate::eval::value::type_never::r#const::TypeNever>()
}

fn types_equal_desc(a: &dyn Type, b: &dyn Type) -> bool {
    a.desc() == b.desc()
}

fn maybe_wrap_call_exception(
    xx: &ExprContext,
    loc: QExprId,
    callee_exc: &dyn Type,
    v: CodeValue,
) -> EResult<CodeValue> {
    if is_type_never(callee_exc) {
        return EResult::ok(v);
    }
    if types_equal_desc(callee_exc, xx.current_exception_type.as_ref()) {
        return EResult::ok(v);
    }
    if !callee_exc.can_cast_to(xx, loc, xx.current_exception_type.as_ref()) {
        return EResult::ok(v);
    }
    let probe_binding_id = BindingId::Local(xx.vx.next_local_id.next());
    let probe_meta_type = match callee_exc.as_value().type_(xx, loc).into_res() {
        Ok(t) => t,
        Err(errs) => return EResult { errors: Some(errs), v: None },
    };
    let probe_value: Rc<dyn Value> = Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
        source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
        type_: probe_meta_type,
    });
    let probe_cv = CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: probe_binding_id }),
            })),
        },
        value: Some(probe_value),
    };
    let probe = match callee_exc
        .cast_to(xx, loc, probe_cv, xx.current_exception_type.as_ref())
        .into_res() {
        Ok(p) => p,
        Err(errs) => return EResult { errors: Some(errs), v: None },
    };
    if matches!(probe.code.code, Code_::None) {
        return EResult::ok(v);
    }
    let body_dyn = match &v.code.code {
        Code_::Dyn(c) => match (c.as_ref() as &dyn Any).downcast_ref::<CodeDynamic>() {
            Some(d) => d.clone(),
            None => return EResult::err(
                loc,
                "Call result code must be CodeDynamic to wrap with exception cast".to_string(),
            ),
        },
        Code_::None => return EResult::ok(v),
    };
    let catch_binding = BindingId::Local(xx.vx.next_local_id.next());
    let bound_value_type = match callee_exc.clone_rc() {
        Some(t) => t,
        None => return EResult::err(
            loc,
            format!("Cannot clone callee exception type {}", callee_exc.desc()),
        ),
    };
    let bound_value: Rc<dyn Value> = Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
        source: crate::eval::value::value_dynamic::ValueDynamicSource::Binding(
            crate::eval::value::value_dynamic::ValueDynamicDataScope { key: catch_binding },
        ),
        type_: bound_value_type,
    });
    let bound_cv = CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: catch_binding }),
            })),
        },
        value: Some(bound_value),
    };
    let cast_res = match callee_exc.cast_to(xx, loc, bound_cv, xx.current_exception_type.as_ref()).into_res() {
        Ok(c) => c,
        Err(errs) => return EResult { errors: Some(errs), v: None },
    };
    let to_dynamic = match crate::r#gen::aspect_consume_dynamic_get(xx, loc, xx.current_exception_type.as_ref()).into_res() {
        Ok(c) => c,
        Err(errs) => return EResult { errors: Some(errs), v: None },
    };
    let cast_code = match to_dynamic.to_dynamic(xx, loc, cast_res).into_res() {
        Ok(c) => c,
        Err(errs) => return EResult { errors: Some(errs), v: None },
    };
    let throw_code = CodeDynamic {
        loc,
        type_: CodeDynamicType::Throw(crate::code_dynamic::CodeDynamicThrow { value: Box::new(cast_code) }),
    };
    let try_code = CodeDynamic {
        loc,
        type_: CodeDynamicType::Try(crate::code_dynamic::CodeDynamicTry {
            body: Box::new(body_dyn),
            catch: Some(crate::code_dynamic::CodeDynamicCatch {
                binding: catch_binding,
                body: Box::new(throw_code),
            }),
            finally: None,
        }),
    };
    EResult::ok(CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(try_code)),
        },
        value: v.value,
    })
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
