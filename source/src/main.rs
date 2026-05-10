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
            AspectLiftJs,
            AspectNull,
            AspectSerial,
            AspectString,
            AspectJsString,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
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
            aspect_string_aspect_id,
            aspect_string_get,
        },
        jscode::{
            JsCode,
            JsCodeCall,
            JsCodeSeq,
            JsCodeType,
            ScopeEntryId,
        },
        langhelp::{
            Artifacts,
            Aspect,
            Code,
            CodeValue,
            CompileContext,
            CompileId,
            CompileRes,
            EvalContext,
            EvalErr,
            EvalRes,
            ExprContext,
            ExprErr,
            FullCompileContext,
            ImportStateId,
            QExprId,
            Scope,
            ScopeEntry,
            SerialValueSource,
            SourceId,
            Type,
            Unique,
            Value,
            SerialValue as LangSerialValue,
        },
        syntax::{
            ExprId,
            ScopeKey,
            SerialExpr,
            SerialExprCall,
            SerialExprScopeUse,
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
        cell::Cell,
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
    errors: Vec<EvalErr>,
}

impl Erb {
    fn new(loc: QExprId) -> Self {
        return Self {
            loc: loc,
            errors: Default::default(),
        };
    }

    fn maybe_er(&mut self, r: EvalRes) -> Option<CodeValue> {
        let Some(cv) = r.cv else {
            return None;
        };
        self.errors.extend(r.errors);
        return Some(cv);
    }

    fn one_of_er(&mut self, r: EvalRes) -> Option<CodeValue> {
        self.errors.extend(r.errors);
        let Some(cv) = r.cv else {
            return None;
        };
        return Some(cv);
    }

