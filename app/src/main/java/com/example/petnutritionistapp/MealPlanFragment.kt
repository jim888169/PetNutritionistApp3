package com.example.petnutritionistapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class MealPlanFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvMealSuggestion: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meal_plan, container, false)

        tvMealSuggestion = view.findViewById(R.id.tvMealSuggestion)
        db = FirebaseFirestore.getInstance()

        val breed = arguments?.getString("DOG_BREED") ?: ""
        val bcsIndex = (arguments?.getInt("BCS_INDEX") ?: -1) + 1  // +1 對應 1~9

        if (breed.isNotEmpty() && bcsIndex in 1..9) {
            val docId = "${breed}_$bcsIndex"

            db.collection("mealplans").document(docId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val suggestion = document.getString("suggestion")
                        tvMealSuggestion.text = suggestion ?: "沒有建議內容。"
                    } else {
                        tvMealSuggestion.text = "找不到對應的配餐建議文件。"
                    }
                }
                .addOnFailureListener { e ->
                    tvMealSuggestion.text = "讀取資料失敗：${e.message}"
                    Log.e("MealPlanFragment", "Firebase 錯誤", e)
                }
        } else {
            tvMealSuggestion.text = "未提供有效的狗狗品種或 BCS。"
        }

        return view
    }
}
