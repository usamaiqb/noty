package com.example.noty.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.noty.data.Task
import com.example.noty.data.TaskType
import com.example.noty.databinding.ItemTaskBinding

class TaskAdapter(private val onDeleteClick: (Task) -> Unit) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

        fun bind(task: Task) {
            binding.textTitle.text = task.title
            binding.textTimestamp.text = dateFormat.format(Date(task.timestamp))
            
            if (!task.description.isNullOrEmpty()) {
                binding.textDescription.text = task.description
                binding.textDescription.visibility = android.view.View.VISIBLE
            } else {
                binding.textDescription.visibility = android.view.View.GONE
            }

            binding.buttonDelete.setOnClickListener {
                 onDeleteClick(task)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
