package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contactmanager.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SignUp : AppCompatActivity() {
    private lateinit var binding : ActivitySignUpBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    lateinit var databaseReference : DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //SignUp Process
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmailInput.text.toString().trim()
            val password = binding.etPasswordInput.text.toString().trim()
            val confirmPassword = binding.etConfirmPasswordInput.text.toString().trim()

            if(isValidInput(email,password,confirmPassword)){
                auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        val userID = auth.currentUser?.uid

                        databaseReference = database.getReference("Users").child(userID.toString())

                        databaseReference.child("email").setValue(email)

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }else{
                Toast.makeText(this,"Failure",Toast.LENGTH_SHORT).show()
            }
        }

        binding.BackToLogin.setOnClickListener {
            val i = Intent(this, LoginActivity::class.java)
            startActivity(i)
        }
    }
    private fun isValidInput(email: String?, password: String, confirmPassword: String?): Boolean {
        var valid = true

        // Check if email is valid
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email address"
            valid = false
        } else {
            binding.etEmail.error = null
        }

        // Check if password is empty or meets minimum length
        if (TextUtils.isEmpty(password) || password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            valid = false
        } else {
            binding.etPassword.error = null
        }

        // Check if passwords match
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            valid = false
        } else {
            binding.etConfirmPassword.error = null
        }

        return valid
    }
}