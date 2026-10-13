package io.jakobmelchard.pull

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

fun greeting(name: String) = "hello, $name"

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(TextView(this).apply { text = greeting("pull") })
  }
}
