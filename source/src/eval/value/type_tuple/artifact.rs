use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactTupleType {
    pub elements: Vec<Artifact>,
}
pub(crate) struct TypeTupleArtifact;
