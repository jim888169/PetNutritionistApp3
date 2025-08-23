package com.example.petnutritionistapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val backBtn = findViewById<Button>(R.id.backBtn)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirm = confirmPasswordInput.text.toString()

            if (email.isBlank() || password.isBlank() || confirm.isBlank()) {
                Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show()
            } else if (password != confirm) {
                Toast.makeText(this, "兩次密碼不一致", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        // 註冊成功後，自動新增到 Firestore
                        val userData = hashMapOf("email" to email)

                        db.collection("users")
                            .document(email)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "✅ 註冊成功！", Toast.LENGTH_SHORT).show()
                                finish() // 返回登入畫面
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "⚠️ 資料寫入失敗：${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "❌ 註冊失敗：${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        backBtn.setOnClickListener {
            finish() // 回到 LoginActivity
        }
    }
}
