use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectField,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_lift_js_aspect_id,
            aspect_string_get,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeCall,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataDefinitionSet,
            MortarJsValueSource,
        },
        mortar_null::TypeMortarNull,
        typecheck_convert,
    },
};

// --- DefinitionSet data ---

pub(crate) struct DefinitionSetFunctionData {
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub arg_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub arg_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct DefinitionSet {
    pub functions: RefCell<Vec<DefinitionSetFunctionData>>,
    pub finished: Cell<bool>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSet {
    fn accept(&self, _visitor: &mut V) -> Result<(), ()> {
        Ok(())
    }
}

impl DefinitionSet {
    pub fn new() -> Self {
        DefinitionSet {
            functions: RefCell::new(Vec::new()),
            finished: Cell::new(false),
        }
    }

    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        let functions = self.functions.borrow();
        for (i, func_data) in functions.iter().enumerate() {
            if func_data.impl_expr.borrow().is_none() {
                return Err(format!("Definition set function {} is missing an implementation", i));
            }
        }
        self.finished.set(true);
        Ok(())
    }
}

/// A function within a definition set. Used both as the v8::External data
/// passed through JS and as the Type after lifting.
pub(crate) struct DefinitionSetFunction {
    pub set: Gc<DefinitionSet>,
    pub func_index: usize,
}

// --- v8 callbacks ---

pub(crate) fn define_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    _args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    let obj = v8::Object::new(scope);

    // Create DefinitionSet and store as v8::External via rc_to_v8
    let ds_ext = super::rc_to_v8(scope, Rc::new(Gc::new(DefinitionSet::new())));
    let ds_key = v8::String::new(scope, "_ds").unwrap();
    obj.set(scope, ds_key.into(), ds_ext.into());

    // new_func method
    let new_func_fn = v8::Function::builder(new_func_js_callback).build(scope).unwrap();
    let nf_key = v8::String::new(scope, "new_func").unwrap();
    obj.set(scope, nf_key.into(), new_func_fn.into());

    ret.set(obj.into());
}

fn new_func_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    let this = args.this();
    let ds_key = v8::String::new(scope, "_ds").unwrap();
    let ds: Rc<Gc<DefinitionSet>> = super::rc_from_v8(&*this.get(scope, ds_key.into()).unwrap());

    let idx = {
        let mut functions = ds.functions.borrow_mut();
        let idx = functions.len();
        functions.push(DefinitionSetFunctionData {
            impl_expr: RefCell::new(None),
            arg_type_expr: RefCell::new(None),
            ret_type_expr: RefCell::new(None),
            scope_entry_id: Cell::new(None),
            arg_scope_entry_id: Cell::new(None),
        });
        idx
    };
    let func_ref = Rc::new(DefinitionSetFunction { set: (*ds).clone(), func_index: idx });

    // Build result object with {func, impl}
    let result_obj = v8::Object::new(scope);

    // func: the function ref external
    let func_ext = super::rc_to_v8(scope, func_ref.clone());
    let func_key = v8::String::new(scope, "func").unwrap();
    result_obj.set(scope, func_key.into(), func_ext.into());

    // _func: internal reference for impl callback
    let internal_func_ext = super::rc_to_v8(scope, func_ref);
    let internal_func_key = v8::String::new(scope, "_func").unwrap();
    result_obj.set(scope, internal_func_key.into(), internal_func_ext.into());

    // impl: method that takes staged expr
    let impl_fn = v8::Function::builder(builder_impl_js_callback).build(scope).unwrap();
    let impl_key = v8::String::new(scope, "impl").unwrap();
    result_obj.set(scope, impl_key.into(), impl_fn.into());

    // arg: method that takes a staged type expr
    let arg_fn = v8::Function::builder(builder_arg_js_callback).build(scope).unwrap();
    let arg_key = v8::String::new(scope, "arg").unwrap();
    result_obj.set(scope, arg_key.into(), arg_fn.into());

    // ret: method that takes a staged type expr
    let ret_fn = v8::Function::builder(builder_ret_js_callback).build(scope).unwrap();
    let ret_key = v8::String::new(scope, "ret").unwrap();
    result_obj.set(scope, ret_key.into(), ret_fn.into());

    ret.set(result_obj.into());
}

