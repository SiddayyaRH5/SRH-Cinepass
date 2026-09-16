import React, {
  createContext,
  useContext,
  useState
} from 'react';

const LocationContext = createContext();

export const LocationProvider = ({ children }) => {

  const [selectedLocation, setSelectedLocation] = useState(() => {

    const savedLocation =
      localStorage.getItem('cinepassLocation');

    if (!savedLocation) {
      return null;
    }

    try {

      return JSON.parse(savedLocation);

    } catch (error) {

      // Remove old-format location data
      localStorage.removeItem('cinepassLocation');

      return null;
    }
  });

  const selectLocation = (location) => {

    setSelectedLocation(location);

    localStorage.setItem(
      'cinepassLocation',
      JSON.stringify(location)
    );
  };

  const clearLocation = () => {

    setSelectedLocation(null);

    localStorage.removeItem('cinepassLocation');
  };

  return (
    <LocationContext.Provider
      value={{
        selectedLocation,
        selectLocation,
        clearLocation
      }}
    >
      {children}
    </LocationContext.Provider>
  );
};

export const useLocationContext = () => {

  const context = useContext(LocationContext);

  if (!context) {
    throw new Error(
      'useLocationContext must be used inside LocationProvider'
    );
  }

  return context;
};