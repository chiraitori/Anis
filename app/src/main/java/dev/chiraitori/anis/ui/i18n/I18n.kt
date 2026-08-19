package dev.chiraitori.anis.ui.i18n

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import dev.chiraitori.anis.data.model.AppLanguage
import org.json.JSONObject
import java.util.Locale

/**
 * Lightweight instant reactive I18n engine for Anis.
 * Translates the entire app in real-time when the user switches language.
 * Seamlessly integrates with Crowdin JSON translation files from assets/locales/.
 */
object I18n {

    private val jsonCache = mutableMapOf<String, Map<String, String>>()

    fun applyLocale(context: Context, language: AppLanguage) {
        try {
            val langCode = language.languageCode.ifBlank { Locale.getDefault().language }
            loadLocaleJson(context, langCode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (language.languageCode.isBlank()) {
                    localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
                } else {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(language.languageCode)
                }
            } else {
                val locale = if (language.languageCode.isBlank()) Locale.getDefault() else Locale.forLanguageTag(language.languageCode)
                Locale.setDefault(locale)
                val config = context.resources.configuration
                config.setLocale(locale)
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }
        } catch (_: Exception) {}
    }

    /**
     * Resolves the translation key for the current active language.
     * Looks up in the Crowdin JSON cache first, then falls back to built-in dictionary.
     */
    fun get(key: String, language: AppLanguage): String {
        val langCode = language.languageCode.ifBlank {
            Locale.getDefault().language
        }
        val cached = jsonCache[langCode]
        if (cached != null && cached.containsKey(key)) {
            return cached[key]!!
        }
        val dict = translations[langCode] ?: translations["en"] ?: emptyMap()
        return dict[key] ?: translations["en"]?.get(key) ?: key
    }

    fun loadLocaleJson(context: Context, langCode: String): Map<String, String> {
        if (jsonCache.containsKey(langCode)) return jsonCache[langCode]!!
        return try {
            val fileName = "locales/$langCode.json"
            val jsonStr = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val flattened = mutableMapOf<String, String>()
            flattenJsonObject("", root, flattened)
            jsonCache[langCode] = flattened
            flattened
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun flattenJsonObject(prefix: String, obj: JSONObject, out: MutableMap<String, String>) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            if (value is JSONObject) {
                flattenJsonObject(fullKey, value, out)
            } else if (value is String) {
                out[fullKey] = value
                out[key] = value
            }
        }
    }

