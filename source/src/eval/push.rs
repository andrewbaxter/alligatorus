use {
    crate::{
        eval::{
            dynamic_util::throw_error,
            from_v8_ref,
            value::{
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

pub(crate) trait Push: Sized + 'static {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self;
    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value>;
    fn dynamic_type() -> Rc<dyn Type>;
}

/// Trait for types that can be used as BTreeMap keys in Push. Converts
/// between Rust keys and JS object property names (strings).
pub(crate) trait PushKey: Sized + Ord + 'static {
    fn from_dynamic_key(s: &str) -> Result<Self, String>;
    fn to_dynamic_key(&self) -> String;
}

/// Wrapper for type values received from JS. In v8, types are always
/// Rc`<dyn Type>` externals.
pub(crate) struct AnyType(pub Rc<dyn Type>);

impl Push for AnyType {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        AnyType(super::rc_type_from_v8(&*value))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("AnyType should not be sent back to v8")
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }
}

pub(crate) fn push_method_name(s: &str) -> &str {
    s.strip_suffix('_').unwrap_or(s)
}

/// Wrapper for staged expressions received from JS. Extracts and consumes the
/// staged value from a StagedExprCell.
pub(crate) struct StagedExpr(pub Rc<dyn Value>);

impl Push for StagedExpr {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        StagedExpr(cell.value.borrow_mut().take().expect("staged expr already consumed (double use)"))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("StagedExpr should not be sent back to v8")
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

impl Push for () {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {

    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::null(scope).into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeNullDynamic)
    }
}

/// Generic `Push` for 2-tuples `(A, B)`. The v8 form is a JS array of
/// length 2. The source-side `Type` is the parameterized `Tuple` with element
/// types `[A::dynamic_type(), B::dynamic_type()]` — the spec author cannot specify a
/// mismatched dynamic by construction.
impl<A, B> Push for (A, B)
where
    A: Push,
    B: Push {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let arr = v8::Local::<v8::Array>::try_from(value).expect("(A, B)::from_v8: expected JS array of length 2");
        let a_val = arr.get_index(scope, 0).expect("(A, B)::from_v8: missing element 0");
        let b_val = arr.get_index(scope, 1).expect("(A, B)::from_v8: missing element 1");
        let a = <A as Push>::from_v8(scope, a_val);
        let b = <B as Push>::from_v8(scope, b_val);
        (a, b)
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("(A, B) should not be sent back to v8 (yet)");
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(
            crate::eval::value::tuple::Tuple {
                elements: vec![
                    crate::eval::value::tuple::TupleElement::Dynamic(<A as Push>::dynamic_type()),
                    crate::eval::value::tuple::TupleElement::Dynamic(<B as Push>::dynamic_type()),
                ],
            },
        )
    }
}

impl Push for bool {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.boolean_value(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::Boolean::new(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_bool::dynamic::TypeBoolDynamic)
    }
}

impl Push for ConstKey {
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

    fn dynamic_type() -> Rc<dyn Type> {
        // Distinct dynamic type whose `cast_from` performs canonical-JSON encoding —
        // matches `ConstKey::from_str` at the v8 boundary. Using plain
        // `TypeStringDynamic` here was wrong: it left string keys un-encoded so
        // `BTreeMap<ConstKey, V>::from_v8` couldn't parse them.
        Rc::new(crate::eval::value::type_constkey::dynamic::TypeConstkeyDynamic)
    }
}

impl PushKey for ConstKey {
    fn from_dynamic_key(s: &str) -> Result<Self, String> {
        ConstKey::from_str(s)
    }

    fn to_dynamic_key(&self) -> String {
        self.to_str()
    }
}

/// `Push` for `TargetBindSub`. dynamic_type returns `TypeAnyDynamic` as a
/// placeholder — proper source-side dynamic (e.g. a record of `{data, type}`) is a
/// TODO once user-defined target dispatch is wired.
impl Push for crate::eval::TargetBindSub {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("TargetBindSub::from_v8: user-defined target dispatch not yet routed through v8");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("TargetBindSub::into_v8: cross-dynamic `TargetBindSub` transfer not yet wired");
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }
}

impl Push for crate::langhelp::Code {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        // Source-level `Code` is `option(code_dyn)`. The runtime form is a JS object
        // shaped like option: `{<json-encoded "some">: <External(Rc<dyn CodeDyn>)>}` or
        // `{<json-encoded "none">: null}`.
        let dummy_loc: crate::langhelp::QExprId = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let obj = v8::Local::<v8::Object>::try_from(value).expect("Code: expected option-shaped JS object");
        let some_key =
            v8::String::new(
                scope,
                &crate::langhelp::ConstKey::String("some".to_string()).to_str(),
            ).expect("Code from_v8: some key alloc");
        let some_val = obj.get(scope, some_key.into()).expect("Code from_v8: get some");
        if !some_val.is_undefined() {
            let code: Rc<dyn crate::langhelp::CodeDyn> =
                super::from_v8_ref::<Rc<dyn crate::langhelp::CodeDyn>>(&*some_val).clone();
            return crate::langhelp::Code {
                loc: dummy_loc,
                code: crate::langhelp::Code_::Dyn(code),
            };
        }
        crate::langhelp::Code {
            loc: dummy_loc,
            code: crate::langhelp::Code_::None,
        }
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Code should not be sent back to v8 (yet)")
    }

    fn dynamic_type() -> Rc<dyn Type> {
        // The source-level `Code` is `option(code_dyn)`. The user writes
        // `option(codedyn).some(<code_dyn>)` / `.none(())` to construct one.
        Rc::new(
            crate::eval::value::type_option::dynamic::TypeOptionDynamic {
                element_type: Rc::new(crate::eval::value::type_code_source::TypeCodeDynHandleDynamic) as
                    Rc<dyn Type>,
            },
        )
    }
}

