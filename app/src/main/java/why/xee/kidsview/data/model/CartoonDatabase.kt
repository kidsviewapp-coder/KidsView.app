package why.xee.kidsview.data.model

/**
 * Cartoon database organized by country and genre
 * Contains curated lists of cartoons for parent mode category selection
 */
object CartoonDatabase {
    
    /**
     * Get cartoons for a specific country and genre
     * @param country Country name (e.g., "Pakistan", "India", "International")
     * @param genre Genre name (e.g., "Action / Adventure", "Comedy / Fun")
     * @return List of cartoon names, or empty list if not found
     */
    fun getCartoons(country: String, genre: String): List<String> {
        return database[country]?.get(genre) ?: emptyList()
    }
    
    /**
     * Get all countries available in the database
     */
    fun getCountries(): List<String> = database.keys.sorted()
    
    /**
     * Get all genres for a specific country
     */
    fun getGenres(country: String): List<String> {
        return database[country]?.keys?.sorted() ?: emptyList()
    }
    
    /**
     * Check if a country exists in the database
     */
    fun hasCountry(country: String): Boolean = database.containsKey(country)
    
    /**
     * Check if a genre exists for a country
     */
    fun hasGenre(country: String, genre: String): Boolean {
        return database[country]?.containsKey(genre) ?: false
    }
    
    /**
     * Cartoon database structure
     * Organized by country, then by genre
     */
    private val database: Map<String, Map<String, List<String>>> = mapOf(
        "Pakistan" to mapOf(
            "Action / Adventure" to listOf(
                "Burka Avenger",
                "Teen Bahadur",
                "Jaan Cartoon"
            ),
            "Comedy / Fun" to listOf(
                "The Donkey King",
                "Tick Tock"
            ),
            "Educational" to listOf(
                "Taleemabad",
                "Quaid Se Baatain",
                "DIT \"daadi ki islaami taleemat\"",
                "HCZ \"History chachu ki zubaani\"",
                "Pakistan ka haseen safar",
                "P² \"Pakistan Pedia\""
            ),
            "Moral / Social Message" to listOf(
                "Commander Safeguard",
                "Allahyar and the Legend of Markhor"
            )
        ),
        "India" to mapOf(
            "Action / Adventure" to listOf(
                "Chhota Bheem",
                "Krishna Balram",
                "Shiva",
                "Mighty Raju",
                "Little Singham",
                "Super Bheem",
                "Vir The Robot Boy",
                "Kid Krrish",
                "Delhi Safari"
            ),
            "Comedy / Fun" to listOf(
                "Motu Patlu",
                "Pakdam Pakdai",
                "Oggy and the Cockroaches",
                "Tom & Jerry Indian Dub",
                "Gattu Battu",
                "Chacha Bhatija",
                "Honey Bunny Ka Jholmaal",
                "Titoo in Ocean Kids"
            ),
            "Educational / Mythology" to listOf(
                "Bal Hanuman",
                "Bal Ganesh"
            ),
            "Movies / 3D" to listOf(
                "Toonpur Ka Superhero",
                "Roll No 21"
            )
        ),
        "International" to mapOf(
            "Action / Adventure" to listOf(
                "Ben 10",
                "Pokemon",
                "Avatar: The Last Airbender",
                "Teen Titans Go",
                "Beyblade",
                "Powerpuff Girls",
                "Miraculous Ladybug",
                "Dragon Ball"
            ),
            "Comedy / Fun" to listOf(
                "SpongeBob SquarePants",
                "Looney Tunes",
                "Tom & Jerry",
                "Scooby-Doo"
            ),
            "Toddler / Nursery" to listOf(
                "Cocomelon",
                "Peppa Pig",
                "Baby Shark",
                "Bob the Train",
                "Bluey",
                "Blippi"
            ),
            "Educational" to listOf(
                "Numberblocks",
                "Alphablocks",
                "Sid the Science Kid",
                "Wild Kratts"
            ),
            "Fantasy / Girls" to listOf(
                "Barbie Series",
                "My Little Pony"
            )
        )
    )
}


