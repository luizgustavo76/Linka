# LinkaBot SDK (Python)

An asynchronous, lightweight SDK for building decentralized bots on the **Linka 2.0** network. 

Unlike traditional bot frameworks, the LinkaBot SDK operates as a **minimal Linka client**. It runs entirely **locally**, bypassing the need to open firewall ports or maintain a dedicated public server, while utilizing asynchronous loops to handle multiple chats simultaneously.

---

## ✨ Features

* **Zero-Configuration Deployment:** Runs locally on your machine. No port forwarding or public IP required.
* **Asynchronous Architecture:** Built on top of Python's `asyncio`. Parse messages, handle mentions, and dispatch replies across multiple chats without blocking.
* **Minimal Footprint:** Acts as a slim client focused purely on bot mechanics (parsing text, checking mentions, and executing predefined actions).
* **Federation-Aware Security:** Integrated with Linka's trust system. Supports both Centralized Global IDs (for cross-federation communication) and Local Server IDs (restricted to a single node).

---

## 🔒 Security & Architecture Model

Linka bots must be registered to interact with the ecosystem. This prevents spam while maintaining decentralized freedom:

1. **Global Registration (Central Service):** Assigns a verified Global ID. The bot can federate and communicate across all nodes in the network. If it violates network-wide policies, this ID can be banned globally.
2. **Local Registration (Node-Specific):** The bot is registered only on a specific local server. It can chat freely within that node but **cannot** communicate with other federations.

---

## 🚀 Quick Start

### Installation

Install the SDK locally in editable mode (during development):

```bash
pip install -e .
