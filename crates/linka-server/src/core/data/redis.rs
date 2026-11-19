use super::read_setting;
use color_eyre::eyre::{WrapErr, ensure};
use deadpool::Runtime;
use deadpool::managed::{Manager, Metrics, Pool, RecycleResult};
use redis::aio::{ConnectionManager, ConnectionManagerConfig};
use redis::{Client, RedisError};
use std::{env, time::Duration};

pub type RedisPool = Pool<RedisManager>;

pub struct RedisManager {
    client: Client,
    config: ConnectionManagerConfig,
}

impl Manager for RedisManager {
    type Type = ConnectionManager;
    type Error = RedisError;

    async fn create(&self) -> Result<Self::Type, Self::Error> {
        ConnectionManager::new_with_config(self.client.clone(), self.config.clone()).await
    }

    async fn recycle(
        &self,
        connection: &mut Self::Type,
        _: &Metrics,
    ) -> RecycleResult<Self::Error> {
        redis::cmd("PING").query_async::<()>(connection).await?;
        Ok(())
    }
}

pub async fn redis_connect() -> color_eyre::Result<RedisPool> {
    let url = env::var("REDIS_URL").wrap_err("reading REDIS_URL")?;
    let max_size = read_setting("REDIS_POOL_MAX_SIZE", 8)? as usize;
    ensure!(max_size > 0, "REDIS_POOL_MAX_SIZE must be positive");

    let wait_timeout = read_timeout("REDIS_POOL_WAIT_TIMEOUT_SECS", 3)?;
    let create_timeout = read_timeout("REDIS_POOL_CREATE_TIMEOUT_SECS", 10)?;
    let connect_timeout = read_timeout("REDIS_CONNECT_TIMEOUT_SECS", 2)?;
    let response_timeout = read_timeout("REDIS_RESPONSE_TIMEOUT_SECS", 2)?;
    let retries = read_setting("REDIS_CONNECT_RETRIES", 3)? as usize;

    let manager = RedisManager {
        client: Client::open(url).wrap_err("invalid REDIS_URL")?,
        config: ConnectionManagerConfig::new()
            .set_connection_timeout(Some(connect_timeout))
            .set_response_timeout(Some(response_timeout))
            .set_number_of_retries(retries),
    };

    let pool = Pool::builder(manager)
        .runtime(Runtime::Tokio1)
        .max_size(max_size)
        .wait_timeout(Some(wait_timeout))
        .create_timeout(Some(create_timeout))
        .recycle_timeout(Some(response_timeout))
        .build()
        .wrap_err("building the Redis pool")?;

    drop(pool.get().await.wrap_err("connecting to Redis")?);
    Ok(pool)
}

fn read_timeout(name: &str, default: u32) -> color_eyre::Result<Duration> {
    let seconds = read_setting(name, default)?;
    ensure!(seconds > 0, "{name} must be positive");
    Ok(Duration::from_secs(seconds.into()))
}
