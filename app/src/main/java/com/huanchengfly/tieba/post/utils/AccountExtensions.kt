package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.Account

/**
 * Creates an encrypted deep copy of the Account entity, protecting sensitive tokens (bduss, sToken, cookie).
 */
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

/**
 * In-place decrypts sensitive tokens (bduss, sToken, cookie) of the Account entity.
 */
fun Account.decryptSensitiveFields(): Account {
    this.bduss = AccountSecurityUtil.decrypt(this.bduss)
    this.sToken = AccountSecurityUtil.decrypt(this.sToken)
    this.cookie = AccountSecurityUtil.decrypt(this.cookie)
    return this
}
