pub mod jscode;
pub mod langhelp;
pub mod syntax;
pub mod builtin;
pub mod eval;

use {
    crate::{
        eval::{
            compile_importid,
            expr::{
                Expr,
                ExprBind,
                ExprBlock,
                ExprCall,
                ExprConditionBranch,
                ExprField,
                ExprForeach,
                ExprIf,
                ExprImport,
                ExprLower,
                ExprMatch,
                ExprMatchBranch,
                ExprOperatorAdd,
                ExprOperatorLt,
                ExprRecord,
                ExprScopeUse,
                ExprSet,
                ExprStage,
                ExprTuple,
                Expr_,
                Module,
            },
            null::ValueNull,
            number::ValueNumber,
            string::ValueString,
        },
        langhelp::{
            CompileRes,
            CompileResResult,
            ExprErr,
            GlobalContext,
            ImportStateId,
            LogEntry,
            SourceId,
        },
        syntax::{
            SourceExpr,
            SourceExpr_,
            SourceLiteral,
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    std::{
        collections::HashMap,
        rc::Rc,
        sync::Arc,
    },
};

pub(crate) fn source_module_to_module(source_id: SourceId, m: &SourceModule) -> Module {
    match m {
        SourceModule::V1(m) => Module::V1(eval::expr::source_expr_to_expr(source_id, m)),
    }
}

pub(crate) fn compile(gx: Arc<GlobalContext>, source: SourceSpecAbsolute) -> Result<Vec<LogEntry>, Vec<ExprErr>> {
    let (source_id, source_module) = gx.sources.get(source).map_err(|e| vec![ExprErr {
        loc: (SourceId(0), crate::syntax::ExprId(0)),
        errors: vec![langhelp::ExprSubErr::Opaque(e)],
    }])?;
    let module = source_module_to_module(source_id, &source_module);
    let res = compile_importid(&gx, ImportStateId(0), module, vec![]);

    fn collect_errors(r: &CompileRes) -> Vec<ExprErr> {
        match &r.result {
            CompileResResult::Err(errs) => errs.clone(),
            CompileResResult::Ok(_) => vec![],
        }
    }

    let mut all_errors = vec![];
    for (_, entry) in gx.compile_results.lock().unwrap().iter() {
        let guard = entry.result.read().unwrap();
        if let Some(Ok(r)) = guard.as_ref() {
            all_errors.extend(collect_errors(r));
            gx.generated_mortar_scripts.lock().unwrap().extend(r.generated_mortar_scripts.iter().cloned());
        }
    }
    all_errors.extend(collect_errors(&res));
    gx.generated_mortar_scripts.lock().unwrap().extend(res.generated_mortar_scripts.iter().cloned());
    if !all_errors.is_empty() {
        return Err(all_errors);
    }
    return Ok(res.logs);
}

fn main() { }
#[cfg(test)]
mod tests;
