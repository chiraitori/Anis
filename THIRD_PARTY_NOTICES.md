# Third-party notices

## BlockAds for Android

The Go tunnel engine in `tunnel/`, its Android binding in `app/libs/tunnel.aar`,
and related integration logic are adapted from BlockAds for Android:

https://github.com/pass-with-high-score/blockads-android

BlockAds for Android is licensed under the GNU General Public License v3.0.
Anis includes and modifies that work under the same GPL-3.0 terms. The complete
license text is in `LICENSE`.

Anis modifications include Android settings integration, application/firewall
callbacks, blocklist integration, UI state wiring, build integration, and local
tunnel lifecycle handling.

## PixelPlayerOSS

The expressive onboarding interaction and visual composition are inspired by
PixelPlayerOSS, including its icon-collage presentation, animated page flow,
and morphing setup action surface:

https://github.com/PixelPlayerHQ/PixelPlayerOSS

PixelPlayerOSS is licensed under the GNU General Public License v3.0. Anis uses
an independently adapted implementation for its DNS, VPN, certificate, and
notification setup flow under the same GPL-3.0 terms.
