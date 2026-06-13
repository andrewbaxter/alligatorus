//! `AspectAspect` — implemented on the *type* of an interface-bearing value
//! (`TypeSourceInterfaceDynamic` / `TypeSourceInterfaceConst`, the wrappers
//! for `Rc<SourceInterfaceRef>`). Exposes a value's `Unique` aspect identity
//! so the source-level `aspect(base, key)` builtin can pull the id out of
//! its `key` argument without RTTI on `dyn Value`.

use {
    crate::langhelp::{
        Aspect,
        EResult,
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectAspect {
    fn get_unique(&self, value: &dyn Value) -> Unique;
    fn get_iface_data(
        &self,
        value: &dyn Value,
    ) -> dumpster::unsync::Gc<crate::eval::value::type_source_interface::SourceInterfaceData>;
});
