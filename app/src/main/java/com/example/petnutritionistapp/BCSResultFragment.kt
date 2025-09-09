package com.example.petnutritionistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

class BCSResultFragment : Fragment() {

    private lateinit var tvResult: TextView
    private lateinit var tvSuggestion: TextView
    private lateinit var ivDogImage: ImageView
    private lateinit var btnMeal: Button
    private lateinit var btnDisease: Button
    private lateinit var db: FirebaseFirestore

    private var finalScore: Int = -1
    private var breedName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            finalScore = it.getInt("FINAL_BCS_SCORE", -1)
            breedName = it.getString("DOG_BREED")
        }
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bcs_result, container, false)

        tvResult = view.findViewById(R.id.tvResult)
        tvSuggestion = view.findViewById(R.id.tvSuggestion)
        ivDogImage = view.findViewById(R.id.ivDogImage)
        btnMeal = view.findViewById(R.id.btnMeal)
        btnDisease = view.findViewById(R.id.btnDisease)

        // 顯示結果
        showResult()

        // 🔽 點擊 MealPlan → 跳轉到 MealPlanFragment
        btnMeal.setOnClickListener {
            val fragment = MealPlanFragment().apply {
                arguments = Bundle().apply {
                    putString("DOG_BREED", breedName)
                    putInt("BCS_INDEX", finalScore)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        // 🔽 點擊 Disease → 跳轉到 DiseaseFragment
        btnDisease.setOnClickListener {
            val fragment = DiseaseFragment().apply {
                arguments = Bundle().apply {
                    putString("DOG_BREED", breedName)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun showResult() {
        // 顯示分數與狀態
        val resultText = when (finalScore) {
            1, 2 -> "超瘦"
            3, 4 -> "過瘦"
            5 -> "適中"
            6, 7 -> "過重"
            8, 9 -> "超重"
            else -> "未知"
        }
        tvResult.text = "BCS 評分：$finalScore ($resultText)"

        // 從 Firebase 取圖片
        breedName?.let { breed ->
            db.collection("dogBreeds").document(breed)
                .get()
                .addOnSuccessListener { doc ->
                    val imageUrl = doc.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .into(ivDogImage)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "讀取圖片失敗", Toast.LENGTH_SHORT).show()
                }
        }

        // 建議文字
        tvSuggestion.text = when (resultText) {
            "超瘦" -> "需要增加營養與熱量。"
            "過瘦" -> "可以適度增加飲食，注意營養均衡。"
            "適中" -> "保持現在的飲食習慣，繼續維持健康！"
            "過重" -> "建議控制飲食，增加運動量。"
            "超重" -> "建議減重，並與獸醫師討論飲食控制。"
            else -> "無法判斷，請重新輸入資料。"
        }
    }

    companion object {
        fun newInstance(score: Int, breed: String): BCSResultFragment {
            val fragment = BCSResultFragment()
            val args = Bundle()
            args.putInt("FINAL_BCS_SCORE", score)
            args.putString("DOG_BREED", breed)
            fragment.arguments = args
            return fragment
        }
    }
}
