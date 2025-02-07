package org.isen.Projet.model

import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import java.net.URLEncoder


// 🔥 Modèle de données pour une station de carburant
data class MyStationResponse(val total_count: Int, val results: List<StationRecord>) {
    class Deserializer : ResponseDeserializable<MyStationResponse> {
        override fun deserialize(content: String): MyStationResponse? {
            return Gson().fromJson(content, MyStationResponse::class.java)
        }
    }
}

data class StationRecord(val id: String, val address: String, val cp: String)

data class Station(
    val id: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val fuelTypes: String,
    val priceGazole: Double?,
    val priceSP95: Double?,
    val priceSP98: Double?,
    val brand: String
)
class StationModel {
    private val client = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(StationModel::class.java)

    fun fetchStationsOnline(): List<Station> {
        val allStations = mutableListOf<Station>()
        val url = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records?limit=100"

        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("❌ Erreur API ${response.code} - ${response.message}")
                return allStations
            }

            val jsonData = response.body?.string() ?: return allStations
            val parsedData = objectMapper.readTree(jsonData)
            val results = parsedData["results"] ?: return allStations

            results.mapNotNull { record ->
                try {
                    allStations.add(
                        Station(
                            id = record["id"]?.asText() ?: "ID inconnu",
                            address = record["address"]?.asText() ?: "Adresse inconnue",
                            city = record["com_arm_name"]?.asText() ?: "Ville inconnue",
                            postalCode = record["cp"]?.asText() ?: "Code Postal inconnu",
                            fuelTypes = record["fuel"]?.map { it.asText() }?.joinToString(", ") ?: "Aucun",
                            priceGazole = record["price_gazole"]?.asDouble(),
                            priceSP95 = record["price_sp95"]?.asDouble(),
                            priceSP98 = record["price_sp98"]?.asDouble(),
                            brand = record["brand"]?.asText() ?: "Marque inconnue"
                        )
                    )
                } catch (e: Exception) {
                    logger.warn("⚠ Erreur lors du traitement des données de la station : ${e.message}")
                }
                return@mapNotNull null
            }
        }

        logger.info("✅ ${allStations.size} stations chargées depuis l'API.")
        return allStations
    }


    fun fetchStationsByCity(city: String): List<Station> {
        val stationsList = mutableListOf<Station>()
        val client = OkHttpClient()
        val objectMapper = jacksonObjectMapper()
        val logger = LoggerFactory.getLogger(StationModel::class.java)


        val encodedCity = URLEncoder.encode(city, "UTF-8")


        val apiUrls = listOf(
            "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records?where=com_arm_name LIKE '$encodedCity'&limit=100",
            "https://www.prix-carburants.gouv.fr/rubrique/opendata/" // API gouvernementale
        )

        for (url in apiUrls) {
            logger.info("🔍 Tentative de requête API: $url")

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.warn("⚠ Échec de l'API ($url) avec erreur ${response.code} - ${response.message}")
                response.close()
                continue
            }

            val jsonData = response.body?.string()
            response.close()

            logger.info("📥 Réponse JSON brute: $jsonData")

            if (jsonData.isNullOrEmpty()) {
                logger.error("❌ JSON vide ou null, impossible de parser.")
                continue
            }

            val parsedData = objectMapper.readTree(jsonData)

            if (!parsedData.has("results")) {
                logger.error("❌ Clé 'results' absente dans la réponse JSON.")
                continue
            }

            val results = parsedData["results"]

            for (record in results) {
                try {
                    val stationCity = record["com_arm_name"]?.asText() ?: "Ville inconnue"

                    if (!stationCity.contains(city, ignoreCase = true)) {
                        continue
                    }

                    stationsList.add(
                        Station(
                            id = record["id"]?.asText() ?: "ID inconnu",
                            address = record["address"]?.asText() ?: "Adresse inconnue",
                            city = stationCity,
                            postalCode = record["cp"]?.asText() ?: "Code Postal inconnu",
                            fuelTypes = record["fuel"]?.map { it.asText() }?.joinToString(", ") ?: "Aucun",
                            priceGazole = record["price_gazole"]?.asDouble(),
                            priceSP95 = record["price_sp95"]?.asDouble(),
                            priceSP98 = record["price_sp98"]?.asDouble(),
                            brand = record["brand"]?.asText() ?: "Marque inconnue"
                        )
                    )
                } catch (e: Exception) {
                    logger.warn("⚠ Erreur lors du traitement des données : ${e.message}")
                }
            }


            return stationsList
        }

        logger.error("❌ Aucune API n'a pu fournir de données pour la ville '$city'.")
        return stationsList
    }




    fun fetchStationsByItinerary(startCity: String, endCity: String): List<Station> {
        logger.info("📍 Recherche des stations entre '$startCity' et '$endCity'...")


        val startCoords = getCityCoordinates(startCity)
        val endCoords = getCityCoordinates(endCity)
        if (startCoords == null || endCoords == null) {
            logger.error("❌ Impossible de récupérer les coordonnées de l'une des villes.")
            return emptyList()
        }
        logger.info("🔍 Coordonnées de $startCity : ${startCoords.first}, ${startCoords.second}")
        logger.info("🔍 Coordonnées de $endCity : ${endCoords.first}, ${endCoords.second}")


        val allStations = fetchStationsOnline()
        logger.info("📊 Nombre total de stations récupérées : ${allStations.size}")


        val filteredStations = allStations.filter { station ->
            val stationCoords = getCityCoordinates(station.city)
            if (stationCoords != null) {
                logger.info("📍 Vérification de la station ${station.city} aux coordonnées ${stationCoords.first}, ${stationCoords.second}")
                isBetween(startCoords, endCoords, stationCoords)
            } else {
                false
            }
        }

        logger.info("✅ ${filteredStations.size} stations trouvées sur l'itinéraire.")
        return filteredStations
    }


    fun getCityCoordinates(city: String): Pair<Double, Double>? {
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=${URLEncoder.encode(city, "UTF-8")}"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null

            val jsonData = response.body?.string() ?: return null
            val parsedData = objectMapper.readTree(jsonData)
            if (parsedData.isEmpty() || !parsedData[0].has("lat") || !parsedData[0].has("lon")) {
                logger.error("❌ Données GPS invalides pour $city")
                return null
            }

            val lat = parsedData[0]["lat"].asDouble()
            val lon = parsedData[0]["lon"].asDouble()
            return Pair(lat, lon)
        }
    }


    fun isBetween(start: Pair<Double, Double>, end: Pair<Double, Double>, point: Pair<Double, Double>): Boolean {
        val (lat1, lon1) = start
        val (lat2, lon2) = end
        val (latP, lonP) = point

        val minLat = minOf(lat1, lat2)
        val maxLat = maxOf(lat1, lat2)
        val minLon = minOf(lon1, lon2)
        val maxLon = maxOf(lon1, lon2)

        return latP in minLat..maxLat && lonP in minLon..maxLon
    }





}