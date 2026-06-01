#[macro_use]
pub mod util;
pub mod null;
pub mod string;
pub mod number;
pub mod bool;
pub mod mortar_js_value;
pub mod mortar_scope_entry;
pub mod mortar_null;
pub mod mortar_string;
pub mod mortar_number;
pub mod mortar_bool;
pub mod mortar_list;
pub mod mortar_map;
pub mod mortar_function;
pub mod expr;
pub mod tuple;
pub mod record;
#[macro_use]
pub mod jswrap;
pub mod type_ref;
pub mod v8util;
pub mod definition_set;

use {
    crate::{
        builtin::{
            aspect_bind_get_maybe,
            aspect_bind_js_get_maybe,
            aspect_bool_get_maybe,
            aspect_consume_js_get,
            aspect_consume_js_get_maybe,
            aspect_field_get,
            aspect_function_get,
            aspect_lift_js_get,
            aspect_iterable_get,
            aspect_operator_add_get,
            aspect_operator_lt_get,
            aspect_serial_get,
            aspect_set_get,
            aspect_string_get_maybe,
            aspect_tagged_union_get,
        },
        jscode::{
            BINDING_PREFIX,
            BUILTIN_PREFIX,
            DEF_PREFIX,
            EXPR_PREFIX,
            TAG_VAR,
            JsCode,
            JsCodeBind,
            JsCodeBindingUse,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeIf,
            JsCodeMatch,
            JsCodeMatchBranch,
            JsCodeNewObject,
            JsCodeOperatorAdd,
            JsCodeOperatorLt,
            JsCodeType,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
            Counter,
            DefSetId,
            CompileRes,
            CompileResResult,
            ConstKey,
            EResult,
            EvalContext,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            GlobalContext,
            ImportStateId,
            QExprId,
            Scope,
            ScopeEntry,
            ScopeEntryId,
            SourceId,
            Type,
            Unique,
            Value,
        },
        syntax::{
            ExprId,
            SerialExpr,
            SerialExpr_,
            SerialLiteral,
            SerialModule,
        },
    },
    definition_set::{
        DefineResult,
        DefinitionSet,
    },
    jswrap::JsWrap,
    expr::{
        Expr,
        ExprBind,
        ExprBlock,
        ExprCall,
        ExprField,
        ExprForeach,
        ExprLower,
        ExprMatch,
        ExprMatchBranch,
        ExprScopeUse,
        ExprSet,
        ExprStage,
        ExprTuple,
        Expr_,
        Module,
    },
    bool::ValueBool,
    flowcontrol::shed,
    mortar_function::MortarTypeFunction,
    mortar_null::TypeMortarNull,
    mortar_scope_entry::{
        MortarScopeEntryAsScope,
        MortarScopeEntryBinding,
    },
    mortar_number::TypeMortarNumber,
    mortar_string::TypeMortarString,
    null::ValueNull,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        env::current_dir,
        mem,
        path::PathBuf,
        rc::Rc,
        sync::{
            Arc,
            OnceLock,
        },
    },
    string::ValueString,
    record::{
        RecordField,
        Record,
    },
    tuple::{
        Tuple,
        TupleElement,
        StaticScopeEntry,
        ValueScopeEntry,
    },
    dumpster::unsync::Gc,
    v8::tc_scope,
};

// --- Rc`<T>` ↔ v8::External helpers ---
/// Weak handle storage, set as an isolate slot. During dispose_annex(), slots are
/// cleared first (Weak handles dropped — but isolate ptr is already null so
/// finalizers can't be cancelled), then finalizer_map is drained, running our
/// guaranteed finalizer callbacks.
pub(crate) struct V8RcStore {
    weaks: RefCell<Vec<v8::Weak<v8::External>>>,
}

pub(crate) struct V8LogStore {
    pub(crate) logs: RefCell<Vec<crate::langhelp::LogEntry>>,
}

pub(crate) struct V8UniqueCounter {
    pub(crate) compile_id: CompileId,
    pub(crate) counter: Counter,
}

pub(crate) struct V8DefSetCounter {
    pub(crate) import_id: ImportStateId,
    pub(crate) counter: Counter,
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
    from_v8_any(value).downcast_ref::<T>().expect("from_v8_ref: type mismatch")
}

/// Store an `Rc<dyn Type>` as a `v8::External`.
/// Only operates on `dyn Type` — for concrete `Rc<T>`, use `to_v8` directly.
pub(crate) fn rc_type_to_v8<'s>(
    scope: &mut v8::PinScope<'s, '_, ()>,
    value: Rc<dyn Type>,
) -> v8::Local<'s, v8::External> {
    to_v8(scope, value)
}

/// Extract a clone of `Rc<dyn Type>` from a `v8::External`.
/// Only operates on `dyn Type` — for concrete `Rc<T>`, use `from_v8_ref` directly.
pub(crate) fn rc_type_from_v8(value: &v8::Value) -> Rc<dyn Type> {
    from_v8_ref::<Rc<dyn Type>>(value).clone()
}

/// EvalResBuilder
pub(crate) struct Erb {
    loc: QExprId,
    errors: Vec<ExprErr>,
}

impl Erb {
    pub(crate) fn new(loc: QExprId) -> Self {
        return Self {
            loc: loc,
            errors: Default::default(),
        };
    }

