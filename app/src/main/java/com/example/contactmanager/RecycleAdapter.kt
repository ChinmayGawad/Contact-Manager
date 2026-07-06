package com.example.contactmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions

sealed class ContactListItem {
    data class Header(val letter: String) : ContactListItem()
    data class ContactItem(val contact: Contact) : ContactListItem()
}

class RecycleAdapter(
    private val onItemClickListener: OnItemClickListener,
    private val onEmptyStateChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayList: List<ContactListItem> = emptyList()
    private var contactsListFull: List<Contact> = emptyList()

    interface OnItemClickListener {
        fun onItemClick(contact: Contact)
    }

    fun updateList(newList: List<Contact>) {
        contactsListFull = ArrayList(newList)
        updateDisplayList(newList)
        onEmptyStateChanged(newList.isEmpty())
    }

    private fun updateDisplayList(newList: List<Contact>) {
        val newDisplayList = buildDisplayList(newList)
        val diffResult = DiffUtil.calculateDiff(
            ContactListDiffCallback(displayList, newDisplayList)
        )
        displayList = newDisplayList
        diffResult.dispatchUpdatesTo(this)
    }

    private fun buildDisplayList(contacts: List<Contact>): List<ContactListItem> {
        if (contacts.isEmpty()) return emptyList()
        val sorted = contacts.sortedBy { it.name.lowercase() }
        val result = mutableListOf<ContactListItem>()
        var currentLetter = ""
        for (contact in sorted) {
            val firstLetter = contact.name.take(1).uppercase()
            if (firstLetter != currentLetter) {
                currentLetter = firstLetter
                result.add(ContactListItem.Header(currentLetter))
            }
            result.add(ContactListItem.ContactItem(contact))
        }
        return result
    }

    fun filter(text: String) {
        val filtered = contactsListFull.filter {
            it.name.lowercase().contains(text.lowercase())
        }
        updateDisplayList(filtered)
        onEmptyStateChanged(filtered.isEmpty())
    }

    override fun getItemViewType(position: Int): Int =
        if (displayList[position] is ContactListItem.Header) VIEW_TYPE_HEADER else VIEW_TYPE_CONTACT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
            ContactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is ContactListItem.Header -> (holder as HeaderViewHolder).bind(item.letter)
            is ContactListItem.ContactItem -> {
                (holder as ContactViewHolder).bind(item.contact)
                holder.itemView.setOnClickListener { onItemClickListener.onItemClick(item.contact) }
            }
        }
    }

    override fun getItemCount(): Int = displayList.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetter: TextView = itemView.findViewById(R.id.tv_header_letter)
        fun bind(letter: String) {
            tvLetter.text = letter
        }
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvInitial: TextView = itemView.findViewById(R.id.tv_contact_initial)
        private val phone: TextView = itemView.findViewById(R.id.tv_contact_phone)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)

        fun bind(contact: Contact) {
            name.text = contact.name
            phone.text = contact.phoneNo

            val firstLetter = contact.name.take(1).uppercase()
            tvInitial.text = firstLetter

            val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3")
            val colorIndex = (contact.name.hashCode() and 0x7FFFFFFF) % colors.size
            tvInitial.background.setTint(android.graphics.Color.parseColor(colors[colorIndex]))

            if (!contact.imageUri.isNullOrEmpty()) {
                ivAvatar.visibility = View.VISIBLE
                tvInitial.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(contact.imageUri)
                    .apply(
                        RequestOptions()
                            .circleCrop()
                            .format(DecodeFormat.PREFER_RGB_565)
                            .override(96, 96)
                    )
                    .into(ivAvatar)
            } else {
                ivAvatar.visibility = View.GONE
                tvInitial.visibility = View.VISIBLE
            }
        }
    }

    private class ContactListDiffCallback(
        private val oldList: List<ContactListItem>,
        private val newList: List<ContactListItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            val old = oldList[oldPos]
            val new = newList[newPos]
            return when {
                old is ContactListItem.Header && new is ContactListItem.Header ->
                    old.letter == new.letter
                old is ContactListItem.ContactItem && new is ContactListItem.ContactItem ->
                    old.contact.id == new.contact.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            oldList[oldPos] == newList[newPos]
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }
}
