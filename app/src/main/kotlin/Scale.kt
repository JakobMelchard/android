package io.jakobmelchard.pull

import kotlinx.coroutines.delay
import kotlin.random.Random
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val COMPANY = 0x0100

data class Reading(val kg: Float, val stable: Boolean, val t: Long)

// WH-C06 (IF_B7) manufacturer data, company id already stripped by Android: hundredths of
// the display unit as big-endian u16 at 10..11, byte 14 is stable (high nibble) and unit (low).
fun decode(d: ByteArray, t: Long): Reading? {
  if (d.size < 12) return null
  val v = (((d[10].toInt() and 0xff) shl 8) or (d[11].toInt() and 0xff)) / 100f
  val b = if (d.size > 14) d[14].toInt() and 0xff else 0x01
  val kg = when (b and 0x0f) {
    0, 2 -> v * 0.45359237f // lb
    3 -> v * 6.35029318f // st
    4 -> v * 0.5f // jin
    else -> v
  }
  return if (kg > 300f) null else Reading(kg, b shr 4 != 0, t)
}

class Scale(private val ctx: Context) {
  val trace = mutableStateListOf<Reading>()
  var status by mutableStateOf("Not scanning")
  var scanning by mutableStateOf(false)
  var tare by mutableStateOf(0f)
  private var addr: String? = null

  private val cb = object : ScanCallback() {
    override fun onScanResult(type: Int, r: ScanResult) {
      val d = r.scanRecord?.getManufacturerSpecificData(COMPANY) ?: return
      if (addr != null && r.device.address != addr) return
      val x = decode(d, SystemClock.elapsedRealtime()) ?: return
      // Lock onto the first device that sends a valid frame so a second scale nearby cannot mix in.
      addr = r.device.address
      trace.add(x.copy(kg = x.kg - tare))
      if (trace.size > 20_000) trace.removeRange(0, 5_000)
    }

    override fun onScanFailed(code: Int) {
      scanning = false
      status = "Scan failed ($code)"
    }
  }

  @SuppressLint("MissingPermission")
  fun start() {
    if (scanning) return
    val s = ctx.getSystemService(BluetoothManager::class.java).adapter?.bluetoothLeScanner
    if (s == null) {
      status = "Bluetooth is off"
      return
    }
    val f = ScanFilter.Builder().setManufacturerData(COMPANY, byteArrayOf()).build()
    val set = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    addr = null
    s.startScan(listOf(f), set, cb)
    scanning = true
    status = "Searching"
  }

  @SuppressLint("MissingPermission")
  fun stop() {
    if (!scanning) return
    ctx.getSystemService(BluetoothManager::class.java).adapter?.bluetoothLeScanner?.stopScan(cb)
    scanning = false
    addr = null
    status = "Not scanning"
  }

  // Synthetic 7 s pulls with 3 s rests, about 7 readings a second like the real scale.
  suspend fun demo() {
    scanning = true
    val t0 = SystemClock.elapsedRealtime()
    while (true) {
      val t = SystemClock.elapsedRealtime()
      val c = (t - t0) % 10_000 / 1000f
      val kg = if (c < 7f) 23f * minOf(1f, c / 0.6f) - c * 0.3f + Random.nextFloat() - 0.5f else 0.3f
      trace.add(Reading(kg.coerceAtLeast(0f) - tare, true, t))
      if (trace.size > 20_000) trace.removeRange(0, 5_000)
      delay(140)
    }
  }

  fun zero() {
    trace.lastOrNull()?.let { tare += it.kg }
  }
}
