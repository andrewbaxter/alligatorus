pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::langhelp::{
        ScopeEntryId,
        Value,
    },
    dumpster::unsync::Gc,
    std::{
        cell::{
            Cell,
            RefCell,
        },
        rc::Rc,
    },
};

pub(crate) struct SourceFunctionData {
    pub arg_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    /// Stable artifact id assigned during serialization.
    pub artifact_id: RefCell<Option<crate::langhelp::ArtifactId>>,
    /// Externals (`a##` bindings) captured by the body's compile environment.
    /// `compiled_js` references these by their Local id; they must be installed as v8
    /// globals before the body runs.
    pub captured_externals: RefCell<Vec<(ScopeEntryId, crate::langhelp::External)>>,
    /// Rendered JS body (e.g. `function d0_0_0(a0) { ... }`). Populated after the
    /// def-set's functions are compiled at end-of-stage.
    pub compiled_js: RefCell<Option<String>>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub name: Option<String>,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
}

impl SourceFunctionData {
    pub fn finish(&self) -> Result<(), String> {
        let name = self.name.as_deref().unwrap_or("(unnamed)");
        if self.impl_expr.borrow().is_none() {
            return Err(format!("Definition set function '{}' is missing an implementation", name));
        }
        if self.arg_type.borrow().is_none() {
            return Err(format!("Definition set function '{}' is missing an arg type", name));
        }
        if self.ret_type.borrow().is_none() {
            return Err(format!("Definition set function '{}' is missing a return type", name));
        }
        Ok(())
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceFunctionData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct TypeSourceFunctionConst {
    pub data: Gc<SourceFunctionData>,
}

impl crate::eval::pull::Pull for Rc<TypeSourceFunctionConst> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<TypeSourceFunctionConst>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }

    fn dynamic_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceFunctionDynamic)
    }
}
