package com.example.fundoonotes.common.util.interfaces

import androidx.fragment.app.FragmentManager

interface LabelFragmentHost {
    fun getLabelFragmentContainerId(): Int
    fun getLabelFragmentManager(): FragmentManager
}

