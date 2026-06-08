use {
    crate::langhelp::{
        Code,
        CodeValue,
        Code_,
        ConstKey,
        EvalRes,
        ScopeEntry,
        Type,
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
    Error(Vec<crate::langhelp::ExprErr>),
    Mortar(Rc<dyn Type>),
    Scope(Box<dyn ScopeEntry>),
}

/// DynamicScopeEntry adapter for sourceless records (used by bind_dynamic when
/// record has no JS code). On use_, constructs a Record from the stored scope
/// entries.
pub(crate) struct ScopeEntryRecordMortar {
    pub fields: BTreeMap<ConstKey, RecordScopeField>,
    pub js_key: Option<crate::langhelp::ScopeEntryId>,
}

impl ScopeEntry for ScopeEntryRecordMortar {
    fn use_(&self, loc: crate::langhelp::QExprId, _code: Code) -> CodeValue {
        let has_mortar = self.fields.values().any(|f| matches!(f, RecordScopeField::Mortar(_)));
        let (use_code, source) = if has_mortar {
            let js_key = self.js_key.expect("ScopeEntryRecordMortar with mortar fields must have js_key");
            (
                Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(crate::code_dynamic::CodeDynamic {
                        loc: loc,
                        type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                            crate::code_dynamic::CodeDynamicBindingUse { id: js_key },
                        ),
                    })),
                },
                Some(
                    super::super::dynamic_util::ValueDynamicSource::Binding(
                        super::super::dynamic_util::ValueDynamicDataScope { key: js_key },
                    ),
                ),
            )
        } else {
            (Code {
                loc: loc,
                code: Code_::None,
            }, None)
        };
        let mut result_fields = BTreeMap::new();
        for (key, field) in &self.fields {
            match field {
                RecordScopeField::Scope(se) => {
                    let cv = se.use_(loc, Code {
                        loc: loc,
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
        CodeValue {
            code: use_code,
            value: Rc::new(Record {
                source: source,
                fields: result_fields,
            }),
        }
    }
}
