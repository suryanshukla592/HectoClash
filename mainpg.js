// import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
// import { getAnalytics } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-analytics.js";
// // TODO: Add SDKs for Firebase products that you want to use
// // https://firebase.google.com/docs/web/setup#available-libraries

// // Your web app's Firebase configuration
// // For Firebase JS SDK v7.20.0 and later, measurementId is optional
// const firebaseConfig = {
//   apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
//   authDomain: "hectoclash-e0c38.firebaseapp.com",
//   databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
//   projectId: "hectoclash-e0c38",
//   storageBucket: "hectoclash-e0c38.firebasestorage.app",
//   messagingSenderId: "590113172954",
//   appId: "1:590113172954:web:5a969945a9aa9283c424e3",
//   measurementId: "G-3ZZHRLSZK4"
// };

// const app = initializeApp(firebaseConfig);
// const db = getFirestore(app);
// const auth = getAuth(app);

document.getElementById("buttonStartGame").addEventListener("click", () => {
  alert("Matchmaking will be implemented soon.");
});

document.getElementById("buttonPractice").addEventListener("click", (e) => {
  e.preventDefault();
  window.location.href = "match.html";
});

document.getElementById("buttonLeaderboard").addEventListener("click", (e) => {
  e.preventDefault();
  window.location.href = "leaderboard.html";
});

// document.getElementById("buttonLeaderboard").onclick = function(e){
//   e.preventDefault();
//   window.location.href = 'lb-hectoc/leaderboard.html'
// }

document.getElementById("buttonSpectate").addEventListener("click", () => {
  alert("Spectator mode coming soon!");
});

document.getElementById("buttonStats").addEventListener("click", () => {
  window.location.href = "stats.html";
});

document.getElementById("buttonHowToPlay").addEventListener("click", () => {
  window.location.href = "howToPlay.html";
});


