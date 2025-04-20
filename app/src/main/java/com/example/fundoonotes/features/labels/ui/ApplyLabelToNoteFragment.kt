package com.example.fundoonotes.features.labels.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.util.interfaces.LabelActionHandler
import com.example.fundoonotes.common.util.interfaces.LabelSelectionListener
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentApplyLabelToNoteBinding
import com.example.fundoonotes.features.labels.util.EditLabelAdapter
import com.example.fundoonotes.features.labels.util.LabelAdapterMode
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ApplyLabelToNoteFragment : Fragment() {

    private var _binding: FragmentApplyLabelToNoteBinding? = null
    private val binding get() = _binding!!

    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    private var adapter: EditLabelAdapter? = null
    private val selectedLabels = mutableSetOf<Label>()

    private var labelSelectionListener: LabelSelectionListener? = null
    private var labelHandler: LabelActionHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        labelSelectionListener = parentFragment as? LabelSelectionListener
            ?: activity as? LabelSelectionListener

        labelHandler = parentFragment as? LabelActionHandler
            ?: activity as? LabelActionHandler
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApplyLabelToNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.labelRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        labelsViewModel.fetchLabels()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    labelsViewModel.labelList.collectLatest { labels ->
                        Log.d("ApplyLabelFragment", "Labels received: ${labels.size}")
                        adapter?.submitList(labels)
                    }
                }

                launch {
                    selectionSharedViewModel.selectedNotes.collectLatest { selectedNotes ->
                        Log.d("ApplyLabelFragment", "Selected Notes: ${selectedNotes.map { it.id }}")

                        val labelSets = selectedNotes.map { it.labels.toSet() }
                        val commonLabels = if (labelSets.isNotEmpty()) {
                            labelSets.reduce { acc, set -> acc.intersect(set) }
                        } else emptySet()

                        Log.d("ApplyLabelFragment", "Computed common labels: $commonLabels")

                        if (adapter == null) {
                            adapter = EditLabelAdapter(
                                mode = LabelAdapterMode.SELECT,
                                preSelectedLabels = commonLabels,
                                onSelect = { label, isChecked ->
                                    if (isChecked) selectedLabels.add(label)
                                    else selectedLabels.remove(label)

                                    labelSelectionListener?.onLabelListUpdated(selectedLabels.toList())
                                    labelHandler?.onLabelToggledForNotes(
                                        label,
                                        isChecked,
                                        selectedNotes.map { it.id })
                                }
                            )
                            binding.labelRecyclerView.adapter = adapter
                        } else {
                            adapter?.updatePreSelectedLabels(commonLabels)
                        }
                    }
                }
            }
        }

        binding.backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ApplyLabelToNoteFragment = ApplyLabelToNoteFragment()
    }
}