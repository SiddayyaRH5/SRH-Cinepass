import logo from './logo.svg'
import marvelLogo from './marvelLogo.svg'
import googlePlay from './googlePlay.svg'
import appStore from './appStore.svg'
import screenImage from './screenImage.svg'
import profile from './profile.png'

export const assets = {
    logo,
    marvelLogo,
    googlePlay,
    appStore,
    screenImage,
    profile
}

export const dummyTrailers = [

    {
        image: "https://img.youtube.com/vi/NZbmcl0QUaU/maxresdefault.jpg",
        videoUrl: 'https://www.youtube.com/watch?v=k2DbnzJa9fk'
    },
    {
        image: "https://www.keralatv.in/media/2025/04/Madharasi-Release-Date.jpg",
        videoUrl: 'https://www.youtube.com/watch?v=gTNohNx6GNw'
    },
    {
        image: "https://img.youtube.com/vi/64XHtNWTB5o/maxresdefault.jpg",
        videoUrl: 'https://www.youtube.com/watch?v=64XHtNWTB5o'
    },
    {
        image: "https://baapofmovies.com/wp-content/uploads/2025/05/xc-xcv-min.jpg",
        videoUrl: 'https://www.youtube.com/watch?v=nsqHCfO1ayQ'
    },
]

const dummyCastsData = [
    { "name": "Milla Jovovich", "profile_path": "https://image.tmdb.org/t/p/original/usWnHCzbADijULREZYSJ0qfM00y.jpg", },
    { "name": "Dave Bautista", "profile_path": "https://image.tmdb.org/t/p/original/snk6JiXOOoRjPtHU5VMoy6qbd32.jpg", },
    { "name": "Arly Jover", "profile_path": "https://image.tmdb.org/t/p/original/zmznPrQ9GSZwcOIUT0c3GyETwrP.jpg", },
    { "name": "Amara Okereke", "profile_path": "https://image.tmdb.org/t/p/original/nTSPtzWu6deZTJtWXHUpACVznY4.jpg", },
    { "name": "Fraser James", "profile_path": "https://image.tmdb.org/t/p/original/mGAPQG2OKTgdKFkp9YpvCSqcbgY.jpg", },
    { "name": "Deirdre Mullins", "profile_path": "https://image.tmdb.org/t/p/original/lJm89neuiVlYISEqNpGZA5kTAnP.jpg", },
    { "name": "Sebastian Stankiewicz", "profile_path": "https://image.tmdb.org/t/p/original/hLN0Ca09KwQOFLZLPIEzgTIbqqg.jpg", },
    { "name": "Tue Lunding", "profile_path": "https://image.tmdb.org/t/p/original/qY4W0zfGBYzlCyCC0QDJS1Muoa0.jpg", },
    { "name": "Jacek Dzisiewicz", "profile_path": "https://image.tmdb.org/t/p/original/6Ksb8ANhhoWWGnlM6O1qrySd7e1.jpg", },
    { "name": "Ian Hanmore", "profile_path": "https://image.tmdb.org/t/p/original/yhI4MK5atavKBD9wiJtaO1say1p.jpg", },
    { "name": "Eveline Hall", "profile_path": "https://image.tmdb.org/t/p/original/uPq4xUPiJIMW5rXF9AT0GrRqgJY.jpg", },
    { "name": "Kamila Klamut", "profile_path": "https://image.tmdb.org/t/p/original/usWnHCzbADijULREZYSJ0qfM00y.jpg", },
    { "name": "Caoilinn Springall", "profile_path": "https://image.tmdb.org/t/p/original/uZNtbPHowlBYo74U1qlTaRlrdiY.jpg", },
    { "name": "Jan Kowalewski", "profile_path": "https://image.tmdb.org/t/p/original/snk6JiXOOoRjPtHU5VMoy6qbd32.jpg", },
    { "name": "Pawel Wysocki", "profile_path": "https://image.tmdb.org/t/p/original/zmznPrQ9GSZwcOIUT0c3GyETwrP.jpg", },
    { "name": "Simon Lööf", "profile_path": "https://image.tmdb.org/t/p/original/cbZrB8crWlLEDjVUoak8Liak6s.jpg", },
    { "name": "Tomasz Cymerman", "profile_path": "https://image.tmdb.org/t/p/original/nTSPtzWu6deZTJtWXHUpACVznY4.jpg", }
]

