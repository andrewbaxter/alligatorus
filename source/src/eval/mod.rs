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
pub mod mortar_function;
pub mod expr;
pub mod tuple;
#[macro_use]
pub mod jswrap;
pub mod type_ref;
pub mod v8util;
pub mod definition_set;

use {
    crate::{
        builtin::{
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
            EXPR_PREFIX,
            TAG_PREFIX,
            JsCode,
            JsCodeBind,
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
    mortar_list::TypeListConstructor,
    mortar_null::TypeMortarNull,
    mortar_scope_entry::MortarScopeEntry,
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
    tuple::{
        StaticScopeEntry,
        ValueRecord,
        ValueTuple,
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

/// Store an `Rc<T>` as a `v8::External`. A guaranteed finalizer drops the boxed Rc
/// when the isolate is disposed, preventing leaks. Use `rc_from_v8` to retrieve a
/// clone of the Rc (incrementing the refcount, never taking ownership), which
/// prevents double-free.
pub(crate) fn rc_to_v8<
    's,
    T: 'static,
>(scope: &mut v8::PinScope<'s, '_, ()>, value: Rc<T>) -> v8::Local<'s, v8::External> {
    let ptr = Box::into_raw(Box::new(value)) as *mut std::ffi::c_void;
    let ext = v8::External::new(&*scope, ptr);
    let weak = v8::Weak::with_guaranteed_finalizer(&mut *scope, ext, Box::new(move || unsafe {
        drop(Box::from_raw(ptr as *mut Rc<T>));
    }));
    scope.get_slot::<V8RcStore>().unwrap().weaks.borrow_mut().push(weak);
    ext
}

/// Extract an `Rc<T>` from a `v8::External` created by `rc_to_v8`. Clones the Rc
/// (bumps refcount) without taking ownership of the box — the weak finalizer
/// remains the sole owner.
pub(crate) fn rc_from_v8<T: 'static>(value: &v8::Value) -> Rc<T> {
    assert!(value.is_external(), "rc_from_v8: expected v8::External");
    let ext = unsafe {
        &*(value as *const v8::Value as *const v8::External)
    };
    let ptr = ext.value() as *const Rc<T>;
    unsafe {
        (*ptr).clone()
    }
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
        self.errors.extend(r.errors);
        return Some(cv);
    }

    pub(crate) fn must_er<T>(&mut self, r: EResult<T>) -> Result<T, ()> {
        self.errors.extend(r.errors);
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

trait Target {
    type Effect;

    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        carry: Code,
        cv: CodeValue,
    ) -> EResult<(Vec<JsCodeBlockEntry>, Box<dyn ScopeEntry>)>;
    fn block(&self, loc: QExprId, entries: Vec<JsCodeBlockEntry>) -> EResult<Code>;
}
struct MortarTarget;

impl Target for MortarTarget {
    type Effect = JsCode;

    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        carry: Code,
        cv: CodeValue,
    ) -> EResult<(Vec<JsCodeBlockEntry>, Box<dyn ScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        let Some(bind_js) = aspect_bind_js_get_maybe(xx, vtype.as_ref()) else {
            let mut entries = vec![];
            if let Some(carry) = carry.cast::<JsCode>().into_res().ok().flatten() {
                entries.push(JsCodeBlockEntry::Code(carry.clone()));
            }
            return EResult::ok((entries, Box::new(StaticScopeEntry { type_: vtype }) as Box<dyn ScopeEntry>));
        };
        return er_seq!(loc, erb, (Vec<JsCodeBlockEntry>, Box < dyn ScopeEntry >), {
            let bind_result = bind_js.bind_js(xx, loc, cv);
            if bind_result.v.is_none() {
                let mut entries = vec![];
                if let Some(carry) = erb.maybe_er(carry.cast::<JsCode>()).flatten() {
                    entries.push(JsCodeBlockEntry::Code(carry.clone()));
                }
                return Ok((entries, Box::new(StaticScopeEntry { type_: vtype }) as Box<dyn ScopeEntry>));
            }
            let (vcode, vtype) = erb.must_er(bind_result)?;
            let js_key = ScopeEntryId(xx.vx.next_scope_entry_id.next());
            let mut entries = vec![];
            if let Some(carry) = erb.maybe_er(carry.cast::<JsCode>()).flatten() {
                entries.push(JsCodeBlockEntry::Code(carry.clone()));
            }
            entries.push(JsCodeBlockEntry::Bind(JsCodeBind {
                base: js_key,
                value: Box::new(vcode),
            }));
            return Ok((
                entries,
                Box::new(MortarScopeEntry {
                    js_key: js_key,
                    type_: vtype,
                }),
            ));
        });
    }

    fn block(&self, loc: QExprId, entries: Vec<JsCodeBlockEntry>) -> EResult<Code> {
        if entries.is_empty() {
            return EResult::ok(Code {
                loc: loc,
                code: Code_::None,
            });
        }
        return EResult::ok(Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(JsCode {
                loc: loc,
                type_: JsCodeType::Block(JsCodeBlock { entries }),
            })),
        });
    }
}

