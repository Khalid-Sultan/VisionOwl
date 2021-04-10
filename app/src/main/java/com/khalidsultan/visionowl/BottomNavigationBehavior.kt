package com.khalidsultan.visionowl

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationBehavior : CoordinatorLayout.Behavior<BottomNavigationView?> {
    constructor() : super() {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    fun layoutDependsOn(
        parent: CoordinatorLayout?,
        child: BottomNavigationView?,
        dependency: View?
    ): Boolean {
        return dependency is FrameLayout
    }

    fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout?,
        child: BottomNavigationView?,
        directTargetChild: View?,
        target: View?,
        nestedScrollAxes: Int
    ): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout?,
        child: BottomNavigationView,
        target: View?,
        dx: Int,
        dy: Int,
        consumed: IntArray?
    ) {
        if (dy < 0) {
            showBottomNavigationView(child)
        } else if (dy > 0) {
            hideBottomNavigationView(child)
        }
    }

    private fun hideBottomNavigationView(view: BottomNavigationView) {
        view.animate().translationY(view.height.toFloat())
    }

    private fun showBottomNavigationView(view: BottomNavigationView) {
        view.animate().translationY(0f)
    }
}