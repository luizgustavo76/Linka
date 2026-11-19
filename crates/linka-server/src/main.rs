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

    if std::env::args().any(|arg| arg == "--hello-demo") {
        return hello_demo().await;
    }

    // connect to the external services
    let _postgres_pool = pgsql_connect().await?;
    info!("successfully connected to postgresql");

    let _redis_pool = redis_connect().await?;
    info!("successfully connected to redis");

    let _s3 = s3_connect().await?;
    info!("successfully connected to S3 object storage");

    Ok(())
}

async fn hello_demo() -> color_eyre::Result<()> {
    use axum::{Router, routing::get};

    let address = std::env::var("LINKA_DEMO_ADDR")
        .unwrap_or_else(|_| "127.0.0.1:8443".to_owned())
        .parse::<std::net::SocketAddr>()?;
    let app = Router::new().route("/hello", get(|| async { "Hello, world!" }));

    match (
        std::env::var_os("LINKA_DEMO_CERT"),
        std::env::var_os("LINKA_DEMO_KEY"),
    ) {
        (Some(cert), Some(key)) => {
            use rustls::crypto::ring::{cipher_suite, default_provider, kx_group};
            use std::{io::BufReader, sync::Arc};

            let mut provider = default_provider();
            provider.cipher_suites = vec![cipher_suite::TLS13_CHACHA20_POLY1305_SHA256];
            provider.kx_groups = vec![kx_group::X25519];
            let certs = rustls_pemfile::certs(&mut BufReader::new(std::fs::File::open(cert)?))
                .collect::<Result<Vec<_>, _>>()?;
            let key = rustls_pemfile::private_key(&mut BufReader::new(std::fs::File::open(key)?))?
                .ok_or_else(|| color_eyre::eyre::eyre!("demo key file contains no private key"))?;
            let mut config = rustls::ServerConfig::builder_with_provider(Arc::new(provider))
                .with_protocol_versions(&[&rustls::version::TLS13])?
                .with_no_client_auth()
                .with_single_cert(certs, key)?;
            config.alpn_protocols = vec![b"http/1.1".to_vec()];
            let config = axum_server::tls_rustls::RustlsConfig::from_config(Arc::new(config));
            let listener = std::net::TcpListener::bind(address)?;
            info!("hello demo listening on https://{}", listener.local_addr()?);
            axum_server::from_tcp_rustls(listener, config)?
                .serve(app.into_make_service())
                .await?;
        }
        (None, None) => {
            let listener = tokio::net::TcpListener::bind(address).await?;
            info!("hello demo listening on http://{}", listener.local_addr()?);
            axum::serve(listener, app).await?;
        }
        _ => color_eyre::eyre::bail!("set both LINKA_DEMO_CERT and LINKA_DEMO_KEY for HTTPS"),
    }

    Ok(())
}
