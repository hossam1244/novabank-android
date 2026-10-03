package com.novabank.app.di

import android.content.Context
import androidx.room.Room
import com.novabank.app.BuildConfig
import com.novabank.app.core.data.AccountDao
import com.novabank.app.core.data.NovaBankDatabase
import com.novabank.app.core.network.AuthInterceptor
import com.novabank.app.core.network.NovaBankApi
import com.novabank.app.core.storage.EncryptedTokenStore
import com.novabank.app.core.storage.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun tokenStore(@ApplicationContext context: Context): TokenStore =
        EncryptedTokenStore(context)

    @Provides
    @Singleton
    fun okHttp(tokenStore: TokenStore): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore, BuildConfig.API_BASE_URL))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun retrofitApi(client: OkHttpClient): NovaBankApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NovaBankApi::class.java)
    }

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): NovaBankDatabase =
        Room.databaseBuilder(context, NovaBankDatabase::class.java, "novabank.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun accountDao(db: NovaBankDatabase): AccountDao = db.accountDao()
}
