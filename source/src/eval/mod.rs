#[macro_use]
pub mod util;
pub mod null;
pub mod string;
pub mod mortar_js_value;
pub mod mortar_scope_entry;
pub mod mortar_null;
pub mod mortar_string;
pub mod mortar_function;
pub mod expr;
pub mod tuple;
#[macro_use]
pub mod jswrap;
pub mod definition_set;

use {
    crate::{
        builtin::{
            aspect_consume_js_get,
            aspect_consume_js_get_maybe,
            aspect_field_get,
            aspect_field_get_maybe,
            aspect_function_get,
            aspect_lift_js_get,
            aspect_serial_get,
            aspect_set_get,
            aspect_string_get_maybe,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBind,
            JsCodeSeq,
            JsCodeType,
            JsCodeArray,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
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
            SerialValueSource,
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
        ExprCall,
        ExprField,
        ExprLower,
        ExprScopeUse,
        ExprSeq,
        ExprSet,
        ExprStage,
        ExprTuple,
        Expr_,
        Module,
    },
    flowcontrol::shed,
    mortar_function::MortarTypeFunction,
    mortar_null::TypeMortarNull,
    mortar_scope_entry::MortarScopeEntry,
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

// --- Rc<T> ↔ v8::External helpers ---

/// Weak handle storage, set as an isolate slot. During dispose_annex(), slots
/// are cleared first (Weak handles dropped — but isolate ptr is already null so
/// finalizers can't be cancelled), then finalizer_map is drained, running our
/// guaranteed finalizer callbacks.
pub(crate) struct V8RcStore {
    weaks: RefCell<Vec<v8::Weak<v8::External>>>,
}

pub(crate) struct V8LogStore {
    pub(crate) logs: RefCell<Vec<crate::langhelp::LogEntry>>,
}

/// Store an `Rc<T>` as a `v8::External`. A guaranteed finalizer drops the
/// boxed Rc when the isolate is disposed, preventing leaks. Use `rc_from_v8`
/// to retrieve a clone of the Rc (incrementing the refcount, never taking
/// ownership), which prevents double-free.
pub(crate) fn rc_to_v8<'s, T: 'static>(
    scope: &mut v8::PinScope<'s, '_, ()>,
    value: Rc<T>,
) -> v8::Local<'s, v8::External> {
    let ptr = Box::into_raw(Box::new(value)) as *mut std::ffi::c_void;
    let ext = v8::External::new(&*scope, ptr);
    let weak = v8::Weak::with_guaranteed_finalizer(
        &mut *scope,
        ext,
        Box::new(move || unsafe {
            drop(Box::from_raw(ptr as *mut Rc<T>));
        }),
    );
    scope
        .get_slot::<V8RcStore>()
        .unwrap()
        .weaks
        .borrow_mut()
        .push(weak);
    ext
}

/// Extract an `Rc<T>` from a `v8::External` created by `rc_to_v8`. Clones
/// the Rc (bumps refcount) without taking ownership of the box — the weak
/// finalizer remains the sole owner.
pub(crate) fn rc_from_v8<T: 'static>(value: &v8::Value) -> Rc<T> {
    assert!(value.is_external(), "rc_from_v8: expected v8::External");
    let ext = unsafe { &*(value as *const v8::Value as *const v8::External) };
    let ptr = ext.value() as *const Rc<T>;
    unsafe { (*ptr).clone() }
}

/// EvalResBuilder
pub(crate) struct Erb {
    loc: QExprId,
    errors: Vec<ExprErr>,
}

impl Erb {
    fn new(loc: QExprId) -> Self {
        return Self {
            loc: loc,
            errors: Default::default(),
        };
    }

    fn maybe_er<T>(&mut self, r: EResult<T>) -> Option<T> {
        let Some(cv) = r.v else {
            return None;
        };
        self.errors.extend(r.errors);
        return Some(cv);
    }

    fn must_er<T>(&mut self, r: EResult<T>) -> Result<T, ()> {
        self.errors.extend(r.errors);
        let Some(cv) = r.v else {
            return Err(());
        };
        return Ok(cv);
    }

