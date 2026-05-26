macro_rules! er_seq{
    ($loc: expr, $v: ident, $t: ty, $($x: tt) *) => {
        {
            let mut $v = Erb:: new($loc);
            match(||-> Result < $t, () > {
                $($x) *
            })() {
                Ok(v) => EResult {
                    v: Some(v),
                    errors: mem:: take(& mut $v.errors),
                },
                Err(()) => EResult {
                    v: None,
                    errors: mem:: take(& mut $v.errors),
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

macro_rules! r_all{
    ($v: ident, {
        $($i: ident = $e: expr,) *
    }) => {
        $(let $i = $v.must_res($e);) * 
        //. .
        $(let $i = $i ?;) *
    };
}

/// Returns the first matching Option or an error EResult if none match.
macro_rules! er_one{
    ($loc: expr, $ctx: expr, {
        $($i: ident = $e: expr => $b: expr,) *
    }) => {
        'done: {
            $('next: {
                let Some($i) = $e else {
                    break 'next;
                };
                break 'done $b;
            }) * 
            //. .
            break 'done EResult:: err($loc, format!("No match while {}", $ctx));
        }
    };
}

macro_rules! r_one{
    ($loc: expr, {
        $($i: ident = $e: expr => $b: expr,) *
    }) => {
        'done: {
            let mut errors = vec![];
            $('next: {
                let $i = match $e {
                    Ok(v) => v,
                    Err(e) => {
                        errors.push(ExprErr {
                            loc: $loc,
                            errors: vec ![ExprSubErr::Opaque(e.to_string())]
                        });
                        break 'next;
                    }
                };
                break 'done($b);
            }) * 
            //. .
            break 'done EvalRes {
                v: None,
                errors: errors,
            };
        }
    };
}

macro_rules! erb_async{
    ($loc: expr, $v: ident $x: tt) => {
        let mut $v = Erb:: new($loc);
        match async {
            ta_return!(EvalRes, EvalRes);
            $x
        }
        .await {
            Ok(er) => return er,
            Err(er) => return er,
        }
    };
}
