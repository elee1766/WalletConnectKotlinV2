package org.walletconnect.example

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.walletconnect.example.databinding.ActivityMainBinding
import org.walletconnect.example.extension.getCurrentDestination
import org.walletconnect.example.extension.getNavController
import org.walletconnect.example.wallet.ToggleBottomNav
import org.walletconnect.example.wallet.WalletViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.blue
                )
            )
        )
        setUiStateObserver()
        setBottomNavigation()
    }

    private fun setUiStateObserver() {
        viewModel.eventFlow.observe(this, { state ->
            (state as? ToggleBottomNav)?.apply {
                if (shouldShown) {
                    binding.bottomNav.visibility = View.VISIBLE
                } else {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        })
    }

    private fun setBottomNavigation() {
        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.wallet -> getNavController().navigate(R.id.action_dappFragment_to_walletFragment)
                R.id.dapp -> getNavController().navigate(R.id.action_walletFragment_to_dappFragment)
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.qrCodeScanner) {
            getNavController().navigate(R.id.action_walletFragment_to_scannerFragment)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when (getCurrentDestination()?.id) {
            R.id.scannerFragment -> {
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = false
                menu?.findItem(R.id.connect)?.isVisible = false
            }
            R.id.walletFragment -> {
                menu?.findItem(R.id.connect)?.isVisible = false
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = true
            }
            R.id.dappFragment -> {
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = false
                menu?.findItem(R.id.connect)?.isVisible = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }
} 