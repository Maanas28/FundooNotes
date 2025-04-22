package com.example.fundoonotes.features.labels.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fundoonotes.databinding.FragmentEditLabelBinding
import com.example.fundoonotes.features.labels.util.EditLabelAdapter
import com.example.fundoonotes.features.labels.util.LabelAdapterMode
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditLabelFragment : Fragment() {

    private var _binding: FragmentEditLabelBinding? = null
    private val binding get() = _binding!!

    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private lateinit var adapter: EditLabelAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter early so it's ready when click listeners are set
        adapter = EditLabelAdapter(
            mode = LabelAdapterMode.EDIT,
            onDelete = { label -> labelsViewModel.deleteLabel(label) },
            onRename = { oldLabel, newName ->
                val newLabel = oldLabel.copy(name = newName)
                labelsViewModel.updateLabel(oldLabel, newLabel)
            }
        )

        binding.recyclerLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLabels.adapter = adapter

        labelsViewModel.fetchLabels()

        // Show input row when "Create new label" is clicked
        binding.createNewLabelEt.setOnClickListener {
            binding.staticAddRow.visibility = View.GONE
            binding.editableAddRow.visibility = View.VISIBLE
            binding.labelInputField.requestFocus()
        }

        // Cancel input
        binding.btnCancelLabel.setOnClickListener {
            binding.labelInputField.setText("")
            binding.editableAddRow.visibility = View.GONE
            binding.staticAddRow.visibility = View.VISIBLE
        }

        // Confirm and save label
        binding.btnConfirmLabel.setOnClickListener {
            val labelName = binding.labelInputField.text.toString().trim()
            if (labelName.isNotEmpty()) {
                labelsViewModel.addLabel(
                    name = labelName,
                    onSuccess = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.labelInputField.setText("")
                            binding.editableAddRow.visibility = View.GONE
                            binding.staticAddRow.visibility = View.VISIBLE
                            adapter.resetEditState()
                        }
                    },
                    onFailure = { exception ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), exception.message ?: "Error adding label", Toast.LENGTH_SHORT).show()
                        }
                    }

                )
            } else {
                Toast.makeText(requireContext(), "Label name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelsViewModel.labelList.collect { labels ->
                    Log.d("LabelFragmentDebug", "Collected labels: $labels")
                    adapter.submitList(labels)
                }
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}