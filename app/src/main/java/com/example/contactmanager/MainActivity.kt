package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private lateinit var recycleAdapter: RecycleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        setupListeners()
        observeContacts()
    }

    private fun setupRecyclerView() {
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.setHasFixedSize(true)

        recycleAdapter = RecycleAdapter(
            onItemClickListener = object : RecycleAdapter.OnItemClickListener {
                override fun onItemClick(contact: Contact) {
                    val intent = Intent(this@MainActivity, ContactDetails::class.java).apply {
                        putExtra("name", contact.name)
                        putExtra("PhoneNo", contact.phoneNo)
                        putExtra("email", contact.email)
                        putExtra("imgId", contact.imgId)
                        putExtra("imageUri", contact.imageUri)
                    }
                    startActivity(intent)
                }
            },
            onEmptyStateChanged = { isEmpty ->
                updateEmptyState(isEmpty)
            }
        )
        binding.rvContacts.adapter = recycleAdapter
    }

    private fun setupSearch() {
        binding.svSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                recycleAdapter.filter(newText ?: "")
                return true
            }
        })
    }

    private fun setupListeners() {
        binding.fabAddContact.setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeContacts() {
        lifecycleScope.launch {
            database.contactDao().getAllContacts().collectLatest { list ->
                binding.pbLoading.visibility = View.GONE
                recycleAdapter.updateList(list)
            }
        }
    }

    fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvContacts.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
