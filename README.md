# Anis

Anis is a local DNS sinkhole, HTTPS filter, and per-app firewall for Android. Its Go/gVisor userspace tunnel processes traffic on-device; upstream sockets bypass the VPN loop and allowed DNS queries can be forwarded over encrypted DNS-over-HTTPS.

## What it does

- **Local DNS Sinkhole**: Intercepts IPv4 and IPv6 DNS through synthetic local endpoints and answers blocked domains locally.
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
        │ (DNS-only or full IPv4 capture for HTTPS filtering)
        ▼
[ Android VpnService TUN ]
        │
        ▼
[ Go + gVisor userspace stack ]
        │
        ├─► [ Host engine ] ─────────► (Blocked Domain) ──► Local block response
        │
        ├─► [ UID/app firewall ] ─────► Blocks DNS for selected applications
        │
        ├─► [ Custom/SafeSearch ] ────► Returns the configured rewrite IP
        │
        ├─► [ HTTPS MITM filter ] ────► Selected browsers; pinned domains bypass
        │
        └─► [ Protected resolver ] ───► DoH / UDP 53 ────► Upstream DNS server
```

### Core components

- `AdBlockVpnService`: Establishes the Android TUN and chooses DNS-only or full IPv4 routing.
- `GoTunnelAdapter`: Connects Anis settings, firewall rules, logs, UID lookup, and CA state to the native engine.
- `tunnel/`: Go DNS resolver, gVisor/tun2socks network stack, firewall callbacks, and HTTPS MITM implementation.
- `CustomRuleParser`: Matches hosts format (`0.0.0.0 domain.com`), plain domain lists, and Adblock Plus syntax (`||domain.com^`).
- `AppIconCache`: In-memory LRU cache storing decoded application drawables from `PackageManager`.

## Building

### Requirements
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 35
- JDK 17 or JDK 21
- Go 1.23+ and `gomobile` only when rebuilding the bundled tunnel AAR

### Build commands

```bash
# Clone the repository
git clone https://github.com/your-username/anis.git
cd anis

# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug

# Build optimized release APKs for each CPU ABI plus a universal APK
./gradlew assembleRelease

# Optional: rebuild app/libs/tunnel.aar after changing tunnel/*.go
./gradlew :app:buildGoTunnel

# Output directories:
# app/build/outputs/apk/debug/
# app/build/outputs/apk/release/
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
  "version": 3,
  "upstreamDnsId": "cloudflare",
  "dnsProtocol": "DOH",
  "protectionMode": "LOCAL_VPN",
  "dnsResponseType": "ZERO_IP",
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
  ],
  "blockLists": [
    {
      "id": "adguard_dns",
      "isEnabled": true,
      "isCustom": false
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
| `ACCESS_FINE_LOCATION` | Allows Android to reveal the active Wi-Fi SSID after the user grants access. |
| `POST_NOTIFICATIONS` | Shows VPN status and optional blocklist update notifications. |
| `QUERY_ALL_PACKAGES` | Lists installed applications for the per-app firewall. |
| `RECEIVE_BOOT_COMPLETED` | Starts protection on device reboot when enabled by the user. |

## License

GNU General Public License v3.0. The Go tunnel is derived from [BlockAds for Android](https://github.com/pass-with-high-score/blockads-android), also licensed under GPL-3.0. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
