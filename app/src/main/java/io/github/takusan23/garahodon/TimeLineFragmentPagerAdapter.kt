package io.github.takusan23.garahodon

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter

class TimeLineFragmentPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(
    fragmentManager,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {

    

    override fun getItem(position: Int): Fragment {
        //切り替え
        val timeLineFragment = TimeLineFragment()
        val bundle = Bundle()
        var url = "home"
        when (position) {
            0 -> {
                url = "home?limit=40"
            }
            1 -> {
                url = "notification?limit=40"
            }
            2 -> {
                url = "public?limit=40&local=true"
            }
            3 -> {
                url = "public?limit=40"
            }
        }
        bundle.putString("url", url)
        timeLineFragment.arguments = bundle
        return timeLineFragment
    }

    override fun getCount(): Int {
        return 4
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when(position){
            0 -> {
                return "ホーム"
            }
            1 -> {
                return "通知"
            }
            2 -> {
                return "ローカル"
            }
            3 -> {
                return "連合"
            }
        }
        return "ホーム"
    }

}