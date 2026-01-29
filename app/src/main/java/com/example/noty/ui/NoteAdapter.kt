package com.example.noty.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.noty.data.Note
import com.example.noty.data.NoteType
import com.example.noty.databinding.ItemNoteBinding

class NoteAdapter(private val onDeleteClick: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.textTitle.text = note.title
            binding.textTimestamp.text = DATE_FORMAT.format(Date(note.timestamp))

            if (!note.description.isNullOrEmpty()) {
                binding.textDescription.text = note.description
                binding.textDescription.visibility = android.view.View.VISIBLE
            } else {
                binding.textDescription.visibility = android.view.View.GONE
            }

            binding.buttonDelete.setOnClickListener {
                 onDeleteClick(note)
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
