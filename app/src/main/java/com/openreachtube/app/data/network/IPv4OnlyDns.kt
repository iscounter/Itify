package com.openreachtube.app.data.network

import java.net.InetAddress
import okhttp3.Dns

/**
 * IPv4-only DNS fallback: some networks return broken AAAA records for media
 * hosts. Swap into OkHttpClient.Builder.dns() behind a user setting.
 */
class IPv4OnlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        Dns.SYSTEM.lookup(hostname).filter { it.address.size == 4 }
            .ifEmpty { Dns.SYSTEM.lookup(hostname) }
}
