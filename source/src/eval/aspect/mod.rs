mod aspect_artifact;
mod aspect_bind;
mod aspect_bool;
mod aspect_consume;
mod aspect_field;
mod aspect_function;
mod aspect_iterable;
mod aspect_lift;
mod aspect_list;
mod aspect_map;
mod aspect_null;
mod aspect_number;
mod aspect_operator_add;
mod aspect_operator_lt;
mod aspect_record;
mod aspect_set;
mod aspect_stack_value;
mod aspect_string;
mod aspect_tagged_union;
mod aspect_tuple;
mod aspect_type;

pub(crate) use {
    aspect_artifact::*,
    aspect_bind::*,
    aspect_bind::dynamic::*,
    aspect_bool::r#const::*,
    aspect_bool::dynamic::*,
    aspect_consume::dynamic::*,
    aspect_field::*,
    aspect_function::*,
    aspect_iterable::*,
    aspect_lift::dynamic::*,
    aspect_list::dynamic::*,
    aspect_map::dynamic::*,
    aspect_null::*,
    aspect_number::r#const::*,
    aspect_number::dynamic::*,
    aspect_operator_add::*,
    aspect_operator_lt::*,
    aspect_record::*,
    aspect_set::*,
    aspect_stack_value::dynamic::*,
    aspect_string::r#const::*,
    aspect_string::dynamic::*,
    aspect_tagged_union::*,
    aspect_tuple::*,
    aspect_type::*,
};
