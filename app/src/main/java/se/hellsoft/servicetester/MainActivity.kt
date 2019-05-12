package se.hellsoft.servicetester

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

fun log(message: String) = Log.d("ServiceTester", message)

class MainActivity : AppCompatActivity() {
    private var selectedService: Class<out Service> = MyLocalService::class.java
    private var connections = mutableListOf<MyServiceConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("MainActivity.onCreate called")
        setContentView(R.layout.activity_main)
        val spinner = findViewById<Spinner>(R.id.serviceSpinner)
        spinner.setSelection(0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ignore
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedService = when (position) {
                    0 -> MyLocalService::class.java
                    1 -> MyRemoteService::class.java
                    else -> throw IllegalArgumentException("Invalid position")
                }
            }
        }

        findViewById<Button>(R.id.bindBtn).setOnClickListener {
            log("Binding to selected service")
            val connection = MyServiceConnection()
            bindService(Intent(this, selectedService), connection, bindFlags())
            connections.add(connection)
        }
        findViewById<Button>(R.id.unbindBtn).setOnClickListener {
            if (connections.isEmpty()) return@setOnClickListener
            log("Unbinding oldest service connection")
            val connection = connections.removeAt(0)
            unbindService(connection)
        }
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            performStart(selectedService)
        }
        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            log("Stopping selected service")
            stopService(Intent(this, selectedService))
        }
        findViewById<Button>(R.id.killBtn).setOnClickListener {
            connections.firstOrNull()?.binder?.myPid()?.let {
                log("Killing process $it")
                android.os.Process.killProcess(it)
            }
        }
    }

    private fun performStart(
        serviceClass: Class<out Service>,
        startReturn: Int = Service.START_STICKY,
        startForeground: Boolean = true
    ) {
        log("Starting selected service")
        val intent = Intent(this, serviceClass)
        intent.putExtra(START_RETURN_KEY, startReturn)
        intent.putExtra(START_FOREGROUND, startForeground)
        startService(intent)
    }

    private fun bindFlags(): Int {
        val bindAboveClientCheckbox = findViewById<CheckBox>(R.id.bindAboveClientCheckBox).isChecked
        val autoCreateCheckBox = findViewById<CheckBox>(R.id.autoCreateCheckBox).isChecked

        var bindFlags = 0

        bindFlags = bindFlags or if (bindAboveClientCheckbox) {
            Context.BIND_ABOVE_CLIENT
        } else {
            0
        }

        bindFlags = bindFlags or if (autoCreateCheckBox) {
            Context.BIND_AUTO_CREATE
        } else {
            0
        }

        return bindFlags
    }

    companion object {
        const val START_RETURN_KEY = "startReturnValue"
        const val START_FOREGROUND = "startForeground"
    }

    class MyServiceConnection : ServiceConnection {
        var binder: IMyServiceInterface? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log("onServiceConnected called for ${name?.flattenToShortString()}")
            binder = IMyServiceInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            log("onServiceDisconnected called for ${name?.flattenToShortString()}")
            binder = null
        }
    }

}
