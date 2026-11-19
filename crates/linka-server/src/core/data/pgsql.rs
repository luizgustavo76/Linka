use super::read_setting;
use color_eyre::eyre::{WrapErr, ensure};
use sqlx::{PgPool, postgres::PgPoolOptions};
use std::{env, time::Duration};

pub async fn pgsql_connect() -> color_eyre::Result<PgPool> {
    let database_url = env::var("DATABASE_URL").wrap_err("reading DATABASE_URL")?;
    let min_connections = read_setting("POSTGRES_POOL_MIN_CONNECTIONS", 4)?;
    let max_connections = read_setting("POSTGRES_POOL_MAX_CONNECTIONS", 16)?;
    let acquire_timeout = read_setting("POSTGRES_POOL_ACQUIRE_TIMEOUT_SECS", 10)?;
    let idle_timeout = read_setting("POSTGRES_POOL_IDLE_TIMEOUT_SECS", 600)?;
    let max_lifetime = read_setting("POSTGRES_POOL_MAX_LIFETIME_SECS", 1800)?;

    ensure!(
        max_connections > 0 && min_connections <= max_connections,
        "POSTGRES_POOL_MAX_CONNECTIONS must be positive and at least POSTGRES_POOL_MIN_CONNECTIONS"
    );
    ensure!(
        acquire_timeout > 0,
        "POSTGRES_POOL_ACQUIRE_TIMEOUT_SECS must be positive"
    );

    PgPoolOptions::new()
        .min_connections(min_connections)
        .max_connections(max_connections)
        .acquire_timeout(Duration::from_secs(acquire_timeout.into()))
        .idle_timeout((idle_timeout > 0).then(|| Duration::from_secs(idle_timeout.into())))
        .max_lifetime((max_lifetime > 0).then(|| Duration::from_secs(max_lifetime.into())))
        .connect(&database_url)
        .await
        .wrap_err("connecting to PostgreSQL")
}
