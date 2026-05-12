pub mod jscode;
pub mod langhelp;
pub mod syntax;
pub mod builtin;

use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectFunction,
            AspectJsStackValue,
            AspectJsString,
            AspectLiftJs,
            AspectNull,
            AspectSerial,
            AspectSet,
            AspectString,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_field_get,
            aspect_function_aspect_id,
            aspect_function_get,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_js_string_aspect_id,
            aspect_js_string_get,
            aspect_lift_js_aspect_id,
            aspect_lift_js_get,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_serial_aspect_id,
            aspect_serial_get,
            aspect_set_get,
            aspect_string_aspect_id,
            aspect_string_get,
            aspect_string_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBindingSet,
            JsCodeBindingUse,
            JsCodeCall,
            JsCodeField,
            JsCodeFieldSet,
            JsCodeSeq,
            JsCodeType,
        },
        langhelp::{
            Artifacts,
            Aspect,
            Code,
            Code_,
            CodeDyn,
            CodeValue,
            CompileContext,
            CompileId,
            CompileRes,
            ConstKey,
            EResult,
            EvalContext,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            FullCompileContext,
            ImportStateId,
            QExprId,
            Scope,
            ScopeEntry,
            ScopeEntryId,
            SerialValue as LangSerialValue,
            SerialValueSource,
            SourceId,
            Type,
            Unique,
            Value,
        },
        syntax::{
            ExprId,
            SerialExpr,
            SerialExprBind,
            SerialExprCall,
            SerialExprScopeUse,
            SerialExprSequence,
            SerialExprType,
            SerialModule,
            SerialValue,
            SourceSpecAbsolute,
        },
    },
    flowcontrol::{
        shed,
        ta_return,
    },
    paste::paste,
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
        process::exit,
        rc::Rc,
        sync::Arc,
    },
    tokio::fs::read,
    v8::{
        Platform,
        tc_scope,
    },
};

/// EvalResBuilder
struct Erb {
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

macro_rules! er_seq{
    ($loc: expr, $v: ident, $t: ty, $($x: tt) *) => {
        {
            let mut $v = Erb:: new($loc);
            match(||-> Result < $t, () > {
                $($x) *
            })() {
                Ok(v) => EResult {
                    v: Some(v),
                    errors: mem:: take(& mut $v.errors),
                },
                Err(()) => EResult {
                    v: None,
                    errors: mem:: take(& mut $v.errors),
                },
            }
        }
    };
}

/// Returns final value as EvalRes, or first error EvalRes.
macro_rules! er_seq_cv{
    ($loc: expr, $v: ident, $($x: tt) *) => {
        er_seq!($loc, $v, CodeValue, $($x) *)
    };
}

/// Unpacks all EvalRes.
macro_rules! er_all{
    ($v: ident, {
        $($i: ident = $e: expr,) *
    }) => {
        $(let $i = $v.must_er($e);) * 
        //. .
        $(let $i = $i ?;) *
    };
}

macro_rules! r_all{
    ($v: ident, {
        $($i: ident = $e: expr,) *
    }) => {
        $(let $i = $v.must_res($e);) * 
        //. .
        $(let $i = $i ?;) *
    };
}

/// Returns the first non-error EvalRes or an error EvalRes containing all failures.
macro_rules! er_one{
    ($($i: ident = $e: expr => $b: expr,) *) => {
        'done: {
            let mut maybe_errors = vec![];
            $('next: {
                let $i = $e;
                if $i.v.is_none() {
                    maybe_errors.extend($i.errors);
                    break 'next;
                }
                let errors = $i.errors;
                let $i = $i.v.unwrap();
                let mut out = $b;
                out.errors.extend(errors);
                break 'done out;
            }) * 
            //. .
            break 'done EResult {
                v: None,
                errors: maybe_errors,
            };
        }
    };
}

