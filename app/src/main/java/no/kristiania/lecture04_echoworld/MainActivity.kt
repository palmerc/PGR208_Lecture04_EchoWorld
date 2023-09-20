package no.kristiania.lecture04_echoworld

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener


interface EchoWebSocketListenerDelegate {
    fun messageReceived(msg: String)
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EchoInterface()
        }
    }
}

private class EchoWebSocketListener : WebSocketListener() {
    val TAG = "EchoWebSocketListener"
    var delegate: EchoWebSocketListenerDelegate? = null

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WSL onOpen")

        webSocket.send("You have entered the chat.")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "WSL onMessage text: ${text}")

        delegate?.messageReceived(text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WSL onClosing")

        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Log.d(TAG, "Closing : $code / $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WSL onClosed")
    }

    companion object {
        private val NORMAL_CLOSURE_STATUS = 1000
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "WSL onFailure")
        Log.e(TAG, "${t.message}")
    }
}

class MessageViewModel : ViewModel(), EchoWebSocketListenerDelegate {
    val TAG = "MessageViewModel"
    val messageStrings = mutableStateListOf<String>()
    private var listener: EchoWebSocketListener? = null
    private var ws: WebSocket? = null

    init {
        startWebSocket()
    }

    private fun startWebSocket() {
        // TODO Change the IP Address
        val ws_url = "ws://172.26.119.103:8765"
        val wsRequest: Request = Request.Builder().url(ws_url).build()
        listener = EchoWebSocketListener()
        listener?.delegate = this
        listener?.also {
            ws = OkHttpClient().newWebSocket(wsRequest, it)
        }
    }

    private fun stopWebSocket() {
        ws?.close(code = 1000, reason = "Client is exiting.")
        ws = null
    }

    override fun messageReceived(message: String) {
        Log.d(TAG, "messageReceived: ${message}")

        viewModelScope.launch {
            messageStrings.add(message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun  EchoInterface(viewModel: MessageViewModel = viewModel()) { // This is the connection UI to WS
    val TAG = "EchoInterface"

    Box(modifier = Modifier.fillMaxSize()) {
        // https://www.jetpackcompose.net/state-in-jetpack-compose
        var mutableText by remember { mutableStateOf(TextFieldValue("Testing 1-2-3")) }
        val value = WindowInsets.displayCutout
        // https://www.jetpackcompose.net/compose-layout-row-and-column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .displayCutoutPadding()
        ) {
            // https://www.jetpackcompose.net/textfield-in-jetpack-compose
            OutlinedTextField(value = mutableText, modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red),
                onValueChange = { newText ->
                    mutableText = newText
                })
            MessageList(messages = viewModel.messageStrings)
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                // https://www.jetpackcompose.net/buttons-in-jetpack-compose
                Button(modifier = Modifier
                    .fillMaxWidth(fraction = 0.8F)
                    .background(Color.Yellow),
                    onClick = {

                    }) {
                    Text("Send it!", fontSize = 30.sp)
                }
            }
        }
    }
}

@Composable
fun MessageList(messages: SnapshotStateList<String>) {
    val TAG = "MessageList"

    LazyColumn(modifier = Modifier
        .fillMaxWidth()
        .background(Color.Yellow)) {
        items(messages) { message ->
            Log.d(TAG, message)
            Text(message, modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue), fontSize = 30.sp)
        }
    }
}


@Preview
@Composable
fun EchoInterfacePreview() {
    EchoInterface()
}
