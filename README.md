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
The core server handling federation, routing, external network bridges, and persistent data storage.
- **Stack:** Python, Flask, PostgreSQL, Redis

### LinkaLite (Android Client)
The primary client implementation for mobile devices.
- **Target:** Android 2.0+ (Java 1.5 compliant)
- **Design:** Ultra-lightweight native components, optimized HTTP request cycle, low memory footprint, no heavy modern framework overhead.

---

## Technical Setup

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
   python main.py

---

## Project Status

Linka is under active development. Current focus areas include:

- Expanding external platform bridge endpoints (Reddit, Bluesky, Threads, Mastodon)
- Optimizing native Java 1.5 data parsing for low-memory Android devices
- Refining feed pagination (LIMIT / OFFSET) and caching strategies
- Strengthening authentication mechanisms across federated nodes

---

## Repository Links & Documentation

- **Repository:** https://github.com/luizgustavo76/Linka
- **Documentation:** https://luizgustavo76.github.io/Linka/documentation/docs.html

---

## License

This project is licensed under the AGPL-3.0 License. See LICENSE.txt for details.
