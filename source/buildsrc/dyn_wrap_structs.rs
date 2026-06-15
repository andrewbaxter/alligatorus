use {
    crate::buildsrc::util::{
        DynWrapMethod,
        arg_mortar_expr_str,
        snake_case,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>) {
    struct DynWrapField {
        body: TokenStream,
        name: &'static str,
        ret: &'static str,
    }

    struct DynWrapStructSpec {
        builtin_unique_id: usize,
        data_fields: Vec<(&'static str, &'static str)>,
        extra_aspect_fn: Option<&'static str>,
        fields: Vec<DynWrapField>,
        methods: Vec<DynWrapMethod>,
        name: &'static str,
    }

    for s in &vec![
        //. .
        DynWrapStructSpec {
            name: "DefinitionSetBuilder",
            builtin_unique_id: 3009,
            data_fields: vec![("inner", "Gc<DefinitionSet>")],
            extra_aspect_fn: Some("crate::eval::definition_set::definition_set_extra_aspect"),
            fields: vec![],
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewFuncResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let entry_id = ds.next_entry_id.next();
                        let func_data = Gc::new(SourceFunctionData {
                            set: ds.clone(),
                            name: Some(name),
                            description: RefCell::new(None),
                            impl_expr: RefCell::new(None),
                            arg_type: RefCell::new(None),
                            ret_type: RefCell::new(None),
                            scope_entry_id: Cell::new(
                                Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,),)
                            ),
                            artifact_id: RefCell::new(None),
                            compiled_js: RefCell::new(None),
                            captured_externals: RefCell::new(Vec::new()),
                        });
                        ds.functions.borrow_mut().push(func_data.clone());
                        let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data });
                        SourceNewFuncResult { func_ref }
                    },
                },
                DynWrapMethod {
                    name: "new_data",
                    args: vec![("name", "String")],
                    ret: "SourceNewDataResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data_field = format!("_{}", ds.next_field_id.next());
                        let dt_data = Gc::new(SourceDataTypeData {
                            set: ds.clone(),
                            name: Some(name),
                            description: RefCell::new(None),
                            underlying_type: crate::invariant::DeserialLazy::new(),
                            methods: RefCell::new(Vec::new()),
                            interface_impls: RefCell::new(Vec::new()),
                            identity,
                            data_field,
                            artifact_id: RefCell::new(None),
                        });
                        ds.data_types.borrow_mut().push(dt_data.clone());
                        let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data });
                        SourceNewDataResult { data_type_ref }
                    },
                },
                DynWrapMethod {
                    name: "build",
                    args: vec![],
                    ret: "DefinitionSetBuilder",
                    body: quote!{
                        DefinitionSetBuilder { inner: self_.inner.clone() }
                    },
                },
                DynWrapMethod {
                    name: "tagged_union",
                    args: vec![],
                    ret: "SourceTaggedUnionBuilder",
                    body: quote!{
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data = Rc::new(SourceTaggedUnionData {
                            variants: RefCell::new(Vec::new()),
                            identity,
                            next_tag_id: crate::langhelp::Counter::default(),
                        });
                        SourceTaggedUnionBuilder { data }
                    },
                },
                DynWrapMethod {
                    name: "new_interface",
                    args: vec![],
                    ret: "SourceNewInterfaceResult",
                    body: quote!{
                        let ds = self_.inner.clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let iface_data = Gc::new(SourceInterfaceData {
                            builtin_kind: None,
                            set: ds.clone(),
                            description: RefCell::new(None),
                            is_aspect: Cell::new(false),
                            methods: RefCell::new(Vec::new()),
                            identity,
                        });
                        ds.interfaces.borrow_mut().push(iface_data.clone());
                        let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data });
                        SourceNewInterfaceResult { interface_ref }
                    },
                },
            ],
        },
        DynWrapStructSpec {
            name: "OptionV8",
            builtin_unique_id: 3001,
            data_fields: vec![("is_some_inner", "bool")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![DynWrapMethod {
                name: "is_some_",
                args: vec![],
                ret: "bool",
                body: quote!{
                    self_.is_some_inner
                },
            }],
        },
        DynWrapStructSpec {
            name: "TypeRefV8",
            builtin_unique_id: 3002,
            data_fields: vec![("type_ref", "Rc<dyn crate::langhelp::Type>")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![DynWrapMethod {
                name: "aspect",
                args: vec![("unique", "Unique")],
                ret: "OptionV8",
                body: quote!{
                    let ex_slot =
                        scope
                            .get_slot::<crate::eval::V8EvalContextSlot>()
                            .expect("TypeRefV8.aspect: V8EvalContextSlot not set");
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
                    OptionV8 { is_some_inner: is_some }
                },
            }],
        },
        DynWrapStructSpec {
            name: "SourceNewFuncResult",
            builtin_unique_id: 3003,
            data_fields: vec![("func_ref", "Rc<TypeSourceFunctionConst>")],
            extra_aspect_fn: None,
            fields: vec![DynWrapField {
                name: "func",
                ret: "Rc<TypeSourceFunctionConst>",
                body: quote!{
                    self_.func_ref.clone()
                },
            }],
            //. .
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.func_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "impl_",
                    args: vec![("staged", "StagedExpr")],
                    ret: "()",
                    body: quote!{
                        let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
                        if impl_expr.is_some() {
                            drop(impl_expr);
                            throw_error(scope, "Function already has an implementation");
                            return;
                        }
                        *impl_expr = Some(staged.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_type",
                    args: vec![("iface_ref", "Rc<SourceInterfaceRef>")],
                    ret: "()",
                    body: quote!{
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type =
                            Some(
                                Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() },) as
                                    Rc<dyn crate::langhelp::Type>
                            );
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            throw_error(scope, "Function already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
            ],
        },
        DynWrapStructSpec {
            name: "SourceNewDataMethodResult",
            builtin_unique_id: 3004,
            data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>"), ("method_index", "usize")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "impl_",
                    args: vec![("staged", "StagedExpr")],
                    ret: "()",
                    body: quote!{
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut impl_expr = method.impl_expr.borrow_mut();
                        if impl_expr.is_some() {
                            drop(impl_expr);
                            drop(methods);
                            throw_error(scope, "Method already has an implementation");
                            return;
                        }
                        *impl_expr = Some(staged.0);
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut ret_type = method.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            drop(methods);
                            throw_error(scope, "Method already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut arg_type = method.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            drop(methods);
                            throw_error(scope, "Method already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
            ],
        },
        DynWrapStructSpec {
            name: "SourceNewInterfaceResult",
            builtin_unique_id: 3005,
            data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>")],
            extra_aspect_fn: None,
            fields: vec![DynWrapField {
                name: "type_",
                ret: "Rc<SourceInterfaceRef>",
                body: quote!{
                    self_.interface_ref.clone()
                },
            }],
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.interface_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewInterfaceMethodResult",
                    body: quote!{
                        let iface_data = &self_.interface_ref.data;
                        let field_name =
                            format!("_im{}", crate::eval::definition_set::next_interface_method_id(&iface_data.set));
                        let method_index = {
                            let mut methods = iface_data.methods.borrow_mut();
                            let idx = methods.len();
                            methods.push(SourceInterfaceMethodData {
                                name,
                                arg_type: RefCell::new(None),
                                ret_type: RefCell::new(None),
                                field_name,
                            });
                            idx
                        };
                        SourceNewInterfaceMethodResult {
                            interface_ref: self_.interface_ref.clone(),
                            method_index,
                        }
                    },
                },
                DynWrapMethod {
                    name: "set_is_aspect",
                    args: vec![("is_aspect", "bool")],
                    ret: "()",
                    body: quote!{
                        self_.interface_ref.data.is_aspect.set(is_aspect);
                    },
                },
            ],
        },
        DynWrapStructSpec {
            name: "SourceNewDataResult",
            builtin_unique_id: 3006,
            data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>")],
            extra_aspect_fn: None,
            fields: vec![
                //. .
                DynWrapField {
                    name: "type_",
                    ret: "Rc<SourceDataTypeRef>",
                    body: quote!{
                        self_.data_type_ref.clone()
                    },
                },
                DynWrapField {
                    name: "new_",
                    ret: "Rc<TypeSourceDataTypeConstructorConst>",
                    body: quote!{
                        Rc::new(TypeSourceDataTypeConstructorConst { data: self_.data_type_ref.data.clone(), })
                    },
                },
            ],
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.data_type_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "set_underlying",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        self_.data_type_ref.data.underlying_type.set(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewDataMethodResult",
                    body: quote!{
                        let dt_data = &self_.data_type_ref.data;
                        let field_name = format!("_{}", dt_data.set.next_field_id.next());
                        let entry_id = dt_data.set.next_entry_id.next();
                        let method_index = {
                            let mut methods = dt_data.methods.borrow_mut();
                            let idx = methods.len();
                            methods.push(SourceDataMethodData {
                                name,
                                arg_type: RefCell::new(None),
                                impl_expr: RefCell::new(None),
                                ret_type: RefCell::new(None),
                                scope_entry_id: Cell::new(
                                    Some(
                                        crate::langhelp::ScopeEntryId::Def(
                                            dt_data.set.import_id,
                                            dt_data.set.def_set_id,
                                            entry_id,
                                        ),
                                    )
                                ),
                                self_scope_entry_id: Cell::new(None),
                                field_name,
                                compiled_js: RefCell::new(None),
                                captured_externals: RefCell::new(Vec::new()),
                            });
                            idx
                        };
                        SourceNewDataMethodResult {
                            data_type_ref: self_.data_type_ref.clone(),
                            method_index,
                        }
                    },
                },
                DynWrapMethod {
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
                        for (imethod_idx, imethod_name) in &iface_method_names {
                            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
                            match record_entries.get(&const_key) {
                                Some(expr) => {
                                    let entry_id = dt_data.set.next_entry_id.next();
                                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                        interface_method_index: *imethod_idx,
                                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                                        impl_expr_artifact: RefCell::new(None),
                                        compiled_js: RefCell::new(None),
                                        captured_externals: RefCell::new(Vec::new()),
                                        scope_entry_id: Cell::new(
                                            Some(
                                                crate::langhelp::ScopeEntryId::Def(
                                                    dt_data.set.import_id,
                                                    dt_data.set.def_set_id,
                                                    entry_id,
                                                ),
                                            )
                                        ),
                                        self_scope_entry_id: Cell::new(None),
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
            ],
        },
        DynWrapStructSpec {
            name: "SourceTaggedUnionBuilder",
            builtin_unique_id: 3007,
            data_fields: vec![("data", "Rc<SourceTaggedUnionData>")],
            extra_aspect_fn: None,
            fields: vec![DynWrapField {
                name: "type_",
                ret: "Rc<SourceTaggedUnionRef>",
                body: quote!{
                    Rc::new(SourceTaggedUnionRef { data: self_.data.clone() })
                },
            }],
            methods: vec![DynWrapMethod {
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
                        });
                        idx
                    };
                    Rc::new(SourceTaggedUnionConstructorRef {
                        data: self_.data.clone(),
                        variant_index,
                    })
                },
            }],
        },
        DynWrapStructSpec {
            name: "SourceNewInterfaceMethodResult",
            builtin_unique_id: 3008,
            data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>"), ("method_index", "usize")],
            extra_aspect_fn: None,
            fields: vec![],
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let methods = self_.interface_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut arg_type = method.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            drop(methods);
                            throw_error(scope, "Interface method already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let methods = self_.interface_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut ret_type = method.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            drop(methods);
                            throw_error(scope, "Interface method already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
            ],
        },
    ] {
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
                if key_name == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#f_name) {
                    let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = <#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(
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
                        value: std:: rc:: Rc:: new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: field_type,
                        }),
                    });
                }
            }
        }).collect();
        let method_dispatch_arms: Vec<TokenStream> = s.methods.iter().enumerate().map(|(i, m)| {
            let m_name = m.name;
            let idx = s.fields.len() + i;
            let idx_str = format!("_{}", idx);
            let arg_mortar = arg_mortar_expr_str(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote!{
                if key_name == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                    let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = std:: rc:: Rc:: new(
                        crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
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
                        value: std:: rc:: Rc:: new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: field_type,
                        }),
                    });
                }
            }
        }).collect();
        let field_type_opt_arms: Vec<TokenStream> = s.fields.iter().map(|f| {
            let f_name = f.name;
            let f_ty: TokenStream = f.ret.parse().unwrap();
            quote!{
                if key.as_str() == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#f_name) {
                    return Some(<#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type());
                }
            }
        }).chain(s.methods.iter().map(|m| {
            let m_name = m.name;
            let arg_mortar = arg_mortar_expr_str(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote!{
                if key.as_str() == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                    return Some(
                        std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
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
                    let _jw_v8_val = <#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(
                        _jw_field_val,
                        scope
                    );
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
                    let #n: #ty = <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: from_v8(
                        scope,
                        _jw_args.get(_jw_arg_i),
                    );
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
                        _jw_rv.set(
                            <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(_jw_result, scope),
                        );
                    }
                    let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                    let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                }
            }
        }).collect();
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
            impl crate:: langhelp:: Value for #type_name {
                fn type_(
                    &self,
                    _xx: &crate::langhelp::ExprContext,
                    _loc: crate::langhelp::QExprId,
                ) -> std::rc::Rc<dyn crate::langhelp::Type> {
                    std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
                }
            }
            impl crate:: langhelp:: Aspect for #type_name {
            }
            impl crate:: langhelp:: Type for #type_name {
                fn desc(&self) -> String {
                    #desc_str.to_string()
                }
                fn as_value(&self) -> std:: rc:: Rc < dyn crate:: langhelp:: Value > {
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
                    let source_type = cv.value.type_(xx, loc);
                    if source_type.aspect(xx, &#identity_fn()).is_some() {
                        return crate:: langhelp:: EvalRes:: ok(crate:: langhelp:: CodeValue {
                            code: cv.code,
                            value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                                source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                                type_: std:: rc:: Rc:: new(#type_name),
                            }),
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
                        value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                            source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                            type_: std:: rc:: Rc:: new(#type_name),
                        }),
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
                        let key_type = key.value.type_(xx, loc);
                        let key_string =
                            erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                        let key_name = key_string.get(key.value.as_ref());
                        #(
                            #field_dispatch_arms
                        ) * #(#method_dispatch_arms) * return erb.err_new(format!(#unknown_field_msg, key_name));
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
                    #(#field_type_opt_arms) * None
                }
            }
            impl crate:: eval:: dynamic_wrap:: DynamicWrap for #name {
                fn mortar_type() -> std:: rc:: Rc < dyn crate:: langhelp:: Type > {
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
                    #(#field_setters) * #(#method_setters) * _jw_obj.into()
                }
            }
        });
    }
}
