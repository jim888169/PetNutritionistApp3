package com.example.petnutritionistapp.api

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petnutritionistapp.R

class BcsIntroBottomFragment : Fragment(R.layout.fragment_bcs_intro_bottom) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnPrev)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
