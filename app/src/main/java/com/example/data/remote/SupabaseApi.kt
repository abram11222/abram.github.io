package com.example.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Single Retrofit interface that talks to Supabase:
 *  - PostgREST (data tables) via generic @Url calls.
 *  - Supabase Auth REST endpoints.
 *
 *  The `apiKey` header is the project anon key; `Authorization` is the user
 *  JWT ("Bearer <access_token>") when present.
 */
interface SupabaseApi {

    // ---------- PostgREST (generic, all tables) ----------
    @GET
    suspend fun getRows(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String?
    ): Response<ResponseBody>

    @POST
    suspend fun postRows(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String?,
        @Header("Prefer") prefer: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @PATCH
    suspend fun patchRow(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String?,
        @Header("Prefer") prefer: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteRow(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String?
    ): Response<ResponseBody>

    @PUT
    suspend fun uploadStorageObject(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String,
        @Header("x-upsert") upsert: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    // ---------- Supabase Auth REST ----------
    // Content-Type comes from the RequestBody's MediaType (application/json) in AuthManager.
    @POST
    suspend fun authSignUp(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun authRefresh(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun authSignIn(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun authSignOut(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String
    ): Response<ResponseBody>

    @GET
    suspend fun authGetUser(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearer: String
    ): Response<ResponseBody>
}
