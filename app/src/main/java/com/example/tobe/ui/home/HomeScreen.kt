package com.example.tobe.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tobe.ui.MainViewModel
import com.example.tobe.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val quote = viewModel.currentQuote
    val uriHandler = LocalUriHandler.current
    var showExplanation by remember { mutableStateOf(false) }
    
    // Check-in state (Resets on app open/recomposition as requested, effectively "per session")
    var hasCheckedIn by remember { mutableStateOf(false) }

    // Infinite transition for breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundTheme")
    val breatheOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseGray)
    ) {
        // --- Dynamic Atmosphere Glows ---
        
        // Top-Left Green Glow
        Box(
            modifier = Modifier
                .offset(x = (-50 + breatheOffset).dp, y = (-50 - breatheOffset).dp)
                .size(500.dp)
                .graphicsLayer(alpha = 0.4f)
                .blur(80.dp)
                .background(SoftGreenGlow, CircleShape)
                .align(Alignment.TopStart)
        )

        // Bottom-Right Blue Glow
        Box(
            modifier = Modifier
                .offset(x = (50 - breatheOffset).dp, y = (50 + breatheOffset).dp)
                .size(600.dp)
                .graphicsLayer(alpha = 0.4f)
                .blur(80.dp)
                .background(SoftBlueGlow, CircleShape)
                .align(Alignment.BottomEnd)
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Remove ripple for a cleaner look or keep it? User might prefer a clean look for a title.
                            ) {
                                uriHandler.openUri("https://github.com/linden713/Tobe")
                            }
                        ) {
                            Text(
                                "活着呢",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExplanation = !showExplanation }) {
                            Icon(Icons.Default.Info, contentDescription = "How it works", tint = IconGray)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = IconGray)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // --- Core Floating Glass Card ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 30.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color.Black.copy(alpha = 0.03f),
                            ambientColor = Color.Transparent
                        )
                        .clip(RoundedCornerShape(32.dp)),
                    color = GlassWhite, // Glass effect with transparency
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 56.dp, horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val lastActive = userPreferences?.lastActiveTime ?: System.currentTimeMillis()
                        val timeAgo = DateUtils.getRelativeTimeSpanString(lastActive)

                        val isMonitoring = userPreferences?.isMonitoringEnabled ?: true
                        Text(
                            text = if (isMonitoring) "监测中" else "未监测",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = if (isMonitoring) DeepCyan else MutedGray,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "上次活动: $timeAgo",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MutedGray,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))

                // --- Quote Section ---
                AnimatedContent(
                    targetState = quote,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(1500)) togetherWith fadeOut(animationSpec = tween(1500))
                    },
                    label = "QuoteAnimation"
                ) { targetQuote ->
                    val annotatedQuote = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = ModernBlue.copy(alpha = 0.3f))) {
                            append("“")
                        }
                        append(targetQuote.text)
                        withStyle(style = SpanStyle(color = ModernBlue.copy(alpha = 0.3f))) {
                            append("”")
                        }
                    }

                    Text(
                        text = annotatedQuote,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 34.sp,
                            color = DarkGray,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(88.dp))

                // --- Check-in Button ---
                Button(
                    onClick = { 
                        viewModel.checkIn()
                        hasCheckedIn = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        .shadow(
                            elevation = if (hasCheckedIn) 0.dp else 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = ModernBlueShadow,
                            ambientColor = Color.Transparent
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasCheckedIn) EmeraldSuccess.copy(alpha = 0.8f) else ModernBlue,
                        disabledContainerColor = EmeraldSuccess.copy(alpha = 0.8f)
                    ),
                    enabled = !hasCheckedIn
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = hasCheckedIn,
                            transitionSpec = {
                                scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                            },
                            label = "ButtonContent"
                        ) { checked ->
                            if (checked) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "不用打卡啦，祝你开心",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = "签到",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // --- Principle Info ---
                AnimatedVisibility(
                    visible = showExplanation,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        color = GlassWhite.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = "工作原理",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrincipleItemCompact("自动感知", "打卡或手机解锁自动刷新平安状态。")
                            PrincipleItemCompact("守护超时", "预设时间内无活动将视为失联。")
                            PrincipleItemCompact("紧急介入", "自动向紧急联系人发送求援短信。")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrincipleItemCompact(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = DarkGray,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray
        )
    }
}
