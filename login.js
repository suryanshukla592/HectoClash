import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import { getAuth, signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
  authDomain: "hectoclash-e0c38.firebaseapp.com",
  databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
  projectId: "hectoclash-e0c38",
  storageBucket: "hectoclash-e0c38.appspot.com", // ✅ fixed
  messagingSenderId: "590113172954",
  appId: "1:590113172954:web:5a969945a9aa9283c424e3",
  measurementId: "G-3ZZHRLSZK4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

document.getElementById("contactform").addEventListener("submit", (e) => {
  e.preventDefault();

  const email = document.getElementById("myemail").value.trim();
  const password = document.getElementById("mypass").value;
  const errorMsg = document.getElementById("errorMsg");

  signInWithEmailAndPassword(auth, email, password)
    .then((userCredential) => {
      window.location.href = "mainpg.html";
    })
    .catch((error) => {
      const code = error.code;
      if (code === "auth/user-not-found") {
        window.alert("No account with this email.");
      } else if (code === "auth/wrong-password") {
        window.alert("Incorrect password.");
      } else {
        window.alert("Login failed: " + error.message);
      }
    });
});
