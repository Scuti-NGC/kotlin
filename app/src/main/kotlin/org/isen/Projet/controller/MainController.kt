package org.isen.Projet.controller

import org.isen.Projet.model.Station
import org.isen.Projet.model.StationModel
import org.isen.Projet.view.MainView
import org.slf4j.LoggerFactory

class MainController {
    private val model = StationModel()
    private lateinit var view: MainView
    private var allStations: List<Station> = listOf()
    private val logger = LoggerFactory.getLogger(MainController::class.java)

    fun setView(view: MainView) {
        this.view = view
        logger.info("✅ Vue correctement liée au contrôleur")
    }

    fun loadOnlineData() {
        logger.info("📡 Chargement des données depuis l'API...")
        val stations = model.fetchStationsOnline()
        allStations = stations
        view.updateData(allStations)
        logger.info("✅ ${stations.size} stations chargées depuis l'API")
    }

    fun searchStationsByCity(city: String) {
        if (city.isEmpty()) {
            view.showError("Veuillez entrer une ville.")
            return
        }

        logger.info("🔍 Recherche des stations pour la ville '$city'...")
        val stations = model.fetchStationsByCity(city)

        if (stations.isEmpty()) {
            logger.warn("❌ Aucune station trouvée pour '$city'")
            view.showError("Aucune station trouvée pour '$city'.")
        } else {
            logger.info("✅ ${stations.size} stations trouvées pour '$city'")
            view.updateData(stations)
        }
    }

    fun searchStationsByItinerary(startCity: String, endCity: String) {
        if (startCity.isEmpty() || endCity.isEmpty()) {
            logger.warn("⚠ Recherche itinéraire incomplète.")
            view.showError("Veuillez entrer une ville de départ et une ville d'arrivée.")
            return
        }

        logger.info("📍 Recherche d'itinéraire entre '$startCity' et '$endCity'...")
        val filteredStations = model.fetchStationsByItinerary(startCity, endCity)

        if (filteredStations.isEmpty()) {
            logger.warn("⚠ Aucune station trouvée sur l'itinéraire.")
            view.showError("Aucune station trouvée entre '$startCity' et '$endCity'.")
        } else {
            logger.info("✅ ${filteredStations.size} stations trouvées sur l'itinéraire.")
            view.updateData(filteredStations)
        }
    }
}
