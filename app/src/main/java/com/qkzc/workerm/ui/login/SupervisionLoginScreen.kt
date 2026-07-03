package com.qkzc.workerm.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qkzc.workerm.ui.theme.LoginBackgroundBottom
import com.qkzc.workerm.ui.theme.LoginBackgroundMiddle
import com.qkzc.workerm.ui.theme.LoginBackgroundTop
import com.qkzc.workerm.ui.theme.LoginBody
import com.qkzc.workerm.ui.theme.LoginCardShadow
import com.qkzc.workerm.ui.theme.LoginFieldBorder
import com.qkzc.workerm.ui.theme.LoginInfoBackground
import com.qkzc.workerm.ui.theme.LoginLinkBlue
import com.qkzc.workerm.ui.theme.LoginMuted
import com.qkzc.workerm.ui.theme.LoginPlaceholder
import com.qkzc.workerm.ui.theme.LoginPrimaryBlue
import com.qkzc.workerm.ui.theme.LoginPrimaryBlueEnd
import com.qkzc.workerm.ui.theme.LoginTitle
import com.qkzc.workerm.ui.theme.WorkerMTheme
import kotlin.math.roundToInt

@Composable
fun SupervisionLoginRoute(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onPrivacyClick: () -> Unit,
    onSecurityPolicyClick: () -> Unit,
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    SupervisionLoginScreen(
        phone = phone,
        password = password,
        passwordVisible = passwordVisible,
        loading = loading,
        onPhoneChange = { value ->
            phone = value.filter(Char::isDigit).take(11)
        },
        onPasswordChange = { value ->
            password = value
        },
        onPasswordVisibilityChange = {
            passwordVisible = !passwordVisible
        },
        onLogin = onLogin,
        onPrivacyClick = onPrivacyClick,
        onSecurityPolicyClick = onSecurityPolicyClick,
    )
}

@Composable
fun SupervisionLoginScreen(
    phone: String,
    password: String,
    passwordVisible: Boolean,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onLogin: (String, String) -> Unit,
    onPrivacyClick: () -> Unit,
    onSecurityPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(validationMessage) {
        val message = validationMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        validationMessage = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        LoginBackgroundTop,
                        LoginBackgroundMiddle,
                        LoginBackgroundBottom,
                    ),
                ),
            ),
    ) {
        BuildingBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            val availableHeightDp = maxHeight.value.roundToInt()
            val scrollableLayout = shouldUseScrollableLoginLayout(availableHeightDp)
            val compactHeight = maxHeight < 760.dp
            val logoTop = when {
                scrollableLayout -> 24.dp
                compactHeight -> 34.dp
                else -> 42.dp
            }
            val logoSize = if (compactHeight) 62.dp else 68.dp
            val logoTitleGap = if (compactHeight) 16.dp else 18.dp
            val titleDescriptionGap = if (compactHeight) 12.dp else 14.dp
            val cardTop = if (compactHeight) 24.dp else 30.dp
            val bottomTop = if (compactHeight) 24.dp else 30.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scrollableLayout) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(logoTop))

                SecurityLogo(
                    modifier = Modifier
                        .size(logoSize)
                        .semantics {
                            contentDescription = "安全监管标志"
                        },
                )

                Spacer(modifier = Modifier.height(logoTitleGap))

                Text(
                    text = "监管端登录",
                    color = LoginTitle,
                    fontSize = if (compactHeight) 33.sp else 36.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                )

                Spacer(modifier = Modifier.height(titleDescriptionGap))

                Text(
                    text = "欢迎登录监管平台，\n实时监管、数据洞察、风险预警。",
                    color = LoginBody,
                    fontSize = if (compactHeight) 15.sp else 16.sp,
                    lineHeight = if (compactHeight) 23.sp else 25.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                )

                Spacer(modifier = Modifier.height(cardTop))

                LoginCard(
                    phone = phone,
                    password = password,
                    passwordVisible = passwordVisible,
                    loading = loading,
                    onPhoneChange = onPhoneChange,
                    onPasswordChange = onPasswordChange,
                    onPasswordVisibilityChange = onPasswordVisibilityChange,
                    onSubmit = {
                        val error = validateSupervisorLoginInput(phone, password)
                        if (error == null) {
                            focusManager.clearFocus()
                            onLogin(phone.trim(), password)
                        } else {
                            validationMessage = error
                        }
                    },
                    focusManager = focusManager,
                    modifier = Modifier.widthIn(max = 520.dp),
                )

                Spacer(modifier = Modifier.height(bottomTop))

                BottomAgreementSection(
                    onPrivacyClick = onPrivacyClick,
                    onSecurityPolicyClick = onSecurityPolicyClick,
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                )

                if (scrollableLayout) {
                    Spacer(modifier = Modifier.height(18.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun LoginCard(
    phone: String,
    password: String,
    passwordVisible: Boolean,
    loading: Boolean,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = cardShape,
                clip = false,
                ambientColor = LoginCardShadow,
                spotColor = LoginCardShadow,
            )
            .clip(cardShape)
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoBanner()

            Spacer(modifier = Modifier.height(28.dp))

            LoginTextField(
                value = phone,
                onValueChange = onPhoneChange,
                placeholder = "请输入手机号",
                keyboardType = KeyboardType.Phone,
                contentDescription = "手机号输入框",
                leadingIcon = {
                    PhoneLineIcon(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF616875),
                    )
                },
            )

            Spacer(modifier = Modifier.height(22.dp))

            LoginTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "请输入密码",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                contentDescription = "密码输入框",
                isPassword = true,
                leadingIcon = {
                    LockLineIcon(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF616875),
                    )
                },
                trailingIcon = {
                    PasswordVisibilityButton(
                        passwordVisible = passwordVisible,
                        onClick = onPasswordVisibilityChange,
                    )
                },
            )

            Spacer(modifier = Modifier.height(28.dp))

            GradientLoginButton(
                text = if (loading) "正在登录..." else "登录并进入监管端",
                enabled = !loading,
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun InfoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LoginInfoBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoCircleIcon(
            modifier = Modifier.size(24.dp),
            color = LoginPrimaryBlue,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "请输入分配的账号信息登录监管端",
            color = LoginTitle,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    contentDescription: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, LoginFieldBorder, shape)
            .padding(start = 18.dp, end = if (trailingIcon == null) 18.dp else 6.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (isPassword) {
                    password()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon()

        Spacer(modifier = Modifier.width(16.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF1F2937),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(LoginPrimaryBlue),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = LoginPlaceholder,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            letterSpacing = 0.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(2.dp))
            trailingIcon()
        }
    }
}

