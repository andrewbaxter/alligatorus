use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EvalRes,
            ScopeEntry,
            Type,
        },
    },
    std::{
        collections::BTreeMap,
        rc::Rc,
    },
    super::{
        Record,
        RecordField,
    },
};

/// Scope entry for a bound record. Maps each key to one of: a DynamicScopeEntry
/// (for fields that were mortar-only), a ScopeEntry (for fields that were fully
/// evaluated), or an error.
pub(crate) enum RecordScopeField {
    Mortar(Rc<dyn Type>),
    Scope(Box<dyn ScopeEntry>),
    Error(Vec<crate::langhelp::ExprErr>),
}

/// DynamicScopeEntry adapter for sourceless records (used by bind_js when record
/// has no JS code). On use_, constructs a Record from the stored scope entries.
pub(crate) struct ScopeEntryRecordMortar {
    pub fields: BTreeMap<ConstKey, RecordScopeField>,
}

impl super::super::value_scope_entry::dynamic::DynamicScopeEntry for ScopeEntryRecordMortar {
    fn use_(&self, value: super::super::value_dynamic::ValueDynamic) -> EvalRes {
        let has_mortar = self.fields.values().any(|f| matches!(f, RecordScopeField::Mortar(_)));
        let (loc, code, source) = if has_mortar {
            let (loc, key) = match &value.source {
                super::super::value_dynamic::ValueDynamicSource::Binding(b) => (b.loc, b.key),
                _ => unreachable!("ScopeEntryRecordMortar with mortar fields called with non-binding source"),
            };
            (loc, Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: key }),
                })),
            }, Some(value.source))
        } else {
            let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
            (loc, Code {
                loc,
                code: Code_::None,
            }, None)
        };
        let mut result_fields = BTreeMap::new();
        for (key, field) in &self.fields {
            match field {
                RecordScopeField::Scope(se) => {
                    let cv = se.use_(loc, Code {
                        loc,
                        code: Code_::None,
                    });
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes::ok(cv)));
                },
                RecordScopeField::Mortar(t) => {
                    result_fields.insert(key.clone(), RecordField::Mortar(t.clone()));
                },
                RecordScopeField::Error(errs) => {
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes {
                        v: None,
                        errors: Some(errs.clone()),
                    }));
                },
            }
        }
        EvalRes::ok(CodeValue {
            code,
            value: Rc::new(Record {
                source,
                fields: result_fields,
            }),
        })
    }
}
