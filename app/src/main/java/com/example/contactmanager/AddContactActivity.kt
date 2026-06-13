package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contactmanager.databinding.ActivityAddContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddContactActivity : AppCompatActivity() {

    private lateinit var bind: ActivityAddContactBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bind = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(bind.root)
        ViewCompat.setOnApplyWindowInsetsListener(bind.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        bind.btnSaveContact.setOnClickListener {
            val name = bind.etContactName.text.toString().trim()
            val phone = bind.etContactPhone.text.toString().trim()
            val email = bind.etContactEmail.text.toString().trim()

            if (validName(name) && validPhone(phone)) {
                val currentUser: FirebaseUser? = auth.currentUser
                if (currentUser != null) {
                    saveContact(currentUser.uid, name, phone, email)
                } else {
                    val i = Intent(this, LoginActivity::class.java)
                    Toast.makeText(this, "Login First", Toast.LENGTH_SHORT).show()
                    startActivity(i)
                    finish()
                }
            } else {
                if (!validName(name)) {
                    Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show()
                } else if (!validPhone(phone)) {
                    Toast.makeText(this, "Enter Phone", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveContact(uid: String, name: String, phone: String, email: String) {
        val contact = Contacts(name, phone, email)
        databaseReference = FirebaseDatabase.getInstance("https://contact-manager-9f08c-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference()
            .child("Contacts")
            .child(uid)
            .child(phone)

        databaseReference.setValue(contact)
            .addOnSuccessListener {
                Toast.makeText(this, "User Added Successfully", Toast.LENGTH_SHORT).show()
                val i = Intent(this, MainActivity::class.java)
                startActivity(i)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: $e", Toast.LENGTH_SHORT).show()
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
}
