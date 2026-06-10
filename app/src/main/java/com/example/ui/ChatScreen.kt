package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.database.SourceInfo
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeMessages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val apiKeyError by viewModel.apiKeyError.collectAsStateWithLifecycle()
    val isDevMode by viewModel.isDevMode.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    var showEditProfile by remember { mutableStateOf(false) }

    // Auto-scroll to the bottom when new messages arrive - fast snap in mock mode
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            if (isDevMode) {
                listState.scrollToItem(activeMessages.size - 1) // Snap instantly for max performance!
            } else {
                listState.animateScrollToItem(activeMessages.size - 1)
            }
        }
    }

    if (showEditProfile) {
        EditProfileScreen(
            viewModel = authViewModel,
            onBack = { showEditProfile = false }
        )
    } else {
        ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp), // Crisp minimal square layout
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "AURA.",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    currentUser?.let { user ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    showEditProfile = true
                                }
                                .padding(all = 8.dp)
                                .testTag("sidebar_profile_row")
                        ) {
                            if (!user.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = (user.displayName ?: user.email).take(1).uppercase()
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName ?: user.email.substringBefore("@"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RECENTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (sessions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "no archived threads",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(sessions, key = { it.id }) { session ->
                                    val isSelected = session.id == activeSessionId
                                    SessionItem(
                                        title = session.title,
                                        isSelected = isSelected,
                                        onSelect = {
                                            viewModel.selectSession(session.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        onDelete = {
                                            viewModel.deleteSession(session.id)
                                        },
                                        modifier = Modifier.testTag("session_item_${session.id}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable {
                                    viewModel.startNewSession()
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New Chat",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "SIGN OUT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    scope.launch {
                                        drawerState.close()
                                        authViewModel.logout()
                                    }
                                }
                                .padding(8.dp)
                                .testTag("btn_logout")
                        )
                    }
                }
            }
        },
        content = {
            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .then(if (LocalInspectionMode.current || isDevMode) Modifier else Modifier.statusBarsPadding())
                    ) {
                        // Row containing the menu button and inline brand title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Archive",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "AURA.",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "INTELLIGENCE, SIMPLIFIED.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .then(if (LocalInspectionMode.current || isDevMode) Modifier else Modifier.navigationBarsPadding())
                            .imePadding()
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        InteractionArea(
                            isGenerating = isGenerating,
                            onSendMessage = { text ->
                                viewModel.sendMessage(text)
                            },
                            onFocusGained = {
                                scope.launch {
                                    if (activeMessages.isNotEmpty()) {
                                        listState.animateScrollToItem(activeMessages.size - 1)
                                    }
                                }
                            }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (apiKeyError != null && !isDevMode) {
                        // Display API Key warning in crisp, highly attention-worthy format
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(18.dp)
                                .align(Alignment.Center)
                        ) {
                            Text(
                                text = "AURA. NEED API CONFIGURATION OR LOCAL CANVAS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = apiKeyError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        viewModel.toggleDevMode()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LAUNCH LOCAL WORKSPACE (FAST MOCK MODE)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else if (activeMessages.isEmpty()) {
                        // Place the 'what's new??' text cleanly in the bottom-left corner, aligned with the hamburger menu (three lines)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = "what's new??",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                lineHeight = 36.sp,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Clean conversation flow without speech bubbles for premium contrast & readability
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(activeMessages, key = { it.id }) { message ->
                                MessageItem(
                                    content = message.content,
                                    role = message.role,
                                    sources = message.getSources(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )
                            }
                            
                            if (isGenerating) {
                                item(key = "generating") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[AURA. IS RETRIEVING...]",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
    }
}

@Composable
fun SessionItem(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Session",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier
                .size(16.dp)
                .clickable { onDelete() }
        )
    }
}

sealed interface ParsedElement {
    data class H1(val text: String) : ParsedElement
    data class H2(val text: String) : ParsedElement
    data class H3(val text: String) : ParsedElement
    data class H4(val text: String) : ParsedElement
    data class Bullet(val text: String) : ParsedElement
    data class Numbered(val number: String, val text: String) : ParsedElement
    data class Body(val text: String) : ParsedElement
}

fun parseMarkdown(content: String): List<ParsedElement> {
    val lines = content.split("\n")
    val elements = mutableListOf<ParsedElement>()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("#### ") -> {
                elements.add(ParsedElement.H4(trimmed.substring(5)))
            }
            trimmed.startsWith("### ") -> {
                elements.add(ParsedElement.H3(trimmed.substring(4)))
            }
            trimmed.startsWith("## ") -> {
                elements.add(ParsedElement.H2(trimmed.substring(3)))
            }
            trimmed.startsWith("# ") -> {
                elements.add(ParsedElement.H1(trimmed.substring(2)))
            }
            trimmed.startsWith("- ") -> {
                elements.add(ParsedElement.Bullet(trimmed.substring(2)))
            }
            trimmed.startsWith("* ") -> {
                elements.add(ParsedElement.Bullet(trimmed.substring(2)))
            }
            trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                val index = trimmed.indexOf(".")
                val number = trimmed.substring(0, index + 1)
                val text = trimmed.substring(index + 1).trim()
                elements.add(ParsedElement.Numbered(number, text))
            }
            else -> {
                elements.add(ParsedElement.Body(line))
            }
        }
    }
    return elements
}

fun buildFormattedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val boldStart = text.indexOf("**", cursor)
            if (boldStart == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, boldStart))
            
            val boldEnd = text.indexOf("**", boldStart + 2)
            if (boldEnd == -1) {
                append(text.substring(boldStart))
                break
            }
            
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text.substring(boldStart + 2, boldEnd))
            }
            cursor = boldEnd + 2
        }
    }
}

