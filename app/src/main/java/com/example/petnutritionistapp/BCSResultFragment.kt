package com.example.petnutritionistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class BCSResultFragment : Fragment() {

    private lateinit var ivDogImage: ImageView
    private lateinit var tvResult: TextView
    private lateinit var tvSuggestion: TextView
    private lateinit var btnMeal: Button
    private lateinit var btnDisease: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bcs_result, container, false)

        ivDogImage = view.findViewById(R.id.ivDogImage)
        tvResult = view.findViewById(R.id.tvResult)
        tvSuggestion = view.findViewById(R.id.tvSuggestion)
        btnMeal = view.findViewById(R.id.btnMeal)
        btnDisease = view.findViewById(R.id.btnDisease)
        db = FirebaseFirestore.getInstance()

        val bcsIndex = arguments?.getInt("FINAL_BCS_SCORE", -1) ?: -1
        val breedName = arguments?.getString("DOG_BREED") ?: "未知品種"

        val resultText: String
        val suggestionText: String

        when (bcsIndex) {
            0, 1 -> {
                resultText = "您的狗狗為：超瘦（BCS 1-2 分）"
                suggestionText = "建議補充足夠熱量與蛋白質，並持續觀察體態變化。"
            }
            2, 3 -> {
                resultText = "您的狗狗為：過瘦（BCS 3-4 分）"
                suggestionText = "建議增加飲食量與營養密度，提升體態。"
            }
            4 -> {
                resultText = "您的狗狗為：適中（BCS 5 分）"
                suggestionText = "目前體態良好，請持續維持良好飲食與運動。"
            }
            5, 6 -> {
                resultText = "您的狗狗為：過重（BCS 6-7 分）"
                suggestionText = "建議減少熱量攝取並增加運動時間。"
            }
            7, 8 -> {
                resultText = "您的狗狗為：超重（BCS 8-9 分）"
                suggestionText = "請積極進行體重控制，避免引發相關疾病。"
            }
            else -> {
                resultText = "無法判斷狗狗體態"
                suggestionText = "請重新進行體態分析。"
            }
        }

        tvResult.text = resultText
        tvSuggestion.text = suggestionText

        // 讀取圖片
        if (breedName.isNotEmpty()) {
            db.collection("breedImages")
                .document(breedName)
                .get()
                .addOnSuccessListener { doc ->
                    val imageUrl = doc.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .into(ivDogImage)
                    } else {
                        Toast.makeText(requireContext(), "找不到圖片連結", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "讀取圖片失敗", Toast.LENGTH_SHORT).show()
                }
        }

        btnMeal.setOnClickListener {
            val fragment = MealPlanFragment().apply {
                arguments = Bundle().apply {
                    putString("DOG_BREED", breedName)
                    putInt("BCS_INDEX", bcsIndex)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        btnDisease.setOnClickListener {
            val fragment = DiseaseFragment().apply {
                arguments = Bundle().apply {
                    putString("DOG_BREED", breedName)
                }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        return view
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