macro_rules! r_one{
    ($loc: expr, {
        $($i: ident = $e: expr => $b: expr,) *
    }) => {
        'done: {
            let mut errors = vec![];
            $('next: {
                let $i = match $e {
                    Ok(v) => v,
                    Err(e) => {
                        errors.push(ExprErr {
                            loc: $loc,
                            errors: vec ![ExprSubErr::Opaque(e.to_string())]
                        });
                        break 'next;
                    }
                };
                break 'done($b);
            }) * 
            //. .
            break 'done EvalRes {
                v: None,
                errors: errors,
            };
        }
    };
}

macro_rules! erb_async{
    ($loc: expr, $v: ident $x: tt) => {
        let mut $v = Erb:: new($loc);
        match async {
            ta_return!(EvalRes, EvalRes);
            $x
        }
        .await {
            Ok(er) => return er,
            Err(er) => return er,
        }
    };
}

pub struct ExprSequence {
    pub exprs: Vec<Expr>,
}

pub struct ExprBind {
    pub base: Box<Expr>,
    pub value: Box<Expr>,
}

pub struct ExprSet {
    pub base: Box<Expr>,
    pub value: Box<Expr>,
}

pub struct ExprField {
    pub base: Box<Expr>,
    pub key: Box<Expr>,
}

pub struct ExprSeq {
    pub seq: Vec<Expr>,
}

pub struct ExprScopeUse {
    pub key: Box<Expr>,
}

pub struct ExprCall {
    pub func: Box<Expr>,
    pub arg: Box<Expr>,
}

type LitValue = Box<dyn Fn() -> Box<dyn Value>>;

pub enum ExprType {
    Literal(LitValue),
    Bind(ExprBind),
    Set(ExprSet),
    Field(ExprField),
    Seq(ExprSeq),
    ScopeUse(ExprScopeUse),
    Call(ExprCall),
}

pub struct Expr {
    pub loc: QExprId,
    pub r#type: ExprType,
}

pub enum Module {
    V1(Expr),
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
            let vcode = erb.must_er(er_one!{
                to_js = aspect_consume_js_get(xx, loc, vtype.as_ref()) => {
                    to_js.to_js(loc, cv)
                },
                js = TypeMortarString:: cast_from(&TypeMortarString, xx, loc, &cv) => {
                    js.code.cast::<JsCode>().map(|x| x.unwrap().clone())
                },
            })?;
            let mut allcode = vec![];
            if let Some(carry) = erb.maybe_er(carry.cast::<JsCode>()).flatten() {
                allcode.push(carry.clone());
            }
            allcode.push(vcode);
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
                    js_key: ScopeEntryId(xx.vx.next_scope_entry_id.next()),
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

fn eval_expr<T: Target>(xx: &ExprContext, expr: &Expr, target: &T) -> EvalRes {
    match &expr.r#type {
        ExprType::Literal(p) => {
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
        ExprType::ScopeUse(p) => return er_seq_cv!(expr.loc, erb, {
            let key = erb.must_er(eval_expr(xx, p.key.as_ref(), target))?;
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
        ExprType::Call(p) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                func = eval_expr(xx, p.func.as_ref(), target),
                arg = eval_expr(xx, p.arg.as_ref(), target),
            });
            let func_type = func.value.type_(xx, expr.loc);
            let aspect_func = erb.must_er(aspect_function_get(xx, expr.loc, func_type.as_ref()))?;
            let v = erb.must_er(aspect_func.call(xx, expr.loc, func, arg))?;
            return Ok(v);
        }),
        ExprType::Set(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_expr(xx, e.base.as_ref(), target),
                value = eval_expr(xx, e.value.as_ref(), target),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let base_set = erb.must_er(aspect_set_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(base_set.set(xx, expr.loc, base, value))?;
            return Ok(v);
        }),
        ExprType::Field(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_expr(xx, e.base.as_ref(), target),
                field = eval_expr(xx, e.key.as_ref(), target),
            });
            let base_type = base.value.type_(xx, expr.loc);
            let aspect_field = erb.must_er(aspect_field_get(xx, expr.loc, base_type.as_ref()))?;
            let v = erb.must_er(aspect_field.field(xx, expr.loc, base, field))?;
            return Ok(v);
        }),
        ExprType::Bind(e) => return er_seq_cv!(expr.loc, erb, {
            er_all!(erb, {
                base = eval_expr(xx, e.base.as_ref(), target),
                value = eval_expr(xx, e.base.as_ref(), target),
            });
            let base_constkey = erb.must_res(extract_const_key(xx, expr.loc, base.value.as_ref()))?;
            let binding = erb.must_er(target.bind(xx, expr.loc, base.code, value))?;
            xx.scope.values.borrow_mut().insert(base_constkey, binding.1);
            return Ok(CodeValue {
                code: binding.0,
                value: Box::new(ValueNull)
            });
        }),
        ExprType::Seq(e) => return er_seq_cv!(expr.loc, erb, {
            let mut code = vec![];
            let mut last_val = None;
            for expr in &e.seq {
                let r = erb.must_er(eval_expr(xx, expr, target))?;
                code.push(r.code);
                last_val = Some(r.value);
            }
            return Ok(CodeValue {
                code: erb.must_er(target.seq(expr.loc, code))?,
                value: last_val.unwrap_or_else(|| Box::new(ValueNull))
            });
        }),
    }
}
struct TypeNull;

