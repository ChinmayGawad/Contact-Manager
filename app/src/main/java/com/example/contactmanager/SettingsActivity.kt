package com.example.contactmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.lang.StringBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val REQUEST_READ_CONTACTS = 101
    private val REQUEST_CREATE_FILE = 102
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupThemeSwitch()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnImportContacts.setOnClickListener {
            checkPermissionAndImport()
        }

        binding.btnExportContacts.setOnClickListener {
            exportContacts()
        }
    }

    private fun setupThemeSwitch() {
        val sharedPref = getSharedPreferences("theme_pref", MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", 0)

        // Set initial selection
        when (themeMode) {
            1 -> binding.themeToggleGroup.check(R.id.btnThemeLight)
            2 -> binding.themeToggleGroup.check(R.id.btnThemeDark)
            else -> binding.themeToggleGroup.check(R.id.btnThemeSystem)
        }

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btnThemeLight -> 1
                    R.id.btnThemeDark -> 2
                    else -> 0
                }
                sharedPref.edit().putInt("theme_mode", newMode).apply()
                
                // Apply the theme immediately
                when (newMode) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
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

    private fun checkPermissionAndImport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
        } else {
            importDeviceContacts()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            importDeviceContacts()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDeviceContacts() {
        val resolver = contentResolver
        val cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        var count = 0
        lifecycleScope.launch {
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                if (nameIndex != -1 && numberIndex != -1) {
                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex) ?: ""
                        val rawNumber = it.getString(numberIndex) ?: ""
                        val cleanNumber = cleanPhoneNumber(rawNumber)

                        if (name.isNotEmpty() && cleanNumber.isNotEmpty()) {
                            val contact = Contact(name = name, phoneNo = cleanNumber, email = "")
                            database.contactDao().insertContact(contact)
                            count++
                        }
                    }
                }
            }
            Toast.makeText(this@SettingsActivity, "Imported $count contacts locally", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanPhoneNumber(number: String): String {
        val digitsOnly = number.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
    }

    private fun exportContacts() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "contacts_export.csv")
        }
        startActivityForResult(intent, REQUEST_CREATE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREATE_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                fetchAndWriteLocalContacts(uri)
            }
        }
    }

    private fun fetchAndWriteLocalContacts(uri: Uri) {
        lifecycleScope.launch {
            val contacts = database.contactDao().getAllContacts().first()
            val csvContent = StringBuilder("Name,Phone,Email\n")
            for (contact in contacts) {
                csvContent.append("${contact.name},${contact.phoneNo},${contact.email}\n")
            }
            writeCsvToFile(uri, csvContent.toString())
        }
    }

    private fun writeCsvToFile(uri: Uri, content: String) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            Toast.makeText(this, "Export successful!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error writing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
