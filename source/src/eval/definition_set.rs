use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectField,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_string_get,
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
        ffi::c_void,
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        mortar_null::TypeMortarNull,
    },
};

// --- DefinitionSet data ---

pub(crate) struct DefinitionSetFunctionData {
    pub impl_expr: RefCell<Option<Box<dyn Value>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
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

/// Wraps a Gc<DefinitionSet> + func_index for passing through v8::External.
pub(crate) struct DefinitionSetFunctionRef {
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

    // Create DefinitionSet and store directly as v8::External on the object
    let ds = Box::new(Gc::new(DefinitionSet::new()));
    let ptr = Box::into_raw(ds) as *mut c_void;
    let ds_ext = v8::External::new(scope, ptr);
    let ds_key = v8::String::new(scope, "_ds").unwrap();
    obj.set(scope, ds_key.into(), ds_ext.into());

    // new_func method
    let new_func_fn = v8::Function::builder(new_func_js_callback).build(scope).unwrap();
    let nf_key = v8::String::new(scope, "new_func").unwrap();
    obj.set(scope, nf_key.into(), new_func_fn.into());

    // impl method
    let impl_fn = v8::Function::builder(impl_js_callback).build(scope).unwrap();
    let impl_key = v8::String::new(scope, "impl").unwrap();
    obj.set(scope, impl_key.into(), impl_fn.into());

    ret.set(obj.into());
}

fn new_func_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    let this = args.this();
    let ds_key = v8::String::new(scope, "_ds").unwrap();
    let ds_ext: v8::Local<v8::External> = this.get(scope, ds_key.into()).unwrap().try_cast().unwrap();
    let ds = unsafe { &*(ds_ext.value() as *const Gc<DefinitionSet>) };

    let idx = {
        let mut functions = ds.functions.borrow_mut();
        let idx = functions.len();
        functions.push(DefinitionSetFunctionData {
            impl_expr: RefCell::new(None),
            scope_entry_id: Cell::new(None),
        });
        idx
    };
    let func_ref = Box::new(DefinitionSetFunctionRef { set: ds.clone(), func_index: idx });

    let ptr = Box::into_raw(func_ref) as *mut c_void;
    let ext = v8::External::new(scope, ptr);
    ret.set(ext.into());
}

fn impl_js_callback(
    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
    args: v8::FunctionCallbackArguments<'_>,
    mut ret: v8::ReturnValue<'_>,
) {
    // args[0] is the function ref (v8::External -> DefinitionSetFunctionRef)
    let func_ref_ext: v8::Local<v8::External> = args.get(0).try_cast().expect(
        "impl: first argument must be a function ref (v8::External)"
    );
    let func_ref = unsafe { &*(func_ref_ext.value() as *const DefinitionSetFunctionRef) };

    // args[1] is the staged expr (v8::External -> StagedExpr)
    let staged_ext: v8::Local<v8::External> = args.get(1).try_cast().expect(
        "impl: second argument must be a staged expr (v8::External)"
    );
    // Take ownership of the StagedExpr
    let staged = unsafe {
        Box::from_raw(staged_ext.value() as *mut super::expr::StagedExpr)
    };

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
    *impl_expr = Some(staged.value);

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
    fn to_js(&self, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
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
                    ret: Rc::new(MortarTypeDefinitionSetFunction),
                }),
                "impl" => Rc::new(super::mortar_function::MortarTypeFunction {
                    arg: Rc::new(super::tuple::TypeTuple {
                        types: vec![
                            Rc::new(MortarTypeDefinitionSetFunction),
                            Rc::new(super::expr::ExprType),
                        ],
                    }),
                    ret: Rc::new(TypeMortarNull),
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
                ret: Rc::new(MortarTypeDefinitionSetFunction),
            })),
            "impl" => Some(Rc::new(super::mortar_function::MortarTypeFunction {
                arg: Rc::new(super::tuple::TypeTuple {
                    types: vec![
                        Rc::new(MortarTypeDefinitionSetFunction),
                        Rc::new(super::expr::ExprType),
                    ],
                }),
                ret: Rc::new(TypeMortarNull),
            })),
            _ => None,
        }
    }
}

// --- MortarTypeDefinitionSetFunction: the eval_half type of a function ref ---
// Stateless during eval_half. After lift, ValueDefinitionSetFunction carries the data.

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
    fn to_js(&self, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
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
        assert!(value.is_external(), "Expected v8::External for definition set function");
        let ext = unsafe { &*(value as *const v8::Value as *const v8::External) };
        // Take ownership of the DefinitionSetFunctionRef
        let func_ref = unsafe { Box::from_raw(ext.value() as *mut DefinitionSetFunctionRef) };
        func_ref.set.finish().expect("Definition set finish failed");
        Box::new(ValueDefinitionSetFunction {
            set: func_ref.set,
            func_index: func_ref.func_index,
        })
    }
}

// --- TypeLiftedDefinitionSetFunction: the type AFTER lift, carries the set ---

pub(crate) struct TypeLiftedDefinitionSetFunction {
    pub set: Gc<DefinitionSet>,
    pub func_index: usize,
}

impl Value for TypeLiftedDefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeLiftedDefinitionSetFunction {
            set: self.set.clone(),
            func_index: self.func_index,
        })
    }
}

impl Type for TypeLiftedDefinitionSetFunction {
    fn desc(&self) -> String {
        format!("lifted_definition_set_function")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeLiftedDefinitionSetFunction {
                set: self.set.clone(),
                func_index: self.func_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert lifted_definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to lifted_definition_set_function"))
    }
}

impl Aspect for TypeLiftedDefinitionSetFunction { }

impl AspectFunction for TypeLiftedDefinitionSetFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, _argument: CodeValue) -> EvalRes {
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

        let scope_id = self.set.functions.borrow()[self.func_index]
            .scope_entry_id.get()
            .expect("Definition set function not registered");

        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(JsCode {
                    loc,
                    type_: JsCodeType::Call(JsCodeCall {
                        func: Box::new(JsCode {
                            loc,
                            type_: JsCodeType::BindingUse(JsCodeBindingUse { id: scope_id }),
                        }),
                        args: vec![],
                    }),
                })),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNull),
            }),
        })
    }
}

// --- ValueDefinitionSetFunction: the lifted value data ---

pub(crate) struct ValueDefinitionSetFunction {
    pub set: Gc<DefinitionSet>,
    pub func_index: usize,
}

impl Value for ValueDefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeLiftedDefinitionSetFunction {
            set: self.set.clone(),
            func_index: self.func_index,
        })
    }
}
