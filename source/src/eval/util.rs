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
