package dev.chiraitori.anis.vpn.root

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object RootIptablesManager {

    private const val TAG = "RootIptablesManager"
    private const val NAT_CHAIN = "ANIS_DNS"
    private const val FILTER_CHAIN = "ANIS_DOT"
    private const val FIREWALL_CHAIN = "ANIS_FIREWALL"
    const val DEFAULT_DNS_PORT = 5354
    const val DEFAULT_HTTPS_PORT = 8443

    /**
     * Applies iptables rules to transparently intercept all port 53 DNS queries
     * and optionally port 443 HTTPS traffic to local proxy ports.
     */
    fun setupRules(
        context: Context,
        localPort: Int = DEFAULT_DNS_PORT,
        httpsPort: Int = DEFAULT_HTTPS_PORT,
        enableHttpsFiltering: Boolean = false,
        whitelistedUids: Set<Int> = emptySet(),
        blockedUids: Set<Int> = emptySet()
    ): Boolean {
        val appUid = context.applicationInfo.uid
        Log.i(TAG, "Setting up Root iptables redirection to port $localPort / https $httpsPort (appUid=$appUid, https=$enableHttpsFiltering, blockedUids=${blockedUids.size})")

        // Teardown first for idempotency
        teardownRules()

        val commands = mutableListOf<String>()

        // 1. Disable Android Private DNS so queries are sent in plaintext to port 53
        commands.add("settings put global private_dns_mode off")

        // 2. Setup NAT Chain for IPv4
        commands.add("iptables -t nat -N $NAT_CHAIN 2>/dev/null || true")
        // Skip our own app so outbound DNS/DoH/HTTPS doesn't loop
        commands.add("iptables -t nat -A $NAT_CHAIN -m owner --uid-owner $appUid -j RETURN")
        // Skip whitelisted apps
        for (uid in whitelistedUids) {
            commands.add("iptables -t nat -A $NAT_CHAIN -m owner --uid-owner $uid -j RETURN")
        }
        // Redirect UDP and TCP DNS to local port
        commands.add("iptables -t nat -A $NAT_CHAIN -p udp --dport 53 -j REDIRECT --to-ports $localPort")
        commands.add("iptables -t nat -A $NAT_CHAIN -p tcp --dport 53 -j REDIRECT --to-ports $localPort")

        // Redirect HTTP and HTTPS to local MITM proxy if enabled
        if (enableHttpsFiltering) {
            commands.add("iptables -t nat -A $NAT_CHAIN -p tcp --dport 80 -j REDIRECT --to-ports $httpsPort")
            commands.add("iptables -t nat -A $NAT_CHAIN -p tcp --dport 443 -j REDIRECT --to-ports $httpsPort")
        }
        commands.add("iptables -t nat -A OUTPUT -j $NAT_CHAIN")

        // 3. Block DoT (Port 853) to force apps to use standard DNS on port 53
        commands.add("iptables -t filter -N $FILTER_CHAIN 2>/dev/null || true")
        commands.add("iptables -t filter -A $FILTER_CHAIN -m owner --uid-owner $appUid -j RETURN")
        for (uid in whitelistedUids) {
            commands.add("iptables -t filter -A $FILTER_CHAIN -m owner --uid-owner $uid -j RETURN")
        }
        commands.add("iptables -t filter -A $FILTER_CHAIN -p tcp --dport 853 -j REJECT")
        commands.add("iptables -t filter -A OUTPUT -j $FILTER_CHAIN")

        // 4. App Firewall: Strict kernel-level packet rejection for blocked apps
        if (blockedUids.isNotEmpty()) {
            commands.add("iptables -t filter -N $FIREWALL_CHAIN 2>/dev/null || true")
            for (uid in blockedUids) {
                commands.add("iptables -t filter -A $FIREWALL_CHAIN -m owner --uid-owner $uid -j REJECT")
            }
            commands.add("iptables -t filter -A OUTPUT -j $FIREWALL_CHAIN")
        }

        val result = RootUtils.executeCommands(commands)
        if (result.isSuccess) {
            Log.i(TAG, "Root iptables DNS redirect and firewall successfully configured")
        } else {
            Log.e(TAG, "Failed configuring iptables rules: ${result.stderr}")
        }

        return result.isSuccess
    }

    /**
     * Removes all Anis iptables redirection rules and restores Android Private DNS.
     */
    fun teardownRules(): Boolean {
        val commands = listOf(
            "iptables -t nat -D OUTPUT -j $NAT_CHAIN 2>/dev/null || true",
            "iptables -t nat -F $NAT_CHAIN 2>/dev/null || true",
            "iptables -t nat -X $NAT_CHAIN 2>/dev/null || true",
            "iptables -t filter -D OUTPUT -j $FILTER_CHAIN 2>/dev/null || true",
            "iptables -t filter -F $FILTER_CHAIN 2>/dev/null || true",
            "iptables -t filter -X $FILTER_CHAIN 2>/dev/null || true",
            "iptables -t filter -D OUTPUT -j $FIREWALL_CHAIN 2>/dev/null || true",
            "iptables -t filter -F $FIREWALL_CHAIN 2>/dev/null || true",
            "iptables -t filter -X $FIREWALL_CHAIN 2>/dev/null || true",
            "settings put global private_dns_mode opportunistic"
        )

        val result = RootUtils.executeCommands(commands)
        Log.i(TAG, "Root iptables rules torn down, Private DNS set to opportunistic")
        return result.isSuccess
    }

    /**
     * Checks if our iptables rules are active.
     */
    fun isRedirectActive(): Boolean {
        val result = RootUtils.executeCommand("iptables -t nat -L OUTPUT -n 2>/dev/null")
        return result.stdout.any { it.contains(NAT_CHAIN) }
    }

    /**
     * Installs the CA certificate directly to Magisk module or system trust store using root.
     */
    fun installCaCertificateToSystem(certPem: String): Boolean {
        return try {
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(
                ByteArrayInputStream(certPem.toByteArray(Charsets.UTF_8))
            ) as X509Certificate

            // Calculate subject hash for Android cert naming: <hash>.0
            val subjectBytes = cert.subjectX500Principal.encoded
            var hash = 0L
            val md5 = java.security.MessageDigest.getInstance("MD5").digest(subjectBytes)
            for (i in 0 until 4) {
                hash = hash or ((md5[i].toLong() and 0xFF) shl (i * 8))
            }
            val certFileName = String.format("%08x.0", hash)

            val commands = listOf(
                "mkdir -p /data/adb/modules/anis_root_ca/system/etc/security/cacerts",
                "echo 'id=anis_root_ca\nname=Anis Root CA\nversion=1.0\nversionCode=1\nauthor=Anis\ndescription=System-trusted Root CA for Anis HTTPS Guard' > /data/adb/modules/anis_root_ca/module.prop",
                "touch /data/adb/modules/anis_root_ca/auto_mount",
                "cat << 'EOF' > /data/adb/modules/anis_root_ca/system/etc/security/cacerts/$certFileName\n$certPem\nEOF",
                "chmod 644 /data/adb/modules/anis_root_ca/system/etc/security/cacerts/$certFileName"
            )

            val result = RootUtils.executeCommands(commands)
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed installing system CA certificate", e)
            false
        }
    }
}
