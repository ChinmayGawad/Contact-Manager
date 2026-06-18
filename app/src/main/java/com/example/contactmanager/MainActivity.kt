package com.example.contactmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraExtensionSession
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val REQUEST_READ_CONTACTS = 101
    private lateinit var binding : ActivityMainBinding
    private var contactsArrayList = ArrayList<Contact>()
    private lateinit var database: AppDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
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

        lifecycleScope.launch {
            database.contactDao().getAllContacts().collectLatest { list ->
                binding.pbLoading.visibility = View.GONE
                contactsArrayList.clear()
                contactsArrayList.addAll(list)
                recycleAdapter.updateList(contactsArrayList)
                updateEmptyState(contactsArrayList.isEmpty())
            }
        }

        recycleAdapter.setOnItemClickListener(object : RecycleAdapter.OnItemClickListener{
            override fun onItemClick(contact: Contact) {
                val i = Intent(this@MainActivity, ContactDetails::class.java)
                i.putExtra("name", contact.name)
                i.putExtra("PhoneNo", contact.phoneNo)
                i.putExtra("email", contact.email)
                i.putExtra("imgId", contact.imgId)
                startActivity(i)
            }
        })

        binding.fabAddContact.setOnClickListener {
            val i = Intent(this, AddContactActivity::class.java)
            startActivity(i)
        }

        binding.btnSettings.setOnClickListener {
            val i = Intent(this, SettingsActivity::class.java)
            startActivity(i)
        }
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("theme_pref", Context.MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", 0) // 0: System, 1: Light, 2: Dark
        when (themeMode) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun cleanPhoneNumber(number: String): String {
        // Keeps only digits
        val digitsOnly = number.replace(Regex("[^0-9]"), "")
        // If it starts with 91 and has 12 digits, it's likely an Indian number with country code
        return if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
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