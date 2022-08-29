package ac.android.sdk.di

import ac.android.sdk.SP_TABLE
import ac.android.sdk.model.acBackend.RemoteRepo
import ac.android.sdk.model.acBackend.RemoteRepoImpl
import ac.android.sdk.model.config.ConfigRepo
import ac.android.sdk.model.config.ConfigRepoImpl
import ac.android.sdk.model.iap.IAPRepo
import ac.android.sdk.model.iap.IAPRepoImpl
import ac.android.sdk.model.paywall.UserRepo
import ac.android.sdk.model.paywall.UserRepoImpl
import ac.android.sdk.model.repositories.impl.*
import ac.android.sdk.model.repositories.interfaces.*
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

internal object RepoModule {

    private val sp: SharedPreferences by lazy {
        provideSP(SDKModule.app)
    }
    private val localRepo: LocalRepo by lazy {
        provideLocalRepo(sp)
    }
    val remoteRepo: RemoteRepo by lazy {
        provideRemoteRepo()
    }
    val remoteConfigRepo: RemoteConfigRepo by lazy {
        provideRemoteConfigRepo(RemoteConfigModule.remoteConfig, configRepo)
    }
    val configRepo: ConfigRepo by lazy {
        provideConfigRepo()
    }
    val helper by lazy { provideHelperRepo() }

    val iapRepo: IAPRepo by lazy {
        provideIAPRepo(SDKModule.app)
    }

    val userRepo: UserRepo by lazy {
        provideUserRepo(localRepo)
    }

    private fun provideSP(context: Context) = context.getSharedPreferences(SP_TABLE, Context.MODE_PRIVATE)

    private fun provideLocalRepo(sp: SharedPreferences): LocalRepo = LocalRepoImpl(sp)

    private fun provideRemoteRepo(): RemoteRepo = RemoteRepoImpl()

    private fun provideRemoteConfigRepo(
        remoteConfig: FirebaseRemoteConfig,
        configRepo: ConfigRepo
    ): RemoteConfigRepo = RemoteConfigRepoImpl(remoteConfig, configRepo)

    private fun provideConfigRepo(): ConfigRepo = ConfigRepoImpl()

    private fun provideHelperRepo(): HelperRepo = HelperRepoImpl()

    private fun provideIAPRepo(context: Context): IAPRepo = IAPRepoImpl(context)

    private fun provideUserRepo(localRepo: LocalRepo): UserRepo = UserRepoImpl(localRepo)
}
