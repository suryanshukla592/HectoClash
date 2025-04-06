import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import { getFirestore, collection, getDocs, query, orderBy } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";

// Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyAqtdb1hl8fVn8HfiAMrVF8waZLaqbHAFw",
  authDomain: "hectoclash-e0c38.firebaseapp.com",
  databaseURL: "https://hectoclash-e0c38-default-rtdb.firebaseio.com",
  projectId: "hectoclash-e0c38",
  storageBucket: "hectoclash-e0c38.appspot.com", // FIXED typo here
  messagingSenderId: "590113172954",
  appId: "1:590113172954:web:5a969945a9aa9283c424e3",
  measurementId: "G-3ZZHRLSZK4"
};

// Initialize Firebase services
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Check auth state
onAuthStateChanged(auth, (user) => {
  if (!user) {
    window.location.href = 'login.html';
  } else {
    fetchLeaderboard();
  }
});

// Fetch leaderboard data
async function fetchLeaderboard() {
  const q = query(collection(db, "Users"), orderBy("Rating", "desc"));
  const querySnapshot = await getDocs(q);
  const players = [];

  querySnapshot.forEach((doc) => {
    const data = doc.data();
    players.push({
      uid: doc.id,
      name: data.Username || 'Anonymous',
      rating: data.Rating || 0,
      accuracy: data.Accuracy,
      time: data.Time,
      profileURL: data["Profile Picture URL"] || 'defaultdp[1].png'
    });
  });
  
  if (players.length > 0) {
    const topPlayer = players[0];
    document.getElementById("firstRankProfile").src = topPlayer.profileURL;
    document.getElementById("namefirst").textContent = topPlayer.name;
    document.getElementById("ratingfirst").textContent = `⭐ Rating: ${topPlayer.rating}`;
    document.getElementById("accufirst").textContent = `🎯 Accuracy:  ${Math.floor(topPlayer.accuracy)}`
    document.getElementById("avgfst").textContent = `⏱ Average Time:  ${topPlayer.time}s`
    // Remaining players
    players.slice(1, 30).forEach((player, index) => renderPlayerCard(player, index));
  }
}

function renderPlayerCard(player, index) {
  const container = document.getElementById("leaderboardList");
  const card = document.createElement("div");
  card.className = "card";
  card.innerHTML = `
    <div class="sub">
      <img class="player-img" src="${player.profileURL}" alt="Profile Picture">
      <div class="player-info">
        <p class="player-name">${player.name}</p>
        <div class="player-details"
        <p class="player-rating">⭐ Rating: ${player.rating}</p>
        <p class="player-rating">🎯 Accuracy:  ${Math.floor(player.accuracy)}</p>
        <p class="player-rating">⏱ Average Time:  ${player.time}s</p>
        </div>
      </div>
    </div>
    <div class="player-rank">${index + 2}</div>
  `;
  container.appendChild(card);
}
