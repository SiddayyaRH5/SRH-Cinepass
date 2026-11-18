/**
 * Centralized catalogue of Indian cities/towns used by the city selector.
 *
 * Each entry: { city, state }. Duplicates are removed and the list is sorted
 * alphabetically by city name (popular cities are surfaced separately via
 * POPULAR_CITIES). Keeping this data out of the UI component makes it easy to
 * maintain and reuse.
 */

export const POPULAR_CITIES = [
  "Mumbai",
  "Delhi",
  "Bengaluru",
  "Hyderabad",
  "Pune",
  "Kolkata",
  "Chennai",
  "Ahmedabad",
  "Kochi",
];

const RAW_CITIES = [
  // Andhra Pradesh
  ["Visakhapatnam", "Andhra Pradesh"],
  ["Vijayawada", "Andhra Pradesh"],
  ["Amaravati", "Andhra Pradesh"],
  ["Guntur", "Andhra Pradesh"],
  ["Nellore", "Andhra Pradesh"],
  ["Kurnool", "Andhra Pradesh"],
  ["Rajahmundry", "Andhra Pradesh"],
  ["Tirupati", "Andhra Pradesh"],
  ["Kakinada", "Andhra Pradesh"],
  ["Kadapa", "Andhra Pradesh"],
  ["Anantapur", "Andhra Pradesh"],
  ["Eluru", "Andhra Pradesh"],
  ["Ongole", "Andhra Pradesh"],
  ["Nandyal", "Andhra Pradesh"],
  ["Machilipatnam", "Andhra Pradesh"],
  ["Chittoor", "Andhra Pradesh"],
  ["Vizianagaram", "Andhra Pradesh"],

  // Arunachal Pradesh
  ["Itanagar", "Arunachal Pradesh"],
  ["Naharlagun", "Arunachal Pradesh"],
  ["Pasighat", "Arunachal Pradesh"],
  ["Tawang", "Arunachal Pradesh"],

  // Assam
  ["Guwahati", "Assam"],
  ["Silchar", "Assam"],
  ["Dibrugarh", "Assam"],
  ["Jorhat", "Assam"],
  ["Nagaon", "Assam"],
  ["Tinsukia", "Assam"],
  ["Tezpur", "Assam"],
  ["Bongaigaon", "Assam"],
  ["Karimganj", "Assam"],
  ["Goalpara", "Assam"],
  ["North Lakhimpur", "Assam"],

  // Bihar
  ["Patna", "Bihar"],
  ["Gaya", "Bihar"],
  ["Bhagalpur", "Bihar"],
  ["Muzaffarpur", "Bihar"],
  ["Purnia", "Bihar"],
  ["Darbhanga", "Bihar"],
  ["Bihar Sharif", "Bihar"],
  ["Arrah", "Bihar"],
  ["Begusarai", "Bihar"],
  ["Katihar", "Bihar"],
  ["Munger", "Bihar"],
  ["Chhapra", "Bihar"],
  ["Saharsa", "Bihar"],
  ["Sasaram", "Bihar"],
  ["Hajipur", "Bihar"],
  ["Dehri", "Bihar"],
  ["Siwan", "Bihar"],
  ["Motihari", "Bihar"],
  ["Nawada", "Bihar"],

  // Chhattisgarh
  ["Raipur", "Chhattisgarh"],
  ["Bhilai", "Chhattisgarh"],
  ["Bilaspur", "Chhattisgarh"],
  ["Korba", "Chhattisgarh"],
  ["Durg", "Chhattisgarh"],
  ["Rajnandgaon", "Chhattisgarh"],
  ["Jagdalpur", "Chhattisgarh"],
  ["Raigarh", "Chhattisgarh"],
  ["Ambikapur", "Chhattisgarh"],

  // Delhi
  ["Delhi", "Delhi"],
  ["New Delhi", "Delhi"],

  // Goa
  ["Panaji", "Goa"],
  ["Margao", "Goa"],
  ["Vasco da Gama", "Goa"],
  ["Mapusa", "Goa"],
  ["Ponda", "Goa"],

  // Gujarat
  ["Ahmedabad", "Gujarat"],
  ["Surat", "Gujarat"],
  ["Vadodara", "Gujarat"],
  ["Rajkot", "Gujarat"],
  ["Bhavnagar", "Gujarat"],
  ["Jamnagar", "Gujarat"],
  ["Junagadh", "Gujarat"],
  ["Gandhinagar", "Gujarat"],
  ["Anand", "Gujarat"],
  ["Navsari", "Gujarat"],
  ["Morbi", "Gujarat"],
  ["Nadiad", "Gujarat"],
  ["Surendranagar", "Gujarat"],
  ["Bharuch", "Gujarat"],
  ["Mehsana", "Gujarat"],
  ["Bhuj", "Gujarat"],
  ["Porbandar", "Gujarat"],
  ["Palanpur", "Gujarat"],
  ["Vapi", "Gujarat"],
  ["Godhra", "Gujarat"],

  // Haryana
  ["Gurugram", "Haryana"],
  ["Faridabad", "Haryana"],
  ["Panipat", "Haryana"],
  ["Ambala", "Haryana"],
  ["Karnal", "Haryana"],
  ["Hisar", "Haryana"],
  ["Rohtak", "Haryana"],
  ["Sonipat", "Haryana"],
  ["Yamunanagar", "Haryana"],
  ["Panchkula", "Haryana"],
  ["Bhiwani", "Haryana"],
  ["Sirsa", "Haryana"],
  ["Bahadurgarh", "Haryana"],
  ["Jind", "Haryana"],
  ["Thanesar", "Haryana"],
  ["Kaithal", "Haryana"],
  ["Rewari", "Haryana"],

  // Himachal Pradesh
  ["Shimla", "Himachal Pradesh"],
  ["Dharamshala", "Himachal Pradesh"],
  ["Mandi", "Himachal Pradesh"],
  ["Solan", "Himachal Pradesh"],
  ["Kullu", "Himachal Pradesh"],
  ["Manali", "Himachal Pradesh"],
  ["Hamirpur", "Himachal Pradesh"],
  ["Una", "Himachal Pradesh"],
  ["Bilaspur", "Himachal Pradesh"],
  ["Chamba", "Himachal Pradesh"],

  // Jharkhand
  ["Ranchi", "Jharkhand"],
  ["Jamshedpur", "Jharkhand"],
  ["Dhanbad", "Jharkhand"],
  ["Bokaro", "Jharkhand"],
  ["Deoghar", "Jharkhand"],
  ["Hazaribagh", "Jharkhand"],
  ["Giridih", "Jharkhand"],
  ["Ramgarh", "Jharkhand"],
  ["Phusro", "Jharkhand"],
  ["Medininagar", "Jharkhand"],

  // Karnataka
  ["Bengaluru", "Karnataka"],
  ["Mysuru", "Karnataka"],
  ["Mangaluru", "Karnataka"],
  ["Hubballi", "Karnataka"],
  ["Belagavi", "Karnataka"],
  ["Shivamogga", "Karnataka"],
  ["Tumakuru", "Karnataka"],
  ["Davanagere", "Karnataka"],
  ["Hassan", "Karnataka"],
  ["Udupi", "Karnataka"],
  ["Ballari", "Karnataka"],
  ["Vijayapura", "Karnataka"],
  ["Raichur", "Karnataka"],
  ["Bidar", "Karnataka"],
  ["Gadag", "Karnataka"],
  ["Haveri", "Karnataka"],
  ["Chitradurga", "Karnataka"],
  ["Chikkamagaluru", "Karnataka"],
  ["Mandya", "Karnataka"],
  ["Kolar", "Karnataka"],
  ["Chikkaballapur", "Karnataka"],
  ["Bagalkot", "Karnataka"],
  ["Dharwad", "Karnataka"],
  ["Kalaburagi", "Karnataka"],
  ["Karwar", "Karnataka"],
  ["Madikeri", "Karnataka"],
  ["Hospet", "Karnataka"],
  ["Gangavathi", "Karnataka"],

  // Kerala
  ["Thiruvananthapuram", "Kerala"],
  ["Kochi", "Kerala"],
  ["Kozhikode", "Kerala"],
  ["Kollam", "Kerala"],
  ["Thrissur", "Kerala"],
  ["Kannur", "Kerala"],
  ["Alappuzha", "Kerala"],
  ["Palakkad", "Kerala"],
  ["Kottayam", "Kerala"],
  ["Malappuram", "Kerala"],
  ["Kasaragod", "Kerala"],
  ["Pathanamthitta", "Kerala"],
  ["Idukki", "Kerala"],
  ["Wayanad", "Kerala"],

  // Madhya Pradesh
  ["Bhopal", "Madhya Pradesh"],
  ["Indore", "Madhya Pradesh"],
  ["Gwalior", "Madhya Pradesh"],
  ["Jabalpur", "Madhya Pradesh"],
  ["Ujjain", "Madhya Pradesh"],
  ["Sagar", "Madhya Pradesh"],
  ["Dewas", "Madhya Pradesh"],
  ["Satna", "Madhya Pradesh"],
  ["Ratlam", "Madhya Pradesh"],
  ["Rewa", "Madhya Pradesh"],
  ["Katni", "Madhya Pradesh"],
  ["Singrauli", "Madhya Pradesh"],
  ["Burhanpur", "Madhya Pradesh"],
  ["Khandwa", "Madhya Pradesh"],
  ["Bhind", "Madhya Pradesh"],
  ["Chhindwara", "Madhya Pradesh"],
  ["Guna", "Madhya Pradesh"],
  ["Shivpuri", "Madhya Pradesh"],
  ["Vidisha", "Madhya Pradesh"],
  ["Chhatarpur", "Madhya Pradesh"],
  ["Damoh", "Madhya Pradesh"],
  ["Mandsaur", "Madhya Pradesh"],
  ["Neemuch", "Madhya Pradesh"],
  ["Pithampur", "Madhya Pradesh"],
  ["Hoshangabad", "Madhya Pradesh"],

  // Maharashtra
  ["Mumbai", "Maharashtra"],
  ["Pune", "Maharashtra"],
  ["Nagpur", "Maharashtra"],
  ["Nashik", "Maharashtra"],
  ["Thane", "Maharashtra"],
  ["Aurangabad", "Maharashtra"],
  ["Solapur", "Maharashtra"],
  ["Amravati", "Maharashtra"],
  ["Kolhapur", "Maharashtra"],
  ["Navi Mumbai", "Maharashtra"],
  ["Sangli", "Maharashtra"],
  ["Jalgaon", "Maharashtra"],
  ["Akola", "Maharashtra"],
  ["Latur", "Maharashtra"],
  ["Ahmednagar", "Maharashtra"],
  ["Chandrapur", "Maharashtra"],
  ["Parbhani", "Maharashtra"],
  ["Jalna", "Maharashtra"],
  ["Bhiwandi", "Maharashtra"],
  ["Satara", "Maharashtra"],
  ["Dhule", "Maharashtra"],
  ["Nanded", "Maharashtra"],
  ["Yavatmal", "Maharashtra"],
  ["Osmanabad", "Maharashtra"],
  ["Beed", "Maharashtra"],
  ["Wardha", "Maharashtra"],
  ["Ratnagiri", "Maharashtra"],
  ["Alibag", "Maharashtra"],

  // Manipur
  ["Imphal", "Manipur"],
  ["Thoubal", "Manipur"],
  ["Bishnupur", "Manipur"],

  // Meghalaya
  ["Shillong", "Meghalaya"],
  ["Tura", "Meghalaya"],
  ["Nongstoin", "Meghalaya"],

  // Mizoram
  ["Aizawl", "Mizoram"],
  ["Lunglei", "Mizoram"],
  ["Champhai", "Mizoram"],

  // Nagaland
  ["Kohima", "Nagaland"],
  ["Dimapur", "Nagaland"],
  ["Mokokchung", "Nagaland"],

  // Odisha
  ["Bhubaneswar", "Odisha"],
  ["Cuttack", "Odisha"],
  ["Rourkela", "Odisha"],
  ["Berhampur", "Odisha"],
  ["Sambalpur", "Odisha"],
  ["Puri", "Odisha"],
  ["Balasore", "Odisha"],
  ["Bhadrak", "Odisha"],
  ["Baripada", "Odisha"],
  ["Jharsuguda", "Odisha"],
  ["Jeypore", "Odisha"],
  ["Angul", "Odisha"],
  ["Dhenkanal", "Odisha"],

  // Punjab
  ["Ludhiana", "Punjab"],
  ["Amritsar", "Punjab"],
  ["Jalandhar", "Punjab"],
  ["Patiala", "Punjab"],
  ["Bathinda", "Punjab"],
  ["Mohali", "Punjab"],
  ["Pathankot", "Punjab"],
  ["Hoshiarpur", "Punjab"],
  ["Moga", "Punjab"],
  ["Batala", "Punjab"],
  ["Abohar", "Punjab"],
  ["Malerkotla", "Punjab"],
  ["Khanna", "Punjab"],
  ["Phagwara", "Punjab"],
  ["Muktsar", "Punjab"],
  ["Barnala", "Punjab"],
  ["Firozpur", "Punjab"],
  ["Kapurthala", "Punjab"],

  // Rajasthan
  ["Jaipur", "Rajasthan"],
  ["Jodhpur", "Rajasthan"],
  ["Udaipur", "Rajasthan"],
  ["Kota", "Rajasthan"],
  ["Ajmer", "Rajasthan"],
  ["Bikaner", "Rajasthan"],
  ["Alwar", "Rajasthan"],
  ["Bharatpur", "Rajasthan"],
  ["Sikar", "Rajasthan"],
  ["Pali", "Rajasthan"],
  ["Sri Ganganagar", "Rajasthan"],
  ["Bhilwara", "Rajasthan"],
  ["Hanumangarh", "Rajasthan"],
  ["Jhunjhunu", "Rajasthan"],
  ["Nagaur", "Rajasthan"],
  ["Churu", "Rajasthan"],
  ["Jaisalmer", "Rajasthan"],
  ["Barmer", "Rajasthan"],
  ["Tonk", "Rajasthan"],
  ["Beawar", "Rajasthan"],

  // Sikkim
  ["Gangtok", "Sikkim"],
  ["Namchi", "Sikkim"],
  ["Gyalshing", "Sikkim"],

  // Tamil Nadu
  ["Chennai", "Tamil Nadu"],
  ["Coimbatore", "Tamil Nadu"],
  ["Madurai", "Tamil Nadu"],
  ["Tiruchirappalli", "Tamil Nadu"],
  ["Salem", "Tamil Nadu"],
  ["Tirunelveli", "Tamil Nadu"],
  ["Tiruppur", "Tamil Nadu"],
  ["Vellore", "Tamil Nadu"],
  ["Erode", "Tamil Nadu"],
  ["Thoothukudi", "Tamil Nadu"],
  ["Dindigul", "Tamil Nadu"],
  ["Thanjavur", "Tamil Nadu"],
  ["Kanchipuram", "Tamil Nadu"],
  ["Nagercoil", "Tamil Nadu"],
  ["Karur", "Tamil Nadu"],
  ["Hosur", "Tamil Nadu"],
  ["Cuddalore", "Tamil Nadu"],
  ["Kumbakonam", "Tamil Nadu"],
  ["Tiruvannamalai", "Tamil Nadu"],
  ["Pollachi", "Tamil Nadu"],
  ["Rajapalayam", "Tamil Nadu"],
  ["Pudukkottai", "Tamil Nadu"],
  ["Nagapattinam", "Tamil Nadu"],
  ["Udhagamandalam", "Tamil Nadu"],

  // Telangana
  ["Hyderabad", "Telangana"],
  ["Warangal", "Telangana"],
  ["Nizamabad", "Telangana"],
  ["Karimnagar", "Telangana"],
  ["Khammam", "Telangana"],
  ["Ramagundam", "Telangana"],
  ["Mahbubnagar", "Telangana"],
  ["Nalgonda", "Telangana"],
  ["Adilabad", "Telangana"],
  ["Suryapet", "Telangana"],
  ["Miryalaguda", "Telangana"],
  ["Siddipet", "Telangana"],
  ["Jagtial", "Telangana"],

  // Tripura
  ["Agartala", "Tripura"],
  ["Udaipur", "Tripura"],
  ["Dharmanagar", "Tripura"],

  // Uttar Pradesh
  ["Lucknow", "Uttar Pradesh"],
  ["Kanpur", "Uttar Pradesh"],
  ["Ghaziabad", "Uttar Pradesh"],
  ["Agra", "Uttar Pradesh"],
  ["Meerut", "Uttar Pradesh"],
  ["Varanasi", "Uttar Pradesh"],
  ["Prayagraj", "Uttar Pradesh"],
  ["Noida", "Uttar Pradesh"],
  ["Bareilly", "Uttar Pradesh"],
  ["Aligarh", "Uttar Pradesh"],
  ["Moradabad", "Uttar Pradesh"],
  ["Saharanpur", "Uttar Pradesh"],
  ["Gorakhpur", "Uttar Pradesh"],
  ["Firozabad", "Uttar Pradesh"],
  ["Jhansi", "Uttar Pradesh"],
  ["Muzaffarnagar", "Uttar Pradesh"],
  ["Mathura", "Uttar Pradesh"],
  ["Rampur", "Uttar Pradesh"],
  ["Shahjahanpur", "Uttar Pradesh"],
  ["Farrukhabad", "Uttar Pradesh"],
  ["Maunath Bhanjan", "Uttar Pradesh"],
  ["Hapur", "Uttar Pradesh"],
  ["Ayodhya", "Uttar Pradesh"],
  ["Etawah", "Uttar Pradesh"],
  ["Mirzapur", "Uttar Pradesh"],
  ["Bulandshahr", "Uttar Pradesh"],
  ["Sambhal", "Uttar Pradesh"],
  ["Amroha", "Uttar Pradesh"],
  ["Hardoi", "Uttar Pradesh"],
  ["Fatehpur", "Uttar Pradesh"],
  ["Rae Bareli", "Uttar Pradesh"],
  ["Orai", "Uttar Pradesh"],
  ["Sitapur", "Uttar Pradesh"],
  ["Bahraich", "Uttar Pradesh"],
  ["Modinagar", "Uttar Pradesh"],
  ["Unnao", "Uttar Pradesh"],
  ["Jaunpur", "Uttar Pradesh"],
  ["Lakhimpur", "Uttar Pradesh"],
  ["Hathras", "Uttar Pradesh"],
  ["Banda", "Uttar Pradesh"],
  ["Pilibhit", "Uttar Pradesh"],
  ["Barabanki", "Uttar Pradesh"],
  ["Khurja", "Uttar Pradesh"],
  ["Gonda", "Uttar Pradesh"],
  ["Shahabad", "Uttar Pradesh"],
  ["Greater Noida", "Uttar Pradesh"],

  // Uttarakhand
  ["Dehradun", "Uttarakhand"],
  ["Haridwar", "Uttarakhand"],
  ["Roorkee", "Uttarakhand"],
  ["Haldwani", "Uttarakhand"],
  ["Rudrapur", "Uttarakhand"],
  ["Kashipur", "Uttarakhand"],
  ["Rishikesh", "Uttarakhand"],
  ["Nainital", "Uttarakhand"],
  ["Mussoorie", "Uttarakhand"],
  ["Pithoragarh", "Uttarakhand"],

  // West Bengal
  ["Kolkata", "West Bengal"],
  ["Howrah", "West Bengal"],
  ["Durgapur", "West Bengal"],
  ["Asansol", "West Bengal"],
  ["Siliguri", "West Bengal"],
  ["Bardhaman", "West Bengal"],
  ["Malda", "West Bengal"],
  ["Baharampur", "West Bengal"],
  ["Habra", "West Bengal"],
  ["Kharagpur", "West Bengal"],
  ["Shantipur", "West Bengal"],
  ["Dankuni", "West Bengal"],
  ["Dhulian", "West Bengal"],
  ["Ranaghat", "West Bengal"],
  ["Haldia", "West Bengal"],
  ["Raiganj", "West Bengal"],
  ["Krishnanagar", "West Bengal"],
  ["Nabadwip", "West Bengal"],
  ["Medinipur", "West Bengal"],
  ["Jalpaiguri", "West Bengal"],
  ["Balurghat", "West Bengal"],
  ["Bankura", "West Bengal"],
  ["Chinsurah", "West Bengal"],
  ["Purulia", "West Bengal"],
  ["Cooch Behar", "West Bengal"],

  // Union Territories
  ["Chandigarh", "Chandigarh"],
  ["Puducherry", "Puducherry"],
  ["Karaikal", "Puducherry"],
  ["Port Blair", "Andaman and Nicobar Islands"],
  ["Leh", "Ladakh"],
  ["Kargil", "Ladakh"],
  ["Srinagar", "Jammu and Kashmir"],
  ["Jammu", "Jammu and Kashmir"],
  ["Anantnag", "Jammu and Kashmir"],
  ["Baramulla", "Jammu and Kashmir"],
  ["Udhampur", "Jammu and Kashmir"],
  ["Kathua", "Jammu and Kashmir"],
  ["Silvassa", "Dadra and Nagar Haveli and Daman and Diu"],
  ["Daman", "Dadra and Nagar Haveli and Daman and Diu"],
  ["Diu", "Dadra and Nagar Haveli and Daman and Diu"],
  ["Kavaratti", "Lakshadweep"],
];