    private val translations: Map<String, Map<String, String>> = mapOf(
        // ── English (Default) ──────────────────────────────────────────
        "en" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "System-Wide DNS Adblocker & Firewall",
            "nav_shield" to "Shield",
            "nav_lists" to "Lists",
            "nav_firewall" to "Firewall",
            "nav_logs" to "Logs",
            "nav_settings" to "Settings",
            "shield_active" to "DNS Protection Active",
            "shield_paused" to "Protection Paused",
            "shield_paused_trusted" to "Paused on Trusted Wi-Fi",
            "tap_to_start" to "Tap shield to activate local DNS adblocker",
            "tap_to_stop" to "Tap shield to pause protection",
            "stats_total" to "Total Queries",
            "stats_blocked" to "Blocked Ads",
            "stats_firewall" to "Firewall Blocks",
            "stats_block_rate" to "Block Rate",
            "profiles_title" to "Protection Profiles",
            "breakdown_title" to "Protection Breakdown",
            "blocklists_title" to "Adblock Filter Lists",
            "blocklists_desc" to "Manage community lists to block ads, trackers, and malicious domains",
            "add_custom_list" to "Add Custom List",
            "update_all" to "Update All",
            "firewall_title" to "App Firewall",
            "firewall_desc" to "Restrict background data and internet access per app locally",
            "logs_title" to "Live Query Logs",
            "logs_desc" to "Real-time DNS query inspector and domain block metrics",
            "search_hint" to "Search domains or apps...",
            "filter_all" to "All",
            "filter_blocked" to "Blocked Only",
            "filter_allowed" to "Allowed",
            "filter_user_apps" to "User Apps",
            "filter_system_apps" to "System",
            "settings_title" to "Settings & Engine",
            "settings_desc" to "Fine-tune DNS routing, root proxy, filter schedules, and privacy rules",
            "sec_engine" to "Protection & DNS Engine",
            "sec_engine_desc" to "Configure packet interception, upstream resolvers, and safe browsing",
            "sec_apps" to "Applications & Routing",
            "sec_apps_desc" to "Manage bypassed applications and trusted home/office Wi-Fi networks",
            "sec_filters" to "Filters & Rule Management",
            "sec_filters_desc" to "Configure automated blocklist updates, domain allowlists, and DNS rewrites",
            "sec_appearance" to "Appearance & System",
            "sec_appearance_desc" to "Customize color themes, tactile haptic response, and boot actions",
            "sec_data" to "Data, Backup & Logs",
            "sec_data_desc" to "Export/import settings configurations, manage log retention, and reset stats",
            "sec_about" to "About & Credits",
            "sec_about_desc" to "Author credits, open source repository, and engine diagnostics",
            "interception_mode" to "Interception Architecture",
            "local_vpn" to "Local VPN",
            "root_iptables" to "Root iptables",
            "upstream_dns" to "Upstream DNS Resolver",
            "dns_transport" to "Resolver Transport Protocol",
            "doh_encrypted" to "DoH (Encrypted)",
            "plain_udp" to "Plain UDP",
            "response_type" to "DNS Response on Block",
            "safesearch" to "Strict SafeSearch Enforcement",
            "safesearch_desc" to "Force family filtering on Google, Bing, DuckDuckGo, and Pixiv",
            "youtube_restricted" to "YouTube Restricted Mode",
            "youtube_restricted_desc" to "Restrict potentially mature videos across all YouTube apps and browsers",
            "auto_reconnect" to "Auto-Reconnect & Roaming Recovery",
            "magisk_ca" to "Install Magisk Root System CA",
            "bypassed_apps" to "Bypassed Applications",
            "trusted_wifi" to "Trusted Wi-Fi SSIDs",
            "update_freq" to "Blocklist Update Frequency",
            "wifi_only" to "Update Over Wi-Fi Only",
            "update_notifications" to "Rule Update Notifications",
            "whitelist_domains" to "Domain Whitelist (Always Allow)",
            "blacklist_domains" to "Domain Blacklist (Always Block)",
            "dns_rewrites" to "Custom DNS Rewrites & Host Mappings",
            "app_theme" to "Application Theme",
            "app_language" to "Application Language",
            "haptics_feedback" to "Expressive Haptic Feedback",
            "start_on_boot" to "Start Protection on Boot",
            "export_backup" to "Export Settings & Rules Backup",
            "import_backup" to "Import & Restore Configuration",
            "clear_logs" to "Clear All Logs & Analytics",
            "created_by" to "Created by chiraitori",
            "root_status" to "Root Status",
            "engine_state" to "Engine State"
        ),

