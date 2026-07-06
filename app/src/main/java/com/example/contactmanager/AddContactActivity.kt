package com.example.contactmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivityAddContactBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddContactBinding
    private lateinit var database: AppDatabase
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedImageUri = uri
            binding.ivNewAvatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.ivNewAvatar.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        binding.btnSaveContact.setOnClickListener { saveContact() }
    }

    private fun saveContact() {
        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etContactPhone.text.toString().trim()
        val email = binding.etContactEmail.text.toString().trim()

        if (!validateInput(name, phone)) return

        val contact = Contact(
            name = name,
            phoneNo = phone,
            email = email,
            imageUri = selectedImageUri?.toString()
        )

        lifecycleScope.launch {
            try {
                database.contactDao().insertContact(contact)
                Snackbar.make(binding.root, "Contact saved", Snackbar.LENGTH_SHORT).show()
                startActivity(Intent(this@AddContactActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to save: ${e.message}", Snackbar.LENGTH_LONG)
                    .setAction("Retry") { saveContact() }
                    .show()
            }
        }
    }

    private fun validateInput(name: String, phone: String): Boolean {
        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            binding.tilName.requestFocus()
            valid = false
        } else {
            binding.tilName.error = null
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            if (valid) binding.tilPhone.requestFocus()
            valid = false
        } else if (!PhoneUtils.isValidPhone(phone)) {
            binding.tilPhone.error = "Invalid phone number"
            if (valid) binding.tilPhone.requestFocus()
            valid = false
        } else {
            binding.tilPhone.error = null
        }

        return valid
    }
}
