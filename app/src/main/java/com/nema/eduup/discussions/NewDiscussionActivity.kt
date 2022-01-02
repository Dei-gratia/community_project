package com.nema.eduup.discussions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.nema.eduup.R
import com.nema.eduup.databinding.ActivityNewDiscussionBinding
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.nema.eduup.BaseActivity
import com.nema.eduup.discussions.groups.GroupsFragment
import com.nema.eduup.discussions.people.PeopleFragment

class NewDiscussionActivity : BaseActivity() {

    private lateinit var binding: ActivityNewDiscussionBinding
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewDiscussionBinding.inflate(layoutInflater)
        init()
        setUpTabLayout()

        setContentView(binding.root)
    }

    private fun init() {
        tabLayout = binding.tabLayout
        viewPager = binding.viewPager
    }

    private fun setUpTabLayout() {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.add(PeopleFragment(), "People")
        adapter.add(GroupsFragment(), "Groups")
        viewPager.adapter = adapter
        viewPager.pageMargin = getResources().getDimension(R.dimen.view_pager_gap).toInt()
        viewPager.setPageMarginDrawable(R.color.colorAccent)
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)
    }

}


private class ViewPagerAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm) {
    private val fragmentList: MutableList<Fragment> = ArrayList()
    private val titleList: MutableList<String> = ArrayList()
    fun add(fragment: Fragment, title: String) {
        fragmentList.add(fragment)
        titleList.add(title)
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return titleList[position]
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }
}