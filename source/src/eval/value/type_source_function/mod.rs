pub mod dynamic;

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
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
    pub name: Option<String>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub arg_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub ret_type: RefCell<Option<Rc<dyn crate::langhelp::Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceFunctionData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct SourceFunction {
    pub data: Gc<SourceFunctionData>,
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceFunction> {
    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(dynamic::TypeSourceFunctionDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<SourceFunction>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }
}
