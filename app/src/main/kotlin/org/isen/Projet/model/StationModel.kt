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

        // ✅ Encoder la ville pour éviter les erreurs d'URL
        val encodedCity = URLEncoder.encode(city, "UTF-8")

        // ✅ Définition des APIs principale et secondaire
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
                continue // ✅ Essayer l’API suivante
            }

            val jsonData = response.body?.string()
            response.close()

            logger.info("📥 Réponse JSON brute: $jsonData")

            if (jsonData.isNullOrEmpty()) {
                logger.error("❌ JSON vide ou null, impossible de parser.")
                continue // ✅ Essayer l’API suivante
            }

            val parsedData = objectMapper.readTree(jsonData)

            if (!parsedData.has("results")) {
                logger.error("❌ Clé 'results' absente dans la réponse JSON.")
                continue // ✅ Essayer l’API suivante
            }

            val results = parsedData["results"]

            for (record in results) {
                try {
                    val stationCity = record["com_arm_name"]?.asText() ?: "Ville inconnue"

                    if (!stationCity.contains(city, ignoreCase = true)) {
                        continue // ✅ Ignorer les villes incorrectes
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

            // ✅ Si une API a réussi, on arrête ici
            return stationsList
        }

        logger.error("❌ Aucune API n'a pu fournir de données pour la ville '$city'.")
        return stationsList
    }




    fun fetchStationsByItinerary(startCity: String, endCity: String): List<Station> {
        logger.info("📍 Recherche des stations entre '$startCity' et '$endCity'...")

        val url = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records?where=com_arm_name='$startCity' OR com_arm_name='$endCity'&limit=100"
        val stationsList = mutableListOf<Station>()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.error("❌ Erreur API ${response.code} - ${response.message}")
                return stationsList
            }

            val jsonData = response.body?.string() ?: return stationsList
            val parsedData = objectMapper.readTree(jsonData)
            val results = parsedData["results"] ?: return stationsList

            results.mapNotNull { record ->
                try {
                    stationsList.add(
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

        logger.info("✅ ${stationsList.size} stations trouvées sur l'itinéraire.")
        return stationsList
    }



}
