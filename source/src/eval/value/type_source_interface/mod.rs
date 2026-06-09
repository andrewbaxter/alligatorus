pub mod dynamic;
pub mod r#const;

use {
    crate::langhelp::Unique,
    dumpster::unsync::Gc,
    std::cell::RefCell,
};

#[derive(Clone, Copy, PartialEq, Eq)]
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
