use {
    crate::{
        artifact::ExporterAny,
        eval::value::type_source_data_type::{
            SourceDataTypeData,
            r#const::TypeSourceDataTypeConst,
        },
        langhelp::{
            EResult,
            ExprContext,
            QExprId,
            Type,
            Value_,
        },
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct ValueSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
    pub isolate_bundle: RefCell<Option<v8::Global<v8::Object>>>,
    pub underlying: Rc<dyn Value_>,
}

impl ExporterAny for ValueSourceDataTypeConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(self.clone()))
    }
}

impl Value_ for ValueSourceDataTypeConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(self.clone())
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }))
    }
}
