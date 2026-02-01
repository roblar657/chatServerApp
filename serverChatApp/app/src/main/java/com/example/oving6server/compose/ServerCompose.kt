import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.oving6server.Server
import com.example.oving6server.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerCompose() {

    val context = LocalContext.current

    var serverStarted by remember { mutableStateOf(false) }
    val logMessages = remember { mutableStateListOf<String>() }
    var server: Server? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Server logg") }) },
        containerColor = MaterialTheme.colorScheme.background,
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logMessages) { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                Button(
                    onClick = {
                        if (!serverStarted) {
                            server = Server(
                                context = context,
                                messageCallback = { msg -> logMessages.add(msg) }
                            )
                            server?.start()
                            serverStarted = true
                        } else {
                            server?.stop()
                            serverStarted = false
                            logMessages.add(context.getString(R.string.msg_server_stopped))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF555555),
                        contentColor = Color.White
                    ),
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (!serverStarted) "START" else "STOPP")
                }

                Spacer(modifier = Modifier.height(8.dp))


                Button(
                    onClick = { logMessages.clear() },
                    enabled = logMessages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF777777),
                        contentColor = Color.White
                    ),
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("TØM LOGG")
                }
            }
        }
    )
}
