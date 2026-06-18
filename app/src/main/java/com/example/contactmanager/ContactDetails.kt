package com.example.contactmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivityContactDetailsBinding
import kotlinx.coroutines.launch

class ContactDetails : AppCompatActivity() {
    private lateinit var binding: ActivityContactDetailsBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        val phone = intent.getStringExtra("PhoneNo")
        val imgId = intent.getIntExtra("imgId", android.R.drawable.ic_menu_myplaces)

        binding.tvDetailName.text = name
        binding.tvDetailEmail.text = email
        binding.tvDetailPhone.text = phone
        binding.ivDetailAvatar.setImageResource(imgId)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDeleteContact.setOnClickListener {
            if (phone != null) {
                lifecycleScope.launch {
                    val contact = database.contactDao().getContactByPhone(phone)
                    if (contact != null) {
                        database.contactDao().deleteContact(contact)
                        Toast.makeText(this@ContactDetails, "Contact Deleted!!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ContactDetails, "Error: Contact not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Error: Could not find contact phone", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }

        binding.fabMessage.setOnClickListener {
            val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri())
            try {
                startActivity(smsIntent)
            } catch (e: Exception) {
                Log.e("Messaging", "Cant find or open the messaging app")
                Toast.makeText(this, "No messaging app installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("theme_pref", MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", 0) // 0: System, 1: Light, 2: Dark
        when (themeMode) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