impl Value for TypeNull {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}

impl Type for TypeNull {
    fn desc(&self) -> String {
        return format!("null");
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectNull>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value: _value } = cv;
            erb.must_er(aspect_null_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNull)
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            erb.must_er(aspect_null_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueNull)
            });
        });
    }
}

impl Aspect for TypeNull { }

impl AspectNull for TypeNull { }
struct ValueNull;

impl Value for ValueNull {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}
struct TypeString;

impl Value for TypeString {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        unimplemented!();
    }
}

impl Type for TypeString {
    fn desc(&self) -> String {
        return format!("string");
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_string_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectString>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let CodeValue { code, value } = cv;
            let type_ = value.type_(xx, loc);
            let aspect_string = erb.must_er(aspect_string_get(xx, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: code.clone(),
                value: Box::new(ValueString(aspect_string.get(value.as_ref())))
            });
        });
    }
}

impl Aspect for TypeString { }

impl AspectString for TypeString {
    fn get(&self, value: &dyn Value) -> String {
        return <dyn Any>::downcast_ref::<ValueString>(value).unwrap().0.clone();
    }
}

struct ValueString(pub String);

impl Value for ValueString {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeString);
    }
}

pub struct MortarJsValueDataScope {
    pub loc: QExprId,
    pub key: ScopeEntryId,
}

pub struct MortarJsValueDataAccess {
    /// Location of where access occurred
    pub loc: QExprId,
    pub base: Box<MortarJsValue>,
    pub key: String,
}

pub enum MortarJsValueSource {
    Binding(MortarJsValueDataScope),
    // Result of effect
    Expr,
    Field(MortarJsValueDataAccess),
}

pub struct MortarJsValue {
    pub source: MortarJsValueSource,
    pub type_: Rc<dyn Type>,
}

impl Value for MortarJsValue {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}

pub struct MortarScopeEntry {
    pub js_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for MortarScopeEntry {
    fn use_(&self, loc: QExprId, code: Code) -> CodeValue {
        return CodeValue {
            code: code,
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Binding(MortarJsValueDataScope {
                    loc: loc,
                    key: self.js_key,
                }),
                type_: self.type_.clone(),
            }),
        };
    }
}
struct TypeMortarString;

impl Value for TypeMortarString {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}

impl Type for TypeMortarString {
    fn desc(&self) -> String {
        return format!("mortar string");
    }

