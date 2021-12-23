package com.example.composeazurecalling.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.example.composeazurecalling.R
import com.example.composeazurecalling.model.JoinCallConfig
import com.example.composeazurecalling.ui.fragment.CallScreenFragment
import com.example.composeazurecalling.viewmodel.CallScreenViewModel

class CallScreenActivity : AppCompatActivity() {
    private val groupCallVM by viewModels<CallScreenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_screen_activity)
        val joinCallConfig = intent.getSerializableExtra("joinCallConfig") as JoinCallConfig

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CallScreenFragment.newInstance(joinCallConfig))
                .commitNow()
        }

        groupCallVM.configureChanged.observe(this, {
            Log.d("debug", "configureChanged : $it")
        })
    }
}