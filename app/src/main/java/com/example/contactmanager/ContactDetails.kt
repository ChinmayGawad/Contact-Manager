package com.example.contactmanager

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
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
        intent.getIntExtra("imgId", android.R.drawable.ic_menu_myplaces)
        val imageUriString = intent.getStringExtra("imageUri")

        binding.tvDetailName.text = name
        binding.tvDetailEmail.text = email
        binding.tvDetailPhone.text = phone

        if (!imageUriString.isNullOrEmpty()) {
            binding.ivDetailAvatar.visibility = View.VISIBLE
            binding.tvDetailInitial.visibility = View.GONE
            binding.ivDetailAvatar.setImageURI(imageUriString.toUri())
        } else {
            binding.ivDetailAvatar.visibility = View.GONE
            binding.tvDetailInitial.visibility = View.VISIBLE

            val firstLetter = name?.take(1)?.uppercase()
            binding.tvDetailInitial.text = firstLetter

            val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3")
            val colorIndex = Math.abs(name.hashCode()) % colors.size

            binding.tvDetailInitial.background.setTint(Color.parseColor(colors[colorIndex]))


        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDeleteContact.setOnClickListener {
            if (phone != null) {
                lifecycleScope.launch {
                    val contact = database.contactDao().getContactByPhone(phone)
                    if (contact != null) {
                        database.contactDao().deleteContact(contact)
                        Toast.makeText(this@ContactDetails, "Contact Deleted!!", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ContactDetails,
                            "Error: Contact not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Error: Could not find contact phone", Toast.LENGTH_SHORT)
                    .show()
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
