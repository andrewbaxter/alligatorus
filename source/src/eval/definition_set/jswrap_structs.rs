use {
    super::{
        DefinitionSet,
        DefinitionSetDataMethodData,
        DefinitionSetDataTypeConstructor,
        DefinitionSetDataTypeData,
        DefinitionSetDataTypeInterfaceImplData,
        DefinitionSetDataTypeInterfaceMethodImplData,
        DefinitionSetDataTypeRef,
        DefinitionSetExports,
        DefinitionSetFunction,
        DefinitionSetFunctionData,
        DefinitionSetInterfaceData,
        DefinitionSetInterfaceMethodData,
        DefinitionSetInterfaceRef,
        next_interface_method_id,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_string_get,
        },
        jscode::{
            JsCode,
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
            Type,
            Unique,
            Value,
        },
    },
    crate::eval::{
        Erb,
        jswrap::{
            JsWrap,
            StagedExpr,
            TypeRef,
            jswrap_method_name,
        },
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        rc_to_v8,
        rc_from_v8,
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
};

jswrap!(struct DefineResult(self_, scope) {
    data { ds: Rc<Gc<DefinitionSet>> }
    fn new_func(name: String) -> NewFuncResult {
        let ds = self_.ds.clone();
        let idx = {
            let mut functions = ds.functions.borrow_mut();
            let idx = functions.len();
            functions.push(DefinitionSetFunctionData {
                name: Some(name),
                description: RefCell::new(None),
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
    fn new_data(name: String) -> NewDataResult {
        let ds = self_.ds.clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Compile(uc.compile_id.clone(), uc.counter.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let idx = {
            let mut data_types = ds.data_types.borrow_mut();
            let idx = data_types.len();
            data_types.push(DefinitionSetDataTypeData {
                name: Some(name),
                description: RefCell::new(None),
                underlying_type: RefCell::new(None),
                methods: RefCell::new(Vec::new()),
                interface_impls: RefCell::new(Vec::new()),
                identity,
                data_field,
            });
            idx
        };
        let data_type_ref = Rc::new(DefinitionSetDataTypeRef {
            set: (*ds).clone(),
            data_type_index: idx,
        });
        NewDataResult { data_type_ref }
    }
    fn build() -> Rc<DefinitionSetExports> {
        Rc::new(DefinitionSetExports {
            set: (*self_.ds).clone(),
        })
    }
    fn new_interface() -> NewInterfaceResult {
        let ds = self_.ds.clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Compile(uc.compile_id.clone(), uc.counter.next());
        let idx = {
            let mut interfaces = ds.interfaces.borrow_mut();
            let idx = interfaces.len();
            interfaces.push(DefinitionSetInterfaceData {
                description: RefCell::new(None),
                methods: RefCell::new(Vec::new()),
                identity,
            });
            idx
        };
        let interface_ref = Rc::new(DefinitionSetInterfaceRef {
            set: (*ds).clone(),
            interface_index: idx,
        });
        NewInterfaceResult { interface_ref }
    }
});

jswrap!(struct NewFuncResult(self_, scope) {
    data { func_ref: Rc<DefinitionSetFunction> }
    field func: Rc<DefinitionSetFunction> { self_.func_ref.clone() }
    fn set_description(description: String) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        *func_data.description.borrow_mut() = Some(description);
    }
    fn impl_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut impl_expr = func_data.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has an implementation");
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
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type_expr = Some(staged.0);
    }
    fn arg_type(type_ref: Rc<DefinitionSetInterfaceRef>) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut arg_type_expr = func_data.arg_type_expr.borrow_mut();
        if arg_type_expr.is_some() {
            drop(arg_type_expr);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        // Create a literal Expr that evaluates to the interface type
        use crate::eval::mortar_js_value::{MortarJsValue, MortarJsValueSource};
        let set = type_ref.set.clone();
        let idx = type_ref.interface_index;
        let literal_expr: Box<dyn crate::langhelp::Value> = Box::new(crate::eval::expr::Expr {
            loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
            r#type: crate::eval::expr::Expr_::Literal(Rc::new(move || {
                Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(super::DefinitionSetInterface {
                        set: set.clone(),
                        interface_index: idx,
                    }),
                })
            })),
        });
        *arg_type_expr = Some(literal_expr);
    }
    fn ret_(staged: StagedExpr) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut ret_type_expr = func_data.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has a ret type");
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- Data type JsWrap structs ---

