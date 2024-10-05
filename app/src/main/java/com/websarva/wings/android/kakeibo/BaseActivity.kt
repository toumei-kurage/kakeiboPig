import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.websarva.wings.android.kakeibo.HomeActivity
import com.websarva.wings.android.kakeibo.MemberListActivity
import com.websarva.wings.android.kakeibo.R

abstract class BaseActivity : AppCompatActivity() {

    lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base) // 各ActivityでsetContentViewを上書きするため、この行は必要に応じて変更

        // DrawerLayoutとNavigationViewのセットアップ
        setupDrawerAndToolbar()
    }

    // DrawerLayoutとNavigationViewのセットアップを共通化
    private fun setupDrawerAndToolbar() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.app_name)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarのハンバーガーメニューアイコン設定
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションメニューアイテムのクリックリスナー設定
        navView.setNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem.itemId)
            drawerLayout.closeDrawers() // メニューを閉じる
            true
        }
    }

    // 各Activityでメニューアイテムの動作をオーバーライド可能に
    open fun onNavigationItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_member_list -> {
                val intent = Intent(this, MemberListActivity::class.java)
                startActivity(intent)
            }
            // 他のメニューアイテムを追加する場合はここに追加
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
