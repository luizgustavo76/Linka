mod core;

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

    Ok(())
}
