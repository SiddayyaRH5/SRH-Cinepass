import { ArrowRight, Group, ShowerHead } from 'lucide-react'
import React, { use } from 'react'
import { useNavigate } from 'react-router-dom'
import BlurCircle from './BlurCircle';
import { dummyShowsData } from '../assets/assets';
import MovieCard from './MovieCard';

const FaturedSection = () => {
    const navigate = useNavigate();
    return (
        <div className='px-6 md:px-16 lg:px-24 xl:px-24 overflow-hidden'>

            <div className='relative flex items-center justify-between pt-20 pb-10'>
                <BlurCircle top='40px' right='-80px' />
                <p className='text-gray-300 text-3xl font-semibold'>Now Showing</p>
                <button onClick={() => navigate('/movies')} 
                className='group flex items-center gap-2 text-1xl font-semibold text-gray-300 cursor-pointer ' >
                    View All
                    <ArrowRight className='group-hover:translate-x-0.5 transition w-4.5 h-4.5' />
                </button>
            </div>
            <div className='flex flex-wrap max-sm:justify-center gap-8 mt-8'>
                {dummyShowsData.slice(0,4).map((Show) => (
                    <MovieCard key={Show._id} movie={Show} />
                ))}
            </div>

            <div className='flex justify-center mt-20'>
                <button onClick={() => { navigate('/movies'); scrollTo(0, 0); }} className='py-3 px-10 text-sm bg-gradient-to-r from-blue-600 to-cyan-400 
                hover:from-blue-700 hover:to-cyan-500 
                transition rounded-md font-medium cursor-pointer'>
                    Show More
                </button>
            </div>

    </div>
  )
}


export default FaturedSection
