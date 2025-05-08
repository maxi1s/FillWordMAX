package com.example.fillwordsmax.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    fun initGoogleSignIn(clientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    suspend fun signIn(email: String, password: String): AuthResult = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        AuthResult.Success(result.user!!)
    } catch (e: Exception) {
        when {
            e.message?.contains("no user record") == true -> AuthResult.Error("Аккаунт не найден")
            e.message?.contains("password is invalid") == true -> AuthResult.Error("Неверный пароль")
            e.message?.contains("operation is not allowed") == true -> 
                AuthResult.Error("Вход по email/паролю отключен. Пожалуйста, используйте вход через Google")
            else -> AuthResult.Error("Ошибка входа: ${e.message}")
        }
    }

    suspend fun signUp(email: String, password: String): AuthResult = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        AuthResult.Success(result.user!!)
    } catch (e: Exception) {
        when {
            e.message?.contains("email address is already in use") == true -> 
                AuthResult.Error("Этот email уже зарегистрирован")
            e.message?.contains("badly formatted") == true -> 
                AuthResult.Error("Неверный формат email")
            e.message?.contains("password is too weak") == true -> 
                AuthResult.Error("Пароль слишком простой")
            e.message?.contains("operation is not allowed") == true -> 
                AuthResult.Error("Регистрация по email/паролю отключена. Пожалуйста, используйте вход через Google")
            else -> AuthResult.Error("Ошибка регистрации: ${e.message}")
        }
    }

    suspend fun signInWithGoogle(intent: Intent): AuthResult = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(intent).await()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()
        AuthResult.Success(result.user!!)
    } catch (e: ApiException) {
        AuthResult.Error("Ошибка входа через Google: ${e.message}")
    } catch (e: Exception) {
        AuthResult.Error("Ошибка входа через Google: ${e.message}")
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
} 