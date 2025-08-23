package com.example.petnutritionistapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔁 如果使用者已登入過，自動跳轉
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPrefs.getString("user_email", null)
        if (savedEmail != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginBtn = findViewById(R.id.loginButton)
        registerBtn = findViewById(R.id.registerButton)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "請填寫帳號與密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔐 Firebase Auth 登入驗證
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // ✅ 儲存帳號到 SharedPreferences
                    sharedPrefs.edit().putString("user_email", email).apply()

                    Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "此帳號尚未註冊或密碼錯誤", Toast.LENGTH_LONG).show()
                }
        }

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}