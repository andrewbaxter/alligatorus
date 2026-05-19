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
            JsCodeField,
            JsCodeObject,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            Counter,
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
        sync::atomic::{AtomicUsize, Ordering},
    },
    super::{
        Erb,
        jswrap::{
            JsWrap,
            StagedExpr,
            jswrap_method_name,
        },
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataDefinitionSet,
            MortarJsValueSource,
        },
        mortar_null::TypeMortarNull,
        rc_to_v8,
        rc_from_v8,
        tuple::TypeTuple,
        typecheck_convert,
    },
};

static NEXT_DATA_TYPE_IDENTITY: AtomicUsize = AtomicUsize::new(0);

// --- DefinitionSet data ---

pub(crate) struct DefinitionSetFunctionData {
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub arg_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub arg_scope_entry_id: Cell<Option<ScopeEntryId>>,
}

pub(crate) struct DefinitionSetDataMethodData {
    pub name: String,
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub ret_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub self_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub field_name: String,
}

pub(crate) struct DefinitionSetDataTypeData {
    pub underlying_type_expr: RefCell<Option<Box<dyn Value>>>,
    pub methods: RefCell<Vec<DefinitionSetDataMethodData>>,
    pub identity: usize,
    pub data_field: String,
    pub unique_field: String,
}

pub(crate) struct DefinitionSet {
    pub functions: RefCell<Vec<DefinitionSetFunctionData>>,
    pub data_types: RefCell<Vec<DefinitionSetDataTypeData>>,
    pub finished: Cell<bool>,
    pub next_field_id: Counter,
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
            data_types: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: Counter::default(),
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
        let data_types = self.data_types.borrow();
        for (i, dt_data) in data_types.iter().enumerate() {
            if dt_data.underlying_type_expr.borrow().is_none() {
                return Err(format!("Data type {} is missing underlying type", i));
            }
            for (j, method) in dt_data.methods.borrow().iter().enumerate() {
                if method.impl_expr.borrow().is_none() {
                    return Err(format!("Data type {} method {} is missing implementation", i, j));
                }
            }
        }
        self.finished.set(true);
        Ok(())
    }
}

pub(crate) struct DefinitionSetFunction {
    pub set: Gc<DefinitionSet>,
    pub func_index: usize,
}

// --- JsWrap structs ---

