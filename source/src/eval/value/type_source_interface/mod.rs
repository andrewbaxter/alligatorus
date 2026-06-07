pub mod dynamic;

use {
    crate::langhelp::Unique,
    dumpster::unsync::Gc,
    std::cell::RefCell,
};

pub(crate) struct SourceInterfaceMethodData {
    pub name: String,
    pub arg_type: RefCell<Option<std::rc::Rc<dyn crate::langhelp::Type>>>,
    pub ret_type: RefCell<Option<std::rc::Rc<dyn crate::langhelp::Type>>>,
    pub field_name: String,
}

pub(crate) struct SourceInterfaceData {
    pub set: Gc<crate::eval::definition_set::DefinitionSet>,
    pub description: RefCell<Option<String>>,
    pub methods: RefCell<Vec<SourceInterfaceMethodData>>,
    pub identity: Unique,
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceInterfaceData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}

pub(crate) struct TypeSourceInterfaceConst {
    pub data: Gc<SourceInterfaceData>,
}
