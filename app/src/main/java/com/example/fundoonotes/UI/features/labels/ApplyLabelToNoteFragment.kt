package com.example.fundoonotes.UI.features.labels

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.LabelSelectionListener
import com.example.fundoonotes.databinding.FragmentApplyLabelToNoteBinding
import kotlinx.coroutines.launch

class ApplyLabelToNoteFragment : Fragment() {

    private var _binding: FragmentApplyLabelToNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LabelsViewModel
    private lateinit var adapter: EditLabelAdapter

    private var selectedNoteIds: List<String> = emptyList()
    private val selectedLabels = mutableSetOf<Label>()

    private var labelSelectionListener: LabelSelectionListener? = null
    private var labelHandler: LabelActionHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedNoteIds = arguments?.getStringArrayList(ARG_NOTE_IDS)?.toList() ?: emptyList()

        labelSelectionListener = parentFragment as? LabelSelectionListener
            ?: activity as? LabelSelectionListener

        labelHandler = parentFragment as? LabelActionHandler
            ?: activity as? LabelActionHandler
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[LabelsViewModel::class.java]
        _binding = FragmentApplyLabelToNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch labels so the list isn't empty
        viewModel.fetchLabels()

        // Get the NotesViewModel to access all notes.
        val notesViewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        // Get the complete list of notes.
        val allNotes = notesViewModel.notesFlow.value
        // Filter out only the selected notes using their IDs.
        val selectedNotes = allNotes.filter { selectedNoteIds.contains(it.id) }
        // Compute the intersection of labels for all selected notes.
        // If only one note is selected, the intersection is that note's labels.
        val commonLabels: Set<String> = if (selectedNotes.isNotEmpty()) {
            selectedNotes.map { it.labels.toSet() }
                .reduce { acc, set -> acc.intersect(set) }
        } else {
            emptySet()
        }
        Log.d("ApplyLabelFragment", "Common labels: $commonLabels")

        // Initialize the adapter in SELECT mode.
        adapter = EditLabelAdapter(
            mode = LabelAdapterMode.SELECT,
            preSelectedLabels = commonLabels,
            onSelect = { label, isChecked ->
                if (isChecked) selectedLabels.add(label) else selectedLabels.remove(label)
                labelSelectionListener?.onLabelListUpdated(selectedLabels.toList())
                labelHandler?.onLabelToggledForNotes(label, isChecked, selectedNoteIds)
            }
        )

        binding.labelRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.labelRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect labels from ViewModel
                viewModel.labelList.collect { labels ->

                    Log.d("ApplyLabelFragment", "Labels received: ${labels.size}")
                    // Pre-check labels that are already applied to all selected notes
                    if (selectedNoteIds.isNotEmpty()) {
                        // You would need to fetch notes to check which labels they already have
                        // For simplicity, we're just submitting the list for now
                        adapter.submitList(labels)
                    } else {
                        adapter.submitList(labels)
                    }
                }
            }
        }

        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NOTE_IDS = "note_ids"

        fun newInstance(noteIds: List<String>): ApplyLabelToNoteFragment {
            return ApplyLabelToNoteFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_NOTE_IDS, ArrayList(noteIds))
                }
            }
        }
    }
}
