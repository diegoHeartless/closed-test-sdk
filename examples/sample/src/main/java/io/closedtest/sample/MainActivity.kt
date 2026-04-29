package io.closedtest.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.closedtest.sdk.ClosedTest

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ClosedTest.handleDeepLink(intent?.data)

        findViewById<Button>(R.id.btnScreen).setOnClickListener {
            ClosedTest.trackScreen("demo_home")
        }
        findViewById<Button>(R.id.btnInteraction).setOnClickListener {
            ClosedTest.trackInteraction("demo_tap")
        }
        findViewById<Button>(R.id.btnEvent).setOnClickListener {
            ClosedTest.trackEvent("demo_step", mapOf("step" to "1"))
        }
        findViewById<Button>(R.id.btnFlush).setOnClickListener {
            ClosedTest.flush()
        }
        findViewById<Button>(R.id.btnDeepLink).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("closedtest://bind?test_session_id=demo_sess&tester_id=demo_tester"),
                ),
            )
        }
        findViewById<Button>(R.id.btnBind).setOnClickListener {
            ClosedTest.bindTester("manual_tester", "manual_session")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ClosedTest.handleDeepLink(intent?.data)
    }
}
