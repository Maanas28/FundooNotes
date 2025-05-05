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
import com.example.fundoonotes.common.util.enums.LabelAdapterMode
import com.example.fundoonotes.databinding.FragmentEditLabelBinding
import com.example.fundoonotes.features.labels.util.EditLabelAdapter
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditLabelFragment : Fragment() {

    private var _binding: FragmentEditLabelBinding? = null
    private val binding get() = _binding!!

    private val labelsViewModel by activityViewModels<LabelsViewModel>()

    private val adapter by lazy {
        EditLabelAdapter(
            mode = LabelAdapterMode.EDIT,
            onDelete = { label -> labelsViewModel.deleteLabel(label) },
            onRename = { oldLabel, newName ->
                val newLabel = oldLabel.copy(name = newName)
                labelsViewModel.updateLabel(oldLabel, newLabel)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeLabels()

        labelsViewModel.fetchLabels()
    }

    private fun setupRecyclerView() {
        binding.recyclerLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLabels.adapter = adapter
    }

    private fun setupListeners() {
        binding.createNewLabelEt.setOnClickListener {
            binding.staticAddRow.visibility = View.GONE
            binding.editableAddRow.visibility = View.VISIBLE
            binding.labelInputField.requestFocus()
        }

        binding.btnCancelLabel.setOnClickListener {
            binding.labelInputField.setText("")
            binding.editableAddRow.visibility = View.GONE
            binding.staticAddRow.visibility = View.VISIBLE
        }

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

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun observeLabels() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelsViewModel.labelList.collect { labels ->
                    Log.d("LabelFragmentDebug", "Collected labels: $labels")
                    adapter.submitList(labels)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
