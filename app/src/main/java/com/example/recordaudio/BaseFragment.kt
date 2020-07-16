package com.example.recordaudio

import android.os.Bundle
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    companion object {

        private val TAG: String = "TAG -- ${BaseFragment::class.java.simpleName}"

        fun getFragmentInstance(fragment: Fragment, data: Bundle): Fragment {
            fragment.arguments = data
            return fragment
        }

        fun getFragmentInstance(
            fragment: Fragment, data: Bundle? = null, targetFragment: Fragment,
            requestCode: Int = 0
        ): Fragment {
            try {
                data?.apply { fragment.arguments = this }
                fragment.setTargetFragment(targetFragment, requestCode)
            } catch (e: Exception) {
                println("$TAG -- ${e.message}")
            }
            return fragment
        }
    }
}