export const dummyShowsData = [
    {
        "_id": "823464",
        "id": 823464,
        "title": "Kalki 2898 AD",
        "overview": "Set in a grim future, the last surviving city, Kasi, is ruled by Yaskin—a despotic god-king. In this desolate landscape, fertile women are abducted for Project K, meant to extract a serum from fetuses to sustain Yaskin’s power. Ashwatthama, forever cursed since the Mahabharata, seeks redemption by protecting SUM-80, the unborn mother of Kalki. Alongside Bhairava, they evade Yaskin’s forces, uniting with rebels in this high-stakes mission to usher in a new age.",
        "poster_path": "https://lh3.googleusercontent.com/QiBRSTOv50JKX6oauKzQUGjlVgmWkdMdtwRho-6c4L4A3jOR8bE_k2GBNRbskiE7aJvy-Rn4-_p4JwY=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/QiBRSTOv50JKX6oauKzQUGjlVgmWkdMdtwRho-6c4L4A3jOR8bE_k2GBNRbskiE7aJvy-Rn4-_p4JwY=w544-h544-l90-rj",
        "genres": [
            { "id": 28, "name": "Action" },
            { "id": 12, "name": "Adventure" },
            { "id": 878, "name": "Science Fiction" },
            { "id": 14, "name": "Fantasy" }
        ],
        "casts": dummyCastsData,
        "release_date": "2024-06-27",
        "original_language": "Telugu | Kannada | Hindi | Tamil | Malayalam",
        "tagline": "The Future Unfolds.",
        "vote_average": 8.0,
        "vote_count": 25000,
        "runtime": 181
    },
    {
        "_id": "9876543",
        "id": 9876543,
        "title": "Mahavatra Narasimha",
        "overview": "Mahavatar Narsimha offers a powerful retelling of Hindu mythology through high-end animation, deep devotional themes, and grand visuals. Ideal for those interested in mythological epics, it sets a new benchmark in Indian animated cinema. The film's stunning animation and engaging storytelling bring the legend of Lord Narasimha to life, making it a must-watch for mythology enthusiasts and animation lovers alike.",
        "poster_path": "https://lh3.googleusercontent.com/DweJuOvcpBewx4Oz1zegi9L5EJnkK33F5EXTOFZMyfLaN3gIi3apeJ_CI-rnabeSOhAB1V3NHGuxgco=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/DweJuOvcpBewx4Oz1zegi9L5EJnkK33F5EXTOFZMyfLaN3gIi3apeJ_CI-rnabeSOhAB1V3NHGuxgco=w544-h544-l90-rj",
        "genres": [
            { "id": 28, "name": "Action" },
            { "id": 12, "name": "Adventure" },
            { "id": 14, "name": "Fantasy" },
            { "id": 36, "name": "History" }
        ],
        "casts": dummyCastsData,
        "release_date": "2025-10-15",
        "original_language": "Telugu | Kannada | Hindi | Tamil | Malayalam",
        "tagline": "When devotion meets destiny, the divine roars.",
        "vote_average": 8.7,
        "vote_count": 25000,
        "runtime": 165
    },


    {
        "_id": "20250501",
        "id": 20250501,
        "title": "Coolie",
        "overview": "Set in 2025, Coolie reimagines the story of Iqbal, a fearless railway worker who becomes the voice of the common people. When a corporate giant threatens to demolish the railway colony for profit, Iqbal rises to protect his community. Battling corruption, betrayal, and powerful enemies, he discovers the true strength of unity, sacrifice, and resilience.",
        "poster_path": "https://lh3.googleusercontent.com/rEP8DGXzEu4hQlgTKlKMpJAVH4_hKne4nGq-G-x_w13k--GA2co00q_qNuJuKU_qj6p7GgXWadTdnA8g=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/rEP8DGXzEu4hQlgTKlKMpJAVH4_hKne4nGq-G-x_w13k--GA2co00q_qNuJuKU_qj6p7GgXWadTdnA8g=w544-h544-l90-rj",
        "genres": [
            { "id": 28, "name": "Action" },
            { "id": 18, "name": "Drama" },
            { "id": 53, "name": "Thriller" }
        ],
        "casts": dummyCastsData,
        "release_date": "2025-12-15",
        "original_language": "Telugu | Kannada | Hindi | Tamil | Malayalam",
        "tagline": "The fight of the people begins again.",
        "vote_average": 7.6,
        "vote_count": 0,
        "runtime": 150
    },

    {
        "_id": "20250668489",
        "id": 20250668489,
        "title": "War 2",
        "overview": "In the high-octane sequel to the blockbuster War, Kabir (Hrithik Roshan) returns to face his deadliest mission yet. When a rogue intelligence network threatens global security, Kabir joins forces with an unlikely ally. Packed with breathtaking action, international espionage, and emotional stakes, War 2 takes the franchise to a whole new level.",
        "poster_path": "https://lh3.googleusercontent.com/skiyfNqxeLRLb6hObuv8UTQPfBcRvmFmaIaMVukgv3UeMetSbVuX0XXkTJN0UuC_BiWp1x57Yvg14aT0oQ=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/skiyfNqxeLRLb6hObuv8UTQPfBcRvmFmaIaMVukgv3UeMetSbVuX0XXkTJN0UuC_BiWp1x57Yvg14aT0oQ=w544-h544-l90-rj",
        "genres": [
            { "id": 28, "name": "Action" },
            { "id": 53, "name": "Thriller" },
            { "id": 12, "name": "Adventure" }
        ],
        "casts": dummyCastsData,
        "release_date": "2025-08-15",
        "original_language": "Hindi | Telugu ",
        "tagline": "The battle has just begun.",
        "vote_average": 7.0,
        "vote_count": 0,
        "runtime": 160
    },
    {
        "_id": "2025950387",
        "id": 2025950387,
        "title": "Mirai",
        "overview": "When a young boy named Kun feels displaced by the arrival of his baby sister Mirai, he discovers a magical garden that allows him to travel through time and meet his relatives from the past and future. Guided by an older version of Mirai, Kun embarks on a heartwarming journey of self-discovery, family bonds, and growing up.",
        "poster_path": "https://lh3.googleusercontent.com/G8i2mNb9h-6ykd1iuvOAJ3aH_piU4rwyp2OE6tWW1XxWDfPkux39WPBeCI0yYzodyOv_Mo7H5LveJKgx=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/G8i2mNb9h-6ykd1iuvOAJ3aH_piU4rwyp2OE6tWW1XxWDfPkux39WPBeCI0yYzodyOv_Mo7H5LveJKgx=w544-h544-l90-rj",
        "genres": [
            { "id": 16, "name": "Animation" },
            { "id": 10751, "name": "Family" },
            { "id": 14, "name": "Fantasy" },
            { "id": 18, "name": "Drama" }
        ],
        "casts": dummyCastsData,
        "release_date": "2025-07-10",
        "original_language": "Telugu | Kannada | Hindi | Tamil | Malayalam",
        "tagline": "The future begins with family.",
        "vote_average": 7.5,
        "vote_count": 0,
        "runtime": 98
    },
    {
        "_id": "20181575265",
        "id": 20181575265,
        "title": "KGF Chapter 2",
        "overview": "Rocky, who rises from poverty to power in the gold fields of Kolar, now stands as the king of KGF. While his name strikes fear in the hearts of his enemies, Rocky must face new challenges as he battles powerful adversaries, political conspiracies, and a dangerous promise made to his dying mother. The blood-soaked power struggle for control of KGF reaches epic heights in this action-packed saga.",
        "poster_path": "https://lh3.googleusercontent.com/21MLYtuMeh2aa-lfhXHsMH-SjiRElHlWd73ggzKHdf3k17C7Sgq4a2VQoLXlDlaaNIpI9l5sjKg3ifU=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/21MLYtuMeh2aa-lfhXHsMH-SjiRElHlWd73ggzKHdf3k17C7Sgq4a2VQoLXlDlaaNIpI9l5sjKg3ifU=w544-h544-l90-rj",
        "genres": [
            { "id": 28, "name": "Action" },
            { "id": 18, "name": "Drama" },
            { "id": 80, "name": "Crime" }
        ],
        "casts": dummyCastsData,
        "release_date": "2022-04-14",
        "original_language": "Telugu | Kannada | Hindi | Tamil | Malayalam",
        "tagline": "The world is my territory.",
        "vote_average": 8.2,
        "vote_count": 250000,
        "runtime": 168
    },
    {
        "_id": "2025986057",
        "id": 2025986057,
        "title": "Su From So",
        "overview": "A gripping Kannada drama-thriller of 2025, Su From So explores love, betrayal, and revenge through the journey of Su, a man caught between destiny and desire. With powerful emotions, raw action, and intense twists, the film delivers a high-voltage cinematic experience that keeps the audience on the edge till the very end.",
        "poster_path": "https://lh3.googleusercontent.com/AQVghsPKkgPPm3d8eg3vtKCx7w_I00U9POuGAHEZ_bx1ikzcZwE0I3ini7Gry1dKOydYxE7_pyTvA22OGg=w544-h544-l90-rj",
        "backdrop_path": "https://lh3.googleusercontent.com/AQVghsPKkgPPm3d8eg3vtKCx7w_I00U9POuGAHEZ_bx1ikzcZwE0I3ini7Gry1dKOydYxE7_pyTvA22OGg=w544-h544-l90-rj",
        "genres": [
            { "id": 18, "name": "Drama" },
            { "id": 28, "name": "Action" },
            { "id": 53, "name": "Thriller" }
        ],
        "casts": dummyCastsData,
        "release_date": "2025-12-20",
        "original_language": "kannada",
        "tagline": "Destiny Writes Its Own Story.",
        "vote_average": 8.5,
        "vote_count": 10000,
        "runtime": 145
    }
]

