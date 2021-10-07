package org.walletconnect.example.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation.findNavController
import org.walletconnect.example.R

fun AppCompatActivity.getNavController(): NavController =
    findNavController(this, R.id.nav_host_container)

fun AppCompatActivity.getCurrentDestination(): NavDestination? =
    findNavController(this, R.id.nav_host_container).currentDestination