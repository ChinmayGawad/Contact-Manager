package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.example.contactmanager.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var contactsArrayList = ArrayList<Contacts>()
    private lateinit var authProfile: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authProfile = FirebaseAuth.getInstance()
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.rvContacts.layoutManager = LinearLayoutManager(this)

        val recycleAdapter = RecycleAdapter(contactsArrayList, this)
        binding.rvContacts.adapter = recycleAdapter

        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                recycleAdapter.filter(newText ?: "")
                return true
            }
        })


        val firebaseUser = authProfile.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            databaseReference = FirebaseDatabase.getInstance("https://contact-manager-9f08c-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Contacts")
                .child(uid)// Consider adding .child(uid) here if contacts should be private

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    binding.pbLoading.visibility = View.GONE
                    contactsArrayList.clear()
                    if (p0.exists()) {
                        for (contactSnapshot in p0.children) {
                            try {
                                val contact = contactSnapshot.getValue(Contacts::class.java)
                                if (contact != null) {
                                    contactsArrayList.add(contact)
                                }
                            } catch (_: Exception) {
                                // Skip contacts that cannot be parsed
                                continue
                            }
                        }
                    }
                    recycleAdapter.updateList(contactsArrayList)
                    updateEmptyState(contactsArrayList.isEmpty())
                }

                override fun onCancelled(p0: DatabaseError) {
                    binding.pbLoading.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Failed to Load Contacts, Try Again Later $p0", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // If user is not logged in, redirect to Log in
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        recycleAdapter.setOnItemClickListener(object : RecycleAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                val i = Intent(this@MainActivity, ContactDetails::class.java)
                i.putExtra("name", contactsArrayList[position].name)
                i.putExtra("PhoneNo", contactsArrayList[position].PhoneNo)
                i.putExtra("email", contactsArrayList[position].email)
                i.putExtra("imgId", contactsArrayList[position].imgId)
                startActivity(i)
            }

        })

        binding.fabAddContact.setOnClickListener {
            val i = Intent(this, AddContactActivity::class.java)
            startActivity(i)
        }
    }

    fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
        }
    }
}