use {
    crate::buildsrc::util::{
        InterfaceSpec,
        PushMethod,
        arg_dynamic_type_expr_str,
        snake_case,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>, trait_specs: Vec<InterfaceSpec>) {
    struct PushField {
        body: TokenStream,
        name: &'static str,
        ret: &'static str,
    }

    struct PushStructSpec {
        builtin_unique_id: usize,
        data_fields: Vec<(&'static str, &'static str)>,
        extra_aspect_fn: Option<&'static str>,
        extra_dispatch_arms: Vec<TokenStream>,
        extra_field_type_opt_arms: Vec<TokenStream>,
        extra_method_setters: Vec<TokenStream>,
        fields: Vec<PushField>,
        methods: Vec<PushMethod>,
        name: &'static str,
    }

    let record_arg_type = "std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>";
    let impl_only_specs: Vec<&InterfaceSpec> = trait_specs.iter().filter(|s| !s.register_in_scope).collect();
    let mut source_object_extra_dispatch: Vec<TokenStream> = Vec::new();
    let mut source_object_extra_setters: Vec<TokenStream> = Vec::new();
    let mut source_object_extra_field_type_opt: Vec<TokenStream> = Vec::new();
    let method_start_idx: usize = 2 + 5;
    for (extra_i, spec) in impl_only_specs.iter().enumerate() {
        let method_idx = method_start_idx + extra_i;
        let method_idx_str = format!("_{}", method_idx);
        let method_name = spec.name;
        let field_ident = format_ident!("{}", if spec.name == "type" {
            "type_"
        } else {
            spec.name
        });
        let body = gen_builtin_impl_body(&field_ident.to_string());
        let record_arg_ty: TokenStream = record_arg_type.parse().unwrap();
        source_object_extra_dispatch.push(quote!{
            if key_name == crate:: eval:: push:: push_method_name(#method_name) {
                let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = std:: rc:: Rc:: new(
                    crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                        arg: <#record_arg_ty as crate:: eval:: push:: Push >:: dynamic_type(),
                        ret: <() as crate:: eval:: push:: Push >:: dynamic_type(),
                    },
                );
                let self_code = erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                return Ok(crate:: langhelp:: CodeValue {
                    code: crate:: langhelp:: Code {
                        loc,
                        code: crate:: langhelp:: Code_:: Dyn(std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                            loc,
                            type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                crate:: code_dynamic:: CodeDynamicFieldUse {
                                    base: Box:: new(self_code),
                                    field: #method_idx_str.to_string(),
                                },
                            ),
                        },)),
                    },
                    value: std:: rc:: Rc:: new(crate::eval::value::value_dynamic::ValueDynamic {
                        source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                        type_: field_type,
                    }).into(),
                });
            }
        });
        source_object_extra_field_type_opt.push(quote!{
            if key.as_str() == crate:: eval:: push:: push_method_name(#method_name) {
                return Some(std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                    arg: <#record_arg_ty as crate:: eval:: push:: Push >:: dynamic_type(),
                    ret: <() as crate:: eval:: push:: Push >:: dynamic_type(),
                },));
            }
        });
        source_object_extra_setters.push(quote!{
            {
                fn _jw_cb <'s >(
                    scope: & mut v8:: PinnedRef <'s,
                    v8:: HandleScope <'_ >>,
                    _jw_args: v8:: FunctionCallbackArguments <'s >,
                    mut _jw_rv: v8:: ReturnValue <'s >,
                ) {
                    let _jw_this = _jw_args.this();
                    let _jw_dk = v8::String::new(scope, "_d").unwrap();
                    #[allow(unused_variables)]
                    let self_: std::rc::Rc<SourceObjectResult> =
                        crate::eval::from_v8_ref::<std::rc::Rc<SourceObjectResult>>(
                            &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                        ).clone();
                    #[allow(unused_mut, unused_variables)]
                    let mut _jw_arg_i = 0i32;
                    let record: #record_arg_ty = <#record_arg_ty as crate:: eval:: push:: Push >:: from_v8(
                        scope,
                        _jw_args.get(_jw_arg_i),
                    );
                    _jw_arg_i += 1;
                    let _jw_result:() = {
                        #body
                    };
                    _jw_rv.set(<() as crate::eval::push::Push>::into_v8(_jw_result, scope),);
                }
                let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                let _jw_key = v8:: String:: new(scope, #method_idx_str).unwrap();
                _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
            }
        });
    }
    let mut specs: Vec<PushStructSpec> = vec![
        //. .
        PushStructSpec {
            name: "DefinitionSetBuilder",
            builtin_unique_id: 3009,
            data_fields: vec![("inner", "Gc<DefinitionSet>")],
            extra_aspect_fn: Some("crate::eval::definition_set::definition_set_extra_aspect"),
            fields: vec![],
            methods: vec![
                //. .
                PushMethod {
                    name: "func",
                    args: vec![("name", "String")],
                    ret: "SourceFuncResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let entry_id = ds.next_entry_id.next();
                        let func_data = Gc::new(SourceFunctionData {
                            set: RefCell::new(Some(ds.clone())),
                            name: Some(name),
                            description: RefCell::new(None),
                            impl_expr: RefCell::new(None),
                            arg_type: RefCell::new(None),
                            ret_type: RefCell::new(None),
                            scope_entry_id: Cell::new(
                                Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, entry_id,),)
                            ),
                            artifact_id: RefCell::new(None),
                            compiled_js: RefCell::new(None),
                            captured_externals: RefCell::new(Vec::new()),
                        });
                        ds.functions.borrow_mut().push(func_data.clone());
                        let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data });
                        SourceFuncResult { func_ref }
                    },
                },
                PushMethod {
                    name: "object",
                    args: vec![("name", "String")],
                    ret: "SourceObjectResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data_field = format!("_{}", ds.next_field_id.next());
                        let dt_data = Gc::new(SourceDataTypeData {
                            set: RefCell::new(Some(ds.clone())),
                            name: Some(name),
                            description: RefCell::new(None),
                            underlying_type: crate::invariant::DeserialLazy::new(),
                            methods: RefCell::new(Vec::new()),
                            interface_impls: RefCell::new(Vec::new()),
                            identity,
                            data_field,
                            artifact_id: RefCell::new(None),
                            constructor_artifact_id: RefCell::new(None),
                        });
                        ds.data_types.borrow_mut().push(dt_data.clone());
                        let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data });
                        SourceObjectResult { data_type_ref }
                    },
                },
                PushMethod {
                    name: "build",
                    args: vec![],
                    ret: "DefinitionSetBuilder",
                    body: quote!{
                        DefinitionSetBuilder { inner: self_.inner.clone() }
                    },
                },
                PushMethod {
                    name: "union",
                    args: vec![],
                    ret: "SourceTaggedUnionBuilder",
                    body: quote!{
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data = Rc::new(SourceTaggedUnionData {
                            variants: RefCell::new(Vec::new()),
                            identity,
                            next_tag_id: crate::langhelp::Counter::default(),
                            new_artifact_id: RefCell::new(None),
                        });
                        SourceTaggedUnionBuilder { data }
                    },
                },
                PushMethod {
                    name: "interface",
                    args: vec![],
                    ret: "SourceInterfaceResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let iface_data = Gc::new(SourceInterfaceData {
                            artifact_id: RefCell::new(None),
                            builtin_kind: None,
                            set: RefCell::new(Some(ds.clone())),
                            description: RefCell::new(None),
                            is_aspect: Cell::new(false),
                            methods: RefCell::new(Vec::new()),
                            identity,
                        });
                        ds.interfaces.borrow_mut().push(iface_data.clone());
                        let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data });
                        SourceInterfaceResult { interface_ref }
                    },
                },
                PushMethod {
                    name: "finish",
                    args: vec![],
                    ret: "DefinitionSetBuilder",
                    body: quote!{
                        let ex = scope.get_slot::<crate::eval::V8EvalContextSlot>().unwrap().ex.clone();
                        let xx = std::rc::Rc::new(crate::langhelp::ExprContext {
                            vx: ex.clone(),
                            import_state: std::rc::Rc::new(std::cell::Cell::new(ex.ix.import_state)),
                            scope: std::rc::Rc::new(crate::langhelp::Scope {
                                values: std::cell::RefCell::new(std::collections::HashMap::new()),
                                parent: None,
                            }),
                            target: std::rc::Rc::new(crate::eval::TargetDynamic),
                        });
                        if let Err(errors) = self_.inner.finish(&xx) {
                            crate::eval::dynamic_util::throw_error(
                                scope,
                                &format!("definition set finish failed: {:?}", errors)
                            );
                            return;
                        }
                        DefinitionSetBuilder { inner: self_.inner.clone() }
                    },
                },
            ],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "OptionPush",
            builtin_unique_id: 3001,
            data_fields: vec![("is_some_inner", "bool")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![PushMethod {
                name: "is_some_",
                args: vec![],
                ret: "bool",
                body: quote!{
                    self_.is_some_inner
                },
            }],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "TypeRefPush",
            builtin_unique_id: 3002,
            data_fields: vec![("type_ref", "Rc<dyn crate::langhelp::Type>")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![PushMethod {
                name: "aspect",
                args: vec![("unique", "Unique")],
                ret: "OptionPush",
                body: quote!{
                    let ex_slot =
                        scope
                            .get_slot::<crate::eval::V8EvalContextSlot>()
                            .expect("TypeRefPush.aspect: V8EvalContextSlot not set");
                    let import_state_id = ex_slot.ex.ix.import_state;
                    let xx = Rc::new(ExprContext {
                        vx: ex_slot.ex.clone(),
                        import_state: Rc::new(Cell::new(import_state_id)),
                        scope: Rc::new(Scope {
                            values: RefCell::new(HashMap::new()),
                            parent: None,
                        }),
                        target: Rc::new(crate::eval::TargetDynamic),
                    });
                    let is_some = self_.type_ref.aspect(&xx, &unique).is_some();
                    OptionPush { is_some_inner: is_some }
                },
            }],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceFuncResult",
            builtin_unique_id: 3003,
            data_fields: vec![("func_ref", "Rc<TypeSourceFunctionConst>")],
            extra_aspect_fn: None,
            fields: vec![PushField {
                name: "func",
                ret: "Rc<TypeSourceFunctionConst>",
                body: quote!{
                    self_.func_ref.clone()
                },
            }],
            //. .
            methods: vec![
                //. .
                PushMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.func_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                PushMethod {
                    name: "impl_",
                    args: vec![("arg", "AnyType"), ("ret", "AnyType"), ("impl_body", "StagedExpr")],
                    ret: "()",
                    body: quote!{
                        {
                            let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                            if arg_type.is_some() {
                                drop(arg_type);
                                throw_error(scope, "Function already has an arg type");
                                return;
                            }
                            *arg_type = Some(arg.0);
                        }
                        {
                            let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
                            if ret_type.is_some() {
                                drop(ret_type);
                                throw_error(scope, "Function already has a ret type");
                                return;
                            }
                            *ret_type = Some(ret.0);
                        }
                        {
                            let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
                            if impl_expr.is_some() {
                                drop(impl_expr);
                                throw_error(scope, "Function already has an implementation");
                                return;
                            }
                            *impl_expr = Some(impl_body.0);
                        }
                    },
                },
            ],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceObjectMethodResult",
            builtin_unique_id: 3004,
            data_fields: vec![("method", "Rc<SourceDataMethodData>")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![
                //. .
                PushMethod {
                    name: "impl_",
                    args: vec![("arg", "AnyType"), ("ret", "AnyType"), ("impl_body", "StagedExpr")],
                    ret: "()",
                    body: quote!{
                        {
                            let mut arg_type = self_.method.arg_type.borrow_mut();
                            if arg_type.is_some() {
                                drop(arg_type);
                                throw_error(scope, "Method already has an arg type");
                                return;
                            }
                            *arg_type = Some(arg.0);
                        }
                        {
                            let mut ret_type = self_.method.ret_type.borrow_mut();
                            if ret_type.is_some() {
                                drop(ret_type);
                                throw_error(scope, "Method already has a ret type");
                                return;
                            }
                            *ret_type = Some(ret.0);
                        }
                        {
                            let mut impl_expr = self_.method.impl_expr.borrow_mut();
                            if impl_expr.is_some() {
                                drop(impl_expr);
                                throw_error(scope, "Method already has an implementation");
                                return;
                            }
                            *impl_expr = Some(impl_body.0);
                        }
                    },
                },
            ],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceInterfaceResult",
            builtin_unique_id: 3005,
            data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>")],
            extra_aspect_fn: None,
            fields: vec![PushField {
                name: "type_",
                ret: "Rc<SourceInterfaceRef>",
                body: quote!{
                    self_.interface_ref.clone()
                },
            }],
            methods: vec![
                //. .
                PushMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.interface_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                PushMethod {
                    name: "func",
                    args: vec![("name", "String")],
                    ret: "SourceInterfaceMethodResult",
                    body: quote!{
                        let iface_data = &self_.interface_ref.data;
                        let field_name =
                            format!(
                                "_im{}",
                                crate::eval::definition_set::next_interface_method_id(
                                    iface_data.set.borrow().as_ref().unwrap()
                                )
                            );
                        let method = Rc::new(SourceInterfaceMethodData {
                            name,
                            arg_type: RefCell::new(None),
                            ret_type: RefCell::new(None),
                            field_name,
                        });
                        iface_data.methods.borrow_mut().push(method.clone());
                        SourceInterfaceMethodResult { method, }
                    },
                },
                PushMethod {
                    name: "set_is_aspect",
                    args: vec![("is_aspect", "bool")],
                    ret: "()",
                    body: quote!{
                        self_.interface_ref.data.is_aspect.set(is_aspect);
                    },
                },
            ],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceObjectResult",
            builtin_unique_id: 3006,
            data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>")],
            extra_aspect_fn: None,
            fields: vec![
                //. .
                PushField {
                    name: "type_ref_",
                    ret: "Rc<SourceDataTypeRef>",
                    body: quote!{
                        self_.data_type_ref.clone()
                    },
                },
                PushField {
                    name: "new_",
                    ret: "Rc<TypeSourceDataTypeConstructorConst>",
                    body: quote!{
                        Rc::new(TypeSourceDataTypeConstructorConst { data: self_.data_type_ref.data.clone(), })
                    },
                },
            ],
            methods: vec![
                //. .
                PushMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.data_type_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                PushMethod {
                    name: "set_inner",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        self_.data_type_ref.data.underlying_type.set(type_ref.0);
                    },
                },
                PushMethod {
                    name: "func",
                    args: vec![("name", "String")],
                    ret: "SourceObjectMethodResult",
                    body: quote!{
                        let dt_data = &self_.data_type_ref.data;
                        let ds = dt_data.set.borrow().as_ref().unwrap().clone();
                        let field_name = format!("_{}", ds.next_field_id.next());
                        let entry_id = ds.next_entry_id.next();
                        let method = Rc::new(SourceDataMethodData {
                            name,
                            arg_type: RefCell::new(None),
                            impl_expr: RefCell::new(None),
                            ret_type: RefCell::new(None),
                            scope_entry_id: Cell::new(
                                Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, entry_id,),)
                            ),
                            inner_scope_entry_id: Cell::new(None),
                            field_name,
                            compiled_js: RefCell::new(None),
                            captured_externals: RefCell::new(Vec::new()),
                        });
                        dt_data.methods.borrow_mut().push(method.clone());
                        SourceObjectMethodResult { method, }
                    },
                },
                PushMethod {
                    name: "impl_interface",
                    args: vec![
                        ("iface_ref", "Rc<SourceInterfaceRef>"),
                        ("record", "std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>"),
                    ],
                    ret: "()",
                    body: quote!{
                        let record_entries = record;
                        let iface_data = &iface_ref.data;
                        let iface_method_names: Vec<(usize, String)> = {
                            let methods = iface_data.methods.borrow();
                            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
                        };
                        let mut method_impls = Vec::new();
                        let dt_data = &self_.data_type_ref.data;
                        let ds = dt_data.set.borrow().as_ref().unwrap().clone();
                        for (imethod_idx, imethod_name) in &iface_method_names {
                            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
                            match record_entries.get(&const_key) {
                                Some(expr) => {
                                    let entry_id = ds.next_entry_id.next();
                                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                        artifact_id: RefCell::new(None),
                                        interface_method_index: *imethod_idx,
                                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                                        impl_expr_artifact: RefCell::new(None),
                                        compiled_js: RefCell::new(None),
                                        captured_externals: RefCell::new(Vec::new()),
                                        scope_entry_id: Cell::new(
                                            Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, entry_id,),)
                                        ),
                                        inner_scope_entry_id: Cell::new(None),
                                    });
                                },
                                None => {
                                    throw_error(
                                        scope,
                                        &format!("Missing implementation for interface method '{}'", imethod_name),
                                    );
                                    return;
                                },
                            }
                        }
                        dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
                            interface: iface_data.clone(),
                            method_impls,
                        });
                    },
                },
                PushMethod {
                    name: "aspect",
                    args: vec![
                        ("record", "std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>"),
                    ],
                    ret: "Rc<SourceInterfaceRef>",
                    body: quote!{
                        let record_entries = record;
                        let ds = self_.data_type_ref.data.set.borrow().as_ref().unwrap().clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let iface_data = Gc::new(SourceInterfaceData {
                            artifact_id: RefCell::new(None),
                            builtin_kind: None,
                            set: RefCell::new(Some(ds.clone())),
                            description: RefCell::new(None),
                            is_aspect: Cell::new(true),
                            methods: RefCell::new(Vec::new()),
                            identity: identity.clone(),
                        });
                        ds.interfaces.borrow_mut().push(iface_data.clone());
                        let iface_method_names: Vec<String> = {
                            record_entries.keys().map(|k| match k {
                                crate::langhelp::ConstKey::String(s) => s.clone(),
                                other => other.to_str(),
                            }).collect()
                        };
                        let mut methods = Vec::new();
                        let any_type: std::rc::Rc<dyn crate::langhelp::Type> =
                            std::rc::Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic);
                        let null_type: std::rc::Rc<dyn crate::langhelp::Type> =
                            std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic);
                        for imethod_name in &iface_method_names {
                            let field_name =
                                format!("_im{}", crate::eval::definition_set::next_interface_method_id(&ds));
                            methods.push(Rc::new(SourceInterfaceMethodData {
                                name: imethod_name.clone(),
                                arg_type: RefCell::new(Some(any_type.clone())),
                                ret_type: RefCell::new(Some(any_type.clone())),
                                field_name,
                            }));
                        }
                        let aspect_unique_field =
                            format!("_im{}", crate::eval::definition_set::next_interface_method_id(&ds));
                        methods.push(Rc::new(SourceInterfaceMethodData {
                            name: "aspect_id".to_string(),
                            arg_type: RefCell::new(Some(null_type.clone())),
                            ret_type: RefCell::new(
                                Some(
                                    std::rc::Rc::new(crate::eval::value::value_unique::TypeUnique) as
                                        std::rc::Rc<dyn crate::langhelp::Type>
                                )
                            ),
                            field_name: aspect_unique_field,
                        }));
                        *iface_data.methods.borrow_mut() = methods;
                        let dt_data = &self_.data_type_ref.data;
                        let ds = dt_data.set.borrow().as_ref().unwrap().clone();
                        let mut method_impls = Vec::new();
                        let all_method_names: Vec<String> =
                            iface_data.methods.borrow().iter().map(|m| m.name.clone()).collect();
                        for (imethod_idx, imethod_name) in all_method_names.iter().enumerate() {
                            if imethod_name == "aspect_id" {
                                let unique_clone = identity.clone();
                                let aspect_id_entry_id = ds.next_entry_id.next();
                                let unique_literal = crate::eval::expr::Expr {
                                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                                    r#type: crate::eval::expr::Expr_::Literal(
                                        crate::eval::expr::LitValue::Shared(std::rc::Rc::new(unique_clone)),
                                    ),
                                };
                                method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                    artifact_id: RefCell::new(None),
                                    interface_method_index: imethod_idx,
                                    impl_expr: RefCell::new(Some(Rc::new(unique_literal))),
                                    impl_expr_artifact: RefCell::new(None),
                                    compiled_js: RefCell::new(None),
                                    captured_externals: RefCell::new(Vec::new()),
                                    scope_entry_id: Cell::new(
                                        Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, aspect_id_entry_id)),
                                    ),
                                    inner_scope_entry_id: Cell::new(None),
                                });
                                continue;
                            }
                            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
                            match record_entries.get(&const_key) {
                                Some(expr) => {
                                    let entry_id = ds.next_entry_id.next();
                                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                        artifact_id: RefCell::new(None),
                                        interface_method_index: imethod_idx,
                                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                                        impl_expr_artifact: RefCell::new(None),
                                        compiled_js: RefCell::new(None),
                                        captured_externals: RefCell::new(Vec::new()),
                                        scope_entry_id: Cell::new(
                                            Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, entry_id)),
                                        ),
                                        inner_scope_entry_id: Cell::new(None),
                                    });
                                },
                                None => {
                                    throw_error(
                                        scope,
                                        &format!("Missing implementation for aspect method '{}'", imethod_name)
                                    );
                                    return;
                                },
                            }
                        }
                        dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
                            interface: iface_data.clone(),
                            method_impls,
                        });
                        Rc::new(SourceInterfaceRef { data: iface_data })
                    },
                },
            ],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceTaggedUnionBuilder",
            builtin_unique_id: 3007,
            data_fields: vec![("data", "Rc<SourceTaggedUnionData>")],
            extra_aspect_fn: None,
            fields: vec![PushField {
                name: "type_",
                ret: "Rc<SourceTaggedUnionRef>",
                body: quote!{
                    Rc::new(SourceTaggedUnionRef { data: self_.data.clone() })
                },
            }],
            methods: vec![PushMethod {
                name: "variant",
                args: vec![("tag", "String"), ("type_ref", "AnyType")],
                ret: "Rc<SourceTaggedUnionConstructorRef>",
                body: quote!{
                    let variant_index = {
                        let mut variants = self_.data.variants.borrow_mut();
                        let idx = variants.len();
                        let js_tag = format!("{}{}", crate::code_dynamic::TAG_PREFIX, self_.data.next_tag_id.next(),);
                        variants.push(SourceTaggedUnionVariant {
                            js_tag,
                            tag: crate::langhelp::ConstKey::String(tag),
                            type_: type_ref.0,
                            constructor_artifact_id: RefCell::new(None),
                        });
                        idx
                    };
                    Rc::new(SourceTaggedUnionConstructorRef {
                        data: self_.data.clone(),
                        variant_index,
                    })
                },
            }],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
        PushStructSpec {
            name: "SourceInterfaceMethodResult",
            builtin_unique_id: 3008,
            data_fields: vec![("method", "Rc<SourceInterfaceMethodData>")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![PushMethod {
                name: "impl_",
                args: vec![("arg", "AnyType"), ("ret", "AnyType")],
                ret: "()",
                body: quote!{
                    {
                        let mut arg_type = self_.method.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Interface method already has an arg type");
                            return;
                        }
                        *arg_type = Some(arg.0);
                    }
                    {
                        let mut ret_type = self_.method.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            throw_error(scope, "Interface method already has a ret type");
                            return;
                        }
                        *ret_type = Some(ret.0);
                    }
                },
            }],
            extra_dispatch_arms: vec![],
            extra_method_setters: vec![],
            extra_field_type_opt_arms: vec![],
        },
    ];
    for spec in specs.iter_mut() {
        if spec.name == "SourceObjectResult" {
            spec.extra_dispatch_arms = source_object_extra_dispatch.clone();
            spec.extra_method_setters = source_object_extra_setters.clone();
            spec.extra_field_type_opt_arms = source_object_extra_field_type_opt.clone();
        }
    }
    for s in &specs {
        let name = format_ident!("{}", s.name);
        let type_name = format_ident!("Type{}Dynamic", s.name);
        let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
        let builtin_unique_id = s.builtin_unique_id;
        assert!(builtin_unique_ids.insert(builtin_unique_id));
        let desc_str = s.name.to_string();
        let data_field_decls: Vec<TokenStream> = s.data_fields.iter().map(|(n, ty)| {
            let n = format_ident!("{}", n);
            let ty: TokenStream = ty.parse().unwrap();
            quote!{
                pub #n: #ty
            }
        }).collect();
        let field_dispatch_arms: Vec<TokenStream> = s.fields.iter().enumerate().map(|(idx, f)| {
            let f_name = f.name;
            let f_ty: TokenStream = f.ret.parse().unwrap();
            let idx_str = format!("_{}", idx);
            quote!{
                if key_name == crate:: eval:: push:: push_method_name(#f_name) {
                    let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = <#f_ty as crate:: eval:: push:: Push >:: dynamic_type(
                    );
                    let self_code =
                        erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                    return Ok(crate:: langhelp:: CodeValue {
                        code: crate:: langhelp:: Code {
                            loc,
                            code: crate:: langhelp:: Code_:: Dyn(
                                std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                                    loc,
                                    type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                        crate:: code_dynamic:: CodeDynamicFieldUse {
                                            base: Box:: new(self_code),
                                            field: #idx_str.to_string(),
                                        },
                                    ),
                                },)
                            ),
                        },
                        value: std:: rc:: Rc:: new(crate::eval::value::value_dynamic::ValueDynamic {
                            source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                            type_: field_type,
                        }).into(),
                    });
                }
            }
        }).collect();
        let method_dispatch_arms: Vec<TokenStream> = s.methods.iter().enumerate().map(|(i, m)| {
            let m_name = m.name;
            let idx = s.fields.len() + i;
            let idx_str = format!("_{}", idx);
            let arg_dynamic_type = arg_dynamic_type_expr_str(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote!{
                if key_name == crate:: eval:: push:: push_method_name(#m_name) {
                    let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = std:: rc:: Rc:: new(
                        crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                            arg: #arg_dynamic_type,
                            ret: <#m_ret as crate:: eval:: push:: Push >:: dynamic_type(),
                        },
                    );
                    let self_code =
                        erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                    return Ok(crate:: langhelp:: CodeValue {
                        code: crate:: langhelp:: Code {
                            loc,
                            code: crate:: langhelp:: Code_:: Dyn(
                                std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                                    loc,
                                    type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                        crate:: code_dynamic:: CodeDynamicFieldUse {
                                            base: Box:: new(self_code),
                                            field: #idx_str.to_string(),
                                        },
                                    ),
                                },)
                            ),
                        },
                        value: std:: rc:: Rc:: new(crate::eval::value::value_dynamic::ValueDynamic {
                            source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                            type_: field_type,
                        }).into(),
                    });
                }
            }
        }).collect();
        let field_type_opt_arms: Vec<TokenStream> = s.fields.iter().map(|f| {
            let f_name = f.name;
            let f_ty: TokenStream = f.ret.parse().unwrap();
            quote!{
                if key.as_str() == crate:: eval:: push:: push_method_name(#f_name) {
                    return Some(<#f_ty as crate:: eval:: push:: Push >:: dynamic_type());
                }
            }
        }).chain(s.methods.iter().map(|m| {
            let m_name = m.name;
            let arg_dynamic_type = arg_dynamic_type_expr_str(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote!{
                if key.as_str() == crate:: eval:: push:: push_method_name(#m_name) {
                    return Some(
                        std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                            arg: #arg_dynamic_type,
                            ret: <#m_ret as crate:: eval:: push:: Push >:: dynamic_type(),
                        },)
                    );
                }
            }
        })).collect();
        let field_setters: Vec<TokenStream> = s.fields.iter().enumerate().map(|(idx, f)| {
            let f_ty: TokenStream = f.ret.parse().unwrap();
            let body = &f.body;
            let key_str = format!("_{}", idx);
            quote!{
                {
                    #[allow(unused_variables)]
                    let self_ = _jw_self_rc.clone();
                    let _jw_field_val: #f_ty = {
                        #body
                    };
                    let _jw_v8_val = <#f_ty as crate:: eval:: push:: Push >:: into_v8(_jw_field_val, scope);
                    let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_v8_val);
                }
            }
        }).collect();
        let method_setters: Vec<TokenStream> = s.methods.iter().enumerate().map(|(i, m)| {
            let idx = s.fields.len() + i;
            let key_str = format!("_{}", idx);
            let arg_decls: Vec<TokenStream> = m.args.iter().map(|(n, ty)| {
                let n = format_ident!("{}", n);
                let ty: TokenStream = ty.parse().unwrap();
                quote!{
                    let #n: #ty = <#ty as crate:: eval:: push:: Push >:: from_v8(scope, _jw_args.get(_jw_arg_i),);
                    _jw_arg_i += 1;
                }
            }).collect();
            let m_ret: TokenStream = m.ret.parse().unwrap();
            let body = &m.body;
            quote!{
                {
                    fn _jw_cb <'s >(
                        scope: & mut v8:: PinnedRef <'s,
                        v8:: HandleScope <'_ >>,
                        _jw_args: v8:: FunctionCallbackArguments <'s >,
                        mut _jw_rv: v8:: ReturnValue <'s >,
                    ) {
                        let _jw_this = _jw_args.this();
                        let _jw_dk = v8::String::new(scope, "_d").unwrap();
                        #[
                            allow(unused_variables)
                        ] let self_: std:: rc:: Rc <#name > = crate:: eval:: from_v8_ref::< std:: rc:: Rc <#name >>(
                            &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                        ).clone();
                        #[allow(unused_mut, unused_variables)]
                        let mut _jw_arg_i = 0i32;
                        #(#arg_decls) * let _jw_result: #m_ret = {
                            #body
                        };
                        _jw_rv.set(<#m_ret as crate:: eval:: push:: Push >:: into_v8(_jw_result, scope),);
                    }
                    let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                    let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                }
            }
        }).collect();
        let extra_dispatch_arms = &s.extra_dispatch_arms;
        let extra_method_setters = &s.extra_method_setters;
        let extra_field_type_opt_arms = &s.extra_field_type_opt_arms;
        let no_fields_msg = format!("Cannot convert {}", s.name);
        let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
        let from_v8_msg = format!("from_v8 not supported for {}", s.name);
        let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);
        let extra_aspect_call: TokenStream = match s.extra_aspect_fn {
            Some(f) => {
                let f: TokenStream = f.parse().unwrap();
                quote!{
                    #f(_xx, key)
                }
            },
            None => quote!{
                None
            },
        };
        out.extend(quote!{
            pub(crate) struct #name {
                #(#data_field_decls,) *
            }
            pub(crate) struct #type_name;
            fn #identity_fn() -> crate:: langhelp:: Unique {
                crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
            }
            impl crate:: artifact:: ExporterAny for #type_name {
                fn as_type(&self) -> crate::langhelp::EResult<std::rc::Rc<dyn crate::langhelp::Type>> {
                    unimplemented!()
                }

                fn as_value(&self) -> crate::langhelp::EResult<std::rc::Rc<dyn crate::langhelp::Value_>> {
                    unimplemented!()
                }
            }
            impl crate:: langhelp:: Value_ for #type_name {
                fn as_exporter_any(&self) -> std::rc::Rc<dyn crate::artifact::ExporterAny> {
                    unimplemented!()
                }

                fn type_(
                    &self,
                    _xx: &crate::langhelp::ExprContext,
                    _loc: crate::langhelp::QExprId,
                ) -> crate::langhelp::EResult<std::rc::Rc<dyn crate::langhelp::Type>> {
                    crate::langhelp::EResult::ok(
                        std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
                    )
                }
            }
            impl crate:: langhelp:: Aspect for #type_name {
            }
            impl crate:: langhelp:: Type for #type_name {
                fn desc(&self) -> String {
                    #desc_str.to_string()
                }
                fn as_value(&self) -> std:: rc:: Rc < dyn crate:: langhelp:: Value_ > {
                    std:: rc:: Rc:: new(#type_name)
                }
                fn aspect(
                    &self,
                    _xx: & crate:: langhelp:: ExprContext,
                    key: & crate:: langhelp:: Unique,
                ) -> Option < Box < dyn std:: any:: Any >> {
                    if * key == #identity_fn() {
                        return Some(Box::new(true));
                    }
                    if * key == crate:: r#gen:: aspect_field_aspect_id() {
                        return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectField >));
                    }
                    if * key == crate:: r#gen:: aspect_consume_dynamic_aspect_id() {
                        return Some(
                            Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectConsumeDynamic >)
                        );
                    }
                    if * key == crate:: r#gen:: aspect_bind_dynamic_aspect_id() {
                        return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBindDynamic >));
                    }
                    if * key == crate:: r#gen:: aspect_bind_aspect_id() {
                        return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBind >));
                    }
                    if * key == crate:: r#gen:: aspect_stack_value_dynamic_aspect_id() {
                        return Some(
                            Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectStackValueDynamic >)
                        );
                    }
                    #extra_aspect_call
                }
                fn cast_to(
                    &self,
                    _xx: & crate:: langhelp:: ExprContext,
                    loc: crate:: langhelp:: QExprId,
                    _cv: crate:: langhelp:: CodeValue,
                    _type_: & dyn crate:: langhelp:: Type,
                ) -> crate:: langhelp:: EvalRes {
                    crate:: langhelp:: EvalRes:: err(loc, #no_fields_msg.to_string())
                }
                fn can_cast_from(
                    &self,
                    xx: & crate:: langhelp:: ExprContext,
                    _loc: crate:: langhelp:: QExprId,
                    source_type: & dyn crate:: langhelp:: Type,
                ) -> bool {
                    source_type.aspect(xx, &#identity_fn()).is_some()
                }
                fn cast_from(
                    &self,
                    xx: & crate:: langhelp:: ExprContext,
                    loc: crate:: langhelp:: QExprId,
                    cv: crate:: langhelp:: CodeValue,
                ) -> crate:: langhelp:: EvalRes {
                    let source_type = match cv.value.type_(xx, loc).into_res() {
                        Ok(v) => v,
                        Err(errors) => return crate::langhelp::EvalRes {
                            errors: Some(errors),
                            v: None
                        },
                    };
                    if source_type.aspect(xx, &#identity_fn()).is_some() {
                        return crate:: langhelp:: EvalRes:: ok(crate:: langhelp:: CodeValue {
                            code: cv.code,
                            value: std:: rc:: Rc:: new(crate:: eval:: value:: value_dynamic:: ValueDynamic {
                                source: crate:: eval:: value:: value_dynamic:: ValueDynamicSource:: Expr,
                                type_: std:: rc:: Rc:: new(#type_name),
                            }).into(),
                        });
                    }
                    crate:: langhelp:: EvalRes:: err(loc, #cannot_convert_to_msg.to_string())
                }
            }
            impl crate:: r#gen:: AspectConsumeDynamic for #type_name {
                fn to_dynamic(
                    &self,
                    _xx: &crate::langhelp::ExprContext,
                    _loc: crate::langhelp::QExprId,
                    cv: crate::langhelp::CodeValue,
                ) -> crate::langhelp::EResult<crate::code_dynamic::CodeDynamic> {
                    cv.code.cast::<crate::code_dynamic::CodeDynamic>().map(|x| x.unwrap().clone())
                }
            }
            impl crate:: r#gen:: AspectBindDynamic for #type_name {
                fn bind_dynamic(
                    &self,
                    xx: &crate::langhelp::ExprContext,
                    loc: crate::langhelp::QExprId,
                    cv: crate::langhelp::CodeValue,
                ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
                    crate::eval::dynamic_util::bind_dynamic_default(xx, loc, cv)
                }
            }
            impl crate:: r#gen:: AspectBind for #type_name {
                fn bind(
                    &self,
                    xx: &crate::langhelp::ExprContext,
                    loc: crate::langhelp::QExprId,
                    cv: crate::langhelp::CodeValue,
                ) -> crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
                    crate::eval::dynamic_util::bind_via_target(xx, loc, cv)
                }
            }
            impl crate:: r#gen:: AspectStackValueDynamic for #type_name {
                fn to_value(
                    &self,
                    loc: crate:: langhelp:: QExprId,
                    value: crate:: code_dynamic:: CodeDynamic,
                ) -> crate:: langhelp:: CodeValue {
                    crate:: langhelp:: CodeValue {
                        code: crate:: langhelp:: Code {
                            loc,
                            code: crate:: langhelp:: Code_:: Dyn(std::rc::Rc::new(value)),
                        },
                        value: std:: rc:: Rc:: new(crate:: eval:: value:: value_dynamic:: ValueDynamic {
                            source: crate:: eval:: value:: value_dynamic:: ValueDynamicSource:: Expr,
                            type_: std:: rc:: Rc:: new(#type_name),
                        }).into(),
                    }
                }
            }
            impl crate:: r#gen:: AspectField for #type_name {
                fn field(
                    &self,
                    xx: &crate::langhelp::ExprContext,
                    loc: crate::langhelp::QExprId,
                    self_: crate::langhelp::CodeValue,
                    key: crate::langhelp::CodeValue,
                ) -> crate::langhelp::EvalRes {
                    crate::er_seq_cv!(loc, erb, {
                        let key_type = erb.must_er(key.value.type_(xx, loc))?;
                        let key_string =
                            erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                        let key_name = key_string.get(key.value.as_ref());
                        #(
                            #field_dispatch_arms
                        ) * #(
                            #method_dispatch_arms
                        ) * #(#extra_dispatch_arms) * return erb.err_new(format!(#unknown_field_msg, key_name));
                    })
                }
                fn field_type_opt(
                    &self,
                    _xx: & crate:: langhelp:: ExprContext,
                    key: & crate:: langhelp:: ConstKey,
                ) -> Option < std:: rc:: Rc < dyn crate:: langhelp:: Type >> {
                    let crate::langhelp::ConstKey::String(key) = key else {
                        return None;
                    };
                    #(#field_type_opt_arms) * #(#extra_field_type_opt_arms) * None
                }
            }
            impl crate:: eval:: push:: Push for #name {
                fn dynamic_type() -> std:: rc:: Rc < dyn crate:: langhelp:: Type > {
                    std:: rc:: Rc:: new(#type_name)
                }
                fn from_v8<
                    's
                >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>,) -> Self {
                    unimplemented!(#from_v8_msg)
                }
                fn into_v8 <'s >(self, scope: & mut v8:: PinnedRef <'s, v8:: HandleScope <'_ >>,) -> v8:: Local <'s,
                v8:: Value > {
                    let _jw_obj = v8::Object::new(scope);
                    let _jw_self_rc = std::rc::Rc::new(self);
                    let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                    _jw_obj.set(scope, v8::String::new(scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                    #(#field_setters) * #(#method_setters) * #(#extra_method_setters) * _jw_obj.into()
                }
            }
        });
    }
}

