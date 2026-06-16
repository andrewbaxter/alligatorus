use {
    crate::{
        eval::value::type_source_data_type::{
            SourceDataTypeData,
            TypeSourceDataTypeConst,
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
    /// Set when the data type implements the builtin `type` interface and the stage
    /// that constructed this instance captured a v8 handle to the shared isolate.
    /// `TypePull.cast_from` uses this to dispatch the compiled `cast_from` method
    /// via v8.
    pub isolate_bundle: RefCell<Option<v8::Global<v8::Object>>>,
    pub underlying: Rc<dyn Value>,
}

impl Value for ValueSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }
}
