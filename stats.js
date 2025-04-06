import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import { getFirestore, doc, getDoc } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
  authDomain: "hectoclash-e0c38.firebaseapp.com",
  databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
  projectId: "hectoclash-e0c38",
  storageBucket: "hectoclash-e0c38.appspot.com",
  messagingSenderId: "590113172954",
  appId: "1:590113172954:web:5a969945a9aa9283c424e3",
  measurementId: "G-3ZZHRLSZK4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Get current user's stats
onAuthStateChanged(auth, async (user) => {
  if (user) {
    const uid = user.uid;
    const userDocRef = doc(db, "Users", uid);
    const userSnap = await getDoc(userDocRef);

    if (userSnap.exists()) {
      const data = userSnap.data();

      const username = data.Username;
      const rating = data.Rating;
      const accuracy = data.Accuracy;
      const played = data.Played;
      const won = data.Won;
      const time = data.Time;
      

      document.getElementById("username").textContent = username;
      document.getElementById("rating").textContent = rating;
      document.getElementById("wins").textContent = won;
      document.getElementById("accuracy").textContent = Math.floor(accuracy);
      document.getElementById("played").textContent = played;
      document.getElementById("time").textContent = time;
    } 
    else {
      console.log("No such document!");
    }
  } else {
    console.log("User not signed in.");
    window.location.href = "login.html";
  }
});
