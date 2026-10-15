package com.trademind.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 15, 27)
    private val card = Color.rgb(22, 30, 47)
    private val card2 = Color.rgb(29, 40, 60)
    private val white = Color.rgb(241, 245, 251)
    private val muted = Color.rgb(153, 169, 192)
    private val green = Color.rgb(49, 211, 159)
    private val red = Color.rgb(255, 104, 119)
    private val amber = Color.rgb(255, 190, 85)
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var page: LinearLayout
    private var screenName = "Home"
    private var liveStatus: TextView? = null
    private var btcQuote: TextView? = null
    private var fxQuote: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showScreen("Home")
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun showScreen(screen: String) {
        screenName = screen
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 20, 18, 24) }
        scroll.addView(page)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { setPadding(2, 6, 2, 6); setBackgroundColor(card); gravity = Gravity.CENTER }
        listOf("Home", "Chart", "Indicators", "Strategies", "AI Lab").forEach { name ->
            nav.addView(Button(this).apply {
                text = name; textSize = 9f; isAllCaps = false
                setTextColor(if (screen == name) green else white)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { showScreen(name) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        outer.addView(nav)
        setContentView(outer)
        liveStatus = null; btcQuote = null; fxQuote = null
        when (screen) {
            "Home" -> home()
            "Chart" -> chart()
            "Indicators" -> indicators()
            "Strategies" -> strategies()
            else -> aiLab()
        }
    }

    private fun home() {
        addText("TradeMind AI", 29, white, true)
        addText("MULTI-MARKET TRADING WORKSPACE", 10, muted, true)
        gap(14)
        card {
            addText("Live data connections", 17, white, true)
            addText("Public endpoints • no API key required for these two demo feeds", 11, muted, false)
            gap(7)
            btcQuote = textView("BTC/USDT: loading…", 14, white, true); addView(btcQuote)
            gap(4)
            fxQuote = textView("EUR/USD reference rate: loading…", 14, white, true); addView(fxQuote)
            gap(8)
            liveStatus = textView("Tap refresh to request current public quotes.", 11, muted, false); addView(liveStatus)
            addView(makeButton("Refresh live quotes") { refreshPublicQuotes() })
        }
        gap(12)
        card {
            addText("Market watch", 18, white, true)
            addText("Non-live illustrative entries are labelled below.", 11, muted, false)
            gap(5)
            market("BTC/USDT", "Loading public quote", "CRYPTO", green)
            divider()
            market("EUR/USD", "Loading reference rate", "FX REFERENCE", green)
            divider()
            market("XAU/USD", "Illustrative only", "GOLD", amber)
            divider()
            market("NAS100", "Illustrative only", "INDEX", amber)
            divider()
            market("WTI OIL", "Illustrative only", "COMMODITY", amber)
        }
        gap(12)
        card {
            addText("Risk / reward calculator", 17, white, true)
            val e = input("Entry price"); val s = input("Stop-loss price"); val t = input("Take-profit price")
            addView(e); addView(s); addView(t)
            val result = textView("Enter prices to calculate risk/reward.", 13, white, false)
            addView(makeButton("Calculate") {
                val entry = e.text.toString().toDoubleOrNull()
                val stop = s.text.toString().toDoubleOrNull()
                val target = t.text.toString().toDoubleOrNull()
                result.text = if (entry != null && stop != null && target != null && entry != stop) {
                    val risk = kotlin.math.abs(entry - stop); val reward = kotlin.math.abs(target - entry)
                    "Price risk ${fmt(risk)} • Potential reward ${fmt(reward)} • R:R ${fmt(reward / risk)}:1"
                } else "Enter valid numbers; entry and stop must differ."
            })
            addView(result)
        }
        gap(12)
        card {
            addText("Daily checklist", 17, white, true)
            bullet("Confirm higher-timeframe context")
            bullet("Wait for a clear setup and confirmation")
            bullet("Define invalidation and position risk first")
            bullet("Check scheduled news and trading costs")
        }
        disclaimer()
        refreshPublicQuotes()
    }

    private fun refreshPublicQuotes() {
        liveStatus?.text = "Fetching public quotes…"
        executor.execute {
            var btc: String? = null
            var fx: String? = null
            var errors = mutableListOf<String>()
            try {
                val json = JSONObject(httpGet("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                btc = json.getString("price").toDoubleOrNull()?.let { String.format(Locale.US, "%,.2f", it) }
            } catch (_: Exception) { errors.add("Crypto feed unavailable") }
            try {
                val json = JSONObject(httpGet("https://api.frankfurter.app/latest?from=EUR&to=USD"))
                fx = json.getJSONObject("rates").getDouble("USD").let { String.format(Locale.US, "%.5f", it) }
            } catch (_: Exception) { errors.add("FX reference feed unavailable") }
            runOnUiThread {
                btcQuote?.text = "BTC/USDT public quote: ${btc ?: "unavailable"}"
                fxQuote?.text = "EUR/USD reference rate: ${fx ?: "unavailable"}"
                val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                liveStatus?.text = if (errors.isEmpty()) "Updated $stamp • Crypto ticker + FX reference rate" else "$stamp • ${errors.joinToString("; ")}"
            }
        }
    }

    private fun httpGet(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/json")
        return try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun chart() {
        addText("Chart workspace", 27, white, true)
        addText("Illustrative layout — real OHLC candle history is not connected yet.", 12, muted, false)
        gap(12)
        card {
            addText("BTC/USDT • 4H", 18, white, true)
            addText("Visual preview only — not a live chart", 12, amber, true)
            gap(12)
            val plot = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
                setPadding(8, 14, 8, 14); minimumHeight = 170; setBackgroundColor(Color.rgb(14, 21, 35))
            }
            val heights = listOf(60,75,54,95,84,110,90,125,102,78,94,68,82,57,70,45)
            heights.forEachIndexed { i, h ->
                val bar = View(this@MainActivity).apply { setBackgroundColor(if (i % 3 == 0) green else red) }
                val lp = LinearLayout.LayoutParams(0, h, 1f); lp.setMargins(2,0,2,0); plot.addView(bar, lp)
            }
            addView(plot)
            addText("Planned: zoomable candlesticks, drawing tools, price alerts and multi-timeframe history.", 12, muted, false)
        }
        gap(12)
        card {
            addText("Technical snapshot", 17, white, true)
            addText("No indicator values are calculated until real OHLC candles are connected.", 13, muted, false)
            gap(8)
            bullet("Trend: awaiting candle data")
            bullet("Momentum: awaiting RSI / MACD calculation")
            bullet("Volatility: awaiting ATR calculation")
        }
        disclaimer()
    }

    private fun indicators() {
        addText("Indicators library", 27, white, true)
        addText("Educational descriptions — not live signals", 12, muted, false)
        val entries = listOf(
            "RSI" to "Momentum oscillator. Traders often watch 30/70 zones, but strong trends can remain extended.",
            "MACD" to "Trend/momentum tool. Crossovers and histogram shifts can be noisy in sideways markets.",
            "EMA / SMA" to "Moving averages can help describe trend and dynamic areas; crossovers lag price.",
            "Bollinger Bands" to "Volatility bands expand and contract. A band touch alone does not guarantee reversal.",
            "ATR" to "Measures recent volatility, not direction; sometimes used to scale stops.",
            "VWAP" to "Compares price with a session volume-weighted average; depends on available volume data.",
            "Fibonacci" to "Retracement levels are possible reaction zones, not guaranteed support/resistance.",
            "Volume" to "Can help contextualize moves; spot FX volume is often tick volume rather than consolidated volume."
        )
        entries.forEach { (title, desc) ->
            gap(10); card { addText(title, 16, white, true); addText(desc, 13, muted, false) }
        }
        disclaimer()
    }

    private fun strategies() {
        addText("Strategy playbook", 27, white, true)
        addText("Educational templates. Test with realistic costs before use.", 12, muted, false)
        strategy("Trend pullback", "Higher-timeframe trend + pullback toward moving-average area + rejection/confirmation; invalidate beyond the swing.")
        strategy("Breakout and retest", "Mark a clear range, wait for a close outside it, then watch whether a retest holds. Beware false breakouts.")
        strategy("Range mean reversion", "Use only in stable ranges; look for rejection near edges and plan exits before entry. Avoid strong trends.")
        strategy("Momentum continuation", "Wait for consolidation in a directional market and confirmation of expansion; avoid chasing late moves.")
        disclaimer()
    }

    private fun aiLab() {
        addText("AI trading lab", 27, white, true)
        addText("AI is not connected yet: it needs a secure provider/backend.", 12, amber, true)
        gap(12)
        card {
            addText("Chart screenshot analysis", 17, white, true)
            addText("The app currently has no image-upload-to-AI service. To connect this safely, a backend must receive the image and call a vision-capable AI provider.", 13, muted, false)
        }
        gap(12)
        card {
            addText("Automated market scanning", 17, white, true)
            addText("Planned pipeline: licensed market candles → indicator calculations → setup rules → AI explanation → risk checks → user alert. No trades are placed.", 13, muted, false)
        }
        gap(12)
        card {
            addText("AI analysis preview", 17, white, true)
            val prompt = input("Ask about a setup (demo only)")
            addView(prompt)
            val output = textView("No AI request is sent from this screen yet.", 13, muted, false)
            addView(makeButton("Show connection requirements") {
                output.text = "To enable real responses, provide/deploy a backend URL that accepts authenticated requests and forwards them to your AI provider. Keep API keys on the server, never in the APK."
            })
            addView(output)
        }
        disclaimer()
    }

    private fun strategy(title: String, body: String) {
        gap(10); card { addText(title, 17, white, true); addText(body, 13, muted, false) }
    }

    private fun disclaimer() {
        gap(14)
        addText("RISK NOTICE", 11, amber, true)
        addText("Prototype for education only. Public quotes may be delayed, restricted or unavailable. FX rates are reference rates, not broker prices. This app is not financial advice and does not execute trades.", 11, muted, false)
    }

    private fun card(content: LinearLayout.() -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(15,15,15,15); setBackgroundColor(card) }
        box.content()
        page.addView(box, LinearLayout.LayoutParams(-1,-2))
    }
    private fun addText(value: String, size: Int, color: Int, bold: Boolean) { page.addView(textView(value,size,color,bold)) }
    private fun textView(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(2f, 1.05f)
    }
    private fun gap(h: Int) { page.addView(Space(this).apply { layoutParams = LinearLayout.LayoutParams(1,h) }) }
    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun market(symbol: String, price: String, tag: String, color: Int) {
        val row = horizontal().apply { setPadding(0,9,0,9) }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(textView(symbol,15,white,true)); addView(textView(tag,9,muted,true)) }
        row.addView(labels, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(textView(price,13,color,true))
        page.addView(row)
    }
    private fun divider() { page.addView(View(this).apply { setBackgroundColor(card2) }, LinearLayout.LayoutParams(-1,1)) }
    private fun bullet(value: String) {
        val row = horizontal().apply { gravity = Gravity.TOP }
        row.addView(textView("•",16,green,true))
        val label = textView(value,13,white,false); label.setPadding(7,2,0,7)
        row.addView(label, LinearLayout.LayoutParams(0,-2,1f)); page.addView(row)
    }
    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText; setHintTextColor(muted); setTextColor(white); textSize = 14f; setSingleLine(true)
        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        backgroundTintList = android.content.res.ColorStateList.valueOf(muted)
    }
    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setTextColor(white); setBackgroundColor(card2); setOnClickListener { action() }
    }
    private fun fmt(value: Double) = String.format(Locale.US, "%.3f", value)
}
