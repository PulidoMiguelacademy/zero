package com.example.desde_cero.model

enum class UserRole {
    USER,   // Usuario que compra y vende divisas
    ADMIN   // Administrador que audita transacciones
}

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val password: String,
    val role: UserRole = UserRole.USER,
    val cedula: String = "",
    val isCedulaVerified: Boolean = false
)
