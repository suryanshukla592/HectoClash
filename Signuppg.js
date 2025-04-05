import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import { getFirestore, doc, setDoc } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import { getAuth, createUserWithEmailAndPassword,sendEmailVerification } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
    authDomain: "hectoclash-e0c38.firebaseapp.com",
    databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
    projectId: "hectoclash-e0c38",
    storageBucket: "hectoclash-e0c38.firebasestorage.app",
    messagingSenderId: "590113172954",
    appId: "1:590113172954:web:5a969945a9aa9283c424e3",
    measurementId: "G-3ZZHRLSZK4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Handle signup form submission
document.getElementById("contactForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("Username").value.trim();
  const email = document.getElementById("Email").value.trim();
  const password = document.getElementById("Password").value;
  const confirmPassword = document.getElementById("CPassword").value;

  if (password !== confirmPassword) {
    alert("Passwords do not match.");
    return;
  }

  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    const user = userCredential.user;

    const userData = {
      UserID: user.uid,
      Username: username,
      Email: email,
      Rating: 0
    };

    await setDoc(doc(db, "Users", user.uid), userData);

    alert("Account created successfully. Please verify your email.");
    await sendEmailVerification(user);
    window.location.href = "mainpg.html";

  } catch (error) {
    if (error.code === "auth/email-already-in-use") {
      alert("Email is already in use.");
    } else if (error.code === "auth/invalid-email") {
      alert("Please enter a valid email.");
    } else if (error.code === "auth/weak-password") {
      alert("Password is too weak. Please choose a stronger password.");
    } else {
      console.error(error);
      alert("Error: " + error.message);
    }
  }
});
