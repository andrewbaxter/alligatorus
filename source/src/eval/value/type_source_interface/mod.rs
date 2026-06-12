pub mod dynamic;
pub mod r#const;

use {
    crate::langhelp::Unique,
    dumpster::unsync::Gc,
    std::cell::RefCell,
};

pub mod artifact;

#[derive(Clone, Copy, PartialEq, Eq, Debug, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum BuiltinInterfaceKind {
    /// The `value` interface: methods that every value must support (e.g.
    /// `type()`).
    Value,
    /// The `type` interface: when implemented on a data type, instances of the
    /// data type act as types (providing `can_cast_from`, `cast_from`, `desc`).
    Type,
    /// The `aspect_function` interface — source-side counterpart of the Rust
    /// `AspectFunction` trait. A data type implementing this gets `call(arg)`
    /// behavior; aspect lookup via `aspect_function_aspect_id()` returns a
    /// dispatcher that runs the user's `call` body.
    AspectFunction,
    /// The `aspect_field` interface — source-side counterpart of the Rust
    /// `AspectField` trait. Methods: `field(arg) -> any` and
    /// `field_type_opt(arg) -> any`.
    AspectField,
    /// The `scope_entry` interface — source-side counterpart of the Rust
    /// `ScopeEntry` trait. Method: `use_ -> any` (the value the binding
    /// resolves to).
    ScopeEntry,
    /// The `target` interface — source-side counterpart of the Rust `Target`
    /// trait. A data type implementing this can be used as a compilation
    /// target.
    Target,
    /// The `code_dyn` interface — empty marker (no methods). Implemented by a
    /// source-defined type to declare "I am a `Rc<dyn CodeDyn>`-style code
    /// AST". Used as the target of `dyn_code_handle.cast(T)` to extract a
    /// specific code-impl from a runtime `Code`.
    CodeDyn,
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

pub(crate) struct BuiltinInterfaces {
    pub value: Gc<SourceInterfaceData>,
    pub type_: Gc<SourceInterfaceData>,
    pub aspect_function: Gc<SourceInterfaceData>,
    pub aspect_field: Gc<SourceInterfaceData>,
    pub scope_entry: Gc<SourceInterfaceData>,
    pub target: Gc<SourceInterfaceData>,
    pub code_dyn: Gc<SourceInterfaceData>,
}

pub(crate) fn builtin_code_dyn_iface_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

pub(crate) fn builtin_scope_entry_iface_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

pub(crate) fn builtin_target_iface_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

/// Build the synthetic builtin `value`/`type`/`aspect_function`/`aspect_field`
/// interfaces. Identities are derived from stable identity helpers so
/// instances built in different compile contexts compare equal for aspect
/// lookups.
pub(crate) fn build_builtin_interfaces(
    import_state: crate::langhelp::ImportStateId,
) -> BuiltinInterfaces {
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
            field_name: "_im_v0".to_string(),
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
                field_name: "_im_t1".to_string(),
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
                field_name: "_im_t2".to_string(),
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
                field_name: "_im_t3".to_string(),
            },
        ]),
        identity: crate::eval::builtin_type_iface_identity(),
    });
    let aspect_function_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::AspectFunction),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("aspect_function".to_string())),
        methods: RefCell::new(vec![SourceInterfaceMethodData {
            name: "call".to_string(),
            arg_type: RefCell::new(Some(any_type.clone())),
            ret_type: RefCell::new(Some(any_type.clone())),
            field_name: "_im_af0".to_string(),
        }]),
        identity: crate::builtin::aspect_function_aspect_id(),
    });
    let aspect_field_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::AspectField),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("aspect_field".to_string())),
        methods: RefCell::new(vec![
            SourceInterfaceMethodData {
                name: "field".to_string(),
                arg_type: RefCell::new(Some(any_type.clone())),
                ret_type: RefCell::new(Some(any_type.clone())),
                field_name: "_im_afd0".to_string(),
            },
            SourceInterfaceMethodData {
                name: "field_type_opt".to_string(),
                arg_type: RefCell::new(Some(any_type.clone())),
                ret_type: RefCell::new(Some(any_type.clone())),
                field_name: "_im_afd1".to_string(),
            },
        ]),
        identity: crate::builtin::aspect_field_aspect_id(),
    });
    let scope_entry_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::ScopeEntry),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("scope_entry".to_string())),
        methods: RefCell::new(vec![SourceInterfaceMethodData {
            name: "use_".to_string(),
            arg_type: RefCell::new(Some(any_type.clone())),
            ret_type: RefCell::new(Some(any_type.clone())),
            field_name: "_im_se0".to_string(),
        }]),
        identity: builtin_scope_entry_iface_identity(),
    });
    let target_method = |name: &str, idx: usize| SourceInterfaceMethodData {
        name: name.to_string(),
        arg_type: RefCell::new(Some(any_type.clone())),
        ret_type: RefCell::new(Some(any_type.clone())),
        field_name: format!("_im_tg{}", idx),
    };
    let target_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::Target),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("target".to_string())),
        methods: RefCell::new(vec![
            target_method("bind", 0),
            target_method("bind_sub_record", 1),
            target_method("bind_sub_tuple", 2),
            target_method("block", 3),
            target_method("code_to_bind_sub", 4),
            target_method("if_", 5),
            target_method("match_", 6),
            target_method("maybe_bind_sub", 7),
            target_method("stage", 8),
        ]),
        identity: builtin_target_iface_identity(),
    });
    let code_dyn_iface_data = Gc::new(SourceInterfaceData {
        builtin_kind: Some(BuiltinInterfaceKind::CodeDyn),
        set: builtin_def_set.clone(),
        description: RefCell::new(Some("code_dyn".to_string())),
        methods: RefCell::new(vec![]),
        identity: builtin_code_dyn_iface_identity(),
    });
    builtin_def_set.interfaces.borrow_mut().push(value_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(type_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(aspect_function_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(aspect_field_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(scope_entry_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(target_iface_data.clone());
    builtin_def_set.interfaces.borrow_mut().push(code_dyn_iface_data.clone());
    BuiltinInterfaces {
        value: value_iface_data,
        type_: type_iface_data,
        aspect_function: aspect_function_iface_data,
        aspect_field: aspect_field_iface_data,
        scope_entry: scope_entry_iface_data,
        target: target_iface_data,
        code_dyn: code_dyn_iface_data,
    }
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
