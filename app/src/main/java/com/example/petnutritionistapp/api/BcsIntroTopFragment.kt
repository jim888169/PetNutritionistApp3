package com.example.petnutritionistapp.api

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.petnutritionistapp.R

class BcsIntroTopFragment : Fragment(R.layout.fragment_bcs_intro_top) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnNext)?.setOnClickListener {
            findNavController().navigate(R.id.action_bcsIntroTop_to_bcsIntroBottom)
        }
    }
}
