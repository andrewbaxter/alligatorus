pub mod dynamic;
pub mod r#const;

use {
    crate::langhelp::Unique,
    dumpster::unsync::Gc,
    std::cell::RefCell,
};

#[derive(Clone, Copy, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum BuiltinInterfaceKind {
    /// The `value` interface: methods that every value must support (e.g.
    /// `type()`).
    Value,
    /// The `type` interface: when implemented on a data type, instances of the
    /// data type act as types (providing `can_cast_from`, `cast_from`, `desc`).
    Type,
}

pub(crate) struct SourceInterfaceData {
    /// `Some` for builtin interfaces created at scope setup (e.g. `value`,
    /// `type`). Builtin interface impls bypass the normal source-method
    /// compilation path and are dispatched in Rust.
    pub builtin_kind: Option<BuiltinInterfaceKind>,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub methods: RefCell<Vec<SourceInterfaceMethodData>>,
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceInterfaceData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct SourceInterfaceMethodData {
    pub arg_type: RefCell<Option<std::rc::Rc<dyn crate::langhelp::Type>>>,
    pub field_name: String,
    pub name: String,
    pub ret_type: RefCell<Option<std::rc::Rc<dyn crate::langhelp::Type>>>,
}

pub(crate) struct SourceInterfaceRef {
    pub data: Gc<SourceInterfaceData>,
}

pub(crate) struct TypeSourceInterfaceConst {
    pub data: Gc<SourceInterfaceData>,
}

/// Build the synthetic builtin `value` and `type` interfaces. Identities are
/// derived from stable `builtin_*_iface_identity()` helpers so instances built
/// in different compile contexts compare equal for aspect lookups.
pub(crate) fn build_builtin_interfaces(
    import_state: crate::langhelp::ImportStateId,
) -> (Gc<SourceInterfaceData>, Gc<SourceInterfaceData>) {
    let builtin_def_set =
        Gc::new(
            crate::eval::definition_set::DefinitionSet::new(
                import_state,
                crate::langhelp::DefSetId(usize::MAX),
                crate::langhelp::SourceId(usize::MAX),
            ),
        );
    let any_type: std::rc::Rc<dyn crate::langhelp::Type> =
        std::rc::Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic);
    let value_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::Value),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("value".to_string())),
        methods: RefCell::new(vec![SourceInterfaceMethodData {
            name: "type".to_string(),
            arg_type: RefCell::new(
                Some(std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as std::rc::Rc<dyn crate::langhelp::Type>),
            ),
            ret_type: RefCell::new(Some(any_type.clone())),
            field_name: "_im0".to_string(),
        }]),
        identity: crate::eval::builtin_value_iface_identity(),
    });
    let type_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::Type),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("type".to_string())),
        methods: RefCell::new(vec![
            SourceInterfaceMethodData {
                name: "can_cast_from".to_string(),
                arg_type: RefCell::new(Some(any_type.clone())),
                ret_type: RefCell::new(
                    Some(
                        std::rc::Rc::new(crate::eval::value::type_bool::dynamic::TypeBoolDynamic) as std::rc::Rc<dyn crate::langhelp::Type>,
                    ),
                ),
                field_name: "_im1".to_string(),
            },
            SourceInterfaceMethodData {
                name: "cast_from".to_string(),
                arg_type: RefCell::new(Some(any_type.clone())),
                ret_type: RefCell::new(
                    Some(
                        std::rc::Rc::new(
                            crate::eval::code_value_extern::TypeCodeValueExternDynamic,
                        ) as std::rc::Rc<dyn crate::langhelp::Type>,
                    ),
                ),
                field_name: "_im2".to_string(),
            },
            SourceInterfaceMethodData {
                name: "desc".to_string(),
                arg_type: RefCell::new(
                    Some(std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as std::rc::Rc<dyn crate::langhelp::Type>),
                ),
                ret_type: RefCell::new(
                    Some(
                        std::rc::Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic) as std::rc::Rc<dyn crate::langhelp::Type>,
                    ),
                ),
                field_name: "_im3".to_string(),
            },
        ]),
        identity: crate::eval::builtin_type_iface_identity(),
    });
    builtin_def_set.interfaces.borrow_mut().push(value_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(type_iface_data.clone());
    (value_iface_data, type_iface_data)
}

impl crate::eval::dynamic_wrap::DynamicWrap for std::rc::Rc<SourceInterfaceRef> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<std::rc::Rc<SourceInterfaceRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }

    fn mortar_type() -> std::rc::Rc<dyn crate::langhelp::Type> {
        std::rc::Rc::new(dynamic::TypeSourceInterfaceDynamic)
    }
}