jswrap!(struct DefineResult(self_, scope) {
    data { ds: Rc<Gc<DefinitionSet>> }
    fn new_func() -> NewFuncResult {
        let ds = self_.ds.clone();
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
        let func_ref = Rc::new(DefinitionSetFunction {
            set: (*ds).clone(),
            func_index: idx,
        });
        NewFuncResult { func_ref }
    }
    fn new_data() -> NewDataResult {
        let ds = self_.ds.clone();
        let identity = NEXT_DATA_TYPE_IDENTITY.fetch_add(1, Ordering::Relaxed);
        let unique_field = format!("_{}", ds.next_field_id.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let idx = {
            let mut data_types = ds.data_types.borrow_mut();
            let idx = data_types.len();
            data_types.push(DefinitionSetDataTypeData {
                underlying_type_expr: RefCell::new(None),
                methods: RefCell::new(Vec::new()),
                identity,
                data_field,
                unique_field,
            });
            idx
        };
        let data_type_ref = Rc::new(DefinitionSetDataTypeRef {
            set: (*ds).clone(),
            data_type_index: idx,
        });
        NewDataResult { data_type_ref }
    }
});

jswrap!(struct NewFuncResult(self_, scope) {
    data { func_ref: Rc<DefinitionSetFunction> }
    field func: Rc<DefinitionSetFunction> { self_.func_ref.clone() }
    fn impl_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut impl_expr = func_data.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has an implementation").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn arg_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut arg_type_expr = func_data.arg_type_expr.borrow_mut();
        if arg_type_expr.is_some() {
            drop(arg_type_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has an arg type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *arg_type_expr = Some(staged.0);
    }
    fn ret_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut ret_type_expr = func_data.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(functions);
            let msg = v8::String::new(scope, "Function already has a ret type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- Data type JsWrap structs ---

pub(crate) struct DefinitionSetDataTypeRef {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}

pub(crate) struct DefinitionSetDataTypeConstructor {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}

jswrap!(struct NewDataResult(self_, scope) {
    data { data_type_ref: Rc<DefinitionSetDataTypeRef> }
    field type_: Rc<DefinitionSetDataTypeRef> { self_.data_type_ref.clone() }
    field new_: Rc<DefinitionSetDataTypeConstructor> {
        Rc::new(DefinitionSetDataTypeConstructor {
            set: self_.data_type_ref.set.clone(),
            data_type_index: self_.data_type_ref.data_type_index,
        })
    }
    fn set_underlying(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let mut underlying = dt.underlying_type_expr.borrow_mut();
        if underlying.is_some() {
            drop(underlying);
            drop(data_types);
            let msg = v8::String::new(scope, "Data type already has an underlying type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *underlying = Some(staged.0);
    }
    fn new_func(name: String) -> NewDataMethodResult {
        let ds_ref = &self_.data_type_ref;
        let field_name = format!("_{}", ds_ref.set.next_field_id.next());
        let method_index = {
            let data_types = ds_ref.set.data_types.borrow();
            let dt = &data_types[ds_ref.data_type_index];
            let mut methods = dt.methods.borrow_mut();
            let idx = methods.len();
            methods.push(DefinitionSetDataMethodData {
                name,
                impl_expr: RefCell::new(None),
                ret_type_expr: RefCell::new(None),
                scope_entry_id: Cell::new(None),
                self_scope_entry_id: Cell::new(None),
                field_name,
            });
            idx
        };
        NewDataMethodResult {
            data_type_ref: self_.data_type_ref.clone(),
            method_index,
        }
    }
});

jswrap!(struct NewDataMethodResult(self_, scope) {
    data { data_type_ref: Rc<DefinitionSetDataTypeRef>, method_index: usize }
    fn impl_(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let methods = dt.methods.borrow();
        let method = &methods[self_.method_index];
        let mut impl_expr = method.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(methods);
            drop(data_types);
            let msg = v8::String::new(scope, "Method already has an implementation").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn ret_(staged: StagedExpr) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let methods = dt.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type_expr = method.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(methods);
            drop(data_types);
            let msg = v8::String::new(scope, "Method already has a ret type").unwrap();
            let exception = v8::Exception::error(scope, msg);
            scope.throw_exception(exception);
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- JsWrap for Rc<DefinitionSetDataTypeRef> ---

impl JsWrap for Rc<DefinitionSetDataTypeRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetDataTypeRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetDataTypeConstructor> ---

impl JsWrap for Rc<DefinitionSetDataTypeConstructor> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetDataTypeConstructor)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        rc_to_v8(scope, self).into()
    }
}

// --- MortarTypeDefinitionSetDataTypeRef: eval_half type during JS execution ---

pub(crate) struct MortarTypeDefinitionSetDataTypeRef;

impl Value for MortarTypeDefinitionSetDataTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetDataTypeRef)
    }
}

impl Type for MortarTypeDefinitionSetDataTypeRef {
    fn desc(&self) -> String {
        format!("definition_set_data_type_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_ref"))
    }
}

impl Aspect for MortarTypeDefinitionSetDataTypeRef { }

impl AspectConsumeJs for MortarTypeDefinitionSetDataTypeRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeRef {
    fn lift(&self, value: &v8::Value) -> Box<dyn Value> {
        let type_ref: Rc<DefinitionSetDataTypeRef> = rc_from_v8(value);
        type_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataType {
                set: type_ref.set.clone(),
                data_type_index: type_ref.data_type_index,
            }),
        })
    }
}

// --- MortarTypeDefinitionSetDataTypeConstructor: eval_half type during JS execution ---

pub(crate) struct MortarTypeDefinitionSetDataTypeConstructor;

impl Value for MortarTypeDefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetDataTypeConstructor)
    }
}

impl Type for MortarTypeDefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for MortarTypeDefinitionSetDataTypeConstructor { }

impl AspectConsumeJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeConstructor),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn lift(&self, value: &v8::Value) -> Box<dyn Value> {
        let ctor_ref: Rc<DefinitionSetDataTypeConstructor> = rc_from_v8(value);
        ctor_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataTypeConstructor {
                set: ctor_ref.set.clone(),
                data_type_index: ctor_ref.data_type_index,
            }),
        })
    }
}

// --- DefinitionSetDataTypeConstructor as Type: used after lift ---

impl Value for DefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(DefinitionSetDataTypeConstructor {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }
}

impl Type for DefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for DefinitionSetDataTypeConstructor { }

