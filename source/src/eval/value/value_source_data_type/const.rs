use {
    crate::artifact::ExporterAny,
    crate::{
        eval::value::type_source_data_type::{
            SourceDataTypeData,
            r#const::TypeSourceDataTypeConst,
        },
        langhelp::{
            EResult,
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

impl ExporterAny for ValueSourceDataTypeConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for ValueSourceDataTypeConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }))
    }
}