fn builder_impl_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    // Get func_ref from this._func
    let this = args.this();
    let func_key = v8::String::new(scope, "_func").unwrap();
    let func_ref: Rc<DefinitionSetFunction> = super::rc_from_v8(&*this.get(scope, func_key.into()).unwrap());

    // args[0] is the staged expr (Rc<StagedExprCell>) — take-once via RefCell
    let staged_cell: Rc<super::expr::StagedExprCell> = super::rc_from_v8(&*args.get(0));
    let staged_value = staged_cell.value.borrow_mut().take()
        .expect("impl: staged expr already consumed (double use)");

    let functions = func_ref.set.functions.borrow();
    let func_data = &functions[func_ref.func_index];
    let mut impl_expr = func_data.impl_expr.borrow_mut();
    if impl_expr.is_some() {
        drop(impl_expr);
        drop(functions);
        let msg = v8::String::new(scope, "Function already has an implementation").unwrap();
        let exception = v8::Exception::error(scope, msg);
        scope.throw_exception(exception);
        return;
    }
    *impl_expr = Some(staged_value);

    ret.set(v8::null(scope).into());
}

fn builder_arg_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    let this = args.this();
    let func_key = v8::String::new(scope, "_func").unwrap();
    let func_ref: Rc<DefinitionSetFunction> = super::rc_from_v8(&*this.get(scope, func_key.into()).unwrap());

    let staged_cell: Rc<super::expr::StagedExprCell> = super::rc_from_v8(&*args.get(0));
    let staged_value = staged_cell.value.borrow_mut().take()
        .expect("arg: staged expr already consumed (double use)");

    let functions = func_ref.set.functions.borrow();
    let func_data = &functions[func_ref.func_index];
    let mut arg_type_expr = func_data.arg_type_expr.borrow_mut();
    if arg_type_expr.is_some() {
        drop(arg_type_expr);
        drop(functions);
        let msg = v8::String::new(scope, "Function already has an arg type").unwrap();
        let exception = v8::Exception::error(scope, msg);
        scope.throw_exception(exception);
        return;
    }
    *arg_type_expr = Some(staged_value);

    ret.set(v8::null(scope).into());
}

fn builder_ret_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    let this = args.this();
    let func_key = v8::String::new(scope, "_func").unwrap();
    let func_ref: Rc<DefinitionSetFunction> = super::rc_from_v8(&*this.get(scope, func_key.into()).unwrap());

    let staged_cell: Rc<super::expr::StagedExprCell> = super::rc_from_v8(&*args.get(0));
    let staged_value = staged_cell.value.borrow_mut().take()
        .expect("ret: staged expr already consumed (double use)");

    let functions = func_ref.set.functions.borrow();
    let func_data = &functions[func_ref.func_index];
    let mut ret_type_expr = func_data.ret_type_expr.borrow_mut();
    if ret_type_expr.is_some() {
        drop(ret_type_expr);
        drop(functions);
        let msg = v8::String::new(scope, "Function already has a ret type").unwrap();
        let exception = v8::Exception::error(scope, msg);
        scope.throw_exception(exception);
        return;
    }
    *ret_type_expr = Some(staged_value);

    ret.set(v8::null(scope).into());
}

// --- MortarTypeDefineResult: the type of the object returned by define() ---
// Stateless — the DefinitionSet is created in JS, not tracked during eval_half.

pub(crate) struct MortarTypeDefineResult;

impl Value for MortarTypeDefineResult {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefineResult)
    }
}

