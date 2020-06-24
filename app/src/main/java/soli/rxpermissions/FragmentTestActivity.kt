package soli.rxpermissions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 *
 * @author Soli
 * @Time 2019/2/19 15:41
 */
class FragmentTestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        val manager = supportFragmentManager
        manager.beginTransaction().add(R.id.container, TestFragment()).commitAllowingStateLoss()
        manager.executePendingTransactions()
    }
}