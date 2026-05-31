use {
    crate::langhelp::{
        Type,
        Value,
    },
    std::rc::Rc,
    super::expr,
};

pub(crate) trait JsWrap: Sized + 'static {
    fn mortar_type() -> Rc<dyn Type>;
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self;
    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value>;
}

impl JsWrap for String {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(super::mortar_string::TypeMortarString)
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.to_string(scope).unwrap().to_rust_string_lossy(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::String::new(scope, &self).unwrap().into()
    }
}

impl JsWrap for () {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(super::mortar_null::TypeMortarNull)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {

    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::null(scope).into()
    }
}

/// Wrapper for staged expressions received from JS. Extracts and consumes the
/// staged value from a StagedExprCell.
pub(crate) struct StagedExpr(pub Box<dyn Value>);

impl JsWrap for StagedExpr {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(expr::ExprType)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = super::rc_from_v8(&*value);
        StagedExpr(cell.value.borrow_mut().take().expect("staged expr already consumed (double use)"))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("StagedExpr should not be sent back to v8")
    }
}

/// Wrapper for type references received from JS. Extracts a TypeRefValue from a
/// StagedExprCell and returns the Rc`<dyn Type>`.
pub(crate) struct TypeRef(pub Rc<dyn Type>);

impl JsWrap for TypeRef {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        // Array format: [StagedExprCell, {entry_name: v8_value, ...}] Produced by
        // TypeType::to_js when entry_bindings are present (record types)
        if value.is_array() {
            let arr = v8::Local::<v8::Array>::try_from(value).unwrap();
            let type_ext = arr.get_index(scope, 0).unwrap();
            let bindings_obj = arr.get_index(scope, 1).unwrap();
            let cell: Rc<expr::StagedExprCell> = super::rc_from_v8(&*type_ext);
            let inner = cell.value.borrow_mut().take().expect("type ref already consumed");
            let type_ref_value =
                (inner.as_ref() as &dyn std::any::Any)
                    .downcast_ref::<super::type_ref::TypeRefValue>()
                    .expect("Expected TypeRefValue");
            let base_type = type_ref_value.type_.clone();

            // Enhance record types using the actual v8 values
            let enhanced = enhance_type_with_v8_obj(scope, base_type, bindings_obj);
            return TypeRef(enhanced);
        }
        let cell: Rc<expr::StagedExprCell> = super::rc_from_v8(&*value);
        let inner = cell.value.borrow_mut().take().expect("type ref already consumed (double use)");
        let type_ref_value =
            (inner.as_ref() as &dyn std::any::Any)
                .downcast_ref::<super::type_ref::TypeRefValue>()
                .expect("Expected TypeRefValue in StagedExprCell");
        TypeRef(type_ref_value.type_.clone())
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("TypeRef should not be sent back to v8")
    }
}

/// Enhance a record type using v8 values from the bindings object. For entries
/// that are MortarTypeTaggedUnionRef, extracts the TaggedUnionRef from the v8
/// external to get variant data.
fn enhance_type_with_v8_obj<
    's,
>(
    scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    base_type: Rc<dyn Type>,
    bindings_obj: v8::Local<'s, v8::Value>,
) -> Rc<dyn Type> {
    use std::any::Any;

    let base_any: &dyn Any = base_type.as_ref();
    if let Some(record_type) = base_any.downcast_ref::<super::tuple::TypeValueRecord>() {
        if !bindings_obj.is_object() || bindings_obj.is_null_or_undefined() {
            return base_type;
        }
        let obj = v8::Local::<v8::Object>::try_from(bindings_obj).unwrap();
        let mut new_entries = record_type.entry_types.clone();
        for (key, entry_type) in &record_type.entry_types {
            let key_str = match key {
                crate::langhelp::ConstKey::String(s) => s.clone(),
            };
            let type_any: &dyn Any = entry_type.as_ref();
            if type_any.downcast_ref::<super::definition_set::tagged_union::MortarTypeTaggedUnionRef>().is_some() {
                let v8_key = v8::String::new(scope, &key_str).unwrap();
                if let Some(v8_val) = obj.get(scope, v8_key.into()) {
                    if v8_val.is_external() {
                        let ref_: Rc<super::definition_set::TaggedUnionRef> = super::rc_from_v8(&*v8_val);
                        new_entries.insert(
                            key.clone(),
                            Rc::new(
                                super::definition_set::tagged_union::MortarTypeTaggedUnionRef {
                                    data: Some(ref_.data.clone()),
                                },
                            ),
                        );
                    }
                }
            }
        }
        return Rc::new(super::tuple::TypeValueRecord { entry_types: new_entries });
    }
    base_type
}

