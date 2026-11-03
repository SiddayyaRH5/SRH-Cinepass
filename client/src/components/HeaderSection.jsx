import React, { use } from 'react'
import { CalendarIcon, ClockIcon, ArrowRight } from 'lucide-react'
import { assets } from '../assets/assets'
import { useNavigate } from 'react-router-dom'

const HeaderSection = () => {

    const navigate = useNavigate();
    return (

        <div className='flex flex-col items-start justify-center 
      gap-4 px-6 md:px-16 lg:px-36 bg-[url("/background.png")] 
      bg-cover bg-center h-screen'>

            <h1 className='text-5xl md:text-[70px] md:leading-[4.5rem] font-semibold max-w-110 mt-20'>
                Welcome to CinePass
            </h1>
            <h2 className='text-2xl md:text-1xl text-blue-300 font-semibold'>Your Movie, Your Time, Your Seat</h2>
            <div className='flex items-center gap-4 text-gray-300'>
                <span>Action | Adventure | Comedy | Drama | Horror</span>

                <div className='flex items-center gap-2'>
                    <CalendarIcon className='w-5 h-5' /> 2025
                </div>

                <div className='flex items-center gap-2'>
                    <ClockIcon className='w-5 h-5' /> 2h 35m
                </div>
            </div>

            <p className='text-gray-300 max-w-2xl leading-relaxed'>
                Your One-Stop Solution For All Movie Bookings
                <br />
                Each year brings new movie trailers that raise excitement among fans.
                This year too, big Bollywood releases with star-studded casts are on the way.
                Don’t miss the thrill—book your tickets early and enjoy the magic of cinema with a lively crowd near you!
            </p>

            <button onClick={() => navigate('/movies')} className='flex items-center gap-1 
                px-6 py-3 text-sm bg-gradient-to-r from-blue-600 to-cyan-400 
                hover:from-blue-700 hover:to-cyan-500  transition rounded-full 
             font-medium cursor-pointer'>
                Explore Movies
                <ArrowRight className='w-5 h-5' />
            </button>

        </div>
    )
}

export default HeaderSection
