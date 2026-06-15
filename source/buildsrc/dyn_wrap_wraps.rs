use {
    crate::buildsrc::util::{
        DynWrapMethod,
        arg_mortar_expr,
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
    struct DynWrapWrapSpec {
        builtin_unique_id: usize,
        methods: Vec<DynWrapMethod>,
        name: &'static str,
        wrapped_type: &'static str,
    }

    for s in &vec![
        //. .
        DynWrapWrapSpec {
            name: "DefinitionSet",
            builtin_unique_id: 4001,
            wrapped_type: "Gc<DefinitionSet>",
            methods: vec![
                //. .
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewFuncResult",
                    body: quote!{
                        let ds = (*self_).clone();
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
                        let ds = (*self_).clone();
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
                    ret: "Gc<DefinitionSet>",
                    body: quote!{
                        (*self_).clone()
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
                        let ds = (*self_).clone();
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
    ] {
        let type_name = format_ident!("Type{}Dynamic", s.name);
        let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
        let extra_aspect_fn = format_ident!("{}_extra_aspect", snake_case(s.name));
        let builtin_unique_id = s.builtin_unique_id;
        assert!(builtin_unique_ids.insert(builtin_unique_id));
        let desc_str = s.name.to_string();
        let wrapped: TokenStream = s.wrapped_type.parse().unwrap();
        let method_dispatch_arms: Vec<TokenStream> = s.methods.iter().enumerate().map(|(idx, m)| {
            let m_name = m.name;
            let idx_str = format!("_{}", idx);
            let arg_mortar = arg_mortar_expr(&m.args);
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
        let field_type_opt_arms: Vec<TokenStream> = s.methods.iter().map(|m| {
            let m_name = m.name;
            let arg_mortar = arg_mortar_expr(&m.args);
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
        }).collect();
        let method_setters: Vec<TokenStream> = s.methods.iter().enumerate().map(|(idx, m)| {
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
                        ] let self_: std:: rc:: Rc <#wrapped > = crate:: eval:: from_v8_ref::< std:: rc:: Rc <#wrapped >>(
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
        let cannot_convert_msg = format!("Cannot convert {}", s.name);
        let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);
        let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
        let from_v8_msg = format!("from_v8 not supported for {}", s.name);
        out.extend(quote!{
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
                        return Some(
                            Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBindDynamic >)
                        );
                    }
                    if * key == crate:: r#gen:: aspect_bind_aspect_id() {
                        return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBind >));
                    }
                    if * key == crate:: r#gen:: aspect_stack_value_dynamic_aspect_id() {
                        return Some(
                            Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectStackValueDynamic >)
                        );
                    }
                    crate:: eval:: definition_set::#extra_aspect_fn(_xx, key)
                }
                fn cast_to(
                    &self,
                    _xx: & crate:: langhelp:: ExprContext,
                    loc: crate:: langhelp:: QExprId,
                    _cv: crate:: langhelp:: CodeValue,
                    _type_: & dyn crate:: langhelp:: Type,
                ) -> crate:: langhelp:: EvalRes {
                    crate:: langhelp:: EvalRes:: err(loc, #cannot_convert_msg.to_string())
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
                        #(#method_dispatch_arms) * return erb.err_new(format!(#unknown_field_msg, key_name));
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
            impl crate:: eval:: dynamic_wrap:: DynamicWrap for #wrapped {
                fn mortar_type() -> std:: rc:: Rc < dyn crate:: langhelp:: Type > {
                    std:: rc:: Rc:: new(#type_name)
                }
                fn from_v8<
                    's
                >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>,) -> Self {
                    unimplemented!(#from_v8_msg)
                }
                fn into_v8 <'s >(
                    self,
                    scope: & mut v8:: PinnedRef <'s,
                    v8:: HandleScope <'_ >>,
                ) -> v8:: Local <'s,
                v8:: Value > {
                    let _jw_obj = v8::Object::new(scope);
                    let _jw_self_rc = std::rc::Rc::new(self);
                    let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                    _jw_obj.set(scope, v8::String::new(scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                    #(#method_setters) * _jw_obj.into()
                }
            }
        });
    }
}
