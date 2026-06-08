use {
    crate::{
        eval::{
            dynamic_util::throw_error,
            from_v8_ref,
            value::{
                record::Record,
                tuple::Tuple,
                type_any::dynamic::TypeAnyDynamic,
                type_expr::TypeExpr,
                type_map::dynamic::TypeMapDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_string::dynamic::TypeStringDynamic,
            },
        },
        langhelp::{
            ConstKey,
            Type,
            Value,
            downcast_value,
        },
    },
    std::rc::Rc,
    super::expr,
};

macro_rules! dynamic_wrap{
    // --- Function variant --- Produces a (&str, ScopeEntryDynamicBinding,
    // v8::Local<v8::Value>) tuple. `$ex` is the EvalContext, `$dynamic_scope` is the
    // v8 scope for building. `$scope` is available in the body as the v8 HandleScope.
    (
        fn $name: ident(
            $ex: ident,
            $dynamic_scope: ident,
            $scope: ident
        )($($arg: ident: $arg_ty: ty), * $(,) ?) -> $ret_ty: ty $body: block
    ) => {
        {
            (stringify!($name), ScopeEntryDynamicBinding {
                dynamic_key: ScopeEntryId:: Builtin(line!() as usize),
                type_: Rc:: new(TypeFunctionDynamic {
                    arg: dynamic_wrap !(@ arg_mortar_type $($arg_ty), *),
                    ret: <$ret_ty as DynamicWrap >:: mortar_type(),
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
                        let $arg: $arg_ty = <$arg_ty as DynamicWrap >:: from_v8($scope, _jw_args.get(_jw_arg_i));
                        _jw_arg_i += 1;
                    ) * let _jw_result: $ret_ty = $body;
                    _jw_rv.set(<$ret_ty as DynamicWrap >:: into_v8(_jw_result, $scope));
                }
                v8:: Function:: builder(_jw_cb).build($dynamic_scope).unwrap().cast::< v8:: Value >()
            })
        }
    };
    // --- Struct variant --- Generates: struct, DynamicWrap impl, Type struct with
    // aspects (Field, ConsumeJs, JsStackValue). Methods use auto-generated JS keys
    // (_0, _1, ...) to avoid collisions. `$self_` (Rc`<StructName>`) and `$scope` (v8
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
            pub(crate) struct[< Type $name Dynamic >];
            fn[<$name: snake _type_identity >]() -> Unique {
                Unique::Builtin {
                    file: file!().to_string(),
                    line: line!() as usize
                }
            }
            impl Value for[< Type $name Dynamic >] {
                fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
                    Rc::new(TypeAnyConst)
                }
            }
            impl Type for[< Type $name Dynamic >] {
                fn desc(&self) -> String {
                    stringify!($name).to_string()
                }
                fn as_value(&self) -> Rc < dyn Value > {
                    Rc:: new([< Type $name Dynamic >])
                }
                fn aspect(&self, _xx: & ExprContext, key: & Unique) -> Option < Box < dyn Any >> {
                    if * key ==[<$name: snake _type_identity >]() {
                        return Some(Box::new(true));
                    }
                    if * key == aspect_field_aspect_id() {
                        return Some(Box:: new(Box:: new([< Type $name Dynamic >]) as Box < dyn AspectField >));
                    }
                    if * key == aspect_consume_dynamic_aspect_id() {
                        return Some(Box:: new(Box:: new([< Type $name Dynamic >]) as Box < dyn AspectConsumeDynamic >));
                    }
                    if * key == aspect_bind_dynamic_aspect_id() {
                        return Some(Box:: new(Box:: new([< Type $name Dynamic >]) as Box < dyn AspectBindDynamic >));
                    }
                    if * key == aspect_bind_aspect_id() {
                        return Some(Box:: new(Box:: new([< Type $name Dynamic >]) as Box < dyn AspectBind >));
                    }
                    if * key == aspect_stack_value_dynamic_aspect_id() {
                        return Some(
                            Box:: new(Box:: new([< Type $name Dynamic >]) as Box < dyn AspectStackValueDynamic >)
                        );
                    }
                    None
                }
                fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type,) -> EvalRes {
                    EvalRes::err(loc, format!("Cannot convert {}", stringify!($name)))
                }
                fn can_cast_from(&self, xx: & ExprContext, _loc: QExprId, source_type: & dyn Type) -> bool {
                    source_type.aspect(xx, &[<$name: snake _type_identity >]()).is_some()
                }
                fn cast_from(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue,) -> EvalRes {
                    let source_type = cv.value.type_(xx, loc);
                    if source_type.aspect(xx, &[<$name: snake _type_identity >]()).is_some() {
                        return EvalRes:: ok(CodeValue {
                            code: cv.code,
                            value: Rc:: new(ValueDynamic {
                                source: ValueDynamicSource:: Expr,
                                type_: std:: rc:: Rc:: new([< Type $name Dynamic >]),
                            }),
                        });
                    }
                    EvalRes::err(loc, format!("Cannot convert to {}", stringify!($name)))
                }
            }
            impl Aspect for[< Type $name Dynamic >] {
            }
            impl AspectConsumeDynamic for[< Type $name Dynamic >] {
                fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue,) -> EResult<CodeDynamic> {
                    cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
                }
            }
            impl AspectBindDynamic for[< Type $name Dynamic >] {
                fn bind_dynamic(
                    &self,
                    xx: &ExprContext,
                    loc: QExprId,
                    cv: CodeValue,
                ) -> EResult<Option<TargetBindSub>> {
                    bind_dynamic_default(xx, loc, cv)
                }
            }
            impl AspectBind for[< Type $name Dynamic >] {
                fn bind(
                    &self,
                    xx: &ExprContext,
                    loc: QExprId,
                    cv: CodeValue,
                ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
                    bind_via_target(xx, loc, cv)
                }
            }
            impl AspectStackValueDynamic for[< Type $name Dynamic >] {
                fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
                    CodeValue {
                        code: Code {
                            loc,
                            code: Code_:: Dyn(Rc::new(value))
                        },
                        value: Rc:: new(ValueDynamic {
                            source: ValueDynamicSource:: Expr,
                            type_: Rc:: new([< Type $name Dynamic >]),
                        }),
                    }
                }
            }
            impl AspectField for[< Type $name Dynamic >] {
                fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue,) -> EvalRes {
                    er_seq_cv!(loc, erb, {
                        let key_type = key.value.type_(xx, loc);
                        let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                        let key_name = key_string.get(key.value.as_ref());
                        #[allow(unused_mut, unused_variables)]
                        let mut _jw_idx = 0usize;
                        $(
                            if key_name == dynamic_wrap_method_name(stringify!($f_name)) {
                                let field_type: Rc < dyn Type > = <$f_ty as DynamicWrap >:: mortar_type();
                                let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
                                return Ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(CodeDynamic {
                                            loc,
                                            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                                                base: Box::new(self_code),
                                                field: format!("_{}", _jw_idx),
                                            },),
                                        })),
                                    },
                                    value: Rc::new(ValueDynamic {
                                        source: ValueDynamicSource::Expr,
                                        type_: field_type,
                                    }),
                                });
                            }
                            _jw_idx += 1;
                        ) * $(
                            if key_name == dynamic_wrap_method_name(stringify!($m_name)) {
                                let field_type: Rc < dyn Type > = Rc:: new(TypeFunctionDynamic {
                                    arg: dynamic_wrap !(@ arg_mortar_type $($m_arg_ty), *),
                                    ret: <$m_ret as DynamicWrap >:: mortar_type(),
                                });
                                let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
                                return Ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(CodeDynamic {
                                            loc,
                                            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                                                base: Box::new(self_code),
                                                field: format!("_{}", _jw_idx),
                                            },),
                                        })),
                                    },
                                    value: Rc::new(ValueDynamic {
                                        source: ValueDynamicSource::Expr,
                                        type_: field_type,
                                    }),
                                });
                            }
                            _jw_idx += 1;
                        ) * return erb.err_new(format!("Unknown field '{}' on {}", key_name, stringify!($name),));
                    })
                }
                fn field_opt(&self, _xx: & ExprContext, key: & str,) -> Option < Rc < dyn Type >> {
                    $(if key == dynamic_wrap_method_name(stringify!($f_name)) {
                        return Some(<$f_ty as DynamicWrap >:: mortar_type());
                    }) * $(if key == dynamic_wrap_method_name(stringify!($m_name)) {
                        return Some(Rc:: new(TypeFunctionDynamic {
                            arg: dynamic_wrap !(@ arg_mortar_type $($m_arg_ty), *),
                            ret: <$m_ret as DynamicWrap >:: mortar_type(),
                        }));
                    }) * None
                }
            }
            impl DynamicWrap for $name {
                fn mortar_type() -> Rc < dyn Type > {
                    Rc:: new([< Type $name Dynamic >])
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
                    let _jw_data_ext = crate:: eval:: to_v8($scope, _jw_self_rc.clone());
                    _jw_obj.set($scope, v8:: String:: new($scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                    #[allow(unused_mut, unused_variables)]
                    let mut _jw_idx = 0usize;
                    // Fields
                    $({
                        #[allow(unused_variables)] let $self_ = _jw_self_rc.clone();
                        let _jw_field_val: $f_ty = $f_body;
                        let _jw_v8_val = <$f_ty as DynamicWrap >:: into_v8(_jw_field_val, $scope);
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
                            #[
                                allow(unused_variables)
                            ] let $self_: Rc <$name > = crate:: eval:: from_v8_ref::< Rc <$name >>(
                                &* _jw_this.get($scope, _jw_dk.into()).unwrap(),
                            ).clone();
                            #[allow(unused_mut, unused_variables)]
                            let mut _jw_arg_i = 0i32;
                            $(
                                let $m_arg: $m_arg_ty = <$m_arg_ty as DynamicWrap >:: from_v8(
                                    $scope,
                                    _jw_args.get(_jw_arg_i),
                                );
                                _jw_arg_i += 1;
                            ) * let _jw_result: $m_ret = $m_body;
                            _jw_rv.set(<$m_ret as DynamicWrap >:: into_v8(_jw_result, $scope,),);
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
        Rc::new(Tuple {
            source: None,
            elements: vec![]
        }) as Rc<dyn Type>
    };
    // 1 arg → single type
    (@ arg_mortar_type $single: ty) => {
        <$single as DynamicWrap >:: mortar_type()
    };
    // 2+ args → tuple type
    (@ arg_mortar_type $first: ty, $($rest: ty), +) => {
        Rc::new(Tuple {
            source: None,
            elements: vec![
                crate:: eval:: value:: tuple:: TupleElement:: Mortar(<$first as DynamicWrap >:: mortar_type()),
                $(crate:: eval:: value:: tuple:: TupleElement:: Mortar(<$rest as DynamicWrap >:: mortar_type())),
                +
            ],
        }) as Rc<dyn Type>
    };
}

pub(crate) trait DynamicWrap: Sized + 'static {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self;
    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value>;
    fn mortar_type() -> Rc<dyn Type>;
}

/// Trait for types that can be used as BTreeMap keys in DynamicWrap. Converts
/// between Rust keys and JS object property names (strings).
pub(crate) trait DynamicWrapKey: Sized + Ord + 'static {
    fn from_dynamic_key(s: &str) -> Result<Self, String>;
    fn to_dynamic_key(&self) -> String;
}

/// Wrapper for type values received from JS. In v8, types are always
/// Rc`<dyn Type>` externals.
pub(crate) struct AnyType(pub Rc<dyn Type>);

impl DynamicWrap for AnyType {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        AnyType(super::rc_type_from_v8(&*value))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("AnyType should not be sent back to v8")
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }
}

