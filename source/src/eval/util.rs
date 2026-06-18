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
