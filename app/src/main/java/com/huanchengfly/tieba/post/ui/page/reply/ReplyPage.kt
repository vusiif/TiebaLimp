package com.huanchengfly.tieba.post.ui.page.reply

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.DraftModel
import com.huanchengfly.tieba.post.pxToDpFloat
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.EMOJI
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.IMAGE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.NONE
import com.huanchengfly.tieba.post.ui.utils.imeNestedScroll
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DraftHelper
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.hideKeyboard
import com.huanchengfly.tieba.post.utils.showKeyboard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(
    ExperimentalLayoutApi::class,
    FlowPreview::class
)
@Composable
internal fun ReplyPageContent(
    viewModel: ReplyViewModel,
    onBack: () -> Unit,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    isDialog: Boolean = false,
) {
    val accountUid = remember { AccountUtil.getUid() }
    val hash = remember(accountUid, threadId, postId, subPostId) {
        DraftHelper.getDraftHash(accountUid, threadId, postId, subPostId)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val curTbs = remember(tbs) { tbs ?: AccountUtil.getAccountInfo { this.tbs }.orEmpty() }

    val isUploading by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isUploading,
        initial = false
    )
    val isSending by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isSending,
        initial = false
    )
    val isReplying by remember { derivedStateOf { isUploading || isSending } }
    val replySuccess by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replySuccess,
        initial = false
    )
    val curKeyboardType by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replyPanelType,
        initial = NONE
    )
    val selectedImageList by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::selectedImageList,
        initial = persistentListOf()
    )
    val isOriginImage by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isOriginImage,
        initial = false
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    var initialText by remember { mutableStateOf("") }
    var waitEditTextToSet by remember { mutableStateOf(false) }
    var editTextView by remember { mutableStateOf<UndoableEditText?>(null) }
    fun getText(): String {
        return editTextView?.text?.toString().orEmpty()
    }

    fun setText(text: String) {
        if (editTextView != null) {
            editTextView?.setText(StringUtil.getEmoticonContent(editTextView!!, text))
            editTextView?.setSelection(text.length)
        } else {
            initialText = text
            waitEditTextToSet = true
        }
    }

    fun insertEmoticon(text: String) {
        editTextView?.apply {
            val start = selectionStart
            this.text?.insert(start, text)
            this.setText(StringUtil.getEmoticonContent(this, this.text))
            setSelection(start + text.length)
        }
    }

    var isDraftLoaded by remember { mutableStateOf(false) }
    val curTextFlow = remember { MutableStateFlow("") }
    val curText by curTextFlow.collectAsState()
    LaunchedEffect(isDraftLoaded) {
        if (!isDraftLoaded) return@LaunchedEffect
        snapshotFlow {
            DraftModel(
                text = curText,
                imageUris = selectedImageList,
                isOriginImage = isOriginImage
            )
        }
            .debounce(500)
            .distinctUntilChanged()
            .collect { draft ->
                if (!replySuccess) {
                    DraftHelper.saveDraft(accountUid, threadId, postId, subPostId, draft)
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isDraftLoaded && !replySuccess) {
                val latestText = editTextView?.text?.toString() ?: curTextFlow.value
                val latestImages = selectedImageList
                val latestOrigin = isOriginImage
                CoroutineScope(Dispatchers.IO).launch {
                    DraftHelper.saveDraft(
                        accountUid,
                        threadId,
                        postId,
                        subPostId,
                        DraftModel(latestText, latestImages, latestOrigin)
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        val draft = DraftHelper.loadDraftModel(accountUid, threadId, postId, subPostId)
        if (draft != null && !draft.isBlank()) {
            if (draft.text.isNotEmpty()) {
                setText(draft.text)
            }
            if (draft.imageUris.isNotEmpty() || draft.isOriginImage) {
                viewModel.send(ReplyUiIntent.RestoreDraft(draft.imageUris, draft.isOriginImage))
            }
        }
        isDraftLoaded = true
    }
    val textLength by remember { derivedStateOf { curText.length } }
    val isTextEmpty by remember { derivedStateOf { curText.isEmpty() } }

    viewModel.onEvent<ReplyUiEvent.ReplySuccess> {
        if (it.expInc.isEmpty()) {
            context.toastShort(R.string.toast_reply_success_default)
        } else {
            context.toastShort(R.string.toast_reply_success, it.expInc)
        }
        coroutineScope.launch {
            DraftHelper.clearDraft(accountUid, threadId, postId, subPostId)
            onBack()
        }
    }

    var waitUploadSuccessToSend by remember { mutableStateOf(false) }
    viewModel.onEvent<CommonUiEvent.Toast> {
        waitUploadSuccessToSend = false
    }

    viewModel.onEvent<ReplyUiEvent.UploadSuccess> {
        if (waitUploadSuccessToSend) {
            waitUploadSuccessToSend = false
            val imageContent = it.resultList
                .joinToString("\n") { image ->
                    "#(pic,${image.picId},${image.picInfo.originPic.width},${image.picInfo.originPic.height})"
                }
            viewModel.send(
                ReplyUiIntent.Send(
                    "${getText()}\n$imageContent",
                    forumId,
                    forumName,
                    threadId,
                    curTbs,
                    postId,
                    subPostId,
                    replyUserId,
                )
            )
        }
    }

    var closingPanel by remember { mutableStateOf(false) }
    var startClosingAnimation by remember { mutableStateOf(false) }

    fun showKeyboard() {
        editTextView?.apply {
            showKeyboard(context, this)
            requestFocus()
        }
        keyboardController?.show()
    }

    fun hideKeyboard() {
        editTextView?.apply {
            hideKeyboard(context, this)
            clearFocus()
        }
        keyboardController?.hide()
    }

    fun switchToPanel(type: ReplyPanelType) {
        if (curKeyboardType == type || type == NONE) {
            if (curKeyboardType != NONE) {
                showKeyboard()
                closingPanel = true
                startClosingAnimation = false
            }
            viewModel.send(ReplyUiIntent.SwitchPanel(NONE))
        } else {
            hideKeyboard()
            viewModel.send(ReplyUiIntent.SwitchPanel(type))
        }
    }

    MyBackHandler(
        enabled = curKeyboardType != NONE,
        currentScreen = ReplyPageDestination.takeUnless { isDialog }
    ) {
        switchToPanel(NONE)
    }

    val density = LocalDensity.current
    val imeInset = WindowInsets.ime
    val imeAnimationTargetInset = WindowInsets.imeAnimationTarget

    val imeCurrentHeight by produceState(initialValue = 0, imeInset, density) {
        snapshotFlow { imeInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationTargetHeight by produceState(
        initialValue = 0,
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationEnd by remember { derivedStateOf { imeCurrentHeight == imeAnimationTargetHeight } }
    val imeVisibleHeightPx by produceState(
        initialValue = remember { context.appPreferences.imeHeight },
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collect {
                context.appPreferences.imeHeight = it
                value = it
            }
    }

    val panelHeight by remember {
        derivedStateOf { max(imeVisibleHeightPx.pxToDpFloat(), 150f).dp }
    }

    val textMeasurer = rememberTextMeasurer()

    val minResult = textMeasurer.measure(
        AnnotatedString("\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    val maxResult = textMeasurer.measure(
        AnnotatedString("\n\n\n\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    LaunchedEffect(closingPanel, imeAnimationEnd) {
        if (closingPanel) {
            if (!startClosingAnimation && !imeAnimationEnd) {
                startClosingAnimation = true
            } else if (startClosingAnimation && imeAnimationEnd) {
                closingPanel = false
                startClosingAnimation = false
            }
        }
    }

    val canSend by remember { derivedStateOf { !isTextEmpty || selectedImageList.isNotEmpty() } }

    val textFieldScrollState = rememberScrollState()

    val parentModifier = if (curKeyboardType == NONE && !closingPanel) {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    } else {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .consumeWindowInsets(WindowInsets.ime)
    }

    Column(
        modifier = parentModifier,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.title_reply),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$textLength",
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary
            )
        }
        VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .imeNestedScroll(textFieldScrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .requiredHeightIn(min = minResult, max = maxResult)
                    .verticalScroll(textFieldScrollState)
            ) {
                AndroidView(
                    factory = { ctx ->
                        (View.inflate(
                            ctx,
                            R.layout.layout_reply_edit_text,
                            null
                        ) as UndoableEditText).apply {
                            editTextView = this
                            if (subPostId != null && subPostId != 0L && replyUserName != null) {
                                hint = ctx.getString(R.string.hint_reply, replyUserName)
                            }
                            setOnFocusChangeListener { _, hasFocus ->
                                if (hasFocus) {
                                    switchToPanel(NONE)
                                }
                            }
                            addTextChangedListener(
                                afterTextChanged = {
                                    curTextFlow.value = it?.toString().orEmpty()
                                }
                            )
                            if (waitEditTextToSet) {
                                waitEditTextToSet = false
                                this.setText(StringUtil.getEmoticonContent(this, initialText))
                                this.setSelection(initialText.length)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                )
            }
//            BaseTextField(
//                value = text,
//                onValueChange = { text = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .requiredHeightIn(min = minResult, max = maxResult)
//                    .focusRequester(focusRequester)
//                    .verticalScroll(textFieldScrollState)
//                    .onFocusChanged {
//                        Log.i("ReplyPage", "onFocusChanged: $it")
//                        if (it.hasFocus) {
//                            switchToKeyboard(NONE)
//                        }
//                    },
//                placeholder = { Text(text = stringResource(id = R.string.tip_reply)) },
//            )
        }
        ReplyActionToolbar(
            onEmojiClick = { switchToPanel(EMOJI) },
            canChooseImage = (postId == null || postId == 0L),
            selectedImageCount = selectedImageList.size,
            onImageClick = { switchToPanel(IMAGE) },
            isReplying = isReplying,
            canSend = canSend,
            onSendClick = {
                val replyContent = if (subPostId == null || subPostId == 0L) {
                    getText()
                } else {
                    "回复 #(reply, ${replyUserPortrait}, ${replyUserName}) :${getText()}"
                }
                if (selectedImageList.isEmpty()) {
                    viewModel.send(
                        ReplyUiIntent.Send(
                            content = replyContent,
                            forumId = forumId,
                            forumName = forumName,
                            threadId = threadId,
                            tbs = curTbs,
                            postId = postId,
                            subPostId = subPostId,
                            replyUserId = replyUserId
                        )
                    )
                } else {
                    waitUploadSuccessToSend = true
                    viewModel.send(
                        ReplyUiIntent.UploadImages(
                            forumName,
                            selectedImageList,
                            isOriginImage
                        )
                    )
                }
            }
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.ime)
        )
        if (curKeyboardType != NONE) {
            Column(modifier = Modifier.height(panelHeight)) {
                when (curKeyboardType) {
                    EMOJI -> {
                        EmoticonPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            onEmoticonClick = { emoticon ->
                                insertEmoticon("#(${emoticon.name})")
                            }
                        )
                    }

                    IMAGE -> {
                        ImagePanel(
                            selectedImages = selectedImageList,
                            onNewImageSelected = { uris ->
                                viewModel.send(ReplyUiIntent.AddImage(uris.map { it.toString() }))
                            },
                            onRemoveImage = {
                                viewModel.send(ReplyUiIntent.RemoveImage(it))
                            },
                            isOriginImage = isOriginImage,
                            onIsOriginImageChange = {
                                viewModel.send(ReplyUiIntent.ToggleIsOriginImage(it))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        )
                    }

                    else -> {}
                }
            }
        } else if (closingPanel) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
            )
        }
    }

    DisposableEffect(editTextView) {
        if (editTextView != null) {
            showKeyboard()
        }

        onDispose {
            if (editTextView != null) {
                hideKeyboard()
            }
        }
    }

    ReplyWarningDialog(
        threadId = threadId,
        postId = postId,
        onBack = onBack,
    )
}

// TODO: 将软键盘状态相关逻辑抽离出来
@Destination(style = DestinationStyleBottomSheet::class)
@Composable
fun ReplyPage(
    navigator: DestinationsNavigator,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    viewModel: ReplyViewModel = pageViewModel(),
) {
    ReplyPageContent(
        viewModel = viewModel,
        onBack = { navigator.navigateUp() },
        forumId = forumId,
        forumName = forumName,
        threadId = threadId,
        postId = postId,
        subPostId = subPostId,
        replyUserId = replyUserId,
        replyUserName = replyUserName,
        replyUserPortrait = replyUserPortrait,
        tbs = tbs
    )
}
