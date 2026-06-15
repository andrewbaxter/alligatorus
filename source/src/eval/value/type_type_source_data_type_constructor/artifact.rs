use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
        eval::Erb,
        langhelp::{
            ArtifactIdBuiltin,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
    },
    std::{
        collections::HashMap,
        mem,
        rc::Rc,
    },
};

pub(crate) struct Deserializer;

impl ArtifactBuiltinDeserializer for Deserializer {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeTypeSourceDataTypeConstructorDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let (type_, type_codes) =
                erb.must_res(
                    crate::eval::value::type_source_data_type_constructor::dynamic::deserialize_defset_ctor_as_type(
                        xx,
                        loc,
                        data,
                    ),
                )?;
            let code = erb.must_er(xx.target.block(loc, type_codes))?;
            return Ok(CodeValue {
                code,
                value: type_.as_value()
            });
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::dynamic::TypeTypeSourceDataTypeConstructorDynamic::artifact_id(), Rc::new(Deserializer));
}