fn extract_const_key(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> Result<ConstKey, String> {
    let vtype = v.type_(xx, loc);
    if let Some(a) = aspect_string_get_maybe(xx, vtype.as_ref()) {
        return Ok(ConstKey::String(a.get(v)));
    }
    return Err(format!("TODO value type [X] must expose const key aspect [LIST] but doesn't"));
}

fn eval_half<T: Target>(xx: &ExprContext, expr: &Expr, target: &T) -> EvalRes {
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
                errors: vec![],
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
                        let binding = erb.must_er(target.bind(&child_xx, sub.loc, base.code, value))?;
                        entries.extend(binding.0);
                        child_xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
                        last_val = Some(Box::new(ValueNull) as Box<dyn Value>);
                    } else {
                        let r = erb.must_er(eval_half(&child_xx, sub, target))?;
                        match r.code.cast::<JsCode>().into_res() {
                            Ok(Some(js)) => entries.push(JsCodeBlockEntry::Code(js.clone())),
                            Ok(None) => {},
                            Err(e) => { erb.errors.extend(e); return Err(()); },
                        }
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
            let cix = &xx.vx.ix;
            let import_state = xx.import_state.get();
            let resolved = match substitute_lowers(cix, import_state, &e.expr) {
                Ok(e) => e,
                Err(errors) => return EvalRes {
                    v: None,
                    errors,
                },
            };
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Box::new(resolved),
            });
        },
        Expr_::Lower(_) => 
        // Just a marker for replacement
        unreachable!(),
        Expr_::Tuple(t) => {
            let elements: Vec<EvalRes> = t.elements.iter().map(|e| eval_half(xx, e, target)).collect();
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None,
                },
                value: Box::new(ValueTuple { elements }),
            });
        },
        Expr_::Record(r) => return er_seq_cv!(expr.loc, erb, {
            let mut entries = Vec::new();
            for (k_expr, v_expr) in &r.entries {
                let k_cv = erb.must_er(eval_half(xx, k_expr, target))?;
                let key = erb.must_res(extract_const_key(xx, expr.loc, k_cv.value.as_ref()))?;
                entries.push((key, eval_half(xx, v_expr, target)));
            }
            // Try to produce a JS object when all entries have JS representations
            let mut js_fields: Vec<(String, JsCode)> = Vec::new();
            let mut type_entries: Vec<(String, Rc<dyn Type>)> = Vec::new();
            let mut all_js = true;
            for (key, entry_res) in &entries {
                let key_str = match key {
                    ConstKey::String(s) => s.clone(),
                    _ => { all_js = false; break; },
                };
                if let Some(cv) = &entry_res.v {
                    let vtype = cv.value.type_(xx, expr.loc);
                    if let Some(consume) = aspect_consume_js_get_maybe(xx, vtype.as_ref()) {
                        // We'll consume these later if all_js stays true
                        type_entries.push((key_str, vtype));
                    } else {
                        all_js = false;
                        break;
                    }
                } else {
                    all_js = false;
                    break;
                }
            }
            if all_js && !entries.is_empty() {
                // Consume all entries to JS
                let consumed_entries: Vec<(ConstKey, EvalRes)> = entries;
                for (key, entry_res) in consumed_entries {
                    let key_str = match &key {
                        ConstKey::String(s) => s.clone(),
                        _ => unreachable!(),
                    };
                    erb.errors.extend(entry_res.errors);
                    if let Some(cv) = entry_res.v {
                        let vtype = cv.value.type_(xx, expr.loc);
                        let consume = aspect_consume_js_get_maybe(xx, vtype.as_ref()).unwrap();
                        let js = erb.must_er(consume.to_js(xx, expr.loc, cv))?;
                        js_fields.push((key_str, js));
                    } else {
                        return Err(());
                    }
                }
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: expr.loc,
                            type_: JsCodeType::NewObject(JsCodeNewObject { fields: js_fields }),
                        })),
                    },
                    value: Box::new(tuple::StaticValue {
                        type_: Rc::new(tuple::TypeRecord { entries: type_entries }),
                    }),
                });
            }
            return Ok(CodeValue {
                code: Code {
                    loc: expr.loc,
                    code: Code_::None
                },
                value: Box::new(ValueRecord { entries }),
            });
        }),
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
            let element_scope_id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
            let child_scope = Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: Some(xx.scope.clone()),
            });
            child_scope.values.borrow_mut().insert(
                element_key,
                Box::new(MortarScopeEntry {
                    js_key: element_scope_id,
                    type_: iterable.element_type(),
                }),
            );
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
            let mut case_tags: Vec<String> = Vec::new();
            for case in &e.cases {
                let cond_cv = erb.must_er(eval_half(xx, &case.condition, target))?;
                let tag = erb.must_res(extract_const_key(xx, expr.loc, cond_cv.value.as_ref()))?;
                let tag_str = match &tag {
                    ConstKey::String(s) => s.clone(),
                    _ => return erb.err_new("Match case condition must be a string"),
                };
                if !variants.iter().any(|(t, _)| t == &tag_str) {
                    return erb.err_new(format!("Match case tag '{}' not found in tagged union", tag_str));
                }
                case_tags.push(tag_str);
            }

            // Check exhaustiveness (if no fallback, all variants must be covered)
            if e.fallback.is_none() {
                for (variant_tag, _) in &variants {
                    if !case_tags.contains(variant_tag) {
                        return erb.err_new(format!(
                            "Non-exhaustive match: missing case for tag '{}'", variant_tag
                        ));
                    }
                }
            }

            // Generate JS match code
            let base_type_inner = base.value.type_(xx, expr.loc);
            let to_js = erb.must_er(aspect_consume_js_get(xx, expr.loc, base_type_inner.as_ref()))?;
            let base_js = erb.must_er(to_js.to_js(xx, expr.loc, base))?;

            let element_scope_id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
            let mut js_cases = Vec::new();
            for (i, case) in e.cases.iter().enumerate() {
                let tag_str = &case_tags[i];
                let variant_type = variants.iter().find(|(t, _)| t == tag_str).unwrap().1.clone();

                let child_scope = Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: Some(xx.scope.clone()),
                });
                child_scope.values.borrow_mut().insert(
                    element_key.clone(),
                    Box::new(MortarScopeEntry {
                        js_key: element_scope_id,
                        type_: variant_type,
                    }),
                );
                let child_xx = ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: child_scope,
                };
                let body_cv = erb.must_er(eval_half(&child_xx, &case.body, target))?;
                let body_type = body_cv.value.type_(&child_xx, expr.loc);
                let body_js = if let Some(consume) = aspect_consume_js_get_maybe(&child_xx, body_type.as_ref()) {
                    erb.must_er(consume.to_js(&child_xx, expr.loc, body_cv))?
                } else {
                    match erb.must_er(body_cv.code.cast::<JsCode>())? {
                        Some(c) => c.clone(),
                        None => JsCode { loc: expr.loc, type_: JsCodeType::Literal(serde_json::Value::Null) },
                    }
                };
                js_cases.push(JsCodeMatchBranch {
                    tag: tag_str.clone(),
                    body: body_js,
                });
            }

            let js_fallback = if let Some(fallback_expr) = &e.fallback {
                let fb_cv = erb.must_er(eval_half(xx, fallback_expr, target))?;
                let fb_type = fb_cv.value.type_(xx, expr.loc);
                let fb_js = if let Some(consume) = aspect_consume_js_get_maybe(xx, fb_type.as_ref()) {
                    erb.must_er(consume.to_js(xx, expr.loc, fb_cv))?
                } else {
                    match erb.must_er(fb_cv.code.cast::<JsCode>())? {
                        Some(c) => c.clone(),
                        None => JsCode { loc: expr.loc, type_: JsCodeType::Literal(serde_json::Value::Null) },
                    }
                };
                Some(Box::new(fb_js))
            } else {
                None
            };

            let match_js = JsCode {
                loc: expr.loc,
                type_: JsCodeType::Match(JsCodeMatch {
                    base: Box::new(base_js),
                    element_key: element_scope_id,
                    cases: js_cases,
                    fallback: js_fallback,
                }),
            };
            let null_stack = crate::builtin::aspect_js_stack_value_get(xx, expr.loc, &TypeMortarNull as &dyn Type);
            let null_stack = erb.must_er(null_stack)?;
            return Ok(null_stack.to_value(expr.loc, match_js));
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
                    code: Code { loc: expr.loc, code: Code_::None },
                    value: Box::new(ValueNull),
                });
            } else {
                // Generate JS if/else chain
                fn cv_to_js(erb: &mut Erb, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> Result<JsCode, ()> {
                    let vtype = cv.value.type_(xx, loc);
                    if let Some(consume) = aspect_consume_js_get_maybe(xx, vtype.as_ref()) {
                        return erb.must_er(consume.to_js(xx, loc, cv));
                    }
                    // Fall back to extracting code directly (e.g. set returns null + JS code)
                    match erb.must_er(cv.code.cast::<JsCode>())? {
                        Some(c) => Ok(c.clone()),
                        None => Ok(JsCode { loc, type_: JsCodeType::Literal(serde_json::Value::Null) }),
                    }
                }
                let mut js_conditions = Vec::new();
                for (_val, cond, body) in conditions_evaluated {
                    let cond_js = cv_to_js(&mut erb, xx, expr.loc, cond)?;
                    let body_cv = erb.must_er(eval_half(xx, body, target))?;
                    let body_js = cv_to_js(&mut erb, xx, expr.loc, body_cv)?;
                    js_conditions.push((cond_js, body_js));
                }
                let js_else = if let Some(else_expr) = &e.else_ {
                    let else_cv = erb.must_er(eval_half(xx, else_expr, target))?;
                    let else_js = cv_to_js(&mut erb, xx, expr.loc, else_cv)?;
                    Some(Box::new(else_js))
                } else {
                    None
                };
                return Ok(CodeValue {
                    code: Code {
                        loc: expr.loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: expr.loc,
                            type_: JsCodeType::If(JsCodeIf {
                                conditions: js_conditions,
                                else_: js_else,
                            }),
                        })),
                    },
                    value: Box::new(ValueNull),
                });
            }
        }),
    }
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let source_type = source.value.type_(xx, loc);
        let dest;
        if let Some(dest1) = erb.maybe_er(source_type.cast_to(xx, loc, &source, dest_type)) {
            dest = dest1;
        } else if let Some(dest1) = erb.maybe_er(dest_type.cast_from(xx, loc, &source)) {
            dest = dest1;
        } else {
            return erb.err_new(
                format!(
                    "Incompatible types: source {} cannot convert to dest {}",
                    source_type.desc(),
                    dest_type.desc()
                )
            );
        }
        return Ok(dest);
    });
}

