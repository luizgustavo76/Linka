# Linka

Linka is an open-source decentralized social network and cross-platform feed aggregator. It combines a federated architecture with bidirectional integration for external platforms, allowing users to read and publish content to networks like Reddit, Bluesky, Threads, and Mastodon directly from a single interface.

The project is built with extreme compatibility and efficiency in mind, enabling modern protocol communication while remaining light enough to run on legacy hardware and older mobile operating systems.

---

## Core Characteristics

- **Decentralized & Federated Architecture:** Host your own node or connect to existing instances.
- **Bidirectional Aggregation:** Read feeds and publish content seamlessly to Reddit, Bluesky, Threads, and Mastodon.
- **Legacy Android Focus (LinkaLite):** Native Android client targeting Java 1.5 and Android 2.0+, designed for low RAM overhead and high execution speed on legacy devices.
- **Extensible Backend:** Lightweight API built for rapid response times and minimal server resource consumption.
- **Developer Tools:** Open endpoints and SDK structure for automated bots, custom scripts, and third-party clients.

---

## Ecosystem

### Linka Backend
Source directory: [.old/backend](.old/backend).
The core server handling federation, routing, external network bridges, and persistent data storage.
- **Stack:** Python, Flask, PostgreSQL, Redis

### LinkaLite (Android Client)
The primary client implementation for mobile devices.
- **Project directory:** [apps/linka-lite](apps/linka-lite)
- **Target:** Android 2.0+ (Java 1.5 compliant)
- **Design:** Ultra-lightweight native components, optimized HTTP request cycle, low memory footprint, no heavy modern framework overhead.

---

## Technical Setup

### Rust server development

From the repository root, copy the development settings and start the services.

```sh
cp .env.example .env
docker compose up -d --wait
cargo run -p linka-server
```

Compose pulls the SeaweedFS image and runs it in single-node mode. The S3
endpoint defaults to `http://127.0.0.1:8333`, and SeaweedFS creates the bucket
named by `AWS_BUCKET` at startup. If you already have a local `.env`, update its
S3 settings to match `.env.example` and keep your other local settings.

The server loads `.env` through dotenvy at startup. The template provides
`DATABASE_URL`, `REDIS_URL`, and the `AWS_*` settings used by the S3-compatible
`object_store` client. Use `docker compose down` to stop the services and keep
their data in named volumes.

For other S3-compatible servers, set `AWS_ENDPOINT` to the service URL and
supply its existing bucket, credentials, and signing region. Use
`AWS_ALLOW_HTTP=false` for HTTPS endpoints and enable
`AWS_VIRTUAL_HOSTED_STYLE_REQUEST` if your provider uses bucket subdomains. In that mode, include the bucket name
in the custom endpoint hostname. Temporary credentials can include `AWS_SESSION_TOKEN`.
Startup checks bucket access with a one-item list request, so the credentials need permission to
list the bucket. `S3_STARTUP_TIMEOUT_SECS` bounds that check.

### Prerequisites
- Python 3.x
- PostgreSQL instance

### Backend Installation

1. Clone the repository:
   git clone https://github.com/luizgustavo76/Linka.git
   cd Linka

2. Install dependencies:
   pip install flask python-dotenv werkzeug flask-cors psycopg2-binary redis

3. Configure environment variables in .env:
   DATABASE_URL=postgresql://user:password@localhost:5432/linka_db
   REDIS_URL=redis://localhost:6379/0
   SECRET_KEY=your_secret_key

4. Run the server:
   cd .old/backend
   python main.py

---

## Project Status

Linka is under active development. Current focus areas include:

- Expanding external platform bridge endpoints (Reddit, Bluesky, Threads, Mastodon)
- Optimizing native Java 1.5 data parsing for low-memory Android devices
- Refining feed pagination (LIMIT / OFFSET) and caching strategies
- Strengthening authentication mechanisms across federated nodes

---

## Repository

- **Repository:** https://github.com/luizgustavo76/Linka

---

## License

This project is licensed under the AGPL-3.0 License. See LICENSE.txt for details.
