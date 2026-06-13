package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contactmanager.databinding.ActivityContactDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ContactDetails : AppCompatActivity() {
    private lateinit var binding: ActivityContactDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid != null && phone != null) {
                val dbRef = FirebaseDatabase.getInstance("https://contact-manager-9f08c-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Contacts")
                    .child(uid)
                    .child(phone)

                dbRef.removeValue().addOnSuccessListener {
                    Toast.makeText(this, "Contact Deleted!!", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to delete: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error: Could not find contact ID", Toast.LENGTH_SHORT).show()
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
}
