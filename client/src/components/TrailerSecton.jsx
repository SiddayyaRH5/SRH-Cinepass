import React, { useState } from 'react'
import { dummyTrailers } from '../assets/assets'
import BlurCircle from './BlurCircle'
import { PlayCircleIcon } from 'lucide-react'

const TrailerSection = () => {
  const [selectedTrailer, setSelectedTrailer] = useState(dummyTrailers[0]);

  // Convert normal YouTube link to embed link
  const getEmbedUrl = (url) => {
    const videoId = url.includes("watch?v=")
      ? url.split("v=")[1]?.split("&")[0]
      : url.split("youtu.be/")[1]?.split("?")[0]; // ✅ supports youtu.be links too
    return `https://www.youtube.com/embed/${videoId}`;
  };

  return (
    <div className='px-4 sm:px-8 md:px-16 lg:px-24 xl:px-44 py-12 md:py-20 overflow-hidden'>
      <p className='text-gray-300 text-2xl md:text-3xl font-semibold text-center'>Trailers</p>

      {/* Main Video Player */}
      <div className="relative mt-6 max-w-5xl mx-auto">
        <BlurCircle top='-100px' right='-100px' />

        {/* Responsive iframe wrapper */}
        <div className="relative w-full pt-[56.25%]">
          <iframe
            className='absolute top-0 left-0 w-full h-full rounded-lg shadow-lg'
            src={getEmbedUrl(selectedTrailer.videoUrl)}
            title="YouTube video player"
            frameBorder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          ></iframe>
        </div>
      </div>

      {/* Thumbnails */}
      <div className='group grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 md:gap-8 mt-8 max-w-3xl mx-auto'>
        {dummyTrailers.map((trailer) => (
          <div
            key={trailer.image}
            className='relative hover:-translate-y-1 duration-300 transition cursor-pointer'
            onClick={() => setSelectedTrailer(trailer)} // ✅ Corrected
          >
            <img
              src={trailer.image}
              alt="trailer"
              className='rounded-lg w-full h-full object-cover brightness-75'
            />
            <PlayCircleIcon
              strokeWidth={1.6}
              className="absolute top-1/2 left-1/2 w-8 h-8 md:w-12 md:h-12 text-white transform -translate-x-1/2 -translate-y-1/2 drop-shadow-lg"
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default TrailerSection;
