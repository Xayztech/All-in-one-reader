package xy.readerone.id

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import xy.readerone.id.databinding.ActivityMainBinding
import xy.readerone.id.ui.EditorFragment
import xy.readerone.id.ui.LibraryFragment
import xy.readerone.id.ui.ToolsFragment
import xy.readerone.id.ui.ViewerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestLegacyStorageIfNeeded()

        if (savedInstanceState == null) {
            switchFragment(LibraryFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> switchFragment(LibraryFragment())
                R.id.nav_create -> switchFragment(EditorFragment())
                R.id.nav_tools -> switchFragment(ToolsFragment())
            }
            true
        }

        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val viewIntent = Intent(this, ViewerActivity::class.java).apply {
                data = intent.data
            }
            startActivity(viewIntent)
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun requestLegacyStorageIfNeeded() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    101
                )
            }
        }
    }
}