// Deduplicate by city name (case-insensitive), keep first occurrence.
const seen = new Set();
export const INDIAN_CITIES = RAW_CITIES
  .map(([city, state]) => ({ city, state, country: "India" }))
  .filter((entry) => {
    const key = entry.city.trim().toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  })
  .sort((a, b) => a.city.localeCompare(b.city));

export const CITY_NAMES = INDIAN_CITIES.map((c) => c.city);
export const CITY_BY_NAME = new Map(
  INDIAN_CITIES.map((c) => [c.city.toLowerCase(), c])
);

/**
 * Common alternate spellings / former names mapped to their canonical city.
 * Used by search and by reverse-geocoding so "Bangalore", "Bombay", "Trivandrum"
 * etc. still resolve to a supported city.
 */
export const CITY_ALIASES = {
  bangalore: "Bengaluru",
  bengalooru: "Bengaluru",
  mysore: "Mysuru",
  mangalore: "Mangaluru",
  hubli: "Hubballi",
  belgaum: "Belagavi",
  shimoga: "Shivamogga",
  tumkur: "Tumakuru",
  davangere: "Davanagere",
  gulbarga: "Kalaburagi",
  bijapur: "Vijayapura",
  bellary: "Ballari",
  chikmagalur: "Chikkamagaluru",
  mercara: "Madikeri",
  bombay: "Mumbai",
  poona: "Pune",
  madras: "Chennai",
  calcutta: "Kolkata",
  cochin: "Kochi",
  trivandrum: "Thiruvananthapuram",
  calicut: "Kozhikode",
  quilon: "Kollam",
  trichur: "Thrissur",
  cannanore: "Kannur",
  alleppey: "Alappuzha",
  pondicherry: "Puducherry",
  trichy: "Tiruchirappalli",
  tiruchi: "Tiruchirappalli",
  tuticorin: "Thoothukudi",
  nellai: "Tirunelveli",
  ooty: "Udhagamandalam",
  vizag: "Visakhapatnam",
  bezawada: "Vijayawada",
  allahabad: "Prayagraj",
  banaras: "Varanasi",
  benares: "Varanasi",
  kashi: "Varanasi",
  gurgaon: "Gurugram",
  faizabad: "Ayodhya",
  cawnpore: "Kanpur",
  nasik: "Nashik",
  baroda: "Vadodara",
  simla: "Shimla",
  dharamsala: "Dharamshala",
  bhubaneshwar: "Bhubaneswar",
  jullundur: "Jalandhar",
  panjim: "Panaji",
  madgaon: "Margao",
  vasco: "Vasco da Gama",
  chhatrapatisambhajinagar: "Aurangabad",
  dharashiv: "Osmanabad",
  ahilyanagar: "Ahmednagar",
  "sri vijaya puram": "Port Blair",
  delhincr: "Delhi",
};

