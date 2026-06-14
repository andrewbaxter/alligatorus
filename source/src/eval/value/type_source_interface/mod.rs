pub mod dynamic;
pub mod r#const;

// `build.rs` writes per-interface specs (method names + source-side
// arg/ret types) into `$OUT_DIR/gen_builtin_interfaces.rs` and we include
// that here so the build runs the spec→Rust generation step every build.
include!(concat!(env!("OUT_DIR"), "/gen_builtin_interfaces.rs"));

use {
    crate::langhelp::Unique,
    dumpster::unsync::Gc,
    std::cell::{Cell, RefCell},
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
    /// When `true`, this interface is treated as an aspect at runtime:
    /// implementers get a `cast_to` path that wraps the value as a
    /// `Rc<dyn Aspect>` (paralleling the Rust builtin aspects), and the
    /// implementer's `Type::aspect(iface.identity)` lookup returns a handle
    /// that dispatches the interface's methods on the v8 instance.
    /// `Cell` rather than plain field so source code can flip it via
    /// `iface.set_is_aspect(true)` after `new_interface()`.
    pub is_aspect: Cell<bool>,
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

// `BuiltinInterfaces` struct used to live here; replaced by the
// build.rs-generated `GenBuiltinInterfaces` (same shape). Callers use
// `gen_build_builtin_interfaces(import_state)` directly.
pub(crate) use GenBuiltinInterfaces as BuiltinInterfaces;

// Ids match `build.rs::interfaces()` entries for `code_dyn` / `scope_entry`
// / `target`. Kept here as thin wrappers so existing callers
// (`type_any_target::dynamic`, etc.) compile unchanged; the canonical
// source of truth is the spec.
pub(crate) fn builtin_code_dyn_iface_identity() -> Unique { Unique::Builtin(2007) }
pub(crate) fn builtin_scope_entry_iface_identity() -> Unique { Unique::Builtin(2005) }
pub(crate) fn builtin_target_iface_identity() -> Unique { Unique::Builtin(2006) }

// `build_builtin_interfaces` removed — callers now invoke
// `gen_build_builtin_interfaces(import_state)` (from `build.rs`'s generated
// gen_builtin_interfaces.rs) directly.

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
