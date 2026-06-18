use {
    crate::{
        builtin::ArtifactDeserializer,
        eval::{
            artifact::{
                ArtifactDeserializer,
                ArtifactId,
                ArtifactIdBuiltin,
                ArtifactIdSource,
                impl_deserialize_independent_artifact,
            },
            value::type_list::dynamic::TypeListDynamic,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            Type,
        },
    },
    std::{
        any::Any,
        collections::HashMap,
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactListType {
    pub element_type: ArtifactId,
}
pub(crate) struct Deserializer;

impl ArtifactDeserializer for Deserializer {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: ArtifactIdSource,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        return impl_deserialize_independent_artifact::<ArtifactListType>(&xx, loc, &id, data, |data| {
            let shell = Rc::new(TypeListDynamic { element_type: None });
            return Ok((shell as Rc<dyn Any>, vec![data.element_type], Box::new({
                let shell = shell.clone();
                |xx, loc| {
                    let Some(element_type) = xx.vx.ix.deserialized_artifacts.get(data.element_type) else {
                        return;
                    };
                    shell.element_type.borrow_mut() = element_type;
                }
            })));
        });
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactDeserializer>>) {
    m.insert(TypeListArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeListArtifact;