    fn aspect(&self, ex: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_js_string_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectJsString>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectConsumeJs>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            erb.must_er(aspect_js_string_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(TypeMortarString),
                })
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            return Ok(erb.must_er(er_one!{
                aspect_string = aspect_string_get(xx, loc, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value } = cv;
                    let mut seq = vec![];
                    if let Some(c) = erb.must_er(code.cast::<JsCode>())? {
                        seq.push(c.clone());
                    };
                    seq.push(JsCode {
                        loc: loc,
                        type_: JsCodeType::Value(serde_json::Value::String(aspect_string.get(value.as_ref())))
                    });
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::Seq(JsCodeSeq { sub: seq })
                            }))
                        },
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarString),
                        }),
                    });
                }),
                _x = aspect_js_string_get(xx, loc, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value: _value } = cv;
                    return Ok(CodeValue {
                        code: code.clone(),
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarString),
                        })
                    });
                }),
            })?);
        });
    }
}

impl Aspect for TypeMortarString { }

impl AspectJsString for TypeMortarString { }

impl AspectConsumeJs for TypeMortarString {
    fn to_js(&self, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let r = cv.code.cast::<JsCode>();
        if let Some(c) = r.v {
            if let Some(c) = c {
                return EResult {
                    v: Some(c.clone()),
                    errors: r.errors,
                };
            } else {
                let mut errors = r.errors;
                errors.push(ExprErr {
                    loc: loc,
                    errors: vec![ExprSubErr::Opaque(format!("Expression resulted in empty code"))],
                });
                return EResult {
                    v: None,
                    errors: errors,
                }
            }
        } else {
            return EResult {
                v: None,
                errors: r.errors,
            };
        }
    }
}

impl AspectSet for TypeMortarString {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let value_type = value.value.type_(xx, loc);
            er_all!(erb, {
                value_to_js = aspect_consume_js_get(xx, loc, value_type.as_ref()),
                self_code = self_.code.cast::<JsCode>(),
            });
            let value_code = erb.must_er(value_to_js.to_js(loc, value))?;
            let v = (self_.value.as_ref() as &dyn std::any::Any).downcast_ref::<MortarJsValue>().unwrap();
            match &v.source {
                MortarJsValueSource::Field(b) => {
                    let Some(self_code) = self_code else {
                        return erb.err_new(
                            format!("TODO assertion set left hand doesn't evaluate to js code (empty)")
                        );
                    };
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::FieldSet(JsCodeFieldSet {
                                    base: Box::new(self_code.clone()),
                                    key: b.key.clone(),
                                    value: Box::new(value_code)
                                })
                            }))
                        },
                        value: Box::new(ValueNull),
                    })
                },
                MortarJsValueSource::Expr => {
                    return erb.err_new(format!("Cannot modify a temporary value"));
                },
                MortarJsValueSource::Binding(b) => {
                    let mut seq = vec![];
                    if let Some(self_code) = self_code {
                        seq.push(self_code.clone());
                    }
                    seq.push(JsCode {
                        loc: loc,
                        type_: JsCodeType::BindingSet(JsCodeBindingSet {
                            base: b.key,
                            value: Box::new(value_code),
                        })
                    });
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::Seq(JsCodeSeq { sub: seq })
                            }))
                        },
                        value: Box::new(ValueNull),
                    })
                },
            }
        });
    }
}
struct TypeMortarNull;

impl Value for TypeMortarNull {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeMortarNull);
    }
}

impl Type for TypeMortarNull {
    fn desc(&self) -> String {
        return format!("mortar null");
    }

    fn aspect(&self, ex: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_serial_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectSerial>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        unimplemented!();
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        unimplemented!();
    }
}

impl Aspect for TypeMortarNull { }

impl AspectJsStackValue for TypeMortarNull {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNull),
            }),
        }
    }
}

impl AspectSerial for TypeMortarNull {
    fn to_serial(&self, _artifacts: &mut Artifacts, _value: Box<dyn Value>) -> EResult<LangSerialValue> {
        EResult::ok(LangSerialValue::Null)
    }
}

