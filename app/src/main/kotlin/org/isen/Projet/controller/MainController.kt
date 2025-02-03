package org.isen.Projet.controller

import org.isen.Projet.model.Station
import org.isen.Projet.model.StationModel
import org.isen.Projet.view.MainView

class MainController {
    private val model = StationModel()
    private lateinit var view: MainView
    private var allStations: List<Station> = listOf()

    fun setView(view: MainView) {
        this.view = view
    }

    fun loadOnlineData() {
        val stations = model.fetchStationsOnline()

        if (stations.isEmpty()) {
            view.showError("Aucune station trouvée.")
            return
        }

        allStations = stations // On stocke bien `stations` sous forme de `List<Station>`
        view.updateData(allStations) // ✅ Mise à jour de la vue avec les nouvelles données
    }

    fun searchStationsByCity(city: String) {
        if (city.isEmpty()) {
            view.showError("Veuillez entrer une ville.")
            return
        }

        val filteredStations = allStations.filter { it.city.contains(city, ignoreCase = true) }

        if (filteredStations.isEmpty()) {
            view.showError("Aucune station trouvée pour la ville '$city'.")
        } else {
            view.updateData(filteredStations)
        }
    }

    fun searchStationsByItinerary(startCity: String, endCity: String) {
        if (startCity.isEmpty() || endCity.isEmpty()) {
            view.showError("Veuillez entrer une ville de départ et une ville d'arrivée.")
            return
        }

        val filteredStations = model.fetchStationsByItinerary(startCity, endCity)

        if (filteredStations.isEmpty()) {
            view.showError("Aucune station trouvée entre '$startCity' et '$endCity'.")
        } else {
            view.updateData(filteredStations)
        }
    }
}