    pub(crate) fn maybe_er<T>(&mut self, r: EResult<T>) -> Option<T> {
        let Some(cv) = r.v else {
            return None;
        };
        if let Some(errs) = r.errors {
            self.errors.extend(errs);
        }
        return Some(cv);
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

    pub(crate) fn maybe_res<T>(&mut self, r: Result<T, String>) -> Option<T> {
        match r {
            Ok(r) => {
                return Some(r);
            },
            Err(_) => {
                return None;
            },
        }
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
}

fn path_to_fs_path(path: &Vec<String>) -> PathBuf {
    let mut at = PathBuf::new();
    for seg in path {
        at = at.join(seg);
    }
    return at;
}

fn cerr<T>(e: String) -> Result<T, ExprSubErr> {
    return Err(ExprSubErr::Opaque(e));
}

trait Effect { }

pub(crate) trait Target {
    type Effect;

    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)>;
    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code>;
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
    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes;
}
pub(crate) struct MortarTarget;

impl Target for MortarTarget {
    type Effect = JsCode;

    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        let none_code = Code {
            loc: loc,
            code: Code_::None,
        };
        if let Some(bind) = aspect_bind_get_maybe(xx, vtype.as_ref()) {
            return er_seq!(loc, erb, (Code, Box < dyn ScopeEntry >), {
                let scope_entry = erb.must_er(bind.bind(xx, loc, cv))?;
                return Ok((none_code, scope_entry));
            });
        }
        let Some(bind_js) = aspect_bind_js_get_maybe(xx, vtype.as_ref()) else {
            return EResult::err(
                loc,
                format!("Type ({}) is missing aspect AspectBind and AspectBindJs", vtype.desc()),
            );
        };
        return er_seq!(loc, erb, (Code, Box < dyn ScopeEntry >), {
            let bind_result = bind_js.bind_js(xx, loc, cv);
            if bind_result.v.is_none() {
                return Ok((none_code, Box::new(StaticScopeEntry { type_: vtype }) as Box<dyn ScopeEntry>));
            }
            let (js_code, mortar_entry) = erb.must_er(bind_result)?;
            match js_code {
                Some(vcode) => {
                    let js_key = ScopeEntryId::Local(xx.vx.next_local_id.next());
                    let code = Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: loc,
                            type_: JsCodeType::Block(JsCodeBlock { entries: vec![JsCodeBlockEntry::Bind(JsCodeBind {
                                base: js_key,
                                value: Box::new(vcode),
                            })], }),
                        })),
                    };
                    return Ok((code, Box::new(MortarScopeEntryAsScope {
                        js_key: Some(js_key),
                        type_: vtype,
                        inner: mortar_entry,
                    })));
                },
                None => {
                    return Ok((none_code, Box::new(MortarScopeEntryAsScope {
                        js_key: None,
                        type_: vtype,
                        inner: mortar_entry,
                    }) as Box<dyn ScopeEntry>));
                },
            }
        });
    }

    fn block(&self, loc: QExprId, entries: Vec<Code>) -> EResult<Code> {
        let mut js_entries = vec![];
        let mut errors = vec![];
        for entry in &entries {
            match entry.cast::<JsCode>().into_res() {
                Ok(Some(js)) => {
                    match &js.type_ {
                        JsCodeType::Block(block) if
                            !block.entries.is_empty() &&
                                block.entries.iter().all(|e| matches!(e, JsCodeBlockEntry::Bind(_))) => {
                            js_entries.extend(block.entries.clone());
                        },
                        _ => js_entries.push(JsCodeBlockEntry::Code(js.clone())),
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
        if js_entries.is_empty() {
            return EResult::ok(Code {
                loc: loc,
                code: Code_::None,
            });
        }
        return EResult::ok(Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(JsCode {
                loc: loc,
                type_: JsCodeType::Block(JsCodeBlock { entries: js_entries }),
            })),
        });
    }

    fn if_(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        conditions: Vec<(CodeValue, &Expr)>,
        else_: Option<&Expr>,
    ) -> EvalRes {
        fn cv_to_js(erb: &mut Erb, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> Result<JsCode, ()> {
            let vtype = cv.value.type_(xx, loc);
            if let Some(consume) = aspect_consume_js_get_maybe(xx, vtype.as_ref()) {
                return erb.must_er(consume.to_js(xx, loc, cv));
            }
            match erb.must_er(cv.code.cast::<JsCode>())? {
                Some(c) => Ok(c.clone()),
                None => Ok(JsCode {
                    loc,
                    type_: JsCodeType::Literal(serde_json::Value::Null),
                }),
            }
        }

        return er_seq_cv!(loc, erb, {
            let mut js_conditions = Vec::new();
            for (cond, body) in conditions {
                let cond_js = cv_to_js(&mut erb, xx, loc, cond)?;
                let body_cv = erb.must_er(eval_half(xx, body, self))?;
                let body_js = cv_to_js(&mut erb, xx, loc, body_cv)?;
                js_conditions.push((cond_js, body_js));
            }
            let js_else = if let Some(else_expr) = else_ {
                let else_cv = erb.must_er(eval_half(xx, else_expr, self))?;
                let else_js = cv_to_js(&mut erb, xx, loc, else_cv)?;
                Some(Box::new(else_js))
            } else {
                None
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::If(JsCodeIf {
                            conditions: js_conditions,
                            else_: js_else,
                        }),
                    })),
                },
                value: Box::new(ValueNull),
            });
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
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, base_type.as_ref()))?;
            let base_js = erb.must_er(to_js.to_js(xx, loc, base))?;
            let element_scope_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            let mut js_cases = Vec::new();
            for (tag, variant_type, body_expr) in &cases {
                let child_scope = Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: Some(xx.scope.clone()),
                });
                child_scope.values.borrow_mut().insert(element_key.clone(), Box::new(MortarScopeEntryBinding {
                    js_key: element_scope_id,
                    type_: variant_type.clone(),
                }),);
                let child_xx = ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: child_scope,
                };
                let body_cv = erb.must_er(eval_half(&child_xx, body_expr, self))?;
                let body_type = body_cv.value.type_(&child_xx, loc);
                let body_js = if let Some(consume) = aspect_consume_js_get_maybe(&child_xx, body_type.as_ref()) {
                    erb.must_er(consume.to_js(&child_xx, loc, body_cv))?
                } else {
                    match erb.must_er(body_cv.code.cast::<JsCode>())? {
                        Some(c) => c.clone(),
                        None => JsCode {
                            loc,
                            type_: JsCodeType::Literal(serde_json::Value::Null)
                        },
                    }
                };
                js_cases.push(JsCodeMatchBranch {
                    js_tag: tag.clone(),
                    body: body_js,
                });
            }
            let js_fallback = if let Some(fallback_expr) = fallback {
                let fb_cv = erb.must_er(eval_half(xx, fallback_expr, self))?;
                let fb_type = fb_cv.value.type_(xx, loc);
                let fb_js = if let Some(consume) = aspect_consume_js_get_maybe(xx, fb_type.as_ref()) {
                    erb.must_er(consume.to_js(xx, loc, fb_cv))?
                } else {
                    match erb.must_er(fb_cv.code.cast::<JsCode>())? {
                        Some(c) => c.clone(),
                        None => JsCode {
                            loc,
                            type_: JsCodeType::Literal(serde_json::Value::Null)
                        },
                    }
                };
                Some(Box::new(fb_js))
            } else {
                None
            };
            let match_js = JsCode {
                loc,
                type_: JsCodeType::Match(JsCodeMatch {
                    base: Box::new(base_js),
                    element_key: element_scope_id,
                    cases: js_cases,
                    fallback: js_fallback,
                }),
            };
            let null_stack = crate::builtin::aspect_js_stack_value_get(xx, loc, &TypeMortarNull as &dyn Type);
            let null_stack = erb.must_er(null_stack)?;
            return Ok(null_stack.to_value(loc, match_js));
        });
    }

    fn stage(&self, xx: &ExprContext, loc: QExprId, tree: Expr, lower_values: Vec<CodeValue>) -> EvalRes {
        // Store tree as external
        let tree_cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Box::new(tree))) });
        let tree_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((tree_id, Box::new(tree_cell)));
        let tree_js = JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id: tree_id }),
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
                        xx.vx.externals.borrow_mut().push((id, Box::new(cell)));
                        stage_args.push(JsCode {
                            loc,
                            type_: JsCodeType::Literal(serde_json::Value::Null),
                        });
                        stage_args.push(JsCode {
                            loc,
                            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
                        });
                    },
                    Code_::Dyn(_) => {
                        let lift = erb.must_er(aspect_lift_js_get(xx, loc, vtype.as_ref()))?;
                        let lift_cell = Rc::new(StageLiftCell { lift });
                        let lift_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((lift_id, Box::new(lift_cell)));
                        let inner_js = if let Some(consume) = aspect_consume_js_get_maybe(xx, vtype.as_ref()) {
                            erb.must_er(consume.to_js(xx, loc, cv))?
                        } else {
                            erb.must_er(cv.code.cast::<JsCode>())?.unwrap().clone()
                        };
                        stage_args.push(JsCode {
                            loc,
                            type_: JsCodeType::BindingUse(JsCodeBindingUse { id: lift_id }),
                        });
                        stage_args.push(inner_js);
                    },
                }
            }
            let stage_js = JsCode {
                loc,
                type_: JsCodeType::Call(crate::jscode::JsCodeCall {
                    func: Box::new(JsCode {
                        loc,
                        type_: JsCodeType::BindingUse(JsCodeBindingUse { id: STAGE_FN_SCOPE_ID, }),
                    }),
                    args: stage_args,
                }),
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(stage_js)),
                },
                value: Box::new(expr::StagedExprPlaceholder),
            });
        });
    }
}

