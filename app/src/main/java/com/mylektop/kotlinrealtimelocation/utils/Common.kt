package com.mylektop.kotlinrealtimelocation.utils

import com.mylektop.kotlinrealtimelocation.model.User

/**
 * Created by MyLektop on 19/04/2019.
 */
object Common {
    lateinit var loggedUser: User

    const val USER_INFORMATION: String = "UserInformation"
    const val TOKENS: String = "Tokens"
    const val USER_UID_SAVE_KEY: String = "SAVE_KEY"
}