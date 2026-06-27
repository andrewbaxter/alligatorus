pub mod buildsrc;

use {
    quote::quote,
    std::{
        collections::HashSet,
        env,
        fs,
        path::PathBuf,
    },
};

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    let mut builtin_unique_ids = HashSet::new();
    let mut scope_ids = HashSet::new();
    let mut out = quote!{
        use std::rc::Rc;
        use std::cell::{
            Cell,
            RefCell
        };
        use std::collections::HashMap;
        use dumpster::unsync::Gc;
        use crate::langhelp::{
            ConstKey,
            DefSetId,
            ExprContext,
            Scope,
            BindingId,
            Unique,
        };
        use crate::eval::DynamicEvalContextSlot;
        use crate::eval::DynamicLogStore;
        use crate::langhelp::LogEntry;
        use crate::eval::RcBindingWrap;
        use crate::eval::const_eval;
        use crate::eval::definition_set::DefinitionSet;
        use crate::eval::expr::Expr;
        use crate::eval::value::type_source_function::{
            SourceFunctionData,
            r#const::TypeSourceFunctionConst
        };
        use crate::eval::value::type_source_union::{
            SourceUnionData,
            SourceUnionRef,
            SourceUnionVariant,
        };
        use crate::eval::value::type_source_union_new::SourceUnionNewRef;
        use crate::eval::value::type_source_object::{
            SourceDataMethodData,
            SourceObjectData,
            SourceObjectInterfaceImplData,
            SourceObjectInterfaceMethodImplData,
            SourceObjectRef,
        };
        use crate::eval::value::type_source_object_new::TypeSourceObjectNewConst;
        use crate::eval::value::type_source_interface::{
            SourceInterfaceData,
            SourceInterfaceMethodData,
            SourceInterfaceRef,
            r#const::TypeSourceInterfaceConst,
        };
        use crate::eval::push::{
            AnyType,
            StagedExpr
        };
        use crate::eval::dynamic_util::throw_error;
    };
    buildsrc::aspects::build(&mut out, &mut builtin_unique_ids, &mut scope_ids);
    let mut extra_iface_specs: Vec<buildsrc::util::InterfaceSpec> = Vec::new();
    let mut tree_out = quote!{};
    buildsrc::builtin_tree::emit_gen_builtin_tree(&mut tree_out, &mut scope_ids, &mut extra_iface_specs);
    let mut interface_specs = buildsrc::aspects::interface_specs();
    interface_specs.extend(buildsrc::traits::specs());
    interface_specs.extend(extra_iface_specs);
    buildsrc::interfaces::build(&mut out, &mut builtin_unique_ids, &mut scope_ids, interface_specs);
    buildsrc::identities::build(&mut out, &mut builtin_unique_ids, &mut scope_ids);
    buildsrc::push_structs::build(&mut out, &mut builtin_unique_ids, buildsrc::traits::specs());
    out.extend(tree_out);
    fs::write(PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("gen.rs"), out.to_string()).unwrap();
}