pub(crate) fn dynamic_wrap_method_name(s: &str) -> &str {
    s.strip_suffix('_').unwrap_or(s)
}

/// Wrapper for staged expressions received from JS. Extracts and consumes the
/// staged value from a StagedExprCell.
pub(crate) struct StagedExpr(pub Rc<dyn Value>);

impl DynamicWrap for StagedExpr {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        StagedExpr(cell.value.borrow_mut().take().expect("staged expr already consumed (double use)"))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("StagedExpr should not be sent back to v8")
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

impl DynamicWrap for () {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {

    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::null(scope).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeNullDynamic)
    }
}

impl DynamicWrap for ConstKey {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let s = value.to_string(scope).unwrap().to_rust_string_lossy(scope);
        match ConstKey::from_str(&s) {
            Ok(k) => k,
            Err(e) => {
                throw_error(scope, &format!("Invalid ConstKey: {}", e));
                ConstKey::String(String::new())
            },
        }
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let s = self.to_str();
        v8::String::new(scope, &s).unwrap().into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeStringDynamic)
    }
}

impl DynamicWrapKey for ConstKey {
    fn from_dynamic_key(s: &str) -> Result<Self, String> {
        ConstKey::from_str(s)
    }

    fn to_dynamic_key(&self) -> String {
        self.to_str()
    }
}