@Composable
private fun PasswordVisibilityButton(
    passwordVisible: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (passwordVisible) {
                    "隐藏密码"
                } else {
                    "显示密码"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (passwordVisible) {
            EyeLineIcon(
                modifier = Modifier.size(30.dp),
                color = Color(0xFF616875),
            )
        } else {
            EyeOffLineIcon(
                modifier = Modifier.size(30.dp),
                color = Color(0xFF616875),
            )
        }
    }
}

@Composable
private fun GradientLoginButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val buttonAlpha = if (enabled) 1f else 0.68f

    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0x331264F3),
                spotColor = Color(0x331264F3),
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        LoginPrimaryBlue.copy(alpha = buttonAlpha),
                        LoginPrimaryBlueEnd.copy(alpha = buttonAlpha),
                    ),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomAgreementSection(
    onPrivacyClick: () -> Unit,
    onSecurityPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountShieldIcon(
                modifier = Modifier.size(25.dp),
                color = Color(0xFF657080),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "账号由系统管理员统一分配",
                color = LoginMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
            )
        }

        Row(verticalAlignment = Alignment.Top) {
            DocumentLineIcon(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(25.dp),
                color = Color(0xFF657080),
            )

            Spacer(modifier = Modifier.width(14.dp))

            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = "登录即表示你已阅读并同意",
                    color = LoginMuted,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                )

                LinkText(
                    text = "《隐私说明》",
                    contentDescription = "查看隐私说明",
                    onClick = onPrivacyClick,
                )

                Text(
                    text = " 与 ",
                    color = LoginMuted,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                )

                LinkText(
                    text = "《数据安全规范》",
                    contentDescription = "查看数据安全规范",
                    onClick = onSecurityPolicyClick,
                )
            }
        }
    }
}

@Composable
private fun LinkText(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = LoginLinkBlue,
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append(text)
            }
        },
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
    )
}

