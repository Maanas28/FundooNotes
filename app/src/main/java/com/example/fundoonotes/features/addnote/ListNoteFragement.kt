package com.example.fundoonotes.features.addnote

    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageButton
    import android.widget.TextView
    import androidx.fragment.app.Fragment
    import com.example.fundoonotes.R

    class ListNoteFragement(private val label: String) : Fragment() {


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val view = inflater.inflate(R.layout.fragment_list_note_fragement, container, false)
            view.findViewById<TextView>(R.id.tempText).text = label
            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)


            view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

    }