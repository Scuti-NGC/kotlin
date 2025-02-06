package org.isen.Projet.view

import org.isen.Projet.model.Station
import org.isen.Projet.controller.MainController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import java.awt.*
import java.io.File

import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class MainView(private val controller: MainController) : JFrame() {
    private val columnNames = arrayOf("ID", "Adresse", "Ville", "Code Postal", "Carburants", "Gazole (€)", "SP95 (€)", "SP98 (€)", "Marque")
    private val tableModel = DefaultTableModel(columnNames, 0)
    private val table = JTable(tableModel)

    private val searchField = JTextField(15)
    private val searchButton = createStyledButton("🔍 Rechercher", Color(52, 152, 219))
    private val resetButton = createStyledButton("🔄 Réinitialiser", Color(211, 84, 0))
    private val toggleThemeButton = createStyledButton("🌙 Mode Sombre/Clair", Color(241, 196, 15))

    private val startCityField = JTextField(10)
    private val endCityField = JTextField(10)
    private val searchItineraryButton = createStyledButton("🚗 Rechercher Itinéraire", Color(46, 204, 113))

    private val brandFilter = JComboBox(arrayOf("Toutes", "Total", "Carrefour", "Intermarché", "Leclerc", "Autres"))
    private val fuelFilter = JComboBox(arrayOf("Tous", "Gazole", "SP95", "SP98", "E10", "E85"))

    private val addToFavoritesButton = createStyledButton("⭐ Ajouter aux Favoris", Color(39, 174, 96))
    private val removeFromFavoritesButton = createStyledButton("❌ Retirer des Favoris", Color(231, 76, 60))
    private val showFavoritesButton = createStyledButton("⭐ Voir Favoris", Color(243, 156, 18))

    private var isDarkMode = false
    private var allStations: List<Array<String>> = listOf()
    private var favorites: MutableList<String> = loadFavorites()

    init {
        title = "⛽ Stations de Carburant - Interface Swing"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 700)
        setupUI()
        setLocationRelativeTo(null)
        isVisible = true
    }


    private fun setupUI() {
        UIManager.setLookAndFeel(FlatLightLaf())

        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = Color(236, 240, 241)

        val topPanel = JPanel(GridBagLayout())
        topPanel.background = Color(189, 195, 199)
        topPanel.border = EmptyBorder(10, 10, 10, 10)

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.gridy = 0
        gbc.gridx = 0

        topPanel.add(JLabel("🔍 Ville :"), gbc)
        gbc.gridx++
        topPanel.add(searchField, gbc)
        gbc.gridx++
        topPanel.add(searchButton, gbc)
        gbc.gridx++
        topPanel.add(resetButton, gbc)
        gbc.gridx = 0
        gbc.gridy++

        topPanel.add(JLabel("🚗 Départ :"), gbc)
        gbc.gridx++
        topPanel.add(startCityField, gbc)
        gbc.gridx++
        topPanel.add(JLabel("🏁 Arrivée :"), gbc)
        gbc.gridx++
        topPanel.add(endCityField, gbc)
        gbc.gridx++
        topPanel.add(searchItineraryButton, gbc)
        gbc.gridx = 0
        gbc.gridy++

        topPanel.add(showFavoritesButton, gbc)
        gbc.gridx++
        topPanel.add(toggleThemeButton, gbc)

        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter
        table.rowHeight = 30
        table.setShowGrid(false)
        table.background = Color(236, 240, 241)
        table.gridColor = Color(189, 195, 199)
        table.intercellSpacing = Dimension(5, 5)

        val scrollPane = JScrollPane(table)
        scrollPane.border = EmptyBorder(10, 10, 10, 10)

        val bottomPanel = JPanel()
        bottomPanel.background = Color(52, 73, 94)
        bottomPanel.add(addToFavoritesButton)
        bottomPanel.add(removeFromFavoritesButton)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        add(mainPanel)
        searchButton.addActionListener {
            val city = searchField.text.trim()
            controller.searchStationsByCity(city)
        }


        searchButton.addActionListener { applyFilters() }
        searchItineraryButton.addActionListener { searchItinerary() }
        resetButton.addActionListener { resetFilters() }
        showFavoritesButton.addActionListener { showFavorites() }
        toggleThemeButton.addActionListener { toggleTheme() }
        addToFavoritesButton.addActionListener { addSelectedToFavorites() }
        removeFromFavoritesButton.addActionListener { removeSelectedFromFavorites() }
    }

    private fun toggleTheme() {
        if (isDarkMode) {
            UIManager.setLookAndFeel(FlatLightLaf())
        } else {
            UIManager.setLookAndFeel(FlatDarkLaf())
        }
        isDarkMode = !isDarkMode
        SwingUtilities.updateComponentTreeUI(this)
    }
    private fun applyFilters() {
        val selectedBrand = brandFilter.selectedItem as String
        val selectedFuel = fuelFilter.selectedItem as String
        val searchText = searchField.text.trim()

        val filteredStations = allStations.filter {
            val brandMatch = selectedBrand == "Toutes" || it[8] == selectedBrand ||
                    (selectedBrand == "Autres" && !arrayOf("Total", "Carrefour", "Intermarché", "Leclerc").contains(it[8]))

            val fuelMatch = selectedFuel == "Tous" || it[4].contains(selectedFuel, ignoreCase = true)
            val cityMatch = searchText.isEmpty() || it[2].contains(searchText, ignoreCase = true)

            brandMatch && fuelMatch && cityMatch
        }

        tableModel.setRowCount(0)
        filteredStations.forEach { tableModel.addRow(it) }
    }
    private fun createStyledButton(text: String, bgColor: Color): JButton {
        val button = JButton(text)
        button.background = bgColor
        button.foreground = Color.WHITE
        button.isFocusPainted = false
        button.border = BorderFactory.createEmptyBorder(10, 15, 10, 15)
        button.font = Font("Arial", Font.BOLD, 14)
        return button
    }

    private fun searchItinerary() {
        val startCity = startCityField.text.trim()
        val endCity = endCityField.text.trim()

        if (startCity.isEmpty() || endCity.isEmpty()) {
            showError("Veuillez entrer une ville de départ et une ville d'arrivée.")
            return
        }

        controller.searchStationsByItinerary(startCity, endCity)
    }
    private fun resetFilters() {
        searchField.text = ""
        startCityField.text = ""
        endCityField.text = ""
        brandFilter.selectedIndex = 0
        fuelFilter.selectedIndex = 0
        controller.loadOnlineData()

        tableModel.setRowCount(0)  // Vide le tableau des stations affichées

    }

    private fun showFavorites() {
        val favoriteStations = allStations.filter { it[0] in favorites }
        tableModel.setRowCount(0)
        favoriteStations.forEach { tableModel.addRow(it) }
    }
    private fun addSelectedToFavorites() {
        val selectedRow = table.selectedRow
        if (selectedRow == -1) {
            showError("Veuillez sélectionner une station à ajouter aux favoris.")
            return
        }

        val stationId = table.getValueAt(selectedRow, 0) as String
        if (stationId in favorites) {
            showError("Cette station est déjà dans vos favoris.")
            return
        }

        favorites.add(stationId)
        saveFavorites(favorites)
        JOptionPane.showMessageDialog(this, "Station ajoutée aux favoris !", "Favoris", JOptionPane.INFORMATION_MESSAGE)
    }
    private fun removeSelectedFromFavorites() {
        val selectedRow = table.selectedRow
        if (selectedRow == -1) {
            showError("Veuillez sélectionner une station à retirer des favoris.")
            return
        }

        val stationId = table.getValueAt(selectedRow, 0) as String
        if (stationId !in favorites) {
            showError("Cette station n'est pas dans vos favoris.")
            return
        }

        favorites.remove(stationId)
        saveFavorites(favorites)
        JOptionPane.showMessageDialog(this, "Station retirée des favoris.", "Favoris", JOptionPane.INFORMATION_MESSAGE)

        applyFilters()
    }
    private fun saveFavorites(favorites: MutableList<String>) {
        val file = File("favorites.json")
        val objectMapper = jacksonObjectMapper()
        file.writeText(objectMapper.writeValueAsString(favorites))
    }

    private fun loadFavorites(): MutableList<String> {
        val file = File("favorites.json")
        if (!file.exists()) return mutableListOf()
        val objectMapper = jacksonObjectMapper()
        return objectMapper.readValue(file, MutableList::class.java) as MutableList<String>
    }


    fun updateData(stations: List<Station>) {
        allStations = stations.map { station ->
            arrayOf(
                station.id,
                station.address,
                station.city,
                station.postalCode,
                station.fuelTypes,
                station.priceGazole?.toString() ?: "N/A",
                station.priceSP95?.toString() ?: "N/A",
                station.priceSP98?.toString() ?: "N/A",
                station.brand
            )
        }

        tableModel.setRowCount(0)
        allStations.forEach { tableModel.addRow(it) }
        table.revalidate()
        table.repaint()
    }

    fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE)
    }
}