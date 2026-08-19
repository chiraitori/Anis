# Anis

Anis is a local DNS sinkhole and per-app firewall for Android. It intercepts port 53 DNS queries on-device using Android's `VpnService` and resolves blocked domains to `0.0.0.0`, blocking ads, trackers, and telemetry without routing traffic through remote VPN servers.

## What it does

- **Local DNS Sinkhole**: Intercepts DNS queries on `10.111.222.1:53` and drops blocked domains locally.
- **DNS-over-HTTPS (DoH)**: Forwards allowed queries over encrypted HTTPS to Cloudflare, Quad9, AdGuard, Google, Mullvad, or custom resolvers.
- **App Firewall**: Block internet access for specific applications or bypass the VPN tunnel per app.
- **Trusted Wi-Fi Detection**: Automatically pauses filtering on designated home or work SSIDs and resumes when disconnected.
- **SafeSearch Enforcement**: Redirects Google, Bing, DuckDuckGo, and YouTube queries to restricted mode endpoints at the DNS level.
- **Custom DNS Rewrites**: Maps custom hostnames to local or remote IPs.
- **Configuration Backup**: Exports and imports rules, blocklists, and whitelists as plain JSON.
- **Material 3 UI**: Built with Jetpack Compose, dynamic color support, and Material 3 Expressive motion.

## How it works

```
[ Installed Apps ]
        │ (Port 53 UDP DNS Requests)
        ▼
[ Local VpnService (10.111.222.1) ]
        │
        ├─► [ Trie / Host Engine ] ──► (Blocked Domain) ──► Returns 0.0.0.0
        │
        ├─► [ Custom DNS Rewrites ] ──► Returns Rewrite IP (e.g. SafeSearch)
        │
        └─► [ Upstream Resolver ] ────► DoH / UDP 53 ────► Upstream DNS Server
```

### Core components

- `AnisVpnService`: Creates the local TUN interface and processes raw IPv4 and IPv6 UDP packets on port 53.
- `DnsPacketParser`: Decodes and serializes RFC 1035 wire-format DNS queries and responses.
- `CustomRuleParser`: Matches hosts format (`0.0.0.0 domain.com`), plain domain lists, and Adblock Plus syntax (`||domain.com^`).
- `DohClient`: Sends DNS wire queries over HTTP/2 with connection pooling.
- `AppIconCache`: In-memory LRU cache storing decoded application drawables from `PackageManager`.

## Building

### Requirements
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 35
- JDK 17 or JDK 21

### Build commands

```bash
# Clone the repository
git clone https://github.com/your-username/anis.git
cd anis

# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Default filter lists

Anis includes starter rules and supports updating from external sources:

| Source | Category | Description |
|---|---|---|
| AdGuard DNS Filter | Ads | Ad and popup servers optimized for DNS blocking |
| Steven Black Unified | Ads & Malware | Consolidated hosts file for ads, scam sites, and malware |
| EasyPrivacy | Trackers | Web analytics and user tracking beacons |
| URLhaus | Malware | Active malware payloads and ransomware C2 endpoints |
| Hägezi Multi PRO | Multi-Engine | Low-false-positive tracker and telemetry blocklist |
| OISD Big | Ads | Mobile ad networks and analytics endpoints |

## Backup format

Configurations export to standard JSON:

```json
{
  "version": 2,
  "upstreamDnsId": "cloudflare",
  "dnsProtocol": "DOH",
  "safeSearchEnabled": true,
  "youtubeRestricted": false,
  "pauseOnTrusted": true,
  "trustedSsids": ["Home_5G"],
  "whitelist": ["work.internal.corp"],
  "blacklist": ["telemetry.unwanted.com"],
  "customRules": [
    {
      "id": "rule_1",
      "domain": "internal.nas",
      "targetIp": "192.168.1.150",
      "isEnabled": true
    }
  ]
}
```

## Permissions

| Permission | Purpose |
|---|---|
| `BIND_VPN_SERVICE` | Creates the local TUN interface for port 53 DNS packet capture. |
| `ACCESS_NETWORK_STATE` | Detects network connectivity and Wi-Fi state changes. |
| `ACCESS_WIFI_STATE` | Reads the active SSID for trusted network pausing. |
| `QUERY_ALL_PACKAGES` | Lists installed applications for the per-app firewall. |
| `RECEIVE_BOOT_COMPLETED` | Starts protection on device reboot when enabled by the user. |

## License

MIT
