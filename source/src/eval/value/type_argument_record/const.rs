use {
    crate::{
        artifact::ExporterAny,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectField,
            Binding,
            aspect_field_aspect_id,
            aspect_string_const_get,
        },
    },
    std::{
        any::Any,
        collections::BTreeMap,
        rc::Rc,
    },
};

pub(crate) struct ArgumentRecord {
    pub fields: BTreeMap<ConstKey, Rc<dyn Binding>>,
}

impl ArgumentRecord {
    fn clone_shallow(&self) -> ArgumentRecord {
        ArgumentRecord {
            fields: self.fields.iter().map(|(k, v)| (k.clone(), v.clone())).collect(),
        }
    }
}

impl Aspect for ArgumentRecord { }

impl AspectField for ArgumentRecord {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = match key.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        let const_key = ConstKey::String(key_name.clone());
        let Some(binding) = self.fields.get(&const_key) else {
            return EvalRes {
                v: None,
                errors: Some(vec![ExprErr {
                    loc,
                    errors: vec![
                        ExprSubErr::Opaque(format!("Field '{}' not found on argument record", key_name)),
                    ],
                }]),
            };
        };
        let cv = binding.use_(xx, loc, Code {
            loc,
            code: Code_::None,
        });
        EvalRes::ok(cv)
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        None
    }
}

impl ExporterAny for ArgumentRecord {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(self.clone_shallow()))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(self.clone_shallow()))
    }
}

impl Type for ArgumentRecord {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone_shallow())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone_shallow()) as Box<dyn AspectField>));
        }
        None
    }

    fn desc(&self) -> String {
        format!("argument_record({})", self.fields.keys().map(|k| format!("{:?}", k)).collect::<Vec<_>>().join(", "))
    }
}

impl Value for ArgumentRecord {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(self.clone_shallow())
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(self.clone_shallow()))
    }
}

pub(crate) struct ArgumentRecordBinding {
    pub record: Rc<ArgumentRecord>,
}

impl Binding for ArgumentRecordBinding {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: self.record.clone(),
        }
    }
}