/// Holds an AspectLiftJs, stored as a v8::External so __stage can lift raw values.
pub(crate) struct StageLiftCell {
    pub lift: Box<dyn crate::builtin::AspectLiftJs>,
}

fn extract_const_key(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> Result<ConstKey, String> {
    let vtype = v.type_(xx, loc);
    if let Some(a) = aspect_string_get_maybe(xx, vtype.as_ref()) {
        return Ok(ConstKey::String(a.get(v)));
    }
    return Err(format!("TODO value type [X] must expose const key aspect [LIST] but doesn't"));
}

pub(crate) fn eval_half<T: Target>(xx: &ExprContext, expr: &Expr, target: &T) -> EvalRes {
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
            let key = erb.must_er(eval_half(xx, p.key.as_ref(), target))?;
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
                func = eval_half(xx, p.func.as_ref(), target),
                arg = eval_half(xx, p.arg.as_ref(), target),
            });
            let func_type = func.value.type_(xx, expr.loc);
            let aspect_func = erb.must_er(aspect_function_get(xx, expr.loc, func_type.as_ref()))?;
            let v = erb.must_er(aspect_func.call(xx, expr.loc, func, arg))?;
            return Ok(v);
        }),
        Expr_::Set(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                value = eval_half(xx, e.value.as_ref(), target),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let base_set = erb.must_er(aspect_set_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(base_set.set(xx, expr.loc, base, value))?;
            return Ok(v);
        }),
        Expr_::Field(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                field = eval_half(xx, e.key.as_ref(), target),
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
            };
            return er_seq_cv!(expr.loc, erb, {
                let mut entries = vec![];
                let mut last_val = None;
                for sub in &e.exprs {
                    if let Expr_::Bind(bind) = &sub.r#type {
                        er_all!(erb, {
                            base = eval_half(&child_xx, bind.base.as_ref(), target),
                            value = eval_half(&child_xx, bind.value.as_ref(), target),
                        });
                        let base_constkey = erb.must_res(extract_const_key(&child_xx, sub.loc, base.value.as_ref()))?;
                        entries.push(base.code);
                        let binding = erb.must_er(target.bind(&child_xx, sub.loc, value))?;
                        entries.push(binding.0);
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Box::new(ValueNull) as Box<dyn Value>);
                    } else {
                        let r = erb.must_er(eval_half(&child_xx, sub, target))?;
                        entries.push(r.code);
                        last_val = Some(r.value);
                    }
                }
                return Ok(CodeValue {
                    code: erb.must_er(target.block(expr.loc, entries))?,
                    value: last_val.unwrap_or_else(|| Box::new(ValueNull))
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
                    Expr_::Literal(_) | Expr_::ScopeUse(_) => { },
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
                    value: Box::new(e.expr.as_ref().clone()),
                });
            }

            // half_eval each matched lower's inner expression
            return er_seq_cv!(expr.loc, erb, {
                let mut lower_values = Vec::new();
                for lower_inner in &matched_lower_exprs {
                    let cv = erb.must_er(eval_half(xx, lower_inner, target))?;
                    lower_values.push(cv);
                }
                let v = erb.must_er(target.stage(xx, expr.loc, e.expr.as_ref().clone(), lower_values))?;
                return Ok(v);
            });
        },
        Expr_::Lower(e) => {
            // Transparently evaluate inner expression
            return eval_half(xx, &e.expr, target);
        },
        Expr_::Tuple(t) => {
            let elements: Vec<TupleElement> =
                t.elements.iter().map(|e| TupleElement::EvalRes(eval_half(xx, e, target))).collect();
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Box::new(Tuple {
                    source: None,
                    elements,
                }),
            });
        },
        Expr_::Record(r) => {
            let mut entries = std::collections::BTreeMap::new();
            let mut key_codes = vec![];
            let mut errors = vec![];
            for (k_expr, v_expr) in &r.entries {
                let k_res = eval_half(xx, k_expr, target);
                if let Some(errs) = k_res.errors {
                    errors.extend(errs);
                }
                let Some(k_cv) = k_res.v else {
                    continue;
                };
                if let Some(js) = k_cv.code.cast::<JsCode>().v.flatten() {
                    key_codes.push(js.clone());
                }
                match extract_const_key(xx, expr.loc, k_cv.value.as_ref()) {
                    Ok(key) => {
                        entries.insert(key, eval_half(xx, v_expr, target));
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

            // Check if any entry value has JS code (Code_::Dyn). If so, the record must also
            // carry Code_::Dyn so stage processing knows to lift it.
            let has_entry_code = entries.values().any(|e| {
                e.v.as_ref().map_or(false, |cv| matches!(cv.code.code, Code_::Dyn(_)))
            });
            let code = if key_codes.is_empty() && !has_entry_code {
                Code {
                    loc: expr.loc,
                    code: Code_::None,
                }
            } else {
                let block_entries = key_codes.into_iter().map(JsCodeBlockEntry::Code).collect();
                Code {
                    loc: expr.loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc: expr.loc,
                        type_: JsCodeType::Block(JsCodeBlock { entries: block_entries }),
                    })),
                }
            };
            let fields = entries.into_iter().map(|(k, v)| (k, RecordField::EvalRes(v))).collect();
            return EvalRes::ok(CodeValue {
                code,
                value: Box::new(Record {
                    source: None,
                    fields,
                }),
            });
        },
        Expr_::OperatorLt(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                reference = eval_half(xx, e.reference.as_ref(), target),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_lt = erb.must_er(aspect_operator_lt_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_lt.lt(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::OperatorAdd(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                reference = eval_half(xx, e.reference.as_ref(), target),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_add = erb.must_er(aspect_operator_add_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_add.add(xx, expr.loc, base, reference))?;
            return Ok(v);
        }),
        Expr_::Foreach(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                element_key_cv = eval_half(xx, e.element_key.as_ref(), target),
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
            child_scope.values.borrow_mut().insert(element_key, Box::new(MortarScopeEntryBinding {
                js_key: element_scope_id,
                type_: iterable.element_type(),
            }),);
            let child_xx = ExprContext {
                vx: xx.vx.clone(),
                import_state: xx.import_state.clone(),
                scope: child_scope,
            };
            let body = erb.must_er(eval_half(&child_xx, e.body.as_ref(), target))?;
            let v = erb.must_er(iterable.foreach(xx, expr.loc, base, element_scope_id, body))?;
            return Ok(v);
        }),
        Expr_::Match(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                element_key_cv = eval_half(xx, e.element_key.as_ref(), target),
            });
            let element_key = erb.must_res(extract_const_key(xx, expr.loc, element_key_cv.value.as_ref()))?;
            let base_type = base.value.type_(xx, expr.loc);
            let tagged_union = erb.must_er(aspect_tagged_union_get(xx, expr.loc, base_type.as_ref()))?;
            let variants = tagged_union.variants();

            // Validate case tags against variant tags
            let mut case_tags: Vec<ConstKey> = Vec::new();
            for case in &e.cases {
                let cond_cv = erb.must_er(eval_half(xx, &case.condition, target))?;
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
                let (_, js_tag, variant_type) = variants.iter().find(|(t, _, _)| t == tag).unwrap();
                cases.push((js_tag.clone(), variant_type.clone(), &case.body));
            }
            let v = erb.must_er(target.match_(xx, expr.loc, base, element_key, cases, e.fallback.as_deref()))?;
            return Ok(v);
        }),
        Expr_::If(e) => return er_seq_cv!(expr.loc, erb, {
            // Try constant folding first: check if all conditions resolve to constant bools
            let mut conditions_evaluated = Vec::new();
            let mut all_const = true;
            for branch in &e.conditions {
                let cond = erb.must_er(eval_half(xx, &branch.condition, target))?;
                let cond_type = cond.value.type_(xx, expr.loc);
                if let Some(aspect_bool) = aspect_bool_get_maybe(xx, cond_type.as_ref()) {
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
                        let body_cv = erb.must_er(eval_half(xx, body, target))?;
                        return Ok(body_cv);
                    }
                }

                // No true branch; use else or null
                if let Some(else_expr) = &e.else_ {
                    let else_cv = erb.must_er(eval_half(xx, else_expr, target))?;
                    return Ok(else_cv);
                }
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::None
                    },
                    value: Box::new(ValueNull),
                });
            } else {
                // Delegate code generation to target
                let conditions: Vec<(CodeValue, &Expr)> =
                    conditions_evaluated.into_iter().map(|(_, cond, body)| (cond, body as &Expr)).collect();
                let v = erb.must_er(target.if_(xx, expr.loc, conditions, e.else_.as_deref(),))?;
                return Ok(v);
            }
        }),
    }
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let source_type = source.value.type_(xx, loc);
        let dest;
        if source_type.can_cast_to(xx, loc, dest_type) {
            dest = erb.must_er(source_type.cast_to(xx, loc, source, dest_type))?;
        } else if dest_type.can_cast_from(xx, loc, &source) {
            dest = erb.must_er(dest_type.cast_from(xx, loc, source))?;
        } else {
            return erb.err_new(
                format!("Incompatible types: source {} cannot convert to dest {}", source_type.desc(), dest_type.desc())
            );
        }
        return Ok(dest);
    });
}

