package com.example.petnutritionistapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseApp

class DogInputFragment : Fragment() {

    private lateinit var spinnerSize: Spinner
    private lateinit var spinnerBreed: Spinner
    private lateinit var btnStartPhoto: Button

    private val sizeMap = mapOf(
        "迷你犬" to listOf("吉娃娃", "博美", "馬爾濟斯", "約克夏"),
        "小型犬" to listOf("雪納瑞", "米克斯", "貴賓犬", "臘腸犬", "法國鬥牛犬", "比熊犬", "西施犬", "狐狸犬", "巴哥犬", "西高地白犬"),
        "中型犬" to listOf("柴犬", "邊境牧羊犬", "可卡犬"),
        "大型犬" to listOf("黃金獵犬", "哈士奇", "拉布拉多", "沙皮犬", "杜賓犬", "大丹犬")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FirebaseApp.initializeApp(requireContext())
        val view = inflater.inflate(R.layout.fragment_dog_input, container, false)

        spinnerSize = view.findViewById(R.id.spinnerSize)
        spinnerBreed = view.findViewById(R.id.spinnerBreed)
        btnStartPhoto = view.findViewById(R.id.btnNext)

        val sizeList = sizeMap.keys.toList()
        spinnerSize.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sizeList)

        spinnerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSize = sizeList[position]
                val breeds = sizeMap[selectedSize] ?: listOf()
                spinnerBreed.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, breeds)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnStartPhoto.setOnClickListener {
            val size = spinnerSize.selectedItem?.toString() ?: ""
            val breed = spinnerBreed.selectedItem?.toString() ?: ""

            if (size.isEmpty() || breed.isEmpty()) {
                Toast.makeText(requireContext(), "請選擇體型與品種", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 跳轉到拍照與觸感頁面
            val intent = Intent(requireContext(), PhotoAndTouchInputActivity::class.java)
            intent.putExtra("DOG_SIZE", size)
            intent.putExtra("DOG_BREED", breed)
            startActivity(intent)
        }

        return view
    }
}
