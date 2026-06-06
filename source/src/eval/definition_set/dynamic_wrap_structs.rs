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
        dynamic_code::{
            DynamicCode,
            DynamicCodeType,
        },
        eval::{
            Erb,
            dynamic_wrap::{
                AnyType,
                DynamicWrap,
                StagedExpr,
                dynamic_wrap_method_name,
            },
            value::{
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                value_function::dynamic::TypeFunctionDynamic,
            },
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

dynamic_wrap!(struct DefineResult(self_, scope) {
    data {
        ds: Rc < Gc < DefinitionSet >>
    }
    fn new_func(name: String) -> NewFuncResult {
        let ds = self_.ds.clone();
        let entry_id = ds.next_entry_id.next();
        let func_data = Gc::new(DefinitionSetFunctionData {
            set: (*ds).clone(),
            name: Some(name),
            description: RefCell::new(None),
            impl_expr: RefCell::new(None),
            arg_type: RefCell::new(None),
            ret_type: RefCell::new(None),
            scope_entry_id: Cell::new(
                Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,))
            ),
        });
        ds.functions.borrow_mut().push(func_data.clone());
        let func_ref = Rc::new(DefinitionSetFunction { data: func_data, });
        NewFuncResult { func_ref }
    }
    fn new_data(name: String) -> NewDataResult {
        let ds = self_.ds.clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Compile(uc.compile_id.clone(), uc.counter.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let dt_data = Gc::new(DefinitionSetDataTypeData {
            set: (*ds).clone(),
            name: Some(name),
            description: RefCell::new(None),
            underlying_type: RefCell::new(None),
            methods: RefCell::new(Vec::new()),
            interface_impls: RefCell::new(Vec::new()),
            identity,
            data_field,
            artifact_id: RefCell::new(None),
        });
        ds.data_types.borrow_mut().push(dt_data.clone());
        let data_type_ref = Rc::new(DefinitionSetDataTypeRef { data: dt_data, });
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
        let iface_data = Gc::new(DefinitionSetInterfaceData {
            set: (*ds).clone(),
            description: RefCell::new(None),
            methods: RefCell::new(Vec::new()),
            identity,
        });
        ds.interfaces.borrow_mut().push(iface_data.clone());
        let interface_ref = Rc::new(DefinitionSetInterfaceRef { data: iface_data, });
        NewInterfaceResult { interface_ref }
    }
});

