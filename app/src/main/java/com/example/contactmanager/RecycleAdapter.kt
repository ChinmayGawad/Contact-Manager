package com.example.contactmanager

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

sealed class ContactListItem {
    data class Header(val letter: String) : ContactListItem()
    data class ContactItem(val contact: Contact) : ContactListItem()
}

class RecycleAdapter(var contactsList: List<Contact>, var context: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var contactsListFull: List<Contact> = ArrayList(contactsList)
    private var displayList: ArrayList<ContactListItem> = ArrayList()
    private lateinit var myListener: OnItemClickListener

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_CONTACT = 1
    interface OnItemClickListener {
        fun onItemClick(contact: Contact)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        myListener = listener
    }

    init {
        updateDisplayList(contactsList)
    }

    fun updateList(newList: List<Contact>) {
        contactsList = newList
        contactsListFull = ArrayList(newList)
        updateDisplayList(newList)
    }

    private fun updateDisplayList(list: List<Contact>) {
        displayList.clear()
        if (list.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        val sortedList = list.sortedBy { it.name.lowercase() }
        var currentLetter = ""

        for (contact in sortedList) {
            val firstLetter = contact.name.take(1).uppercase()
            if (firstLetter != currentLetter) {
                currentLetter = firstLetter
                displayList.add(ContactListItem.Header(currentLetter))
            }
            displayList.add(ContactListItem.ContactItem(contact))
        }
        notifyDataSetChanged()
    }

    fun filter(text: String) {
        val filteredList = contactsListFull.filter {
            it.name.lowercase().contains(text.lowercase())
        }
        updateDisplayList(filteredList)

        if (context is MainActivity) {
            (context as MainActivity).updateEmptyState(filteredList.isEmpty())
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayList[position]) {
            is ContactListItem.Header -> VIEW_TYPE_HEADER
            is ContactListItem.ContactItem -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
            ContactViewHolder(view, myListener, displayList)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is ContactListItem.Header -> (holder as HeaderViewHolder).bind(item.letter)
            is ContactListItem.ContactItem -> (holder as ContactViewHolder).bind(item.contact)
        }
    }

    override fun getItemCount(): Int = displayList.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetter: TextView = itemView.findViewById(R.id.tv_header_letter)
        fun bind(letter: String) {
            tvLetter.text = letter
        }
    }

    class ContactViewHolder(
        itemView: View,
        private val listener: OnItemClickListener,
        private val displayList: List<ContactListItem>
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        private val tvInitial : TextView = itemView.findViewById(R.id.tv_contact_initial)
        private val phone: TextView = itemView.findViewById(R.id.tv_contact_phone)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)


        init {
            itemView.setOnClickListener {
                val item = displayList[bindingAdapterPosition]
                if (item is ContactListItem.ContactItem) {
                    listener.onItemClick(item.contact)
                }
            }
        }

        fun bind(contact: Contact) {
            name.text = contact.name
            phone.text = contact.phoneNo

            //Initial Letter Avatar
            val firstLetter = contact.name.take(1).uppercase()
            tvInitial.text = firstLetter

            val colors = listOf("#F44336", "#E91E63", "#9C27B0","#673AB7", "#3F51B5", "#2196F3")
            val colorIndex = Math.abs(contact.name.hashCode()) % colors.size
            tvInitial.background.setTint(Color.parseColor(colors[colorIndex]))

            //local image Fetching
            if (!contact.imageUri.isNullOrEmpty()){
                ivAvatar.visibility = View.VISIBLE
                tvInitial.visibility = View.GONE

                Glide.with(itemView.context).load(contact.imageUri).into(ivAvatar)
            }else{
                ivAvatar.visibility = View.GONE
                tvInitial.visibility = View.VISIBLE
            }
        }
    }
}
