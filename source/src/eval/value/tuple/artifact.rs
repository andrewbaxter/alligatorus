use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactTupleType {
    pub elements: Vec<Artifact>,
}

pub(crate) struct TypeTupleArtifact;