impl DynamicWrap for expr::Expr {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        let inner = cell.value.borrow_mut().take().expect("Expr staged value already consumed");
        Rc::try_unwrap(downcast_value::<expr::Expr>(inner).expect("Expected Expr in StagedExprCell"))
            .ok()
            .expect("Expr in StagedExprCell: Rc not unique")
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Expr should not be sent back to v8")
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

impl<K: DynamicWrapKey + DynamicWrap, V: DynamicWrap> DynamicWrap for std::collections::BTreeMap<K, V> {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("BTreeMap argument must be a JS object");
        let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
        let len = keys.length();
        let mut map = std::collections::BTreeMap::new();
        for i in 0 .. len {
            let dynamic_key = keys.get_index(scope, i).unwrap();
            let key_str = dynamic_key.to_string(scope).unwrap().to_rust_string_lossy(scope);
            let key = match K::from_dynamic_key(&key_str) {
                Ok(k) => k,
                Err(e) => {
                    throw_error(scope, &format!("Invalid map key: {}", e));
                    return std::collections::BTreeMap::new();
                },
            };
            let dynamic_val = obj.get(scope, dynamic_key).unwrap();
            let val = V::from_v8(scope, dynamic_val);
            map.insert(key, val);
        }
        map
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let obj = v8::Object::new(scope);
        for (k, v) in self {
            let dynamic_key: v8::Local<v8::Value> = v8::String::new(scope, &k.to_dynamic_key()).unwrap().into();
            let dynamic_val = V::into_v8(v, scope);
            obj.set(scope, dynamic_key, dynamic_val);
        }
        obj.into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeMapDynamic {
            key_type: K::mortar_type(),
            value_type: V::mortar_type(),
        })
    }
}

impl DynamicWrap for String {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.to_string(scope).unwrap().to_rust_string_lossy(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::String::new(scope, &self).unwrap().into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeStringDynamic)
    }
}
