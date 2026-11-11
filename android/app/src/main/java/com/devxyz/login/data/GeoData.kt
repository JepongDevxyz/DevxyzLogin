package com.devxyz.login.data

/**
 * Curated regions/states + major cities for the Add-proxy country
 * picker (v2.6.0, spec v2: "Row: flag + name + chevron-down (expands
 * states/cities)"). Every entry is a REAL place, hand-curated:
 * country code → regions (states / provinces / prefectures…), each
 * with 1–4 of its real major cities.
 *
 * This is PICKER data only — a preference the user expresses. The
 * exit city a profile actually gets is whatever the server's real
 * proxy check measures afterwards; nothing here is ever presented
 * as a measured location.
 *
 * Coverage: the built-in catalog countries plus the major picker
 * markets. Countries absent here stay country-level in the picker
 * (the row says so honestly).
 */
data class GeoRegion(val name: String, val cities: List<String>)

object GeoData {

    private fun r(name: String, vararg cities: String) = GeoRegion(name, cities.toList())

    val byCountry: Map<String, List<GeoRegion>> = mapOf(

        // ---------------------------------------------------------- US
        // All 50 states (+ DC), 1–3 real major cities each.
        "US" to listOf(
            r("Alabama", "Birmingham", "Huntsville", "Montgomery"),
            r("Alaska", "Anchorage", "Juneau"),
            r("Arizona", "Phoenix", "Tucson", "Mesa"),
            r("Arkansas", "Little Rock", "Fayetteville"),
            r("California", "Los Angeles", "San Francisco", "San Jose"),
            r("Colorado", "Denver", "Colorado Springs", "Boulder"),
            r("Connecticut", "Bridgeport", "New Haven", "Hartford"),
            r("Delaware", "Wilmington", "Dover"),
            r("District of Columbia", "Washington"),
            r("Florida", "Miami", "Orlando", "Tampa"),
            r("Georgia", "Atlanta", "Savannah", "Augusta"),
            r("Hawaii", "Honolulu"),
            r("Idaho", "Boise", "Idaho Falls"),
            r("Illinois", "Chicago", "Springfield", "Naperville"),
            r("Indiana", "Indianapolis", "Fort Wayne", "Evansville"),
            r("Iowa", "Des Moines", "Cedar Rapids"),
            r("Kansas", "Wichita", "Kansas City", "Topeka"),
            r("Kentucky", "Louisville", "Lexington"),
            r("Louisiana", "New Orleans", "Baton Rouge", "Shreveport"),
            r("Maine", "Portland", "Bangor"),
            r("Maryland", "Baltimore", "Annapolis", "Frederick"),
            r("Massachusetts", "Boston", "Worcester", "Springfield"),
            r("Michigan", "Detroit", "Grand Rapids", "Ann Arbor"),
            r("Minnesota", "Minneapolis", "Saint Paul", "Rochester"),
            r("Mississippi", "Jackson", "Gulfport"),
            r("Missouri", "Kansas City", "St. Louis", "Springfield"),
            r("Montana", "Billings", "Missoula", "Bozeman"),
            r("Nebraska", "Omaha", "Lincoln"),
            r("Nevada", "Las Vegas", "Reno", "Henderson"),
            r("New Hampshire", "Manchester", "Nashua"),
            r("New Jersey", "Newark", "Jersey City", "Paterson"),
            r("New Mexico", "Albuquerque", "Santa Fe", "Las Cruces"),
            r("New York", "New York City", "Buffalo", "Rochester"),
            r("North Carolina", "Charlotte", "Raleigh", "Durham"),
            r("North Dakota", "Fargo", "Bismarck"),
            r("Ohio", "Columbus", "Cleveland", "Cincinnati"),
            r("Oklahoma", "Oklahoma City", "Tulsa", "Norman"),
            r("Oregon", "Portland", "Salem", "Eugene"),
            r("Pennsylvania", "Philadelphia", "Pittsburgh", "Allentown"),
            r("Rhode Island", "Providence", "Warwick"),
            r("South Carolina", "Charleston", "Columbia", "Greenville"),
            r("South Dakota", "Sioux Falls", "Rapid City"),
            r("Tennessee", "Nashville", "Memphis", "Knoxville"),
            r("Texas", "Houston", "Dallas", "Austin"),
            r("Utah", "Salt Lake City", "Provo", "Ogden"),
            r("Vermont", "Burlington", "Montpelier"),
            r("Virginia", "Virginia Beach", "Richmond", "Norfolk"),
            r("Washington", "Seattle", "Spokane", "Tacoma"),
            r("West Virginia", "Charleston", "Huntington"),
            r("Wisconsin", "Milwaukee", "Madison", "Green Bay"),
            r("Wyoming", "Cheyenne", "Casper")
        ),

        // ---------------------------------------------------------- PH
        // Key provinces of every region (the picker's region level is
        // the province). Western Visayas → Capiz → Roxas City is the
        // exact resolution her recording showed ("Philippines • Roxas").
        "PH" to listOf(
            // National Capital Region
            r("Metro Manila", "Manila", "Quezon City", "Makati"),
            // Cordillera Administrative Region
            r("Benguet", "Baguio City"),
            // Ilocos Region
            r("Ilocos Norte", "Laoag City"),
            r("Pangasinan", "Dagupan City", "Urdaneta City"),
            // Cagayan Valley
            r("Cagayan", "Tuguegarao City"),
            r("Isabela", "Santiago City", "Ilagan City"),
            // Central Luzon
            r("Bulacan", "Malolos City", "Meycauayan City"),
            r("Nueva Ecija", "Cabanatuan City", "San Jose City"),
            r("Pampanga", "Angeles City", "San Fernando City"),
            r("Tarlac", "Tarlac City"),
            r("Zambales", "Olongapo City"),
            // CALABARZON
            r("Batangas", "Batangas City", "Lipa City"),
            r("Cavite", "Imus City", "Dasmariñas City"),
            r("Laguna", "Calamba City", "Santa Rosa City"),
            r("Quezon", "Lucena City"),
            r("Rizal", "Antipolo City"),
            // MIMAROPA
            r("Oriental Mindoro", "Calapan City"),
            r("Palawan", "Puerto Princesa City"),
            // Bicol Region
            r("Albay", "Legazpi City"),
            r("Camarines Sur", "Naga City"),
            // Western Visayas
            r("Aklan", "Kalibo"),
            r("Capiz", "Roxas City"),
            r("Iloilo", "Iloilo City"),
            r("Negros Occidental", "Bacolod City", "Silay City"),
            // Central Visayas
            r("Bohol", "Tagbilaran City"),
            r("Cebu", "Cebu City", "Lapu-Lapu City", "Mandaue City"),
            r("Negros Oriental", "Dumaguete City"),
            // Eastern Visayas
            r("Leyte", "Tacloban City", "Ormoc City"),
            r("Samar", "Catbalogan City"),
            // Zamboanga Peninsula
            r("Zamboanga City", "Zamboanga City"),
            r("Zamboanga del Norte", "Dipolog City"),
            r("Zamboanga del Sur", "Pagadian City"),
            // Northern Mindanao
            r("Bukidnon", "Malaybalay City", "Valencia City"),
            r("Lanao del Norte", "Iligan City"),
            r("Misamis Oriental", "Cagayan de Oro City"),
            // Davao Region
            r("Davao City", "Davao City"),
            r("Davao del Norte", "Tagum City"),
            r("Davao del Sur", "Digos City"),
            // SOCCSKSARGEN
            r("Cotabato", "Kidapawan City"),
            r("General Santos City", "General Santos City"),
            r("Sarangani", "Alabel"),
            r("South Cotabato", "Koronadal City"),
            r("Sultan Kudarat", "Tacurong City"),
            // Caraga
            r("Agusan del Norte", "Butuan City"),
            r("Surigao del Norte", "Surigao City"),
            // BARMM
            r("Basilan", "Isabela City"),
            r("Cotabato City", "Cotabato City"),
            r("Lanao del Sur", "Marawi City")
        ),

        // ---------------------------------------------------------- CA
        "CA" to listOf(
            r("Alberta", "Calgary", "Edmonton", "Red Deer"),
            r("British Columbia", "Vancouver", "Surrey", "Burnaby"),
            r("Manitoba", "Winnipeg", "Brandon"),
            r("New Brunswick", "Moncton", "Fredericton", "Saint John"),
            r("Newfoundland and Labrador", "St. John's", "Corner Brook"),
            r("Nova Scotia", "Halifax", "Sydney"),
            r("Ontario", "Toronto", "Ottawa", "Mississauga"),
            r("Prince Edward Island", "Charlottetown"),
            r("Quebec", "Montreal", "Quebec City", "Laval"),
            r("Saskatchewan", "Saskatoon", "Regina")
        ),

        // ---------------------------------------------------------- GB
        "GB" to listOf(
            r("London", "London"),
            r("South East", "Brighton", "Southampton", "Oxford"),
            r("East of England", "Cambridge", "Norwich", "Peterborough"),
            r("East Midlands", "Nottingham", "Leicester", "Derby"),
            r("West Midlands", "Birmingham", "Coventry", "Wolverhampton"),
            r("North West", "Manchester", "Liverpool", "Preston"),
            r("North East", "Newcastle upon Tyne", "Sunderland", "Durham"),
            r("Yorkshire and the Humber", "Leeds", "Sheffield", "York"),
            r("South West", "Bristol", "Exeter", "Plymouth"),
            r("Scotland", "Edinburgh", "Glasgow", "Aberdeen"),
            r("Wales", "Cardiff", "Swansea", "Newport"),
            r("Northern Ireland", "Belfast", "Derry")
        ),

        // ---------------------------------------------------------- DE
        "DE" to listOf(
            r("Baden-Württemberg", "Stuttgart", "Mannheim", "Karlsruhe"),
            r("Bavaria", "Munich", "Nuremberg", "Augsburg"),
            r("Berlin", "Berlin"),
            r("Brandenburg", "Potsdam", "Cottbus"),
            r("Bremen", "Bremen", "Bremerhaven"),
            r("Hamburg", "Hamburg"),
            r("Hesse", "Frankfurt am Main", "Wiesbaden", "Kassel"),
            r("Lower Saxony", "Hanover", "Braunschweig", "Oldenburg"),
            r("North Rhine-Westphalia", "Cologne", "Düsseldorf", "Dortmund"),
            r("Rhineland-Palatinate", "Mainz", "Ludwigshafen", "Koblenz"),
            r("Saarland", "Saarbrücken"),
            r("Saxony", "Dresden", "Leipzig", "Chemnitz"),
            r("Saxony-Anhalt", "Magdeburg", "Halle"),
            r("Schleswig-Holstein", "Kiel", "Lübeck"),
            r("Thuringia", "Erfurt", "Jena")
        ),

        // ---------------------------------------------------------- FR
        "FR" to listOf(
            r("Île-de-France", "Paris", "Saint-Denis", "Boulogne-Billancourt"),
            r("Provence-Alpes-Côte d'Azur", "Marseille", "Nice", "Toulon"),
            r("Auvergne-Rhône-Alpes", "Lyon", "Grenoble", "Saint-Étienne"),
            r("Occitanie", "Toulouse", "Montpellier", "Nîmes"),
            r("Nouvelle-Aquitaine", "Bordeaux", "Limoges", "Poitiers"),
            r("Hauts-de-France", "Lille", "Amiens", "Dunkirk"),
            r("Brittany", "Rennes", "Brest", "Quimper"),
            r("Normandy", "Rouen", "Caen", "Le Havre"),
            r("Pays de la Loire", "Nantes", "Angers", "Le Mans"),
            r("Grand Est", "Strasbourg", "Metz", "Reims"),
            r("Bourgogne-Franche-Comté", "Dijon", "Besançon"),
            r("Centre-Val de Loire", "Tours", "Orléans"),
            r("Corsica", "Ajaccio", "Bastia")
        ),

        // ---------------------------------------------------------- AU
        "AU" to listOf(
            r("New South Wales", "Sydney", "Newcastle", "Wollongong"),
            r("Victoria", "Melbourne", "Geelong", "Ballarat"),
            r("Queensland", "Brisbane", "Gold Coast", "Cairns"),
            r("Western Australia", "Perth", "Fremantle", "Bunbury"),
            r("South Australia", "Adelaide", "Mount Gambier"),
            r("Tasmania", "Hobart", "Launceston"),
            r("Australian Capital Territory", "Canberra"),
            r("Northern Territory", "Darwin", "Alice Springs")
        ),

        // ---------------------------------------------------------- JP
        "JP" to listOf(
            r("Hokkaido", "Sapporo", "Asahikawa", "Hakodate"),
            r("Miyagi", "Sendai"),
            r("Saitama", "Saitama", "Kawaguchi"),
            r("Chiba", "Chiba", "Funabashi"),
            r("Tokyo", "Tokyo"),
            r("Kanagawa", "Yokohama", "Kawasaki", "Sagamihara"),
            r("Aichi", "Nagoya", "Toyota", "Okazaki"),
            r("Kyoto", "Kyoto", "Uji"),
            r("Osaka", "Osaka", "Sakai", "Higashiosaka"),
            r("Hyogo", "Kobe", "Himeji"),
            r("Fukuoka", "Fukuoka", "Kitakyushu"),
            r("Okinawa", "Naha", "Urasoe")
        ),

        // ---------------------------------------------------------- SG
        "SG" to listOf(
            r("Central", "Singapore", "Orchard", "Bukit Merah"),
            r("East", "Tampines", "Bedok", "Pasir Ris"),
            r("North", "Woodlands", "Yishun", "Sembawang"),
            r("North-East", "Sengkang", "Punggol", "Hougang"),
            r("West", "Jurong East", "Clementi", "Bukit Batok")
        ),

        // ---------------------------------------------------------- IN
        "IN" to listOf(
            r("Andhra Pradesh", "Visakhapatnam", "Vijayawada", "Guntur"),
            r("Delhi", "New Delhi", "Delhi"),
            r("Gujarat", "Ahmedabad", "Surat", "Vadodara"),
            r("Haryana", "Gurugram", "Faridabad"),
            r("Karnataka", "Bengaluru", "Mysuru", "Mangaluru"),
            r("Kerala", "Kochi", "Thiruvananthapuram", "Kozhikode"),
            r("Madhya Pradesh", "Indore", "Bhopal", "Jabalpur"),
            r("Maharashtra", "Mumbai", "Pune", "Nagpur"),
            r("Punjab", "Ludhiana", "Amritsar", "Jalandhar"),
            r("Rajasthan", "Jaipur", "Jodhpur", "Udaipur"),
            r("Tamil Nadu", "Chennai", "Coimbatore", "Madurai"),
            r("Telangana", "Hyderabad", "Warangal"),
            r("Uttar Pradesh", "Lucknow", "Kanpur", "Agra"),
            r("West Bengal", "Kolkata", "Howrah", "Siliguri")
        ),

        // ---------------------------------------------------------- BR
        "BR" to listOf(
            r("São Paulo", "São Paulo", "Campinas", "Santos"),
            r("Rio de Janeiro", "Rio de Janeiro", "Niterói", "Duque de Caxias"),
            r("Minas Gerais", "Belo Horizonte", "Uberlândia", "Contagem"),
            r("Bahia", "Salvador", "Feira de Santana"),
            r("Paraná", "Curitiba", "Londrina", "Maringá"),
            r("Rio Grande do Sul", "Porto Alegre", "Caxias do Sul"),
            r("Pernambuco", "Recife", "Olinda"),
            r("Ceará", "Fortaleza", "Caucaia"),
            r("Santa Catarina", "Florianópolis", "Joinville", "Blumenau"),
            r("Goiás", "Goiânia", "Aparecida de Goiânia"),
            r("Distrito Federal", "Brasília"),
            r("Pará", "Belém", "Ananindeua"),
            r("Amazonas", "Manaus")
        ),

        // ---------------------------------------------------------- MX
        "MX" to listOf(
            r("Mexico City", "Mexico City"),
            r("Estado de México", "Toluca", "Ecatepec", "Naucalpan"),
            r("Jalisco", "Guadalajara", "Zapopan", "Tlaquepaque"),
            r("Nuevo León", "Monterrey", "San Nicolás", "Guadalupe"),
            r("Puebla", "Puebla", "Cholula"),
            r("Guanajuato", "León", "Irapuato", "Celaya"),
            r("Querétaro", "Querétaro"),
            r("Baja California", "Tijuana", "Mexicali", "Ensenada"),
            r("Yucatán", "Mérida"),
            r("Quintana Roo", "Cancún", "Playa del Carmen"),
            r("Chihuahua", "Ciudad Juárez", "Chihuahua"),
            r("Veracruz", "Veracruz", "Xalapa")
        ),

        // ---------------------------------------------------------- ES
        "ES" to listOf(
            r("Madrid", "Madrid", "Alcalá de Henares", "Getafe"),
            r("Catalonia", "Barcelona", "Girona", "Tarragona"),
            r("Andalusia", "Seville", "Málaga", "Granada"),
            r("Valencian Community", "Valencia", "Alicante", "Elche"),
            r("Basque Country", "Bilbao", "San Sebastián", "Vitoria-Gasteiz"),
            r("Galicia", "A Coruña", "Vigo", "Ourense"),
            r("Castile and León", "Valladolid", "León", "Burgos"),
            r("Murcia", "Murcia", "Cartagena"),
            r("Balearic Islands", "Palma", "Ibiza"),
            r("Canary Islands", "Las Palmas", "Santa Cruz de Tenerife"),
            r("Aragon", "Zaragoza", "Huesca"),
            r("Asturias", "Gijón", "Oviedo")
        ),

        // ---------------------------------------------------------- IT
        "IT" to listOf(
            r("Lombardy", "Milan", "Brescia", "Monza"),
            r("Lazio", "Rome", "Latina", "Fiumicino"),
            r("Campania", "Naples", "Salerno", "Caserta"),
            r("Piedmont", "Turin", "Novara", "Alessandria"),
            r("Veneto", "Venice", "Verona", "Padua"),
            r("Emilia-Romagna", "Bologna", "Modena", "Parma"),
            r("Tuscany", "Florence", "Pisa", "Livorno"),
            r("Sicily", "Palermo", "Catania", "Messina"),
            r("Apulia", "Bari", "Taranto", "Foggia"),
            r("Liguria", "Genoa", "La Spezia"),
            r("Calabria", "Reggio Calabria", "Catanzaro"),
            r("Sardinia", "Cagliari", "Sassari")
        ),

        // ---------------------------------------------------------- NL
        "NL" to listOf(
            r("North Holland", "Amsterdam", "Haarlem", "Alkmaar"),
            r("South Holland", "Rotterdam", "The Hague", "Leiden"),
            r("Utrecht", "Utrecht", "Amersfoort", "Nieuwegein"),
            r("North Brabant", "Eindhoven", "Tilburg", "Breda"),
            r("Gelderland", "Arnhem", "Nijmegen", "Apeldoorn"),
            r("Limburg", "Maastricht", "Venlo", "Heerlen"),
            r("Overijssel", "Enschede", "Zwolle", "Deventer"),
            r("Friesland", "Leeuwarden", "Sneek"),
            r("Groningen", "Groningen"),
            r("Flevoland", "Almere", "Lelystad"),
            r("Drenthe", "Assen", "Emmen"),
            r("Zeeland", "Middelburg", "Vlissingen")
        ),

        // ---------------------------------------------------------- KR
        "KR" to listOf(
            r("Seoul", "Seoul"),
            r("Incheon", "Incheon"),
            r("Gyeonggi", "Suwon", "Seongnam", "Yongin"),
            r("Busan", "Busan"),
            r("Daegu", "Daegu"),
            r("Gwangju", "Gwangju"),
            r("Daejeon", "Daejeon"),
            r("Ulsan", "Ulsan"),
            r("Gangwon", "Chuncheon", "Gangneung"),
            r("North Chungcheong", "Cheongju"),
            r("North Gyeongsang", "Pohang", "Gumi"),
            r("South Jeolla", "Yeosu", "Suncheon"),
            r("Jeju", "Jeju City", "Seogwipo")
        ),

        // ---------------------------------------------------------- ID
        "ID" to listOf(
            r("Jakarta", "Jakarta"),
            r("West Java", "Bandung", "Bekasi", "Bogor"),
            r("Central Java", "Semarang", "Surakarta", "Magelang"),
            r("East Java", "Surabaya", "Malang", "Kediri"),
            r("Banten", "Tangerang", "Serang", "Cilegon"),
            r("Yogyakarta", "Yogyakarta"),
            r("North Sumatra", "Medan", "Binjai", "Pematangsiantar"),
            r("South Sumatra", "Palembang", "Lubuklinggau"),
            r("South Sulawesi", "Makassar", "Parepare"),
            r("Bali", "Denpasar", "Singaraja"),
            r("East Kalimantan", "Balikpapan", "Samarinda"),
            r("Riau", "Pekanbaru", "Dumai")
        ),

        // ---------------------------------------------------------- TH
        "TH" to listOf(
            r("Bangkok", "Bangkok"),
            r("Nonthaburi", "Nonthaburi", "Pak Kret"),
            r("Pathum Thani", "Rangsit", "Thanyaburi"),
            r("Chonburi", "Pattaya", "Chon Buri", "Si Racha"),
            r("Chiang Mai", "Chiang Mai"),
            r("Phuket", "Phuket"),
            r("Khon Kaen", "Khon Kaen"),
            r("Nakhon Ratchasima", "Nakhon Ratchasima"),
            r("Songkhla", "Hat Yai", "Songkhla"),
            r("Surat Thani", "Surat Thani", "Ko Samui"),
            r("Udon Thani", "Udon Thani"),
            r("Rayong", "Rayong")
        ),

        // ---------------------------------------------------------- VN
        "VN" to listOf(
            r("Ho Chi Minh City", "Ho Chi Minh City"),
            r("Hanoi", "Hanoi"),
            r("Da Nang", "Da Nang"),
            r("Hai Phong", "Hai Phong"),
            r("Can Tho", "Can Tho"),
            r("Binh Duong", "Thu Dau Mot", "Di An"),
            r("Dong Nai", "Bien Hoa"),
            r("Khanh Hoa", "Nha Trang"),
            r("Lam Dong", "Da Lat"),
            r("Quang Ninh", "Ha Long", "Cam Pha"),
            r("Nghe An", "Vinh"),
            r("Thanh Hoa", "Thanh Hoa"),
            r("Dak Lak", "Buon Ma Thuot"),
            r("Hue", "Hue")
        ),

        // ---------------------------------------------------------- MY
        "MY" to listOf(
            r("Kuala Lumpur", "Kuala Lumpur"),
            r("Selangor", "Shah Alam", "Petaling Jaya", "Subang Jaya"),
            r("Penang", "George Town", "Butterworth"),
            r("Johor", "Johor Bahru", "Iskandar Puteri"),
            r("Perak", "Ipoh", "Taiping"),
            r("Negeri Sembilan", "Seremban", "Nilai"),
            r("Melaka", "Malacca City"),
            r("Pahang", "Kuantan", "Temerloh"),
            r("Kedah", "Alor Setar", "Sungai Petani"),
            r("Kelantan", "Kota Bharu"),
            r("Sabah", "Kota Kinabalu", "Sandakan", "Tawau"),
            r("Sarawak", "Kuching", "Miri", "Sibu")
        ),

        // ---------------------------------------------------------- AE
        "AE" to listOf(
            r("Dubai", "Dubai"),
            r("Abu Dhabi", "Abu Dhabi", "Al Ain"),
            r("Sharjah", "Sharjah"),
            r("Ajman", "Ajman"),
            r("Ras Al Khaimah", "Ras Al Khaimah"),
            r("Fujairah", "Fujairah"),
            r("Umm Al Quwain", "Umm Al Quwain")
        ),

        // ---------------------------------------------------------- SA
        "SA" to listOf(
            r("Riyadh", "Riyadh", "Al Kharj"),
            r("Makkah", "Jeddah", "Mecca", "Taif"),
            r("Eastern Province", "Dammam", "Khobar", "Dhahran"),
            r("Madinah", "Medina", "Yanbu"),
            r("Asir", "Abha", "Khamis Mushait"),
            r("Tabuk", "Tabuk"),
            r("Qassim", "Buraidah", "Unaizah"),
            r("Hail", "Hail"),
            r("Jazan", "Jazan"),
            r("Najran", "Najran")
        ),

        // ---------------------------------------------------------- TR
        "TR" to listOf(
            r("Istanbul", "Istanbul"),
            r("Ankara", "Ankara"),
            r("Izmir", "Izmir"),
            r("Bursa", "Bursa"),
            r("Antalya", "Antalya"),
            r("Adana", "Adana"),
            r("Konya", "Konya"),
            r("Gaziantep", "Gaziantep"),
            r("Mersin", "Mersin"),
            r("Kayseri", "Kayseri"),
            r("Eskisehir", "Eskisehir"),
            r("Trabzon", "Trabzon"),
            r("Diyarbakir", "Diyarbakir"),
            r("Samsun", "Samsun")
        ),

        // ---------------------------------------------------------- PL
        "PL" to listOf(
            r("Masovian", "Warsaw", "Radom", "Płock"),
            r("Lesser Poland", "Kraków", "Tarnów", "Nowy Sącz"),
            r("Silesian", "Katowice", "Gliwice", "Częstochowa"),
            r("Lower Silesian", "Wrocław", "Legnica", "Wałbrzych"),
            r("Greater Poland", "Poznań", "Kalisz", "Konin"),
            r("Pomeranian", "Gdańsk", "Gdynia", "Słupsk"),
            r("Łódź", "Łódź", "Piotrków Trybunalski"),
            r("West Pomeranian", "Szczecin", "Koszalin"),
            r("Lublin", "Lublin", "Chełm"),
            r("Kuyavian-Pomeranian", "Bydgoszcz", "Toruń"),
            r("Podkarpackie", "Rzeszów", "Przemyśl"),
            r("Warmian-Masurian", "Olsztyn", "Elbląg")
        ),

        // ---------------------------------------------------------- UA
        "UA" to listOf(
            r("Kyiv", "Kyiv"),
            r("Lviv", "Lviv", "Drohobych"),
            r("Odesa", "Odesa", "Izmail"),
            r("Dnipropetrovsk", "Dnipro", "Kryvyi Rih", "Kamianske"),
            r("Kharkiv", "Kharkiv", "Izium"),
            r("Zaporizhzhia", "Zaporizhzhia", "Melitopol"),
            r("Vinnytsia", "Vinnytsia"),
            r("Poltava", "Poltava", "Kremenchuk"),
            r("Ivano-Frankivsk", "Ivano-Frankivsk"),
            r("Chernivtsi", "Chernivtsi"),
            r("Zhytomyr", "Zhytomyr"),
            r("Khmelnytskyi", "Khmelnytskyi", "Kamianets-Podilskyi"),
            r("Rivne", "Rivne"),
            r("Cherkasy", "Cherkasy")
        )
    )

    /** Regions for a country code, or empty when the picker stays country-level. */
    fun regionsFor(countryCode: String): List<GeoRegion> =
        byCountry[countryCode.uppercase()] ?: emptyList()
}
