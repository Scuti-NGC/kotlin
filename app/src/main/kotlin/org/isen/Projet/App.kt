package org.isen.Projet

import org.isen.Projet.controller.MainController
import org.isen.Projet.view.MainView
import javax.swing.SwingUtilities
import com.formdev.flatlaf.FlatLightLaf

fun main() {
    SwingUtilities.invokeLater {
        FlatLightLaf.setup()
        val controller = MainController()
        val view = MainView(controller)
        controller.setView(view)
    }
}
