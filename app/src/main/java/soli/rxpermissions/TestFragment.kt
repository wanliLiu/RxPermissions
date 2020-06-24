package soli.rxpermissions

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.soli.permissions.RxPermissions
import kotlinx.android.synthetic.main.activity_fragment_test.*

/**
 *
 * @author Soli
 * @Time 2019/2/19 15:44
 */
class TestFragment : Fragment() {

    private val rxPermissions by lazy { RxPermissions(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_fragment_test, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentTest.setOnClickListener { requestPermissions() }
    }

    private fun requestPermissions() {
        val temp = rxPermissions.request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO)
                .subscribe { pass ->
                    Toast.makeText(activity, if (pass) "所有申请成功" else "没有都通过", Toast.LENGTH_LONG).show()
                }

    }
}