    fn maybe_res<T>(&mut self, r: Result<T, String>) -> Option<T> {
        match r {
            Ok(r) => {
                return Some(r);
            },
            Err(_) => {
                return None;
            },
        }
    }

    fn must_res<T>(&mut self, r: Result<T, String>) -> Result<T, ()> {
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

    fn err_new(&mut self, message: impl ToString) -> Result<CodeValue, ()> {
        self.errors.push(ExprErr {
            loc: self.loc,
            errors: vec![ExprSubErr::Opaque(message.to_string())],
        });
        return Err(());
    }

    fn err_push(&mut self, message: impl ToString) {
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
    ) -> EResult<(Code, Box<dyn ScopeEntry>)>;
    fn seq(&self, loc: QExprId, c: Vec<Code>) -> EResult<Code>;
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
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);

        // Check if this value has a JS representation
        let has_js_aspect = aspect_consume_js_get_maybe(xx, vtype.as_ref()).is_some();
        let has_js_code = !matches!(cv.code.code, Code_::None);
        if !has_js_aspect || !has_js_code {
            let can_cast = TypeMortarString::cast_from(&TypeMortarString, xx, loc, &cv).v.is_some();
            if !can_cast {
                // Static binding: no JS representation
                return EResult::ok((carry, Box::new(StaticScopeEntry { type_: vtype }) as Box<dyn ScopeEntry>));
            }
        }

        return er_seq!(loc, erb, (Code, Box < dyn ScopeEntry >), {
            let (vcode, vtype) = erb.must_er(er_one!{
                to_js = aspect_consume_js_get(xx, loc, vtype.as_ref()) => {
                    to_js.to_js(xx, loc, cv).map(|c| (c, vtype.clone()))
                },
                js = TypeMortarString:: cast_from(&TypeMortarString, xx, loc, &cv) => {
                    js.code.cast::<JsCode>().map(|x| (x.unwrap().clone(), Rc::new(TypeMortarString) as Rc<dyn Type>))
                },
            })?;
            let js_key = ScopeEntryId(xx.vx.next_scope_entry_id.next());
            let mut allcode = vec![];
            if let Some(carry) = erb.maybe_er(carry.cast::<JsCode>()).flatten() {
                allcode.push(carry.clone());
            }
            allcode.push(JsCode {
                loc: loc,
                type_: JsCodeType::Bind(JsCodeBind {
                    base: js_key,
                    value: Box::new(vcode),
                }),
            });
            return Ok((
                //. .
                Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc: loc,
                        type_: JsCodeType::Seq(JsCodeSeq { sub: allcode })
                    }))
                },
                Box::new(MortarScopeEntry {
                    js_key: js_key,
                    type_: vtype,
                }),
            ));
        });
    }

    fn seq(&self, loc: QExprId, c: Vec<Code>) -> EResult<Code> {
        let mut out = vec![];
        for c in c {
            match c.cast::<JsCode>().into_res() {
                Ok(v) => {
                    if let Some(v) = v {
                        out.push(v.clone());
                    }
                },
                Err(e) => {
                    return EResult {
                        v: None,
                        errors: e,
                    };
                },
            }
        }
        if out.is_empty() {
            return EResult::ok(Code { loc: loc, code: Code_::None });
        }
        return EResult::ok(Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(JsCode {
                loc: loc,
                type_: JsCodeType::Seq(JsCodeSeq { sub: out }),
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
            if let Some(aspect_field) = aspect_field_get_maybe(xx, base_type.as_ref()) {
                let v = erb.must_er(aspect_field.field(xx, expr.loc, base, field))?;
                return Ok(v);
            }
            // Fallback: check if mortar string type has this field via field_opt
            let mortar_string_field = aspect_field_get_maybe(xx, &TypeMortarString);
            if let Some(msf) = mortar_string_field {
                let field_key_type = field.value.type_(xx, expr.loc);
                if let Some(field_key_string) = aspect_string_get_maybe(xx, field_key_type.as_ref()) {
                    let key_name = field_key_string.get(field.value.as_ref());
                    if msf.field_opt(xx, &key_name).is_some() {
                        let converted = erb.must_er(TypeMortarString.cast_from(xx, expr.loc, &base))?;
                        let v = erb.must_er(msf.field(xx, expr.loc, converted, field))?;
                        return Ok(v);
                    }
                }
            }
            return erb.err_new(format!("Type ({}) is missing aspect AspectField", base_type.desc()));
        }),
        Expr_::Bind(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_half(xx, e.base.as_ref(), target),
                value = eval_half(xx, e.value.as_ref(), target),
            });
            let base_constkey = erb.must_res(extract_const_key(xx, expr.loc, base.value.as_ref()))?;
            let binding = erb.must_er(target.bind(xx, expr.loc, base.code, value))?;
            xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
            return Ok(CodeValue {
                code: binding.0,
                value: Box::new(ValueNull)
            });
        }),
        Expr_::Seq(e) => return er_seq_cv!(expr.loc, erb, {
            let mut code = vec![];
            let mut last_val = None;
            for expr in &e.seq {
                let r = erb.must_er(eval_half(xx, expr, target))?;
                code.push(r.code);
                last_val = Some(r.value);
            }
            return Ok(CodeValue {
                code: erb.must_er(target.seq(expr.loc, code))?,
                value: last_val.unwrap_or_else(|| Box::new(ValueNull))
            });
        }),
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
            let elements: Vec<EvalRes> = t.elements.iter()
                .map(|e| eval_half(xx, e, target))
                .collect();
            return EvalRes::ok(CodeValue {
                code: Code { loc: expr.loc, code: Code_::None },
                value: Box::new(ValueTuple { elements }),
            });
        },
        Expr_::Record(r) => return er_seq_cv!(expr.loc, erb, {
            let mut entries = Vec::new();
            for (k_expr, v_expr) in &r.entries {
                let k_cv = erb.must_er(eval_half(xx, k_expr, target))?;
                let k_str = erb.must_res(extract_const_key(xx, expr.loc, k_cv.value.as_ref()))?;
                let key = match k_str {
                    ConstKey::String(s) => s,
                };
                entries.push((key, eval_half(xx, v_expr, target)));
            }
            return Ok(CodeValue {
                code: Code { loc: expr.loc, code: Code_::None },
                value: Box::new(ValueRecord { entries }),
            });
        }),
    }
}

