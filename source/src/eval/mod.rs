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

use {
    crate::{
        builtin::{
            aspect_consume_js_get,
            aspect_field_get,
            aspect_field_get_maybe,
            aspect_function_get,
            aspect_lift_js_get,
            aspect_serial_get,
            aspect_set_get,
            aspect_string_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBind,
            JsCodeSeq,
            JsCodeType,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileId,
            CompileImportContext,
            CompileRes,
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
    v8::tc_scope,
};

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
        return er_seq!(loc, erb, (Code, Box < dyn ScopeEntry >), {
            let vtype = cv.value.type_(xx, loc);
            let (vcode, vtype) = erb.must_er(er_one!{
                to_js = aspect_consume_js_get(xx, loc, vtype.as_ref()) => {
                    to_js.to_js(loc, cv).map(|c| (c, vtype.clone()))
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
            return erb.err_new(format!("TODO incompatible types"));
        }
        return Ok(dest);
    });
}

pub(crate) fn scope_entry_id_js_key(e: ScopeEntryId) -> String {
    return format!("a{}", e.0);
}

fn substitute_lowers(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<Expr, Vec<ExprErr>> {
    match &expr.r#type {
        Expr_::Lower(e) => {
            let resolved_inner = substitute_lowers(cix, import_state, &e.expr)?;
            let value = eval(cix, import_state, &resolved_inner)?;
            let value_cell = Rc::new(RefCell::new(Some(value)));
            Ok(Expr {
                loc: expr.loc,
                r#type: Expr_::Literal(Rc::new(move || {
                    value_cell.borrow_mut().take().expect("Lower literal consumed twice")
                })),
            })
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
    }
}

fn eval(
    cix: &Rc<CompileImportContext>,
    import_state: ImportStateId,
    expr: &Expr,
) -> Result<Box<dyn Value>, Vec<ExprErr>> {
    let mut js_scope_isolate = v8::Isolate::new(Default::default());
    let js_scope_handle = std::pin::pin!(v8::HandleScope::new(&mut js_scope_isolate));
    let mut js_scope_handle2 = js_scope_handle.init();
    let js_scope_context = v8::Context::new(&mut js_scope_handle2, Default::default());
    let js_scope_context2 = &mut v8::ContextScope::new(&mut js_scope_handle2, js_scope_context);
    tc_scope!(let js_scope, js_scope_context2);
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_scope_entry_id: Default::default(),
    });
    let mut s1_scope = HashMap::new();
    for (key, eval_value, js_value) in [(
        "print",
        MortarScopeEntry {
            js_key: ScopeEntryId(ex.next_scope_entry_id.next()),
            type_: Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarString),
                ret: Rc::new(TypeMortarNull),
            }),
        },
        v8::Function::builder(
            |
                scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
                args: v8::FunctionCallbackArguments<'_>,
                _ret: v8::ReturnValue<'_>,
            | {
                println!("{}", args.get(0).to_string(scope).unwrap().to_rust_string_lossy(scope));
            },
        ).build(js_scope).unwrap().cast::<v8::Value>(),
    )] {
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
    let stage1_code = cv.code.cast::<JsCode>().into_res()?;
    match stage1_code {
        None => Ok(cv.value),
        Some(code) => {
            let value_from_js = aspect_lift_js_get(&xx, root_loc, value_type.as_ref()).into_res()?;
            let mut code1 = JsSourceBuilder::default();
            let root_var = render_code(&mut code1, &code);
            code1.push(root_loc, root_var);
            if let Some(stage2) =
                v8::Script::compile(js_scope, v8::String::new(js_scope, &code1.lines.join("\n")).unwrap(), None)
                    .unwrap()
                    .run(js_scope) {
                Ok(value_from_js.lift(&stage2))
            } else {
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
                    Some(js_expr) => {
                        Err(vec![ExprErr {
                            loc: *code1.mapping.get(&js_expr).unwrap(),
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
            let func_var = render_code(lines, &c.func);
            let mut arg_vars = vec![];
            for arg in &c.args {
                arg_vars.push(render_code(lines, arg));
            }
            lines.push(code.loc, format!("{} = ({})({});", out, func_var, arg_vars.join(",")));
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
        Err(errors) => return CompileRes::Err(errors),
    };
    let root_loc = root_expr.loc;
    let value = match eval(&cix, import_state, &root_expr) {
        Ok(v) => v,
        Err(errors) => return CompileRes::Err(errors),
    };

    // Serialize result
    let ex = Rc::new(EvalContext {
        ix: cix.clone(),
        next_scope_entry_id: Default::default(),
    });
    let xx = Rc::new(ExprContext {
        vx: ex,
        import_state: Rc::new(Cell::new(import_state)),
        scope: Rc::new(Scope {
            values: RefCell::new(HashMap::new()),
            parent: None,
        }),
    });
    let value_type = value.type_(&xx, root_loc);
    let value_serial = match aspect_serial_get(&xx, root_loc, value_type.as_ref()).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    let mut artifacts = fx.artifacts.write().unwrap();
    let value = match value_serial.to_serial(&mut artifacts, value).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    return CompileRes::Ok(value);
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
