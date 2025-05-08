package com.fillwordmax.auth

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
} 