const normalize = (value) =>
  String(value || "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");

// Build a per-city search haystack that includes aliases so partial and
// alternate-spelling searches resolve to the right city.
const ALIASES_BY_CITY = new Map();

Object.entries(CITY_ALIASES).forEach(([alias, canonical]) => {
  const key = canonical.toLowerCase();
  const list = ALIASES_BY_CITY.get(key) || [];
  list.push(alias);
  ALIASES_BY_CITY.set(key, list);
});

export const CITY_SEARCH_INDEX = INDIAN_CITIES.map((entry) => ({
  ...entry,
  search: normalize(
    [
      entry.city,
      entry.state,
      ...(ALIASES_BY_CITY.get(entry.city.toLowerCase()) || []),
    ].join(" ")
  ),
}));

export const searchIndianCities = (query) => {
  const needle = normalize(query);

  if (!needle) {
    return INDIAN_CITIES;
  }

  return CITY_SEARCH_INDEX.filter((entry) =>
    entry.search.includes(needle)
  ).map(({ city, state, country }) => ({ city, state, country }));
};

export const findIndianCity = (name) => {
  const normalized = normalize(name);

  if (!normalized) {
    return null;
  }

  const direct = INDIAN_CITIES.find(
    (entry) => normalize(entry.city) === normalized
  );

  if (direct) {
    return direct;
  }

  const alias = Object.entries(CITY_ALIASES).find(
    ([key]) => normalize(key) === normalized
  );

  if (alias) {
    return (
      INDIAN_CITIES.find(
        (entry) => entry.city.toLowerCase() === alias[1].toLowerCase()
      ) || null
    );
  }

  // Reverse geocoding often returns a broader label such as
  // "Bangalore Urban" or "Mumbai Suburban". Prefer the longest prefix match
  // so the most specific supported city wins.
  const prefixMatches = [
    ...INDIAN_CITIES.map((entry) => [normalize(entry.city), entry.city]),
    ...Object.entries(CITY_ALIASES).map(([key, canonical]) => [
      normalize(key),
      canonical,
    ]),
  ]
    .filter(([key]) => key && normalized.startsWith(key))
    .sort(([a], [b]) => b.length - a.length);

  if (prefixMatches.length > 0) {
    const canonical = prefixMatches[0][1];

    return (
      INDIAN_CITIES.find(
        (entry) => entry.city.toLowerCase() === canonical.toLowerCase()
      ) || null
    );
  }

  return null;
};

export default INDIAN_CITIES;