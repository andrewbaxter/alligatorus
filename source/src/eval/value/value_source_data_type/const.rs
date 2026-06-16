use {
    crate::{
        eval::value::type_source_data_type::{
            SourceDataTypeData,
            r#const::TypeSourceDataTypeConst,
        },
        langhelp::{
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) struct ValueSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
    pub isolate_bundle: RefCell<Option<v8::Global<v8::Object>>>,
    pub underlying: Rc<dyn Value>,
}

impl Value for ValueSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }
}
