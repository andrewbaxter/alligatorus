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
    std::rc::Rc,
};

pub(crate) struct ValueSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
    pub underlying: Rc<dyn Value>,
}

impl Value for ValueSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }
}
