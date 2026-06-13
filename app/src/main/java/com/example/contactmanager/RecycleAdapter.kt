package com.example.contactmanager

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

class RecycleAdapter(var arrayList: ArrayList<Contacts>, var context: Activity) :
    Adapter<RecycleAdapter.MyViewHolder>() {

    private var arrayListFull: ArrayList<Contacts> = ArrayList(arrayList)
    private lateinit var myListener: onItemClickListener

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        myListener = listener
    }

    fun updateList(newList: ArrayList<Contacts>) {
        arrayList = newList
        arrayListFull = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun filter(text: String) {
        val filteredList = ArrayList<Contacts>()
        for (item in arrayListFull) {
            if (item.name.lowercase().contains(text.lowercase())) {
                filteredList.add(item)
            }
        }
        arrayList = filteredList
        notifyDataSetChanged()

        // Show/hide empty state based on search results
        if (context is MainActivity) {
            (context as MainActivity).updateEmptyState(filteredList.isEmpty())
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyViewHolder {
        val itemView = LayoutInflater.from(p0.context).inflate(R.layout.item_contact, p0, false)
        return MyViewHolder(itemView, myListener)
    }

    override fun onBindViewHolder(p0: MyViewHolder, p1: Int) {
        val currentItem = arrayList[p1]
        p0.name.text = currentItem.name
        p0.phone.text = currentItem.PhoneNo

        // Guard against invalid image resource IDs (like 0 or 1) that cause crashes.
        if (currentItem.imgId > 1000) {
            p0.img.setImageResource(currentItem.imgId)
        } else {
            // Default avatar if no valid image ID is provided
            p0.img.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class MyViewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        val phone: TextView = itemView.findViewById(R.id.tv_contact_phone)
        val img: ImageView = itemView.findViewById(R.id.iv_avatar)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}
