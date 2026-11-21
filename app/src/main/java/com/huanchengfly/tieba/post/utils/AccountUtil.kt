package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.litepal.LitePal.findAll
import org.litepal.LitePal.where
import org.litepal.extension.findFirst

@Stable
object AccountUtil {
    const val TAG = "AccountUtil"
    const val ACTION_SWITCH_ACCOUNT = "com.huanchengfly.tieba.post.action.SWITCH_ACCOUNT"

    val LocalAccount = staticCompositionLocalOf<Account?> { null }
    val AllAccounts = staticCompositionLocalOf<List<Account>> { emptyList() }

    @Composable
    fun LocalAccountProvider(content: @Composable () -> Unit) {
        val account by mutableCurrentAccountState
        val allAccounts by mutableAllAccountsState
        CompositionLocalProvider(
            LocalAccount provides account,
            AllAccounts provides allAccounts
        ) {
            content()
        }
    }

    @set:Synchronized
    private var mutableCurrentAccountState: MutableState<Account?> = mutableStateOf(null)

    private var mutableAllAccountsState: MutableState<List<Account>> = mutableStateOf(emptyList())

    internal fun updateAllAccounts(accounts: List<Account>) {
        mutableAllAccountsState.value = accounts
    }

    val currentAccount
        get() = mutableCurrentAccountState.value

    val allAccounts: List<Account>
        get() = mutableAllAccountsState.value

    fun init(context: Context) {
        val rawAccounts = findAll(Account::class.java)
        var migrated = false
        for (acc in rawAccounts) {
            if (AccountSecurityUtil.isPlaintext(acc.bduss) || AccountSecurityUtil.isPlaintext(acc.sToken)) {
                acc.toEncryptedCopy().update(acc.id.toLong())
                migrated = true
            }
        }
        val finalAccounts = if (migrated) findAll(Account::class.java) else rawAccounts
        val decryptedAccounts = finalAccounts.map { it.decryptSensitiveFields() }
        mutableAllAccountsState.value = decryptedAccounts

        val account = runCatching {
            val loginUser =
                context.getSharedPreferences("accountData", Context.MODE_PRIVATE).getInt("now", -1)
            if (loginUser == -1) {
                null
            } else {
                decryptedAccounts.firstOrNull { it.id == loginUser } ?: getAccountInfo(loginUser)
            }
        }.getOrNull()
        mutableCurrentAccountState.value = account
    }

    @JvmStatic
    fun getLoginInfo(): Account? {
        return currentAccount
    }

    @JvmStatic
    fun <T> getAccountInfo(getter: Account.() -> T): T? {
        return currentAccount?.getter()
    }

    fun newAccount(uid: String, account: Account, callback: (Boolean) -> Unit) {
        val encrypted = account.toEncryptedCopy()
        encrypted.saveOrUpdateAsync("uid = ?", uid).listen { success ->
            mutableAllAccountsState.value = findAll(Account::class.java).map { it.decryptSensitiveFields() }
            callback(success)
        }
    }

    private fun getAccountInfo(accountId: Int): Account {
        return allAccounts.firstOrNull { it.id == accountId }
            ?: where("id = ?", accountId.toString()).findFirst(Account::class.java)?.decryptSensitiveFields()
            ?: Account()
    }

    @JvmStatic
    fun getAccountInfoByUid(uid: String): Account? {
        return allAccounts.firstOrNull { it.uid == uid }
            ?: where("uid = ?", uid).findFirst<Account>()?.decryptSensitiveFields()
    }

    @JvmStatic
    fun getAccountInfoByBduss(bduss: String): Account {
        return allAccounts.firstOrNull { it.bduss == bduss }
            ?: run {
                val encBduss = AccountSecurityUtil.encrypt(bduss)
                where("bduss = ? or bduss = ?", encBduss, bduss).findFirst(Account::class.java)?.decryptSensitiveFields()
                    ?: Account()
            }
    }

    @JvmStatic
    fun isLoggedIn(): Boolean {
        return getLoginInfo() != null
    }

    @JvmStatic
    fun switchAccount(context: Context, id: Int): Boolean {
        context.sendBroadcast(Intent().setAction(ACTION_SWITCH_ACCOUNT))
        val account = runCatching { getAccountInfo(id) }.getOrNull() ?: return false
        mutableCurrentAccountState.value = account
        GlobalScope.launch {
            emitGlobalEvent(GlobalEvent.AccountSwitched)
        }
        return context.getSharedPreferences("accountData", Context.MODE_PRIVATE).edit()
            .putInt("now", id).commit()
    }

    fun fetchAccountFlow(account: Account = getLoginInfo()!!): Flow<Account> =
        AccountSyncHelper.fetchAccountFlow(account)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchAccountFlow(
        bduss: String,
        sToken: String,
        cookie: String? = null
    ): Flow<Account> = AccountSyncHelper.fetchAccountFlow(bduss, sToken, cookie)

    @JvmStatic
    fun parseCookie(cookie: String): Map<String, String> = AccountCookieUtil.parseCookie(cookie)

    @JvmStatic
    fun updateLoginInfo(cookie: String): Boolean {
        val bduss = AccountCookieUtil.extractBduss(cookie)
        val sToken = AccountCookieUtil.extractSToken(cookie)
        if (bduss != null && sToken != null) {
            val account = getAccountInfoByBduss(bduss)
            account.apply {
                this.sToken = sToken
                this.cookie = cookie
            }
            account.toEncryptedCopy().update(account.id.toLong())
            mutableAllAccountsState.value = allAccounts.map { if (it.id == account.id) account else it }
            return true
        }
        return false
    }

    fun exit(context: Context) {
        var accounts = allAccounts
        var account = getLoginInfo() ?: return
        account.delete()
        CookieManager.getInstance().removeAllCookies(null)
        if (accounts.size > 1) {
            accounts = allAccounts
            account = accounts[0]
            switchAccount(context, account.id)
            Toast.makeText(context, "退出登录成功，已切换至账号 " + account.nameShow, Toast.LENGTH_SHORT).show()
            return
        }
        mutableCurrentAccountState.value = null
        context.getSharedPreferences("accountData", Context.MODE_PRIVATE).edit().clear().commit()
        Toast.makeText(context, R.string.toast_exit_account_success, Toast.LENGTH_SHORT).show()
    }

    fun getSToken(): String? = getLoginInfo()?.sToken

    fun getCookie(): String? = getLoginInfo()?.cookie

    fun getUid(): String? = getLoginInfo()?.uid

    fun getBduss(): String? = getLoginInfo()?.bduss

    @JvmStatic
    fun getBdussCookie(): String? = getBduss()?.let { AccountCookieUtil.getBdussCookie(it) }

    @JvmStatic
    fun getBdussCookie(bduss: String): String = AccountCookieUtil.getBdussCookie(bduss)
}