pub(crate) fn scope_entry_id_js_key(e: ScopeEntryId) -> String {
    return format!("{}{}", BINDING_PREFIX, e.0);
}

pub(crate) fn eval_half_mortar(xx: &ExprContext, expr: &Expr) -> EvalRes {
    eval_half(xx, expr, &MortarTarget)
}

fn substitute_lowers(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<Expr, Vec<ExprErr>> {
    match &expr.r#type {
        Expr_::Lower(e) => {
            let resolved_inner = substitute_lowers(cix, import_state, &e.expr)?;
            let eval_result = eval(cix, import_state, &resolved_inner)?;
            let value: Box<dyn Value> = eval_result.value;
            if let Some(expr) = (value.as_ref() as &dyn Any).downcast_ref::<Expr>() {
                Ok(expr.clone())
            } else {
                let value_cell = Rc::new(RefCell::new(Some(value)));
                Ok(Expr {
                    loc: expr.loc,
                    r#type: Expr_::Literal(Rc::new(move || {
                        value_cell.borrow_mut().take().expect("Lower literal consumed twice")
                    })),
                })
            }
        },
        Expr_::Stage(_) => Ok(expr.clone()),
        Expr_::Literal(_) => Ok(expr.clone()),
        Expr_::ScopeUse(_) => Ok(expr.clone()),
        Expr_::Call(c) => {
            let func = substitute_lowers(cix, import_state, &c.func)?;
            let arg = substitute_lowers(cix, import_state, &c.arg)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Call(ExprCall {
                    func: Rc::new(func),
                    arg: Rc::new(arg),
                }),
            })
        },
        Expr_::Bind(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let value = substitute_lowers(cix, import_state, &e.value)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Bind(ExprBind {
                    base: Rc::new(base),
                    value: Rc::new(value),
                }),
            })
        },
        Expr_::Set(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let value = substitute_lowers(cix, import_state, &e.value)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Set(ExprSet {
                    base: Rc::new(base),
                    value: Rc::new(value),
                }),
            })
        },
        Expr_::Field(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let key = substitute_lowers(cix, import_state, &e.key)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Field(ExprField {
                    base: Rc::new(base),
                    key: Rc::new(key),
                }),
            })
        },
        Expr_::Block(e) => {
            let mut resolved = Vec::new();
            for sub in &e.exprs {
                resolved.push(substitute_lowers(cix, import_state, sub)?);
            }
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Block(ExprBlock { exprs: resolved }),
            })
        },
        Expr_::Tuple(t) => {
            let mut elements = Vec::new();
            for e in &t.elements {
                elements.push(substitute_lowers(cix, import_state, e)?);
            }
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Tuple(ExprTuple { elements }),
            })
        },
        Expr_::Record(r) => {
            let mut entries = Vec::new();
            for (k, e) in &r.entries {
                entries.push(
                    (substitute_lowers(cix, import_state, k)?, substitute_lowers(cix, import_state, e)?),
                );
            }
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Record(expr::ExprRecord { entries }),
            })
        },
        Expr_::If(e) => {
            let mut conditions = Vec::new();
            for b in &e.conditions {
                conditions.push(expr::ExprConditionBranch {
                    condition: substitute_lowers(cix, import_state, &b.condition)?,
                    body: substitute_lowers(cix, import_state, &b.body)?,
                });
            }
            let else_ = match &e.else_ {
                Some(e) => Some(Rc::new(substitute_lowers(cix, import_state, e)?)),
                None => None,
            };
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::If(expr::ExprIf { conditions, else_ }),
            })
        },
        Expr_::OperatorLt(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let reference = substitute_lowers(cix, import_state, &e.reference)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::OperatorLt(expr::ExprOperatorLt {
                    base: Rc::new(base),
                    reference: Rc::new(reference),
                }),
            })
        },
        Expr_::OperatorAdd(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let reference = substitute_lowers(cix, import_state, &e.reference)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::OperatorAdd(expr::ExprOperatorAdd {
                    base: Rc::new(base),
                    reference: Rc::new(reference),
                }),
            })
        },
        Expr_::Foreach(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let element_key = substitute_lowers(cix, import_state, &e.element_key)?;
            let body = substitute_lowers(cix, import_state, &e.body)?;
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Foreach(ExprForeach {
                    base: Rc::new(base),
                    element_key: Rc::new(element_key),
                    body: Rc::new(body),
                }),
            })
        },
        Expr_::Match(e) => {
            let base = substitute_lowers(cix, import_state, &e.base)?;
            let element_key = substitute_lowers(cix, import_state, &e.element_key)?;
            let mut cases = Vec::new();
            for c in &e.cases {
                cases.push(ExprMatchBranch {
                    condition: substitute_lowers(cix, import_state, &c.condition)?,
                    body: substitute_lowers(cix, import_state, &c.body)?,
                });
            }
            let fallback = match &e.fallback {
                Some(f) => Some(Rc::new(substitute_lowers(cix, import_state, f)?)),
                None => None,
            };
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Match(ExprMatch {
                    base: Rc::new(base),
                    element_key: Rc::new(element_key),
                    cases,
                    fallback,
                }),
            })
        },
    }
}

