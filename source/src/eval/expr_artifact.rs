use {
    crate::{
        eval::{
            artifact::{
                Artifact,
                deserialize_artifact,
            },
            expr::{
                Expr,
                ExprAs,
                ExprBind,
                ExprCall,
                ExprConditionBranch,
                ExprField,
                ExprForeach,
                ExprGraft,
                ExprIf,
                ExprImport,
                ExprLower,
                ExprMatch,
                ExprMatchBranch,
                ExprOperatorAdd,
                ExprOperatorEq,
                ExprOperatorLt,
                ExprRecord,
                ExprScopeUse,
                ExprSeq,
                ExprSet,
                ExprStage,
                ExprTuple,
                Expr_,
                make_lit_value,
            },
        },
        langhelp::{
            ExprContext,
            QExprId,
            SourceId,
        },
        r#gen::aspect_artifact_get,
        syntax::ExprId,
    },
    serde::{
        Deserialize,
        Serialize,
    },
    std::rc::Rc,
};

#[derive(Serialize, Deserialize, Clone)]
#[serde(deny_unknown_fields)]
pub(crate) struct ArtifactCondBranch {
    pub body: ArtifactExpr,
    pub condition: ArtifactExpr,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactExpr {
    pub loc: ArtifactQExprId,
    pub r#type: Box<ArtifactExpr_>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", tag = "tag", content = "value", deny_unknown_fields)]
pub(crate) enum ArtifactExpr_ {
    As {
        base: ArtifactExpr,
        target: ArtifactExpr,
    },
    Bind {
        base: ArtifactExpr,
        value: ArtifactExpr,
    },
    Call {
        arg: ArtifactExpr,
        func: ArtifactExpr,
    },
    Field {
        base: ArtifactExpr,
        key: ArtifactExpr,
    },
    Foreach {
        base: ArtifactExpr,
        body: ArtifactExpr,
        element_key: ArtifactExpr,
    },
    Graft {
        expr: ArtifactExpr,
    },
    If {
        conditions: Vec<ArtifactCondBranch>,
        else_: Option<ArtifactExpr>,
    },
    Import {
        spec: ArtifactExpr,
    },
    Literal {
        value: Artifact,
    },
    Lower {
        expr: ArtifactExpr,
    },
    Match {
        base: ArtifactExpr,
        cases: Vec<ArtifactMatchBranch>,
        element_key: ArtifactExpr,
        fallback: Option<ArtifactExpr>,
    },
    OperatorAdd {
        base: ArtifactExpr,
        reference: ArtifactExpr,
    },
    OperatorEq {
        base: ArtifactExpr,
        reference: ArtifactExpr,
    },
    OperatorLt {
        base: ArtifactExpr,
        reference: ArtifactExpr,
    },
    Record {
        entries: Vec<(ArtifactExpr, ArtifactExpr)>,
    },
    ScopeUse {
        key: ArtifactExpr,
    },
    Seq {
        exprs: Vec<ArtifactExpr>,
    },
    Set {
        base: ArtifactExpr,
        value: ArtifactExpr,
    },
    Stage {
        expr: ArtifactExpr,
    },
    Tuple {
        elements: Vec<ArtifactExpr>,
    },
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(deny_unknown_fields)]
pub(crate) struct ArtifactMatchBranch {
    pub body: ArtifactExpr,
    pub condition: ArtifactExpr,
}

#[derive(Serialize, Deserialize, Clone, Copy)]
#[serde(deny_unknown_fields)]
pub(crate) struct ArtifactQExprId(pub usize, pub usize);

pub(crate) fn expr_from_artifact(xx: &ExprContext, art: &ArtifactExpr) -> Result<Expr, String> {
    let loc = loc_from(art.loc);
    let type_ = match art.r#type.as_ref() {
        ArtifactExpr_::As { base, target } => Expr_::As(ExprAs {
            base: Rc::new(expr_from_artifact(xx, base)?),
            target: Rc::new(expr_from_artifact(xx, target)?),
        }),
        ArtifactExpr_::Bind { base, value } => Expr_::Bind(ExprBind {
            base: Rc::new(expr_from_artifact(xx, base)?),
            value: Rc::new(expr_from_artifact(xx, value)?),
        }),
        ArtifactExpr_::Seq { exprs } => Expr_::Seq(
            ExprSeq { exprs: exprs.iter().map(|e| expr_from_artifact(xx, e)).collect::<Result<_, _>>()? },
        ),
        ArtifactExpr_::Call { arg, func } => Expr_::Call(ExprCall {
            arg: Rc::new(expr_from_artifact(xx, arg)?),
            func: Rc::new(expr_from_artifact(xx, func)?),
        }),
        ArtifactExpr_::Field { base, key } => Expr_::Field(ExprField {
            base: Rc::new(expr_from_artifact(xx, base)?),
            key: Rc::new(expr_from_artifact(xx, key)?),
        }),
        ArtifactExpr_::Foreach { base, body, element_key } => Expr_::Foreach(ExprForeach {
            base: Rc::new(expr_from_artifact(xx, base)?),
            body: Rc::new(expr_from_artifact(xx, body)?),
            element_key: Rc::new(expr_from_artifact(xx, element_key)?),
        }),
        ArtifactExpr_::Graft { expr } => Expr_::Graft(ExprGraft::Pending(Rc::new(expr_from_artifact(xx, expr)?))),
        ArtifactExpr_::If { conditions, else_ } => Expr_::If(ExprIf {
            conditions: conditions.iter().map(|b| Ok::<_, String>(ExprConditionBranch {
                condition: expr_from_artifact(xx, &b.condition)?,
                body: expr_from_artifact(xx, &b.body)?,
            })).collect::<Result<_, _>>()?,
            else_: else_.as_ref().map(|x| expr_from_artifact(xx, x).map(Rc::new)).transpose()?,
        }),
        ArtifactExpr_::Import { spec } => Expr_::Import(
            ExprImport { spec: Rc::new(expr_from_artifact(xx, spec)?) },
        ),
        ArtifactExpr_::Literal { value } => {
            let cv =
                deserialize_artifact(xx, loc, value)
                    .into_res()
                    .map_err(|e| format!("Failed to deserialize Literal value: {:?}", e))?;
            Expr_::Literal(make_lit_value(cv.value))
        },
        ArtifactExpr_::Lower { expr } => Expr_::Lower(ExprLower { expr: Rc::new(expr_from_artifact(xx, expr)?) }),
        ArtifactExpr_::Match { base, cases, element_key, fallback } => Expr_::Match(ExprMatch {
            base: Rc::new(expr_from_artifact(xx, base)?),
            cases: cases.iter().map(|c| Ok::<_, String>(ExprMatchBranch {
                condition: expr_from_artifact(xx, &c.condition)?,
                body: expr_from_artifact(xx, &c.body)?,
            })).collect::<Result<_, _>>()?,
            element_key: Rc::new(expr_from_artifact(xx, element_key)?),
            fallback: fallback.as_ref().map(|x| expr_from_artifact(xx, x).map(Rc::new)).transpose()?,
        }),
        ArtifactExpr_::OperatorAdd { base, reference } => Expr_::OperatorAdd(ExprOperatorAdd {
            base: Rc::new(expr_from_artifact(xx, base)?),
            reference: Rc::new(expr_from_artifact(xx, reference)?),
        }),
        ArtifactExpr_::OperatorEq { base, reference } => Expr_::OperatorEq(ExprOperatorEq {
            base: Rc::new(expr_from_artifact(xx, base)?),
            reference: Rc::new(expr_from_artifact(xx, reference)?),
        }),
        ArtifactExpr_::OperatorLt { base, reference } => Expr_::OperatorLt(ExprOperatorLt {
            base: Rc::new(expr_from_artifact(xx, base)?),
            reference: Rc::new(expr_from_artifact(xx, reference)?),
        }),
        ArtifactExpr_::Record { entries } => Expr_::Record(ExprRecord { entries: entries.iter().map(|(k, v)| {
            Ok::<_, String>((expr_from_artifact(xx, k)?, expr_from_artifact(xx, v)?))
        }).collect::<Result<_, _>>()? }),
        ArtifactExpr_::ScopeUse { key } => Expr_::ScopeUse(
            ExprScopeUse { key: Rc::new(expr_from_artifact(xx, key)?) },
        ),
        ArtifactExpr_::Set { base, value } => Expr_::Set(ExprSet {
            base: Rc::new(expr_from_artifact(xx, base)?),
            value: Rc::new(expr_from_artifact(xx, value)?),
        }),
        ArtifactExpr_::Stage { expr } => Expr_::Stage(ExprStage { expr: Rc::new(expr_from_artifact(xx, expr)?) }),
        ArtifactExpr_::Tuple { elements } => Expr_::Tuple(
            ExprTuple { elements: elements.iter().map(|e| expr_from_artifact(xx, e)).collect::<Result<_, _>>()? },
        ),
    };
    Ok(Expr {
        loc,
        r#type: type_,
    })
}

pub(crate) fn expr_to_artifact(xx: &ExprContext, expr: &Expr) -> Result<ArtifactExpr, String> {
    let loc = loc_to(expr.loc);
    let type_ = match &expr.r#type {
        Expr_::As(e) => ArtifactExpr_::As {
            base: expr_to_artifact(xx, &e.base)?,
            target: expr_to_artifact(xx, &e.target)?,
        },
        Expr_::Bind(e) => ArtifactExpr_::Bind {
            base: expr_to_artifact(xx, &e.base)?,
            value: expr_to_artifact(xx, &e.value)?,
        },
        Expr_::Seq(e) => ArtifactExpr_::Seq {
            exprs: e.exprs.iter().map(|s| expr_to_artifact(xx, s)).collect::<Result<_, _>>()?,
        },
        Expr_::Call(e) => ArtifactExpr_::Call {
            arg: expr_to_artifact(xx, &e.arg)?,
            func: expr_to_artifact(xx, &e.func)?,
        },
        Expr_::Field(e) => ArtifactExpr_::Field {
            base: expr_to_artifact(xx, &e.base)?,
            key: expr_to_artifact(xx, &e.key)?,
        },
        Expr_::Foreach(e) => ArtifactExpr_::Foreach {
            base: expr_to_artifact(xx, &e.base)?,
            body: expr_to_artifact(xx, &e.body)?,
            element_key: expr_to_artifact(xx, &e.element_key)?,
        },
        Expr_::Graft(e) => {
            let inner = match e {
                ExprGraft::Pending(inner) => inner,
                ExprGraft::Evaluated(_) => {
                    return Err("Cannot serialize Graft after it has been evaluated by stage processing".to_string());
                },
            };
            ArtifactExpr_::Graft { expr: expr_to_artifact(xx, inner)? }
        },
        Expr_::If(e) => ArtifactExpr_::If {
            conditions: e.conditions.iter().map(|b| Ok::<_, String>(ArtifactCondBranch {
                condition: expr_to_artifact(xx, &b.condition)?,
                body: expr_to_artifact(xx, &b.body)?,
            })).collect::<Result<_, _>>()?,
            else_: e.else_.as_ref().map(|x| expr_to_artifact(xx, x)).transpose()?,
        },
        Expr_::Import(e) => ArtifactExpr_::Import { spec: expr_to_artifact(xx, &e.spec)? },
        Expr_::Literal(p) => {
            let value = p.peek().ok_or_else(|| "Cannot serialize consumed Literal".to_string())?;
            let value_type = value.type_(xx, expr.loc);
            let aspect_artifact =
                aspect_artifact_get(xx, expr.loc, value_type.as_ref())
                    .into_res()
                    .map_err(|e| format!("Literal value type has no AspectArtifact: {:?}", e))?;
            let artifact =
                aspect_artifact
                    .to_serial(xx, expr.loc, value)
                    .into_res()
                    .map_err(|e| format!("Failed to serialize Literal value: {:?}", e))?;
            ArtifactExpr_::Literal { value: artifact }
        },
        Expr_::Lower(e) => ArtifactExpr_::Lower { expr: expr_to_artifact(xx, &e.expr)? },
        Expr_::Match(e) => ArtifactExpr_::Match {
            base: expr_to_artifact(xx, &e.base)?,
            cases: e.cases.iter().map(|c| Ok::<_, String>(ArtifactMatchBranch {
                condition: expr_to_artifact(xx, &c.condition)?,
                body: expr_to_artifact(xx, &c.body)?,
            })).collect::<Result<_, _>>()?,
            element_key: expr_to_artifact(xx, &e.element_key)?,
            fallback: e.fallback.as_ref().map(|x| expr_to_artifact(xx, x)).transpose()?,
        },
        Expr_::OperatorAdd(e) => ArtifactExpr_::OperatorAdd {
            base: expr_to_artifact(xx, &e.base)?,
            reference: expr_to_artifact(xx, &e.reference)?,
        },
        Expr_::OperatorEq(e) => ArtifactExpr_::OperatorEq {
            base: expr_to_artifact(xx, &e.base)?,
            reference: expr_to_artifact(xx, &e.reference)?,
        },
        Expr_::OperatorLt(e) => ArtifactExpr_::OperatorLt {
            base: expr_to_artifact(xx, &e.base)?,
            reference: expr_to_artifact(xx, &e.reference)?,
        },
        Expr_::Record(e) => ArtifactExpr_::Record { entries: e.entries.iter().map(|(k, v)| {
            Ok::<_, String>((expr_to_artifact(xx, k)?, expr_to_artifact(xx, v)?))
        }).collect::<Result<_, _>>()? },
        Expr_::ScopeUse(e) => ArtifactExpr_::ScopeUse { key: expr_to_artifact(xx, &e.key)? },
        Expr_::Set(e) => ArtifactExpr_::Set {
            base: expr_to_artifact(xx, &e.base)?,
            value: expr_to_artifact(xx, &e.value)?,
        },
        Expr_::Stage(e) => ArtifactExpr_::Stage { expr: expr_to_artifact(xx, &e.expr)? },
        Expr_::Tuple(e) => ArtifactExpr_::Tuple {
            elements: e.elements.iter().map(|x| expr_to_artifact(xx, x)).collect::<Result<_, _>>()?,
        },
    };
    Ok(ArtifactExpr {
        loc,
        r#type: Box::new(type_),
    })
}

fn loc_from(loc: ArtifactQExprId) -> QExprId {
    (SourceId(loc.0), ExprId(loc.1))
}

fn loc_to(loc: QExprId) -> ArtifactQExprId {
    ArtifactQExprId(loc.0.0, loc.1.0)
}
