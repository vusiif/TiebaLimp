package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import org.litepal.LitePal
import org.litepal.extension.findAllAsync
import java.util.UUID

/**
 * Helper class handling remote Baidu account synchronization flow and persistence.
 */
object AccountSyncHelper {
    fun updateAccount(
        account: Account,
        loginBean: LoginBean,
    ) {
        account.apply {
            uid = loginBean.user.id
            name = loginBean.user.name
            portrait = loginBean.user.portrait
            tbs = loginBean.anti.tbs
            if (uuid.isNullOrBlank()) uuid = UUID.randomUUID().toString()
        }
    }

    fun fetchAccountFlow(account: Account = AccountUtil.getLoginInfo()!!): Flow<Account> {
        return fetchAccountFlow(account.bduss, account.sToken, account.cookie)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchAccountFlow(
        bduss: String,
        sToken: String,
        cookie: String? = null
    ): Flow<Account> {
        return TiebaApi.getInstance()
            .loginFlow(bduss, sToken)
            .zip(TiebaApi.getInstance().initNickNameFlow(bduss, sToken)) { loginBean, _ ->
                AccountUtil.getAccountInfoByUid(loginBean.user.id)?.apply {
                    this.bduss = bduss
                    this.sToken = sToken
                    this.cookie = cookie ?: AccountCookieUtil.getBdussCookie(bduss)
                    updateAccount(this, loginBean)
                } ?: Account(
                    loginBean.user.id,
                    loginBean.user.name,
                    bduss,
                    loginBean.anti.tbs,
                    loginBean.user.portrait,
                    sToken,
                    cookie ?: AccountCookieUtil.getBdussCookie(bduss),
                )
            }
            .zip(SofireUtils.fetchZid()) { account, zid ->
                account.apply { this.zid = zid }
            }
            .flatMapConcat { account ->
                TiebaApi.getInstance()
                    .getUserInfoFlow(account.uid.toLong(), account.bduss, account.sToken)
                    .map { checkNotNull(it.data_?.user) }
                    .map { user ->
                        account.apply {
                            nameShow = user.nameShow
                            portrait = user.portrait
                        }
                    }
                    .catch {
                        emit(account)
                    }
            }
            .onEach { account ->
                account.toEncryptedCopy().saveOrUpdateAsync("uid = ?", account.uid)
                    .listen {
                        LitePal.findAllAsync<Account>()
                            .listen { list ->
                                val decrypted = list.map { it.decryptSensitiveFields() }
                                AccountUtil.updateAllAccounts(decrypted)
                                if (AccountUtil.currentAccount?.uid == account.uid) {
                                    AccountUtil.updateCurrentAccount(account)
                                }
                            }
                    }
            }
            .flowOn(Dispatchers.IO)
    }
}