@Composable
private fun SecurityLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.085f
        val shieldBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF5FA2FF), Color(0xFF1264F3)),
            start = Offset(w * 0.15f, h * 0.12f),
            end = Offset(w * 0.86f, h * 0.88f),
        )

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            cubicTo(w * 0.62f, h * 0.17f, w * 0.74f, h * 0.22f, w * 0.84f, h * 0.25f)
            lineTo(w * 0.84f, h * 0.58f)
            cubicTo(w * 0.84f, h * 0.75f, w * 0.68f, h * 0.87f, w * 0.50f, h * 0.94f)
            cubicTo(w * 0.32f, h * 0.87f, w * 0.16f, h * 0.75f, w * 0.16f, h * 0.58f)
            lineTo(w * 0.16f, h * 0.25f)
            cubicTo(w * 0.27f, h * 0.22f, w * 0.39f, h * 0.17f, w * 0.50f, h * 0.08f)
            close()
        }

        drawPath(
            path = shield,
            color = Color(0xFF2F7BF5).copy(alpha = 0.08f),
        )

        drawPath(
            path = shield,
            brush = shieldBrush,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        val barWidth = w * 0.12f
        val barRadius = w * 0.025f
        drawRoundRect(
            brush = shieldBrush,
            topLeft = Offset(w * 0.34f, h * 0.50f),
            size = Size(barWidth, h * 0.24f),
            cornerRadius = CornerRadius(barRadius, barRadius),
        )
        drawRoundRect(
            brush = shieldBrush,
            topLeft = Offset(w * 0.49f, h * 0.36f),
            size = Size(barWidth, h * 0.38f),
            cornerRadius = CornerRadius(barRadius, barRadius),
        )
        drawRoundRect(
            brush = shieldBrush,
            topLeft = Offset(w * 0.64f, h * 0.57f),
            size = Size(barWidth, h * 0.17f),
            cornerRadius = CornerRadius(barRadius, barRadius),
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF61AAFF), Color(0xFF1264F3)),
                start = Offset(w * 0.62f, h * 0.54f),
                end = Offset(w, h),
            ),
            radius = w * 0.20f,
            center = Offset(w * 0.78f, h * 0.70f),
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.69f, h * 0.69f),
            end = Offset(w * 0.76f, h * 0.77f),
            strokeWidth = strokeWidth * 0.65f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.76f, h * 0.77f),
            end = Offset(w * 0.90f, h * 0.61f),
            strokeWidth = strokeWidth * 0.65f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BuildingBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseBlue = Color(0xFF1264F3)

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                drawCircle(
                    color = baseBlue.copy(alpha = 0.045f),
                    radius = 1.6f,
                    center = Offset(
                        x = w * 0.78f + col * w * 0.035f,
                        y = h * 0.17f + row * h * 0.019f,
                    ),
                )
            }
        }

        val backTower = Path().apply {
            moveTo(w * 0.75f, h * 0.29f)
            lineTo(w * 0.96f, h * 0.23f)
            lineTo(w * 1.09f, h * 0.29f)
            lineTo(w * 1.09f, h * 0.62f)
            lineTo(w * 0.75f, h * 0.62f)
            close()
        }
        drawPath(backTower, color = baseBlue.copy(alpha = 0.065f))

        val sideTower = Path().apply {
            moveTo(w * 0.89f, h * 0.23f)
            lineTo(w * 0.96f, h * 0.19f)
            lineTo(w * 1.09f, h * 0.27f)
            lineTo(w * 1.09f, h * 0.62f)
            lineTo(w * 0.89f, h * 0.55f)
            close()
        }
        drawPath(sideTower, color = Color(0xFF0057E8).copy(alpha = 0.045f))

        val frontTower = Path().apply {
            moveTo(w * 0.60f, h * 0.35f)
            lineTo(w * 0.75f, h * 0.29f)
            lineTo(w * 0.90f, h * 0.38f)
            lineTo(w * 0.90f, h * 0.64f)
            lineTo(w * 0.60f, h * 0.64f)
            close()
        }
        drawPath(frontTower, color = baseBlue.copy(alpha = 0.055f))

        repeat(8) { index ->
            val y = h * (0.39f + index * 0.034f)
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(w * 0.63f, y),
                end = Offset(w * 1.04f, y - h * 0.055f),
                strokeWidth = 9f,
                cap = StrokeCap.Square,
            )
        }

        drawRect(
            color = Color.White.copy(alpha = 0.30f),
            topLeft = Offset(w * 0.91f, h * 0.21f),
            size = Size(w * 0.018f, h * 0.42f),
        )
    }
}

@Composable
private fun InfoCircleIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        drawCircle(color = color)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.50f, size.height * 0.45f),
            end = Offset(size.width * 0.50f, size.height * 0.72f),
            strokeWidth = size.width * 0.10f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = size.width * 0.045f,
            center = Offset(size.width * 0.50f, size.height * 0.30f),
        )
    }
}