export const dummyDateTimeData = {
    "2025-07-24": [
        { "time": "2025-07-24T01:00:00.000Z", "showId": "68395b407f6329be2bb45bd1" },
        { "time": "2025-07-24T03:00:00.000Z", "showId": "68395b407f6329be2bb45bd2" },
        { "time": "2025-07-24T05:00:00.000Z", "showId": "68395b407f6329be2bb45bd3" }
    ],
    "2025-07-25": [
        { "time": "2025-07-25T01:00:00.000Z", "showId": "68395b407f6329be2bb45bd4" },
        { "time": "2025-07-25T03:00:00.000Z", "showId": "68395b407f6329be2bb45bd5" },
        { "time": "2025-07-25T05:00:00.000Z", "showId": "68395b407f6329be2bb45bd6" }
    ],
    "2025-07-26": [
        { "time": "2025-07-26T01:00:00.000Z", "showId": "68395b407f6329be2bb45bd7" },
        { "time": "2025-07-26T03:00:00.000Z", "showId": "68395b407f6329be2bb45bd8" },
        { "time": "2025-07-26T05:00:00.000Z", "showId": "68395b407f6329be2bb45bd9" }
    ],
    "2025-07-27": [
        { "time": "2025-07-27T01:00:00.000Z", "showId": "68395b407f6329be2bb45bda" },
        { "time": "2025-07-27T03:00:00.000Z", "showId": "68395b407f6329be2bb45bdb" },
        { "time": "2025-07-27T05:00:00.000Z", "showId": "68395b407f6329be2bb45bdc" }
    ]
}

