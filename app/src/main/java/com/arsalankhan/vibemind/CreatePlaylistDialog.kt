package com.arsalankhan.vibemind

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.arsalankhan.vibemind.databinding.DialogCreatePlaylistBinding

// CreatePlaylistDialog.kt
class CreatePlaylistDialog : DialogFragment() {
    private var _binding: DialogCreatePlaylistBinding? = null
    private val binding get() = _binding!!

    var onCreateListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreatePlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog background
        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnCreate.setOnClickListener {
            val name = binding.etPlaylistName.text.toString().trim()
            if (name.isNotEmpty()) {
                onCreateListener?.invoke(name)
                dismiss()
            } else {
                binding.etPlaylistName.error = "Please enter a name"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}