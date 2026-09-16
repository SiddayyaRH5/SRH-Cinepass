import React from "react";
import { Route, Routes, useLocation } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import Home from "./pages/Home";
import Movies from "./pages/Movies";
import MovieDetails from "./pages/MovieDetails";
import MyBookings from "./pages/MyBookings";
import SeatLayout from "./pages/SeatLayout";
import Favorite from "./pages/Favorite";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Theaters from "./pages/Theaters";
import TheatreShows from "./pages/TheatreShows";
import Layout from "./pages/Admin/Layout";
import Dashboard from "./pages/Admin/Dashboard";
import ListShows from "./pages/Admin/ListShows";
import ListofBookings from "./pages/Admin/ListofBookings";
import AddShows from "./pages/Admin/AddShows";
import { LocationProvider } from "./lib/LocationContext";

function Shell() {
  const { pathname } = useLocation();
  const admin = pathname.startsWith("/admin");
  return <>
    {!admin && <Navbar/>}
    <Routes>
      <Route path="/" element={<Home/>}/>
      <Route path="/login" element={<Login/>}/>
      <Route path="/signup" element={<Signup/>}/>
      <Route path="/movies" element={<Movies/>}/>
      <Route path="/movie/:id" element={<MovieDetails/>}/>
      <Route path="/theaters" element={<Theaters/>}/>
      <Route path="/theatre/:theatreId" element={<TheatreShows/>}/>
      <Route path="/show/:id" element={<SeatLayout/>}/>
      <Route path="/My-Bookings" element={<MyBookings/>}/>
      <Route path="/favorite" element={<Favorite/>}/>
      <Route path="/admin" element={<Layout/>}>
        <Route index element={<Dashboard/>}/>
        <Route path="list-shows" element={<ListShows/>}/>
        <Route path="list-bookings" element={<ListofBookings/>}/>
        <Route path="add-shows" element={<AddShows/>}/>
      </Route>
      <Route path="*" element={<Home/>}/>
    </Routes>
    {!admin && <Footer/>}
  </>;
}

export default function App() {
  return <LocationProvider>
    <Toaster position="top-right" toastOptions={{duration:3500, style:{background:"#111722",color:"#fff",border:"1px solid rgba(255,255,255,.1)",borderRadius:"14px"}}}/>
    <Shell/>
  </LocationProvider>;
}
