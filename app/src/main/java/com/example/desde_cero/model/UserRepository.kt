package com.example.desde_cero.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class UserRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("currency_users_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERS = "registered_users"
        private const val KEY_CURRENT_USER_ID = "current_logged_in_user_id"
    }

    init {
        // Seed initial admin and user if repository is fresh
        if (getAllUsers().isEmpty()) {
            val initialUsers = listOf(
                User(
                    id = "admin_01",
                    fullName = "Auditor Financiero (Admin)",
                    email = "admin@divisas.com",
                    password = "admin",
                    role = UserRole.ADMIN
                ),
                User(
                    id = "user_01",
                    fullName = "Carlos Comprador (Trader)",
                    email = "usuario@divisas.com",
                    password = "user",
                    role = UserRole.USER
                )
            )
            saveUsers(initialUsers)
        }
    }

    fun register(fullName: String, email: String, password: String, role: UserRole): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val users = getAllUsers().toMutableList()

        if (users.any { it.email.lowercase() == trimmedEmail }) {
            return Result.failure(IllegalArgumentException("El correo ya se encuentra registrado."))
        }

        val newUser = User(
            id = "user_${System.currentTimeMillis()}",
            fullName = fullName.trim(),
            email = trimmedEmail,
            password = password,
            role = role
        )

        users.add(newUser)
        saveUsers(users)
        return Result.success(newUser)
    }

    fun login(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val users = getAllUsers()
        val foundUser = users.firstOrNull { it.email.lowercase() == trimmedEmail }

        return if (foundUser != null) {
            if (foundUser.password == password) {
                prefs.edit().putString(KEY_CURRENT_USER_ID, foundUser.id).apply()
                Result.success(foundUser)
            } else {
                Result.failure(IllegalArgumentException("Contraseña incorrecta."))
            }
        } else {
            Result.failure(IllegalArgumentException("No existe ninguna cuenta con este correo."))
        }
    }

    fun getCurrentUser(): User? {
        val currentId = prefs.getString(KEY_CURRENT_USER_ID, null) ?: return null
        return getAllUsers().firstOrNull { it.id == currentId }
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    fun getAllUsers(): List<User> {
        val jsonString = prefs.getString(KEY_USERS, null) ?: return emptyList()
        val list = mutableListOf<User>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val roleStr = obj.optString("role", UserRole.USER.name)
                val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.USER }
                list.add(
                    User(
                        id = obj.getString("id"),
                        fullName = obj.getString("fullName"),
                        email = obj.getString("email"),
                        password = obj.getString("password"),
                        role = role
                    )
                )
            }
        } catch (_: Exception) {
            // Error parsing
        }
        return list
    }

    private fun saveUsers(users: List<User>) {
        val jsonArray = JSONArray()
        for (user in users) {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("fullName", user.fullName)
                put("email", user.email)
                put("password", user.password)
                put("role", user.role.name)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_USERS, jsonArray.toString()).apply()
    }
}
