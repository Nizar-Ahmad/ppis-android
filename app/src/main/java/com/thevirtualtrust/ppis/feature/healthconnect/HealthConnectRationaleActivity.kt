package com.thevirtualtrust.ppis.feature.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevirtualtrust.ppis.ui.theme.PPISTheme

class HealthConnectRationaleActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            PPISTheme {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                24.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        )
                ) {

                    Text(
                        text =
                            "Health Connect",
                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )

                    Text(
                        text =
                            "PPIS uses Health Connect read access " +
                            "to prefill health and activity values " +
                            "such as steps and activity duration. " +
                            "The data is used to reduce manual entry. " +
                            "You remain in control of Health Connect " +
                            "permissions and can revoke them in " +
                            "Android settings at any time."
                    )

                    Button(
                        onClick = {
                            finish()
                        }
                    ) {

                        Text(
                            text = "Close"
                        )
                    }
                }
            }
        }
    }
}
