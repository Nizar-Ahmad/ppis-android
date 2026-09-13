package com.thevirtualtrust.ppis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thevirtualtrust.ppis.navigation.PPISRoot
import com.thevirtualtrust.ppis.ui.theme.PPISTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {
            PPISTheme {
                PPISRoot()
            }
        }
    }
}
