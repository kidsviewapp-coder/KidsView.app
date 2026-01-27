package why.xee.kidsview.data.model

/**
 * Complete cartoon dataset for browsing
 * Contains all cartoons organized by country and genre
 */
object CartoonData {
    
    /**
     * Represents a cartoon with its metadata
     */
    data class Cartoon(
        val name: String,
        val country: String,
        val genre: String
    )
    
    /**
     * Get all cartoons organized by country and genre
     */
    fun getCartoonsByCountryAndGenre(): Map<String, Map<String, List<String>>> {
        return mapOf(
            "Pakistan" to mapOf(
                "Islamic" to listOf(
                    "Burka Avenger",
                    "Sheikh Chilli Adventures (Pak Version)",
                    "Chotoons (Islamic moral stories)",
                    "Allahyar and The Legend (Islamic themes)",
                    "DIT \"daadi ki islaami taleemat\""
                ),
                "Educational" to listOf(
                    "Quaid-e-Azam Animated Stories",
                    "Allama Iqbal Animated Stories",
                    "Kids Planet Urdu Stories",
                    "Urdu Phonics Animation",
                    "HCZ \"History chachu ki zubaani\"",
                    "Pakistan ka haseen safar",
                    "P² \"Pakistan Pedia\""
                ),
                "Folk Tales" to listOf(
                    "Nani Ki Kahaniyan",
                    "Dastan-e-Pakistan Animated",
                    "Bait Bazi Animated"
                ),
                "Comedy" to listOf(
                    "Commander Safeguard",
                    "Funny Urdu Animated Clips"
                ),
                "Movies / 3D" to listOf(
                    "The Donkey King",
                    "Allahyar and The Legend",
                    "Umro Ayyar (Animated)"
                ),
                "Action / Adventure" to listOf(
                    "Teen Bahadur",
                    "Jaan Cartoon"
                )
            ),
            "India" to mapOf(
                "Action / Superhero" to listOf(
                    "Chhota Bheem",
                    "Little Singham",
                    "Shiva",
                    "Roll No. 21",
                    "Baahubali: The Lost Legends",
                    "Kris",
                    "Super Bheem",
                    "Vir The Robot Boy",
                    "Kid Krrish"
                ),
                "Comedy" to listOf(
                    "Motu Patlu",
                    "Oggy and the Cockroaches",
                    "Honey Bunny Ka Jholmaal",
                    "Gattu Battu",
                    "Bandbudh Aur Budbak",
                    "Chacha Bhatija",
                    "Titoo in Ocean Kids"
                ),
                "Movies / 3D" to listOf(
                    "Delhi Safari",
                    "Toonpur Ka Superhero"
                ),
                "Mythological" to listOf(
                    "Bal Hanuman",
                    "Bal Ganesha",
                    "Bal Krishna",
                    "Ramayan Kids",
                    "Mahabharata Kids",
                    "Krishna Aur Kans"
                ),
                "Educational / Moral" to listOf(
                    "Akbar Birbal",
                    "Tenali Raman",
                    "Vikram Betaal",
                    "Panchatantra Stories",
                    "Krishna Stories"
                ),
                "Preschool" to listOf(
                    "ChuChu TV",
                    "Infobells",
                    "Jugnu Kids Hindi Rhymes"
                )
            ),
            "International" to mapOf(
                "Preschool" to listOf(
                    "Cocomelon",
                    "Little Baby Bum",
                    "Super Simple Songs",
                    "Dave & Ava",
                    "Pinkfong",
                    "BabyBus"
                ),
                "Educational" to listOf(
                    "Dora the Explorer",
                    "Go Diego Go",
                    "Sid the Science Kid",
                    "Alphablocks",
                    "Numberblocks",
                    "Blue's Clues",
                    "Arthur"
                ),
                "Action / Adventure" to listOf(
                    "Ben 10",
                    "Avatar: The Last Airbender",
                    "Transformers Rescue Bots",
                    "Power Rangers Animated",
                    "The Dragon Prince",
                    "Ninjago"
                ),
                "Comedy" to listOf(
                    "Tom & Jerry",
                    "Looney Tunes",
                    "SpongeBob SquarePants",
                    "Pink Panther",
                    "Mr. Bean Animated",
                    "Shaun the Sheep"
                ),
                "Superheroes" to listOf(
                    "PJ Masks",
                    "Spider-Man Animated",
                    "Batman Animated",
                    "Iron Man Animated",
                    "Teen Titans Go"
                ),
                "3D / Modern" to listOf(
                    "Paw Patrol",
                    "Octonauts",
                    "Super Wings",
                    "Robocar Poli",
                    "Masha and the Bear",
                    "Miraculous Ladybug"
                ),
                "Disney / Pixar" to listOf(
                    "Mickey Mouse Clubhouse",
                    "DuckTales",
                    "Frozen Tales",
                    "The Lion Guard",
                    "Tangled Animated Series"
                )
            )
        )
    }
    
    /**
     * Get all countries
     */
    fun getCountries(): List<String> {
        return getCartoonsByCountryAndGenre().keys.sorted()
    }
    
    /**
     * Get genres for a specific country
     */
    fun getGenresForCountry(country: String): List<String> {
        return getCartoonsByCountryAndGenre()[country]?.keys?.sorted() ?: emptyList()
    }
    
    /**
     * Get cartoons for a specific country and genre
     */
    fun getCartoonsForCountryAndGenre(country: String, genre: String): List<String> {
        return getCartoonsByCountryAndGenre()[country]?.get(genre) ?: emptyList()
    }
    
    /**
     * Get all cartoons as a flat list (for Simple List Mode)
     */
    fun getAllCartoons(): List<Cartoon> {
        val cartoons = mutableListOf<Cartoon>()
        getCartoonsByCountryAndGenre().forEach { (country, genres) ->
            genres.forEach { (genre, cartoonNames) ->
                cartoonNames.forEach { name ->
                    cartoons.add(Cartoon(name = name, country = country, genre = genre))
                }
            }
        }
        return cartoons.sortedBy { it.name }
    }
}