pub(crate) fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let source_type = source.value.type_(xx, loc);
        // Identity check: if same concrete type, pass through
        let rust_type_eq = (source_type.as_ref() as &dyn Any).type_id() == (dest_type as &dyn Any).type_id();
        if rust_type_eq {
            let ids_match = match (source_type.identity(), dest_type.identity()) {
                (Some(a), Some(b)) => a == b,
                (None, None) => true,
                _ => false,
            };
            if ids_match {
                return Ok(source);
            }
        }
        let dest;
        if let Some(dest1) = erb.maybe_er(source_type.cast_to(xx, loc, &source, dest_type)) {
            dest = dest1;
        } else if let Some(dest1) = erb.maybe_er(dest_type.cast_from(xx, loc, &source)) {
            dest = dest1;
        } else {
            return erb.err_new(format!("TODO incompatible types"));
        }
        return Ok(dest);
    });
}

pub(crate) fn scope_entry_id_js_key(e: ScopeEntryId) -> String {
    return format!("a{}", e.0);
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
        Expr_::Seq(e) => {
            let mut resolved = Vec::new();
            for sub in &e.seq {
                resolved.push(substitute_lowers(cix, import_state, sub)?);
            }
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Seq(ExprSeq { seq: resolved }),
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
                entries.push((
                    substitute_lowers(cix, import_state, k)?,
                    substitute_lowers(cix, import_state, e)?,
                ));
            }
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Record(expr::ExprRecord { entries }),
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
    for (key, eval_value, js_value) in [
        jswrap!(fn log(ex, js_scope, scope)(message: String) -> () {
            let timestamp_ms = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64;
            scope
                .get_slot::<V8LogStore>()
                .unwrap()
                .logs
                .borrow_mut()
                .push(crate::langhelp::LogEntry { timestamp_ms, message });
        }),
        jswrap!(fn define(ex, js_scope, scope)() -> DefineResult {
            DefineResult { ds: Rc::new(Gc::new(DefinitionSet::new())) }
        }),
    ] {
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
            let impl_expr = impl_expr.as_ref()
                .expect("Definition set function missing implementation");
            let impl_expr_ast = (impl_expr.as_ref() as &dyn Any)
                .downcast_ref::<Expr>()
                .expect("Definition set impl must be an Expr");

            // Resolve arg type and create scope entry if present
            let arg_scope_id = if func_data.arg_type_expr.borrow().is_some() {
                let arg_expr = func_data.arg_type_expr.borrow();
                let arg_expr_ast = (arg_expr.as_ref().unwrap().as_ref() as &dyn Any)
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
                let ret_expr_ast = (ret_expr.as_ref().unwrap().as_ref() as &dyn Any)
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
                child_scope.values.borrow_mut().insert(
                    ConstKey::String("arg".to_string()),
                    Box::new(MortarScopeEntry { js_key: arg_id, type_: arg_type.clone() }),
                );
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

            let impl_code = impl_cv.code.cast::<JsCode>().into_res()?
                .expect("Definition set function body must produce JS code")
                .clone();
            let scope_id = func_data.scope_entry_id.get()
                .expect("Definition set function not registered");
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
            // Resolve underlying type
            let underlying_type: Rc<dyn Type> = {
                let expr = dt_data.underlying_type_expr.borrow();
                let expr_ast = (expr.as_ref().unwrap().as_ref() as &dyn Any)
                    .downcast_ref::<expr::Expr>()
                    .expect("underlying type must be an Expr");
                let result = eval_half(&xx, expr_ast, &MortarTarget);
                if !result.errors.is_empty() {
                    return Err(result.errors);
                }
                result.v.unwrap().value.type_(&xx, root_loc)
            };

            let data_field = dt_data.data_field.clone();

            for method in dt_data.methods.borrow().iter() {
                let impl_expr = method.impl_expr.borrow();
                let impl_expr_ast = (impl_expr.as_ref().unwrap().as_ref() as &dyn Any)
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
                    let ret_expr_ast = (ret_expr.as_ref().unwrap().as_ref() as &dyn Any)
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
                    child_scope.values.borrow_mut().insert(
                        ConstKey::String("self".to_string()),
                        Box::new(MortarScopeEntry {
                            js_key: self_id,
                            type_: underlying_type.clone(),
                        }),
                    );
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

                let impl_code = impl_cv.code.cast::<JsCode>().into_res()?
                    .expect("Method body must produce JS code")
                    .clone();

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
                    let impl_expr_ast = (method_impl.impl_expr.as_ref() as &dyn Any)
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
                        child_scope.values.borrow_mut().insert(
                            ConstKey::String("self".to_string()),
                            Box::new(MortarScopeEntry {
                                js_key: self_id,
                                type_: underlying_type.clone(),
                            }),
                        );
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

                    let impl_code = impl_cv.code.cast::<JsCode>().into_res()?
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
        return Ok(EvalResult { value: fallback_value.expect("No JS and no fallback value"), logs });
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

    // Render data type method implementations as JS functions
    // Methods use `this` to access the wrapper object; self = this.<data_field>
    for method in &def_data_methods {
        let func_name = scope_entry_id_js_key(method.scope_entry_id);
        code1.push(root_loc, format!("function {}() {{", func_name));
        code1.push(root_loc, format!(
            "let {} = this.{};",
            scope_entry_id_js_key(method.self_scope_entry_id),
            method.data_field,
        ));
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
        let cell: Rc<expr::StagedExprCell> = value.clone()
            .downcast::<expr::StagedExprCell>()
            .expect("External must hold StagedExprCell");
        let ext = rc_to_v8(js_scope, cell);
        js_scope
            .get_current_context()
            .global(js_scope)
            .set(
                js_scope,
                v8::String::new(js_scope, &scope_entry_id_js_key(*id)).unwrap().cast(),
                ext.into(),
            );
    }

    code1.push(root_loc, "})()".to_string());
    let js_source = code1.lines.join("\n");

    let script = v8::Script::compile(
        js_scope,
        v8::String::new(js_scope, &js_source).unwrap(),
        None,
    ).unwrap();
    match script.run(js_scope) {
        Some(stage2) => {
            let value: Box<dyn Value> = if let Some(tuple_aspect) = aspect_tuple_get_maybe(&xx, value_type.as_ref()) {
                let elem_types = tuple_aspect.element_types();
                let array = v8::Local::<v8::Array>::try_from(stage2)
                    .expect("Expected JS array for tuple result");
                let mut elements = Vec::new();
                for (i, elem_type) in elem_types.iter().enumerate() {
                    let elem_v8 = array.get_index(js_scope, i as u32)
                        .expect("Array index out of bounds in tuple lift");
                    let elem_lift = aspect_lift_js_get(&xx, root_loc, elem_type.as_ref()).into_res()?;
                    let lifted = elem_lift.lift(&elem_v8);
                    elements.push(EvalRes::ok(CodeValue {
                        code: Code { loc: root_loc, code: Code_::None },
                        value: lifted,
                    }));
                }
                Box::new(ValueTuple { elements })
            } else {
                let value_from_js = aspect_lift_js_get(&xx, root_loc, value_type.as_ref()).into_res()?;
                value_from_js.lift(&stage2)
            };
            let logs = js_scope.get_slot::<V8LogStore>().unwrap().logs.take();
            Ok(EvalResult { value, logs })
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
    let out = format!("e{}", lines.next_expr_id());
    lines.push(code.loc, format!("let {};", out));
    match &code.type_ {
        JsCodeType::Seq(c) => {
            let mut last_var = out.clone();
            for x in &c.sub {
                last_var = render_code(lines, x);
            }
            lines.push(code.loc, format!("{} = {};", out, last_var));
        },
        JsCodeType::Value(c) => {
            lines.push(code.loc, format!("{} = {};", out, serde_json::to_string(c).unwrap()));
        },
        JsCodeType::BindingUse(c) => {
            lines.push(code.loc, format!("{} = {};", out, scope_entry_id_js_key(c.id)));
        },
        JsCodeType::Call(c) => {
            // Special case: method call preserves `this` binding
            if let JsCodeType::Field(f) = &c.func.type_ {
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
        JsCodeType::Field(js_code_field) => {
            let base = render_code(lines, &js_code_field.base);
            lines.push(code.loc, format!("{} = {}.{};", out, base, js_code_field.field));
        },
        JsCodeType::Bind(c) => {
            let val_var = render_code(lines, &c.value);
            lines.push(code.loc, format!("let {} = {};", scope_entry_id_js_key(c.base), val_var));
            lines.push(code.loc, format!("{} = null;", out));
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
        JsCodeType::Object(c) => {
            let mut fields_str = vec![];
            for (key, value) in &c.fields {
                let val_var = render_code(lines, value);
                fields_str.push(format!("{}: {}", key, val_var));
            }
            lines.push(code.loc, format!("{} = {{ {} }};", out, fields_str.join(", ")));
        },
        JsCodeType::Array(c) => {
            let mut elem_vars = vec![];
            for elem in &c.elements {
                elem_vars.push(render_code(lines, elem));
            }
            lines.push(code.loc, format!("{} = [{}];", out, elem_vars.join(", ")));
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
        Err(errors) => return CompileRes { logs: vec![], result: CompileResResult::Err(errors) },
    };
    let root_loc = root_expr.loc;
    let eval_result = match eval(&cix, import_state, &root_expr) {
        Ok(v) => v,
        Err(errors) => return CompileRes { logs: vec![], result: CompileResResult::Err(errors) },
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
        Err(e) => return CompileRes { logs, result: CompileResResult::Err(e) },
    };
    let mut artifacts = fx.artifacts.write().unwrap();
    let value = match value_serial.to_serial(&mut artifacts, eval_result.value).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes { logs, result: CompileResResult::Err(e) },
    };
    return CompileRes { logs, result: CompileResResult::Ok(value) };
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