pub(crate) fn scope_entry_id_js_key(e: ScopeEntryId) -> String {
    match e {
        ScopeEntryId::Local(id) => format!("{}{}", BINDING_PREFIX, id),
        ScopeEntryId::Builtin(id) => format!("{}{}", BUILTIN_PREFIX, id),
        ScopeEntryId::Def(import_id, def_set_id, entry_id) => {
            format!("{}{}_{}_{}", DEF_PREFIX, import_id.0, def_set_id.0, entry_id)
        },
    }
}

struct EvalResult {
    value: Box<dyn Value>,
    logs: Vec<crate::langhelp::LogEntry>,
}

/// Well-known scope entry ID for the __stage function (not a real scope entry).
const STAGE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for __record_to_type mortar function.
pub(crate) const RECORD_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

/// Well-known scope entry ID for __tuple_to_type mortar function.
pub(crate) const TUPLE_TO_TYPE_FN_SCOPE_ID: ScopeEntryId = ScopeEntryId::Builtin(line!() as usize);

fn eval(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<EvalResult, Vec<ExprErr>> {
    let mut js_scope_isolate = v8::Isolate::new(Default::default());
    js_scope_isolate.set_slot(V8RcStore { weaks: RefCell::new(Vec::new()) });
    js_scope_isolate.set_slot(V8LogStore { logs: RefCell::new(Vec::new()) });
    js_scope_isolate.set_slot(V8UniqueCounter {
        compile_id: cix
            .compile_path
            .last()
            .map(|(c, _)| c.clone())
            .unwrap_or(CompileId(ImportStateId(0), SourceId(0))),
        counter: Counter::default(),
    });
    js_scope_isolate.set_slot(V8DefSetCounter {
        import_id: import_state,
        counter: Counter::default(),
    });
    let js_scope_handle = std::pin::pin!(v8::HandleScope::new(&mut js_scope_isolate));
    let mut js_scope_handle2 = js_scope_handle.init();
    let js_scope_context = v8::Context::new(&mut js_scope_handle2, Default::default());
    let js_scope_context2 = &mut v8::ContextScope::new(&mut js_scope_handle2, js_scope_context);
    tc_scope!(let js_scope, js_scope_context2);
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_local_id: Default::default(),
        definition_sets: RefCell::new(Vec::new()),
        externals: RefCell::new(Vec::new()),
    });
    let mut s1_scope = HashMap::new();
    let mut builtin_scope_entries = Vec::new();
    for (key, eval_value, js_value) in [jswrap!(fn log(ex, js_scope, scope)(message: String) ->() {
        let timestamp_ms =
            std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
        scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(crate::langhelp::LogEntry {
            timestamp_ms,
            message
        });
    }), jswrap!(fn define(ex, js_scope, scope)() -> DefineResult {
        let dsc = scope.get_slot::<V8DefSetCounter>().unwrap();
        let def_set_id = DefSetId(dsc.counter.next());
        let import_id = dsc.import_id;
        DefineResult { ds: Rc::new(Gc::new(DefinitionSet::new(import_id, def_set_id))) }
    }), jswrap!(fn str_to_json(ex, js_scope, scope)(s: String) -> String {
        serde_json::to_string(&s).unwrap()
    }), jswrap!(fn constkey_to_str(ex, js_scope, scope)(key: crate:: langhelp:: ConstKey) -> String {
        key.to_str()
    })] {
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(
                js_scope,
                v8::String::new(js_scope, &scope_entry_id_js_key(eval_value.js_key)).unwrap().cast(),
                js_value,
            );
        builtin_scope_entries.push((key.to_string(), MortarScopeEntryBinding {
            js_key: eval_value.js_key,
            type_: eval_value.type_.clone(),
        }));
        s1_scope.insert(ConstKey::String(key.to_string()), Box::new(eval_value) as Box<dyn ScopeEntry>);
    }

    // Type references (static scope entries, no JS representation)
    s1_scope.insert(
        ConstKey::String("null".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarNull) }) as Box<dyn ScopeEntry>,
    );
    s1_scope.insert(
        ConstKey::String("string".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarString) }) as Box<dyn ScopeEntry>,
    );
    s1_scope.insert(
        ConstKey::String("number".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarNumber) }) as Box<dyn ScopeEntry>,
    );

    // list/map constructors: jswrap fns for v8 with constructor types for half-eval
    for (
        key,
        constructor_type,
        (_, jswrap_entry, js_value),
    ) in [
        (
            "list",
            Rc::new(mortar_list::TypeListConstructor) as Rc<dyn Type>,
            jswrap!(fn list(ex, js_scope, scope)(element_type: jswrap:: TypeRef) -> mortar_list:: TypeMortarList {
                mortar_list::TypeMortarList { element_type: element_type.0 }
            }),
        ),
        (
            "map",
            Rc::new(mortar_map::TypeMapConstructor) as Rc<dyn Type>,
            jswrap!(
                fn map(
                    ex,
                    js_scope,
                    scope
                )(key_type: jswrap:: TypeRef, value_type: jswrap:: TypeRef) -> mortar_map:: TypeMortarMap {
                    mortar_map::TypeMortarMap {
                        key_type: key_type.0,
                        value_type: value_type.0
                    }
                }
            ),
        ),
    ] {
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(
                js_scope,
                v8::String::new(js_scope, &scope_entry_id_js_key(jswrap_entry.js_key)).unwrap().cast(),
                js_value,
            );
        let entry = MortarScopeEntryBinding {
            js_key: jswrap_entry.js_key,
            type_: constructor_type,
        };
        builtin_scope_entries.push((key.to_string(), entry.clone()));
        s1_scope.insert(ConstKey::String(key.to_string()), Box::new(entry) as Box<dyn ScopeEntry>);
    }
    let root_loc = expr.loc;
    let xx = Rc::new(ExprContext {
        vx: ex.clone(),
        import_state: Rc::new(Cell::new(import_state)),
        scope: Rc::new(Scope {
            values: RefCell::new(s1_scope),
            parent: None,
        }),
    });
    let result = eval_half(&xx, expr, &MortarTarget);
    if let Some(errs) = result.errors {
        return Err(errs);
    }
    let cv = result.v.unwrap();
    let value_type = cv.value.type_(&xx, root_loc);
    let (stage1_code, fallback_value) = match aspect_consume_js_get_maybe(&xx, value_type.as_ref()) {
        Some(to_js) => {
            let js = to_js.to_js(&xx, root_loc, cv).into_res()?;
            (Some(js), None)
        },
        None => {
            let code = cv.code.cast::<JsCode>().into_res()?.cloned();
            (code, Some(cv.value))
        },
    };

    // Compile definition set implementations Collect function data first to release
    // the borrow on definition_sets, since eval_half may trigger function calls that
    // re-borrow it.
    struct DefFuncInfo {
        impl_expr_ast: Box<dyn Value>,
        arg_type: Rc<dyn Type>,
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
    // references. Definition set members are accessed via lower + to_js.
    fn build_builtin_scope(
        builtin_scope_entries: &[(String, MortarScopeEntryBinding)],
    ) -> HashMap<ConstKey, Box<dyn ScopeEntry>> {
        let mut scope_entries = HashMap::<ConstKey, Box<dyn ScopeEntry>>::new();
        for (name, entry) in builtin_scope_entries {
            scope_entries.insert(ConstKey::String(name.clone()), Box::new(MortarScopeEntryBinding {
                js_key: entry.js_key,
                type_: entry.type_.clone(),
            }));
        }
        scope_entries.insert(
            ConstKey::String("null".to_string()),
            Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarNull) }) as Box<dyn ScopeEntry>,
        );
        scope_entries.insert(
            ConstKey::String("string".to_string()),
            Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarString) }) as Box<dyn ScopeEntry>,
        );
        scope_entries.insert(
            ConstKey::String("number".to_string()),
            Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarNumber) }) as Box<dyn ScopeEntry>,
        );
        scope_entries
    }

    let mut def_funcs: Vec<(ScopeEntryId, Option<ScopeEntryId>, JsCode)> = vec![];
    for info in &def_func_infos {
        let impl_expr_ast =
            (info.impl_expr_ast.as_ref() as &dyn Any)
                .downcast_ref::<Expr>()
                .expect("Definition set impl must be an Expr");

        // Create a separate evaluation context for the function body
        let body_ex = Rc::new(EvalContext {
            ix: ex.ix.clone(),
            next_local_id: Counter::default(),
            definition_sets: RefCell::new(Vec::new()),
            externals: RefCell::new(Vec::new()),
        });
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let func_xx = {
            let mut body_scope = build_builtin_scope(&builtin_scope_entries);
            body_scope.insert(ConstKey::String("arg".to_string()), Box::new(MortarScopeEntryBinding {
                js_key: arg_id,
                type_: info.arg_type.clone(),
            }));
            Rc::new(ExprContext {
                vx: body_ex,
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope {
                    values: RefCell::new(body_scope),
                    parent: None,
                }),
            })
        };
        let impl_result = eval_half(&func_xx, impl_expr_ast, &MortarTarget);
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
                .cast::<JsCode>()
                .into_res()?
                .expect("Definition set function body must produce JS code")
                .clone();
        def_funcs.push((info.scope_entry_id, Some(arg_id), impl_code));
    }

    // Compile data type method implementations
    struct CompiledDataMethod {
        scope_entry_id: ScopeEntryId,
        self_scope_entry_id: ScopeEntryId,
        data_field: String,
        impl_code: JsCode,
    }

    let mut def_data_methods: Vec<CompiledDataMethod> = vec![];
    for ds in ex.definition_sets.borrow().iter() {
        for dt_data in ds.data_types.borrow().iter() {
            let underlying_type: Rc<dyn Type> =
                dt_data.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone();
            let data_field = dt_data.data_field.clone();
            for method in dt_data.methods.borrow().iter() {
                let impl_expr = method.impl_expr.borrow();
                let impl_expr_ast =
                    (impl_expr.as_ref().unwrap().as_ref() as &dyn Any)
                        .downcast_ref::<expr::Expr>()
                        .expect("Method impl must be an Expr");
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not assigned");
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
                    let mut body_scope = build_builtin_scope(&builtin_scope_entries);
                    body_scope.insert(ConstKey::String("self".to_string()), Box::new(MortarScopeEntryBinding {
                        js_key: self_id,
                        type_: underlying_type.clone(),
                    }));
                    Rc::new(ExprContext {
                        vx: body_ex,
                        import_state: xx.import_state.clone(),
                        scope: Rc::new(Scope {
                            values: RefCell::new(body_scope),
                            parent: None,
                        }),
                    })
                };
                let impl_result = eval_half(&method_xx, impl_expr_ast, &MortarTarget);
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
                    impl_cv.code.cast::<JsCode>().into_res()?.expect("Method body must produce JS code").clone();
                def_data_methods.push(CompiledDataMethod {
                    scope_entry_id: method_id,
                    self_scope_entry_id: self_id,
                    data_field: data_field.clone(),
                    impl_code,
                });
            }

            // Compile interface method implementations
            for impl_data in dt_data.interface_impls.borrow().iter() {
                for method_impl in &impl_data.method_impls {
                    let impl_expr_ast =
                        (method_impl.impl_expr.as_ref() as &dyn Any)
                            .downcast_ref::<expr::Expr>()
                            .expect("Interface method impl must be an Expr");
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not assigned");

                    // Resolve ret type from interface method
                    let ret_type: Rc<dyn Type> = {
                        let interfaces = ds.interfaces.borrow();
                        let iface = &interfaces[impl_data.interface_index];
                        let iface_methods = iface.methods.borrow();
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
                        let mut body_scope = build_builtin_scope(&builtin_scope_entries);
                        body_scope.insert(ConstKey::String("self".to_string()), Box::new(MortarScopeEntryBinding {
                            js_key: self_id,
                            type_: underlying_type.clone(),
                        }));
                        Rc::new(ExprContext {
                            vx: body_ex,
                            import_state: xx.import_state.clone(),
                            scope: Rc::new(Scope {
                                values: RefCell::new(body_scope),
                                parent: None,
                            }),
                        })
                    };
                    let impl_result = eval_half(&method_xx, impl_expr_ast, &MortarTarget);
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
                            .cast::<JsCode>()
                            .into_res()?
                            .expect("Interface method body must produce JS code")
                            .clone();
                    def_data_methods.push(CompiledDataMethod {
                        scope_entry_id: method_id,
                        self_scope_entry_id: self_id,
                        data_field: data_field.clone(),
                        impl_code,
                    });
                }
            }
        }
    }
    let has_js = stage1_code.is_some() || !def_funcs.is_empty() || !def_data_methods.is_empty();
    if !has_js {
        let logs = js_scope.get_slot::<V8LogStore>().unwrap().logs.take();
        return Ok(EvalResult {
            value: fallback_value.expect("No JS and no fallback value"),
            logs,
        });
    }
    let mut code1 = JsSourceBuilder::default();

    // Render definition set function implementations as JS functions
    for (scope_id, arg_scope_id, impl_code) in &def_funcs {
        let func_name = scope_entry_id_js_key(*scope_id);
        if let Some(arg_id) = arg_scope_id {
            code1.push(root_loc, format!("function {}({}) {{", func_name, scope_entry_id_js_key(*arg_id)));
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
        let func_name = scope_entry_id_js_key(method.scope_entry_id);
        code1.push(root_loc, format!("function {}() {{", func_name));
        code1.push(
            root_loc,
            format!("let {} = this.{};", scope_entry_id_js_key(method.self_scope_entry_id), method.data_field,),
        );
        let body_var = render_code(&mut code1, &method.impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "}".to_string());
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
            let tree_cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*args.get(0)).clone();
            let tree_value = tree_cell.value.borrow_mut().take().expect("stage tree already consumed");
            let tree =
                (tree_value.as_ref() as &dyn Any)
                    .downcast_ref::<Expr>()
                    .expect("stage tree must be an Expr")
                    .clone();
            let mut values: Vec<Box<dyn Value>> = Vec::new();
            let mut i = 1;
            while i < args.length() {
                let lift_arg = args.get(i);
                let val_arg = args.get(i + 1);
                i += 2;
                if lift_arg.is_null_or_undefined() {
                    // Already a StagedExprCell external
                    let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*val_arg).clone();
                    values.push(cell.value.borrow_mut().take().expect("stage lower value already consumed"));
                } else {
                    // Lift the raw v8 value
                    let lift_cell: Rc<StageLiftCell> = from_v8_ref::<Rc<StageLiftCell>>(&*lift_arg).clone();
                    let value = lift_cell.lift.lift(&*scope, val_arg).expect("__stage: lift failed");
                    values.push(value);
                }
            }

            /// Clone an expression tree, replacing Lower nodes at net depth 1 (lower_depth -
            /// stage_depth == 1) with Expr values from the iterator.
            fn clone_with_lower_substitutions(
                expr: &Expr,
                sd: usize,
                ld: usize,
                values: &mut dyn Iterator<Item = Box<dyn Value>>,
            ) -> Expr {
                match &expr.r#type {
                    Expr_::Lower(e) => {
                        let new_ld = ld + 1;
                        if new_ld - sd == 1 {
                            let value = values.next().expect("Not enough lower values for substitution");
                            if let Some(expr_val) = (value.as_ref() as &dyn Any).downcast_ref::<Expr>() {
                                expr_val.clone()
                            } else {
                                let value_cell = Rc::new(RefCell::new(Some(value)));
                                Expr {
                                    loc: expr.loc,
                                    r#type: Expr_::Literal(Rc::new(move || {
                                        value_cell.borrow_mut().take().expect("Lower literal consumed twice")
                                    })),
                                }
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
                    Expr_::Literal(_) | Expr_::ScopeUse(_) => expr.clone(),
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
            let result_cell = Rc::new(expr::StagedExprCell { value: RefCell::new(Some(Box::new(new_tree))) });
            let ext = to_v8(scope, result_cell);
            rv.set(ext.into());
        }

        let stage_fn = v8::Function::builder(stage_callback).build(js_scope).unwrap();
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(
                js_scope,
                v8::String::new(js_scope, &scope_entry_id_js_key(STAGE_FN_SCOPE_ID)).unwrap().cast(),
                stage_fn.into(),
            );
    }

    // Register __record_to_type and __tuple_to_type mortar functions.
    // These convert JS objects/arrays into Rc<dyn Type> externals at runtime.
    {
        /// Recursively convert a v8 value (external, object, or array) to Rc<dyn Type>.
        fn v8_value_to_type(
            scope: &mut v8::PinnedRef<v8::HandleScope>,
            value: v8::Local<v8::Value>,
        ) -> Rc<dyn Type> {
            if value.is_external() {
                return from_v8_ref::<Rc<dyn Type>>(&*value).clone();
            }
            if value.is_array() {
                let arr = v8::Local::<v8::Array>::try_from(value).unwrap();
                let len = arr.length();
                let mut elements = Vec::with_capacity(len as usize);
                for i in 0..len {
                    let elem = arr.get_index(scope, i).unwrap();
                    elements.push(tuple::TupleElement::Mortar(v8_value_to_type(scope, elem)));
                }
                return Rc::new(tuple::Tuple { source: None, elements });
            }
            if value.is_object() && !value.is_null_or_undefined() {
                let obj = v8::Local::<v8::Object>::try_from(value).unwrap();
                let keys = obj
                    .get_own_property_names(scope, v8::GetPropertyNamesArgs::default())
                    .unwrap();
                let mut fields = std::collections::BTreeMap::new();
                for i in 0..keys.length() {
                    let key = keys.get_index(scope, i).unwrap();
                    let key_str = key.to_string(scope).unwrap().to_rust_string_lossy(scope);
                    let val = obj.get(scope, key).unwrap();
                    fields.insert(
                        ConstKey::String(key_str),
                        record::RecordField::Mortar(v8_value_to_type(scope, val)),
                    );
                }
                return Rc::new(record::Record { source: None, fields });
            }
            panic!("v8_value_to_type: unexpected v8 value type");
        }

        fn record_to_type_callback<'s>(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let type_ = v8_value_to_type(scope, args.get(0));
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        fn tuple_to_type_callback<'s>(
            scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            args: v8::FunctionCallbackArguments<'s>,
            mut rv: v8::ReturnValue<'s>,
        ) {
            let type_ = v8_value_to_type(scope, args.get(0));
            let ext = rc_type_to_v8(scope, type_);
            rv.set(ext.into());
        }

        let record_to_type_fn =
            v8::Function::builder(record_to_type_callback).build(js_scope).unwrap();
        js_scope.get_current_context().global(js_scope).set(
            js_scope,
            v8::String::new(js_scope, &scope_entry_id_js_key(RECORD_TO_TYPE_FN_SCOPE_ID))
                .unwrap()
                .cast(),
            record_to_type_fn.into(),
        );

        let tuple_to_type_fn =
            v8::Function::builder(tuple_to_type_callback).build(js_scope).unwrap();
        js_scope.get_current_context().global(js_scope).set(
            js_scope,
            v8::String::new(js_scope, &scope_entry_id_js_key(TUPLE_TO_TYPE_FN_SCOPE_ID))
                .unwrap()
                .cast(),
            tuple_to_type_fn.into(),
        );
    }

    // Set externals as globals before running the script
    for (id, value) in ex.externals.borrow().iter() {
        let key = v8::String::new(js_scope, &scope_entry_id_js_key(*id)).unwrap().cast();
        if let Some(cell) = value.downcast_ref::<Rc<expr::StagedExprCell>>() {
            let ext = to_v8(js_scope, cell.clone());
            js_scope.get_current_context().global(js_scope).set(js_scope, key, ext.into());
        } else if let Some(cell) = value.downcast_ref::<Rc<StageLiftCell>>() {
            let ext = to_v8(js_scope, cell.clone());
            js_scope.get_current_context().global(js_scope).set(js_scope, key, ext.into());
        } else if let Some(type_) = value.downcast_ref::<Rc<dyn Type>>() {
            let ext = rc_type_to_v8(js_scope, type_.clone());
            js_scope.get_current_context().global(js_scope).set(js_scope, key, ext.into());
        } else {
            panic!("Unknown external type");
        }
    }
    let js_source = code1.lines.join("\n");
    eprintln!("source: [[{}]]", js_source);
    cix.gx.js_sources.lock().unwrap().push(js_source.clone());
    let script = v8::Script::compile(js_scope, v8::String::new(js_scope, &js_source).unwrap(), None).unwrap();
    match script.run(js_scope) {
        Some(stage2) => {
            let value_from_js = aspect_lift_js_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let value: Box<dyn Value> = value_from_js.lift(&*js_scope, stage2).map_err(|e| vec![ExprErr {
                loc: root_loc,
                errors: vec![ExprSubErr::Opaque(e)],
            }])?;
            let logs = js_scope.get_slot::<V8LogStore>().unwrap().logs.take();
            Ok(EvalResult {
                value,
                logs,
            })
        },
        None => {
            let text;
            match js_scope.exception() {
                Some(e) => {
                    text = e.to_rust_string_lossy(&js_scope);
                },
                None => {
                    text = format!("Script failed to return, but no exception captured (compiler bug)");
                },
            }
            match js_scope.message().and_then(|e| e.get_line_number(&js_scope)) {
                Some(js_line) => {
                    Err(vec![ExprErr {
                        loc: *code1.mapping.get(&(js_line as usize)).unwrap_or(&root_loc),
                        errors: vec![ExprSubErr::Opaque(text)]
                    }])
                },
                None => {
                    Err(vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque(text)]
                    }])
                },
            }
        },
    }
}

