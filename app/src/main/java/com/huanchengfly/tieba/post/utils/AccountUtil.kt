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
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.litepal.LitePal
import org.litepal.LitePal.findAll
import org.litepal.LitePal.where
import org.litepal.extension.findAllAsync
import org.litepal.extension.findFirst
import java.util.UUID

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

    private fun updateAccount(
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

    fun fetchAccountFlow(account: Account = getLoginInfo()!!): Flow<Account> {
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
                getAccountInfoByUid(loginBean.user.id)?.apply {
                    this.bduss = bduss
                    this.sToken = sToken
                    this.cookie = cookie ?: getBdussCookie(bduss)
                    updateAccount(this, loginBean)
                } ?: Account(
                    loginBean.user.id,
                    loginBean.user.name,
                    bduss,
                    loginBean.anti.tbs,
                    loginBean.user.portrait,
                    sToken,
                    cookie ?: getBdussCookie(bduss),
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
                                mutableAllAccountsState.value = list.map { it.decryptSensitiveFields() }
                            }
                    }
            }
            .flowOn(Dispatchers.IO)
    }

    fun parseCookie(cookie: String): Map<String, String> {
        return cookie
            .split(";")
            .map { it.trim().split("=") }
            .filter { it.size > 1 }
            .associate { it.first() to it.drop(1).joinToString("=") }
    }

    @JvmStatic
    fun updateLoginInfo(cookie: String): Boolean {
        val cookies = parseCookie(cookie).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
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

    fun getSToken(): String? {
        val account = getLoginInfo()
        return account?.sToken
    }

    fun getCookie(): String? {
        val account = getLoginInfo()
        return account?.cookie
    }

    fun getUid(): String? {
        val account = getLoginInfo()
        return account?.uid
    }

    fun getBduss(): String? {
        val account = getLoginInfo()
        return account?.bduss
    }

    @JvmStatic
    fun getBdussCookie(): String? {
        val bduss = getBduss()
        return if (bduss != null) {
            getBdussCookie(bduss)
        } else null
    }

    fun getBdussCookie(bduss: String): String {
        return "BDUSS=$bduss; Path=/; Max-Age=315360000; Domain=.baidu.com; Httponly; Secure"
    }
}

fun Account.toEncryptedCopy(): Account {
    return Account(
        uid = this.uid,
        name = this.name,
        bduss = AccountSecurityUtil.encrypt(this.bduss),
        tbs = this.tbs,
        portrait = this.portrait,
        sToken = AccountSecurityUtil.encrypt(this.sToken),
        cookie = AccountSecurityUtil.encrypt(this.cookie),
        nameShow = this.nameShow,
        intro = this.intro,
        sex = this.sex,
        fansNum = this.fansNum,
        postNum = this.postNum,
        threadNum = this.threadNum,
        concernNum = this.concernNum,
        tbAge = this.tbAge,
        age = this.age,
        birthdayShowStatus = this.birthdayShowStatus,
        birthdayTime = this.birthdayTime,
        constellation = this.constellation,
        tiebaUid = this.tiebaUid,
        loadSuccess = this.loadSuccess,
        uuid = this.uuid,
        zid = this.zid,
    )
}

fun Account.decryptSensitiveFields(): Account {
    this.bduss = AccountSecurityUtil.decrypt(this.bduss)
    this.sToken = AccountSecurityUtil.decrypt(this.sToken)
    this.cookie = AccountSecurityUtil.decrypt(this.cookie)
    return this
}