fn gen_builtin_impl_body(iface_field: &str) -> TokenStream {
    let iface_field = format_ident!("{}", iface_field);
    quote!{
        let record_entries = record;
        let import_id = self_.data_type_ref.data.set.borrow().as_ref().unwrap().def_set_id.0.0;
        let builtin_ifaces = crate::r#gen::gen_build_builtin_interfaces(import_id);
        let iface_data = builtin_ifaces.#iface_field.clone();
        let iface_method_names: Vec<(usize, String)> = {
            let methods = iface_data.methods.borrow();
            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
        };
        let mut method_impls = Vec::new();
        let dt_data = &self_.data_type_ref.data;
        let ds = dt_data.set.borrow().as_ref().unwrap().clone();
        for (imethod_idx, imethod_name) in &iface_method_names {
            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
            match record_entries.get(&const_key) {
                Some(expr) => {
                    let entry_id = ds.next_entry_id.next();
                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                        artifact_id: RefCell::new(None),
                        interface_method_index: *imethod_idx,
                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                        impl_expr_artifact: RefCell::new(None),
                        compiled_js: RefCell::new(None),
                        captured_externals: RefCell::new(Vec::new()),
                        scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(ds.def_set_id, entry_id)),),
                        inner_scope_entry_id: Cell::new(None),
                    });
                },
                None => {
                    throw_error(
                        scope,
                        &format!("Missing implementation for builtin interface method '{}'", imethod_name)
                    );
                    return;
                },
            }
        }
        dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
            interface: iface_data,
            method_impls,
        });
    }
}
