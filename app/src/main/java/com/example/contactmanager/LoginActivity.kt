package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.contactmanager.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Objects


class LoginActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLoginBinding
    private lateinit var authLogin : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        authLogin = FirebaseAuth.getInstance()

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailInput.text.toString().trim()
            val pass = binding.etPasswordInput.text.toString().trim()
           authLogin.signInWithEmailAndPassword(email,pass).addOnCompleteListener { task ->
               if(task.isSuccessful){
                   val intent = Intent(this, MainActivity::class.java)
                   startActivity(intent)
                   finish()
               }
               else{
                   Toast.makeText(this,"Failed to Login", Toast.LENGTH_SHORT).show()

               }
           }

        }
        binding.CreateAccount.setOnClickListener {
            val i = Intent(this, SignUp::class.java)
            startActivity(i)
        }

    }
}