package com.aunt13psychot1c.chaoshub.web

/**
 * Some sites turn away anything that looks like an in-app browser (the "; wv" marker), and a
 * few refuse phones entirely. Both versions keep the real Chrome version so sites don't think
 * the browser is out of date.
 */
object UserAgents {
    fun mobile(default: String): String =
        default.replace("; wv)", ")").replace(Regex("""Version/\d+(\.\d+)* """), "")

    fun desktop(default: String): String {
        val chrome = Regex("""Chrome/([\d.]+)""").find(default)?.groupValues?.get(1) ?: "140.0.0.0"
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chrome Safari/537.36"
    }
}