pub(crate) fn jswrap_method_name(s: &str) -> &str {
    s.strip_suffix('_').unwrap_or(s)
}

macro_rules! jswrap{
    // --- Function variant --- Produces a (&str, MortarScopeEntry,
    // v8::Local<v8::Value>) tuple. `$ex` is the EvalContext, `$js_scope` is the v8
    // scope for building. `$scope` is available in the body as the v8 HandleScope.
    (
        fn $name: ident(
            $ex: ident,
            $js_scope: ident,
            $scope: ident
        )($($arg: ident: $arg_ty: ty), * $(,) ?) -> $ret_ty: ty $body: block
    ) => {
        {
            (stringify!($name), MortarScopeEntry {
                js_key: ScopeEntryId:: Builtin(line!() as usize),
                type_: Rc:: new(MortarTypeFunction {
                    arg: jswrap !(@ arg_mortar_type $($arg_ty), *),
                    ret: <$ret_ty as JsWrap >:: mortar_type(),
                }),
            }, {
                fn _jw_cb <'s >(
                    $scope: & mut v8:: PinnedRef <'s,
                    v8:: HandleScope <'_ >>,
                    _jw_args: v8:: FunctionCallbackArguments <'s >,
                    mut _jw_rv: v8:: ReturnValue <'s >,
                ) {
                    #[allow(unused_mut, unused_variables)]
                    let mut _jw_arg_i = 0i32;
                    $(
                        let $arg: $arg_ty = <$arg_ty as JsWrap >:: from_v8($scope, _jw_args.get(_jw_arg_i));
                        _jw_arg_i += 1;
                    ) * let _jw_result: $ret_ty = $body;
                    _jw_rv.set(<$ret_ty as JsWrap >:: into_v8(_jw_result, $scope));
                }
                v8:: Function:: builder(_jw_cb).build($js_scope).unwrap().cast::< v8:: Value >()
            })
        }
    };
    // --- Struct variant --- Generates: struct, JsWrap impl, Type struct with aspects
    // (Field, ConsumeJs, JsStackValue). Methods use auto-generated JS keys (_0, _1,
    // ...) to avoid collisions. `$self_` (Rc`<StructName>`) and `$scope` (v8
    // HandleScope) are available in field/method bodies. Method names with trailing
    // underscore are stripped for the JS-visible name (e.g. impl_ -> "impl").
    (struct $name: ident($self_: ident, $scope: ident) {
        data {
            $($d_name: ident: $d_ty: ty),
            * $(,) ?
        }
        $(
            field $f_name: ident: $f_ty: ty $f_body: block
        ) * $(fn $m_name: ident($($m_arg: ident: $m_arg_ty: ty), * $(,) ?) -> $m_ret: ty $m_body: block) *
    }) => {
        ::paste::paste!{
            pub(crate) struct $name {
                $(pub $d_name: $d_ty,) *
            }
            pub(crate) struct[<$name Type >];
            fn[<$name: snake _type_identity >]() -> Unique {
                Unique::Builtin {
                    file: file!(),
                    line: line!() as usize
                }
            }
            impl Value for[<$name Type >] {
                fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
                    Rc::new(crate::eval::type_ref::TypeType)
                }
            }
            impl Type for[<$name Type >] {
                fn desc(&self) -> String {
                    stringify!($name).to_string()
                }
                fn aspect(&self, _xx: & ExprContext, key: & Unique) -> Option < Box < dyn Any >> {
                    if * key ==[<$name: snake _type_identity >]() {
                        return Some(Box::new(true));
                    }
                    if * key == aspect_field_aspect_id() {
                        return Some(Box:: new(Box:: new([<$name Type >]) as Box < dyn AspectField >));
                    }
                    if * key == aspect_consume_js_aspect_id() {
                        return Some(Box:: new(Box:: new([<$name Type >]) as Box < dyn AspectConsumeJs >));
                    }
                    if * key == aspect_bind_js_aspect_id() {
                        return Some(Box:: new(Box:: new([<$name Type >]) as Box < dyn AspectBindJs >));
                    }
                    if * key == aspect_js_stack_value_aspect_id() {
                        return Some(Box:: new(Box:: new([<$name Type >]) as Box < dyn AspectJsStackValue >));
                    }
                    None
                }
                fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type,) -> EvalRes {
                    EvalRes::err(loc, format!("Cannot convert {}", stringify!($name)))
                }
                fn cast_from(&self, xx: & ExprContext, loc: QExprId, cv: & CodeValue,) -> EvalRes {
                    let source_type = cv.value.type_(xx, loc);
                    if source_type.aspect(xx, &[<$name: snake _type_identity >]()).is_some() {
                        return EvalRes:: ok(CodeValue {
                            code: cv.code.clone(),
                            value: Box:: new(crate:: eval:: mortar_js_value:: MortarJsValue {
                                source: crate:: eval:: mortar_js_value:: MortarJsValueSource:: Expr,
                                type_: std:: rc:: Rc:: new([<$name Type >]),
                            }),
                        });
                    }
                    EvalRes::err(loc, format!("Cannot convert to {}", stringify!($name)))
                }
            }
            impl Aspect for[<$name Type >] {
            }
            impl AspectConsumeJs for[<$name Type >] {
                fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue,) -> EResult<JsCode> {
                    cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
                }
            }
            impl AspectBindJs for[<$name Type >] {
                fn bind_js(
                    &self,
                    xx: &ExprContext,
                    loc: QExprId,
                    cv: CodeValue,
                ) -> EResult<(Option<JsCode>, CodeValue)> {
                    let vtype = cv.value.type_(xx, loc);
                    self.to_js(xx, loc, cv).map(|c| crate::eval::mortar_js_value::bind_js_some(loc, c, vtype))
                }
            }
            impl AspectJsStackValue for[<$name Type >] {
                fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
                    CodeValue {
                        code: Code {
                            loc,
                            code: Code_:: Dyn(Rc::new(value))
                        },
                        value: Box:: new(MortarJsValue {
                            source: MortarJsValueSource:: Expr,
                            type_: Rc:: new([<$name Type >]),
                        }),
                    }
                }
            }
            impl AspectField for[<$name Type >] {
                fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue,) -> EvalRes {
                    er_seq_cv!(loc, erb, {
                        let key_type = key.value.type_(xx, loc);
                        let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
                        let key_name = key_string.get(key.value.as_ref());
                        #[allow(unused_mut, unused_variables)]
                        let mut _jw_idx = 0usize;
                        $(
                            if key_name == jswrap_method_name(stringify!($f_name)) {
                                let field_type: Rc < dyn Type > = <$f_ty as JsWrap >:: mortar_type();
                                let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
                                return Ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(JsCode {
                                            loc,
                                            type_: JsCodeType::FieldUse(crate::jscode::JsCodeFieldUse {
                                                base: Box::new(self_code),
                                                field: format!("_{}", _jw_idx),
                                            },),
                                        })),
                                    },
                                    value: Box::new(MortarJsValue {
                                        source: MortarJsValueSource::Expr,
                                        type_: field_type,
                                    }),
                                });
                            }
                            _jw_idx += 1;
                        ) * $(
                            if key_name == jswrap_method_name(stringify!($m_name)) {
                                let field_type: Rc < dyn Type > = Rc:: new(MortarTypeFunction {
                                    arg: jswrap !(@ arg_mortar_type $($m_arg_ty), *),
                                    ret: <$m_ret as JsWrap >:: mortar_type(),
                                });
                                let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
                                return Ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(JsCode {
                                            loc,
                                            type_: JsCodeType::FieldUse(crate::jscode::JsCodeFieldUse {
                                                base: Box::new(self_code),
                                                field: format!("_{}", _jw_idx),
                                            },),
                                        })),
                                    },
                                    value: Box::new(MortarJsValue {
                                        source: MortarJsValueSource::Expr,
                                        type_: field_type,
                                    }),
                                });
                            }
                            _jw_idx += 1;
                        ) * return erb.err_new(format!("Unknown field '{}' on {}", key_name, stringify!($name),));
                    })
                }
                fn field_opt(&self, _xx: & ExprContext, key: & str,) -> Option < Rc < dyn Type >> {
                    $(if key == jswrap_method_name(stringify!($f_name)) {
                        return Some(<$f_ty as JsWrap >:: mortar_type());
                    }) * $(if key == jswrap_method_name(stringify!($m_name)) {
                        return Some(Rc:: new(MortarTypeFunction {
                            arg: jswrap !(@ arg_mortar_type $($m_arg_ty), *),
                            ret: <$m_ret as JsWrap >:: mortar_type(),
                        }));
                    }) * None
                }
            }
            impl JsWrap for $name {
                fn mortar_type() -> Rc < dyn Type > {
                    Rc:: new([<$name Type >])
                }
                fn from_v8<
                    's
                >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>,) -> Self {
                    unimplemented!(concat!("from_v8 not supported for ", stringify!($name),))
                }
                fn into_v8 <'s >(self, $scope: & mut v8:: PinnedRef <'s, v8:: HandleScope <'_ >>,) -> v8:: Local <'s,
                v8:: Value > {
                    let _jw_obj = v8:: Object:: new($scope);
                    let _jw_self_rc = Rc::new(self);
                    let _jw_data_ext = rc_to_v8($scope, _jw_self_rc.clone());
                    _jw_obj.set($scope, v8:: String:: new($scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                    #[allow(unused_mut, unused_variables)]
                    let mut _jw_idx = 0usize;
                    // Fields
                    $({
                        #[allow(unused_variables)] let $self_ = _jw_self_rc.clone();
                        let _jw_field_val: $f_ty = $f_body;
                        let _jw_v8_val = <$f_ty as JsWrap >:: into_v8(_jw_field_val, $scope);
                        let _jw_key_str = format!("_{}", _jw_idx);
                        let _jw_key = v8:: String:: new($scope, &_jw_key_str).unwrap();
                        _jw_obj.set($scope, _jw_key.into(), _jw_v8_val);
                        _jw_idx += 1;
                    }) * 
                    // Methods
                    $({
                        fn _jw_cb <'s >(
                            $scope: & mut v8:: PinnedRef <'s,
                            v8:: HandleScope <'_ >>,
                            _jw_args: v8:: FunctionCallbackArguments <'s >,
                            mut _jw_rv: v8:: ReturnValue <'s >,
                        ) {
                            let _jw_this = _jw_args.this();
                            let _jw_dk = v8:: String:: new($scope, "_d").unwrap();
                            let $self_: Rc <$name > = rc_from_v8(&* _jw_this.get($scope, _jw_dk.into()).unwrap(),);
                            #[allow(unused_mut, unused_variables)]
                            let mut _jw_arg_i = 0i32;
                            $(
                                let $m_arg: $m_arg_ty = <$m_arg_ty as JsWrap >:: from_v8(
                                    $scope,
                                    _jw_args.get(_jw_arg_i),
                                );
                                _jw_arg_i += 1;
                            ) * let _jw_result: $m_ret = $m_body;
                            _jw_rv.set(<$m_ret as JsWrap >:: into_v8(_jw_result, $scope,),);
                        }
                        let _jw_method_fn = v8:: Function:: builder(_jw_cb).build($scope).unwrap();
                        let _jw_key_str = format!("_{}", _jw_idx);
                        let _jw_key = v8:: String:: new($scope, &_jw_key_str).unwrap();
                        _jw_obj.set($scope, _jw_key.into(), _jw_method_fn.into(),);
                        _jw_idx += 1;
                    }) * _jw_obj.into()
                }
            }
        }
    };
    // --- Helpers --- 0 args → empty tuple type
    (@ arg_mortar_type) => {
        Rc::new(crate::eval::tuple::TypeTuple { types: vec![] }) as Rc<dyn Type>
    };
    // 1 arg → single type
    (@ arg_mortar_type $single: ty) => {
        <$single as JsWrap >:: mortar_type()
    };
    // 2+ args → tuple type
    (@ arg_mortar_type $first: ty, $($rest: ty), +) => {
        Rc::new(
            crate::eval::tuple::TypeTuple {
                types: vec![<$first as JsWrap >:: mortar_type(), $(<$rest as JsWrap >:: mortar_type()), +],
            }
        ) as
            Rc<dyn Type>
    };
}