struct EvalResult {
    value: Box<dyn Value>,
    logs: Vec<crate::langhelp::LogEntry>,
}

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
    let js_scope_handle = std::pin::pin!(v8::HandleScope::new(&mut js_scope_isolate));
    let mut js_scope_handle2 = js_scope_handle.init();
    let js_scope_context = v8::Context::new(&mut js_scope_handle2, Default::default());
    let js_scope_context2 = &mut v8::ContextScope::new(&mut js_scope_handle2, js_scope_context);
    tc_scope!(let js_scope, js_scope_context2);
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_scope_entry_id: Default::default(),
        definition_sets: RefCell::new(Vec::new()),
        externals: RefCell::new(Vec::new()),
    });
    let mut s1_scope = HashMap::new();
    for (key, eval_value, js_value) in [jswrap!(fn log(ex, js_scope, scope)(message: String) ->() {
        let timestamp_ms =
            std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
        scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(crate::langhelp::LogEntry {
            timestamp_ms,
            message
        });
    }), jswrap!(fn define(ex, js_scope, scope)() -> DefineResult {
        DefineResult { ds: Rc::new(Gc::new(DefinitionSet::new())) }
    })] {
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(
                js_scope,
                v8::String::new(js_scope, &scope_entry_id_js_key(eval_value.js_key)).unwrap().cast(),
                js_value,
            );
        s1_scope.insert(ConstKey::String(key.to_string()), Box::new(eval_value) as Box<dyn ScopeEntry>);
    }

    // Type references (static scope entries, no JS representation)
    s1_scope.insert(
        ConstKey::String("String".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarString) }) as Box<dyn ScopeEntry>,
    );
    s1_scope.insert(
        ConstKey::String("string".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarString) }) as Box<dyn ScopeEntry>,
    );
    s1_scope.insert(
        ConstKey::String("number".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeMortarNumber) }) as Box<dyn ScopeEntry>,
    );
    s1_scope.insert(
        ConstKey::String("list".to_string()),
        Box::new(StaticScopeEntry { type_: Rc::new(TypeListConstructor) }) as Box<dyn ScopeEntry>,
    );
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
    if !result.errors.is_empty() {
        return Err(result.errors);
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

    // Compile definition set implementations
    let mut def_funcs: Vec<(ScopeEntryId, Option<ScopeEntryId>, JsCode)> = vec![];
    for ds in ex.definition_sets.borrow().iter() {
        for func_data in ds.functions.borrow().iter() {
            let impl_expr = func_data.impl_expr.borrow();
            let impl_expr = impl_expr.as_ref().expect("Definition set function missing implementation");
            let impl_expr_ast =
                (impl_expr.as_ref() as &dyn Any)
                    .downcast_ref::<Expr>()
                    .expect("Definition set impl must be an Expr");

            // Resolve arg type and create scope entry if present
            let arg_scope_id = if func_data.arg_type_expr.borrow().is_some() {
                let arg_expr = func_data.arg_type_expr.borrow();
                let arg_expr_ast =
                    (arg_expr.as_ref().unwrap().as_ref() as &dyn Any)
                        .downcast_ref::<Expr>()
                        .expect("arg type must be an Expr");
                let arg_result = eval_half(&xx, arg_expr_ast, &MortarTarget);
                if !arg_result.errors.is_empty() {
                    return Err(arg_result.errors);
                }
                let arg_cv = arg_result.v.unwrap();
                let arg_type = arg_cv.value.type_(&xx, root_loc);
                let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                func_data.arg_scope_entry_id.set(Some(id));
                Some((id, arg_type))
            } else {
                None
            };

            // Resolve ret type if present
            let ret_type: Option<Rc<dyn Type>> = if func_data.ret_type_expr.borrow().is_some() {
                let ret_expr = func_data.ret_type_expr.borrow();
                let ret_expr_ast =
                    (ret_expr.as_ref().unwrap().as_ref() as &dyn Any)
                        .downcast_ref::<Expr>()
                        .expect("ret type must be an Expr");
                let ret_result = eval_half(&xx, ret_expr_ast, &MortarTarget);
                if !ret_result.errors.is_empty() {
                    return Err(ret_result.errors);
                }
                let ret_cv = ret_result.v.unwrap();
                Some(ret_cv.value.type_(&xx, root_loc))
            } else {
                None
            };

            // Evaluate impl body with arg in scope if needed
            let func_xx = if let Some((arg_id, ref arg_type)) = arg_scope_id {
                let child_scope = Rc::new(Scope {
                    values: RefCell::new(HashMap::new()),
                    parent: Some(xx.scope.clone()),
                });
                child_scope
                    .values
                    .borrow_mut()
                    .insert(ConstKey::String("arg".to_string()), Box::new(MortarScopeEntry {
                        js_key: arg_id,
                        type_: arg_type.clone(),
                    }));
                Rc::new(ExprContext {
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: child_scope,
                })
            } else {
                xx.clone()
            };
            let impl_result = eval_half(&func_xx, impl_expr_ast, &MortarTarget);
            if !impl_result.errors.is_empty() {
                return Err(impl_result.errors);
            }
            let impl_cv = impl_result.v.unwrap();

            // Type check return value against ret type if specified
            let impl_cv = if let Some(ref ret_type) = ret_type {
                let r = typecheck_convert(&func_xx, root_loc, ret_type.as_ref(), impl_cv);
                if !r.errors.is_empty() {
                    return Err(r.errors);
                }
                r.v.unwrap()
            } else {
                impl_cv
            };
            let impl_code =
                impl_cv
                    .code
                    .cast::<JsCode>()
                    .into_res()?
                    .expect("Definition set function body must produce JS code")
                    .clone();
            let scope_id = func_data.scope_entry_id.get().expect("Definition set function not registered");
            def_funcs.push((scope_id, arg_scope_id.map(|(id, _)| id), impl_code));
        }
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

                // Allocate self scope entry ID if needed
                if method.self_scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    method.self_scope_entry_id.set(Some(id));
                }
                if method.scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    method.scope_entry_id.set(Some(id));
                }
                let self_id = method.self_scope_entry_id.get().unwrap();
                let method_id = method.scope_entry_id.get().unwrap();

                // Resolve ret type
                let ret_type: Option<Rc<dyn Type>> = if method.ret_type_expr.borrow().is_some() {
                    let ret_expr = method.ret_type_expr.borrow();
                    let ret_expr_ast =
                        (ret_expr.as_ref().unwrap().as_ref() as &dyn Any)
                            .downcast_ref::<expr::Expr>()
                            .expect("ret type must be an Expr");
                    let ret_result = eval_half(&xx, ret_expr_ast, &MortarTarget);
                    if !ret_result.errors.is_empty() {
                        return Err(ret_result.errors);
                    }
                    Some(ret_result.v.unwrap().value.type_(&xx, root_loc))
                } else {
                    None
                };

                // Compile method body with `self` in scope
                let method_xx = {
                    let child_scope = Rc::new(Scope {
                        values: RefCell::new(HashMap::new()),
                        parent: Some(xx.scope.clone()),
                    });
                    child_scope
                        .values
                        .borrow_mut()
                        .insert(ConstKey::String("self".to_string()), Box::new(MortarScopeEntry {
                            js_key: self_id,
                            type_: underlying_type.clone(),
                        }));
                    Rc::new(ExprContext {
                        vx: xx.vx.clone(),
                        import_state: xx.import_state.clone(),
                        scope: child_scope,
                    })
                };
                let impl_result = eval_half(&method_xx, impl_expr_ast, &MortarTarget);
                if !impl_result.errors.is_empty() {
                    return Err(impl_result.errors);
                }
                let impl_cv = impl_result.v.unwrap();

                // Type check return value
                let impl_cv = if let Some(ref ret_type) = ret_type {
                    let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                    if !r.errors.is_empty() {
                        return Err(r.errors);
                    }
                    r.v.unwrap()
                } else {
                    impl_cv
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
                    if method_impl.self_scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method_impl.self_scope_entry_id.set(Some(id));
                    }
                    if method_impl.scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method_impl.scope_entry_id.set(Some(id));
                    }
                    let self_id = method_impl.self_scope_entry_id.get().unwrap();
                    let method_id = method_impl.scope_entry_id.get().unwrap();

                    // Resolve ret type from interface method
                    let ret_type_expr_clone = {
                        let interfaces = ds.interfaces.borrow();
                        let iface = &interfaces[impl_data.interface_index];
                        let iface_methods = iface.methods.borrow();
                        let iface_method = &iface_methods[method_impl.interface_method_index];
                        iface_method.ret_type_expr.borrow().as_ref().map(|v| {
                            (v.as_ref() as &dyn Any)
                                .downcast_ref::<expr::Expr>()
                                .expect("ret type must be an Expr")
                                .clone()
                        })
                    };
                    let ret_type: Option<Rc<dyn Type>> = match ret_type_expr_clone {
                        Some(ret_expr_ast) => {
                            let ret_result = eval_half(&xx, &ret_expr_ast, &MortarTarget);
                            if !ret_result.errors.is_empty() {
                                return Err(ret_result.errors);
                            }
                            Some(ret_result.v.unwrap().value.type_(&xx, root_loc))
                        },
                        None => None,
                    };

                    // Compile method body with `self` in scope
                    let method_xx = {
                        let child_scope = Rc::new(Scope {
                            values: RefCell::new(HashMap::new()),
                            parent: Some(xx.scope.clone()),
                        });
                        child_scope
                            .values
                            .borrow_mut()
                            .insert(ConstKey::String("self".to_string()), Box::new(MortarScopeEntry {
                                js_key: self_id,
                                type_: underlying_type.clone(),
                            }));
                        Rc::new(ExprContext {
                            vx: xx.vx.clone(),
                            import_state: xx.import_state.clone(),
                            scope: child_scope,
                        })
                    };
                    let impl_result = eval_half(&method_xx, impl_expr_ast, &MortarTarget);
                    if !impl_result.errors.is_empty() {
                        return Err(impl_result.errors);
                    }
                    let impl_cv = impl_result.v.unwrap();
                    let impl_cv = if let Some(ref ret_type) = ret_type {
                        let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
                        if !r.errors.is_empty() {
                            return Err(r.errors);
                        }
                        r.v.unwrap()
                    } else {
                        impl_cv
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
    code1.push(root_loc, "(function() {".to_string());

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
        code1.push(root_loc, format!("return {};", root_var));
    } else {
        code1.push(root_loc, "return null;".to_string());
    }

    // Set externals as globals before running the script
    for (id, value) in ex.externals.borrow().iter() {
        let cell: Rc<expr::StagedExprCell> =
            value.clone().downcast::<expr::StagedExprCell>().expect("External must hold StagedExprCell");
        let ext = rc_to_v8(js_scope, cell);
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(js_scope, v8::String::new(js_scope, &scope_entry_id_js_key(*id)).unwrap().cast(), ext.into());
    }
    code1.push(root_loc, "})()".to_string());
    let js_source = code1.lines.join("\n");
    eprintln!("source: [[{}]]", js_source);
    let script = v8::Script::compile(js_scope, v8::String::new(js_scope, &js_source).unwrap(), None).unwrap();
    match script.run(js_scope) {
        Some(stage2) => {
            let value_from_js = aspect_lift_js_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let value: Box<dyn Value> = value_from_js.lift(&*js_scope, stage2);
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
        JsCodeType::Match(c) => {
            let base_var = render_code(lines, &c.base);
            let tag_var = format!("{}{}", TAG_PREFIX, lines.next_expr_id());
            let elem_key = scope_entry_id_js_key(c.element_key);
            lines.push(code.loc, format!("for (let [{}, {}] of Object.entries({})) {{", tag_var, elem_key, base_var));
            for (i, case) in c.cases.iter().enumerate() {
                if i == 0 {
                    lines.push(code.loc, format!("if ({} === \"{}\") {{", tag_var, case.tag));
                } else {
                    lines.push(code.loc, format!("}} else if ({} === \"{}\") {{", tag_var, case.tag));
                }
                let body_var = render_code(lines, &case.body);
                lines.push(code.loc, format!("{} = {};", out, body_var));
            }
            if let Some(fallback) = &c.fallback {
                lines.push(code.loc, "} else {".to_string());
                let fb_var = render_code(lines, fallback);
                lines.push(code.loc, format!("{} = {};", out, fb_var));
            }
            if !c.cases.is_empty() || c.fallback.is_some() {
                lines.push(code.loc, "}".to_string());
            }
            lines.push(code.loc, "}".to_string());
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
    let root_expr = match source {
        Module::V1(e) => e,
    };
    let root_expr = match substitute_lowers(&cix, import_state, &root_expr) {
        Ok(e) => e,
        Err(errors) => return CompileRes {
            logs: vec![],
            result: CompileResResult::Err(errors),
        },
    };
    let root_loc = root_expr.loc;
    let eval_result = match eval(&cix, import_state, &root_expr) {
        Ok(v) => v,
        Err(errors) => return CompileRes {
            logs: vec![],
            result: CompileResResult::Err(errors),
        },
    };
    let logs = eval_result.logs;

    // Serialize result
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_scope_entry_id: Default::default(),
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
    let value_type = eval_result.value.type_(&xx, root_loc);
    let value_serial = match aspect_serial_get(&xx, root_loc, value_type.as_ref()).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes {
            logs,
            result: CompileResResult::Err(e),
        },
    };
    let mut artifacts = fx.artifacts.write().unwrap();
    let value = match value_serial.to_serial(&mut artifacts, eval_result.value).into_res() {
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

pub fn setup_v8() {
    static V: OnceLock<()> = OnceLock::new();
    V.get_or_init(|| {
        let platform = v8::new_default_platform(0, false).make_shared();
        v8::V8::initialize_platform(platform);
        v8::V8::initialize();
        return ();
    });
}
