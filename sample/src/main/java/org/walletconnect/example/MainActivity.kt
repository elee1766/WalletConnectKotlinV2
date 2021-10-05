package org.walletconnect.example

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.walletconnect.example.databinding.ActivityMainBinding
import org.walletconnect.example.extension.getCurrentDestination
import org.walletconnect.example.extension.getNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.qrCodeScanner) {
            getNavController().navigate(R.id.action_sessionsFragment_to_scannerFragment)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when (getCurrentDestination()?.id) {
            R.id.scannerFragment -> menu?.findItem(R.id.qrCodeScanner)?.isVisible = false
            R.id.sessionsFragment -> menu?.findItem(R.id.qrCodeScanner)?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }
}