    fn must_er(&mut self, r: EvalRes) -> Result<CodeValue, EvalRes> {
        self.errors.extend(r.errors);
        let Some(cv) = r.cv else {
            return Err(self.err().ok().unwrap());
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

    fn one_of_res<T>(&mut self, r: Result<T, String>) -> Option<T> {
        match r {
            Ok(r) => {
                return Some(r);
            },
            Err(e) => {
                self.err_new(e);
                return None;
            },
        }
    }

    fn must_res<T>(&mut self, r: Result<T, String>) -> Result<T, EvalRes> {
        match r {
            Ok(r) => {
                return Ok(r);
            },
            Err(e) => {
                return Err(self.err_new(e).ok().unwrap());
            },
        }
    }

    fn err(&mut self) -> Result<EvalRes, EvalRes> {
        return Ok(EvalRes {
            cv: None,
            errors: mem::take(&mut self.errors),
        });
    }

    fn err_new(&mut self, message: impl ToString) -> Result<EvalRes, EvalRes> {
        self.errors.push(EvalErr {
            expr: self.loc,
            errors: vec![ExprErr::Opaque(message.to_string())],
        });
        return self.err();
    }

    fn value(&mut self, cv: CodeValue) -> Result<EvalRes, EvalRes> {
        return Ok(EvalRes {
            cv: Some(cv),
            errors: mem::take(&mut self.errors),
        });
    }
}

macro_rules! erb{
    ($loc: expr, $v: ident $x: tt) => {
        let mut $v = Erb:: new($loc);
        match(||-> Result < EvalRes, EvalRes > {
            $x
        })() {
            Ok(er) => return er,
            Err(er) => return er,
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

pub struct ExprScopeSet {
    pub id: ScopeKey,
    pub value: Box<Expr>,
}

pub struct ExprScopeUse {
    pub key: ScopeKey,
}

pub struct ExprCall {
    pub func: Box<Expr>,
    pub arg: Box<Expr>,
}

pub enum ExprType {
    Literal(Box<dyn Value>),
    //.    Sequence(ExprSequence),
    //.    ScopeSet(ExprScopeSet),
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

fn cerr<T>(e: String) -> Result<T, ExprErr> {
    return Err(ExprErr::Opaque(e));
}

trait Effect { }

trait Target {
    type Effect;

    /// Value is guaranteed to be of the type described by the aspect's type.
    ///
    /// Used before interacting with JS: namely, function call arguments.
    fn place(&self, xx: &ExprContext, loc: QExprId, v: &dyn Value) -> EvalRes;
}
struct MortarTarget;

impl Target for MortarTarget {
    type Effect = JsCode;

    fn place(&self, xx: &ExprContext, loc: QExprId, v: &dyn Value) -> EvalRes {
        erb!(loc, erb {
            let v_type = v.type_(xx, loc);
            if let Some(_) = erb.one_of_res(aspect_null_get(xx, v_type.as_ref())) {
                return erb.value(CodeValue {
                    code: Code::None,
                    value: Box::new(ValueNull)
                });
            } else if let Some(aspect) = erb.one_of_res(aspect_string_get(xx, v_type.as_ref())) {
                return erb.value(CodeValue {
                    code: Code::None,
                    value: Box::new(ValueString(aspect.get(v)))
                });
            } else {
                return erb.err();
            }
        });
    }
}

fn eval_expr<T: Target>(xx: &ExprContext, phrase: &Expr, target: &T) -> EvalRes {
    erb!(phrase.loc, erb {
        match &phrase.r#type {
            ExprType::Literal(p) => {
                let v = erb.must_er(target.place(xx, phrase.loc, p.as_ref()))?;
                return erb.value(v);
            },
            ExprType::ScopeUse(p) => {
                let binding = shed!{
                    'found _;
                    let mut at = &xx.scope;
                    loop {
                        if let Some(found) = at.values.get(&p.key) {
                            break 'found found;
                        }
                        if let Some(parent) = &at.parent {
                            at = parent;
                        } else {
                            break;
                        }
                    }
                    return erb.err_new(format!("No bound value with id [{}]", p.key.0));
                };
                return erb.value(binding.use_());
            },
            ExprType::Call(p) => {
                let func = erb.one_of_er(eval_expr(xx, p.func.as_ref(), target));
                let arg = erb.one_of_er(eval_expr(xx, p.arg.as_ref(), target));
                let (Some(func), Some(arg)) = (func, arg) else {
                    return erb.err();
                };
                let func_type = func.value.type_(xx, phrase.loc);
                let aspect_func = erb.must_res(aspect_function_get(xx, func_type.as_ref()))?;
                let v = erb.must_er(aspect_func.call(xx, phrase.loc, func, arg))?;
                return erb.value(v);
            },
        }
    });
}
struct TypeNull;

impl Value for TypeNull {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeNull);
    }
}

impl Type for TypeNull {
    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeNull) as Box<dyn AspectNull>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        erb!(loc, erb {
            erb.must_res(aspect_null_get(xx, type_))?;
            return erb.value(CodeValue {
                code: cv.code.clone(),
                value: Box::new(ValueNull)
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        erb!(loc, erb {
            let type_ = cv.value.type_(xx, loc);
            erb.must_res(aspect_null_get(xx, type_.as_ref()))?;
            return erb.value(CodeValue {
                code: cv.code.clone(),
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
    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_string_aspect_id() {
            return Some(Box::new(Box::new(TypeString) as Box<dyn AspectString>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        erb!(loc, erb {
            let aspect_string = erb.must_res(aspect_string_get(xx, type_))?;
            return erb.value(CodeValue {
                code: cv.code.clone(),
                value: Box::new(ValueString(aspect_string.get(cv.value.as_ref())))
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        erb!(loc, erb {
            let type_ = cv.value.type_(xx, loc);
            let aspect_string = erb.must_res(aspect_string_get(xx, type_.as_ref()))?;
            return erb.value(CodeValue {
                code: cv.code.clone(),
                value: Box::new(ValueString(aspect_string.get(cv.value.as_ref())))
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

fn serial_value_to_expr_lit(lit: &SerialValue) -> Box<dyn Value> {
    return match lit {
        SerialValue::Null => Box::new(ValueNull),
        SerialValue::String(l) => Box::new(ValueString(l.clone())),
    //.        SerialValue::Number(l) => Value::Number(*l),
    //.        SerialValue::Bool(l) => Value::Bool(*l),
    //.        SerialValue::Array(l) => Value::Array(l.iter().map(serial_value_to_value).collect()),
    //.        SerialValue::Set(l) => Value::Set(l.iter().map(serial_value_to_value).collect()),
    //.        SerialValue::Map(l) => Value::Map(
    //.            l.iter().map(|(k, v)| (serial_value_to_value(k), serial_value_to_value(v))).collect(),
    //.        ),
    };
}

fn serial_expr_to_expr(source_id: SourceId, m: &SerialExpr) -> Expr {
    return Expr {
        loc: (source_id, m.id),
        r#type: match &m.r#type {
            SerialExprType::Literal(lit) => ExprType::Literal(serial_value_to_expr_lit(lit)),
            //.            SerialExprType::Sequence(expr) => ExprType::Sequence(
            //.                ExprSequence { exprs: expr.exprs.iter().map(serial_expr_to_expr).collect() },
            //.            ),
            //.            SerialExprType::Set(expr) => ExprType::ScopeSet(ExprScopeSet {
            //.                id: expr.id.clone(),
            //.                value: Box::new(serial_expr_to_expr(&expr.value)),
            //.            }),
            SerialExprType::ScopeUse(expr) => ExprType::ScopeUse(ExprScopeUse { key: expr.key.clone() }),
            SerialExprType::Call(expr) => ExprType::Call(ExprCall {
                func: Box::new(serial_expr_to_expr(source_id, &expr.func)),
                arg: Box::new(serial_expr_to_expr(source_id, &expr.args)),
            }),
        },
    };
}

fn serial_module_to_module(source_id: SourceId, m: &SerialModule) -> Module {
    match m {
        SerialModule::V1(m) => Module::V1(serial_expr_to_expr(source_id, m)),
    }
}

pub enum MortarJsValueData {
    Scope(ScopeEntryId),
    // Result of effect
    Expr,
    Access(Box<MortarJsValue>, String),
}

pub struct MortarJsValue {
    pub data: MortarJsValueData,
    pub type_: Rc<dyn Type>,
}

impl Value for MortarJsValue {
    //.     fn drop(&self, xx: &ExprContext, loc: QExprId) -> Code {
    //.         let mut root = self;
    //.         loop {
    //.             match &root.data {
    //.                 MortarJsValueData::Scope(d) => {
    //.                     // No load actually happened
    //.                     return Code::None;
    //.                 },
    //.                 MortarJsValueData::Expr(d) => {
    //.                     if let Some(aspect_drop) = root.type_.aspect(xx, &xx.vx.ix.cx.const_aspect_drop) { }
    //.                 },
    //.                 MortarJsValueData::Access(d, _) => {
    //.                     root = d.as_ref();
    //.                 },
    //.             }
    //.         }
    //.     }
    //. 
    //.     fn consume(&self, ex: &ExprContext, loc: QExprId) -> Code {
    //.         return Code::None;
    //.     }
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}

pub struct MortarScopeEntry {
    pub js_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for MortarScopeEntry {
    fn use_(&self) -> CodeValue {
        return CodeValue {
            code: Code::None,
            value: Box::new(MortarJsValue {
                data: MortarJsValueData::Scope(self.js_key.clone()),
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
        erb!(loc, erb {
            erb.must_res(aspect_js_string_get(xx, type_))?;
            return erb.value(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    data: MortarJsValueData::Expr,
                    type_: Rc::new(TypeMortarString),
                })
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        erb!(loc, erb {
            let type_ = cv.value.type_(xx, loc);
            let cv2: CodeValue;
            if let Some(aspect_string) = erb.one_of_res(aspect_string_get(xx, type_.as_ref())) {
                let mut seq = vec![];
                if let Some(c) = erb.must_res(cv.code.cast::<JsCode>())? {
                    seq.push(c.clone());
                };
                seq.push(JsCode {
                    loc: loc,
                    type_: JsCodeType::Value(serde_json::Value::String(aspect_string.get(cv.value.as_ref())))
                });
                cv2 = CodeValue {
                    code: Code::Dyn(Rc::new(JsCode {
                        loc: loc,
                        type_: JsCodeType::Seq(JsCodeSeq { sub: seq })
                    })),
                    value: Box::new(MortarJsValue {
                        data: MortarJsValueData::Expr,
                        type_: Rc::new(TypeMortarString),
                    }),
                };
            } else if let Some(_) = erb.one_of_res(aspect_js_string_get(xx, type_.as_ref())) {
                cv2 = CodeValue {
                    code: cv.code.clone(),
                    value: Box::new(MortarJsValue {
                        data: MortarJsValueData::Expr,
                        type_: Rc::new(TypeMortarString),
                    })
                };
            } else {
                return erb.err();
            }
            return erb.value(cv2);
        });
    }
}

impl Aspect for TypeMortarString { }

impl AspectJsString for TypeMortarString { }

impl AspectConsumeJs for TypeMortarString {
    fn consume(&self, cv: CodeValue) -> JsCode {
        match cv.code {
            Code::Dyn(rc) => (rc.as_ref() as &dyn std::any::Any).downcast_ref::<JsCode>().unwrap().clone(),
            Code::None => panic!("TypeMortarString consume: expected code"),
        }
    }
}
struct TypeMortarNull;

impl Value for TypeMortarNull {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(TypeMortarNull);
    }
}

impl Type for TypeMortarNull {
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
    fn to_value(&self, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code::Dyn(Rc::new(value)),
            value: Box::new(MortarJsValue {
                data: MortarJsValueData::Expr,
                type_: Rc::new(TypeMortarNull),
            }),
        }
    }
}

impl AspectSerial for TypeMortarNull {
    fn to_serial(&self, _artifacts: &mut Artifacts, _value: Box<dyn Value>) -> LangSerialValue {
        LangSerialValue::Null
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
    fn aspect(&self, ex: &ExprContext, key: &Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeJs>));
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
    erb!(loc, erb {
        let source_type = source.value.type_(xx, loc);
        let dest;
        if let Some(dest1) = erb.maybe_er(source_type.cast_to(xx, loc, &source, dest_type)) {
            dest = dest1;
        } else if let Some(dest1) = erb.maybe_er(dest_type.cast_from(xx, loc, &source)) {
            dest = dest1;
        } else {
            erb.err_new(format!("TODO incompatible types"));
            return erb.err();
        }
        return erb.value(dest);
    });
}

impl Aspect for MortarTypeFunction { }

impl AspectConsumeJs for MortarTypeFunction {
    fn consume(&self, cv: CodeValue) -> JsCode {
        let v = (cv.value.as_ref() as &dyn std::any::Any).downcast_ref::<MortarJsValue>().unwrap();
        let dummy_loc = (SourceId(0), ExprId(0));
        match &v.data {
            MortarJsValueData::Scope(id) => JsCode {
                loc: dummy_loc,
                type_: JsCodeType::Get(jscode::JsCodeGet { id: *id }),
            },
            MortarJsValueData::Expr => match cv.code {
                Code::Dyn(rc) => (rc.as_ref() as &dyn std::any::Any).downcast_ref::<JsCode>().unwrap().clone(),
                Code::None => panic!("MortarTypeFunction consume Expr: expected code"),
            },
            MortarJsValueData::Access(_, _) => todo!(),
        }
    }
}

impl AspectFunction for MortarTypeFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, function: CodeValue, argument: CodeValue) -> langhelp::EvalRes {
        erb!(loc, erb {
            let func_type = function.value.type_(xx, loc);
            let func_to_js = erb.maybe_res(aspect_consume_js_get(xx, func_type.as_ref()));
            let arg_to_js = erb.maybe_res(aspect_consume_js_get(xx, self.arg.as_ref()));
            let ret_stack = erb.maybe_res(aspect_js_stack_value_get(xx, self.ret.as_ref()));
            let argument = erb.must_er(typecheck_convert(xx, loc, self.arg.as_ref(), argument))?;
            let (Some(func_to_js), Some(arg_to_js), Some(ret_stack)) = (func_to_js, arg_to_js, ret_stack) else {
                return erb.err();
            };
            let func = func_to_js.consume(function);
            let arg = arg_to_js.consume(argument);
            return erb.value(ret_stack.to_value(JsCode {
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
    for (id, eval_value, js_value) in [(
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
        s1_scope.insert(ScopeKey(id.to_string()), Box::new(eval_value) as Box<dyn ScopeEntry>);
    }
    let root_loc;
    let root_xx = Rc::new(ExprContext {
        vx: ex.clone(),
        import_state: Rc::new(Cell::new(import_state)),
        scope: Rc::new(Scope {
            values: s1_scope,
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
    let stage1_cv = stage1.cv.unwrap();
    let value_type = stage1_cv.value.type_(root_xx.as_ref(), root_loc);
    let value_serial = match aspect_serial_get(&root_xx, value_type.as_ref()) {
        Ok(v) => v,
        Err(e) => {
            return CompileRes::Err(vec![EvalErr {
                expr: root_loc,
                errors: vec![ExprErr::Opaque(format!("Returned value type is missing artifact aspect: {}", e))]
            }]);
        },
    };
    let value_from_js = match aspect_lift_js_get(&root_xx, value_type.as_ref()) {
        Ok(v) => v,
        Err(e) => {
            return CompileRes::Err(vec![EvalErr {
                expr: root_loc,
                errors: vec![ExprErr::Opaque(format!("Returned value type is missing from-js aspect: {}", e))]
            }]);
        },
    };
    let value = if let Code::Dyn(code) = stage1_cv.code {
        let code = (code.as_ref() as &dyn std::any::Any).downcast_ref::<JsCode>().unwrap();

        #[derive(Default)]
        struct JsSourceBuilder {
            lines: Vec<String>,
            mapping: HashMap<usize, QExprId>,
        }

        impl JsSourceBuilder {
            fn push(&mut self, id: QExprId, line: String) {
                let js_num = self.lines.len() + 1;
                self.mapping.insert(js_num, id);
                self.lines.push(line);
            }
        }

        fn render_code(lines: &mut JsSourceBuilder, code: &JsCode) {
            match &code.type_ {
                JsCodeType::Seq(c) => {
                    for x in &c.sub {
                        render_code(lines, x);
                    }
                },
                JsCodeType::Value(c) => {
                    lines.push(code.loc, serde_json::to_string(&c).unwrap());
                },
                JsCodeType::Set(c) => {
                    lines.push(code.loc, format!("v{} = ", c.id.0));
                    render_code(lines, &c.value);
                },
                JsCodeType::Get(c) => {
                    lines.push(code.loc, scope_entry_id_js_key(c.id));
                },
                JsCodeType::Call(c) => {
                    lines.push(code.loc, format!("("));
                    lines.push(code.loc, format!("("));
                    render_code(lines, &c.func);
                    lines.push(code.loc, format!(")"));
                    lines.push(code.loc, format!("("));
                    for arg in &c.args {
                        render_code(lines, arg);
                        lines.push(code.loc, format!(","));
                    }
                    lines.push(code.loc, format!(")"));
                    lines.push(code.loc, format!(")"));
                },
            }
        }

        let mut code1 = JsSourceBuilder::default();
        render_code(&mut code1, &code);
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
                    return CompileRes::Err(vec![EvalErr {
                        expr: *code1.mapping.get(&js_expr).unwrap(),
                        errors: vec![ExprErr::Opaque(text)]
                    }]);
                },
                None => {
                    return CompileRes::Err(vec![EvalErr {
                        expr: root_loc,
                        errors: vec![ExprErr::Opaque(text)]
                    }]);
                },
            }
        }
    } else {
        stage1_cv.value
    };
    let mut artifacts = fx.artifacts.write().unwrap();
    return CompileRes::Ok(value_serial.to_serial(&mut artifacts, value));
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
                r#type: SerialExprType::ScopeUse(SerialExprScopeUse { key: ScopeKey(key.to_string()) }),
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
    }

    let mb = ModuleBuilder { next_phrase_id: Cell::new(0) };
    let source_id = SourceId(0);
    let module =
        serial_module_to_module(
            source_id,
            &SerialModule::V1(mb.call(mb.scope_use("print"), mb.lit_str("Hello world (mortar)"))),
        );
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
                    .entry(eval_err.expr.0.0.to_string())
                    .or_default()
                    .entry(eval_err.expr.1.0.to_string())
                    .or_default();
            for err in &eval_err.errors {
                out.push(match err {
                    ExprErr::Opaque(err) => err.clone(),
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
