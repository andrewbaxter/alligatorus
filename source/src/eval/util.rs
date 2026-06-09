/// Unpacks all EvalRes.
macro_rules! er_all{
    ($v: ident, {
        $($i: ident = $e: expr,) *
    }) => {
        $(let $i = $v.must_er($e);) * 
        //. .
        $(let $i = $i ?;) *
    };
}

macro_rules! er_seq{
    ($loc: expr, $v: ident, $t: ty, $($x: tt) *) => {
        {
            let mut $v = Erb:: new($loc);
            match(||-> Result < $t, () > {
                $($x) *
            })() {
                Ok(v) => {
                    let errs = mem:: take(& mut $v.errors);
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
                    errors: Some(mem:: take(& mut $v.errors)),
                },
            }
        }
    };
}

/// Returns final value as EvalRes, or first error EvalRes.
macro_rules! er_seq_cv{
    ($loc: expr, $v: ident, $($x: tt) *) => {
        er_seq!($loc, $v, CodeValue, $($x) *)
    };
}

pub(crate) fn deserialize_artifact(
    xx: &crate::langhelp::ExprContext,
    loc: crate::langhelp::QExprId,
    artifact: &crate::langhelp::Artifact,
) -> crate::langhelp::EvalRes {
    er_seq_cv_inline(loc, |erb| {
        let (type_, type_code) = match &artifact.type_ {
            crate::langhelp::ArtifactId::Builtin(id) => {
                let Some(type_) = xx.vx.ix.builtin_types.get(id) else {
                    return erb.err_new(format!("Unknown builtin type ID: {:?}", id));
                };
                (type_.clone(), crate::langhelp::Code {
                    loc: loc,
                    code: crate::langhelp::Code_::None,
                })
            },
            crate::langhelp::ArtifactId::Source(compile_id) => {
                erb.must_er(deserialize_type_artifact(xx, loc, &crate::langhelp::ArtifactId::Source(*compile_id)))?
            },
        };
        let aspect_artifact = erb.must_er(crate::builtin::aspect_artifact_get(xx, loc, type_.as_ref()))?;
        let data_cv = erb.must_er(aspect_artifact.from_serial(xx, loc, &artifact.data))?;
        if matches!(type_code.code, crate::langhelp::Code_::None) {
            Ok(data_cv)
        } else {
            let combined_code = erb.must_er(xx.target.block(loc, vec![type_code, data_cv.code]))?;
            Ok(crate::langhelp::CodeValue {
                code: combined_code,
                value: data_cv.value,
            })
        }
    })
}

pub(crate) fn deserialize_type_artifact(
    xx: &crate::langhelp::ExprContext,
    loc: crate::langhelp::QExprId,
    id: &crate::langhelp::ArtifactId,
) -> crate::langhelp::EResult<(std::rc::Rc<dyn crate::langhelp::Type>, crate::langhelp::Code)> {
    match id {
        crate::langhelp::ArtifactId::Builtin(id) => {
            match xx.vx.ix.builtin_types.get(id) {
                Some(type_) => crate::langhelp::EResult::ok((type_.clone(), crate::langhelp::Code {
                    loc: loc,
                    code: crate::langhelp::Code_::None,
                })),
                None => crate::langhelp::EResult::err(loc, format!("Unknown builtin type ID: {:?}", id)),
            }
        },
        crate::langhelp::ArtifactId::Source(compile_id) => {
            let artifact = match xx.vx.ix.gx.artifacts.get(crate::langhelp::ArtifactId::Source(*compile_id)) {
                Ok(v) => v,
                Err(e) => return crate::langhelp::EResult::err(loc, e),
            };
            let cv = match deserialize_artifact(xx, loc, &artifact).into_res() {
                Ok(v) => v,
                Err(e) => return crate::langhelp::EResult {
                    v: None,
                    errors: Some(e),
                },
            };
            let type_value_type = cv.value.type_(xx, loc);
            match crate::builtin::aspect_type_get(xx, loc, type_value_type.as_ref()).into_res() {
                Ok(aspect_type) => {
                    crate::langhelp::EResult::ok((aspect_type.type_(cv.value.as_ref()), cv.code))
                },
                Err(e) => crate::langhelp::EResult {
                    v: None,
                    errors: Some(e),
                },
            }
        },
    }
}

pub(crate) fn er_seq_cv_inline(
    loc: crate::langhelp::QExprId,
    f: impl FnOnce(&mut super::Erb) -> Result<crate::langhelp::CodeValue, ()>,
) -> crate::langhelp::EvalRes {
    let mut erb = super::Erb::new(loc);
    match f(&mut erb) {
        Ok(v) => {
            let errs = std::mem::take(&mut erb.errors);
            crate::langhelp::EResult {
                v: Some(v),
                errors: if errs.is_empty() {
                    None
                } else {
                    Some(errs)
                },
            }
        },
        Err(()) => crate::langhelp::EResult {
            v: None,
            errors: Some(std::mem::take(&mut erb.errors)),
        },
    }
}
