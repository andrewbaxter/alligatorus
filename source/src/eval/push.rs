use {
    crate::{
        eval::{
            TargetBindSub,
            dynamic_util::throw_error,
            from_v8_ref,
            value::{
                tuple::{
                    Tuple,
                    TupleElement,
                },
                type_any::dynamic::TypeAnyDynamic,
                type_any_scope_entry::dynamic::TypeAnyScopeEntryDynamic,
                type_any_target::dynamic::TypeAnyTargetDynamic,
                type_bool::dynamic::TypeBoolDynamic,
                type_code_source::TypeCodeDynHandleDynamic,
                type_constkey::dynamic::TypeConstkeyDynamic,
                type_expr::TypeExpr,
                type_map::dynamic::TypeMapDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_option::dynamic::TypeOptionDynamic,
                type_string::dynamic::TypeStringDynamic,
                type_value::dynamic::TypeValueDynamic,
            },
        },
        langhelp::{
            Code,
            CodeDyn,
            Code_,
            ConstKey,
            EResult,
            QExprId,
            ScopeEntryId,
            SourceId,
            Type,
            Value,
            downcast_value,
        },
        r#gen::{
            ScopeEntry,
            Target,
        },
        syntax::ExprId,
    },
    std::{
        collections::{
            BTreeMap,
            HashMap,
        },
        rc::Rc,
    },
    super::expr,
};

pub(crate) trait Push: Sized + 'static {
    fn dynamic_type() -> Rc<dyn Type>;
    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self;
    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value>;
}

pub(crate) trait PushKey: Sized + Ord + 'static {
    fn from_dynamic_key(s: &str) -> Result<Self, String>;
    fn to_dynamic_key(&self) -> String;
}

pub(crate) struct AnyType(pub Rc<dyn Type>);

impl Push for AnyType {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        AnyType(super::rc_type_from_v8(&*value))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("AnyType should not be sent back to v8")
    }
}

pub(crate) fn push_method_name(s: &str) -> &str {
    s.strip_suffix('_').unwrap_or(s)
}

pub(crate) struct StagedExpr(pub Rc<dyn Value>);

impl Push for StagedExpr {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        StagedExpr(cell.value.borrow_mut().take().expect("staged expr already consumed (double use)"))
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("StagedExpr should not be sent back to v8")
    }
}

impl Push for () {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeNullDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {

    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::null(scope).into()
    }
}

impl<A, B> Push for (A, B)
where
    A: Push,
    B: Push {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(
            Tuple {
                elements: vec![
                    TupleElement::Dynamic(<A as Push>::dynamic_type()),
                    TupleElement::Dynamic(<B as Push>::dynamic_type()),
                ],
            },
        )
    }

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
}

impl Push for bool {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeBoolDynamic)
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.boolean_value(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::Boolean::new(scope, self).into()
    }
}

impl<K: PushKey + Push, V: Push> Push for BTreeMap<K, V> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeMapDynamic {
            key_type: K::dynamic_type(),
            value_type: V::dynamic_type(),
        })
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("BTreeMap argument must be a JS object");
        let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
        let len = keys.length();
        let mut map = BTreeMap::new();
        for i in 0 .. len {
            let dynamic_key = keys.get_index(scope, i).unwrap();
            let key_str = dynamic_key.to_string(scope).unwrap().to_rust_string_lossy(scope);
            let key = match K::from_dynamic_key(&key_str) {
                Ok(k) => k,
                Err(e) => {
                    throw_error(scope, &format!("Invalid map key: {}", e));
                    return BTreeMap::new();
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
}

impl Push for Code {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeOptionDynamic { element_type: Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type> })
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let dummy_loc: QExprId = (SourceId(0), ExprId(0));
        let obj = v8::Local::<v8::Object>::try_from(value).expect("Code: expected option-shaped JS object");
        let some_key =
            v8::String::new(
                scope,
                &ConstKey::String("some".to_string()).to_str(),
            ).expect("Code from_v8: some key alloc");
        let some_val = obj.get(scope, some_key.into()).expect("Code from_v8: get some");
        if !some_val.is_undefined() {
            let code: Rc<dyn CodeDyn> = super::from_v8_ref::<Rc<dyn CodeDyn>>(&*some_val).clone();
            return Code {
                loc: dummy_loc,
                code: Code_::Dyn(code),
            };
        }
        Code {
            loc: dummy_loc,
            code: Code_::None,
        }
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Code should not be sent back to v8 (yet)")
    }
}

impl Push for ConstKey {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeConstkeyDynamic)
    }

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
}

impl PushKey for ConstKey {
    fn from_dynamic_key(s: &str) -> Result<Self, String> {
        ConstKey::from_str(s)
    }

    fn to_dynamic_key(&self) -> String {
        self.to_str()
    }
}

impl<T: Push> Push for EResult<T> {
    fn dynamic_type() -> Rc<dyn Type> {
        <T as Push>::dynamic_type()
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("EResult<T>::from_v8: not yet wired");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("EResult<T>::into_v8: not yet wired");
    }
}

impl Push for expr::Expr {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeExpr)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let cell: Rc<expr::StagedExprCell> = from_v8_ref::<Rc<expr::StagedExprCell>>(&*value).clone();
        let inner = cell.value.borrow_mut().take().expect("Expr staged value already consumed");
        Rc::try_unwrap(downcast_value::<expr::Expr>(inner)).ok().expect("Expr in StagedExprCell: Rc not unique")
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Expr should not be sent back to v8")
    }
}

impl<K, V> Push for HashMap<K, V>
where
    K: PushKey + Push + Eq + std::hash::Hash,
    V: Push {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeMapDynamic {
            key_type: <K as Push>::dynamic_type(),
            value_type: <V as Push>::dynamic_type(),
        })
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("HashMap::from_v8: expected JS object");
        let keys = obj.get_own_property_names(scope, v8::GetPropertyNamesArgs::default()).unwrap();
        let mut out = HashMap::new();
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
}

impl<T: Push> Push for Option<T> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeOptionDynamic { element_type: <T as Push>::dynamic_type() })
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("Option<T>::from_v8: not yet wired");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("Option<T>::into_v8: not yet wired");
    }
}

impl Push for Rc<dyn ScopeEntry> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyScopeEntryDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_scope_entry_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_scope_entry_to_v8(scope, self).into()
    }
}

impl Push for Rc<dyn Target> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyTargetDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_target_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_target_to_v8(scope, self).into()
    }
}

impl Push for Rc<dyn Value> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeValueDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        super::rc_value_from_v8(&*value)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_value_to_v8(scope, self).into()
    }
}

impl Push for ScopeEntryId {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeStringDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("ScopeEntryId::from_v8: user-defined target dispatch not yet routed through v8",);
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("ScopeEntryId::into_v8: cross-dynamic `ScopeEntryId` transfer not yet wired",);
    }
}

impl Push for String {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeStringDynamic)
    }

    fn from_v8<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        value.to_string(scope).unwrap().to_rust_string_lossy(scope)
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        v8::String::new(scope, &self).unwrap().into()
    }
}

impl Push for TargetBindSub {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>) -> Self {
        unimplemented!("TargetBindSub::from_v8: user-defined target dispatch not yet routed through v8");
    }

    fn into_v8<'s>(self, _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        unimplemented!("TargetBindSub::into_v8: cross-dynamic `TargetBindSub` transfer not yet wired");
    }
}
