mod pgsql;
mod redis;
mod s3;

pub use pgsql::pgsql_connect;
pub use redis::redis_connect;
pub use s3::s3_connect;

use color_eyre::eyre::WrapErr;
use std::env;

fn read_setting(name: &str, default: u32) -> color_eyre::Result<u32> {
    match env::var(name) {
        Ok(value) => value
            .parse()
            .wrap_err_with(|| format!("{name} must be a non-negative integer")),
        Err(env::VarError::NotPresent) => Ok(default),
        Err(error) => Err(error).wrap_err_with(|| format!("reading {name}")),
    }
}
