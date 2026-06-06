use {
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
        eval::{
            Erb,
            jswrap::{
                AnyType,
                JsWrap,
                StagedExpr,
                jswrap_method_name,
            },
            mortar_function::MortarTypeFunction,
            mortar_js_value::{
                MortarJsValue,
                MortarJsValueSource,
            },
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
        TaggedUnionConstructorRef,
        TaggedUnionData,
        TaggedUnionRef,
        TaggedUnionVariant,
        next_interface_method_id,
    },
};

jswrap!(struct DefineResult(self_, scope) {
    data {
        ds: Rc < Gc < DefinitionSet >>
    }
    fn new_func(name: String) -> NewFuncResult {
        let ds = self_.ds.clone();
        let idx = {
            let mut functions = ds.functions.borrow_mut();
            let idx = functions.len();
            let entry_id = ds.next_entry_id.next();
            functions.push(DefinitionSetFunctionData {
                name: Some(name),
                description: RefCell::new(None),
                impl_expr: RefCell::new(None),
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,))
                ),
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
                artifact_id: RefCell::new(None),
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
        Rc::new(DefinitionSetExports { set: (*self_.ds).clone(), })
    }
    fn tagged_union() -> TaggedUnionBuilder {
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Compile(uc.compile_id.clone(), uc.counter.next());
        let data = Rc::new(TaggedUnionData {
            variants: RefCell::new(Vec::new()),
            identity,
            next_tag_id: crate::langhelp::Counter::default(),
        });
        TaggedUnionBuilder { data }
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
    data {
        func_ref: Rc < DefinitionSetFunction >
    }
    field func: Rc < DefinitionSetFunction > {
        self_.func_ref.clone()
    }
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
    fn arg_(type_ref: AnyType) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut arg_type = func_data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn arg_type(iface_ref: Rc<DefinitionSetInterfaceRef>) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut arg_type = func_data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type = Some(Rc::new(super::DefinitionSetInterface {
            set: iface_ref.set.clone(),
            interface_index: iface_ref.interface_index,
        }) as Rc<dyn crate::langhelp::Type>);
    }
    fn ret_(type_ref: AnyType) -> () {
        let functions = self_.func_ref.set.functions.borrow();
        let func_data = &functions[self_.func_ref.func_index];
        let mut ret_type = func_data.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(functions);
            crate::eval::v8util::throw_error(scope, "Function already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- Tagged union JsWrap structs ---
jswrap!(struct TaggedUnionBuilder(self_, scope) {
    data {
        data: Rc < TaggedUnionData >
    }
    field type_: Rc < TaggedUnionRef > {
        Rc::new(TaggedUnionRef { data: self_.data.clone() })
    }
    fn variant(tag: String, type_ref: AnyType) -> Rc<TaggedUnionConstructorRef> {
        let variant_index = {
            let mut variants = self_.data.variants.borrow_mut();
            let idx = variants.len();
            let js_tag = format!("{}{}", crate::jscode::TAG_PREFIX, self_.data.next_tag_id.next());
            variants.push(TaggedUnionVariant {
                tag: crate::langhelp::ConstKey::String(tag),
                js_tag,
                type_: type_ref.0,
            });
            idx
        };
        Rc::new(TaggedUnionConstructorRef {
            data: self_.data.clone(),
            variant_index,
        })
    }
});

// --- Data type JsWrap structs ---
jswrap!(struct NewDataResult(self_, scope) {
    data {
        data_type_ref: Rc < DefinitionSetDataTypeRef >
    }
    field type_: Rc < DefinitionSetDataTypeRef > {
        self_.data_type_ref.clone()
    }
    field new_: Rc < DefinitionSetDataTypeConstructor > {
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
    fn set_underlying(type_ref: AnyType) -> () {
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
            let entry_id = ds_ref.set.next_entry_id.next();
            methods.push(DefinitionSetDataMethodData {
                name,
                impl_expr: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(ds_ref.set.import_id, ds_ref.set.def_set_id, entry_id,))
                ),
                self_scope_entry_id: Cell::new(None),
                field_name,
                compiled_js: RefCell::new(None),
            });
            idx
        };
        NewDataMethodResult {
            data_type_ref: self_.data_type_ref.clone(),
            method_index,
        }
    }
    fn impl_interface(
        iface_ref: Rc<DefinitionSetInterfaceRef>,
        record: std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>
    ) -> () {
        let record_entries = record;
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
            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
            match record_entries.get(&const_key) {
                Some(expr) => {
                    let entry_id = self_.data_type_ref.set.next_entry_id.next();
                    method_impls.push(DefinitionSetDataTypeInterfaceMethodImplData {
                        interface_method_index: *imethod_idx,
                        impl_expr: Rc::new(expr.clone()),
                        scope_entry_id: Cell::new(
                            Some(
                                crate::langhelp::ScopeEntryId::Def(
                                    self_.data_type_ref.set.import_id,
                                    self_.data_type_ref.set.def_set_id,
                                    entry_id,
                                )
                            )
                        ),
                        self_scope_entry_id: Cell::new(None),
                    });
                },
                None => {
                    crate::eval::v8util::throw_error(
                        scope,
                        &format!("Missing implementation for interface method '{}'", imethod_name)
                    );
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
    data {
        data_type_ref: Rc < DefinitionSetDataTypeRef >,
        method_index: usize
    }
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
    fn ret_(type_ref: AnyType) -> () {
        let data_types = self_.data_type_ref.set.data_types.borrow();
        let dt = &data_types[self_.data_type_ref.data_type_index];
        let methods = dt.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            drop(data_types);
            crate::eval::v8util::throw_error(scope, "Method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- Interface JsWrap structs ---
jswrap!(struct NewInterfaceResult(self_, scope) {
    data {
        interface_ref: Rc < DefinitionSetInterfaceRef >
    }
    field type_: Rc < DefinitionSetInterfaceRef > {
        self_.interface_ref.clone()
    }
    fn set_description(description: String) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        *iface.description.borrow_mut() = Some(description);
    }
    fn new_func(name: String) -> NewInterfaceMethodResult {
        let iface_ref = &self_.interface_ref;
        let field_name = format!("_im{}", next_interface_method_id(&iface_ref.set));
        let method_index = {
            let interfaces = iface_ref.set.interfaces.borrow();
            let iface = &interfaces[iface_ref.interface_index];
            let mut methods = iface.methods.borrow_mut();
            let idx = methods.len();
            methods.push(DefinitionSetInterfaceMethodData {
                name,
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
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
    data {
        interface_ref: Rc < DefinitionSetInterfaceRef >,
        method_index: usize
    }
    fn arg_(type_ref: AnyType) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        let methods = iface.methods.borrow();
        let method = &methods[self_.method_index];
        let mut arg_type = method.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            drop(methods);
            drop(interfaces);
            crate::eval::v8util::throw_error(scope, "Interface method already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn ret_(type_ref: AnyType) -> () {
        let interfaces = self_.interface_ref.set.interfaces.borrow();
        let iface = &interfaces[self_.interface_ref.interface_index];
        let methods = iface.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            drop(interfaces);
            crate::eval::v8util::throw_error(scope, "Interface method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- JsWrap for Rc`<DefinitionSetInterfaceRef>` ---
impl JsWrap for Rc<DefinitionSetInterfaceRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetInterfaceRef)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetInterfaceRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc`<DefinitionSetDataTypeRef>` ---
impl JsWrap for Rc<DefinitionSetDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeRef)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn crate::langhelp::Type> = crate::eval::from_v8_ref(&*value);
        let dt =
            (type_.as_ref() as &dyn std::any::Any)
                .downcast_ref::<super::DefinitionSetDataType>()
                .expect("DefinitionSetDataTypeRef::from_v8: expected DefinitionSetDataType");
        Rc::new(DefinitionSetDataTypeRef {
            set: dt.set.clone(),
            data_type_index: dt.data_type_index,
        })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn crate::langhelp::Type> = Rc::new(super::DefinitionSetDataType {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        });
        crate::eval::to_v8(scope, type_).into()
    }
}

// --- JsWrap for Rc`<DefinitionSetDataTypeConstructor>` ---
impl JsWrap for Rc<DefinitionSetDataTypeConstructor> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetDataTypeConstructor)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetDataTypeConstructor>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc`<DefinitionSetExports>` ---
impl JsWrap for Rc<DefinitionSetExports> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetExports)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetExports>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- JsWrap for Rc`<DefinitionSetFunction>` ---
impl JsWrap for Rc<DefinitionSetFunction> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::MortarTypeDefinitionSetFunction)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetFunction>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}