impl AspectLiftJs for TypeMortarNull {
    fn lift(&self, _value: &v8::Value) -> Box<dyn Value> {
        Box::new(ValueNull)
    }
}

#[derive(Clone)]
struct MortarTypeFunction {
    arg: Rc<dyn Type>,
    ret: Rc<dyn Type>,
}

impl Value for MortarTypeFunction {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}

impl Type for MortarTypeFunction {
    fn desc(&self) -> String {
        return format!("mortar func");
    }

    fn aspect(&self, ex: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        unimplemented!();
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        unimplemented!();
    }
}

fn typecheck_convert(xx: &ExprContext, loc: QExprId, dest_type: &dyn Type, source: CodeValue) -> EvalRes {
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

impl Aspect for MortarTypeFunction { }

impl AspectFunction for MortarTypeFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, function: CodeValue, argument: CodeValue) -> langhelp::EvalRes {
        return er_seq_cv!(loc, erb, {
            let func_type = function.value.type_(xx, loc);
            er_all!(erb, {
                func_to_js = aspect_consume_js_get(xx, loc, func_type.as_ref()),
                arg_to_js = aspect_consume_js_get(xx, loc, self.arg.as_ref()),
                ret_stack = aspect_js_stack_value_get(xx, loc, self.ret.as_ref()),
                argument = typecheck_convert(xx, loc, self.arg.as_ref(), argument),
            });
            er_all!(erb, {
                func = func_to_js.to_js(loc, function),
                arg = arg_to_js.to_js(loc, argument),
            });
            return Ok(ret_stack.to_value(loc, JsCode {
                loc: loc,
                type_: JsCodeType::Call(JsCodeCall {
                    func: Box::new(func),
                    args: vec![arg],
                })
            }));
        });
    }
}

fn scope_entry_id_js_key(e: ScopeEntryId) -> String {
    return format!("a{}", e.0);
}

async fn compile(
    fx: &Arc<FullCompileContext>,
    import_state: ImportStateId,
    source: Module,
    compile_path: Vec<(CompileId, ExprId)>,
) -> CompileRes {
    let cmx = Rc::new(CompileContext {
        cx: fx.clone(),
        compile_path: compile_path,
        next_unique_id: Default::default(),
    });

    // Prep scope
    let mut s1_scope = HashMap::new();
    let mut js_scope_isolate = v8::Isolate::new(Default::default());
    let js_scope_handle = std::pin::pin!(v8::HandleScope::new(&mut js_scope_isolate));
    let mut js_scope_handle2 = js_scope_handle.init();
    let js_scope_context = v8::Context::new(&mut js_scope_handle2, Default::default());
    let js_scope_context2 = &mut v8::ContextScope::new(&mut js_scope_handle2, js_scope_context);
    tc_scope!(let js_scope, js_scope_context2);
    let ex = Rc::new(EvalContext {
        ix: cmx.clone(),
        next_scope_entry_id: Default::default(),
    });
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
    let root_loc;
    let root_xx = Rc::new(ExprContext {
        vx: ex.clone(),
        import_state: Rc::new(Cell::new(import_state)),
        scope: Rc::new(Scope {
            values: RefCell::new(s1_scope),
            parent: None,
        }),
    });
    let stage1 = match source {
        Module::V1(root_expr) => {
            root_loc = root_expr.loc;
            eval_expr(
                //. .
                &root_xx,
                &root_expr,
                &MortarTarget,
            )
        },
    };
    if !stage1.errors.is_empty() {
        return CompileRes::Err(stage1.errors);
    }
    let stage1_cv = stage1.v.unwrap();
    let value_type = stage1_cv.value.type_(root_xx.as_ref(), root_loc);
    let value_serial = match aspect_serial_get(&root_xx, root_loc, value_type.as_ref()).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    let value_from_js = match aspect_lift_js_get(&root_xx, root_loc, value_type.as_ref()).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    let stage1_code = match stage1_cv.code.cast::<JsCode>().into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    let value = match stage1_code {
        None => {
            stage1_cv.value
        },
        Some(code) => {
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

            let mut code1 = JsSourceBuilder::default();
            let root_var = render_code(&mut code1, &code);
            code1.push(root_loc, root_var);
            if let Some(stage2) =
                v8::Script::compile(js_scope, v8::String::new(js_scope, &code1.lines.join("\n")).unwrap(), None)
                    .unwrap()
                    .run(js_scope) {
                value_from_js.lift(&stage2)
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
                        return CompileRes::Err(vec![ExprErr {
                            loc: *code1.mapping.get(&js_expr).unwrap(),
                            errors: vec![ExprSubErr::Opaque(text)]
                        }]);
                    },
                    None => {
                        return CompileRes::Err(vec![ExprErr {
                            loc: root_loc,
                            errors: vec![ExprSubErr::Opaque(text)]
                        }]);
                    },
                }
            }
        },
    };
    let mut artifacts = fx.artifacts.write().unwrap();
    let value = match value_serial.to_serial(&mut artifacts, value).into_res() {
        Ok(v) => v,
        Err(e) => return CompileRes::Err(e),
    };
    return CompileRes::Ok(value);
}

