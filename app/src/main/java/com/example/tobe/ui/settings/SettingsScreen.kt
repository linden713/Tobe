package com.example.tobe.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tobe.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    )

    // Permission Launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setSmsEnabled(isGranted)
        if (!isGranted) {
             Toast.makeText(context, "SMS功能需要权限才能使用", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置 / Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(gradientBrush)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            var phone by remember(userPreferences) { mutableStateOf(userPreferences?.contactPhone ?: "") }

            SettingsCategory("紧急联系人 / Emergency Contact") {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        phone = it
                        viewModel.updateContact("", phone) // Name no longer needed
                    },
                    label = { Text("电话号码") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            SettingsCategory("守护设置 / Monitoring Settings") {
                val isMonitoringEnabled = userPreferences?.isMonitoringEnabled ?: true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "开启安全守护",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "关闭后将停止后台监控通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMonitoringEnabled,
                        onCheckedChange = { viewModel.setMonitoringEnabled(it) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "超时时长 (小时)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val currentTimeout = userPreferences?.timeoutHours ?: 24
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                     listOf(12, 24, 48).forEach { hours ->
                         val isSelected = currentTimeout == hours
                         Button(
                             onClick = { viewModel.updateTimeout(hours) },
                             modifier = Modifier.weight(1f),
                             shape = RoundedCornerShape(12.dp),
                             colors = if(isSelected) ButtonDefaults.buttonColors(
                                 containerColor = MaterialTheme.colorScheme.primary
                             ) else ButtonDefaults.filledTonalButtonColors()
                         ) {
                             Text("${hours}h")
                         }
                     }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsCategory("SMS 求助设置") {
                val isSmsEnabled = userPreferences?.isSmsEnabled ?: false
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用自动发送短信", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isSmsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            } else {
                                viewModel.setSmsEnabled(false)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                var message by remember(userPreferences) { mutableStateOf(userPreferences?.smsMessage ?: "") }
                OutlinedTextField(
                    value = message,
                    onValueChange = { 
                        message = it
                        viewModel.updateSmsMessage(message)
                    },
                    label = { Text("自定义短信内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FilledTonalButton(
                    onClick = { 
                        if (isSmsEnabled) {
                            try {
                                val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                                    context.getSystemService(android.telephony.SmsManager::class.java)
                                } else {
                                    android.telephony.SmsManager.getDefault()
                                }
                                smsManager.sendTextMessage(phone, null, message, null, null)
                                Toast.makeText(context, "短信已发送", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "请先启用SMS功能", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("发送测试短信")
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "本应用旨在提供一份额外的安全保障。请确保应用在后台不被系统杀死，并授予必要权限。应用不请求网络连接不会泄露个人信息。本应用为开源应用，目前尚不能保证在所有机型上都可运行。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
