mod core;

use crate::core::data::{pgsql_connect, redis_connect, s3_connect};
use crate::core::logging;
use tracing::info;

#[tokio::main]
async fn main() -> color_eyre::Result<()> {
    color_eyre::install()?;
    dotenvy::dotenv().ok();
    logging::init_logger();
    info!(
        "Linka Server v{} will start soon...",
        env!("CARGO_PKG_VERSION")
    );

    // connect to the external services
    let _postgres_pool = pgsql_connect().await?;
    info!("successfully connected to postgresql");

    let _redis_pool = redis_connect().await?;
    info!("successfully connected to redis");

    let _s3 = s3_connect().await?;
    info!("successfully connected to S3 object storage");

    Ok(())
}
