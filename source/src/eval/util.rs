use {
    crate::{
        langhelp::{
            Artifact,
            ArtifactId,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::{
            aspect_artifact_get,
            aspect_type_get,
        },
    },
    std::rc::Rc,
};

#[macro_export]
macro_rules! er_all{
    ($v: ident, {
        $($i: ident = $e: expr,) *
    }) => {
        $(let $i = $v.must_er($e);) * 
        //. .
        $(let $i = $i ?;) *
    };
}

#[macro_export]
macro_rules! er_seq{
    ($loc: expr, $v: ident, $t: ty, $($x: tt) *) => {
        {
            let mut $v = $crate:: eval:: Erb:: new($loc);
            match(||-> :: core:: result:: Result < $t, () > {
                $($x) *
            })() {
                :: core:: result:: Result:: Ok(v) => {
                    let errs = :: core:: mem:: take(& mut $v.errors);
                    $crate:: langhelp:: EResult {
                        v: :: core:: option:: Option:: Some(v),
                        errors: if errs.is_empty() {
                            ::core::option::Option::None
                        }
                        else {
                            ::core::option::Option::Some(errs)
                        },
                    }
                },
                :: core:: result:: Result:: Err(()) => $crate:: langhelp:: EResult {
                    v: :: core:: option:: Option:: None,
                    errors: :: core:: option:: Option:: Some(:: core:: mem:: take(& mut $v.errors)),
                },
            }
        }
    };
}

#[macro_export]
macro_rules! er_seq_cv{
    ($loc: expr, $v: ident, $($x: tt) *) => {
        $crate:: er_seq !($loc, $v, $crate:: langhelp:: CodeValue, $($x) *)
    };
}

pub(crate) fn deserialize_artifact(xx: &ExprContext, loc: QExprId, artifact: &Artifact) -> EvalRes {
    match &artifact.type_ {
        ArtifactId::Builtin(id) => {
            let Some(deserializer) = xx.vx.ix.builtin_deserializers.get(id) else {
                return EvalRes::err(loc, format!("Unknown builtin artifact ID: {:?}", id));
            };
            deserializer.value_from_serial(xx, loc, &artifact.data)
        },
        ArtifactId::Source(_) => {
            er_seq_cv_inline(loc, |erb| {
                let (type_, type_code) = erb.must_er(deserialize_type_artifact(xx, loc, artifact))?;
                let aspect_artifact = erb.must_er(aspect_artifact_get(xx, loc, type_.as_ref()))?;
                let data_cv = erb.must_er(aspect_artifact.from_serial(xx, loc, &artifact.data))?;
                if matches!(type_code.code, Code_::None) {
                    Ok(data_cv)
                } else {
                    let combined_code = erb.must_er(xx.target.seq(loc, vec![type_code, data_cv.code]))?;
                    Ok(CodeValue {
                        code: combined_code,
                        value: data_cv.value,
                    })
                }
            })
        },
    }
}

pub(crate) fn deserialize_type_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: &Artifact,
) -> EResult<(Rc<dyn Type>, Code)> {
    match &artifact.type_ {
        ArtifactId::Builtin(id) => {
            match xx.vx.ix.builtin_deserializers.get(id) {
                Some(deserializer) => deserializer.type_from_serial(xx, loc, &artifact.data),
                None => EResult::err(loc, format!("Unknown builtin artifact ID: {:?}", id)),
            }
        },
        ArtifactId::Source(compile_id) => {
            let source_artifact = match xx.vx.ix.gx.artifacts.get(ArtifactId::Source(*compile_id)) {
                Ok(v) => v,
                Err(e) => return EResult::err(loc, e),
            };
            let cv = match deserialize_artifact(xx, loc, &source_artifact).into_res() {
                Ok(v) => v,
                Err(e) => return EResult {
                    v: None,
                    errors: Some(e),
                },
            };
            let type_value_type = cv.value.type_(xx, loc);
            match aspect_type_get(xx, loc, type_value_type.as_ref()).into_res() {
                Ok(aspect_type) => {
                    EResult::ok((aspect_type.type_(cv.value.as_ref()), cv.code))
                },
                Err(e) => EResult {
                    v: None,
                    errors: Some(e),
                },
            }
        },
    }
}

pub(crate) fn er_seq_cv_inline(loc: QExprId, f: impl FnOnce(&mut super::Erb) -> Result<CodeValue, ()>) -> EvalRes {
    let mut erb = super::Erb::new(loc);
    match f(&mut erb) {
        Ok(v) => {
            let errs = std::mem::take(&mut erb.errors);
            EResult {
                v: Some(v),
                errors: if errs.is_empty() {
                    None
                } else {
                    Some(errs)
                },
            }
        },
        Err(()) => EResult {
            v: None,
            errors: Some(std::mem::take(&mut erb.errors)),
        },
    }
}
