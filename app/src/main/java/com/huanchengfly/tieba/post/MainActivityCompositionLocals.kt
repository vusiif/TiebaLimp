package com.huanchengfly.tieba.post

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.ramcosta.composedestinations.spec.DestinationSpec
import kotlinx.coroutines.flow.Flow

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }

val LocalDevicePosture =
    staticCompositionLocalOf<State<DevicePosture>> { throw IllegalStateException("not allowed here!") }

val LocalNavController =
    staticCompositionLocalOf<NavHostController> { throw IllegalStateException("not allowed here!") }

val LocalDestination = compositionLocalOf<DestinationSpec<*>?> { null }