        // ── Indonesian (Bahasa Indonesia) ──────────────────────────────
        "id" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Pemblokir Iklan DNS & Firewall Seluruh Sistem",
            "nav_shield" to "Proteksi",
            "nav_lists" to "Daftar",
            "nav_firewall" to "Firewall",
            "nav_logs" to "Log",
            "nav_settings" to "Pengaturan",
            "shield_active" to "Perlindungan DNS Aktif",
            "shield_paused" to "Perlindungan Dijeda",
            "shield_paused_trusted" to "Dijeda di Wi-Fi Tepercaya",
            "tap_to_start" to "Ketuk perisai untuk mengaktifkan adblocker",
            "tap_to_stop" to "Ketuk perisai untuk menjeda perlindungan",
            "stats_total" to "Total Permintaan",
            "stats_blocked" to "Iklan Diblokir",
            "stats_firewall" to "Blokir Firewall",
            "stats_block_rate" to "Tingkat Blokir",
            "profiles_title" to "Profil Perlindungan",
            "breakdown_title" to "Rincian Perlindungan",
            "blocklists_title" to "Daftar Filter Pemblokir Iklan",
            "blocklists_desc" to "Kelola daftar komunitas untuk memblokir iklan, pelacak, dan domain berbahaya",
            "add_custom_list" to "Tambah Daftar Kustom",
            "update_all" to "Perbarui Semua",
            "firewall_title" to "Firewall Aplikasi",
            "firewall_desc" to "Batasi data latar belakang dan akses internet per aplikasi secara lokal",
            "logs_title" to "Log Permintaan Langsung",
            "logs_desc" to "Inspektur kueri DNS waktu nyata dan statistik pemblokiran domain",
            "search_hint" to "Cari domain atau aplikasi...",
            "filter_all" to "Semua",
            "filter_blocked" to "Hanya Diblokir",
            "filter_allowed" to "Diizinkan",
            "filter_user_apps" to "Aplikasi Pengguna",
            "filter_system_apps" to "Sistem",
            "settings_title" to "Pengaturan & Mesin",
            "settings_desc" to "Atur perutean DNS, proksi root, jadwal pembaruan, dan privasi",
            "sec_engine" to "Mesin Proteksi & DNS",
            "sec_engine_desc" to "Konfigurasi intersepsi paket, resolver hulu, dan penjelajahan aman",
            "sec_apps" to "Aplikasi & Perutean",
            "sec_apps_desc" to "Kelola aplikasi yang dilewati dan jaringan Wi-Fi tepercaya",
            "sec_filters" to "Filter & Manajemen Aturan",
            "sec_filters_desc" to "Konfigurasi pembaruan otomatis, daftar izin domain, dan penulisan ulang DNS",
            "sec_appearance" to "Tampilan & Sistem",
            "sec_appearance_desc" to "Sesuaikan tema warna, umpan balik haptic, dan tindakan saat boot",
            "sec_data" to "Data, Cadangan & Log",
            "sec_data_desc" to "Ekspor/impor konfigurasi pengaturan, retensi log, dan reset statistik",
            "sec_about" to "Tentang & Kredit",
            "sec_about_desc" to "Kredit pembuat, repositori sumber terbuka, dan diagnostik mesin",
            "interception_mode" to "Arsitektur Intersepsi",
            "local_vpn" to "VPN Lokal",
            "root_iptables" to "Root iptables",
            "upstream_dns" to "Resolver DNS Hulu",
            "dns_transport" to "Protokol Transport Resolver",
            "doh_encrypted" to "DoH (Terenkripsi)",
            "plain_udp" to "UDP Biasa",
            "response_type" to "Respons DNS saat Diblokir",
            "safesearch" to "Penegakan SafeSearch Ketat",
            "safesearch_desc" to "Paksa filter keluarga di Google, Bing, DuckDuckGo, dan Pixiv",
            "youtube_restricted" to "Mode Terbatas YouTube",
            "youtube_restricted_desc" to "Batasi video dewasa di semua aplikasi YouTube dan browser",
            "auto_reconnect" to "Sambung Ulang Otomatis & Pemulihan Roaming",
            "magisk_ca" to "Pasang CA Sistem Root Magisk",
            "bypassed_apps" to "Aplikasi yang Dilewati",
            "trusted_wifi" to "SSID Wi-Fi Tepercaya",
            "update_freq" to "Frekuensi Pembaruan Filter",
            "wifi_only" to "Perbarui Hanya Lewat Wi-Fi",
            "update_notifications" to "Notifikasi Pembaruan Aturan",
            "whitelist_domains" to "Daftar Putih Domain (Selalu Izinkan)",
            "blacklist_domains" to "Daftar Hitam Domain (Selalu Blokir)",
            "dns_rewrites" to "Pengalihan DNS Kustom & Pemetaan Host",
            "app_theme" to "Tema Aplikasi",
            "app_language" to "Bahasa Aplikasi",
            "haptics_feedback" to "Umpan Balik Haptik Ekspresif",
            "start_on_boot" to "Mulai Proteksi saat Boot",
            "export_backup" to "Ekspor Cadangan Pengaturan & Aturan",
            "import_backup" to "Impor & Pulihkan Konfigurasi",
            "clear_logs" to "Hapus Semua Log & Statistik",
            "created_by" to "Dibuat oleh chiraitori",
            "root_status" to "Status Root",
            "engine_state" to "Status Mesin"
        ),

        // ── Japanese (日本語) ───────────────────────────────────────────
        "ja" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "システム全体 DNS 広告ブロッカー＆ファイアウォール",
            "nav_shield" to "保護",
            "nav_lists" to "リスト",
            "nav_firewall" to "ファイアウォール",
            "nav_logs" to "ログ",
            "nav_settings" to "設定",
            "shield_active" to "DNS保護が有効です",
            "shield_paused" to "保護が一時停止中",
            "shield_paused_trusted" to "信頼できるWi-Fiで一時停止中",
            "tap_to_start" to "シールドをタップして保護を開始",
            "tap_to_stop" to "シールドをタップして保護を停止",
            "stats_total" to "総クエリ数",
            "stats_blocked" to "ブロックした広告",
            "stats_firewall" to "ファイアウォール遮断",
            "stats_block_rate" to "ブロック率",
            "profiles_title" to "保護プロファイル",
            "breakdown_title" to "保護の内訳",
            "blocklists_title" to "広告ブロックフィルターリスト",
            "blocklists_desc" to "広告、トラッカー、悪意あるドメインをブロックするコミュニティリスト",
            "add_custom_list" to "カスタムリスト追加",
            "update_all" to "すべて更新",
            "firewall_title" to "アプリファイアウォール",
            "firewall_desc" to "アプリごとのバックグラウンドデータ通信とインターネット接続を制限",
            "logs_title" to "リアルタイムクエリログ",
            "logs_desc" to "リアルタイムDNSクエリインスペクターおよびブロック分析",
            "search_hint" to "ドメインやアプリを検索...",
            "filter_all" to "すべて",
            "filter_blocked" to "ブロックのみ",
            "filter_allowed" to "許可",
            "settings_title" to "設定とエンジン",
            "settings_desc" to "DNSルーティング、Rootプロキシ、フィル更新、プライバシーを調整",
            "sec_engine" to "保護とDNSエンジン",
            "sec_apps" to "アプリとルーティング",
            "sec_filters" to "フィルターとルール管理",
            "sec_appearance" to "外観とシステム",
            "sec_data" to "データとバックアップ",
            "sec_about" to "情報とクレジット",
            "interception_mode" to "インターセプト方式",
            "local_vpn" to "ローカルVPN",
            "root_iptables" to "Root iptables",
            "upstream_dns" to "アップストリームDNS",
            "doh_encrypted" to "DoH (暗号化)",
            "plain_udp" to "通常UDP",
            "safesearch" to "厳格なセーフサーチ",
            "youtube_restricted" to "YouTube制限付きモード",
            "app_theme" to "アプリのテーマ",
            "app_language" to "アプリの言語",
            "haptics_feedback" to "触覚フィードバック (Haptics)",
            "created_by" to "開発者: chiraitori",
            "root_status" to "Root権限",
            "engine_state" to "エンジン状態"
        ),

        // ── Vietnamese (Tiếng Việt) ────────────────────────────────────
        "vi" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Trình chặn quảng cáo DNS & Tường lửa toàn hệ thống",
            "nav_shield" to "Bảo vệ",
            "nav_lists" to "Danh sách",
            "nav_firewall" to "Tường lửa",
            "nav_logs" to "Nhật ký",
            "nav_settings" to "Cài đặt",
            "shield_active" to "Bảo vệ DNS Đang Hoạt Động",
            "shield_paused" to "Đã Tạm Dừng Bảo Vệ",
            "tap_to_start" to "Chạm vào khiên để kích hoạt chặn quảng cáo",
            "tap_to_stop" to "Chạm vào khiên để tạm dừng",
            "stats_total" to "Tổng truy vấn",
            "stats_blocked" to "Quảng cáo đã chặn",
            "stats_firewall" to "Chặn tường lửa",
            "stats_block_rate" to "Tỷ lệ chặn",
            "blocklists_title" to "Danh Sách Lọc Quảng Cáo",
            "firewall_title" to "Tường Lửa Ứng Dụng",
            "logs_title" to "Nhật Ký Truy Vấn Trực Tiếp",
            "settings_title" to "Cài Đặt & Cấu Hình",
            "sec_engine" to "Động cơ Bảo Vệ & DNS",
            "sec_apps" to "Ứng Dụng & Định Tuyến",
            "sec_filters" to "Bộ Lọc & Quy Tắc",
            "sec_appearance" to "Giao Diện & Hệ Thống",
            "sec_data" to "Dữ Liệu & Sao Lưu",
            "sec_about" to "Giới Thiệu & Tác Giả",
            "app_language" to "Ngôn Ngữ Ứng Dụng",
            "created_by" to "Được tạo bởi chiraitori"
        ),

        // ── Spanish (Español) ──────────────────────────────────────────
        "es" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Bloqueador de Anuncios DNS y Firewall del Sistema",
            "nav_shield" to "Escudo",
            "nav_lists" to "Listas",
            "nav_firewall" to "Cortafuegos",
            "nav_logs" to "Registros",
            "nav_settings" to "Ajustes",
            "shield_active" to "Protección DNS Activa",
            "shield_paused" to "Protección Pausada",
            "tap_to_start" to "Toca el escudo para activar el bloqueador",
            "tap_to_stop" to "Toca el escudo para pausar",
            "stats_total" to "Consultas Totales",
            "stats_blocked" to "Anuncios Bloqueados",
            "stats_firewall" to "Bloqueos Firewall",
            "stats_block_rate" to "Tasa de Bloqueo",
            "blocklists_title" to "Listas de Filtros de Anuncios",
            "firewall_title" to "Cortafuegos de Aplicaciones",
            "logs_title" to "Registros de Consultas en Vivo",
            "settings_title" to "Ajustes y Motor",
            "sec_engine" to "Motor de Protección y DNS",
            "sec_apps" to "Aplicaciones y Enrutamiento",
            "sec_filters" to "Filtros y Gestión de Reglas",
            "sec_appearance" to "Apariencia y Sistema",
            "sec_data" to "Datos y Copias de Seguridad",
            "sec_about" to "Acerca de y Créditos",
            "app_language" to "Idioma de la Aplicación",
            "created_by" to "Creado por chiraitori"
        ),

        // ── German (Deutsch) ───────────────────────────────────────────
        "de" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Systemweiter DNS-Werbeblocker & Firewall",
            "nav_shield" to "Schutz",
            "nav_lists" to "Listen",
            "nav_firewall" to "Firewall",
            "nav_logs" to "Protokolle",
            "nav_settings" to "Optionen",
            "shield_active" to "DNS-Schutz Aktiv",
            "shield_paused" to "Schutz Pausiert",
            "tap_to_start" to "Tippen, um DNS-Schutz zu aktivieren",
            "tap_to_stop" to "Tippen, um Schutz zu pausieren",
            "stats_total" to "Gesamtanfragen",
            "stats_blocked" to "Blockierte Werbung",
            "stats_firewall" to "Firewall-Blöcke",
            "stats_block_rate" to "Blockierungsrate",
            "blocklists_title" to "Werbeblocker-Filterlisten",
            "firewall_title" to "App-Firewall",
            "logs_title" to "Echtzeit-Abfrageprotokolle",
            "settings_title" to "Einstellungen & Engine",
            "sec_engine" to "Schutz & DNS-Engine",
            "sec_apps" to "Apps & Routing",
            "sec_filters" to "Filter & Regelverwaltung",
            "sec_appearance" to "Darstellung & System",
            "sec_data" to "Daten & Sicherung",
            "sec_about" to "Über & Credits",
            "app_language" to "App-Sprache",
            "created_by" to "Erstellt von chiraitori"
        ),

        // ── French (Français) ──────────────────────────────────────────
        "fr" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Bloqueur de Pubs DNS & Pare-feu Système",
            "nav_shield" to "Bouclier",
            "nav_lists" to "Listes",
            "nav_firewall" to "Pare-feu",
            "nav_logs" to "Journaux",
            "nav_settings" to "Paramètres",
            "shield_active" to "Protection DNS Active",
            "shield_paused" to "Protection en Pause",
            "tap_to_start" to "Appuyez pour activer le bloqueur DNS",
            "tap_to_stop" to "Appuyez pour suspendre la protection",
            "stats_total" to "Requêtes Totales",
            "stats_blocked" to "Publicités Bloquées",
            "stats_firewall" to "Blocages Pare-feu",
            "stats_block_rate" to "Taux de Blocage",
            "blocklists_title" to "Listes de Filtrage des Publicités",
            "firewall_title" to "Pare-feu des Applications",
            "logs_title" to "Journaux des Requêtes en Direct",
            "settings_title" to "Paramètres & Moteur",
            "sec_engine" to "Moteur de Protection & DNS",
            "sec_apps" to "Applications & Routage",
            "sec_filters" to "Filtres & Gestion des Règles",
            "sec_appearance" to "Apparence & Système",
            "sec_data" to "Données & Sauvegarde",
            "sec_about" to "À Propos & Crédits",
            "app_language" to "Langue de l'Application",
            "created_by" to "Créé par chiraitori"
        ),

        // ── Russian (Русский) ──────────────────────────────────────────
        "ru" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "Системный DNS блокировщик рекламы и фаервол",
            "nav_shield" to "Защита",
            "nav_lists" to "Списки",
            "nav_firewall" to "Фаервол",
            "nav_logs" to "Журнал",
            "nav_settings" to "Настройки",
            "shield_active" to "DNS Защита Активна",
            "shield_paused" to "Защита Приостановлена",
            "tap_to_start" to "Нажмите для запуска блокировщика",
            "tap_to_stop" to "Нажмите для приостановки",
            "stats_total" to "Всего запросов",
            "stats_blocked" to "Заблокировано рекламы",
            "stats_firewall" to "Блокировок фаервола",
            "stats_block_rate" to "Процент блокировки",
            "blocklists_title" to "Списки фильтрации рекламы",
            "firewall_title" to "Фаервол приложений",
            "logs_title" to "Журнал DNS запросов",
            "settings_title" to "Настройки и Движок",
            "sec_engine" to "Защита и DNS Движок",
            "sec_apps" to "Приложения и Маршрутизация",
            "sec_filters" to "Фильтры и Управление правилами",
            "sec_appearance" to "Внешний вид и Система",
            "sec_data" to "Данные и Резервные копии",
            "sec_about" to "О программе и Авторы",
            "app_language" to "Язык Приложения",
            "created_by" to "Создано chiraitori"
        ),

        // ── Chinese (中文) ──────────────────────────────────────────────
        "zh" to mapOf(
            "app_name" to "Anis",
            "app_subtitle" to "全系统 DNS 广告拦截器与防火墙",
            "nav_shield" to "防护",
            "nav_lists" to "规则列表",
            "nav_firewall" to "防火墙",
            "nav_logs" to "实时日志",
            "nav_settings" to "设置",
            "shield_active" to "DNS 拦截防护已开启",
            "shield_paused" to "防护已暂停",
            "tap_to_start" to "点击盾牌以激活 DNS 广告拦截",
            "tap_to_stop" to "点击盾牌以暂停防护",
            "stats_total" to "总查询数",
            "stats_blocked" to "已拦截广告",
            "stats_firewall" to "防火墙拦截",
            "stats_block_rate" to "拦截率",
            "blocklists_title" to "广告过滤规则列表",
            "firewall_title" to "应用防火墙",
            "logs_title" to "实时 DNS 查询日志",
            "settings_title" to "设置与引擎",
            "sec_engine" to "防护与 DNS 引擎",
            "sec_apps" to "应用与路由分流",
            "sec_filters" to "过滤列表与自定义规则",
            "sec_appearance" to "外观与系统设置",
            "sec_data" to "数据、备份与日志",
            "sec_about" to "关于与作者鸣谢",
            "app_language" to "应用语言",
            "created_by" to "由 chiraitori 开发"
        )
    )
}