export const dummyDashboardData = {
    "totalBookings": 14,
    "totalRevenue": 1517,
    "totalUser": 5,
    "activeShows": [
        {
            "_id": "68352363e96d99513e4221a4",
            "movie": dummyShowsData[0],
            "showDateTime": "2025-06-30T02:30:00.000Z",
            "showPrice": 59,
            "occupiedSeats": {
                "A1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "B1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "C1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok"
            },
        },
        {
            "_id": "6835238fe96d99513e4221a8",
            "movie": dummyShowsData[1],
            "showDateTime": "2025-06-30T15:30:00.000Z",
            "showPrice": 81,
            "occupiedSeats": {},
        },
        {
            "_id": "6835238fe96d99513e4221a9",
            "movie": dummyShowsData[2],
            "showDateTime": "2025-06-30T03:30:00.000Z",
            "showPrice": 81,
            "occupiedSeats": {},
        },
        {
            "_id": "6835238fe96d99513e4221aa",
            "movie": dummyShowsData[3],
            "showDateTime": "2025-07-15T16:30:00.000Z",
            "showPrice": 81,
            "occupiedSeats": {
                "A1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "A2": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "A3": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "A4": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok"
            },
        },
        {
            "_id": "683682072b5989c29fc6dc0d",
            "movie": dummyShowsData[4],
            "showDateTime": "2025-06-05T15:30:00.000Z",
            "showPrice": 49,
            "occupiedSeats": {
                "A1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "A2": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "A3": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "B1": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "B2": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok",
                "B3": "user_2xO4XPCgWWwWq9EHuQxc5UWqIok"
            },
            "__v": 0
        },
        {
            "_id": "68380044686d454f2116b39a",
            "movie": dummyShowsData[5],
            "showDateTime": "2025-06-20T16:00:00.000Z",
            "showPrice": 79,
            "occupiedSeats": {
                "A1": "user_2xl7eCSUHddibk5lRxfOtw9RMwX",
                "A2": "user_2xl7eCSUHddibk5lRxfOtw9RMwX"
            }
        }
    ]
}


export const dummyBookingData = [
    {
        "_id": "68396334fb83252d82e17295",
        "user": { "name": "GreatStack", },
        "show": {
            _id: "68352363e96d99513e4221a4",
            movie: dummyShowsData[0],
            showDateTime: "2025-06-30T02:30:00.000Z",
            showPrice: 59,
        },
        "amount": 98,
        "bookedSeats": ["D1", "D2"],
        "isPaid": false,
    },
    {
        "_id": "68396334fb83252d82e17295",
        "user": { "name": "GreatStack", },
        "show": {
            _id: "68352363e96d99513e4221a4",
            movie: dummyShowsData[0],
            showDateTime: "2025-06-30T02:30:00.000Z",
            showPrice: 59,
        },
        "amount": 49,
        "bookedSeats": ["A1"],
        "isPaid": true,
    },
    {
        "_id": "68396334fb83252d82e17295",
        "user": { "name": "GreatStack", },
        "show": {
            _id: "68352363e96d99513e4221a4",
            movie: dummyShowsData[0],
            showDateTime: "2025-06-30T02:30:00.000Z",
            showPrice: 59,
        },
        "amount": 147,
        "bookedSeats": ["A1", "A2", "A3"],
        "isPaid": true,
    },
]