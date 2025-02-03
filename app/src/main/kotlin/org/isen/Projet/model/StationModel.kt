package org.isen.Projet.model

import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.JsonNode

// 🔥 Modèle de données pour une station de carburant
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

    fun fetchStationsOnline(): List<Station> {
        val allStations = mutableListOf<Station>()
        var offset = 0
        val limit = 100
        var totalRecords = 1000

        while (offset < totalRecords) {
            val url = "https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/prix-des-carburants-j-1/records?limit=$limit&offset=$offset"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("❌ Erreur API ${response.code} - ${response.message}")
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
                        println("⚠ Erreur sur une station : ${e.message}")
                    }
                    return@mapNotNull null
                }

                offset += limit
            }
        }

        return allStations
    }

    fun fetchStationsByItinerary(startCity: String, endCity: String): List<Station> {
        val allStations = fetchStationsOnline()

        return allStations.filter { station ->
            station.city.equals(startCity, ignoreCase = true) || station.city.equals(endCity, ignoreCase = true)
        }
    }
}
