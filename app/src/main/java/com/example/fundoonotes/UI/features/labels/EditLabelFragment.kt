package com.example.fundoonotes.UI.features.labels

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fundoonotes.databinding.FragmentEditLabelBinding
import kotlinx.coroutines.launch

class EditLabelFragment : Fragment() {

    private var _binding: FragmentEditLabelBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LabelsViewModel
    private lateinit var adapter: EditLabelAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[LabelsViewModel::class.java]
        _binding = FragmentEditLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter early so it's ready when click listeners are set
        adapter = EditLabelAdapter(
            mode = LabelAdapterMode.EDIT,
            onDelete = { label -> viewModel.deleteLabel(label) },
            onRename = { oldLabel, newName ->
                val newLabel = oldLabel.copy(name = newName)
                viewModel.updateLabel(oldLabel, newLabel)
            }
        )

        binding.recyclerLabels.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLabels.adapter = adapter

        viewModel.fetchLabels()

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
                viewModel.addLabel(labelName)
                binding.labelInputField.setText("")
                binding.editableAddRow.visibility = View.GONE
                binding.staticAddRow.visibility = View.VISIBLE

                // Reset edit state so new label doesn't appear in edit mode
                adapter.resetEditState()
            } else {
                Toast.makeText(requireContext(), "Label name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.labelList.collect { labels ->
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
