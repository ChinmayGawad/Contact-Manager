package com.example.contactmanager

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivityContactDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ContactDetails : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailsBinding
    private lateinit var database: AppDatabase
    private var phone: String? = null
    private var contactName: String? = null

    companion object {
        private val AVATAR_COLORS = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#009688", "#4CAF50"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        extractIntentData()
        setupListeners()
    }

    private fun extractIntentData() {
        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        phone = intent.getStringExtra("PhoneNo")
        val imageUriString = intent.getStringExtra("imageUri")
        contactName = name

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

            val colorIndex = (name.hashCode() and 0x7FFFFFFF) % AVATAR_COLORS.size
            binding.tvDetailInitial.background.setTint(Color.parseColor(AVATAR_COLORS[colorIndex]))
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnDeleteContact.setOnClickListener { showDeleteConfirmation() }

        binding.fabCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            startActivity(intent)
        }

        binding.fabMessage.setOnClickListener {
            val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:$phone".toUri())
            try {
                startActivity(smsIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No messaging app installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contactName ?: "this contact"}?")
            .setPositiveButton("Delete") { _, _ -> deleteContact() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteContact() {
        if (phone == null) {
            Toast.makeText(this, "Error: Could not find contact", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val contact = database.contactDao().getContactByPhone(phone!!)
            if (contact != null) {
                database.contactDao().deleteContact(contact)
                Toast.makeText(this@ContactDetails, "Contact deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@ContactDetails, "Contact not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
