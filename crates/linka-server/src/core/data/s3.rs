use color_eyre::eyre::{WrapErr, ensure};
use object_store::aws::{AmazonS3, AmazonS3Builder};
use object_store::list::{PaginatedListOptions, PaginatedListStore};
use std::time::Duration;

use super::read_setting;

pub async fn s3_connect() -> color_eyre::Result<AmazonS3> {
    let startup_timeout = read_setting("S3_STARTUP_TIMEOUT_SECS", 10)?;
    ensure!(
        startup_timeout > 0,
        "S3_STARTUP_TIMEOUT_SECS must be positive"
    );

    let store = AmazonS3Builder::from_env()
        .build()
        .wrap_err("building the S3 client from AWS environment settings")?;

    let probe = store.list_paginated(
        None,
        PaginatedListOptions {
            max_keys: Some(1),
            ..Default::default()
        },
    );
    tokio::time::timeout(Duration::from_secs(startup_timeout.into()), probe)
        .await
        .wrap_err("timed out checking access to the S3 bucket")?
        .wrap_err("checking access to the S3 bucket")?;

    Ok(store)
}