jswrap!(struct NewDataResult(self_, scope) {
    data { data_type_ref: Rc<DefinitionSetDataTypeRef> }
    field type_: Rc<DefinitionSetDataTypeRef> { self_.data_type_ref.clone() }
    field new_: Rc<DefinitionSetDataTypeConstructor> {
        Rc::new(DefinitionSetDataTypeConstructor {
            set: self_.data_type_ref.set.clone(),
            data_type_index: self_.data_type_ref.data_type_index,
        })
    }
    fn set_description(description: String) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        *dt.description.borrow_mut() = Some(description);
    }
    fn set_underlying(type_ref: TypeRef) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let mut underlying = dt.underlying_type.borrow_mut();
        if underlying.is_some() {
            drop(underlying);
            drop(data_types);
            crate::eval::v8util::throw_error(scope, "Data type already has an underlying type");
            return;
        }
        *underlying = Some(type_ref.0);
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
    fn impl_interface(iface_ref: Rc<DefinitionSetInterfaceRef>, record: StagedExpr) -> () {
        let record_expr = (record.0.as_ref() as &dyn std::any::Any)
            .downcast_ref::<crate::eval::expr::Expr>()
            .expect("impl_interface record must be a staged expression");

        let record_entries = match &record_expr.r#type {
            crate::eval::expr::Expr_::Record(r) => &r.entries,
            _ => {
                crate::eval::v8util::throw_error(scope, "impl_interface argument must be a Record expression");
                return;
            },
        };

        // Extract string keys from record key expressions
        let record_string_entries: Vec<(String, &crate::eval::expr::Expr)> = {
            let mut out = Vec::new();
            for (k_expr, v_expr) in record_entries {
                match &k_expr.r#type {
                    crate::eval::expr::Expr_::Literal(lit) => {
                        let val = lit();
                        let val_any: &dyn std::any::Any = val.as_ref();
                        if let Some(s) = val_any.downcast_ref::<crate::eval::string::ValueString>() {
                            out.push((s.0.clone(), v_expr));
                        } else {
                            crate::eval::v8util::throw_error(scope, "Record keys must be string literals");
                            return;
                        }
                    },
                    _ => {
                        crate::eval::v8util::throw_error(scope, "Record keys must be string literals");
                        return;
                    },
                }
            }
            out
        };

        let interface_index = iface_ref.interface_index;

        // Extract method names and indices from interface (release borrow after)
        let iface_method_names: Vec<(usize, String)> = {
            let interfaces = iface_ref.set.interfaces.borrow();
            let iface = &interfaces[interface_index];
            let methods = iface.methods.borrow();
            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
        };

        let mut method_impls = Vec::new();
        for (imethod_idx, imethod_name) in &iface_method_names {
            let entry = record_string_entries.iter()
                .find(|(k, _)| k == imethod_name);
            match entry {
                Some((_, expr)) => {
                    method_impls.push(DefinitionSetDataTypeInterfaceMethodImplData {
                        interface_method_index: *imethod_idx,
                        impl_expr: Box::new((*expr).clone()),
                        scope_entry_id: Cell::new(None),
                        self_scope_entry_id: Cell::new(None),
                    });
                },
                None => {
                    crate::eval::v8util::throw_error(scope, &format!("Missing implementation for interface method '{}'", imethod_name));
                    return;
                },
            }
        }

        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        dt.interface_impls.borrow_mut().push(DefinitionSetDataTypeInterfaceImplData {
            interface_index,
            method_impls,
        });
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
            crate::eval::v8util::throw_error(scope, "Method already has an implementation");
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
            crate::eval::v8util::throw_error(scope, "Method already has a ret type");
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- Interface JsWrap structs ---

jswrap!(struct NewInterfaceResult(self_, scope) {
    data { interface_ref: Rc<DefinitionSetInterfaceRef> }
    field type_: Rc<DefinitionSetInterfaceRef> { self_.interface_ref.clone() }
    fn set_description(description: String) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        *iface.description.borrow_mut() = Some(description);
    }
    fn new_func(name: String) -> NewInterfaceMethodResult {
        let iface_ref = &self_.interface_ref;
        let field_name = format!("_im{}", next_interface_method_id());
        let method_index = {
            let interfaces = iface_ref.set.interfaces.borrow();
            let iface = &interfaces[iface_ref.interface_index];
            let mut methods = iface.methods.borrow_mut();
            let idx = methods.len();
            methods.push(DefinitionSetInterfaceMethodData {
                name,
                arg_type_expr: RefCell::new(None),
                ret_type_expr: RefCell::new(None),
                field_name,
            });
            idx
        };
        NewInterfaceMethodResult {
            interface_ref: self_.interface_ref.clone(),
            method_index,
        }
    }
});

jswrap!(struct NewInterfaceMethodResult(self_, scope) {
    data { interface_ref: Rc<DefinitionSetInterfaceRef>, method_index: usize }
    fn arg_(staged: StagedExpr) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        let methods = iface.methods.borrow();
        let method = &methods[self_.method_index];
        let mut arg_type_expr = method.arg_type_expr.borrow_mut();
        if arg_type_expr.is_some() {
            drop(arg_type_expr);
            drop(methods);
            drop(iface);
            drop(interfaces);
            crate::eval::v8util::throw_error(scope, "Interface method already has an arg type");
            return;
        }
        *arg_type_expr = Some(staged.0);
    }
    fn ret_(staged: StagedExpr) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        let methods = iface.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type_expr = method.ret_type_expr.borrow_mut();
        if ret_type_expr.is_some() {
            drop(ret_type_expr);
            drop(methods);
            drop(iface);
            drop(interfaces);
            crate::eval::v8util::throw_error(scope, "Interface method already has a ret type");
            return;
        }
        *ret_type_expr = Some(staged.0);
    }
});

// --- JsWrap for Rc<DefinitionSetInterfaceRef> ---

impl JsWrap for Rc<DefinitionSetInterfaceRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetInterfaceRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetDataTypeRef> ---

impl JsWrap for Rc<DefinitionSetDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetDataTypeConstructor> ---

impl JsWrap for Rc<DefinitionSetDataTypeConstructor> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeConstructor)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetExports> ---

impl JsWrap for Rc<DefinitionSetExports> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetExports)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc<DefinitionSetFunction> ---

impl JsWrap for Rc<DefinitionSetFunction> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetFunction)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}
