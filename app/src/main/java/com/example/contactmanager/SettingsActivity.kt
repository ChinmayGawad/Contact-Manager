package com.example.contactmanager

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactmanager.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var database: AppDatabase

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) exportContactsToFile(uri)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) importDeviceContacts()
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        setupListeners()
        setupVersionInfo()
    }

    private fun setupThemeSwitch() {
        val sharedPref = getSharedPreferences("theme_pref", MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", 0)

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

                when (newMode) {
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnImportContacts.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Import Contacts")
                .setMessage("This will import contacts from your device. Duplicates will be skipped.")
                .setPositiveButton("Import") { _, _ -> checkPermissionAndImport() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnExportContacts.setOnClickListener {
            createFileLauncher.launch("contacts_export.csv")
        }
    }

    private fun setupVersionInfo() {
        try {
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
            binding.tvAppVersion.text = getString(R.string.version_format, pkg.versionName ?: "1.0")
        } catch (e: Exception) {
            binding.tvAppVersion.text = getString(R.string.version_format, "1.0")
        }
    }

    private fun checkPermissionAndImport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            importDeviceContacts()
        }
    }

    private fun importDeviceContacts() {
        val resolver = contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        val contactsToImport = mutableListOf<Contact>()

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            if (nameIndex != -1 && numberIndex != -1) {
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: ""
                    val rawNumber = it.getString(numberIndex) ?: ""
                    val cleanNumber = PhoneUtils.cleanPhoneNumber(rawNumber)

                    if (name.isNotEmpty() && cleanNumber.isNotEmpty()) {
                        contactsToImport.add(
                            Contact(name = name, phoneNo = cleanNumber, email = "")
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            database.contactDao().importContacts(contactsToImport)
            Snackbar.make(binding.root, "Imported ${contactsToImport.size} contacts", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun exportContactsToFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val contacts = database.contactDao().getAllContacts().first()
                val csvContent = buildCsvContent(contacts)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvContent)
                    }
                }
                Snackbar.make(binding.root, "Exported ${contacts.size} contacts", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun buildCsvContent(contacts: List<Contact>): String {
        val sb = StringBuilder("Name,Phone,Email\n")
        for (contact in contacts) {
            val escapedName = contact.name.replace("\"", "\"\"")
            val escapedEmail = contact.email.replace("\"", "\"\"")
            sb.appendLine("\"$escapedName\",${contact.phoneNo},\"$escapedEmail\"")
        }
        return sb.toString()
    }
}