#[tokio::main(flavor = "multi_thread")]
async fn main() {
    let platform = v8::new_default_platform(0, false).make_shared();
    v8::V8::initialize_platform(platform);
    v8::V8::initialize();
    let mut builtin_unique = {
        let mut next = 0;
        move || {
            let v = next;
            next += 1;
            return Unique(SerialValueSource::Builtin, v);
        }
    };
    let gx = Arc::new(FullCompileContext {
        artifacts: Default::default(),
        cache_root: {
            let fs_cwd = current_dir().unwrap();
            let fs_cwd = match fs_cwd.to_str() {
                Some(c) => c,
                None => {
                    panic!("Non-string current path [{:?}], required for cache path", fs_cwd);
                },
            };
            let mut out = vec![];
            for c in fs_cwd.split("/") {
                out.push(c.to_string());
            }
            out
        },
        compile_results: Default::default(),
        const_aspect_mortar_func: builtin_unique(),
        const_aspect_unique: builtin_unique(),
        const_aspect_func: builtin_unique(),
        const_aspect_artifact: builtin_unique(),
        const_aspect_mortar_from_js: builtin_unique(),
        const_aspect_mortar_null: builtin_unique(),
        const_aspect_mortar_str: builtin_unique(),
    });

    struct ModuleBuilder {
        next_phrase_id: Cell<usize>,
    }

    impl ModuleBuilder {
        fn id(&self) -> ExprId {
            let id = self.next_phrase_id.get();
            self.next_phrase_id.set(id + 1);
            return ExprId(id);
        }

        fn lit_str(&self, s: &str) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExprType::Literal(SerialValue::String(s.to_string())),
            }
        }

        fn scope_use(&self, key: &str) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExprType::ScopeUse(SerialExprScopeUse { key: Box::new(SerialExpr {
                    id: self.id(),
                    r#type: SerialExprType::Literal(SerialValue::String(key.to_string())),
                }) }),
            };
        }

        fn call(&self, func: SerialExpr, args: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExprType::Call(SerialExprCall {
                    func: Box::new(func),
                    args: Box::new(args),
                }),
            };
        }

        fn seq(&self, exprs: Vec<SerialExpr>) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExprType::Sequence(SerialExprSequence { exprs: exprs }),
            };
        }

        fn bind(&self, key: &str, expr: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExprType::Bind(SerialExprBind {
                    base: key.to_string(),
                    value: Box::new(expr),
                }),
            };
        }
    }

    fn serial_module_to_module(source_id: SourceId, m: &SerialModule) -> Module {
        fn serial_expr_to_expr(source_id: SourceId, m: &SerialExpr) -> Expr {
            return Expr {
                loc: (source_id, m.id),
                r#type: match &m.r#type {
                    SerialExprType::Literal(lit) => ExprType::Literal(match lit {
                        SerialValue::Null => Box::new(|| Box::new(ValueNull)),
                        SerialValue::String(l) => {
                            let l = l.clone();
                            Box::new(move || Box::new(ValueString(l.clone())))
                        },
                    }),
                    SerialExprType::ScopeUse(expr) => ExprType::ScopeUse(
                        ExprScopeUse { key: Box::new(serial_expr_to_expr(source_id, &expr.key)) },
                    ),
                    SerialExprType::Call(expr) => ExprType::Call(ExprCall {
                        func: Box::new(serial_expr_to_expr(source_id, &expr.func)),
                        arg: Box::new(serial_expr_to_expr(source_id, &expr.args)),
                    }),
                    SerialExprType::Set(expr) => ExprType::Set(ExprSet {
                        base: Box::new(serial_expr_to_expr(source_id, &expr.base)),
                        value: Box::new(serial_expr_to_expr(source_id, &expr.value)),
                    }),
                    SerialExprType::Field(expr) => ExprType::Field(ExprField {
                        base: Box::new(serial_expr_to_expr(source_id, &expr.base)),
                        key: Box::new(serial_expr_to_expr(source_id, &expr.key)),
                    }),
                    SerialExprType::Sequence(expr) => ExprType::Seq(
                        ExprSeq { seq: expr.exprs.iter().map(|e| serial_expr_to_expr(source_id, e)).collect() },
                    ),
                    SerialExprType::Bind(expr) => ExprType::Bind(ExprBind {
                        base: Box::new(Expr {
                            loc: (source_id, m.id),
                            r#type: ExprType::Literal({
                                let base = expr.base.clone();
                                Box::new(move || Box::new(ValueString(base.clone())))
                            }),
                        }),
                        value: Box::new(serial_expr_to_expr(source_id, &expr.value)),
                    }),
                },
            };
        }

        match m {
            SerialModule::V1(m) => Module::V1(serial_expr_to_expr(source_id, m)),
        }
    }

    let mb = ModuleBuilder { next_phrase_id: Cell::new(0) };
    let source_id = SourceId(0);
    let serial_module = SerialModule::V1(
        //. ex1
        //. mb.call(mb.scope_use("print"), mb.lit_str("Hello world (mortar)"))
        mb.seq(
            vec![
                mb.bind("hello", mb.lit_str("Hello world (mortar)")),
                mb.call(mb.scope_use("print"), mb.scope_use("hello"))
            ],
        ),
    );
    eprintln!("code {:?}", serial_module);
    let module = serial_module_to_module(source_id, &serial_module);
    let mut source_names = HashMap::new();
    source_names.insert(source_id, "(virtual)".to_string());
    let res = compile(&gx, ImportStateId(0), module, vec![]).await;

    fn assemble_errors(out: &mut HashMap<String, HashMap<String, Vec<String>>>, r: &CompileRes) {
        let CompileRes::Err(errors) = r else {
            return;
        };
        for eval_err in errors {
            let out =
                out
                    .entry(eval_err.loc.0.0.to_string())
                    .or_default()
                    .entry(eval_err.loc.1.0.to_string())
                    .or_default();
            for err in &eval_err.errors {
                out.push(match err {
                    ExprSubErr::Opaque(err) => err.clone(),
                });
            }
        }
    }

    let mut errors = HashMap::new();
    for (_, r) in gx.compile_results.lock().unwrap().iter() {
        let Ok(r) = r.clone().await else {
            // Root module errors are handled by importer
            continue;
        };
        assemble_errors(&mut errors, &r);
    }
    assemble_errors(&mut errors, &res);
    if !errors.is_empty() {
        eprintln!("{}", serde_json::to_string_pretty(&errors).unwrap());
        exit(1);
    }
}