dynamic_wrap!(struct NewFuncResult(self_, scope) {
    data {
        func_ref: Rc < DefinitionSetFunction >
    }
    field func: Rc < DefinitionSetFunction > {
        self_.func_ref.clone()
    }
    fn set_description(description: String) -> () {
        *self_.func_ref.data.description.borrow_mut() = Some(description);
    }
    fn impl_(staged: StagedExpr) -> () {
        let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            crate::eval::v8util::throw_error(scope, "Function already has an implementation");
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn arg_(type_ref: AnyType) -> () {
        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn arg_type(iface_ref: Rc<DefinitionSetInterfaceRef>) -> () {
        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            crate::eval::v8util::throw_error(scope, "Function already has an arg type");
            return;
        }
        *arg_type =
            Some(
                Rc::new(super::DefinitionSetInterface { data: iface_ref.data.clone(), }) as
                    Rc<dyn crate::langhelp::Type>
            );
    }
    fn ret_(type_ref: AnyType) -> () {
        let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            crate::eval::v8util::throw_error(scope, "Function already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- Tagged union DynamicWrap structs ---
dynamic_wrap!(struct TaggedUnionBuilder(self_, scope) {
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
            let js_tag = format!("{}{}", crate::dynamic_code::TAG_PREFIX, self_.data.next_tag_id.next());
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

// --- Data type DynamicWrap structs ---
dynamic_wrap!(struct NewDataResult(self_, scope) {
    data {
        data_type_ref: Rc < DefinitionSetDataTypeRef >
    }
    field type_: Rc < DefinitionSetDataTypeRef > {
        self_.data_type_ref.clone()
    }
    field new_: Rc < DefinitionSetDataTypeConstructor > {
        Rc::new(DefinitionSetDataTypeConstructor { data: self_.data_type_ref.data.clone(), })
    }
    fn set_description(description: String) -> () {
        *self_.data_type_ref.data.description.borrow_mut() = Some(description);
    }
    fn set_underlying(type_ref: AnyType) -> () {
        let mut underlying = self_.data_type_ref.data.underlying_type.borrow_mut();
        if underlying.is_some() {
            drop(underlying);
            crate::eval::v8util::throw_error(scope, "Data type already has an underlying type");
            return;
        }
        *underlying = Some(type_ref.0);
    }
    fn new_func(name: String) -> NewDataMethodResult {
        let dt_data = &self_.data_type_ref.data;
        let field_name = format!("_{}", dt_data.set.next_field_id.next());
        let entry_id = dt_data.set.next_entry_id.next();
        let method_index = {
            let mut methods = dt_data.methods.borrow_mut();
            let idx = methods.len();
            methods.push(DefinitionSetDataMethodData {
                name,
                impl_expr: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(dt_data.set.import_id, dt_data.set.def_set_id, entry_id,))
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
        let iface_data = &iface_ref.data;

        // Extract method names and indices from interface (release borrow after)
        let iface_method_names: Vec<(usize, String)> = {
            let methods = iface_data.methods.borrow();
            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
        };
        let mut method_impls = Vec::new();
        let dt_data = &self_.data_type_ref.data;
        for (imethod_idx, imethod_name) in &iface_method_names {
            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
            match record_entries.get(&const_key) {
                Some(expr) => {
                    let entry_id = dt_data.set.next_entry_id.next();
                    method_impls.push(DefinitionSetDataTypeInterfaceMethodImplData {
                        interface_method_index: *imethod_idx,
                        impl_expr: Rc::new(expr.clone()),
                        scope_entry_id: Cell::new(
                            Some(
                                crate::langhelp::ScopeEntryId::Def(
                                    dt_data.set.import_id,
                                    dt_data.set.def_set_id,
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
        dt_data.interface_impls.borrow_mut().push(DefinitionSetDataTypeInterfaceImplData {
            interface: iface_data.clone(),
            method_impls,
        });
    }
});

dynamic_wrap!(struct NewDataMethodResult(self_, scope) {
    data {
        data_type_ref: Rc < DefinitionSetDataTypeRef >,
        method_index: usize
    }
    fn impl_(staged: StagedExpr) -> () {
        let methods = self_.data_type_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut impl_expr = method.impl_expr.borrow_mut();
        if impl_expr.is_some() {
            drop(impl_expr);
            drop(methods);
            crate::eval::v8util::throw_error(scope, "Method already has an implementation");
            return;
        }
        *impl_expr = Some(staged.0);
    }
    fn ret_(type_ref: AnyType) -> () {
        let methods = self_.data_type_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            crate::eval::v8util::throw_error(scope, "Method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- Interface DynamicWrap structs ---
dynamic_wrap!(struct NewInterfaceResult(self_, scope) {
    data {
        interface_ref: Rc < DefinitionSetInterfaceRef >
    }
    field type_: Rc < DefinitionSetInterfaceRef > {
        self_.interface_ref.clone()
    }
    fn set_description(description: String) -> () {
        *self_.interface_ref.data.description.borrow_mut() = Some(description);
    }
    fn new_func(name: String) -> NewInterfaceMethodResult {
        let iface_data = &self_.interface_ref.data;
        let field_name = format!("_im{}", next_interface_method_id(&iface_data.set));
        let method_index = {
            let mut methods = iface_data.methods.borrow_mut();
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

dynamic_wrap!(struct NewInterfaceMethodResult(self_, scope) {
    data {
        interface_ref: Rc < DefinitionSetInterfaceRef >,
        method_index: usize
    }
    fn arg_(type_ref: AnyType) -> () {
        let methods = self_.interface_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut arg_type = method.arg_type.borrow_mut();
        if arg_type.is_some() {
            drop(arg_type);
            drop(methods);
            crate::eval::v8util::throw_error(scope, "Interface method already has an arg type");
            return;
        }
        *arg_type = Some(type_ref.0);
    }
    fn ret_(type_ref: AnyType) -> () {
        let methods = self_.interface_ref.data.methods.borrow();
        let method = &methods[self_.method_index];
        let mut ret_type = method.ret_type.borrow_mut();
        if ret_type.is_some() {
            drop(ret_type);
            drop(methods);
            crate::eval::v8util::throw_error(scope, "Interface method already has a ret type");
            return;
        }
        *ret_type = Some(type_ref.0);
    }
});

// --- DynamicWrap for Rc`<DefinitionSetInterfaceRef>` ---
impl DynamicWrap for Rc<DefinitionSetInterfaceRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::TypeSourceInterfaceRefDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetInterfaceRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- DynamicWrap for Rc`<DefinitionSetDataTypeRef>` ---
impl DynamicWrap for Rc<DefinitionSetDataTypeRef> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::TypeSourceDataTypeRefDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn crate::langhelp::Type> = crate::eval::from_v8_ref(&*value);
        let dt =
            (type_.as_ref() as &dyn std::any::Any)
                .downcast_ref::<super::DefinitionSetDataType>()
                .expect("DefinitionSetDataTypeRef::from_v8: expected DefinitionSetDataType");
        Rc::new(DefinitionSetDataTypeRef { data: dt.data.clone() })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn crate::langhelp::Type> =
            Rc::new(super::DefinitionSetDataType { data: self.data.clone() });
        crate::eval::to_v8(scope, type_).into()
    }
}

// --- DynamicWrap for Rc`<DefinitionSetDataTypeConstructor>` ---
impl DynamicWrap for Rc<DefinitionSetDataTypeConstructor> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::TypeSourceDataTypeConstructorDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetDataTypeConstructor>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- DynamicWrap for Rc`<DefinitionSetExports>` ---
impl DynamicWrap for Rc<DefinitionSetExports> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::TypeSourceExportsDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetExports>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}

// --- DynamicWrap for Rc`<DefinitionSetFunction>` ---
impl DynamicWrap for Rc<DefinitionSetFunction> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(super::TypeSourceFunctionDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetFunction>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}