impl Type for MortarTypeDefineResult {
    fn desc(&self) -> String {
        format!("define_result")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefineResult) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefineResult) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefineResult) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert define_result"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to define_result"))
    }
}

impl Aspect for MortarTypeDefineResult { }

impl AspectConsumeJs for MortarTypeDefineResult {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefineResult {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefineResult),
            }),
        }
    }
}

impl AspectField for MortarTypeDefineResult {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type: Rc<dyn Type> = match key_name.as_str() {
                "new_func" => Rc::new(super::mortar_function::MortarTypeFunction {
                    arg: Rc::new(super::tuple::TypeTuple { types: vec![] }),
                    ret: Rc::new(MortarTypeNewFuncResult),
                }),
                _ => return erb.err_new(format!("Unknown field '{}' on define result", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::Field(crate::jscode::JsCodeField {
                            base: Box::new(self_code),
                            field: key_name,
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: field_type,
                }),
            });
        })
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "new_func" => Some(Rc::new(super::mortar_function::MortarTypeFunction {
                arg: Rc::new(super::tuple::TypeTuple { types: vec![] }),
                ret: Rc::new(MortarTypeNewFuncResult),
            })),
            _ => None,
        }
    }
}

// --- MortarTypeDefinitionSetFunction: the eval_half type of a function ref ---
// Stateless during eval_half. After lift, becomes a MortarJsValue with
// DefinitionSetFunction type and DefinitionSet source.

pub(crate) struct MortarTypeDefinitionSetFunction;

impl Value for MortarTypeDefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetFunction)
    }
}

impl Type for MortarTypeDefinitionSetFunction {
    fn desc(&self) -> String {
        format!("definition_set_function")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for MortarTypeDefinitionSetFunction { }

impl AspectConsumeJs for MortarTypeDefinitionSetFunction {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetFunction {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetFunction),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetFunction {
    fn lift(&self, value: &v8::Value) -> Box<dyn Value> {
        let func_ref: Rc<DefinitionSetFunction> = super::rc_from_v8(value);
        func_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::DefinitionSet(MortarJsValueDataDefinitionSet {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
            type_: Rc::new(DefinitionSetFunction {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
        })
    }
}

// --- MortarTypeNewFuncResult: the return type of new_func (object with func + builder fields) ---

pub(crate) struct MortarTypeNewFuncResult;

impl Value for MortarTypeNewFuncResult {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeNewFuncResult)
    }
}

impl Type for MortarTypeNewFuncResult {
    fn desc(&self) -> String {
        format!("new_func_result")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeNewFuncResult) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeNewFuncResult) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeNewFuncResult) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert new_func_result"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to new_func_result"))
    }
}

impl Aspect for MortarTypeNewFuncResult { }

impl AspectConsumeJs for MortarTypeNewFuncResult {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeNewFuncResult {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeNewFuncResult),
            }),
        }
    }
}

impl AspectField for MortarTypeNewFuncResult {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type: Rc<dyn Type> = match key_name.as_str() {
                "func" => Rc::new(MortarTypeDefinitionSetFunction),
                "impl" | "arg" | "ret" => Rc::new(super::mortar_function::MortarTypeFunction {
                    arg: Rc::new(super::tuple::TypeTuple {
                        types: vec![
                            Rc::new(super::expr::ExprType),
                        ],
                    }),
                    ret: Rc::new(TypeMortarNull),
                }),
                _ => return erb.err_new(format!("Unknown field '{}' on new_func_result", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::Field(crate::jscode::JsCodeField {
                            base: Box::new(self_code),
                            field: key_name,
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: field_type,
                }),
            });
        })
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "func" => Some(Rc::new(MortarTypeDefinitionSetFunction)),
            "impl" | "arg" | "ret" => Some(Rc::new(super::mortar_function::MortarTypeFunction {
                arg: Rc::new(super::tuple::TypeTuple {
                    types: vec![
                        Rc::new(super::expr::ExprType),
                    ],
                }),
                ret: Rc::new(TypeMortarNull),
            })),
            _ => None,
        }
    }
}

