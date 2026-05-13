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

/// Returns the first non-error EvalRes or an error EvalRes containing all failures.
macro_rules! er_one{
    ($($i: ident = $e: expr => $b: expr,) *) => {
        'done: {
            let mut maybe_errors = vec![];
            $('next: {
                let $i = $e;
                if $i.v.is_none() {
                    maybe_errors.extend($i.errors);
                    break 'next;
                }
                let errors = $i.errors;
                let $i = $i.v.unwrap();
                let mut out = $b;
                out.errors.extend(errors);
                break 'done out;
            }) * 
            //. .
            break 'done EResult {
                v: None,
                errors: maybe_errors,
            };
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
