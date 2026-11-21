package com.huanchengfly.tieba.post.ui.page.user.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.CounterTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.utils.StringUtil

@Composable
fun EditProfileCard(
    portrait: String,
    name: String,
    nickName: String,
    sex: Int,
    intro: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onNickNameChange: ((String) -> Unit)? = null,
    onIntroChange: ((String) -> Unit)? = null,
    onUploadPortrait: (() -> Unit)? = null,
    onModifySex: (() -> Unit)? = null,
    color: Color = ExtendedTheme.colors.background,
) {
    val context = LocalContext.current
    Card(elevation = 0.dp, backgroundColor = color, modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_margin))
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally)
                    .placeholder(visible = loading)
            ) {
                AsyncImage(
                    imageUri = StringUtil.getAvatarUrl(portrait),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    modifier = Modifier
                        .background(color = Color(0x77000000))
                        .fillMaxSize(),
                    onClick = {
                        onUploadPortrait?.invoke()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = stringResource(id = R.string.upload_portrait),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (
                    nameTitle,
                    nameContent,
                    nickNameTitle,
                    nickNameContent,
                    sexTitle,
                    sexContent,
                    introTitle,
                    introContent,
                ) = createRefs()

                val titleEndBarrier = createEndBarrier(
                    nameTitle,
                    nickNameTitle,
                    sexTitle,
                    introTitle,
                    margin = 24.dp
                )

                Text(
                    text = stringResource(id = R.string.title_username),
                    color = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier.constrainAs(nameTitle) {
                        top.linkTo(parent.top)
                        bottom.linkTo(nameContent.bottom)
                        start.linkTo(parent.start)
                    }
                )
                Text(
                    text = name,
                    color = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier
                        .constrainAs(nameContent) {
                            top.linkTo(nameTitle.top)
                            bottom.linkTo(nameTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading)
                )

                Text(
                    text = stringResource(id = R.string.title_nickname),
                    color = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier.constrainAs(nickNameTitle) {
                        top.linkTo(nameTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                Row(
                    modifier = Modifier
                        .constrainAs(nickNameContent) {
                            top.linkTo(nickNameTitle.top)
                            bottom.linkTo(nickNameTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading)
                ) {
                    BaseTextField(
                        value = nickName,
                        onValueChange = { onNickNameChange?.invoke(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        maxLines = 1,
                    )
                }

                Text(
                    text = stringResource(id = R.string.profile_sex),
                    color = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier.constrainAs(sexTitle) {
                        top.linkTo(nickNameTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                Row(
                    modifier = Modifier
                        .constrainAs(sexContent) {
                            top.linkTo(sexTitle.top)
                            bottom.linkTo(sexTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onModifySex?.invoke() }
                        .placeholder(visible = loading)
                ) {
                    Text(
                        text = stringResource(
                            id = when (sex) {
                                1 -> R.string.profile_sex_male
                                2 -> R.string.profile_sex_female
                                else -> R.string.profile_sex_unset
                            }
                        ),
                        color = ExtendedTheme.colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_chevron_right),
                        contentDescription = null,
                        tint = ExtendedTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

                Text(
                    text = stringResource(id = R.string.title_intro),
                    color = ExtendedTheme.colors.textDisabled,
                    modifier = Modifier.constrainAs(introTitle) {
                        top.linkTo(sexTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                CounterTextField(
                    value = intro,
                    onValueChange = { onIntroChange?.invoke(it) },
                    maxLength = 500,
                    countWhitespace = false,
                    onLengthBeyondRestrict = { context.toastShort(R.string.toast_intro_length_beyond_restrict) },
                    placeholder = { Text(text = stringResource(id = R.string.tip_no_intro)) },
                    modifier = Modifier
                        .constrainAs(introContent) {
                            top.linkTo(introTitle.top)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading),
                )
            }
        }
    }
}

@Preview
@Composable
fun EditProfileCardPreview() {
    EditProfileCard(
        portrait = "",
        name = "test",
        nickName = "test",
        sex = 1,
        intro = "test",
        loading = false,
        color = Color.White
    )
}

@Preview
@Composable
fun EditProfileCardLoadingPreview() {
    EditProfileCard(
        portrait = "",
        name = "",
        nickName = "",
        sex = 0,
        intro = "",
        loading = true,
        color = Color.White
    )
}