// --- DefinitionSetFunction as Type: used after lift ---

impl Value for DefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(DefinitionSetFunction {
            set: self.set.clone(),
            func_index: self.func_index,
        })
    }
}

impl Type for DefinitionSetFunction {
    fn desc(&self) -> String {
        format!("definition_set_function")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetFunction {
                set: self.set.clone(),
                func_index: self.func_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for DefinitionSetFunction { }

impl AspectFunction for DefinitionSetFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Ensure scope entry IDs are allocated
        {
            let functions = self.set.functions.borrow();
            for func_data in functions.iter() {
                if func_data.scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    func_data.scope_entry_id.set(Some(id));
                }
            }
        }

        // Register with eval context (dedup)
        {
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already = sets.iter().any(|s| std::ptr::eq(
                &**s as *const DefinitionSet,
                &*self.set as *const DefinitionSet,
            ));
            if !already {
                sets.push(self.set.clone());
            }
        }

        // Clone arg/ret type exprs (to release borrow before eval)
        let (scope_id, arg_type_expr, ret_type_expr) = {
            let functions = self.set.functions.borrow();
            let fd = &functions[self.func_index];
            let sid = fd.scope_entry_id.get()
                .expect("Definition set function not registered");
            let arg_e = fd.arg_type_expr.borrow().as_ref().map(|v| {
                (v.as_ref() as &dyn Any)
                    .downcast_ref::<super::expr::Expr>()
                    .expect("arg type must be an Expr")
                    .clone()
            });
            let ret_e = fd.ret_type_expr.borrow().as_ref().map(|v| {
                (v.as_ref() as &dyn Any)
                    .downcast_ref::<super::expr::Expr>()
                    .expect("ret type must be an Expr")
                    .clone()
            });
            (sid, arg_e, ret_e)
        };

        // Resolve arg/ret types by evaluating the type exprs
        return er_seq_cv!(loc, erb, {
            let arg_type: Option<Rc<dyn Type>> = match &arg_type_expr {
                Some(expr) => {
                    let r = super::eval_half_mortar(xx, expr);
                    let cv = erb.must_er(r)?;
                    Some(cv.value.type_(xx, loc))
                },
                None => None,
            };
            let ret_type: Option<Rc<dyn Type>> = match &ret_type_expr {
                Some(expr) => {
                    let r = super::eval_half_mortar(xx, expr);
                    let cv = erb.must_er(r)?;
                    Some(cv.value.type_(xx, loc))
                },
                None => None,
            };

            // Handle argument
            let mut js_args = vec![];
            if let Some(arg_type) = &arg_type {
                let arg_value_type = argument.value.type_(xx, loc);
                if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                    let elements = arg_tuple.take_elements(argument.value);
                    if elements.len() != 1 {
                        return erb.err_new(
                            format!("Function expects 1 argument, got {}", elements.len())
                        );
                    }
                    let elem = elements.into_iter().next().unwrap();
                    let cv = match elem.v {
                        Some(cv) => cv,
                        None => {
                            erb.errors.extend(elem.errors);
                            return erb.err_new(format!("Argument failed to evaluate"));
                        },
                    };
                    erb.errors.extend(elem.errors);
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), cv))?;
                    let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                    js_args.push(js);
                } else {
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), argument))?;
                    let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                    js_args.push(js);
                }
            }

            let ret_type_rc: Rc<dyn Type> = ret_type.unwrap_or_else(|| Rc::new(TypeMortarNull));
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, ret_type_rc.as_ref()))?;

            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::Call(JsCodeCall {
                    func: Box::new(JsCode {
                        loc,
                        type_: JsCodeType::BindingUse(JsCodeBindingUse { id: scope_id }),
                    }),
                    args: js_args,
                }),
            }));
        });
    }
}
