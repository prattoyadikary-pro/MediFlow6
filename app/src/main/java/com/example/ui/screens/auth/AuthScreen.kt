package com.example.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.ShowcaseTealPrimary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Auto Google Sign-In cancelled or dismissed: ${e.message}", e)
            onUnauthenticated()
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    FirebaseAuth.getInstance().signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on welcome screen for interactive sign in */ },
            scope = scope
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_shift"
    )

    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF061820),
                Color(0xFF0A2732),
                Color(0xFF05151C)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFCDE9F0),
                Color(0xFFE7F6F7),
                Color(0xFFD6EEF0),
                Color(0xFF88C2CA)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Serene mountain and lake water reflection canvas matching Showcase Screen 1
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Distant soft mountain range
            val farMountainPath = Path().apply {
                moveTo(0f, h * 0.63f)
                cubicTo(w * 0.22f, h * 0.55f, w * 0.48f, h * 0.65f, w * 0.76f, h * 0.56f)
                cubicTo(w * 0.88f, h * 0.52f, w * 0.95f, h * 0.57f, w, h * 0.54f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = farMountainPath,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0x440E4B56), Color(0x77072B33))
                    } else {
                        listOf(Color(0x6698C7D0), Color(0x996BA6B0))
                    }
                )
            )

            // Midground misty ridge
            val midMountainPath = Path().apply {
                moveTo(0f, h * 0.70f)
                cubicTo(w * 0.30f, h * 0.64f, w * 0.62f, h * 0.73f, w, h * 0.65f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = midMountainPath,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0x55125B67), Color(0x8806232B))
                    } else {
                        listOf(Color(0x887EB4BD), Color(0xBB4B8D96))
                    }
                )
            )

            // Calm lake water surface with gentle wave
            val wave1Path = Path().apply {
                val yOffset = (waveShift - 0.5f) * 14f
                moveTo(0f, h * 0.79f + yOffset)
                cubicTo(
                    w * 0.35f, h * 0.74f - yOffset,
                    w * 0.65f, h * 0.84f + yOffset,
                    w, h * 0.77f - yOffset
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = wave1Path,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0x77167985), Color(0xCC041B22))
                    } else {
                        listOf(Color(0xBB6CB2BA), Color(0xEE1F6E75))
                    }
                )
            )

            // Specular glossy foreground water ripple
            val wave2Path = Path().apply {
                val yOffset = (0.5f - waveShift) * 10f
                moveTo(0f, h * 0.86f + yOffset)
                cubicTo(
                    w * 0.3f, h * 0.81f + yOffset,
                    w * 0.7f, h * 0.90f - yOffset,
                    w, h * 0.84f + yOffset
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = wave2Path,
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0x5527B9C0), Color(0xEE031217))
                    } else {
                        listOf(Color(0x99B6E8EA), Color(0xEE0D5860))
                    }
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Centerpiece Logo & Branding matching Showcase Screen 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MediFlowDropletLeafLogo(size = 118.dp)

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "MediFlow",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0D3B44),
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Take your health, one step at a time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFBFEBEF) else Color(0xFF426770),
                    textAlign = TextAlign.Center
                )
            }

            // Sleek Compact Liquid Glass Gmail Sign-In Pill Bar over the lake reflection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0x33075B63),
                        spotColor = Color(0x33087F82)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (isDark) Color(0xD90A222B) else Color(0xD9F8FCFC)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0x5527B9C0) else Color(0xCCFFFFFF),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShowcaseTealPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ShowcaseTealPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Register or sign in with Gmail to sync your personal profile",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF9AC2CC) else Color(0xFF56767F),
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = Color(0xFFDF6666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 3D Glossy Water Droplet with inner organic leaf matching Showcase Screen 1.
 */
@Composable
fun MediFlowDropletLeafLogo(
    size: Dp = 104.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer water droplet path
        val dropPath = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(
                w * 0.86f, h * 0.45f,
                w * 0.90f, h * 0.76f,
                w * 0.5f, h * 0.94f
            )
            cubicTo(
                w * 0.10f, h * 0.76f,
                w * 0.14f, h * 0.45f,
                w * 0.5f, h * 0.06f
            )
            close()
        }

        drawPath(
            path = dropPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7CE8E6),
                    Color(0xFF1B9AAA),
                    Color(0xFF005B66)
                ),
                start = Offset(w * 0.25f, h * 0.1f),
                end = Offset(w * 0.8f, h * 0.9f)
            )
        )

        // Inner organic leaf curve (right petal)
        val leafPath = Path().apply {
            moveTo(w * 0.5f, h * 0.92f)
            cubicTo(
                w * 0.48f, h * 0.62f,
                w * 0.68f, h * 0.44f,
                w * 0.76f, h * 0.52f
            )
            cubicTo(
                w * 0.82f, h * 0.68f,
                w * 0.68f, h * 0.88f,
                w * 0.5f, h * 0.92f
            )
            close()
        }

        drawPath(
            path = leafPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE0FBF8),
                    Color(0xFF56C5B6),
                    Color(0xFF0F767E)
                ),
                start = Offset(w * 0.5f, h * 0.45f),
                end = Offset(w * 0.75f, h * 0.9f)
            )
        )

        // Left specular highlight
        val highlightPath = Path().apply {
            moveTo(w * 0.45f, h * 0.18f)
            cubicTo(
                w * 0.26f, h * 0.46f,
                w * 0.24f, h * 0.68f,
                w * 0.36f, h * 0.80f
            )
            cubicTo(
                w * 0.29f, h * 0.64f,
                w * 0.33f, h * 0.44f,
                w * 0.45f, h * 0.18f
            )
            close()
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}