#[derive(Default)]
struct JsSourceBuilder {
    lines: Vec<String>,
    mapping: HashMap<usize, QExprId>,
    next_expr_id: usize,
}

impl JsSourceBuilder {
    fn push(&mut self, id: QExprId, line: String) {
        let js_num = self.lines.len() + 1;
        self.mapping.insert(js_num, id);
        self.lines.push(line);
    }

    fn next_expr_id(&mut self) -> usize {
        let id = self.next_expr_id;
        self.next_expr_id += 1;
        id
    }
}

fn render_code(lines: &mut JsSourceBuilder, code: &JsCode) -> String {
    let out = format!("{}{}", EXPR_PREFIX, lines.next_expr_id());
    lines.push(code.loc, format!("let {};", out));
    match &code.type_ {
        JsCodeType::Block(c) => {
            for entry in &c.entries {
                match entry {
                    JsCodeBlockEntry::Bind(b) => {
                        let bind_key = scope_entry_id_js_key(b.base);
                        lines.push(code.loc, format!("let {};", bind_key));
                        lines.push(code.loc, "{".to_string());
                        let val_var = render_code(lines, &b.value);
                        lines.push(code.loc, format!("{} = {};", bind_key, val_var));
                        lines.push(code.loc, "}".to_string());
                    },
                    JsCodeBlockEntry::Code(sub) => {
                        lines.push(code.loc, "{".to_string());
                        let sub_var = render_code(lines, sub);
                        lines.push(code.loc, format!("{} = {};", out, sub_var));
                        lines.push(code.loc, "}".to_string());
                    },
                }
            }
        },
        JsCodeType::Literal(c) => {
            lines.push(code.loc, format!("{} = {};", out, serde_json::to_string(c).unwrap()));
        },
        JsCodeType::BindingUse(c) => {
            lines.push(code.loc, format!("{} = {};", out, scope_entry_id_js_key(c.id)));
        },
        JsCodeType::Call(c) => {
            // Special case: method call preserves `this` binding
            if let JsCodeType::FieldUse(f) = &c.func.type_ {
                let base_var = render_code(lines, &f.base);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(code.loc, format!("{} = {}.{}({});", out, base_var, f.field, arg_vars.join(",")));
            } else {
                let func_var = render_code(lines, &c.func);
                let mut arg_vars = vec![];
                for arg in &c.args {
                    arg_vars.push(render_code(lines, arg));
                }
                lines.push(code.loc, format!("{} = ({})({});", out, func_var, arg_vars.join(",")));
            }
        },
        JsCodeType::FieldUse(js_code_field) => {
            let base = render_code(lines, &js_code_field.base);
            lines.push(code.loc, format!("{} = {}.{};", out, base, js_code_field.field));
        },
        JsCodeType::BindingSet(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{} = {};", scope_entry_id_js_key(c.base), val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        JsCodeType::FieldSet(c) => {
            let base_var = render_code(lines, &c.base);
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("{}.{} = {};", base_var, c.key, val_var));
            lines.push(code.loc, format!("{} = null;", out));
        },
        JsCodeType::NewObject(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let val_var = render_code(lines, value);
                fields_str.push(format!("{}: {}", key, val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        JsCodeType::NewMap(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let key_var = render_code(lines, key);
                let val_var = render_code(lines, value);
                fields_str.push(format!("[{}]: {}", key_var, val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        JsCodeType::NewArray(c) => {
            let mut elem_vars = vec![];
            for elem in &c.elements {
                elem_vars.push(render_code(lines, elem));
            }
            lines.push(code.loc, format!("{} = [{}];", out, elem_vars.join(", ")));
        },
        JsCodeType::If(c) => {
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
        JsCodeType::OperatorLt(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} < {});", out, base_var, ref_var));
        },
        JsCodeType::OperatorAdd(c) => {
            let base_var = render_code(lines, &c.base);
            let ref_var = render_code(lines, &c.reference);
            lines.push(code.loc, format!("{} = ({} + {});", out, base_var, ref_var));
        },
        JsCodeType::Foreach(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_js_key(c.element_key);
            lines.push(code.loc, format!("for (let {} of {}) {{", elem_key, base_var));
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        JsCodeType::ForeachMap(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_js_key(c.element_key);
            lines.push(code.loc, format!("for (let __entry of Object.entries({})) {{", base_var));
            lines.push(code.loc, format!("let {} = {{ key: __entry[0], value: __entry[1] }};", elem_key));
            let body_var = render_code(lines, &c.body);
            lines.push(code.loc, format!("{} = {};", out, body_var));
            lines.push(code.loc, "}".to_string());
        },
        JsCodeType::Match(c) => {
            let base_var = render_code(lines, &c.base);
            let elem_key = scope_entry_id_js_key(c.element_key);
            lines.push(code.loc, format!("{{ let [{}, {}] = Object.entries({})[0];", TAG_VAR, elem_key, base_var));
            lines.push(code.loc, format!("switch ({}) {{", TAG_VAR));
            for case in &c.cases {
                lines.push(code.loc, format!("case \"{}\": {{", case.js_tag));
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

pub async fn compile_importid(
    fx: &Arc<GlobalContext>,
    import_state: ImportStateId,
    source: Module,
    compile_path: Vec<(CompileId, ExprId)>,
) -> CompileRes {
    let cix = Rc::new(CompileImportContext {
        gx: fx.clone(),
        compile_path: compile_path,
        next_unique_id: Default::default(),
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
                Expr_::Literal(_) | Expr_::ScopeUse(_) => 0,
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
            });
            let value_type = eval_value.type_(&xx, root_loc);
            let value_serial = match aspect_serial_get(&xx, root_loc, value_type.as_ref()).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    result: CompileResResult::Err(e),
                },
            };
            let mut artifacts = fx.artifacts.write().unwrap();
            let value = match value_serial.to_serial(&mut artifacts, eval_value).into_res() {
                Ok(v) => v,
                Err(e) => return CompileRes {
                    logs,
                    result: CompileResResult::Err(e),
                },
            };
            return CompileRes {
                logs,
                result: CompileResResult::Ok(value),
            };
        }

        // Result is an Expr (from stage eval) - use it as next expression
        let next_expr = match (eval_value.as_ref() as &dyn Any).downcast_ref::<Expr>() {
            Some(expr) => expr.clone(),
            None => {
                // Value is not an Expr - this shouldn't happen for stage results
                return CompileRes {
                    logs: all_logs,
                    result: CompileResResult::Err(vec![ExprErr {
                        loc: root_loc,
                        errors: vec![ExprSubErr::Opaque("Stage eval produced non-Expr value".to_string(),)],
                    }]),
                };
            },
        };
        root_expr = next_expr;
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