impl AspectFunction for DefinitionSetDataTypeConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Ensure scope entry IDs are allocated for all functions and methods
        {
            let functions = self.set.functions.borrow();
            for func_data in functions.iter() {
                if func_data.scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    func_data.scope_entry_id.set(Some(id));
                }
            }
        }
        {
            let data_types = self.set.data_types.borrow();
            for dt in data_types.iter() {
                for method in dt.methods.borrow().iter() {
                    if method.scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method.scope_entry_id.set(Some(id));
                    }
                    if method.self_scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method.self_scope_entry_id.set(Some(id));
                    }
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

        // Resolve underlying type
        let underlying_type_expr = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let expr = dt.underlying_type_expr.borrow();
            expr.as_ref().map(|v| {
                (v.as_ref() as &dyn Any)
                    .downcast_ref::<super::expr::Expr>()
                    .expect("underlying type must be an Expr")
                    .clone()
            }).expect("Data type missing underlying type")
        };

        return er_seq_cv!(loc, erb, {
            let underlying_cv = erb.must_er(super::eval_half_mortar(xx, &underlying_type_expr))?;
            let underlying_type = underlying_cv.value.type_(xx, loc);

            // Convert argument to underlying type
            let arg_value_type = argument.value.type_(xx, loc);
            let converted = if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);
                if elements.len() != 1 {
                    return erb.err_new(
                        format!("Constructor expects 1 argument, got {}", elements.len())
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
                erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), cv))?
            } else {
                erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?
            };
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;

            // Build wrapper object fields
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let mut object_fields = vec![
                (dt.unique_field.clone(), JsCode {
                    loc,
                    type_: JsCodeType::Value(serde_json::Value::Number(
                        serde_json::Number::from(dt.identity as u64),
                    )),
                }),
                (dt.data_field.clone(), underlying_js),
            ];
            for method in dt.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get()
                    .expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
                }));
            }

            let result_type = Rc::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, result_type.as_ref()))?;

            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::Object(JsCodeObject { fields: object_fields }),
            }));
        });
    }
}

// --- DefinitionSetDataType: the instance type ---

pub(crate) struct DefinitionSetDataType {
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
}

impl Value for DefinitionSetDataType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(DefinitionSetDataType {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }
}

impl Type for DefinitionSetDataType {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        format!("data_type(identity={})", dt.identity)
    }

    fn identity(&self) -> Option<usize> {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        Some(dt.identity)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }
}

impl Aspect for DefinitionSetDataType { }

impl AspectConsumeJs for DefinitionSetDataType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for DefinitionSetDataType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(DefinitionSetDataType {
                    set: self.set.clone(),
                    data_type_index: self.data_type_index,
                }),
            }),
        }
    }
}

impl AspectField for DefinitionSetDataType {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up method by name - extract data before calling eval
            let (field_name, ret_type_expr_clone) = {
                let data_types = self.set.data_types.borrow();
                let dt = &data_types[self.data_type_index];
                let methods = dt.methods.borrow();
                let method = methods.iter().find(|m| m.name == key_name);
                match method {
                    Some(m) => {
                        let ret_clone = m.ret_type_expr.borrow().as_ref().map(|v| {
                            (v.as_ref() as &dyn Any)
                                .downcast_ref::<super::expr::Expr>()
                                .expect("ret type must be an Expr")
                                .clone()
                        });
                        (m.field_name.clone(), ret_clone)
                    },
                    None => return erb.err_new(
                        format!("Field '{}' not found on data type", key_name)
                    ),
                }
            };

            // Resolve return type
            let ret_type: Rc<dyn Type> = match ret_type_expr_clone {
                Some(expr) => {
                    let r = super::eval_half_mortar(xx, &expr);
                    let cv = erb.must_er(r)?;
                    cv.value.type_(xx, loc)
                },
                None => Rc::new(TypeMortarNull),
            };

            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            let field_type = Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: ret_type,
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::Field(JsCodeField {
                            base: Box::new(self_code),
                            field: field_name,
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        let methods = dt.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(MortarTypeFunction {
                    arg: Rc::new(TypeMortarNull),
                    ret: Rc::new(TypeMortarNull),
                }));
            }
        }
        None
    }
}

// --- JsWrap for Rc<DefinitionSetFunction> ---

impl JsWrap for Rc<DefinitionSetFunction> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetFunction)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        rc_to_v8(scope, self).into()
    }
}

// --- MortarTypeDefinitionSetFunction: eval_half type of a function ref ---
// Kept manual because it has AspectLiftJs (special lifting behavior).

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
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
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
        let func_ref: Rc<DefinitionSetFunction> = rc_from_v8(value);
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