@Composable
private fun PhoneLineIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.26f, size.height * 0.08f),
            size = Size(size.width * 0.48f, size.height * 0.84f),
            cornerRadius = CornerRadius(size.width * 0.07f),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = size.width * 0.035f,
            center = Offset(size.width * 0.50f, size.height * 0.82f),
        )
    }
}

@Composable
private fun LockLineIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.44f),
            size = Size(size.width * 0.60f, size.height * 0.42f),
            cornerRadius = CornerRadius(size.width * 0.07f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.31f, size.height * 0.14f),
            size = Size(size.width * 0.38f, size.height * 0.48f),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = size.width * 0.035f,
            center = Offset(size.width * 0.50f, size.height * 0.64f),
        )
    }
}

@Composable
private fun EyeLineIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val eye = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.50f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.24f,
                size.width * 0.72f,
                size.height * 0.24f,
                size.width * 0.90f,
                size.height * 0.50f,
            )
            cubicTo(
                size.width * 0.72f,
                size.height * 0.76f,
                size.width * 0.28f,
                size.height * 0.76f,
                size.width * 0.10f,
                size.height * 0.50f,
            )
        }
        drawPath(path = eye, color = color, style = stroke)
        drawCircle(
            color = color,
            radius = size.width * 0.11f,
            center = Offset(size.width * 0.50f, size.height * 0.50f),
            style = stroke,
        )
    }
}

@Composable
private fun EyeOffLineIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val eye = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.50f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.24f,
                size.width * 0.72f,
                size.height * 0.24f,
                size.width * 0.90f,
                size.height * 0.50f,
            )
            cubicTo(
                size.width * 0.72f,
                size.height * 0.76f,
                size.width * 0.28f,
                size.height * 0.76f,
                size.width * 0.10f,
                size.height * 0.50f,
            )
        }
        drawPath(path = eye, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(size.width * 0.20f, size.height * 0.16f),
            end = Offset(size.width * 0.82f, size.height * 0.84f),
            strokeWidth = size.width * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun AccountShieldIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val shield = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.08f)
            lineTo(size.width * 0.82f, size.height * 0.22f)
            lineTo(size.width * 0.82f, size.height * 0.54f)
            cubicTo(
                size.width * 0.82f,
                size.height * 0.72f,
                size.width * 0.65f,
                size.height * 0.86f,
                size.width * 0.50f,
                size.height * 0.94f,
            )
            cubicTo(
                size.width * 0.35f,
                size.height * 0.86f,
                size.width * 0.18f,
                size.height * 0.72f,
                size.width * 0.18f,
                size.height * 0.54f,
            )
            lineTo(size.width * 0.18f, size.height * 0.22f)
            close()
        }
        drawPath(shield, color = color, style = stroke)
        drawCircle(
            color = color,
            radius = size.width * 0.09f,
            center = Offset(size.width * 0.50f, size.height * 0.42f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(size.width * 0.35f, size.height * 0.53f),
            size = Size(size.width * 0.30f, size.height * 0.24f),
            style = stroke,
        )
    }
}

@Composable
private fun DocumentLineIcon(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.10f),
            size = Size(size.width * 0.60f, size.height * 0.80f),
            cornerRadius = CornerRadius(size.width * 0.025f),
            style = stroke,
        )
        repeat(3) { index ->
            val y = size.height * (0.34f + index * 0.16f)
            drawLine(
                color = color,
                start = Offset(size.width * 0.34f, y),
                end = Offset(size.width * 0.66f, y),
                strokeWidth = size.width * 0.06f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview(
    name = "监管端登录 - 正常手机",
    widthDp = 393,
    heightDp = 873,
    showBackground = true,
)
@Composable
private fun SupervisionLoginScreenPreview() {
    WorkerMTheme {
        SupervisionLoginScreen(
            phone = "",
            password = "",
            passwordVisible = false,
            onPhoneChange = {},
            onPasswordChange = {},
            onPasswordVisibilityChange = {},
            onLogin = { _, _ -> },
            onPrivacyClick = {},
            onSecurityPolicyClick = {},
        )
    }
}

@Preview(
    name = "监管端登录 - 小屏窄屏",
    widthDp = 360,
    heightDp = 640,
    showBackground = true,
)
@Composable
private fun SupervisionLoginScreenSmallPreview() {
    WorkerMTheme {
        SupervisionLoginScreen(
            phone = "13800138000",
            password = "123456",
            passwordVisible = false,
            onPhoneChange = {},
            onPasswordChange = {},
            onPasswordVisibilityChange = {},
            onLogin = { _, _ -> },
            onPrivacyClick = {},
            onSecurityPolicyClick = {},
        )
    }
}