/// Generic `Push` for `EResult<T>`. The source-side dynamic Type delegates
/// to `T`'s — errors are Rust-side dispatch plumbing, not part of the source-level
/// type. `from_v8`/`into_v8` are stubs for now; user-defined dispatch through v8
/// isn't routed through these.
impl<T: Push> Push for crate::langhelp::EResult<T> {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("EResult<T>::from_v8: not yet wired");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("EResult<T>::into_v8: not yet wired");
    }

    fn dynamic_type() -> Rc<dyn Type> {
        <T as Push>::dynamic_type()
    }
}

/// `Push` for `ScopeEntryId` — the source-side representation is a string
/// (the binding name produced by `gen_binding_name`). Used by the `Target::bind`
/// ret tuple. `from_v8`/`into_v8` are stubs for now — user-defined target dispatch
/// isn't yet routed through v8, and the builtin `TargetDynamic` constructs
/// `ScopeEntryId` directly.
impl Push for crate::langhelp::ScopeEntryId {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("ScopeEntryId::from_v8: user-defined target dispatch not yet routed through v8",);
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("ScopeEntryId::into_v8: cross-dynamic `ScopeEntryId` transfer not yet wired",);
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic)
    }
}

impl Push for expr::Expr {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        let inner = cell.value.borrow_mut().take().expect("Expr staged value already consumed");
        Rc::try_unwrap(downcast_value::<expr::Expr>(inner)).ok().expect("Expr in StagedExprCell: Rc not unique")
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Expr should not be sent back to v8")
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }
}

/// Generic `Push` for `Option<T>`. dynamic_type = `option(T::dynamic_type())`.
impl<T: Push> Push for Option<T> {
    fn from_v8<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("Option<T>::from_v8: not yet wired");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Option<T>::into_v8: not yet wired");
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(
            crate::eval::value::type_option::dynamic::TypeOptionDynamic {
                element_type: <T as Push>::dynamic_type(),
            },
        )
    }
}

impl Push for Rc<dyn crate::langhelp::Value> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_value_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_value_to_v8(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_value::dynamic::TypeValueDynamic)
    }
}

impl Push for Rc<dyn crate::r#gen::ScopeEntry> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_scope_entry_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_scope_entry_to_v8(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any_scope_entry::dynamic::TypeAnyScopeEntryDynamic)
    }
}

impl Push for Rc<dyn crate::r#gen::Target> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_target_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_target_to_v8(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any_target::dynamic::TypeAnyTargetDynamic)
    }
}

impl<K: PushKey + Push, V: Push> Push for std::collections::BTreeMap<K, V> {
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

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeMapDynamic {
            key_type: K::dynamic_type(),
            value_type: V::dynamic_type(),
        })
    }
}

/// Generic manual `Push` for `HashMap<K, V>` over any
/// `K: PushKey + Eq + std::hash::Hash` and `V: Push`. Walks the v8
/// record's keys and delegates each value's parsing to `V::from_v8`. `dynamic_type`
/// reports `map<string, V::dynamic_type()>` since the v8 record keys are JS strings
/// regardless of `K`.
impl<K, V> Push for std::collections::HashMap<K, V>
where
    K: PushKey + Push + Eq + std::hash::Hash,
    V: Push {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("HashMap::from_v8: expected JS object");
        let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
        let mut out = std::collections::HashMap::new();
        for i in 0 .. keys.length() {
            let key = keys.get_index(scope, i).unwrap();
            let key_str = key.to_string(scope).unwrap().to_rust_string_lossy(scope);
            let parsed_key =
                K::from_dynamic_key(
                    &key_str,
                ).unwrap_or_else(|e| panic!("HashMap::from_v8: bad key {:?}: {}", key_str, e));
            let val = obj.get(scope, key).unwrap();
            let parsed_val = V::from_v8(scope, val);
            out.insert(parsed_key, parsed_val);
        }
        out
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("HashMap should not be sent back to v8")
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeMapDynamic {
            key_type: <K as Push>::dynamic_type(),
            value_type: <V as Push>::dynamic_type(),
        })
    }
}

impl Push for String {
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.to_string(scope).unwrap().to_rust_string_lossy(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::String::new(scope, &self).unwrap().into()
    }

    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeStringDynamic)
    }
}

