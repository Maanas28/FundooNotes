package com.example.fundoonotes.UI.util

import androidx.fragment.app.FragmentManager

interface LabelFragmentHost {
    fun getLabelFragmentContainerId(): Int
    fun getLabelFragmentManager(): FragmentManager
}

