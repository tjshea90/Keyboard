package app.swoop.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * The hand-over of what ANOTHER app learned (the Kestrel browser's own small keyboard, owner request 17): the other app starts this screen with an EXPLICIT intent (ACTION_SEND, a file it grants read access to in
 * EXTRA_STREAM). It has no intent filter, so it never shows up in a share sheet. It only ever shows what the file holds and asks; nothing is added without a tap on Import, the file must be a learning export
 * (magic header, size limit, the same checks as a file picked in Settings) and the file is never kept. docs/KESTREL-LINK.md.
 */
class ReceiveLearnedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri: Uri? = if (intent?.action != Intent.ACTION_SEND) null else if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (uri == null) { finish(); return }
        val flow = KeyboardFlow(this, Engine.host(this), object : KeyboardFlow.Host {
            override fun say(message: String) { Toast.makeText(this@ReceiveLearnedActivity, message, Toast.LENGTH_LONG).show() }
            override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) {
                AlertDialog.Builder(this@ReceiveLearnedActivity).setTitle(title).setMessage(message)
                    .setPositiveButton(yes) { _, _ -> result(true) }.setNegativeButton(no) { _, _ -> result(false) }.setOnCancelListener { result(false) }.show()
            }
        })
        flow.importUri(uri) { if (!isFinishing) finish() }
    }
}
