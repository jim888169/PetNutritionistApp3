package com.example.petnutritionistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class DiseaseFragment : Fragment() {

    private lateinit var tvDiseaseTitle: TextView
    private lateinit var tvPrevention: TextView
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_disease, container, false)

        tvDiseaseTitle = view.findViewById(R.id.tvDiseaseTitle)
        tvPrevention = view.findViewById(R.id.tvPrevention)
        db = FirebaseFirestore.getInstance()

        val selectedBreed = arguments?.getString("DOG_BREED")

        if (selectedBreed != null) {
            db.collection("commonDiseases")
                .document(selectedBreed)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val disease = document.getString("diseases") ?: "無資料"
                        val prevention = document.getString("prevention") ?: "無資料"
                        tvDiseaseTitle.text = "疾病：$disease"
                        tvPrevention.text = "預防建議：$prevention"
                    } else {
                        Toast.makeText(requireContext(), "找不到該品種疾病資料", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "讀取資料失敗：${it.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "未提供品種資料", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}