package com.example.contactmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivityAddContactBinding
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity() {

    private lateinit var bind: ActivityAddContactBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bind = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(bind.root)

        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bind.btnSaveContact.setOnClickListener {
            val name = bind.etContactName.text.toString().trim()
            val phone = bind.etContactPhone.text.toString().trim()
            val email = bind.etContactEmail.text.toString().trim()

            if (validName(name) && validPhone(phone)) {
                saveContactLocally(name, phone, email)
            } else {
                if (!validName(name)) {
                    Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show()
                } else if (!validPhone(phone)) {
                    Toast.makeText(this, "Enter Phone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveContactLocally(name: String, phone: String, email: String) {
        val contact = Contact(name = name, phoneNo = phone, email = email)
        lifecycleScope.launch {
            database.contactDao().insertContact(contact)
            Toast.makeText(this@AddContactActivity, "Contact Saved Locally", Toast.LENGTH_SHORT).show()
            val i = Intent(this@AddContactActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    private fun validName(name: String): Boolean {
        if (name.isEmpty()) {
            bind.tilName.error = "This Field is Required!!"
            bind.tilName.requestFocus()
            return false
        } else {
            bind.tilName.error = null
            return true
        }
    }

    private fun validPhone(phone: String): Boolean {
        if (phone.isEmpty()) {
            bind.tilPhone.error = "This Field is Required!!"
            bind.tilPhone.requestFocus()
            return false
        } else {
            bind.tilPhone.error = null
            return true
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
