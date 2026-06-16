pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::definition_set::DefinitionSet,
        langhelp::{
            ArtifactId,
            External,
            ScopeEntryId,
            Type,
            Value,
        },
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
    pub arg_type: RefCell<Option<Rc<dyn Type>>>,
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub name: Option<String>,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub set: Gc<DefinitionSet>,
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
