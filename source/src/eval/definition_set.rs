use {
    crate::{
        eval::{
            dynamic_wrap::DynamicWrap,
            value::{
                type_source_data::SourceDataTypeData,
                type_source_function::SourceFunctionData,
                type_source_interface::SourceInterfaceData,
            },
        },
        langhelp::{
            Counter,
            DefSetId,
            ImportStateId,
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

pub(crate) struct DefinitionSet {
    pub data_types: RefCell<Vec<Gc<SourceDataTypeData>>>,
    pub def_set_id: DefSetId,
    pub finished: Cell<bool>,
    pub functions: RefCell<Vec<Gc<SourceFunctionData>>>,
    pub import_id: ImportStateId,
    pub interfaces: RefCell<Vec<Gc<SourceInterfaceData>>>,
    pub next_entry_id: Counter,
    pub next_field_id: Counter,
    pub next_interface_method_id: Counter,
}

impl DefinitionSet {
    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        let functions = self.functions.borrow();
        for func_data in functions.iter() {
            let name = func_data.name.as_deref().unwrap_or("(unnamed)");
            if func_data.impl_expr.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an implementation", name));
            }
            if func_data.arg_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an arg type", name));
            }
            if func_data.ret_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing a return type", name));
            }
        }
        let data_types = self.data_types.borrow();
        for dt_data in data_types.iter() {
            let name = dt_data.name.as_deref().unwrap_or("(unnamed)");
            if dt_data.underlying_type.borrow().is_none() {
                return Err(format!("Data type '{}' is missing underlying type", name));
            }
            for method in dt_data.methods.borrow().iter() {
                if method.impl_expr.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing implementation", name, method.name));
                }
                if method.ret_type.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing a return type", name, method.name));
                }
            }
        }
        let interfaces = self.interfaces.borrow();
        for (i, iface) in interfaces.iter().enumerate() {
            let iface_name = iface.description.borrow().clone().unwrap_or_else(|| format!("(interface {})", i));
            for method in iface.methods.borrow().iter() {
                if method.arg_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing an arg type", iface_name, method.name),
                    );
                }
                if method.ret_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing a return type", iface_name, method.name),
                    );
                }
            }
        }
        self.finished.set(true);
        Ok(())
    }

    pub fn new(import_id: ImportStateId, def_set_id: DefSetId) -> Self {
        DefinitionSet {
            import_id,
            def_set_id,
            functions: RefCell::new(Vec::new()),
            data_types: RefCell::new(Vec::new()),
            interfaces: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: Counter::default(),
            next_entry_id: Counter::default(),
            next_interface_method_id: Counter::default(),
        }
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSet {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        let functions = self.functions.try_borrow().map_err(|_| ())?;
        for f in functions.iter() {
            visitor.visit_unsync(f);
        }
        drop(functions);
        let data_types = self.data_types.try_borrow().map_err(|_| ())?;
        for dt in data_types.iter() {
            visitor.visit_unsync(dt);
        }
        drop(data_types);
        let interfaces = self.interfaces.try_borrow().map_err(|_| ())?;
        for i in interfaces.iter() {
            visitor.visit_unsync(i);
        }
        Ok(())
    }
}

pub(crate) struct DefinitionSetExports {
    pub set: Gc<DefinitionSet>,
}

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
}

impl DynamicWrap for Rc<DefinitionSetExports> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<DefinitionSetExports>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }

    fn mortar_type() -> Rc<dyn crate::langhelp::Type> {
        Rc::new(crate::eval::value::type_source_exports::dynamic::TypeSourceExportsDynamic)
    }
}
