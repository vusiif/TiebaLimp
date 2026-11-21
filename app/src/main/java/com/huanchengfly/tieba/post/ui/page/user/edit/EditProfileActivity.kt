package com.huanchengfly.tieba.post.ui.page.user.edit

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestPermission
import com.huanchengfly.tieba.post.utils.shouldUsePhotoPicker
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import java.io.File

@AndroidEntryPoint
class EditProfileActivity : BaseActivity() {
    private val uCropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.send(EditProfileIntent.UploadPortrait(File(cacheDir, "cropped_portrait")))
            } else if (it.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(it.data!!)
                cropError!!.printStackTrace()
            }
        }

    private val pickMediasLauncher =
        registerPickMediasLauncher { (_, uris) ->
            if (uris.isNotEmpty()) {
                val sourceUri = uris[0]
                runCatching {
                    val tempSourceFile = File(cacheDir, "temp_avatar.jpg")
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        tempSourceFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val sourceFileUri = Uri.fromFile(tempSourceFile)
                    val destUri = Uri.fromFile(File(cacheDir, "cropped_portrait"))
                    val intent = UCrop.of(sourceFileUri, destUri)
                        .withAspectRatio(1f, 1f)
                        .withOptions(UCrop.Options().apply {
                            setShowCropFrame(true)
                            setShowCropGrid(true)
                            setStatusBarColor(
                                ColorUtils.getDarkerColor(
                                    ThemeUtils.getColorByAttr(
                                        this@EditProfileActivity,
                                        R.attr.colorPrimary
                                    )
                                )
                            )
                            setToolbarColor(
                                ThemeUtils.getColorByAttr(
                                    this@EditProfileActivity,
                                    R.attr.colorPrimary
                                )
                            )
                            setToolbarWidgetColor(
                                ThemeUtils.getColorByAttr(
                                    this@EditProfileActivity,
                                    R.attr.colorTextOnPrimary
                                )
                            )
                            setActiveControlsWidgetColor(
                                ThemeUtils.getColorByAttr(
                                    this@EditProfileActivity,
                                    R.attr.colorAccent
                                )
                            )
                            setLogoColor(
                                ThemeUtils.getColorByAttr(
                                    this@EditProfileActivity,
                                    R.attr.colorPrimary
                                )
                            )
                            setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        })
                        .getIntent(this@EditProfileActivity)
                    uCropLauncher.launch(intent)
                }
            }
        }

    private val viewModel: EditProfileViewModel by viewModels()

    private val intents by lazy {
        merge(
            flowOf(EditProfileIntent.Init(AccountUtil.getUid() ?: "0"))
        )
    }
    private val handler = Handler(Looper.getMainLooper())
    override val isNeedImmersionBar: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TiebaLiteTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.apply {
                        setStatusBarColor(
                            Color.Transparent,
                            darkIcons = ThemeUtil.isStatusBarFontDark()
                        )
                        setNavigationBarColor(
                            Color.Transparent,
                            darkIcons = ThemeUtil.isNavigationBarFontDark()
                        )
                    }
                }
                PageEditProfile(viewModel, onBackPressed = { onBackPressed() })
            }
        }
        handler.post {
            intents.onEach(viewModel::send).launchIn(lifecycleScope)
        }
        viewModel.uiEventFlow
            .filterIsInstance<EditProfileEvent>()
            .collectIn(this) { handleEvent(it) }
    }

    private fun handleEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.Init.Fail -> {
                toastShort(event.toast)
            }

            is EditProfileEvent.Submit.Result -> {
                if (event.success) {
                    if (event.changed) toastShort(R.string.toast_success)
                    finish()
                } else {
                    toastShort(event.message)
                }
            }

            EditProfileEvent.UploadPortrait.Pick -> {
                if (shouldUsePhotoPicker()) {
                    pickMediasLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
                } else {
                    requestPermission {
                        permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            listOf(
                                PermissionUtils.READ_EXTERNAL_STORAGE,
                                PermissionUtils.WRITE_EXTERNAL_STORAGE
                            )
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            listOf(
                                PermissionUtils.READ_EXTERNAL_STORAGE
                            )
                        } else {
                            listOf(PermissionUtils.READ_MEDIA_IMAGES)
                        }
                        description = context.getString(R.string.tip_permission_storage)
                        onGranted = {
                            pickMediasLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
                        }
                        onDenied = {
                            toastShort(R.string.toast_no_permission_upload_portrait)
                        }
                    }
                }
            }

            is EditProfileEvent.UploadPortrait.Fail -> toastShort(event.error)
            is EditProfileEvent.UploadPortrait.Success -> toastShort(event.message)
        }
    }
}