@Composable
fun MessageItem(
    content: String,
    role: String,
    sources: List<SourceInfo> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isUser = role == "user"
    val textColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White

    Column(
        modifier = modifier
            .padding(vertical = 12.dp)
            .testTag(if (isUser) "user_message" else "assistant_message")
    ) {
        // Header label
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUser) "[USER]" else "[AURA.]",
                style = MaterialTheme.typography.labelLarge,
                color = if (isUser) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (isUser) {
            Text(
                text = buildFormattedString(content),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val elements = parseMarkdown(content)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                elements.forEach { element ->
                    when (element) {
                        is ParsedElement.H1 -> {
                            Text(
                                text = buildFormattedString(element.text),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        is ParsedElement.H2 -> {
                            Text(
                                text = buildFormattedString(element.text),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 20.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        is ParsedElement.H3 -> {
                            Text(
                                text = buildFormattedString(element.text),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        is ParsedElement.H4 -> {
                            Text(
                                text = buildFormattedString(element.text),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        is ParsedElement.Bullet -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = buildFormattedString(element.text),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        is ParsedElement.Numbered -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = element.number,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = buildFormattedString(element.text),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        is ParsedElement.Body -> {
                            if (element.text.isNotBlank()) {
                                Text(
                                    text = buildFormattedString(element.text),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        if (!isUser && sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOURCES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sources.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(6.dp))
                            .clickable { 
                                try {
                                    uriHandler.openUri(source.url)
                                } catch (e: Exception) {
                                    // Handle invalid URLs gracefully
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionArea(
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusGained: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isFocused) {
                            onFocusGained()
                        }
                        isFocused = focusState.isFocused
                    }
                    .testTag("prompt_input"),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                placeholder = {
                    Text(
                        text = "ask me",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                },
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF212121),
                    unfocusedContainerColor = Color(0xFF212121)
                ),
                shape = RoundedCornerShape(32.dp),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(40.dp)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .clickable(enabled = inputText.isNotBlank() && !isGenerating) {
                                onSendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Prompt",
                            tint = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    }
}

@Preview(name = "AURA Canvas", showBackground = true, showSystemUi = false)
@Composable
fun AuraCanvasPreview() {
    com.example.ui.theme.MyApplicationTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .size(380.dp, 750.dp)
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Simulated Menu bar with inline branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                         Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AURA.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "INTELLIGENCE, SIMPLIFIED.",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MessageItem(
                        content = "Analyze strategy and optimize rendering performance.",
                        role = "user"
                    )
                    MessageItem(
                        content = "Strategic model aligned. Fast response pipeline initialized. Extra status margins bypassed in isolated preview mode, keeping layout latency at absolute zero.",
                        role = "model"
                    )
                }

                InteractionArea(
                    isGenerating = false,
                    onSendMessage = {}
                )
            }
        }
    }
}
